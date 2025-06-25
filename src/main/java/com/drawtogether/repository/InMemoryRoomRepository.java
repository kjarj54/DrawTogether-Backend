package com.drawtogether.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.drawtogether.model.Room;

public class InMemoryRoomRepository implements RoomRepository {
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    
    @Override
    public Room save(Room room) {
        rooms.put(room.getId(), room);
        return room;
    }

    @Override
    public Optional<Room> findById(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    @Override
    public Collection<Room> findAll() {
        return rooms.values();
    }

    @Override
    public boolean deleteById(String id) {
        return rooms.remove(id) != null;
    }

    @Override
    public boolean existsById(String id) {
        return rooms.containsKey(id);
    }
}
