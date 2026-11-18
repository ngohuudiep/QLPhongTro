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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LastActiveAdminProtectionTest {
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
    void adminCannotDemoteTheLastActiveAdminEvenWhenAnotherAdminRequestsIt() {
        authenticateAs("admin-operator");
        User lastAdmin = admin(20, "admin-target");
        when(userRepository.findById(20)).thenReturn(Optional.of(lastAdmin));
        when(roleRepository.findByRoleName("STAFF")).thenReturn(Optional.of(new Role(3, "STAFF")));
        when(userRepository.findActiveAdministratorsForUpdate()).thenReturn(List.of(lastAdmin));

        assertThrows(AccessDeniedException.class,
                () -> userService.saveManagedUser(form(20, "admin-target", "STAFF")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void adminCannotLockTheLastActiveAdmin() {
        authenticateAs("admin-operator");
        User lastAdmin = admin(20, "admin-target");
        when(userRepository.findById(20)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.findActiveAdministratorsForUpdate()).thenReturn(List.of(lastAdmin));

        assertThrows(AccessDeniedException.class, () -> userService.setAccountEnabled(20, false));

        verify(userRepository, never()).save(any(User.class));
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "", List.of()));
    }

    private User admin(Integer id, String username) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setFullName("Admin");
        user.setEmail(username + "@example.com");
        user.setPhone("0900000000");
        user.setRole(new Role(1, "ADMIN"));
        user.setAccountEnabled(true);
        return user;
    }

    private AdminUserForm form(Integer userId, String username, String roleName) {
        AdminUserForm form = new AdminUserForm();
        form.setUserId(userId);
        form.setUsername(username);
        form.setFullName("Admin");
        form.setEmail(username + "@example.com");
        form.setPhone("0900000000");
        form.setRoleName(roleName);
        return form;
    }
}
