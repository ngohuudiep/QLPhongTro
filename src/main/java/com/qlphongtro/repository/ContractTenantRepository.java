package com.qlphongtro.repository;

import com.qlphongtro.entity.ContractTenant;
import com.qlphongtro.entity.ContractTenantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractTenantRepository extends JpaRepository<ContractTenant, ContractTenantId> {

    List<ContractTenant> findById_ContractId(Integer contractId);

    List<ContractTenant> findById_TenantId(Integer tenantId);
}
