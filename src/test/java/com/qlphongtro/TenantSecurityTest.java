package com.qlphongtro;

import com.qlphongtro.controller.TenantController;
import com.qlphongtro.security.CustomUserDetailsService;
import com.qlphongtro.security.SecurityConfig;
import com.qlphongtro.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class TenantSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void guestCannotViewTenantList() throws Exception {
        mockMvc.perform(get("/tenants"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void guestCannotReachStaffOrAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/staff/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "USER")
    void tenantRoleCannotViewTenantList() throws Exception {
        mockMvc.perform(get("/tenants"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanViewTenantList() throws Exception {
        mockMvc.perform(get("/tenants"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingTenantWithGetIsNotAllowed() throws Exception {
        mockMvc.perform(get("/tenants/delete/1"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postDeleteWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/tenants/delete/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteWithCsrf() throws Exception {
        mockMvc.perform(post("/tenants/delete/1").with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(tenantService).deleteTenant(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedUserCannotAccessUnlistedRoute() throws Exception {
        mockMvc.perform(get("/unlisted/internal-route"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void logoutCannotBeTriggeredWithGet() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().isForbidden());
    }
}
