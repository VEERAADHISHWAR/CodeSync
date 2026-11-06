package com.collaborative.platform;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/execute")
@CrossOrigin(origins = "http://localhost:5173")
public class ExecutionController {

    private final CodeExecutionService executionService;

    public ExecutionController(CodeExecutionService executionService) {
        this.executionService = executionService;
    }

    public record ExecutionRequest(String language, String code) {}

    @PostMapping
    public ResponseEntity<?> executeCode(
            @RequestBody ExecutionRequest request) {

        if (request == null ||
                request.language() == null ||
                request.code() == null ||
                request.code().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Language and code are required.");
        }

        if (request.code().length() > 100_000) {
            return ResponseEntity.badRequest()
                    .body("Code exceeds the 100 KB limit.");
        }

        try {
            var result = executionService.execute(
                    request.language(), request.code());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Docker execution failed: " + e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError()
                    .body("Execution was interrupted.");
        }
    }
}
