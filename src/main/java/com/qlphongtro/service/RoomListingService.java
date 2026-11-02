package com.qlphongtro.service;

import com.qlphongtro.entity.RoomListing;

import java.math.BigDecimal;
import java.util.List;

public interface RoomListingService {

    List<RoomListing> getAllRooms();

    List<RoomListing> getAvailableRooms();

    List<RoomListing> getAvailableRoomsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    List<RoomListing> searchRooms(String keyword);

    List<RoomListing> filterRooms(String keyword, String status, String priceRange, Integer floorNo, String sortBy);

    RoomListing getById(Integer id);

    long countAvailableRooms();

    RoomListing save(RoomListing roomListing);

    void delete(Integer id);

    boolean isRoomCodeTaken(String roomCode, Integer excludeId);

    void seedDemoRoomsIfEmpty();
}
