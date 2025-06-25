package com.drawtogether.service;

import java.util.Collection;
import java.util.Optional;

import com.drawtogether.model.DrawEvent;
import com.drawtogether.model.Room;

public interface RoomService {
    Room createRoom(String name, int maxParticipants);
    Optional<Room> getRoomById(String roomId);
    Collection<Room> getAllRooms();
    boolean joinRoom(String roomId, String userId);
    boolean leaveRoom(String roomId, String userId);
    boolean deleteRoom(String roomId);
    void addDrawEvent(String roomId, DrawEvent event);
}
