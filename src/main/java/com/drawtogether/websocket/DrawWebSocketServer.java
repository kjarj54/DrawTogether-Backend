package com.drawtogether.websocket;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.drawtogether.repository.InMemoryRoomRepository;
import com.drawtogether.service.RoomService;
import com.drawtogether.service.RoomServiceImpl;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
        
    }

    @Override
    public void onError(WebSocket conn, Exception arg1) {

    }

    @Override
    public void onMessage(WebSocket conn, String arg1) {

    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nueva conexion WebSocket:" + conn.getRemoteSocketAddress());
        sendMessage(conn,createResponse("CONNECTION_ESTABLISHED", "Conectado al server", null));

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
        Map<String,Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());

        if (data != null){
            response.put("data", data);

        }
        return gson.toJson(response);

    }

    private void handleGetRooms(WebSocket conn){
        var rooms = roomService.getAllRooms();
        Map<String, Object> response = Map.of("rooms",rooms);
        sendMessage(conn, createResponse("ROOMS_LIST", "Lista de salas", response));

    }

    private void broadcastToRoom(String roomId, String message){
        broadcastToRoom(roomId, message, null);
    }

    private void broadcastToRoom(String roomId, String message, WebSocket excludeConnection){
        connectionToRoomId.entrySet().stream()
            .filter(entry -> roomId.equals(entry.getValue()))
            .filter(entry -> !entry.getKey().equals(excludeConnection))
            .forEach(entry -> sendMessage(entry.getKey(), message));
    }

    public void handleCreateRoom(WebSocket conn, JsonObject message){
        String roomName = message.get("roomName").getAsString();
        int maxUsers = message.get("maxUsers").getAsInt();

        var room = roomService.createRoom(roomName, maxUsers);
        Map<String, Object> roomData = Map.of(
            "roomId", room.getId(),
            "roomName", room.getName(),
            "maxParticipants", room.getMaxParticipants()
        );

        sendMessage(conn, createResponse("ROOM_CREATED", "Sala creada exitosamente", roomData));
    }
}
