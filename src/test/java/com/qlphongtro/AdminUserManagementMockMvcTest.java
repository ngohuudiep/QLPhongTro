package com.qlphongtro;

import com.qlphongtro.controller.AdminUserController;
import com.qlphongtro.security.CustomUserDetailsService;
import com.qlphongtro.security.SecurityConfig;
import com.qlphongtro.service.UserService;
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

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class AdminUserManagementMockMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;
    @MockBean CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenAccountManagement() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotOpenAccountManagement() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void tenantCannotOpenAccountManagement() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }
}
