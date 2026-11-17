package com.qlphongtro;

import com.qlphongtro.dto.AdminUserForm;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.OtpSender;
import com.qlphongtro.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminUserServicePolicyTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserServiceImpl userService = new UserServiceImpl(
            userRepository, roleRepository, passwordEncoder, mock(OtpSender.class));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void editingExistingUserCanPreserveUserRole() {
        authenticateAs("admin");
        User existing = user("tenant", "USER");
        when(userRepository.findById(12)).thenReturn(Optional.of(existing));
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(existing.getRole()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.saveManagedUser(form(12, "tenant", "USER"));

        assertEquals("USER", saved.getRole().getRoleName());
    }

    @Test
    void adminCannotCreateUserRoleFromAccountManagement() {
        authenticateAs("admin");

        assertThrows(IllegalArgumentException.class,
                () -> userService.saveManagedUser(form(null, "tenant-new", "USER")));
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "", java.util.List.of()));
    }

    private User user(String username, String roleName) {
        User user = new User();
        user.setUserId(12);
        user.setUsername(username);
        user.setFullName("Tenant Example");
        user.setEmail("tenant@example.com");
        user.setPhone("0900000000");
        user.setRole(new Role(2, roleName));
        return user;
    }

    private AdminUserForm form(Integer userId, String username, String roleName) {
        AdminUserForm form = new AdminUserForm();
        form.setUserId(userId);
        form.setUsername(username);
        form.setFullName("Tenant Example");
        form.setEmail("tenant@example.com");
        form.setPhone("0900000000");
        form.setRoleName(roleName);
        return form;
    }
}
