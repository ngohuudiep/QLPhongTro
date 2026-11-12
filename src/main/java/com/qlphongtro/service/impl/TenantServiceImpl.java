package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Tenant;
import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final ContractTenantRepository contractTenantRepository;

    @Autowired
    public TenantServiceImpl(
            TenantRepository tenantRepository,
            ContractTenantRepository contractTenantRepository) {
        this.tenantRepository = tenantRepository;
        this.contractTenantRepository = contractTenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant getTenantById(Integer id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách thuê có ID: " + id));
    }

    @Override
    public Tenant saveTenant(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    @Override
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
    public boolean existsByIdNumber(String idNumber) {
        return tenantRepository.existsByIdNumber(idNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> searchTenants(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return tenantRepository.findAll();
        }
        String cleanKeyword = keyword.trim();
        return tenantRepository.findByFullNameContainingIgnoreCaseOrIdNumberContainingOrPhoneContaining(
                cleanKeyword, cleanKeyword, cleanKeyword
        );
    }
}
