# DrawTogether Backend

Backend server for the collaborative drawing application DrawTogether. This WebSocket server allows multiple users to draw in real-time on a shared canvas.

## 🚀 Features

- **Real-time collaboration**: Multiple users can draw simultaneously
- **Room management**: Room system with participant limits
- **WebSocket**: Low-latency bidirectional communication
- **In-memory persistence**: Drawing events are maintained during the session
- **User management**: Control of user entry and exit from rooms

## 🛠️ Technologies

- **Java 21**: Main programming language
- **Maven**: Dependency management and project building
- **Java-WebSocket**: WebSocket server implementation
- **Gson**: JSON serialization/deserialization
- **Logback**: Logging system
- **JUnit 5**: Testing framework

## 📋 Requirements

- Java JDK 21 or higher
- Maven 3.6+
- Port 8080 available (configurable)

## 🏗️ Project Structure

```
src/
├── main/
│   └── java/
│       └── com/
│           └── drawtogether/
│               ├── Main.java                    # Application entry point
│               ├── model/                       # Data models
│               │   ├── DrawData.java           # Draw data (coordinates, color, etc.)
│               │   ├── DrawEvent.java          # Complete draw event
│               │   ├── DrawEventType.java      # Event types (DRAW, JOIN, LEAVE, etc.)
│               │   ├── Room.java               # Room model
│               │   └── User.java               # User model
│               ├── repository/                  # Data access layer
│               │   ├── RoomRepository.java     # Repository interface
│               │   └── InMemoryRoomRepository.java # In-memory implementation
│               ├── service/                     # Business logic
│               │   ├── RoomService.java        # Service interface
│               │   └── RoomServiceImpl.java    # Service implementation
│               └── websocket/                   # WebSocket server
│                   └── DrawWebSocketServer.java # Main WebSocket server
└── test/
    └── java/                                   # Unit tests
```

## 🚀 Installation and Execution

### 1. Clone the repository
```bash
git clone https://github.com/kjarj54/DrawTogether-Backend.git
cd DrawTogether-Backend
```

### 2. Compile the project
```bash
mvn clean compile
```

### 3. Run the server
```bash
# Run with default port (8080)
mvn exec:java -Dexec.mainClass="com.drawtogether.Main"

# Or run with custom port
mvn exec:java -Dexec.mainClass="com.drawtogether.Main" -Dexec.args="9090"
```

### 4. Alternative: Create executable JAR
```bash
# Compile and package
mvn clean package

# Run JAR
java -cp target/classes com.drawtogether.Main [port]
```

## 📡 WebSocket API

The server listens for WebSocket connections at `ws://localhost:8080` (or the configured port).

### Event Types

#### Client → Server

**JOIN_ROOM**: Join a room
```json
{
    "type": "JOIN_ROOM",
    "roomId": "room123",
    "userId": "user456",
    "userName": "John"
}
```

**LEAVE_ROOM**: Leave a room
```json
{
    "type": "LEAVE_ROOM",
    "roomId": "room123",
    "userId": "user456"
}
```

**DRAW**: Send drawing data
```json
{
    "type": "DRAW",
    "roomId": "room123",
    "userId": "user456",
    "drawData": {
        "x": 100,
        "y": 150,
        "color": "#FF0000",
        "strokeWidth": 3,
        "tool": "brush"
    }
}
```

**CREATE_ROOM**: Create new room
```json
{
    "type": "CREATE_ROOM",
    "roomName": "My Room",
    "maxParticipants": 10,
    "userId": "user456",
    "userName": "John"
}
```

#### Server → Client

**USER_JOINED**: Notifies that a user joined
```json
{
    "type": "USER_JOINED",
    "roomId": "room123",
    "user": {
        "id": "user456",
        "name": "John"
    },
    "participants": ["user123", "user456"]
}
```

**USER_LEFT**: Notifies that a user left
```json
{
    "type": "USER_LEFT",
    "roomId": "room123",
    "userId": "user456",
    "participants": ["user123"]
}
```

**DRAW_EVENT**: Broadcasts drawing event
```json
{
    "type": "DRAW_EVENT",
    "roomId": "room123",
    "userId": "user456",
    "drawData": {
        "x": 100,
        "y": 150,
        "color": "#FF0000",
        "strokeWidth": 3,
        "tool": "brush"
    },
    "timestamp": "2025-06-25T10:30:00"
}
```

**ROOM_CREATED**: Confirms room creation
```json
{
    "type": "ROOM_CREATED",
    "roomId": "room789",
    "roomName": "My Room",
    "maxParticipants": 10
}
```

**ERROR**: Notifies errors
```json
{
    "type": "ERROR",
    "message": "Room is full",
    "code": "ROOM_FULL"
}
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run tests with detailed report
mvn test -Dsurefire.useFile=false
```

## 🔧 Configuration

### Server port
The server uses port 8080 by default. You can change it:
- Passing as argument: `java Main 9090`
- Modifying the `port` variable in `Main.java`

### Participants limit per room
Configurable when creating each room (default value can be modified in `RoomServiceImpl.java`)

### Logging
Log configuration is located in `src/main/resources/logback.xml` (if it exists) or uses Logback's default configuration.

## 🐛 Troubleshooting

### Port in use
```
Error: Address already in use
```
**Solution**: Change the port or kill the process using it:
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### WebSocket connection issues
1. Verify that the server is running
2. Check that no firewall is blocking the port
3. Make sure to use the correct URL: `ws://localhost:8080`

### Compilation errors
```bash
# Clean and recompile
mvn clean install
```

## 🤝 Contributing

1. Fork the project
2. Create a branch for your feature (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is under the MIT License. See the `LICENSE` file for more details.

## 📞 Contact

For questions or suggestions, you can:
- Open an issue on GitHub
- Contact the development team



