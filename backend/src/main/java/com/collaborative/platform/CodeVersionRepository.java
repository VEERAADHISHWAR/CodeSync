
package com.collaborative.platform;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CodeVersionRepository {

    private final JdbcTemplate jdbcTemplate;

    public CodeVersionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean saveVersion(
            String roomId,
            String code,
            String language,
            String versionName,
            String description) {

        // Lock this room while generating its next version number.
        jdbcTemplate.queryForObject(
                "SELECT room_id FROM rooms WHERE room_id = ? FOR UPDATE",
                String.class,
                roomId
        );

        // Prevent saving the same code and language consecutively.
        String checkSql = """
                SELECT COUNT(*)
                FROM code_versions
                WHERE room_id = ?
                AND code = ?
                AND language = ?
                AND version_id = (
                    SELECT MAX(version_id)
                    FROM code_versions
                    WHERE room_id = ?
                )
                """;

        Integer count = jdbcTemplate.queryForObject(
                checkSql,
                Integer.class,
                roomId, code, language, roomId
        );

        if (count != null && count > 0) {
            System.out.println("Duplicate version skipped.");
            return false;
        }

        // Generate the next version number for this room.
        String numberSql = """
                SELECT COALESCE(MAX(version_number), 0) + 1
                FROM code_versions
                WHERE room_id = ?
                """;

        Long nextVersionNumber = jdbcTemplate.queryForObject(
                numberSql,
                Long.class,
                roomId
        );

        // Generate a default name when the user doesn't provide one.
        String finalVersionName =
                (versionName == null || versionName.isBlank())
                        ? "Version " + nextVersionNumber
                        : versionName.trim();

        String finalDescription =
                (description == null || description.isBlank())
                        ? null
                        : description.trim();

        // IMPORTANT: version_number is now included in the INSERT.
        String insertSql = """
                INSERT INTO code_versions
                (room_id, version_number, code, language,
                 version_name, description)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                insertSql,
                roomId,
                nextVersionNumber,
                code,
                language,
                finalVersionName,
                finalDescription
        );

        System.out.println(
                "Version saved successfully: " + finalVersionName
        );

        return true;
    }

    // Retrieve all versions of a room.
    public List<Map<String, Object>> getVersions(String roomId) {

        String sql = """
                SELECT version_id, version_number, room_id, code,
                       language, version_name, description, created_at
                FROM code_versions
                WHERE room_id = ?
                ORDER BY version_number DESC
                """;

        return jdbcTemplate.queryForList(sql, roomId);
    }

    // Delete a version belonging to the specified room.
    public int deleteVersion(String roomId, long versionId) {

        String sql = """
                DELETE FROM code_versions
                WHERE room_id = ?
                AND version_id = ?
                """;

        return jdbcTemplate.update(sql, roomId, versionId);
    }
}
