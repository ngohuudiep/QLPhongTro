package com.qlphongtro;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.security.CustomUserDetailsService;
import com.qlphongtro.security.SecurityConfig;
import com.qlphongtro.service.RoomListingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.qlphongtro.controller.HomeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class PublicRoomViewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomListingService roomListingService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private com.qlphongtro.service.RentalWorkflowService rentalWorkflowService;

    @MockBean
    private com.qlphongtro.service.ReviewService reviewService;

    @Test
    void rentedRoomDetailDoesNotExposeTenantIdentityOrLeaseDates() throws Exception {
        RoomListing room = new RoomListing();
        room.setListingId(7);
        room.setRoomCode("P107");
        room.setTitle("Public room title");
        room.setDescription("Public room description");
        room.setPrice(new BigDecimal("3500000"));
        room.setArea(new BigDecimal("25"));
        room.setStatus("DA_THUE");
        room.setCurrentTenant("UNIQUE_PRIVATE_TENANT_9f7");
        room.setTenantPhone("0912345678");
        room.setLeaseStartDate(LocalDate.of(2030, 1, 2));
        room.setLeaseEndDate(LocalDate.of(2031, 2, 3));

        when(roomListingService.getById(7)).thenReturn(room);
        when(roomListingService.getAvailableRooms()).thenReturn(List.of());

        mockMvc.perform(get("/rooms/7"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("P107")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("UNIQUE_PRIVATE_TENANT_9f7"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("0912345678"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("2030-01-02"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("2031-02-03"))));
    }

    @Test
    void guestCannotReachRoomManagementForm() throws Exception {
        mockMvc.perform(get("/rooms/new"))
                .andExpect(status().is3xxRedirection());
    }
}
