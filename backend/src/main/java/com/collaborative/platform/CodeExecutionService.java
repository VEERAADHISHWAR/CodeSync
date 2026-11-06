package com.collaborative.platform;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    public record ExecutionResult(
            String output,
            boolean success,
            boolean timedOut) {}

    public ExecutionResult execute(String language, String code)
            throws IOException, InterruptedException {

        String lang = language.toLowerCase(Locale.ROOT);
        Path workspace = Files.createTempDirectory("codetogether-");

        try {
            String filename;
            String image;
            List<String> command;
            int memory = 256;

            switch (lang) {
                case "python" -> {
                    filename = "main.py";
                    image = "codetogether-python";
                    command = List.of("python3", "/app/main.py");
                }
                case "java" -> {
                    filename = "Main.java";
                    image = "codetogether-java";
                    memory = 512;
                    command = List.of("sh", "-c",
                            "javac Main.java && java -cp /app Main");
                }
                case "c" -> {
                    filename = "main.c";
                    image = "codetogether-cpp";
                    command = List.of("sh", "-c",
                            "gcc main.c -o program && ./program");
                }
                case "cpp", "c++" -> {
                    filename = "main.cpp";
                    image = "codetogether-cpp";
                    command = List.of("sh", "-c",
                            "g++ main.cpp -o program && ./program");
                }
                case "javascript", "js" -> {
                    filename = "main.js";
                    image = "codetogether-node";
                    command = List.of("node", "/app/main.js");
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported language: " + language);
            }

            Files.writeString(
                    workspace.resolve(filename),
                    code,
                    StandardCharsets.UTF_8);

            String mountPath = workspace.toAbsolutePath()
                    .toString().replace("\\", "/");

            List<String> dockerCommand = new ArrayList<>(List.of(
                    "docker", "run",
                    "--rm",
                    "--network", "none",
                    "--memory", memory + "m",
                    "--cpus", "1",
                    "--pids-limit", "64",
                    "--read-only",
                    "--cap-drop", "ALL",
                    "--security-opt", "no-new-privileges",
                    "--tmpfs", "/tmp:rw,noexec,nosuid,size=64m",
                    "--mount",
                    "type=bind,source=" + mountPath + ",target=/app",
                    "--workdir", "/app",
                    image
            ));

            dockerCommand.addAll(command);

            Process process = new ProcessBuilder(dockerCommand)
                    .redirectErrorStream(true)
                    .start();

            CompletableFuture<String> outputFuture =
                    CompletableFuture.supplyAsync(
                            () -> readOutput(process));

            boolean finished = process.waitFor(
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
            }

            String output;

            try {
                output = outputFuture.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                output = "Unable to retrieve execution output.";
            }

            if (!finished) {
                output += "\nExecution timed out after "
                        + TIMEOUT_SECONDS + " seconds.";
            }

            return new ExecutionResult(
                    output,
                    finished && process.exitValue() == 0,
                    !finished);

        } finally {
            try (var paths = Files.walk(workspace)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    private String readOutput(Process process) {
        try (InputStream input = process.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int total = 0;
            int count;

            while ((count = input.read(buffer)) != -1) {
                int remaining = MAX_OUTPUT_BYTES - total;

                if (remaining <= 0) {
                    process.destroyForcibly();
                    break;
                }

                int accepted = Math.min(count, remaining);
                output.write(buffer, 0, accepted);
                total += accepted;

                if (accepted < count) {
                    process.destroyForcibly();
                    break;
                }
            }

            String result = output.toString(StandardCharsets.UTF_8);

            if (total >= MAX_OUTPUT_BYTES) {
                result += "\n[Output limit reached.]";
            }

            return result;

        } catch (IOException e) {
            return "Error reading output: " + e.getMessage();
        }
    }
}
