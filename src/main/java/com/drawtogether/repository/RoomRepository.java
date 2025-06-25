package com.drawtogether.repository;

import java.util.Collection;
import java.util.Optional;

import com.drawtogether.model.Room;

public interface RoomRepository {
    Room save(Room room);
    Optional<Room> findById(String id);
    Collection<Room> findAll();
    boolean deleteById(String id);
    boolean existsById(String id);
}
