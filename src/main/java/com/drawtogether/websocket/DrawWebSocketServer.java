package com.drawtogether.websocket;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.drawtogether.model.DrawData;
import com.drawtogether.model.DrawEvent;
import com.drawtogether.model.DrawEventType;
import com.drawtogether.repository.InMemoryRoomRepository;
import com.drawtogether.service.RoomService;
import com.drawtogether.service.RoomServiceImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

public class DrawWebSocketServer extends WebSocketServer {

    private final RoomService roomService;
    private final Gson gson;
    private final Map<WebSocket, String> connectionToUserId;
    private final Map<WebSocket, String> connectionToRoomId;
    private final Set<WebSocket> allConnections;

    public DrawWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        this.roomService = new RoomServiceImpl(new InMemoryRoomRepository());
        
        // Configurar Gson con adaptador personalizado para LocalDateTime
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
                    new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> 
                    LocalDateTime.parse(json.getAsString()))
                .create();
                
        this.connectionToUserId = new ConcurrentHashMap<>();
        this.connectionToRoomId = new ConcurrentHashMap<>();
        this.allConnections = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Conexion cerrada:" + conn.getRemoteSocketAddress());

        String userId = connectionToUserId.get(conn);
        String roomId = connectionToRoomId.get(conn);

        if (userId != null && roomId != null) {
            roomService.leaveRoom(roomId, userId);
            
            // Obtener información actualizada de la sala después de que el usuario salga
            roomService.getRoomById(roomId).ifPresent(room -> {
                // Convertir Set a List para JSON
                List<String> participantsList = new ArrayList<>(room.getParticipants());
                
                Map<String, Object> userLeftData = Map.of(
                        "userId", userId,
                        "roomId", roomId,
                        "participants", participantsList,
                        "maxParticipants", room.getMaxParticipants(),
                        "currentParticipantsCount", room.getCurrentParticipantsCount()
                );
                broadcastToRoom(roomId, createResponse("USER_LEFT", "Usuario desconectado", userLeftData));
            });
            
            // Actualizar la lista de salas para todos los clientes
            broadcastRoomListUpdate();
        }

        connectionToUserId.remove(conn);
        connectionToRoomId.remove(conn);
        allConnections.remove(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        if (conn != null) {
            System.err.println("Connection: " + conn.getRemoteSocketAddress());
        }
        ex.printStackTrace();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            System.out.println("Received message: " + message); // Debug log
            JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
            String action = jsonMessage.get("action").getAsString();

            System.out.println("Processing action: " + action); // Debug log

            switch (action) {
                case "JOIN_ROOM" -> handleJoinRoom(conn, jsonMessage);
                case "LEAVE_ROOM" -> handleLeaveRoom(conn);
                case "DRAW_EVENT" -> handleDrawEvent(conn, jsonMessage);
                case "CREATE_ROOM" -> handleCreateRoom(conn, jsonMessage);
                case "GET_ROOMS" -> handleGetRooms(conn);
                default -> {
                    System.err.println("Unrecognized action: " + action);
                    sendMessage(conn, createResponse("ERROR", "Acción no reconocida: " + action, null));
                }
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Error procesando mensaje JSON: " + e.getMessage());
            System.err.println("Mensaje recibido: " + message);
            sendMessage(conn, createResponse("ERROR", "Error procesando mensaje JSON", null));
        } catch (Exception e) {
            System.err.println("Error general procesando mensaje: " + e.getMessage());
            e.printStackTrace();
            sendMessage(conn, createResponse("ERROR", "Error interno del servidor", null));
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nueva conexion WebSocket:" + conn.getRemoteSocketAddress());
        allConnections.add(conn);
        sendMessage(conn, createResponse("CONNECTION_ESTABLISHED", "Conectado al server", null));
    }

    @Override
    public void onStart() {

    }

    private void sendMessage(WebSocket conn, String message) {
        if (conn != null && conn.isOpen()) {
            conn.send(message);
        } else {
            System.out.println("Connection is not open or is null.");
        }

    }

    private String createResponse(String type, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());

        if (data != null) {
            response.put("data", data);

        }
        return gson.toJson(response);

    }

    private void handleGetRooms(WebSocket conn) {
        try {
            var rooms = roomService.getAllRooms();
            
            // Convertir las salas a un formato serializable simple
            var roomsData = rooms.stream()
                .map(room -> Map.of(
                    "id", room.getId(),
                    "name", room.getName(),
                    "maxParticipants", room.getMaxParticipants(),
                    "currentParticipantsCount", room.getCurrentParticipantsCount(),
                    "createdAt", room.getCreatedAt().toString()
                ))
                .toList();
            
            Map<String, Object> response = Map.of("rooms", roomsData);
            sendMessage(conn, createResponse("ROOMS_LIST", "Lista de salas", response));
        } catch (Exception e) {
            System.err.println("Error getting rooms: " + e.getMessage());
            e.printStackTrace();
            sendMessage(conn, createResponse("ERROR", "Error obteniendo lista de salas", null));
        }
    }

    private void broadcastToRoom(String roomId, String message) {
        broadcastToRoom(roomId, message, null);
    }

    private void broadcastToRoom(String roomId, String message, WebSocket excludeConnection) {
        connectionToRoomId.entrySet().stream()
                .filter(entry -> roomId.equals(entry.getValue()))
                .filter(entry -> !entry.getKey().equals(excludeConnection))
                .forEach(entry -> sendMessage(entry.getKey(), message));
    }

    private void broadcastToAll(String message) {
        allConnections.forEach(conn -> sendMessage(conn, message));
    }

    public void handleCreateRoom(WebSocket conn, JsonObject message) {
        try {
            // Validar que los parámetros existen
            if (!message.has("roomName") || !message.has("maxUsers")) {
                sendMessage(conn, createResponse("ERROR", "Faltan parámetros: roomName y maxUsers son requeridos", null));
                return;
            }

            String roomName = message.get("roomName").getAsString().trim();
            int maxUsers = message.get("maxUsers").getAsInt();

            // Validaciones
            if (roomName.isEmpty()) {
                sendMessage(conn, createResponse("ERROR", "El nombre de la sala no puede estar vacío", null));
                return;
            }

            if (roomName.length() < 3) {
                sendMessage(conn, createResponse("ERROR", "El nombre de la sala debe tener al menos 3 caracteres", null));
                return;
            }

            if (maxUsers < 2 || maxUsers > 20) {
                sendMessage(conn, createResponse("ERROR", "El número de participantes debe estar entre 2 y 20", null));
                return;
            }

            var room = roomService.createRoom(roomName, maxUsers);
            
            // Crear datos simples para enviar (sin objetos complejos)
            Map<String, Object> roomData = Map.of(
                    "roomId", room.getId(),
                    "roomName", room.getName(),
                    "maxParticipants", room.getMaxParticipants(),
                    "currentParticipantsCount", room.getCurrentParticipantsCount(),
                    "createdAt", room.getCreatedAt().toString());

            sendMessage(conn, createResponse("ROOM_CREATED", "Sala creada exitosamente", roomData));
            
            System.out.println("Room created successfully: " + room.getId());
            
            // Notificar a todos los clientes conectados sobre la nueva sala
            try {
                var allRooms = roomService.getAllRooms();
                var roomsData = allRooms.stream()
                    .map(r -> Map.of(
                        "id", r.getId(),
                        "name", r.getName(),
                        "maxParticipants", r.getMaxParticipants(),
                        "currentParticipantsCount", r.getCurrentParticipantsCount(),
                        "createdAt", r.getCreatedAt().toString()
                    ))
                    .toList();
                
                Map<String, Object> updateResponse = Map.of("rooms", roomsData);
                broadcastToAll(createResponse("ROOMS_UPDATED", "Lista de salas actualizada", updateResponse));
            } catch (Exception ex) {
                System.err.println("Error broadcasting room update: " + ex.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error creating room: " + e.getMessage());
            e.printStackTrace();
            sendMessage(conn, createResponse("ERROR", "Error interno del servidor al crear la sala", null));
        }
    }

    private void handleLeaveRoom(WebSocket conn) {
        String userId = connectionToUserId.get(conn);
        String roomId = connectionToRoomId.get(conn);

        if (userId != null && roomId != null) {
            roomService.leaveRoom(roomId, userId);
            connectionToUserId.remove(conn);
            connectionToRoomId.remove(conn);

            sendMessage(conn, createResponse("ROOM_LEFT", "Has salido de la sala", null));
            
            // Obtener información actualizada de la sala después de que el usuario salga
            roomService.getRoomById(roomId).ifPresent(room -> {
                // Convertir Set a List para JSON
                List<String> participantsList = new ArrayList<>(room.getParticipants());
                
                Map<String, Object> userLeftData = Map.of(
                        "userId", userId,
                        "roomId", roomId,
                        "participants", participantsList,
                        "maxParticipants", room.getMaxParticipants(),
                        "currentParticipantsCount", room.getCurrentParticipantsCount()
                );
                broadcastToRoom(roomId, createResponse("USER_LEFT", "Usuario salio de la sala", userLeftData));
            });

            // Actualizar la lista de salas para todos los clientes
            broadcastRoomListUpdate();
        }
    }

    private void handleJoinRoom(WebSocket conn, JsonObject message) {
        String roomId = message.get("roomId").getAsString();
        String userId = message.get("userId").getAsString();

        if (roomService.joinRoom(roomId, userId)) {
            connectionToUserId.put(conn, userId);
            connectionToRoomId.put(conn, roomId);

            // Enviar historial de eventos de dibujo
            roomService.getRoomById(roomId).ifPresent(room -> {
                // Convertir Set a List para JSON
                List<String> participantsList = new ArrayList<>(room.getParticipants());
                
                Map<String, Object> roomData = Map.of(
                        "roomId", roomId,
                        "roomName", room.getName(),
                        "maxParticipants", room.getMaxParticipants(),
                        "currentParticipantsCount", room.getCurrentParticipantsCount(),
                        "drawEvents", room.getDrawEvents(),
                        "participants", participantsList);
                sendMessage(conn, createResponse("ROOM_JOINED", "Te has unido a la sala", roomData));

                // Notificar a otros usuarios con información completa de la sala
                Map<String, Object> userJoinedData = Map.of(
                        "userId", userId,
                        "roomId", roomId,
                        "participants", participantsList,
                        "maxParticipants", room.getMaxParticipants(),
                        "currentParticipantsCount", room.getCurrentParticipantsCount()
                );
                broadcastToRoom(roomId, createResponse("USER_JOINED", "Nuevo usuario se unió",
                        userJoinedData), conn);
            });

            // Actualizar la lista de salas para todos los clientes
            broadcastRoomListUpdate();
        } else {
            sendMessage(conn, createResponse("ERROR", "No se pudo unir a la sala", null));
        }
    }

    private void handleDrawEvent(WebSocket conn, JsonObject message) {
        String userId = connectionToUserId.get(conn);
        String roomId = connectionToRoomId.get(conn);

        if (userId == null || roomId == null) {
            sendMessage(conn, createResponse("ERROR", "No estás en una sala", null));
            return;
        }

        try {
            JsonObject eventData = message.getAsJsonObject("eventData");
            DrawEventType eventType = DrawEventType.valueOf(eventData.get("type").getAsString());
            
            DrawData drawData = null;
            
            // Extraer datos de dibujo si están presentes
            if (eventData.has("x") && eventData.has("y") && eventData.has("color") && eventData.has("strokeWidth")) {
                double x = eventData.get("x").getAsDouble();
                double y = eventData.get("y").getAsDouble();
                String color = eventData.get("color").getAsString();
                double strokeWidth = eventData.get("strokeWidth").getAsDouble();
                // Siempre incluir la herramienta, por defecto "brush"
                String tool = eventData.has("tool") && !eventData.get("tool").isJsonNull() ? 
                    eventData.get("tool").getAsString() : "brush";
                
                drawData = new DrawData(color, strokeWidth, x, y, tool);
            }

            DrawEvent drawEvent = new DrawEvent(
                    UUID.randomUUID().toString(),
                    roomId,
                    LocalDateTime.now(),
                    userId,
                    eventType,
                    drawData);

            roomService.addDrawEvent(roomId, drawEvent);

            // Crear respuesta con todos los datos necesarios para el frontend
            Map<String, Object> eventResponse = new HashMap<>();
            eventResponse.put("eventId", drawEvent.getId());
            eventResponse.put("userId", drawEvent.getUserId());
            eventResponse.put("timestamp", drawEvent.getTimestamp().toString());
            eventResponse.put("type", drawEvent.getType().toString());
            
            if (drawData != null) {
                Map<String, Object> drawDataMap = Map.of(
                    "x", drawData.getX(),
                    "y", drawData.getY(),
                    "color", drawData.getColor(),
                    "strokeWidth", drawData.getStrokeWidth(),
                    "tool", drawData.getTool()
                );
                eventResponse.put("drawData", drawDataMap);
            }

            // Retransmitir el evento solo a otros usuarios en la sala (NO al remitente)
            broadcastToRoom(roomId, createResponse("DRAW_EVENT", "Evento de dibujo", eventResponse), conn);
            
        } catch (Exception e) {
            System.err.println("Error processing draw event: " + e.getMessage());
            e.printStackTrace();
            sendMessage(conn, createResponse("ERROR", "Error procesando evento de dibujo", null));
        }
    }

    /**
     * Método para enviar la lista actualizada de salas a todos los clientes conectados
     */
    private void broadcastRoomListUpdate() {
        try {
            var allRooms = roomService.getAllRooms();
            var roomsData = allRooms.stream()
                .map(room -> Map.of(
                    "id", room.getId(),
                    "name", room.getName(),
                    "maxParticipants", room.getMaxParticipants(),
                    "currentParticipantsCount", room.getCurrentParticipantsCount(),
                    "createdAt", room.getCreatedAt().toString()
                ))
                .toList();
            
            Map<String, Object> updateResponse = Map.of("rooms", roomsData);
            broadcastToAll(createResponse("ROOMS_UPDATED", "Lista de salas actualizada", updateResponse));
        } catch (Exception e) {
            System.err.println("Error broadcasting room list update: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
