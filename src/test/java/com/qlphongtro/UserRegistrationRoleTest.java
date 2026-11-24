package com.qlphongtro;

import com.qlphongtro.dto.RegisterRequest;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRegistrationRoleTest {

    @Test
    void publicRegistrationCannotCreateAdmin() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(users, roles, encoder);
        RegisterRequest request = requestFor("ADMIN");
        when(users.existsByUsername(any())).thenReturn(false);
        when(users.existsByEmail(any())).thenReturn(false);
        when(users.existsByPhone(any())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.registerUser(request));

        verify(roles, never()).save(any(Role.class));
        verify(users, never()).save(any(User.class));
    }

    @Test
    void publicLandlordRegistrationStartsPendingApproval() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(users, roles, encoder);
        when(users.existsByUsername(any())).thenReturn(false);
        when(users.existsByEmail(any())).thenReturn(false);
        when(users.existsByPhone(any())).thenReturn(false);
        when(roles.findByRoleNameIgnoreCase(Role.LANDLORD)).thenReturn(Optional.of(new Role(2, Role.LANDLORD)));
        when(encoder.encode("Passw0rd")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.registerUser(requestFor(Role.LANDLORD));

        assertEquals("PENDING_APPROVAL", created.getAccountStatus());
        assertTrue(created.isEnabled());
        assertNotNull(created.getRegisteredAt());
        assertEquals("encoded", created.getPassword());
    }

    private static RegisterRequest requestFor(String role) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Demo User");
        request.setUsername("demouser");
        request.setEmail("demo@example.test");
        request.setPhone("0901234567");
        request.setPassword("Passw0rd");
        request.setConfirmPassword("Passw0rd");
        request.setRoleChoice(role);
        return request;
    }
}
