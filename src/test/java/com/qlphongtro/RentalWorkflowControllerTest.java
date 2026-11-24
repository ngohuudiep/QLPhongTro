package com.qlphongtro;

import com.qlphongtro.controller.RentalWorkflowController;
import com.qlphongtro.entity.BookingRequest;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.service.RentalWorkflowService;
import com.qlphongtro.service.RoomListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RentalWorkflowControllerTest {

    private RentalWorkflowService rentalWorkflowService;
    private RoomListingService roomListingService;
    private RentalWorkflowController controller;

    @BeforeEach
    void setUp() {
        rentalWorkflowService = mock(RentalWorkflowService.class);
        roomListingService = mock(RoomListingService.class);
        controller = new RentalWorkflowController(rentalWorkflowService, roomListingService);
    }

    @Test
    void roomSpecificRequestRouteOpensTheModalForThatListing() {
        RoomListing room = new RoomListing();
        room.setListingId(7);
        room.setStatus("TRONG");
        when(roomListingService.getById(7)).thenReturn(room);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.tenantRequestForm(7, redirectAttributes);

        assertEquals("redirect:/rooms/list", result);
        assertEquals(7, redirectAttributes.getFlashAttributes().get("openRentalModalListingId"));
    }

    @Test
    void roomSpecificRequestRouteDoesNotOpenModalForUnavailableRoom() {
        RoomListing room = new RoomListing();
        room.setListingId(7);
        room.setStatus("DA_THUE");
        when(roomListingService.getById(7)).thenReturn(room);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.tenantRequestForm(7, redirectAttributes);

        assertEquals("redirect:/rooms/7", result);
        assertEquals("Phòng này không còn trống.",
                redirectAttributes.getFlashAttributes().get("errorMessage"));
        assertFalse(redirectAttributes.getFlashAttributes().containsKey("openRentalModalListingId"));
    }

    @Test
    void successfulRequestRedirectsToRoomListWithSuccessNotice() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        var authentication = new UsernamePasswordAuthenticationToken("nguoithue", "N/A");

        String result = controller.tenantCreateRequest(
                "1", "2", "2026-12-01", "Ghi chú", authentication, redirectAttributes);

        assertEquals("redirect:/tenant/rental/requests", result);
        assertEquals(
                "Đã gửi yêu cầu thuê phòng thành công! Yêu cầu đã được gửi đến chủ trọ. Vui lòng chờ chủ trọ xác nhận.",
                redirectAttributes.getFlashAttributes().get("successMessage"));
        verify(rentalWorkflowService).createRequest(
                "nguoithue", 1, (short) 2, LocalDate.parse("2026-12-01"), "Ghi chú");
    }

    @Test
    void databaseFailureKeepsTheModalAndEnteredValuesWithoutSuccessNotice() {
        doThrow(new DataIntegrityViolationException("database failure"))
                .when(rentalWorkflowService)
                .createRequest(eq("nguoithue"), eq(1), anyShort(), any(LocalDate.class), eq("Ghi chú"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        var authentication = new UsernamePasswordAuthenticationToken("nguoithue", "N/A");

        String result = controller.tenantCreateRequest(
                "1", "2", "2026-12-01", "Ghi chú", authentication, redirectAttributes);

        assertEquals("redirect:/rooms/list", result);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
        assertEquals(1, redirectAttributes.getFlashAttributes().get("openRentalModalListingId"));
        assertEquals("2026-12-01", redirectAttributes.getFlashAttributes().get("submittedExpectedMoveInDate"));
        assertEquals("2", redirectAttributes.getFlashAttributes().get("submittedExpectedOccupants"));
        assertEquals("Ghi chú", redirectAttributes.getFlashAttributes().get("submittedRentalNote"));
        assertFalse(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    void depositConfirmationRequestRedirectsWithPendingNotice() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        var authentication = new UsernamePasswordAuthenticationToken("nguoithue", "N/A");

        String result = controller.createDeposit(5, authentication, redirectAttributes);

        assertEquals("redirect:/tenant/rental/contracts/5", result);
        assertEquals("Đã gửi yêu cầu xác nhận tiền đặt cọc thành công. Vui lòng chờ chủ trọ kiểm tra.",
                redirectAttributes.getFlashAttributes().get("successMessage"));
        verify(rentalWorkflowService).createDepositConfirmationRequest(5, "nguoithue");
    }

    @Test
    void depositRequestDatabaseFailureDoesNotShowSuccess() {
        doThrow(new DataIntegrityViolationException("database failure"))
                .when(rentalWorkflowService).createDepositConfirmationRequest(5, "nguoithue");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        var authentication = new UsernamePasswordAuthenticationToken("nguoithue", "N/A");

        String result = controller.createDeposit(5, authentication, redirectAttributes);

        assertEquals("redirect:/tenant/rental/contracts/5", result);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
        assertFalse(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }
}
