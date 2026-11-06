```sql
-- ==========================================
-- REAL-TIME COLLABORATIVE CODE PLATFORM
-- DATABASE SETUP
-- ==========================================

-- 1. Create the database
CREATE DATABASE IF NOT EXISTS collaborative_code;

-- 2. Select the database
USE collaborative_code;


-- 3. Create the rooms table
CREATE TABLE IF NOT EXISTS rooms (
    room_id VARCHAR(8) PRIMARY KEY,
    code LONGTEXT NOT NULL,
    language VARCHAR(20) NOT NULL DEFAULT 'java',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);


-- 4. Create the code_versions table
CREATE TABLE IF NOT EXISTS code_versions (
    version_id BIGINT NOT NULL AUTO_INCREMENT,
    room_id VARCHAR(8) NOT NULL,
    code LONGTEXT NOT NULL,
    language VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    version_name VARCHAR(100) DEFAULT NULL,
    description TEXT,
    version_number BIGINT NOT NULL,

    PRIMARY KEY (version_id),

    CONSTRAINT unique_room_version
        UNIQUE (room_id, version_number),

    CONSTRAINT code_versions_ibfk_1
        FOREIGN KEY (room_id)
        REFERENCES rooms(room_id)
        ON DELETE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


-- 5. Display available tables
SHOW TABLES;


-- 6. Display table structures
DESCRIBE rooms;
DESCRIBE code_versions;


-- 7. Display existing records
SELECT * FROM rooms;
SELECT * FROM code_versions;
```