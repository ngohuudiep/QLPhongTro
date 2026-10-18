package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Integer contractId;

    @Column(name = "contract_code", nullable = false, length = 30)
    private String contractCode;

    // Quan hệ ManyToOne tới Rooms (Hợp đồng cho phòng nào)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // Quan hệ ManyToOne tới Tenants (Khách thuê đại diện đứng tên hợp đồng)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_id", nullable = false)
    private Tenant representative;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "deposit_amount", precision = 14, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "billing_cycle_months")
    private Short billingCycleMonths;

    @Column(name = "status", length = 15)
    private String status;

    // Quan hệ 1-Nhiều với danh sách khách thuê cùng ở trong hợp đồng này
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractTenant> contractTenants = new ArrayList<>();

    public Contract() {
    }

    public Contract(Integer contractId, String contractCode, Room room, Tenant representative, LocalDate startDate, LocalDate endDate, BigDecimal depositAmount, Short billingCycleMonths, String status, List<ContractTenant> contractTenants) {
        this.contractId = contractId;
        this.contractCode = contractCode;
        this.room = room;
        this.representative = representative;
        this.startDate = startDate;
        this.endDate = endDate;
        this.depositAmount = depositAmount;
        this.billingCycleMonths = billingCycleMonths;
        this.status = status;
        this.contractTenants = contractTenants != null ? contractTenants : new ArrayList<>();
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public String getContractCode() {
        return contractCode;
    }

    public void setContractCode(String contractCode) {
        this.contractCode = contractCode;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Tenant getRepresentative() {
        return representative;
    }

    public void setRepresentative(Tenant representative) {
        this.representative = representative;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public Short getBillingCycleMonths() {
        return billingCycleMonths;
    }

    public void setBillingCycleMonths(Short billingCycleMonths) {
        this.billingCycleMonths = billingCycleMonths;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ContractTenant> getContractTenants() {
        return contractTenants;
    }

    public void setContractTenants(List<ContractTenant> contractTenants) {
        this.contractTenants = contractTenants;
    }

    public static class ContractBuilder {
        private Integer contractId;
        private String contractCode;
        private Room room;
        private Tenant representative;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal depositAmount;
        private Short billingCycleMonths;
        private String status;
        private List<ContractTenant> contractTenants = new ArrayList<>();

        ContractBuilder() {}

        public ContractBuilder contractId(Integer contractId) {
            this.contractId = contractId;
            return this;
        }

        public ContractBuilder contractCode(String contractCode) {
            this.contractCode = contractCode;
            return this;
        }

        public ContractBuilder room(Room room) {
            this.room = room;
            return this;
        }

        public ContractBuilder representative(Tenant representative) {
            this.representative = representative;
            return this;
        }

        public ContractBuilder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public ContractBuilder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public ContractBuilder depositAmount(BigDecimal depositAmount) {
            this.depositAmount = depositAmount;
            return this;
        }

        public ContractBuilder billingCycleMonths(Short billingCycleMonths) {
            this.billingCycleMonths = billingCycleMonths;
            return this;
        }

        public ContractBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ContractBuilder contractTenants(List<ContractTenant> contractTenants) {
            this.contractTenants = contractTenants;
            return this;
        }

        public Contract build() {
            return new Contract(contractId, contractCode, room, representative, startDate, endDate, depositAmount, billingCycleMonths, status, contractTenants);
        }
    }

    public static ContractBuilder builder() {
        return new ContractBuilder();
    }
}
