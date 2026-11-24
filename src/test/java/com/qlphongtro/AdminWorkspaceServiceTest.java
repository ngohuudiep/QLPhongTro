package com.qlphongtro;

import com.qlphongtro.dto.AdminAccountForm;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.BookingRequestRepository;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.MeterReadingRepository;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.RoomServiceRepository;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.repository.UserNotificationRepository;
import com.qlphongtro.service.AdminWorkspaceService;
import com.qlphongtro.service.RoomListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminWorkspaceServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private RoomListingRepository roomListingRepository;
    private RoomRepository roomRepository;
    private BuildingRepository buildingRepository;
    private ContractRepository contractRepository;
    private BookingRequestRepository bookingRequestRepository;
    private PasswordEncoder passwordEncoder;
    private RoomListingService roomListingService;
    private MeterReadingRepository meterReadingRepository;
    private RoomServiceRepository roomServiceRepository;
    private UserNotificationRepository userNotificationRepository;
    private SessionRegistry sessionRegistry;
    private AdminWorkspaceService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        roomListingRepository = mock(RoomListingRepository.class);
        roomRepository = mock(RoomRepository.class);
        buildingRepository = mock(BuildingRepository.class);
        contractRepository = mock(ContractRepository.class);
        bookingRequestRepository = mock(BookingRequestRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        roomListingService = mock(RoomListingService.class);
        meterReadingRepository = mock(MeterReadingRepository.class);
        roomServiceRepository = mock(RoomServiceRepository.class);
        userNotificationRepository = mock(UserNotificationRepository.class);
        sessionRegistry = mock(SessionRegistry.class);
        service = new AdminWorkspaceService(userRepository, roleRepository, roomListingRepository, roomRepository,
                buildingRepository, contractRepository, bookingRequestRepository, passwordEncoder, roomListingService,
                meterReadingRepository, roomServiceRepository, userNotificationRepository, sessionRegistry);
    }

    @Test
    void approvingPendingLandlordEnablesLandlordWorkflow() {
        User landlord = userWithRole(Role.LANDLORD, "PENDING");
        User admin = userWithRole(Role.ADMIN, "ACTIVE");
        when(userRepository.findById(7)).thenReturn(Optional.of(landlord));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        service.reviewLandlord(7, true, null, "admin");

        assertEquals("APPROVED", landlord.getAccountStatus());
        assertNotNull(landlord.getApprovedAt());
        assertSame(admin, landlord.getApprovedBy());
        verify(userRepository).save(landlord);
        verify(userNotificationRepository).save(any());
    }

    @Test
    void cannotReviewAlreadyProcessedLandlordRequest() {
        when(userRepository.findById(7)).thenReturn(Optional.of(userWithRole(Role.LANDLORD, "REJECTED")));

        assertThrows(IllegalStateException.class, () -> service.reviewLandlord(7, true, null, "admin"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectingPendingLandlordStoresReasonAndTimestamp() {
        User landlord = userWithRole(Role.LANDLORD, "PENDING_APPROVAL");
        User admin = userWithRole(Role.ADMIN, "ACTIVE");
        when(userRepository.findById(7)).thenReturn(Optional.of(landlord));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        service.reviewLandlord(7, false, "Thiếu giấy tờ.", "admin");

        assertEquals("REJECTED", landlord.getAccountStatus());
        assertEquals("Thiếu giấy tờ.", landlord.getRejectionReason());
        assertNotNull(landlord.getRejectedAt());
        verify(userRepository).save(landlord);
        verify(userNotificationRepository).save(any());
    }

    @Test
    void rejectingLandlordExpiresItsActiveSessions() {
        User landlord = userWithRole(Role.LANDLORD, "PENDING_APPROVAL");
        User admin = userWithRole(Role.ADMIN, "ACTIVE");
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("test-user").password("encoded").authorities("ROLE_LANDLORD").build();
        SessionInformation activeSession = new SessionInformation(principal, "session-1", new Date());
        when(userRepository.findById(7)).thenReturn(Optional.of(landlord));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessionRegistry.getAllSessions(principal, false)).thenReturn(List.of(activeSession));

        service.reviewLandlord(7, false, "Không đủ điều kiện.", "admin");

        assertTrue(activeSession.isExpired());
    }

    @Test
    void rejectingPendingLandlordRequiresReason() {
        User landlord = userWithRole(Role.LANDLORD, "PENDING_APPROVAL");
        User admin = userWithRole(Role.ADMIN, "ACTIVE");
        when(userRepository.findById(7)).thenReturn(Optional.of(landlord));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class,
                () -> service.reviewLandlord(7, false, " ", "admin"));

        verify(userRepository, never()).save(landlord);
    }

    @Test
    void registrationFilterIncludesCurrentAndLegacyPendingStatuses() {
        User current = userWithRole(Role.LANDLORD, "PENDING_APPROVAL");
        User legacy = userWithRole(Role.LANDLORD, "PENDING");
        User approved = userWithRole(Role.LANDLORD, "APPROVED");
        when(userRepository.searchAccountsByRole(Role.LANDLORD, null))
                .thenReturn(List.of(current, legacy, approved));

        assertEquals(List.of(current, legacy), service.getLandlordRegistrations("PENDING"));
        assertEquals(List.of(approved), service.getLandlordRegistrations("APPROVED"));
    }

    @Test
    void accountManagementCannotChangeRoleThroughCategoryIdMismatch() {
        when(userRepository.findById(9)).thenReturn(Optional.of(userWithRole(Role.ADMIN, "ACTIVE")));

        assertThrows(AccessDeniedException.class, () -> service.getAccount(9, Role.TENANT));
    }

    @Test
    void deletingAccountWithOwnedBuildingsIsRejectedToPreserveData() {
        when(userRepository.findById(12)).thenReturn(Optional.of(userWithRole(Role.LANDLORD, "APPROVED")));
        when(buildingRepository.existsByOwner_UserId(12)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteAccount(12, Role.LANDLORD, "admin"));

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void adminCreatedAccountsAreApprovedAndPasswordsAreEncoded() {
        Role tenantRole = new Role(3, Role.TENANT);
        when(userRepository.existsByUsername("tenant01")).thenReturn(false);
        when(userRepository.existsByEmail("tenant@example.test")).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(false);
        when(roleRepository.findByRoleNameIgnoreCase(Role.TENANT)).thenReturn(Optional.of(tenantRole));
        when(passwordEncoder.encode("Passw0rd")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminAccountForm form = new AdminAccountForm();
        form.setUsername("tenant01");
        form.setFullName("Tenant One");
        form.setEmail("tenant@example.test");
        form.setPhone("0901234567");
        form.setPassword("Passw0rd");

        User saved = service.saveAccount(form, Role.TENANT);

        assertEquals("bcrypt-hash", saved.getPassword());
        assertEquals("APPROVED", saved.getAccountStatus());
        assertTrue(saved.isEnabled());
        assertNotNull(saved.getRegisteredAt());
        assertEquals(Role.TENANT, saved.getRole().getRoleName());
    }

    private static User userWithRole(String roleName, String status) {
        User user = new User();
        user.setUserId(7);
        user.setUsername("test-user");
        user.setRole(new Role(1, roleName));
        user.setAccountStatus(status);
        return user;
    }
}
