# voxlink: distributed real-time collaboration topology

voxlink is a high-performance, concurrent client-server architecture built in pure java. designed to handle low-latency packet multiplexing, it simulates modern collaborative ecosystems (like discord or slack) using raw tcp sockets for state synchronization and high-throughput udp datagrams for voice telemetry. 

built strictly without bloated frameworks, voxlink relies on bare-metal java concurrency, custom binary packet serialization, and low-level socket streams. the entire codebase is geared toward deterministic packet routing, unyielding thread safety, and relentless runtime efficiency. it features hierarchical workspaces, real-time broadcasting, and role-based access control built from scratch.

## core architecture & payload telemetry

### async tcp/ip protocol layer
- the primary transport layer operates over persistent, full-duplex tcp/ip sockets.
- object streams are hijacked using custom `packet` data-transfer objects, avoiding the bloat of standard json/xml parsing. each payload consists of strongly typed `requesttype` or `responsetype` headers.
- a multiplexed event-loop utilizes a strict one-thread-per-client ingress architecture. the `clienthandler` daemon continuously reads the `objectinputstream` in a non-blocking loop, immediately dispatching inbound bytecode into the `packetprocessor` for concurrent evaluation.
- server-side message broadcasting runs through zero-latency memory pointers across a global static `concurrenthashmap<integer, clienthandler>`, pushing outbound packets directly into connected output streams to achieve real-time server-client event broadcasting. 

### concurrent voice streaming (udp)
- real-time voice telemetry is physically decoupled from the tcp command layer and transmitted via volatile udp datagram sockets targeting a dedicated `voiceserver`.
- raw hardware interfacing is achieved via the `javax.sound.sampled` api. a dedicated `audiocapturethread` binds directly to the active `targetdataline` (microphone), ripping raw byte arrays in 20ms chunks.
- payload spec: non-blocking audio packet streaming enforced at 16khz, 16-bit mono.
- these udp datagrams are fired rapidly at the server, which then mirrors the payload to all concurrent active clients in the localized `voicesessionmanager` session map.
- stateful hardware interruptions enable instantaneous buffer drops when the client triggers local mute/deafen execution.

### file sharing and byte stream processing
- arbitrary binary blobs (images, documents) are buffered and streamed directly over the raw socket layer.
- background worker threads on the client side manage chunked byte uploading and downloading to completely isolate heavy i/o blocking from the main execution threads.
- server-side local storage engine manages physical file distribution, generating unique cryptographic hashes for storage paths and returning mapped metadata dtos to connected peers.

### distributed state & strict thread safety
- the global operational state uses pure `concurrenthashmap` and thread-safe collections to prevent volatile read/write collisions across the multi-threaded network boundary.
- strict mutex synchronization is utilized during concurrent writes (adding users, purging invites, mutating channel structures).
- ui threading is strictly segregated from the network stack. the javafx main application thread is treated as an immutable sink; all reactive ui updates triggered by incoming network packets are offloaded using precisely bounded `platform.runlater()` blocks to ensure the gui never hangs during high-volume packet bursts.

### persistence & low-level memory schema
- the persistence layer consists of bare-metal jdbc sql queries. no orm overhead.
- queries are piped through pre-compiled `preparedstatement` caching, stripping out sql injection vulnerabilities while optimizing execution timing at the database engine level.
- the relational graph strictly enforces referential integrity using `on delete cascade` and explicitly declared foreign keys across `users`, `workspaces`, `channels`, and `messages` tables to guarantee zero orphan records.
- transient user state is aggressively cached in memory to intercept repetitive read cycles, dynamically pruning read payloads directly from ram before they hit the disk cluster.

### lightweight http companion service & web integration
- an embedded, low-footprint http daemon runs parallel to the tcp/udp sockets.
- designed for external web integration, allowing dynamic generation and resolution of workspace invite links.
- exposes stateless rest endpoints for live server telemetry, connected user statistics, and decoupled account management outside of the heavy java client.

### rmi & decentralized orchestration
- the java rmi (remote method invocation) registry handles isolated remote procedural orchestration on a parallel subsystem.
- decoupled interface contracts guarantee stateless execution of complex micro-tasks, exposing internal server endpoints directly over the remote boundary without mixing logic with the main tcp switchboard.

## major system features

### a. network programming (the core system)
- **full-duplex tcp/ip sockets**: persistent connection topology established via [ServerSocketListener.java](src/voxlink/server/src/main/network/ServerSocketListener.java) and managed by [ClientHandler.java](src/voxlink/server/src/main/network/ClientHandler.java). this guarantees bidirectional state synchronization between remote clients and the centralized server over persistent raw streams.
- **custom network protocol**: state is marshaled using a custom transport packet wrapper defined in [Packet.java](src/voxlink/shared/protocol/Packet.java). it exposes strongly typed packet headers defined in [RequestType.java](src/voxlink/shared/protocol/RequestType.java) (e.g. `auth_login`, `auth_register`, `workspace_join`, `message_send`, `user_update_status`) to bypass high serialization overhead.
- **real-time presence notifications**: user status updates and active sessions are propagated immediately. when status transitions occur, [ClientHandler.java](src/voxlink/server/src/main/network/ClientHandler.java) maps the online presence and broadcasts updates asynchronously to all active client streams using the global concurrent client registry.

