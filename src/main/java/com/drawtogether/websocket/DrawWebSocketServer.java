package com.drawtogether.websocket;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.drawtogether.model.DrawEvent;
import com.drawtogether.model.DrawEventType;
import com.drawtogether.repository.InMemoryRoomRepository;
import com.drawtogether.service.RoomService;
import com.drawtogether.service.RoomServiceImpl;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DrawWebSocketServer extends WebSocketServer {

    private final RoomService roomService;
    private final Gson gson;
    private final Map<WebSocket, String> connectionToUserId;
    private final Map<WebSocket, String> connectionToRoomId;

    public DrawWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        this.roomService = new RoomServiceImpl(new InMemoryRoomRepository());
        this.gson = new Gson();
        this.connectionToUserId = new ConcurrentHashMap<>();
        this.connectionToRoomId = new ConcurrentHashMap<>();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Conexion cerrada:" + conn.getRemoteSocketAddress());

        String userId = connectionToUserId.get(conn);
        String roomId = connectionToRoomId.get(conn);

        if (userId != null && roomId != null) {
            roomService.leaveRoom(roomId, userId);
            broadcastToRoom(roomId, createResponse("USER_LEFT", "Usuario desconectado", Map.of("userId", userId)));
        }

        connectionToUserId.remove(conn);
        connectionToRoomId.remove(conn);

    }

    @Override
    public void onError(WebSocket conn, Exception arg1) {

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
            String action = jsonMessage.get("action").getAsString();

            switch (action) {
                case "JOIN_ROOM" -> handleJoinRoom(conn, jsonMessage);
                case "LEAVE_ROOM" -> handleLeaveRoom(conn, jsonMessage);
                case "DRAW_EVENT" -> handleDrawEvent(conn, jsonMessage);
                case "CREATE_ROOM" -> handleCreateRoom(conn, jsonMessage);
                case "GET_ROOMS" -> handleGetRooms(conn);
                default -> sendMessage(conn, createResponse("ERROR", "Acción no reconocida", null));
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            sendMessage(conn, createResponse("ERROR", "Error procesando mensaje", null));
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nueva conexion WebSocket:" + conn.getRemoteSocketAddress());
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
        var rooms = roomService.getAllRooms();
        Map<String, Object> response = Map.of("rooms", rooms);
        sendMessage(conn, createResponse("ROOMS_LIST", "Lista de salas", response));

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

    public void handleCreateRoom(WebSocket conn, JsonObject message) {
        String roomName = message.get("roomName").getAsString();
        int maxUsers = message.get("maxUsers").getAsInt();

        var room = roomService.createRoom(roomName, maxUsers);
        Map<String, Object> roomData = Map.of(
                "roomId", room.getId(),
                "roomName", room.getName(),
                "maxParticipants", room.getMaxParticipants());

        sendMessage(conn, createResponse("ROOM_CREATED", "Sala creada exitosamente", roomData));
    }

    private void handleLeaveRoom(WebSocket conn, JsonObject message) {
        String userId = connectionToUserId.get(conn);
        String roomId = connectionToRoomId.get(conn);

        if (userId != null && roomId != null) {
            roomService.leaveRoom(roomId, userId);
            connectionToUserId.remove(conn);
            connectionToRoomId.remove(conn);

            sendMessage(conn, createResponse("ROOM_LEFT", "Has salido de la sala", null));
            broadcastToRoom(roomId, createResponse("USER_LEFT", "Usuario salio de la sala", Map.of("userId", userId)));

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
                Map<String, Object> roomData = Map.of(
                        "roomId", roomId,
                        "drawEvents", room.getDrawEvents(),
                        "participants", room.getParticipants());
                sendMessage(conn, createResponse("ROOM_JOINED", "Te has unido a la sala", roomData));

                // Notificar a otros usuarios
                broadcastToRoom(roomId, createResponse("USER_JOINED", "Nuevo usuario se unió",
                        Map.of("userId", userId)), conn);
            });
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

        JsonObject eventData = message.getAsJsonObject("eventData");
        DrawEventType eventType = DrawEventType.valueOf(eventData.get("type").getAsString());

        DrawEvent drawEvent = new DrawEvent(
                UUID.randomUUID().toString(),
                roomId,
                LocalDateTime.now(),
                userId,
                eventType);

        roomService.addDrawEvent(roomId, drawEvent);

        // Retransmitir el evento a todos los usuarios en la sala
        Map<String, Object> eventResponse = Map.of(
                "drawEvent", drawEvent,
                "eventData", eventData.toString());

        broadcastToRoom(roomId, createResponse("DRAW_EVENT", "Evento de dibujo", eventResponse));
    }
}
