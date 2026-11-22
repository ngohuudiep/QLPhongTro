package com.qlphongtro;

import com.qlphongtro.service.RentalWorkflowService;
import com.qlphongtro.service.RoomListingService;
import com.qlphongtro.controller.HomeController;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeControllerTest {

    private final RoomListingService roomListingService = mock(RoomListingService.class);
    private final HomeController controller = new HomeController(
            roomListingService, mock(RentalWorkflowService.class));

    @Test
    void missingRoomRedirectsWithNotFoundMessage() {
        when(roomListingService.getById(999)).thenThrow(new IllegalArgumentException("Không tìm thấy phòng."));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.roomDetail(999, new ExtendedModelMap(), redirectAttributes);

        assertEquals("redirect:/rooms/list", result);
        assertEquals("Không tìm thấy phòng.", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void databaseFailureIsNotReportedAsMissingRoom() {
        when(roomListingService.getById(7))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> controller.roomDetail(7, new ExtendedModelMap(), new RedirectAttributesModelMap()));
    }
}
