package com.drawtogether.service;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.drawtogether.model.DrawEvent;
import com.drawtogether.model.Room;
import com.drawtogether.repository.RoomRepository;

public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public Room createRoom(String name, int maxParticipants) {
        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId, name, maxParticipants);
        return roomRepository.save(room);
    }

    @Override
    public Optional<Room> getRoomById(String roomId) {
        return roomRepository.findById(roomId);
    }

    @Override
    public Collection<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public boolean joinRoom(String roomId, String userId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            boolean joined = room.addParticipant(userId);
            if (joined) {
                roomRepository.save(room);
            }
            return joined;
        }
        return false;
    }

    @Override
    public boolean leaveRoom(String roomId, String userId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            boolean left = room.removeParticipant(userId);
            if (left) {
                roomRepository.save(room);
                // Si la sala queda vacía, la eliminamos
                if (room.getCurrentParticipantsCount() == 0) {
                    roomRepository.deleteById(roomId);
                }
            }
            return left;
        }
        return false;
    }

    @Override
    public void addDrawEvent(String roomId, DrawEvent event) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            room.addDrawEvent(event);
            roomRepository.save(room);
        }
    }

    @Override
    public boolean deleteRoom(String roomId) {
        return roomRepository.deleteById(roomId);
    }
}