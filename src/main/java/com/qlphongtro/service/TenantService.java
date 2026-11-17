package com.qlphongtro.service;

import com.qlphongtro.entity.Tenant;
import java.util.List;

public interface TenantService {

    List<Tenant> getAllTenants();

    Tenant getTenantById(Integer id);

    Tenant getCurrentTenant();

    Tenant saveTenant(Tenant tenant);

    void deleteTenant(Integer id);

    boolean existsByIdNumber(String idNumber);

    List<Tenant> searchTenants(String keyword);
}
