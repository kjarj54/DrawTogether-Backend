package com.drawtogether.model;

import java.time.LocalDateTime;

public class User {
    private final String id;
    private final String username;
    private final String color;
    private final LocalDateTime joinedAt;

    public User(String color, String id, String username) {
        this.color = color;
        this.id = id;
        this.username = username;
        this.joinedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getColor() {
        return color;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

}
