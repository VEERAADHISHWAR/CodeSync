
package com.collaborative.platform;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

    private final JdbcTemplate jdbcTemplate;

    public RoomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Save a new room with its initial code and language
    public void createRoom(
            String roomId,
            String code,
            String language) {

        String sql = """
                INSERT INTO rooms (room_id, code, language)
                VALUES (?, ?, ?)
                """;

        jdbcTemplate.update(sql, roomId, code, language);
    }

    // Check whether a room exists
    public boolean roomExists(String roomId) {

        String sql = "SELECT COUNT(*) FROM rooms WHERE room_id = ?";

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                roomId
        );

        return count != null && count > 0;
    }

    // Update the code and language of an existing room
    public void updateCodeAndLanguage(
            String roomId,
            String code,
            String language) {

        String sql = """
                UPDATE rooms
                SET code = ?, language = ?
                WHERE room_id = ?
                """;

        jdbcTemplate.update(sql, code, language, roomId);
    }

    // Retrieve the saved code of a room
    public String getCode(String roomId) {

        String sql = "SELECT code FROM rooms WHERE room_id = ?";

        return jdbcTemplate.queryForObject(
                sql,
                String.class,
                roomId
        );
    }

    // Retrieve the saved programming language
    public String getLanguage(String roomId) {

        String sql = "SELECT language FROM rooms WHERE room_id = ?";

        return jdbcTemplate.queryForObject(
                sql,
                String.class,
                roomId
        );
    }
}
