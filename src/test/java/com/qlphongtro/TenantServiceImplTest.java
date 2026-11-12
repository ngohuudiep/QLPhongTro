package com.qlphongtro;

import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.service.impl.TenantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantServiceImplTest {

    private TenantRepository tenantRepository;
    private ContractTenantRepository contractTenantRepository;
    private TenantServiceImpl tenantService;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        contractTenantRepository = mock(ContractTenantRepository.class);
        tenantService = new TenantServiceImpl(tenantRepository, contractTenantRepository);
    }

    @Test
    void deleteIsBlockedWhenTenantHasContractHistory() {
        when(tenantRepository.existsById(1)).thenReturn(true);
        when(contractTenantRepository.existsById_TenantId(1)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> tenantService.deleteTenant(1));

        verify(tenantRepository, never()).deleteById(1);
    }

    @Test
    void deleteIsAllowedWhenTenantHasNoContracts() {
        when(tenantRepository.existsById(1)).thenReturn(true);
        when(contractTenantRepository.existsById_TenantId(1)).thenReturn(false);

        tenantService.deleteTenant(1);

        verify(tenantRepository).deleteById(1);
    }
}
