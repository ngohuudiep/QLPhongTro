package com.qlphongtro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContractTenantId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "contract_id")
    private Integer contractId;

    @Column(name = "tenant_id")
    private Integer tenantId;

    public ContractTenantId() {
    }

    public ContractTenantId(Integer contractId, Integer tenantId) {
        this.contractId = contractId;
        this.tenantId = tenantId;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractTenantId that = (ContractTenantId) o;
        return Objects.equals(contractId, that.contractId) &&
               Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId, tenantId);
    }
}
