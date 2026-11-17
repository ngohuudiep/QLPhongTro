package com.qlphongtro;

import com.qlphongtro.security.CustomUserDetailsService;
import com.qlphongtro.security.SecurityConfig;
import com.qlphongtro.service.RoomListingService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.qlphongtro.controller.HomeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class ProtectedRoutesMockMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean RoomListingService roomListingService;
    @MockBean CustomUserDetailsService userDetailsService;

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/dashboard", "/staff/dashboard", "/tenants", "/profile",
            "/rooms/new", "/buildings", "/contracts", "/invoices",
            "/meter-readings", "/incidents", "/assets"
    })
    void anonymousIsSentToLoginForProtectedRoute(String route) throws Exception {
        mockMvc.perform(get(route))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
