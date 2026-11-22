package com.qlphongtro;

import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.service.impl.TenantPortalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantPortalServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractTenantRepository contractTenantRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserNotificationRepository userNotificationRepository;
    @Mock private IncidentRepository incidentRepository;

    private TenantPortalServiceImpl service;
    private User tenantUser;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        service = new TenantPortalServiceImpl(userRepository, tenantRepository, contractRepository,
                contractTenantRepository, invoiceRepository, paymentRepository,
                userNotificationRepository, incidentRepository);
        tenantUser = new User();
        tenantUser.setUserId(7);
        tenantUser.setUsername("tenant-a");
        tenantUser.setRole(new Role(1, Role.TENANT));
        tenantUser.setAccountStatus("ACTIVE");
        tenantUser.setEnabled(true);
        tenant = new Tenant();
        tenant.setTenantId(9);
        tenant.setUser(tenantUser);
        when(userRepository.findByUsername("tenant-a")).thenReturn(Optional.of(tenantUser));
    }

    @Test
    void tenantCannotReadInvoiceOutsideTheirContract() {
        when(tenantRepository.findByUser_UserId(7)).thenReturn(Optional.of(tenant));
        Contract foreignContract = new Contract();
        foreignContract.setContractId(200);
        Invoice foreignInvoice = new Invoice();
        foreignInvoice.setInvoiceId(500);
        foreignInvoice.setContract(foreignContract);
        when(invoiceRepository.findById(500)).thenReturn(Optional.of(foreignInvoice));

        assertThrows(AccessDeniedException.class, () -> service.getInvoice("tenant-a", 500));
    }

    @Test
    void tenantCannotMarkAnotherUsersNotificationRead() {
        when(userNotificationRepository.findByNotificationIdAndRecipient_UserId(4, 7))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.markNotificationRead("tenant-a", 4));

        verify(userNotificationRepository, never()).save(any(UserNotification.class));
    }

    @Test
    void formerCoTenantCannotReadInvoiceFromAfterMovingOut() {
        Contract contract = new Contract();
        contract.setContractId(300);
        ContractTenant link = new ContractTenant();
        link.setContract(contract);
        link.setTenant(tenant);
        link.setMoveInDate(LocalDate.of(2026, 1, 1));
        link.setMoveOutDate(LocalDate.of(2026, 10, 31));
        when(tenantRepository.findByUser_UserId(7)).thenReturn(Optional.of(tenant));
        when(contractTenantRepository.findById_TenantId(9)).thenReturn(List.of(link));
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(501);
        invoice.setContract(contract);
        invoice.setPeriodStart(LocalDate.of(2026, 11, 1));
        invoice.setPeriodEnd(LocalDate.of(2026, 11, 30));
        when(invoiceRepository.findById(501)).thenReturn(Optional.of(invoice));

        assertThrows(AccessDeniedException.class, () -> service.getInvoice("tenant-a", 501));
    }

    @Test
    void acceptedFutureDatedContractAppearsAsCurrentRental() {
        when(tenantRepository.findByUser_UserId(7)).thenReturn(Optional.of(tenant));
        Contract contract = new Contract();
        contract.setContractId(801);
        contract.setTenantUser(tenantUser);
        contract.setStatus("TENANT_ACCEPTED");
        contract.setStartDate(LocalDate.now().plusDays(10));
        contract.setEndDate(LocalDate.now().plusMonths(6));
        when(contractRepository.findByTenantUser_UserIdOrderByContractIdDesc(7))
                .thenReturn(List.of(contract));

        List<Contract> activeContracts = service.getActiveContracts("tenant-a");

        assertEquals(List.of(contract), activeContracts);
    }
}
