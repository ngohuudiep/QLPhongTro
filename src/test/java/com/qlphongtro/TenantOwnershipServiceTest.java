package com.qlphongtro;

import com.qlphongtro.repository.*;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.impl.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TenantOwnershipServiceTest {
    private TenantRepository tenantRepository;
    private ContractRepository contractRepository;
    private InvoiceRepository invoiceRepository;
    private PaymentRepository paymentRepository;
    private MeterReadingRepository meterReadingRepository;
    private IncidentRepository incidentRepository;

    @BeforeEach
    void authenticateTenant() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "tenant-a", "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        tenantRepository = mock(TenantRepository.class);
        contractRepository = mock(ContractRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        meterReadingRepository = mock(MeterReadingRepository.class);
        incidentRepository = mock(IncidentRepository.class);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantCannotLoadAnotherTenantProfileById() {
        TenantServiceImpl service = new TenantServiceImpl(tenantRepository, mock(ContractTenantRepository.class),
                contractRepository, mock(BuildingAccess.class));
        when(tenantRepository.findByTenantIdAndUser_Username(42, "tenant-a")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getTenantById(42));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(tenantRepository).findByTenantIdAndUser_Username(42, "tenant-a");
        verify(tenantRepository, never()).findById(42);
    }

    @Test
    void tenantCannotLoadAnotherTenantsContractById() {
        ContractServiceImpl service = new ContractServiceImpl(contractRepository, mock(ContractTenantRepository.class),
                mock(RoomRepository.class), tenantRepository, mock(BuildingAccess.class));
        when(contractRepository.findByIdAndTenantUsername(17, "tenant-a")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getContractById(17));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(contractRepository).findByIdAndTenantUsername(17, "tenant-a");
        verify(contractRepository, never()).findById(17);
    }

    @Test
    void tenantCannotLoadAnotherTenantsInvoiceById() {
        InvoiceServiceImpl service = new InvoiceServiceImpl(invoiceRepository, contractRepository,
                meterReadingRepository, mock(RoomServiceRepository.class), mock(BuildingAccess.class));
        when(invoiceRepository.findByIdAndTenantUsername(23, "tenant-a")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getInvoiceById(23));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(invoiceRepository).findByIdAndTenantUsername(23, "tenant-a");
        verify(invoiceRepository, never()).findById(23);
    }

    @Test
    void tenantInvoicePaymentListUsesPrincipalScopedQuery() {
        PaymentServiceImpl service = new PaymentServiceImpl(paymentRepository, invoiceRepository, mock(BuildingAccess.class));
        when(paymentRepository.findByInvoiceIdAndTenantUsername(29, "tenant-a")).thenReturn(List.of());

        service.getPaymentsByInvoiceId(29);

        verify(paymentRepository).findByInvoiceIdAndTenantUsername(29, "tenant-a");
        verify(paymentRepository, never()).findByInvoice_InvoiceId(29);
    }

    @Test
    void tenantCannotLoadAnotherTenantsMeterReadingById() {
        MeterReadingServiceImpl service = new MeterReadingServiceImpl(meterReadingRepository, mock(BuildingAccess.class));
        when(meterReadingRepository.findByIdAndTenantUsername(31, "tenant-a")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getReadingById(31));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(meterReadingRepository).findByIdAndTenantUsername(31, "tenant-a");
        verify(meterReadingRepository, never()).findById(31);
    }

    @Test
    void tenantCannotLoadAnotherTenantsIncidentById() {
        IncidentServiceImpl service = new IncidentServiceImpl(incidentRepository, tenantRepository, contractRepository, mock(BuildingAccess.class));
        when(incidentRepository.findByIdAndTenantUsername(37, "tenant-a")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getIncidentById(37));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(incidentRepository).findByIdAndTenantUsername(37, "tenant-a");
        verify(incidentRepository, never()).findById(37);
    }
}
