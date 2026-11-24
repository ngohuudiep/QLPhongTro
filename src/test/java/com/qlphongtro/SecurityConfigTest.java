package com.qlphongtro;

import com.qlphongtro.security.CustomUserDetailsService;
import com.qlphongtro.security.SecurityConfig;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final SecurityConfig securityConfig =
            new SecurityConfig(mock(CustomUserDetailsService.class), "test-only-remember-me-key");

    @Test
    void tenantRoleAliasesRedirectToTenantDashboard() throws ServletException, IOException {
        assertRedirect("ROLE_TENANT", "/tenant/dashboard");
        assertRedirect("ROLE_NGUOITHUE", "/tenant/dashboard");
        assertRedirect("ROLE_NGUOI_THUE", "/tenant/dashboard");
    }

    @Test
    void landlordAndAdminRedirectToTheirOwnDashboards() throws ServletException, IOException {
        assertRedirect("ROLE_LANDLORD", "/landlord/dashboard");
        assertRedirect("ROLE_ADMIN", "/admin/dashboard");
    }

    @Test
    void unknownRoleDoesNotRedirectToTenantDashboard() throws ServletException, IOException {
        assertRedirect("ROLE_UNRECOGNIZED", "/access-denied");
    }

    @Test
    void loginFailureHandlerPreservesApprovalStatusMessage() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        DisabledException failure = new DisabledException("Tài khoản chủ trọ đang chờ Admin phê duyệt.");

        securityConfig.authenticationFailureHandler().onAuthenticationFailure(request, response, failure);

        assertEquals("/login?error=true", response.getRedirectedUrl());
        assertEquals(failure.getMessage(), request.getSession().getAttribute("loginErrorMessage"));
    }

    private void assertRedirect(String authority, String expectedUrl) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var authentication = new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority(authority)));

        securityConfig.roleBasedAuthenticationSuccessHandler()
                .onAuthenticationSuccess(request, response, authentication);

        assertEquals(expectedUrl, response.getRedirectedUrl());
    }
}
