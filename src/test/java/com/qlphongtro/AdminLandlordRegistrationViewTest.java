package com.qlphongtro;

import com.qlphongtro.controller.AdminWorkspaceController;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.User;
import com.qlphongtro.service.AdminWorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminWorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminLandlordRegistrationViewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminWorkspaceService adminWorkspaceService;

    @Test
    void registrationListRendersAddressFilterAndReasonForm() throws Exception {
        User landlord = landlord("PENDING_APPROVAL");
        when(adminWorkspaceService.getLandlordRegistrations("PENDING")).thenReturn(List.of(landlord));
        when(adminWorkspaceService.countApprovedLandlords()).thenReturn(2L);

        mockMvc.perform(get("/admin/landlords/registrations").principal(adminPrincipal()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("12 Đường Mẫu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Từ chối")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("reason-17")));
    }

    @Test
    void registrationDetailRendersAuditAndRejectionReason() throws Exception {
        User landlord = landlord("REJECTED");
        landlord.setRejectionReason("Thiếu giấy tờ xác minh.");
        landlord.setRejectedAt(LocalDateTime.now());
        when(adminWorkspaceService.getAccount(17, Role.LANDLORD)).thenReturn(landlord);

        mockMvc.perform(get("/admin/landlords/registrations/17").principal(adminPrincipal()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thiếu giấy tờ xác minh.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ngày từ chối")));
    }

    private static User landlord(String status) {
        User landlord = new User();
        landlord.setUserId(17);
        landlord.setUsername("newlandlord");
        landlord.setFullName("Chủ trọ mới");
        landlord.setEmail("owner@example.test");
        landlord.setPhone("0901234567");
        landlord.setContactAddress("12 Đường Mẫu");
        landlord.setRegisteredAt(LocalDateTime.now());
        landlord.setAccountStatus(status);
        landlord.setRole(new Role(2, Role.LANDLORD));
        return landlord;
    }

    private static UsernamePasswordAuthenticationToken adminPrincipal() {
        return UsernamePasswordAuthenticationToken.authenticated("admin", "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
