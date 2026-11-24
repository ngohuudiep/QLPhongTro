package com.qlphongtro;

import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Test
    void disabledAccountCannotAuthenticate() {
        User user = user(Role.TENANT, "APPROVED");
        user.setEnabled(false);
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> new CustomUserDetailsService(repository).loadUserByUsername("tenant"));
    }

    @Test
    void pendingLandlordCannotAuthenticate() {
        User user = user(Role.LANDLORD, "PENDING");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> new CustomUserDetailsService(repository).loadUserByUsername("tenant"));
    }

    @Test
    void pendingApprovalLandlordGetsSpecificLoginMessage() {
        User user = user(Role.LANDLORD, "PENDING_APPROVAL");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        DisabledException failure = assertThrows(DisabledException.class,
                () -> new CustomUserDetailsService(repository).loadUserByUsername("tenant"));

        assertEquals("Tài khoản chủ trọ đang chờ Admin phê duyệt.", failure.getMessage());
    }

    @Test
    void rejectedLandlordGetsRejectionReasonInLoginFailure() {
        User user = user(Role.LANDLORD, "REJECTED");
        user.setRejectionReason("Thiếu giấy tờ xác minh.");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        DisabledException failure = assertThrows(DisabledException.class,
                () -> new CustomUserDetailsService(repository).loadUserByUsername("tenant"));

        assertTrue(failure.getMessage().contains("Thiếu giấy tờ xác minh."));
    }

    @Test
    void enabledTenantCanAuthenticate() {
        User user = user(Role.TENANT, "APPROVED");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        UserDetails details = new CustomUserDetailsService(repository).loadUserByUsername("tenant");

        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> "ROLE_TENANT".equals(a.getAuthority())));
    }

    @Test
    void activeTenantCanAuthenticateWithItsExistingDatabaseStatus() {
        User user = user(Role.TENANT, "ACTIVE");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        UserDetails details = new CustomUserDetailsService(repository).loadUserByUsername("tenant");

        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> "ROLE_TENANT".equals(a.getAuthority())));
    }

    @Test
    void pendingTenantCannotAuthenticate() {
        User user = user(Role.TENANT, "PENDING");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class,
                () -> new CustomUserDetailsService(repository).loadUserByUsername("tenant"));
    }

    @Test
    void legacyVietnameseTenantRoleAuthenticatesAsTenant() {
        User user = user("ROLE_NGUOITHUE", "APPROVED");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        UserDetails details = new CustomUserDetailsService(repository).loadUserByUsername("tenant");

        assertTrue(details.getAuthorities().stream().anyMatch(a -> "ROLE_TENANT".equals(a.getAuthority())));
    }

    @Test
    void landlordAndAdminAuthoritiesRemainSeparate() {
        User landlord = user("CHU TRO", "APPROVED");
        User admin = user(Role.ADMIN, "APPROVED");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("landlord")).thenReturn(Optional.of(landlord));
        when(repository.findByIdentifier("admin")).thenReturn(Optional.of(admin));
        CustomUserDetailsService service = new CustomUserDetailsService(repository);

        assertTrue(service.loadUserByUsername("landlord").getAuthorities().stream()
                .anyMatch(a -> "ROLE_LANDLORD".equals(a.getAuthority())));
        assertTrue(service.loadUserByUsername("admin").getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        assertFalse(service.loadUserByUsername("landlord").getAuthorities().stream()
                .anyMatch(a -> "ROLE_TENANT".equals(a.getAuthority())));
        assertFalse(service.loadUserByUsername("admin").getAuthorities().stream()
                .anyMatch(a -> "ROLE_TENANT".equals(a.getAuthority())));
    }

    @Test
    void unknownRoleDoesNotFallBackToTenant() {
        User user = user("UNRECOGNIZED_ROLE", "APPROVED");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdentifier("tenant")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class,
                () -> new CustomUserDetailsService(repository).loadUserByUsername("tenant"));
    }

    private static User user(String roleName, String status) {
        User user = new User();
        user.setUsername("tenant");
        user.setPassword("encoded-password");
        user.setRole(new Role(1, roleName));
        user.setAccountStatus(status);
        user.setEnabled(true);
        return user;
    }
}