### b. multi-threading & concurrency
- **server-side socket multiplexing**: client connections are accepted by the server listener and offloaded to a fixed thread-per-client model using a `fixedthreadpool` containing worker daemons executing [ClientHandler.java](src/voxlink/server/src/main/network/ClientHandler.java) run loops. this prevents synchronous I/O blocking from causing connection bottlenecks.
- **client-side worker execution**: client-side transport uses a dedicated background daemon thread [MessageReceiver.java](src/voxlink/client/src/main/network/MessageReceiver.java) to continuously poll incoming server payloads. asynchronous background processes for file storage are offloaded to [FileUploader.java](src/voxlink/client/src/main/network/FileUploader.java) and [FileDownloader.java](src/voxlink/client/src/main/network/FileDownloader.java) threads, keeping the graphics loop smooth and lag-free.

### c. database & persistence (jdbc)
- **raw jdbc integration**: database interactions bypass heavyweight object-relational mapping (orm) to prevent query overhead. connection pooling and statement compilation are handled directly in [DBConnection.java](src/voxlink/server/src/main/database/DBConnection.java) using parameterized SQL queries.
- **relational relational graph schema**: schema normalization is defined in [SchemaInitializer.java](src/voxlink/server/src/main/database/SchemaInitializer.java). tables include `users`, `workspaces`, `workspace_members`, `channels`, `channel_members`, `roles`, `user_roles`, `messages`, `file_attachments`, `invites`, `friendships`, and `audit_logs`.
- **foreign key referential integrity**: constraints are set at the database layer (e.g. `on delete cascade` and `on delete set null`). this ensures that when a workspace or channel is deleted, the relational graph remains completely consistent, purging orphan rows automatically to prevent data corruption.

### d. graphical user interface (gui)
- **javafx dashboard layout**: features a modern multi-pane layout styled via [styles.css](src/voxlink/client/src/main/resources/styles/styles.css). layout coordinates a left-hand navigation pane for workspaces and channels, a central chat stream rendering message history, and a right-hand active presence user list.
- **thread-safe ui updates**: incoming event packets from the [MessageReceiver.java](src/voxlink/client/src/main/network/MessageReceiver.java) daemon cannot write directly to the scene graph without causing thread collisions. all visual rendering modifications are offloaded to the main thread queue using `platform.runlater()` within [MainViewController.java](src/voxlink/client/src/main/ui/controllers/MainViewController.java).

### e. file processing & media management
- **chunked socket file streaming**: binary objects are transferred over sockets. upload and download tasks are managed by [FileUploader.java](src/voxlink/client/src/main/network/FileUploader.java) and [FileDownloader.java](src/voxlink/client/src/main/network/FileDownloader.java), parsing files into byte arrays and writing them to the disk filesystem sequentially using Java file I/O streams.
- **chat audit logging engine**: server logs system actions into the database via [AuditLogRepository.java](src/voxlink/server/src/main/repository/AuditLogRepository.java). an administrator utility executes raw queries, parses history, and compiles audit records into structured CSV strings exported directly to client files using stream processing.

## execution trace

1. ingress phase: the `serversocketlistener` daemon traps an incoming socket connection. it spins up a distinct `clienthandler` thread and injects it into the concurrent active pool.
2. deserialization pipeline: the client payload drops in. the byte stream is decoded, extracting the `requesttype` flag.
3. parallel processing: `packetprocessor.handlepacket()` catches the signature and routes the raw request into segmented service logic blocks.
4. state graph morph: the server alters the internal memory maps, runs the transactional sql batch, and subsequently constructs a return `packet`.
5. outbound flood: utilizing `server.sendpackettouser()` or broadcast logic, the packet bypasses the main thread and flushes directly into the target client's `objectoutputstream`.
6. client-side ingest: the `messagereceiver` daemon catches the raw bytes, parses the server's `responsetype`, and fires off an asynchronous ui render interrupt into the javafx queue.

## build matrix & hardware reqs

- backend runtime: pure java (jdk 17 standard)
- network primitives: `java.net.socket`, `java.net.serversocket`, `java.net.datagramsocket`, `java.rmi`
- audio hardware translation: `javax.sound.sampled`
- graphics pipeline: javafx & fxml (hardware accelerated rendering via native bindings, dynamic layouts, css styling)
- persistence runtime: raw sql via standard jdbc driver
- build system: apache maven

## directory tree map

```plaintext
voxlink/
 ├─ server/
 │   ├─ config/     (runtime properties, env loaders)
 │   ├─ database/   (schema intialization, connection pooling, migrations)
 │   ├─ network/    (tcp listeners, client handler threads, udp voice packet bridges)
 │   ├─ service/    (http companion daemons, web integration logic)
 │   ├─ repository/ (raw sql prepared statement executions)
 │   ├─ model/      (backend domain objects)
 │   └─ rmi/        (remote method dispatchers)
 │
 ├─ client/
 │   ├─ main/
 │   │   ├─ media/      (microphone dataline ingestion, speaker hardware out)
 │   │   ├─ network/    (tcp egress buffers, udp voice client loops)
 │   │   ├─ model/      (client-side data encapsulation)
 │   │   ├─ state/      (client-side caching, reactive object states)
 │   │   └─ ui/         (scene graph controllers, fxml parsing, interactive components)
 │   │
 │   ├─ resources/
 │   │   └─ styles/     (css cascading stylesheets)
 │   │
 │   └─ assets/
 │       ├─ icons/
 │       └─ images/
 └─ shared/
     ├─ dto/        (stateless network transfer objects)
     ├─ protocol/   (binary protocol enums, packet headers)
     └─ util/       (cryptographic hashing, shared helpers)
```

## auth keys

- ets1350/16 thomas addisu
- ets1359/16 tinsae zegeye
- ets1446/16 yeisrael dawit

mit license. code speaks for itself. read the source.
