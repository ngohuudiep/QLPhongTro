package com.qlphongtro;

import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.service.impl.RentalWorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RentalWorkflowServiceImplTest {

    private RentalWorkflowServiceImpl service;
    private UserRepository userRepository;
    private RoomListingRepository roomListingRepository;
    private RoomRepository roomRepository;
    private BookingRequestRepository bookingRequestRepository;
    private ContractRepository contractRepository;
    private ContractTenantRepository contractTenantRepository;
    private PaymentRepository paymentRepository;
    private DepositConfirmationRequestRepository depositConfirmationRequestRepository;
    private TenantRepository tenantRepository;
    private UserNotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        tenantRepository = Mockito.mock(TenantRepository.class);
        roomListingRepository = Mockito.mock(RoomListingRepository.class);
        roomRepository = Mockito.mock(RoomRepository.class);
        bookingRequestRepository = Mockito.mock(BookingRequestRepository.class);
        contractRepository = Mockito.mock(ContractRepository.class);
        contractTenantRepository = Mockito.mock(ContractTenantRepository.class);
        paymentRepository = Mockito.mock(PaymentRepository.class);
        depositConfirmationRequestRepository = Mockito.mock(DepositConfirmationRequestRepository.class);
        notificationRepository = Mockito.mock(UserNotificationRepository.class);
        service = new RentalWorkflowServiceImpl(userRepository, tenantRepository, roomListingRepository,
                roomRepository, bookingRequestRepository, contractRepository,
                contractTenantRepository, paymentRepository, depositConfirmationRequestRepository,
                notificationRepository);
    }

    @Test
    void createRequestReservesRoomAndNotifiesOwner() {
        User tenant = tenantUser("tenant1");
        User landlord = landlordUser("landlord1");
        Building building = new Building();
        building.setOwner(landlord);
        Room room = new Room();
        room.setRoomId(10);
        room.setRoomCode("P101");
        room.setBuilding(building);
        room.setMaxOccupants((short) 4);
        room.setStatus("TRONG");
        RoomListing listing = new RoomListing();
        listing.setListingId(1);
        listing.setRoomCode("P101");
        listing.setStatus("TRONG");
        listing.setManagedRoom(room);

        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(roomListingRepository.findById(1)).thenReturn(Optional.of(listing));
        when(roomListingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(listing));
        when(roomRepository.findByIdForUpdate(10)).thenReturn(Optional.of(room));
        when(bookingRequestRepository.existsByUser_UserIdAndRoom_ListingIdAndStatusIn(
                eq(tenant.getUserId()), eq(1), any())).thenReturn(false);
        when(bookingRequestRepository.save(any(BookingRequest.class))).thenAnswer(inv -> {
            BookingRequest request = inv.getArgument(0);
            request.setRequestId(99);
            return request;
        });

        BookingRequest result = service.createRequest("tenant1", 1, "Nguyen Van A", "0901234567",
                "123456789", (short) 2, LocalDate.now().plusDays(3), "Ghi chú");

        assertEquals("PENDING_LANDLORD_REVIEW", result.getStatus());
        assertEquals("RESERVED", listing.getStatus());
        assertEquals("RESERVED", room.getStatus());
        assertNotEquals("DA_THUE", listing.getStatus());
        ArgumentCaptor<UserNotification> notificationCaptor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertEquals(landlord, notificationCaptor.getValue().getRecipient());
        assertEquals(UserNotification.TYPE_RENTAL_REQUEST, notificationCaptor.getValue().getNotificationType());
        assertEquals(result, notificationCaptor.getValue().getRentalRequest());
        assertTrue(notificationCaptor.getValue().getMessage().contains("P101"));
        assertTrue(notificationCaptor.getValue().getMessage().contains("2"));
    }

    @Test
    void landlordApprovalCreatesEditableDraftWithoutOccupyingRoom() {
        User tenant = tenantUser("tenant1");
        User landlord = landlordUser("landlord1");
        Building building = new Building();
        building.setOwner(landlord);
        Room room = new Room();
        room.setRoomId(10);
        room.setRoomCode("P101");
        room.setBuilding(building);
        room.setStatus("RESERVED");
        RoomListing listing = new RoomListing();
        listing.setListingId(1);
        listing.setRoomCode("P101");
        listing.setStatus("RESERVED");
        listing.setPrice(new BigDecimal("2500000"));
        listing.setManagedRoom(room);
        BookingRequest request = new BookingRequest(tenant, listing);
        request.setRequestId(99);
        request.setStatus("PENDING_LANDLORD_REVIEW");
        request.setExpectedMoveInDate(LocalDate.now().plusDays(5));

        when(userRepository.findByUsername("landlord1")).thenReturn(Optional.of(landlord));
        when(bookingRequestRepository.findByIdForUpdate(99)).thenReturn(Optional.of(request));
        when(roomRepository.findByIdForUpdate(10)).thenReturn(Optional.of(room));
        when(roomListingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(listing));
        when(tenantRepository.findByUser_UserId(tenant.getUserId())).thenReturn(Optional.empty());
        when(bookingRequestRepository.save(any(BookingRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest result = service.approveRequest(99, "landlord1");

        assertEquals("APPROVED_BY_LANDLORD", result.getStatus());
        assertEquals("RESERVED", room.getStatus());
        assertEquals("RESERVED", listing.getStatus());
        ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(contractCaptor.capture());
        Contract draft = contractCaptor.getValue();
        assertEquals("DRAFT", draft.getStatus());
        assertEquals(room, draft.getRoom());
        assertEquals(tenant, draft.getTenantUser());
        assertEquals(request.getExpectedMoveInDate(), draft.getStartDate());
        assertEquals(new BigDecimal("2500000"), draft.getMonthlyRent());
        assertNull(draft.getEndDate());
        assertNull(draft.getDepositAmount());
        verify(notificationRepository).save(argThat(notification ->
                notification.getRecipient().equals(tenant)
                        && notification.getContract() == null
                        && notification.getRentalRequest().equals(result)));
    }

    @Test
    void tenantAcceptanceOccupiesLinkedRoomAndCompletesRequestAtomically() {
        User tenant = tenantUser("tenant1");
        User landlord = landlordUser("landlord1");
        Building building = new Building();
        building.setOwner(landlord);
        Room room = new Room();
        room.setRoomId(10);
        room.setRoomCode("P101");
        room.setBuilding(building);
        room.setStatus("RESERVED");
        RoomListing listing = new RoomListing();
        listing.setListingId(1);
        listing.setRoomCode("P101");
        listing.setStatus("RESERVED");
        listing.setManagedRoom(room);
        BookingRequest request = new BookingRequest(tenant, listing);
        request.setRequestId(99);
        request.setStatus("CONTRACT_SENT");
        request.setFullName("Nguyen Van A");
        request.setPhone("0901234567");
        Contract contract = new Contract();
        contract.setContractId(5);
        contract.setContractCode("HD-TEST");
        contract.setRoom(room);
        contract.setTenantUser(tenant);
        contract.setRentalRequest(request);
        contract.setStatus("SENT_TO_TENANT");
        contract.setStartDate(LocalDate.now().plusDays(5));
        contract.setEndDate(LocalDate.now().plusMonths(6));

        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByUser_UserId(tenant.getUserId())).thenReturn(Optional.empty());
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));
        when(roomRepository.findByIdForUpdate(10)).thenReturn(Optional.of(room));
        when(roomListingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(listing));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomListingRepository.save(any(RoomListing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRequestRepository.save(any(BookingRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        Contract result = service.tenantAcceptContract(5, "tenant1");

        assertEquals("TENANT_ACCEPTED", result.getStatus());
        assertNotNull(result.getTenantAcceptedAt());
        assertEquals("OCCUPIED", room.getStatus());
        assertEquals("DA_THUE", listing.getStatus());
        assertEquals("COMPLETED", request.getStatus());
        assertEquals("Nguyen Van A", listing.getCurrentTenant());
        verify(roomRepository).save(room);
        verify(roomListingRepository).save(listing);
        verify(bookingRequestRepository).save(request);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationRepository).save(argThat(notification ->
                notification.getRecipient().equals(landlord)
                        && notification.getContract().equals(result)));
    }

    @Test
    void createRequestUsesAuthenticatedTenantProfileWithoutAskingForIdentityFields() {
        User tenant = tenantUser("tenant1");
        User landlord = landlordUser("landlord1");
        Building building = new Building();
        building.setOwner(landlord);
        Room room = new Room();
        room.setRoomId(10);
        room.setRoomCode("P101");
        room.setBuilding(building);
        room.setMaxOccupants((short) 4);
        room.setStatus("TRONG");
        RoomListing listing = new RoomListing();
        listing.setListingId(1);
        listing.setRoomCode("P101");
        listing.setStatus("TRONG");
        listing.setManagedRoom(room);

        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByUser_UserId(tenant.getUserId())).thenReturn(Optional.empty());
        when(roomListingRepository.findById(1)).thenReturn(Optional.of(listing));
        when(roomListingRepository.findByIdForUpdate(1)).thenReturn(Optional.of(listing));
        when(roomRepository.findByIdForUpdate(10)).thenReturn(Optional.of(room));
        when(bookingRequestRepository.existsByUser_UserIdAndRoom_ListingIdAndStatusIn(
                eq(tenant.getUserId()), eq(1), any())).thenReturn(false);
        when(bookingRequestRepository.save(any(BookingRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest result = service.createRequest("tenant1", 1, (short) 2,
                LocalDate.now().plusDays(3), "Ghi chú");

        assertEquals(tenant.getFullName(), result.getFullName());
        assertEquals(tenant.getPhone(), result.getPhone());
        assertNull(result.getIdNumber());
        assertEquals("PENDING_LANDLORD_REVIEW", result.getStatus());
        verify(notificationRepository).save(any(UserNotification.class));
    }

    @Test
    void duplicatePendingRequestIsReportedBeforeRoomAvailabilityCheck() {
        User tenant = tenantUser("tenant1");
        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(bookingRequestRepository.existsByUser_UserIdAndRoom_ListingIdAndStatusIn(
                eq(tenant.getUserId()), eq(1), any())).thenReturn(true);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.createRequest("tenant1", 1, "Nguyen Van A", "0901234567",
                        null, (short) 1, LocalDate.now().plusDays(1), null));

        assertTrue(error.getMessage().contains("đã gửi yêu cầu"));
        verify(roomListingRepository, never()).findById(any());
        verify(roomListingRepository, never()).save(any(RoomListing.class));
    }

    @Test
    void landlordCannotApproveWithoutDeposit() {
        User landlord = landlordUser("landlord1");
        Contract contract = pendingFinalContract(landlord);
        contract.setDepositAmount(new BigDecimal("1000000"));
        when(userRepository.findByUsername("landlord1")).thenReturn(Optional.of(landlord));
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));
        when(paymentRepository.existsByContract_ContractIdAndStatusIgnoreCase(5, "SUCCESS")).thenReturn(false);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.landlordApproveContract(5, "landlord1"));

        assertTrue(error.getMessage().contains("tiền cọc"));
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    void tenantDepositRequestIsStoredSeparatelyAndNotMarkedPaid() {
        User tenant = tenantUser("tenant1");
        User landlord = landlordUser("landlord1");
        Contract contract = pendingFinalContract(landlord);
        contract.setStatus("TENANT_ACCEPTED");
        contract.setDepositAmount(new BigDecimal("1000000"));

        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByUser_UserId(tenant.getUserId())).thenReturn(Optional.empty());
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));
        when(contractRepository.findByIdForUpdate(5)).thenReturn(Optional.of(contract));
        when(paymentRepository.existsDepositPaymentByContractIdAndStatus(5, "SUCCESS")).thenReturn(false);
        when(depositConfirmationRequestRepository
                .existsByContract_ContractIdAndStatusIgnoreCase(5, DepositConfirmationRequest.STATUS_PENDING))
                .thenReturn(false);
        when(depositConfirmationRequestRepository.saveAndFlush(any(DepositConfirmationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepositConfirmationRequest result = service.createDepositConfirmationRequest(5, "tenant1");

        assertEquals(DepositConfirmationRequest.STATUS_PENDING, result.getStatus());
        assertEquals(tenant, result.getTenantUser());
        assertEquals(landlord, result.getLandlordUser());
        assertEquals(new BigDecimal("1000000"), result.getAmount());
        assertNull(result.getPayment());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationRepository).save(argThat(notification ->
                notification.getRecipient().equals(landlord)
                        && notification.getNotificationType().equals(UserNotification.TYPE_DEPOSIT)));
    }

    @Test
    void duplicatePendingDepositRequestIsRejected() {
        User tenant = tenantUser("tenant1");
        Contract contract = pendingFinalContract(landlordUser("landlord1"));
        contract.setStatus("TENANT_ACCEPTED");
        contract.setDepositAmount(new BigDecimal("1000000"));
        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByUser_UserId(tenant.getUserId())).thenReturn(Optional.empty());
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));
        when(contractRepository.findByIdForUpdate(5)).thenReturn(Optional.of(contract));
        when(depositConfirmationRequestRepository
                .existsByContract_ContractIdAndStatusIgnoreCase(5, DepositConfirmationRequest.STATUS_PENDING))
                .thenReturn(true);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.createDepositConfirmationRequest(5, "tenant1"));

        assertTrue(error.getMessage().contains("đang được xử lý"));
        verify(depositConfirmationRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void tenantCannotCreateDepositRequestForAnotherTenantsContract() {
        User tenant = tenantUser("tenant1");
        Contract contract = pendingFinalContract(landlordUser("landlord1"));
        contract.setStatus("TENANT_ACCEPTED");
        contract.setDepositAmount(new BigDecimal("1000000"));
        contract.setTenantUser(tenantUser("tenant2"));
        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByUser_UserId(tenant.getUserId())).thenReturn(Optional.empty());
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.createDepositConfirmationRequest(5, "tenant1"));
        verify(contractRepository, never()).findByIdForUpdate(any());
        verify(depositConfirmationRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void landlordApprovalCreatesPaymentOnlyAfterReceivingDeposit() {
        User landlord = landlordUser("landlord1");
        User tenant = tenantUser("tenant1");
        Contract contract = pendingFinalContract(landlord);
        contract.setStatus("TENANT_ACCEPTED");
        contract.setDepositAmount(new BigDecimal("1000000"));
        DepositConfirmationRequest request = pendingDepositRequest(contract, landlord, tenant);
        when(userRepository.findByUsername("landlord1")).thenReturn(Optional.of(landlord));
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));
        when(contractRepository.findByIdForUpdate(5)).thenReturn(Optional.of(contract));
        when(depositConfirmationRequestRepository.findById(17)).thenReturn(Optional.of(request));
        when(depositConfirmationRequestRepository.findStatusByRequestId(17)).thenReturn(Optional.of("PENDING"));
        when(paymentRepository.existsDepositPaymentByContractIdAndStatus(5, "SUCCESS")).thenReturn(false);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(depositConfirmationRequestRepository.save(any(DepositConfirmationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepositConfirmationRequest result =
                service.approveDepositConfirmationRequest(17, "MB-123", "landlord1");

        assertEquals(DepositConfirmationRequest.STATUS_APPROVED, result.getStatus());
        assertEquals(landlord, result.getRespondedBy());
        assertNotNull(result.getRespondedAt());
        assertNotNull(result.getPayment());
        assertNull(result.getPayment().getInvoice());
        assertEquals("SUCCESS", result.getPayment().getStatus());
        assertEquals("MB-123", result.getPayment().getReferenceNo());
        verify(notificationRepository).save(argThat(notification ->
                notification.getRecipient().equals(tenant)));
    }

    @Test
    void landlordRejectionKeepsDepositUnpaidAndStoresReason() {
        User landlord = landlordUser("landlord1");
        User tenant = tenantUser("tenant1");
        Contract contract = pendingFinalContract(landlord);
        contract.setStatus("TENANT_ACCEPTED");
        DepositConfirmationRequest request = pendingDepositRequest(contract, landlord, tenant);
        when(userRepository.findByUsername("landlord1")).thenReturn(Optional.of(landlord));
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));
        when(contractRepository.findByIdForUpdate(5)).thenReturn(Optional.of(contract));
        when(depositConfirmationRequestRepository.findById(17)).thenReturn(Optional.of(request));
        when(depositConfirmationRequestRepository.findStatusByRequestId(17)).thenReturn(Optional.of("PENDING"));
        when(depositConfirmationRequestRepository.save(any(DepositConfirmationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepositConfirmationRequest result =
                service.rejectDepositConfirmationRequest(17, "Chưa nhận được tiền", "landlord1");

        assertEquals(DepositConfirmationRequest.STATUS_REJECTED, result.getStatus());
        assertEquals("Chưa nhận được tiền", result.getRejectionReason());
        assertNull(result.getPayment());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationRepository).save(argThat(notification ->
                notification.getRecipient().equals(tenant)
                        && notification.getMessage().contains("Chưa nhận được tiền")));
    }

    @Test
    void landlordCannotRejectRequestThatWasAlreadyApprovedConcurrently() {
        User landlord = landlordUser("landlord1");
        Contract contract = pendingFinalContract(landlord);
        DepositConfirmationRequest request =
                pendingDepositRequest(contract, landlord, tenantUser("tenant1"));
        when(userRepository.findByUsername("landlord1")).thenReturn(Optional.of(landlord));
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));
        when(contractRepository.findByIdForUpdate(5)).thenReturn(Optional.of(contract));
        when(depositConfirmationRequestRepository.findById(17)).thenReturn(Optional.of(request));
        when(depositConfirmationRequestRepository.findStatusByRequestId(17)).thenReturn(Optional.of("APPROVED"));

        assertThrows(IllegalStateException.class,
                () -> service.rejectDepositConfirmationRequest(17, "reason", "landlord1"));
        verify(depositConfirmationRequestRepository, never()).save(any(DepositConfirmationRequest.class));
    }

    @Test
    void landlordCannotHandleAnotherLandlordsDepositRequest() {
        User otherLandlord = landlordUser("landlord2");
        otherLandlord.setUserId(22);
        Contract contract = pendingFinalContract(otherLandlord);
        DepositConfirmationRequest request =
                pendingDepositRequest(contract, otherLandlord, tenantUser("tenant1"));
        when(userRepository.findByUsername("landlord1")).thenReturn(Optional.of(landlordUser("landlord1")));
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));
        when(depositConfirmationRequestRepository.findById(17)).thenReturn(Optional.of(request));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.approveDepositConfirmationRequest(17, null, "landlord1"));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(depositConfirmationRequestRepository, never()).save(any(DepositConfirmationRequest.class));
    }

    private DepositConfirmationRequest pendingDepositRequest(Contract contract, User landlord, User tenant) {
        DepositConfirmationRequest request = new DepositConfirmationRequest();
        request.setRequestId(17);
        request.setContract(contract);
        request.setTenantUser(tenant);
        request.setLandlordUser(landlord);
        request.setAmount(new BigDecimal("1000000"));
        request.setStatus(DepositConfirmationRequest.STATUS_PENDING);
        request.setRequestedAt(java.time.LocalDateTime.now());
        return request;
    }

    @Test
    void landlordCannotApproveWithoutTenantAcceptance() {
        User landlord = landlordUser("landlord1");
        Contract contract = pendingFinalContract(landlord);
        contract.setTenantAcceptedAt(null);
        contract.setDepositAmount(BigDecimal.ZERO);
        when(userRepository.findByUsername("landlord1")).thenReturn(Optional.of(landlord));
        when(contractRepository.findById(5)).thenReturn(Optional.of(contract));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.landlordApproveContract(5, "landlord1"));
        assertTrue(error.getMessage().contains("đồng ý"));
    }

    private Contract pendingFinalContract(User landlord) {
        Building building = new Building();
        building.setOwner(landlord);
        Room room = new Room();
        room.setRoomId(10);
        room.setRoomCode("P101");
        room.setBuilding(building);
        Tenant tenant = new Tenant();
        tenant.setTenantId(3);
        tenant.setFullName("Nguyen Van A");
        tenant.setPhone("0901234567");
        User tenantUser = tenantUser("tenant1");
        Contract contract = new Contract();
        contract.setContractId(5);
        contract.setContractCode("HD-TEST");
        contract.setRoom(room);
        contract.setRepresentative(tenant);
        contract.setTenantUser(tenantUser);
        contract.setStatus("PENDING_FINAL_LANDLORD_APPROVAL");
        contract.setTenantAcceptedAt(java.time.LocalDateTime.now());
        return contract;
    }

    private User tenantUser(String username) {
        Role role = new Role();
        role.setRoleName(Role.TENANT);
        User user = new User();
        user.setUserId(username.endsWith("1") ? 2 : 99);
        user.setUsername(username);
        user.setFullName("Nguyen Van A");
        user.setPhone("0901234567");
        user.setRole(role);
        user.setEnabled(true);
        user.setAccountStatus("ACTIVE");
        return user;
    }

    private User landlordUser(String username) {
        Role role = new Role();
        role.setRoleName(Role.LANDLORD);
        User user = new User();
        user.setUserId(1);
        user.setUsername(username);
        user.setRole(role);
        user.setEnabled(true);
        user.setAccountStatus("APPROVED");
        return user;
    }
}
