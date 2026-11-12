package com.qlphongtro;

import com.qlphongtro.entity.User;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.security.AuthenticationEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationEventsTest {

    private UserRepository userRepository;
    private AuthenticationEvents authenticationEvents;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authenticationEvents = new AuthenticationEvents(userRepository);
        user = new User();
        user.setUsername("tenant");
        when(userRepository.findByIdentifier("tenant")).thenReturn(Optional.of(user));
    }

    @Test
    void fifthBadCredentialAttemptLocksAccountForFifteenMinutes() {
        for (int attempt = 0; attempt < 5; attempt++) {
            authenticationEvents.onFailure(failedLoginEvent("tenant"));
        }

        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockoutUntil());
        assertTrue(user.getLockoutUntil().isAfter(LocalDateTime.now()));
        verify(userRepository, org.mockito.Mockito.times(5)).save(user);
    }

    @Test
    void successfulLoginResetsFailureCounterAndLockout() {
        user.setFailedLoginAttempts(5);
        user.setLockoutUntil(LocalDateTime.now().plusMinutes(15));
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("tenant")
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "credentials", userDetails.getAuthorities());
        when(userRepository.findByUsername("tenant")).thenReturn(Optional.of(user));

        authenticationEvents.onSuccess(new AuthenticationSuccessEvent(authentication));

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockoutUntil());
        verify(userRepository).save(user);
    }

    private AuthenticationFailureBadCredentialsEvent failedLoginEvent(String identifier) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(identifier, "wrong-password");
        return new AuthenticationFailureBadCredentialsEvent(
                authentication, new BadCredentialsException("Bad credentials"));
    }
}
