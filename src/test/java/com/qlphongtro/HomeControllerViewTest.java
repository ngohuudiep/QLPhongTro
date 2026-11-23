package com.qlphongtro;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.User;
import com.qlphongtro.controller.HomeController;
import com.qlphongtro.service.RentalWorkflowService;
import com.qlphongtro.service.RoomListingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerViewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomListingService roomListingService;

    @MockBean
    private RentalWorkflowService rentalWorkflowService;

    @MockBean
    private com.qlphongtro.service.ReviewService reviewService;

    @Test
    void publicRoomPageDoesNotRenderTenantPersonalData() throws Exception {
        RoomListing room = new RoomListing();
        room.setListingId(1);
        room.setRoomCode("P101");
        room.setTitle("P101");
        room.setStatus("DA_THUE");
        room.setArea(new BigDecimal("25"));
        room.setPrice(new BigDecimal("3000000"));
        room.setCurrentTenant("Private Tenant Name");
        room.setTenantPhone("0912345678");
        when(roomListingService.getById(1)).thenReturn(room);
        when(roomListingService.getAvailableRooms()).thenReturn(List.of());

        mockMvc.perform(get("/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Private Tenant Name"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("0912345678"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("1900 6868"))));
    }

    @Test
    void publicRoomPageUsesTheManagedBuildingAndOwnerContact() throws Exception {
        Building building = new Building();
        building.setAddress("12 Đường Thật");
        building.setCity("Hà Nội");
        building.setContactPhone("0901234567");
        Room managedRoom = new Room();
        managedRoom.setBuilding(building);
        User owner = new User();
        owner.setFullName("Chủ trọ thật");

        RoomListing room = new RoomListing();
        room.setListingId(2);
        room.setRoomCode("P102");
        room.setTitle("P102");
        room.setStatus("TRONG");
        room.setArea(new BigDecimal("25"));
        room.setPrice(new BigDecimal("3000000"));
        room.setManagedRoom(managedRoom);
        room.setCreatedBy(owner);
        when(roomListingService.getById(2)).thenReturn(room);
        when(roomListingService.getAvailableRooms()).thenReturn(List.of());

        mockMvc.perform(get("/rooms/2"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("12 Đường Thật, Hà Nội")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0901234567")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chủ trọ thật")));
    }
}
