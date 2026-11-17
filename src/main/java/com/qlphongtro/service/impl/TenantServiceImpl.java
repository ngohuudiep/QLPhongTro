package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Tenant;
import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.security.CurrentUser;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final ContractRepository contractRepository;
    private final BuildingAccess buildingAccess;

    @Autowired
    public TenantServiceImpl(
            TenantRepository tenantRepository,
            ContractTenantRepository contractTenantRepository,
            ContractRepository contractRepository,
            BuildingAccess buildingAccess) {
        this.tenantRepository = tenantRepository;
        this.contractTenantRepository = contractTenantRepository;
        this.contractRepository = contractRepository;
        this.buildingAccess = buildingAccess;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public List<Tenant> getAllTenants() {
        if (CurrentUser.isTenant()) {
            return tenantRepository.findByUser_Username(CurrentUser.username()).stream().toList();
        }
        if (buildingAccess.isStaff()) {
            return getTenantsInAssignedBuildings();
        }
        return tenantRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Tenant getTenantById(Integer id) {
        if (CurrentUser.isTenant()) {
            return tenantRepository.findByTenantIdAndUser_Username(id, CurrentUser.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        if (buildingAccess.isStaff()) {
            return getTenantsInAssignedBuildings().stream()
                    .filter(tenant -> id.equals(tenant.getTenantId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách thuê có ID: " + id));
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public Tenant getCurrentTenant() {
        return tenantRepository.findByUser_Username(CurrentUser.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Tenant saveTenant(Tenant tenant) {
        if (buildingAccess.isStaff()) {
            if (tenant.getTenantId() == null) {
                throw new org.springframework.security.access.AccessDeniedException("Staff cannot create an unassigned tenant profile.");
            }
            getTenantById(tenant.getTenantId());
        }
        return tenantRepository.save(tenant);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTenant(Integer id) {
        if (!tenantRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa, khách thuê không tồn tại ID: " + id);
        }
        if (contractTenantRepository.existsById_TenantId(id)) {
            throw new IllegalStateException("Không thể xóa khách thuê vì đang có hợp đồng liên quan.");
        }
        tenantRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public boolean existsByIdNumber(String idNumber) {
        return tenantRepository.existsByIdNumber(idNumber);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public List<Tenant> searchTenants(String keyword) {
        if (CurrentUser.isTenant()) {
            Tenant tenant = tenantRepository.findByUser_Username(CurrentUser.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            String term = keyword == null ? "" : keyword.trim().toLowerCase();
            boolean matches = term.isEmpty()
                    || (tenant.getFullName() != null && tenant.getFullName().toLowerCase().contains(term))
                    || (tenant.getIdNumber() != null && tenant.getIdNumber().toLowerCase().contains(term))
                    || (tenant.getPhone() != null && tenant.getPhone().toLowerCase().contains(term));
            return matches ? List.of(tenant) : List.of();
        }
        if (buildingAccess.isStaff()) {
            String term = keyword == null ? "" : keyword.trim().toLowerCase();
            return getTenantsInAssignedBuildings().stream().filter(tenant -> term.isEmpty()
                    || (tenant.getFullName() != null && tenant.getFullName().toLowerCase().contains(term))
                    || (tenant.getIdNumber() != null && tenant.getIdNumber().toLowerCase().contains(term))
                    || (tenant.getPhone() != null && tenant.getPhone().contains(term))).toList();
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return tenantRepository.findAll();
        }
        String cleanKeyword = keyword.trim();
        return tenantRepository.findByFullNameContainingIgnoreCaseOrIdNumberContainingOrPhoneContaining(
                cleanKeyword, cleanKeyword, cleanKeyword
        );
    }

    private List<Tenant> getTenantsInAssignedBuildings() {
        List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
        if (buildingIds.isEmpty()) {
            return List.of();
        }
        return contractRepository.findDistinctByRoom_Building_BuildingIdIn(buildingIds).stream()
                .flatMap(contract -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(contract.getRepresentative()),
                        contract.getContractTenants().stream().map(link -> link.getTenant())))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(Tenant::getTenantId, tenant -> tenant, (first, ignored) -> first))
                .values().stream().toList();
    }
}
