# VoxLink-Distributed Collaborative Workspace

VoxLink is a **client-server communication** platform built in Java, designed to simulate modern collaboration tools like Discord and Slack.

Unlike basic chat applications, VoxLink introduces a hierarchical workspace system, real-time synchronization, and role-based access control, making it a full-scale distributed systems project.

# 🚀 Key Features

## 🌐 Network Communication

- **TCP/IP** socket Communication
- Real-time server - client event broadcasting

## ⚙️ Multi-threading and Concurrency

- One thread per client model on the server
- Concurrent message handling without blocking one another
- Background workers on client side for incoming messages and file transfers

## 🗄️ Database and Persistence

- Built with **JDBC** and **SQL**
- Enforced foreign keys and constraints for data integrity

## 🖥️ Desktop GUI

- Interactive Java based UI build with JavaFX framework
- Dynamic layouts

## 📁 File Sharing and Processing

- Upload/download files via socket streams
- Server-side file storage and distribution

## 🌍 Web Integration

- Lightweight HTTP companion service
- Features like invite links, liveserver stats, account management and more

# 🏗️ System Architecture

# 🧱 Tech Stack

- **Language** - Java
- **Networking** - TCP/IP Sockets
- **Concurrency** - Java Threads
- **Database** - SQL via JDBC
- **UI** - JavaFX
- **File Handling** - Java I/O
- **Web Layer** - HTTP

# 📂 Project Structure

```plaintext
voxlink/
│
├── server/
│       └── src/
│           ├── main/
│           │       ├── config/
│           │       ├── network/
│           │       ├── service/
│           │       ├── repository/
│           │       ├── model/
│           │       ├── database/
│           │       └── util/
│           │
│           └── resources/
│
├── client/
│       └── src/
│           ├── main/
│           │       ├── network/
│           │       ├── ui/
│           │       │     ├── controllers/
│           │       │     └── components/
│           │       ├── model/
│           │       ├── state/
│           │       └── util/
│           │
│           ├── resources/
│           │       └── styles/
│           │
│           └── assets/
│                   ├── icons/
│                   └── images/         
│
└── shared/
    ├── protocol/
    ├── dto/
    └── util/
```

# 🤝 Contributors

- **ID**--------------**Name**
- ETS1350/16 Thomas Addisu
- ETS1446/16 Yeisrael Dawit
- ETS1359/16 Tinsae Zegeye

# 📜 License

This project is licensed under the MIT License – see the LICENSE file for details.
