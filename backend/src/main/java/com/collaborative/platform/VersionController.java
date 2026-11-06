package com.collaborative.platform;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class VersionController {

    private final CodeVersionRepository codeVersionRepository;

    public VersionController(CodeVersionRepository codeVersionRepository) {
        this.codeVersionRepository = codeVersionRepository;
    }

    // Retrieve version history for a room
    @GetMapping("/api/rooms/{roomId}/versions")
    public List<Map<String, Object>> getVersions(
            @PathVariable String roomId) {

        return codeVersionRepository.getVersions(roomId);
    }

    // Delete a version only if it belongs to the specified room
    @DeleteMapping("/api/rooms/{roomId}/versions/{versionId}")
    public ResponseEntity<Map<String, String>> deleteVersion(
            @PathVariable String roomId,
            @PathVariable long versionId) {

        int deletedRows = codeVersionRepository.deleteVersion(
                roomId,
                versionId
        );

        if (deletedRows > 0) {
            return ResponseEntity.ok(
                    Map.of("message", "Version deleted successfully")
            );
        }

        return ResponseEntity.status(404).body(
                Map.of("message", "Version not found")
        );
    }
}