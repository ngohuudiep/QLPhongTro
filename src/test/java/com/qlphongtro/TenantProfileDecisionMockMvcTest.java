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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class TenantProfileDecisionMockMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean TenantService tenantService;
    @MockBean CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanReadTenantListButCannotOpenCreateForm() throws Exception {
        mockMvc.perform(get("/tenants")).andExpect(status().isOk());
        mockMvc.perform(get("/tenants/new")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotCreateTenantThroughDirectPost() throws Exception {
        mockMvc.perform(post("/tenants/save").with(csrf())
                        .param("fullName", "Test Tenant")
                        .param("idNumber", "123456789")
                        .param("phone", "0912345678"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenTenantCreateForm() throws Exception {
        mockMvc.perform(get("/tenants/new")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void tenantCannotUseTenantManagementOrLinkRoute() throws Exception {
        mockMvc.perform(get("/tenants")).andExpect(status().isForbidden());
        mockMvc.perform(post("/tenants/8/link-user").with(csrf()).param("userId", "5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanSubmitAccountLinkThroughManagementRoute() throws Exception {
        mockMvc.perform(post("/tenants/8/link-user").with(csrf()).param("userId", "5"))
                .andExpect(status().is3xxRedirection());
        verify(tenantService).linkUserAccount(8, 5);
    }
}
