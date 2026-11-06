
package com.collaborative.platform;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    private final RoomRepository roomRepository;

    // Initial Java code for every newly created room
    private static final String DEFAULT_LANGUAGE = "java";

    private static final String DEFAULT_CODE = """
            public class Main {

                public static void main(String[] args) {

                    System.out.println("Welcome to CodeTogether!");

                }
            }
            """;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    // Create a room with starter code already saved in MySQL
    @PostMapping("/api/rooms")
    public Map<String, String> createRoom() {

        String roomId =
                UUID.randomUUID().toString().substring(0, 8);

        roomRepository.createRoom(
                roomId,
                DEFAULT_CODE,
                DEFAULT_LANGUAGE
        );

        return Map.of(
                "roomId", roomId,
                "message", "Room created successfully"
        );
    }

    // Join an existing room and retrieve its saved code
    @GetMapping("/api/rooms/{roomId}")
    public ResponseEntity<Map<String, String>> joinRoom(
            @PathVariable String roomId) {

        if (roomRepository.roomExists(roomId)) {

            String savedCode = roomRepository.getCode(roomId);
            String savedLanguage = roomRepository.getLanguage(roomId);

            return ResponseEntity.ok(
                    Map.of(
                            "roomId", roomId,
                            "message", "Joined room successfully",
                            "code", savedCode,
                            "language", savedLanguage
                    )
            );
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Room not found"));
    }
}
