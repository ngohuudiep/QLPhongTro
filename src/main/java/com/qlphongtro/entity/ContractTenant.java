package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Contract_Tenants")
public class ContractTenant {

    @EmbeddedId
    private ContractTenantId id;

    // Quan hệ ManyToOne tới Contracts
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @MapsId("contractId")
    private Contract contract;

    // Quan hệ ManyToOne tới Tenants
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @MapsId("tenantId")
    private Tenant tenant;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "move_out_date")
    private LocalDate moveOutDate;

    public ContractTenant() {
    }

    public ContractTenant(ContractTenantId id, Contract contract, Tenant tenant, LocalDate moveInDate, LocalDate moveOutDate) {
        this.id = id;
        this.contract = contract;
        this.tenant = tenant;
        this.moveInDate = moveInDate;
        this.moveOutDate = moveOutDate;
    }

    public ContractTenantId getId() {
        return id;
    }

    public void setId(ContractTenantId id) {
        this.id = id;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public LocalDate getMoveInDate() {
        return moveInDate;
    }

    public void setMoveInDate(LocalDate moveInDate) {
        this.moveInDate = moveInDate;
    }

    public LocalDate getMoveOutDate() {
        return moveOutDate;
    }

    public void setMoveOutDate(LocalDate moveOutDate) {
        this.moveOutDate = moveOutDate;
    }

    public static class ContractTenantBuilder {
        private ContractTenantId id;
        private Contract contract;
        private Tenant tenant;
        private LocalDate moveInDate;
        private LocalDate moveOutDate;

        ContractTenantBuilder() {}

        public ContractTenantBuilder id(ContractTenantId id) {
            this.id = id;
            return this;
        }

        public ContractTenantBuilder contract(Contract contract) {
            this.contract = contract;
            return this;
        }

        public ContractTenantBuilder tenant(Tenant tenant) {
            this.tenant = tenant;
            return this;
        }

        public ContractTenantBuilder moveInDate(LocalDate moveInDate) {
            this.moveInDate = moveInDate;
            return this;
        }

        public ContractTenantBuilder moveOutDate(LocalDate moveOutDate) {
            this.moveOutDate = moveOutDate;
            return this;
        }

        public ContractTenant build() {
            return new ContractTenant(id, contract, tenant, moveInDate, moveOutDate);
        }
    }

    public static ContractTenantBuilder builder() {
        return new ContractTenantBuilder();
    }
}
