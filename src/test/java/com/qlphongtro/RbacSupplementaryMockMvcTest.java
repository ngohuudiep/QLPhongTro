package com.qlphongtro;

import com.qlphongtro.controller.AdminRoomController;
import com.qlphongtro.controller.ContractController;
import com.qlphongtro.controller.InvoiceController;
import com.qlphongtro.security.CustomUserDetailsService;
import com.qlphongtro.security.SecurityConfig;
import com.qlphongtro.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ContractController.class, InvoiceController.class, AdminRoomController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class RbacSupplementaryMockMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean ContractService contractService;
    @MockBean InvoiceService invoiceService;
    @MockBean PaymentService paymentService;
    @MockBean RoomService roomService;
    @MockBean TenantService tenantService;
    @MockBean RoomListingService roomListingService;
    @MockBean CustomUserDetailsService userDetailsService;
    @MockBean com.qlphongtro.service.AdminWorkspaceService adminWorkspaceService;
    @MockBean com.qlphongtro.repository.BuildingRepository buildingRepository;
    @MockBean com.qlphongtro.repository.UserRepository userRepository;

    @Test
    @WithMockUser(username = "staff-x", roles = "STAFF")
    void staffXReceivesNotFoundForContractInBuildingY() throws Exception {
        when(contractService.getContractById(905)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/contracts/edit/905")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotReachAdminOnlyRoomListingOrInvoiceDeletionRoutes() throws Exception {
        mockMvc.perform(get("/admin/rooms")).andExpect(status().isForbidden());
        mockMvc.perform(post("/invoices/delete/22").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanReachRoomListingAdministration() throws Exception {
        mockMvc.perform(get("/admin/rooms")).andExpect(status().isOk());
    }
}
