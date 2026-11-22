package com.qlphongtro;

import com.qlphongtro.dto.InvoiceCreateRequest;
import com.qlphongtro.dto.InvoiceServiceChargeRequest;
import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.service.impl.InvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private MeterReadingRepository meterReadingRepository;
    @Mock private RoomServiceRepository roomServiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserNotificationRepository userNotificationRepository;

    private InvoiceServiceImpl service;
    private Contract contract;
    private User tenantUser;

    @BeforeEach
    void setUp() {
        service = new InvoiceServiceImpl(invoiceRepository, contractRepository, meterReadingRepository,
                roomServiceRepository, paymentRepository, userNotificationRepository);

        User landlord = new User();
        landlord.setUsername("owner");
        landlord.setRole(new Role(1, Role.LANDLORD));

        Building building = new Building();
        building.setOwner(landlord);
        building.setBuildingName("Khu trọ A");

        Room room = new Room();
        room.setRoomId(12);
        room.setRoomCode("P101");
        room.setBuilding(building);

        tenantUser = new User();
        tenantUser.setUserId(22);
        tenantUser.setUsername("tenant");
        tenantUser.setEnabled(true);
        tenantUser.setAccountStatus("APPROVED");
        tenantUser.setRole(new Role(2, Role.TENANT));

        Tenant tenant = new Tenant();
        tenant.setTenantId(33);
        tenant.setFullName("Người thuê");
        tenant.setUser(tenantUser);

        contract = new Contract();
        contract.setContractId(44);
        contract.setContractCode("HD-44");
        contract.setRoom(room);
        contract.setRepresentative(tenant);
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2027, 12, 31));
        contract.setStatus("ACTIVE");
        contract.setContractTenants(new ArrayList<>());

        lenient().when(contractRepository.findById(44)).thenReturn(Optional.of(contract));
    }

    @Test
    void createsInvoiceLinesTotalsAndNotificationForContractTenant() {
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userNotificationRepository.save(any(UserNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        InvoiceCreateRequest request = request();

        Invoice saved = service.createMonthlyInvoice(request, "owner");

        assertEquals(new BigDecimal("2850000.00"), saved.getTotalAmount());
        assertEquals("UNPAID", saved.getStatus());
        assertEquals(6, saved.getLineItems().size());
        assertEquals(new BigDecimal("120"), saved.getLineItems().get(1).getQuantity());
        assertEquals(new BigDecimal("10"), saved.getLineItems().get(2).getQuantity());
        assertEquals(new BigDecimal("120"), saved.getLineItems().get(1).getPreviousReading());
        assertEquals(new BigDecimal("240"), saved.getLineItems().get(1).getCurrentReading());

        ArgumentCaptor<UserNotification> notificationCaptor = ArgumentCaptor.forClass(UserNotification.class);
        verify(userNotificationRepository).save(notificationCaptor.capture());
        assertEquals(tenantUser, notificationCaptor.getValue().getRecipient());
        assertTrue(notificationCaptor.getValue().getMessage().contains("2.850.000"));
    }

    @Test
    void rejectsDecreasingMeterReadingsWithoutSavingInvoice() {
        InvoiceCreateRequest request = request();
        request.setElectricityCurrentReading(new BigDecimal("119"));

        assertThrows(IllegalArgumentException.class, () -> service.createMonthlyInvoice(request, "owner"));

        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(userNotificationRepository, never()).save(any(UserNotification.class));
    }

    @Test
    void rejectsDuplicateRoomAndBillingPeriod() {
        when(invoiceRepository.existsByContract_Room_RoomIdAndPeriodStart(12, LocalDate.of(2026, 11, 1)))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.createMonthlyInvoice(request(), "owner"));

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void rejectsNegativeServiceCharge() {
        InvoiceCreateRequest request = request();
        InvoiceServiceChargeRequest negativeCharge = new InvoiceServiceChargeRequest();
        negativeCharge.setName("Internet");
        negativeCharge.setAmount(new BigDecimal("-1"));
        request.setServiceCharges(List.of(negativeCharge));

        assertThrows(IllegalArgumentException.class, () -> service.createMonthlyInvoice(request, "owner"));

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void doesNotAllowLandlordToBillAnotherLandlordsContract() {
        assertThrows(AccessDeniedException.class, () -> service.createMonthlyInvoice(request(), "another-owner"));

        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(userNotificationRepository, never()).save(any(UserNotification.class));
    }

    @Test
    void sendsInvoiceToDirectContractAccountWithoutTenantProfile() {
        contract.setRepresentative(null);
        contract.setTenantUser(tenantUser);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userNotificationRepository.save(any(UserNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Invoice saved = service.createMonthlyInvoice(request(), "owner");

        assertEquals("UNPAID", saved.getStatus());
        verify(userNotificationRepository).save(argThat(notification ->
                tenantUser.equals(notification.getRecipient())));
    }

    @Test
    void landlordPaymentConfirmationRecordsSuccessfulPaymentEvidence() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(99);
        invoice.setContract(contract);
        invoice.setTotalAmount(new BigDecimal("1250000.00"));
        invoice.setStatus("UNPAID");
        when(invoiceRepository.findById(99)).thenReturn(Optional.of(invoice));

        service.confirmInvoicePaidByLandlord(99, "owner", "BANK_TRANSFER", "TX-99");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals("SUCCESS", paymentCaptor.getValue().getStatus());
        assertNotNull(paymentCaptor.getValue().getProcessedAt());
        assertEquals("PAID", invoice.getStatus());
    }

    private static InvoiceCreateRequest request() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId(44);
        request.setMonth("2026-11");
        request.setDueDate(LocalDate.of(2026, 12, 10));
        request.setRoomAmount(new BigDecimal("2000000"));
        request.setElectricityPreviousReading(new BigDecimal("120"));
        request.setElectricityCurrentReading(new BigDecimal("240"));
        request.setElectricityUnitPrice(new BigDecimal("3500"));
        request.setWaterPreviousReading(new BigDecimal("30"));
        request.setWaterCurrentReading(new BigDecimal("40"));
        request.setWaterUnitPrice(new BigDecimal("20000"));
        InvoiceServiceChargeRequest internet = new InvoiceServiceChargeRequest();
        internet.setName("Internet");
        internet.setAmount(new BigDecimal("100000"));
        InvoiceServiceChargeRequest cleaning = new InvoiceServiceChargeRequest();
        cleaning.setName("Vệ sinh");
        cleaning.setAmount(new BigDecimal("30000"));
        InvoiceServiceChargeRequest parking = new InvoiceServiceChargeRequest();
        parking.setName("Gửi xe");
        parking.setAmount(new BigDecimal("100000"));
        request.setServiceCharges(List.of(internet, cleaning, parking));
        return request;
    }
}
