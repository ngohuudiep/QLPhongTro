package com.qlphongtro;

import com.qlphongtro.controller.AuthController;
import com.qlphongtro.dto.ProfileDto;
import com.qlphongtro.dto.RegisterRequest;
import com.qlphongtro.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AuthControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = org.mockito.Mockito.mock(UserService.class);
        mockMvc = standaloneSetup(new AuthController(userService)).build();
    }

    @Test
    void sendOtpDoesNotExposeOtpInResponseOrModel() throws Exception {
        doNothing().when(userService).generateResetOtp("tenant@example.com");

        MvcResult result = mockMvc.perform(post("/forgot-password/send-otp")
                        .param("identifier", "tenant@example.com")
                        .param("otp", "654321"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("demoOtp"))
                .andExpect(model().attributeExists("successMessage"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertFalse(response.matches("(?s).*\\b\\d{6}\\b.*"));
        ProfileDto.ForgotPassword dto = (ProfileDto.ForgotPassword) result.getModelAndView()
                .getModel().get("forgotDto");
        assertNull(dto.getOtp());
        assertFalse(readForgotPasswordTemplate().contains("demoOtp"));
    }

    @Test
    void invalidResetDoesNotEchoSubmittedOtpBackToModel() throws Exception {
        when(userService.resetPasswordWithOtp("tenant@example.com", "123456", "Password123"))
                .thenReturn(false);

        MvcResult result = mockMvc.perform(post("/forgot-password/reset")
                        .param("identifier", "tenant@example.com")
                        .param("otp", "123456")
                        .param("newPassword", "Password123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("demoOtp"))
                .andReturn();

        ProfileDto.ForgotPassword dto = (ProfileDto.ForgotPassword) result.getModelAndView()
                .getModel().get("forgotDto");
        assertNull(dto.getOtp());
        assertFalse(result.getResponse().getContentAsString().contains("123456"));
    }

    @Test
    void registrationIgnoresInjectedRoleChoice() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "Tenant Example")
                        .param("username", "tenantexample")
                        .param("email", "tenant@example.com")
                        .param("phone", "0901234567")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .param("roleChoice", "ADMIN"))
                .andExpect(status().is3xxRedirection());

        org.mockito.ArgumentCaptor<RegisterRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(RegisterRequest.class);
        verify(userService).registerUser(requestCaptor.capture());
        assertEquals("tenantexample", requestCaptor.getValue().getUsername());
        assertFalse(Arrays.stream(RegisterRequest.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("roleChoice")));
    }

    private String readForgotPasswordTemplate() throws IOException {
        try (var template = getClass().getClassLoader()
                .getResourceAsStream("templates/auth/forgot-password.html")) {
            if (template == null) {
                throw new IOException("Forgot-password template was not found on the test classpath.");
            }
            return new String(template.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
