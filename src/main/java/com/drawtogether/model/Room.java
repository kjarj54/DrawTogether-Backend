package com.drawtogether.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    private final String id;
    private final String name;
    private final Set<String> participants;
    private final List<DrawEvent> drawEvents;
    private final LocalDateTime createdAt;
    private final int maxParticipants;

    public Room(String id, String name, int maxParticipants) {
        this.id = id;
        this.name = name;
        this.maxParticipants = maxParticipants;
        this.participants = ConcurrentHashMap.newKeySet();
        this.drawEvents = Collections.synchronizedList(new ArrayList<>());
        this.createdAt = LocalDateTime.now();
    }

    public boolean addParticipant(String userId) {
        if (participants.size() >= maxParticipants){
            return false;
        }
        return participants.add(userId);
    }

    public boolean removeParticipant(String userId){
        return participants.remove(userId);
    }

    public void addDrawEvent(DrawEvent event){
        drawEvents.add(event);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getParticipants() {
        return new HashSet<>(participants);
    }

    public List<DrawEvent> getDrawEvents() {
        return new ArrayList<>(drawEvents);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public int getCurrentParticipantsCount() {
        return participants.size();
    }



}
