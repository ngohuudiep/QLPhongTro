package com.qlphongtro.service;

import com.qlphongtro.entity.Tenant;
import com.qlphongtro.entity.User;
import java.util.List;

public interface TenantService {

    List<Tenant> getAllTenants();

    Tenant getTenantById(Integer id);

    Tenant getCurrentTenant();

    Tenant saveTenant(Tenant tenant);

    List<User> getUnlinkedUserAccounts();

    void linkUserAccount(Integer tenantId, Integer userId);

    void deleteTenant(Integer id);

    boolean existsByIdNumber(String idNumber);

    List<Tenant> searchTenants(String keyword);
}
