package com.qlphongtro.service;

import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.ContractTenant;

import java.time.LocalDate;
import java.util.List;

public interface ContractService {

    List<Contract> getAllContracts();

    Contract getContractById(Integer id);

    // Lưu hợp đồng: nếu là hợp đồng mới tạo thành công, tự động cập nhật status của Room thành "Đang thuê" (OCCUPIED)
    Contract saveContract(Contract contract);

    void deleteContract(Integer id);

    // Quản lý khách ở cùng hợp đồng (Contract_Tenants)
    void addTenantToContract(Integer contractId, Integer tenantId, LocalDate moveInDate);

    void removeTenantFromContract(Integer contractId, Integer tenantId);

    List<ContractTenant> getTenantsByContractId(Integer contractId);
}
