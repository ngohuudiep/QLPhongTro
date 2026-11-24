package com.qlphongtro;

import com.qlphongtro.dto.TerminationRequestForm;
import com.qlphongtro.dto.TerminationSettlementForm;
import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.service.impl.ContractTerminationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContractTerminationServiceImplTest {

    private UserRepository userRepository;
    private TenantRepository tenantRepository;
    private ContractRepository contractRepository;
    private ContractTenantRepository contractTenantRepository;
    private RoomRepository roomRepository;
    private RoomListingRepository roomListingRepository;
    private InvoiceRepository invoiceRepository;
    private PaymentRepository paymentRepository;
    private MeterReadingRepository meterReadingRepository;
    private UserNotificationRepository notificationRepository;
    private ContractTerminationRequestRepository requestRepository;
    private ContractSettlementRepository settlementRepository;
    private ContractTerminationHistoryRepository historyRepository;
    private ContractTerminationServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tenantRepository = mock(TenantRepository.class);
        contractRepository = mock(ContractRepository.class);
        contractTenantRepository = mock(ContractTenantRepository.class);
        roomRepository = mock(RoomRepository.class);
        roomListingRepository = mock(RoomListingRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        meterReadingRepository = mock(MeterReadingRepository.class);
        notificationRepository = mock(UserNotificationRepository.class);
        requestRepository = mock(ContractTerminationRequestRepository.class);
        settlementRepository = mock(ContractSettlementRepository.class);
        historyRepository = mock(ContractTerminationHistoryRepository.class);
        service = new ContractTerminationServiceImpl(userRepository, tenantRepository, contractRepository,
                contractTenantRepository, roomRepository, roomListingRepository, invoiceRepository,
                paymentRepository, meterReadingRepository, notificationRepository, requestRepository,
                settlementRepository, historyRepository);
    }

    @Test
    void tenantRequestChangesOnlyContractWorkflowStateAndDoesNotReleaseRoom() {
        User tenant = user("tenant", Role.TENANT, 2);
        User landlord = user("landlord", Role.LANDLORD, 1);
        Contract contract = activeContract(tenant, landlord);
        contract.setEndDate(LocalDate.now().plusMonths(3));
        contract.getRoom().setStatus("OCCUPIED");
        when(userRepository.findByUsername("tenant")).thenReturn(Optional.of(tenant));
        when(contractRepository.findByIdForUpdate(7)).thenReturn(Optional.of(contract));
        when(roomRepository.findByIdForUpdate(10)).thenReturn(Optional.of(contract.getRoom()));
        when(contractRepository.existsByRoom_RoomIdAndStatusInAndContractIdNot(
                eq(10), anyList(), eq(7))).thenReturn(false);
        when(requestRepository.existsByContract_ContractIdAndStatusIn(eq(7), anyList())).thenReturn(false);
        when(requestRepository.save(any(ContractTerminationRequest.class))).thenAnswer(invocation -> {
            ContractTerminationRequest request = invocation.getArgument(0);
            request.setTerminationRequestId(31);
            return request;
        });

        TerminationRequestForm form = requestForm(LocalDate.now().plusDays(10));
        ContractTerminationRequest request = service.requestFromTenant(7, form, "tenant");

        assertEquals(ContractTerminationRequest.PENDING_RESPONSE, request.getStatus());
        assertEquals(landlord, request.getResponder());
        assertEquals("TERMINATION_REQUESTED", contract.getStatus());
        assertEquals("OCCUPIED", contract.getRoom().getStatus());
        assertEquals(LocalDate.now().plusMonths(3), contract.getEndDate());
        assertNull(contract.getActualEndDate());
        verify(roomRepository, never()).save(any(Room.class));
        verify(roomListingRepository, never()).save(any(RoomListing.class));
        verify(notificationRepository).save(argThat(notification ->
                notification.getRecipient().equals(landlord)
                        && UserNotification.TYPE_CONTRACT_TERMINATION.equals(notification.getNotificationType())));
    }

    @Test
    void tenantCannotCreateRequestForAnotherTenantContract() {
        User tenant = user("tenant", Role.TENANT, 2);
        User otherTenant = user("other", Role.TENANT, 3);
        User landlord = user("landlord", Role.LANDLORD, 1);
        Contract contract = activeContract(otherTenant, landlord);
        when(userRepository.findByUsername("tenant")).thenReturn(Optional.of(tenant));
        when(contractRepository.findByIdForUpdate(7)).thenReturn(Optional.of(contract));
        when(tenantRepository.findByUser_UserId(tenant.getUserId())).thenReturn(Optional.empty());
        when(contractTenantRepository.existsById_ContractIdAndId_TenantId(7, tenant.getUserId()))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.requestFromTenant(7, requestForm(LocalDate.now().plusDays(1)), "tenant"));
        verify(requestRepository, never()).save(any(ContractTerminationRequest.class));
        verify(roomRepository, never()).findByIdForUpdate(anyInt());
    }

    @Test
    void rejectingRequestRestoresContractStatusWithoutReleasingRoom() {
        User tenant = user("tenant", Role.TENANT, 2);
        User landlord = user("landlord", Role.LANDLORD, 1);
        Contract contract = activeContract(tenant, landlord);
        contract.setStatus("TERMINATION_REQUESTED");
        ContractTerminationRequest request = request(contract, tenant, landlord);
        request.setStatus(ContractTerminationRequest.PENDING_RESPONSE);
        when(userRepository.findByUsername("landlord")).thenReturn(Optional.of(landlord));
        when(requestRepository.findByIdForUpdate(31)).thenReturn(Optional.of(request));
        when(contractRepository.findByIdForUpdate(7)).thenReturn(Optional.of(contract));

        service.respond(31, false, "Không thống nhất ngày bàn giao", "landlord", true);

        assertEquals(ContractTerminationRequest.REJECTED, request.getStatus());
        assertEquals("ACTIVE", contract.getStatus());
        assertNull(contract.getActualEndDate());
        verify(roomRepository, never()).save(any(Room.class));
        verify(roomListingRepository, never()).save(any(RoomListing.class));
    }

    @Test
    void settlementUsesCalendarDayProrationAndCalculatesAmountsWithoutMarkingTransfersPaid() {
        User tenant = user("tenant", Role.TENANT, 2);
        User landlord = user("landlord", Role.LANDLORD, 1);
        Contract contract = activeContract(tenant, landlord);
        contract.setStatus("TERMINATION_REQUESTED");
        contract.setStartDate(LocalDate.of(2024, 2, 1));
        contract.setEndDate(LocalDate.of(2024, 12, 31));
        contract.setMonthlyRent(new BigDecimal("2900.00"));
        contract.setMonthlyElectricPrice(new BigDecimal("2.00"));
        contract.setMonthlyWaterPrice(new BigDecimal("3.00"));
        contract.setDepositAmount(new BigDecimal("1000.00"));
        ContractTerminationRequest request = request(contract, tenant, landlord);
        request.setStatus(ContractTerminationRequest.PENDING_SETTLEMENT);
        request.setExpectedTerminationDate(LocalDate.of(2024, 2, 15));
        ContractSettlement settlement = new ContractSettlement();
        settlement.setTerminationRequest(request);
        settlement.setStatus(ContractSettlement.DRAFT);
        request.setSettlement(settlement);
        when(userRepository.findByUsername("landlord")).thenReturn(Optional.of(landlord));
        when(requestRepository.findByIdForUpdate(31)).thenReturn(Optional.of(request));
        when(contractRepository.findByIdForUpdate(7)).thenReturn(Optional.of(contract));
        when(settlementRepository.findByTerminationRequest_TerminationRequestId(31))
                .thenReturn(Optional.of(settlement));
        when(invoiceRepository.findByContract_ContractId(7)).thenReturn(List.of());

        TerminationSettlementForm form = new TerminationSettlementForm();
        form.setElectricityOldReading(new BigDecimal("100"));
        form.setElectricityNewReading(new BigDecimal("110"));
        form.setWaterOldReading(new BigDecimal("10"));
        form.setWaterNewReading(new BigDecimal("11"));
        form.setServiceFeeAmount(new BigDecimal("100"));
        form.setCompensationAmount(new BigDecimal("50"));
        form.setCompensationReason("Hư hỏng có biên bản");
        form.setDepositDeductionAmount(new BigDecimal("200"));
        form.setDepositDeductionReason("Khấu trừ theo biên bản");
        form.setRoomHandoverCondition("Đã ghi nhận tình trạng bàn giao");

        ContractSettlement result = service.submitSettlement(31, form, "landlord");

        assertEquals(0, new BigDecimal("1500.00").compareTo(result.getRentThroughMoveOutAmount()));
        assertEquals(0, new BigDecimal("20.00").compareTo(result.getElectricityAmount()));
        assertEquals(0, new BigDecimal("3.00").compareTo(result.getWaterAmount()));
        assertEquals(0, new BigDecimal("1473.00").compareTo(result.getTotalDueAmount()));
        assertEquals(0, new BigDecimal("800.00").compareTo(result.getDepositRefundAmount()));
        assertEquals(ContractSettlement.PENDING_CONFIRMATION, result.getStatus());
        assertNull(result.getTenantPaymentConfirmedAt());
        assertNull(result.getDepositRefundedAt());
    }

    private static TerminationRequestForm requestForm(LocalDate date) {
        TerminationRequestForm form = new TerminationRequestForm();
        form.setReasonCode("Chuyển nơi ở");
        form.setReasonDetails("Người thuê cần chuyển nơi ở.");
        form.setExpectedTerminationDate(date);
        return form;
    }

    private static ContractTerminationRequest request(Contract contract, User tenant, User landlord) {
        ContractTerminationRequest request = new ContractTerminationRequest();
        request.setTerminationRequestId(31);
        request.setContract(contract);
        request.setRequester(tenant);
        request.setResponder(landlord);
        request.setRequesterRole("TENANT");
        request.setOriginalContractStatus("ACTIVE");
        request.setReasonCode("Chuyển nơi ở");
        request.setReasonDetails("Người thuê cần chuyển nơi ở.");
        request.setExpectedTerminationDate(LocalDate.now().plusDays(10));
        return request;
    }

    private static Contract activeContract(User tenant, User landlord) {
        Building building = new Building();
        building.setOwner(landlord);
        building.setBuildingName("Khu trọ A");
        building.setAddress("Địa chỉ A");
        Room room = new Room();
        room.setRoomId(10);
        room.setRoomCode("P101");
        room.setBuilding(building);
        room.setStatus("OCCUPIED");
        room.setPriceOverride(new BigDecimal("2900"));
        Contract contract = new Contract();
        contract.setContractId(7);
        contract.setContractCode("HD-TEST");
        contract.setRoom(room);
        contract.setTenantUser(tenant);
        contract.setStartDate(LocalDate.now().minusMonths(1));
        contract.setEndDate(LocalDate.now().plusMonths(6));
        contract.setMonthlyRent(new BigDecimal("2900"));
        contract.setStatus("ACTIVE");
        return contract;
    }

    private static User user(String username, String roleName, int userId) {
        Role role = new Role();
        role.setRoleName(roleName);
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setFullName(username);
        user.setRole(role);
        user.setEnabled(true);
        user.setAccountStatus("APPROVED");
        return user;
    }
}
