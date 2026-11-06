# CodeTogether – Real-Time Collaborative Code Sharing Platform

CodeTogether is a browser-based collaborative coding platform that allows multiple users to work on the same code editor in real time. Users can create or join rooms, write and share code, execute programs, preview HTML/CSS, and manage code versions.

## Features

- **Collaborative Code Editing:** Multiple users can edit code in a shared room using Monaco Editor.
- **Real-Time Synchronization:** WebSockets synchronize code changes between connected users.
- **Room Management:** Create rooms and share room IDs with collaborators.
- **Multi-Language Support:** Java, C, C++, Python, and JavaScript.
- **Code Execution:** Execute supported programs in isolated Docker containers.
- **HTML/CSS Preview:** Preview web code directly in the browser.
- **Version History:** Save, view, restore, and delete code versions.
- **Database Persistence:** Store room code, language, and version history in MySQL.
- **User Presence:** Display participants connected to a collaborative room.

## Technology Stack

| Component | Technologies |
|---|---|
| Frontend | React, Vite, JavaScript, CSS |
| Code Editor | Monaco Editor |
| Backend | Java 24, Spring Boot |
| Real-Time Communication | WebSocket, STOMP |
| Database | MySQL 8 |
| Code Execution | Docker |
| Build Tool | Maven |
| Version Control | Git, GitHub |

## System Architecture

1. React provides the browser-based user interface and Monaco code editor.
2. Spring Boot handles room management, database operations, version history, and code execution requests.
3. STOMP over WebSocket synchronizes code changes and participant presence.
4. MySQL stores room data and version history.
5. Docker containers execute supported programming languages in isolated environments.

## Project Structure

```text
CollaborativeCodePlatform/
│
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/collaborative/platform/
│   │       └── resources/
│   └── pom.xml
│
├── frontend/
│   ├── public/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
│
├── database/
│   └── database_setup.sql
│
├── docker/
│   ├── Dockerfile
│   ├── Dockerfile.java
│   ├── Dockerfile.cpp
│   ├── Dockerfile.node
│   └── workspace/
│
├── .gitignore
└── README.md
```

## Prerequisites

Install the following software before running the project locally:

- Java Development Kit (JDK) 24
- Apache Maven 3.10 or compatible version
- Node.js 20 or compatible version
- npm
- MySQL 8
- Docker Desktop
- Git

Make sure Docker Desktop is running before executing programs through the platform.

## Local Setup and Installation

### 1. Clone the Repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd CollaborativeCodePlatform
```

### 2. Configure the Database

Start MySQL and open MySQL Workbench.

Execute the database setup script:

```text
database/database_setup.sql
```

This creates the `collaborative_code` database and its required tables:

- `rooms`
- `code_versions`

### 3. Configure Backend Database Credentials

Create the following file:

```text
backend/src/main/resources/application-local.properties
```

Add your local MySQL configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/collaborative_code
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD

spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
server.port=8080
```

Replace the username and password with your local MySQL credentials.

**Important:** Do not commit this local configuration file or share your database credentials.

### 4. Build Docker Images

From the project root directory, run:

```bash
docker build -f docker/Dockerfile -t codetogether-python .
docker build -f docker/Dockerfile.java -t codetogether-java .
docker build -f docker/Dockerfile.cpp -t codetogether-cpp .
docker build -f docker/Dockerfile.node -t codetogether-node .
```

Verify the images:

```bash
docker images
```

### 5. Start the Backend

Open a terminal:

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

The backend runs at:

```text
http://localhost:8080
```

### 6. Start the Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the Vite development URL displayed in the terminal, usually:

```text
http://localhost:5173
```

The application is now ready for local use.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/rooms` | Create a room |
| GET | `/api/rooms/{roomId}` | Retrieve room details |
| GET | `/api/rooms/{roomId}/versions` | Retrieve version history |
| DELETE | `/api/rooms/{roomId}/versions/{versionId}` | Delete a version |
| POST | `/api/execute` | Execute supported code |

## WebSocket Communication

WebSocket endpoint:

```text
/ws
```

Application destinations:

| Destination | Purpose |
|---|---|
| `/app/code/{roomId}` | Submit code and language changes |
| `/app/version/{roomId}` | Save a code version |
| `/app/restore/{roomId}` | Restore a previous version |
| `/app/presence/{roomId}` | Send participant presence updates |

Room updates are broadcast through:

```text
/topic/room/{roomId}
```

Participant updates are broadcast through:

```text
/topic/presence/{roomId}
```

## Code Execution and Security

Supported programming languages are executed through Docker containers.

The current execution configuration includes:

- Execution timeout.
- Memory and CPU limits.
- Process count limits.
- Network isolation.
- Read-only container root filesystem.
- Dropped Linux capabilities.
- Restricted temporary filesystem.
- Temporary workspace cleanup.

**Security note:** These restrictions provide a baseline for container isolation, not a guarantee of production-grade security. Running untrusted code on a public server requires additional hardening and security testing.

## Current Limitations and Future Improvements

Potential future enhancements include:

- Multi-file project support with file explorer and tabs.
- Git integration and project import/export.
- Contribution tracking and code review.
- Code version comparison and change highlighting.
- Improved collaborative cursor synchronization.
- Stronger autosave and recovery mechanisms.
- Room access controls and permissions.
- Additional execution security and deployment hardening.

## License

This project is currently developed as an academic and portfolio project. A license can be added when the repository's distribution terms are finalized.