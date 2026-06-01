# VoxLink — Distributed Collaborative Workspace

VoxLink is a **client–server** communication platform built in Java, inspired by Discord and Slack. It features nested workspaces, channels, role-based access control, real-time TCP messaging, JDBC persistence, JavaFX desktop UI, and a lightweight HTTP web companion.

## Key features

- **TCP sockets** — Custom packet protocol (`AUTH_*`, `WORKSPACE_*`, `CHANNEL_*`, `MESSAGE_*`, file transfer)
- **Multi-threading** — Thread-per-client on the server; background send/receive on the client
- **MySQL + JDBC** — Relational schema with foreign keys
- **JavaFX UI** — Login, registration, and a four-column main workspace (servers, channels, chat, members)
- **File sharing** — Socket-based uploads via `FileUploader`
- **Web portal** — Invite links and stats under `src/voxlink/server/src/resources/web/`

## Project layout

```text
VoxLink-Platform/
├── pom.xml                          # Maven build (Java 17, JavaFX 21)
└── src/voxlink/
    ├── shared/                      # DTOs, protocol, constants
    ├── client/src/
    │   ├── main/                    # ClientMain, models, network, UI controllers
    │   └── resources/
    │       ├── fxml/                # Login, Register, MainView
    │       └── css/                 # Themes
    └── server/src/
        ├── main/                    # ServerMain, handlers, repositories
        └── resources/               # db.properties, web assets
```

## Prerequisites

- **JDK 17+** (JavaFX is pulled in by Maven for the client)
- **Maven 3.9+**
- **MySQL** with database `voxlink_db` (see `src/voxlink/server/src/resources/db.properties`)

## Quick start

### 1. Database

Create the database and adjust credentials in `db.properties`, then start the server once to run `SchemaInitializer`.

### 2. Start the server

```bash
mvn -q exec:java -Dexec.mainClass=voxlink.server.src.main.ServerMain
```

Default socket port: **8888** (see `voxlink.shared.util.Constants`).

### 3. Start the desktop client

```bash
mvn -q javafx:run
```

Optional overrides:

```bash
mvn -q javafx:run -Dvoxlink.host=localhost -Dvoxlink.port=8888
```

### IntelliJ / VS Code

- **Server main class:** `voxlink.server.src.main.ServerMain`
- **Client main class:** `voxlink.client.src.main.ClientMain`
- Mark `src/voxlink/client/src/resources` as a resources root so `/fxml/` and `/css/` load on the classpath.

## UI flow

1. **Login** — Connects to the server and authenticates.
2. **Register** — Creates an account, then returns to login.
3. **Main view** — Workspaces (left rail), channels, live chat, member list.
   - **+** on the rail: create a workspace or join via invite code.
   - **+** next to channel sections: create text or voice channels.
   - **📎** — attach a file to the current channel.
   - **⚙** on the user panel — change online status.

## Contributors

| ID | Name |
|----|------|
| ETS1350/16 | Thomas Addisu |
| ETS1446/16 | Yeisrael Dawit |
| ETS1359/16 | Tinsae Zegeye |

## License

MIT — see LICENSE.
