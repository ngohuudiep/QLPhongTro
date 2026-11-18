package com.qlphongtro;

import com.qlphongtro.controller.ContractController;
import com.qlphongtro.security.CustomUserDetailsService;
import com.qlphongtro.security.SecurityConfig;
import com.qlphongtro.service.ContractService;
import com.qlphongtro.service.RoomService;
import com.qlphongtro.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class ContractDeleteAuthorizationMockMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean ContractService contractService;
    @MockBean RoomService roomService;
    @MockBean TenantService tenantService;
    @MockBean CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotDeleteContract() throws Exception {
        mockMvc.perform(get("/contracts/delete/71")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotHardDeleteContract() throws Exception {
        mockMvc.perform(get("/contracts/delete/71")).andExpect(status().isForbidden());
    }
}
