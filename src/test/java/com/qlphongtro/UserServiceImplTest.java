package com.qlphongtro;

import com.qlphongtro.entity.User;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.OtpSender;
import com.qlphongtro.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private static final String IDENTIFIER = "tenant@example.com";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private OtpSender otpSender;
    private UserServiceImpl userService;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        otpSender = mock(OtpSender.class);
        userService = new UserServiceImpl(userRepository, roleRepository, passwordEncoder, otpSender);

        user = new User();
        user.setEmail(IDENTIFIER);
        user.setUsername("tenant");
        user.setPassword(passwordEncoder.encode("OldPassword123"));
        when(userRepository.findByIdentifierForUpdate(IDENTIFIER)).thenReturn(Optional.of(user));
    }

    @Test
    void generatedOtpIsHashedAndSentOnlyThroughOtpSender() {
        userService.generateResetOtp(IDENTIFIER);

        String storedHash = user.getResetOtpHash();
        assertNotNull(storedHash);
        assertTrue(storedHash.startsWith("$2"));
        assertEquals(1, user.getResetOtpRequestCount());
        assertTrue(user.getResetOtpExpiry().isAfter(LocalDateTime.now()));
        org.mockito.ArgumentCaptor<String> otpCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(otpSender).send(org.mockito.ArgumentMatchers.eq(IDENTIFIER), otpCaptor.capture());
        assertTrue(otpCaptor.getValue().matches("\\d{6}"));
        assertTrue(passwordEncoder.matches(otpCaptor.getValue(), storedHash));
        assertNotEquals(otpCaptor.getValue(), storedHash);
    }

    @Test
    void requestLimitAllowsOnlyThreeOtpEmailsPerHour() {
        for (int request = 0; request < 4; request++) {
            userService.generateResetOtp(IDENTIFIER);
        }

        assertEquals(3, user.getResetOtpRequestCount());
        verify(otpSender, org.mockito.Mockito.times(3)).send(org.mockito.ArgumentMatchers.eq(IDENTIFIER), any());
    }

    @Test
    void fifthIncorrectOtpAttemptInvalidatesToken() {
        user.setResetOtpHash(passwordEncoder.encode("123456"));
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(userService.resetPasswordWithOtp(IDENTIFIER, "654321", "NewPassword123"));
        }

        assertNull(user.getResetOtpHash());
        assertNull(user.getResetOtpExpiry());
        assertEquals(0, user.getResetOtpFailedAttempts());
        assertFalse(userService.resetPasswordWithOtp(IDENTIFIER, "123456", "NewPassword123"));
    }

    @Test
    void successfulResetConsumesOtpAndCannotBeReused() {
        user.setResetOtpHash(passwordEncoder.encode("123456"));
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));

        assertTrue(userService.resetPasswordWithOtp(IDENTIFIER, "123456", "NewPassword123"));

        assertTrue(passwordEncoder.matches("NewPassword123", user.getPassword()));
        assertNull(user.getResetOtpHash());
        assertNull(user.getResetOtpExpiry());
        assertFalse(userService.resetPasswordWithOtp(IDENTIFIER, "123456", "AnotherPassword123"));
    }

    @Test
    void unknownAccountDoesNotSendEmail() {
        when(userRepository.findByIdentifierForUpdate("unknown@example.com")).thenReturn(Optional.empty());

        userService.generateResetOtp("unknown@example.com");

        verify(otpSender, never()).send(any(), any());
    }
}
