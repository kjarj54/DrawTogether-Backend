package com.drawtogether.model;

import java.time.LocalDateTime;

public class DrawEvent {
    private final String id;
    private final String roomId;
    private final String userId;
    private final LocalDateTime timestamp;
    private final DrawEventType type;
    private final DrawData drawData;

    public DrawEvent(String id, String roomId, LocalDateTime timestamp, String userId, DrawEventType type, DrawData drawData) {
        this.id = id;
        this.roomId = roomId;
        this.timestamp = timestamp;
        this.userId = userId;
        this.type = type;
        this.drawData = drawData;
    }

    public DrawEvent(String id, String roomId, LocalDateTime timestamp, String userId, DrawEventType type) {
        this(id, roomId, timestamp, userId, type, null);
    }

    public String getId(){
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getUserId() {
        return userId;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public DrawEventType getType() {
        return type;
    }

    public DrawData getDrawData() {
        return drawData;
    }
}
