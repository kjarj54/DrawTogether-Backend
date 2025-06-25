package com.drawtogether.model;

import java.time.LocalDateTime;

public class DrawEvent {
    private final String id;
    private final String roomId;
    private final String userId;
    private final LocalDateTime timestamp;
    private final DrawEventType type;

    public DrawEvent(String id, String roomId, LocalDateTime timestamp, String userId, DrawEventType type) {
        this.id = id;
        this.roomId = roomId;
        this.timestamp = timestamp;
        this.userId = userId;
        this.type = type;
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
}
