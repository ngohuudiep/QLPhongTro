package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @JoinColumn(name = "representative_id")
    private Tenant representative;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "deposit_amount", precision = 14, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "monthly_rent", precision = 14, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "billing_cycle_months")
    private Short billingCycleMonths;

    @Column(name = "status", length = 30, columnDefinition = "NVARCHAR(30)")
    private String status;

    @Column(name = "contract_type", length = 50, columnDefinition = "NVARCHAR(50)")
    private String contractType;

    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column(name = "payment_day")
    private Short paymentDay;

    @Column(name = "monthly_electric_price", precision = 14, scale = 2)
    private BigDecimal monthlyElectricPrice;

    @Column(name = "monthly_water_price", precision = 14, scale = 2)
    private BigDecimal monthlyWaterPrice;

    @Column(name = "internet_fee", precision = 14, scale = 2)
    private BigDecimal internetFee;

    @Column(name = "service_fee", precision = 14, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "sanitation_fee", precision = 14, scale = 2)
    private BigDecimal sanitationFee;

    @Column(name = "other_service_fee", precision = 14, scale = 2)
    private BigDecimal otherServiceFee;

    @Column(name = "payment_method", length = 100, columnDefinition = "NVARCHAR(100)")
    private String paymentMethod;

    @Column(name = "payment_terms", columnDefinition = "NVARCHAR(MAX)")
    private String paymentTerms;

    @Column(name = "deposit_terms", columnDefinition = "NVARCHAR(MAX)")
    private String depositTerms;

    @Column(name = "utility_terms", columnDefinition = "NVARCHAR(MAX)")
    private String utilityTerms;

    @Column(name = "asset_terms", columnDefinition = "NVARCHAR(MAX)")
    private String assetTerms;

    @Column(name = "repair_terms", columnDefinition = "NVARCHAR(MAX)")
    private String repairTerms;

    @Column(name = "termination_terms", columnDefinition = "NVARCHAR(MAX)")
    private String terminationTerms;

    @Column(name = "occupant_terms", columnDefinition = "NVARCHAR(MAX)")
    private String occupantTerms;

    @Column(name = "renewal_terms", columnDefinition = "NVARCHAR(MAX)")
    private String renewalTerms;

    @Column(name = "landlord_responsibilities", columnDefinition = "NVARCHAR(MAX)")
    private String landlordResponsibilities;

    @Column(name = "tenant_responsibilities", columnDefinition = "NVARCHAR(MAX)")
    private String tenantResponsibilities;

    @Column(name = "additional_terms", columnDefinition = "NVARCHAR(MAX)")
    private String additionalTerms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_request_id")
    private BookingRequest rentalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_user_id")
    private User tenantUser;

    @Column(name = "tenant_accepted_at")
    private LocalDateTime tenantAcceptedAt;

    @Column(name = "landlord_approved_at")
    private LocalDateTime landlordApprovedAt;

    @Column(name = "tenant_rejection_reason", columnDefinition = "NVARCHAR(MAX)")
    private String tenantRejectionReason;

    @Column(name = "landlord_rejection_reason", columnDefinition = "NVARCHAR(MAX)")
    private String landlordRejectionReason;

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

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(BigDecimal monthlyRent) { this.monthlyRent = monthlyRent; }

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

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }
    public Short getPaymentDay() { return paymentDay; }
    public void setPaymentDay(Short paymentDay) { this.paymentDay = paymentDay; }
    public BigDecimal getMonthlyElectricPrice() { return monthlyElectricPrice; }
    public void setMonthlyElectricPrice(BigDecimal monthlyElectricPrice) { this.monthlyElectricPrice = monthlyElectricPrice; }
    public BigDecimal getMonthlyWaterPrice() { return monthlyWaterPrice; }
    public void setMonthlyWaterPrice(BigDecimal monthlyWaterPrice) { this.monthlyWaterPrice = monthlyWaterPrice; }
    public BigDecimal getInternetFee() { return internetFee; }
    public void setInternetFee(BigDecimal internetFee) { this.internetFee = internetFee; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }
    public BigDecimal getSanitationFee() { return sanitationFee; }
    public void setSanitationFee(BigDecimal sanitationFee) { this.sanitationFee = sanitationFee; }
    public BigDecimal getOtherServiceFee() { return otherServiceFee; }
    public void setOtherServiceFee(BigDecimal otherServiceFee) { this.otherServiceFee = otherServiceFee; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public String getDepositTerms() { return depositTerms; }
    public void setDepositTerms(String depositTerms) { this.depositTerms = depositTerms; }
    public String getUtilityTerms() { return utilityTerms; }
    public void setUtilityTerms(String utilityTerms) { this.utilityTerms = utilityTerms; }
    public String getAssetTerms() { return assetTerms; }
    public void setAssetTerms(String assetTerms) { this.assetTerms = assetTerms; }
    public String getRepairTerms() { return repairTerms; }
    public void setRepairTerms(String repairTerms) { this.repairTerms = repairTerms; }
    public String getTerminationTerms() { return terminationTerms; }
    public void setTerminationTerms(String terminationTerms) { this.terminationTerms = terminationTerms; }
    public String getOccupantTerms() { return occupantTerms; }
    public void setOccupantTerms(String occupantTerms) { this.occupantTerms = occupantTerms; }
    public String getRenewalTerms() { return renewalTerms; }
    public void setRenewalTerms(String renewalTerms) { this.renewalTerms = renewalTerms; }
    public String getLandlordResponsibilities() { return landlordResponsibilities; }
    public void setLandlordResponsibilities(String landlordResponsibilities) { this.landlordResponsibilities = landlordResponsibilities; }
    public String getTenantResponsibilities() { return tenantResponsibilities; }
    public void setTenantResponsibilities(String tenantResponsibilities) { this.tenantResponsibilities = tenantResponsibilities; }
    public String getAdditionalTerms() { return additionalTerms; }
    public void setAdditionalTerms(String additionalTerms) { this.additionalTerms = additionalTerms; }
    public BookingRequest getRentalRequest() { return rentalRequest; }
    public void setRentalRequest(BookingRequest rentalRequest) { this.rentalRequest = rentalRequest; }
    public User getTenantUser() { return tenantUser; }
    public void setTenantUser(User tenantUser) { this.tenantUser = tenantUser; }
    public LocalDateTime getTenantAcceptedAt() { return tenantAcceptedAt; }
    public void setTenantAcceptedAt(LocalDateTime tenantAcceptedAt) { this.tenantAcceptedAt = tenantAcceptedAt; }
    public LocalDateTime getLandlordApprovedAt() { return landlordApprovedAt; }
    public void setLandlordApprovedAt(LocalDateTime landlordApprovedAt) { this.landlordApprovedAt = landlordApprovedAt; }
    public String getTenantRejectionReason() { return tenantRejectionReason; }
    public void setTenantRejectionReason(String tenantRejectionReason) { this.tenantRejectionReason = tenantRejectionReason; }
    public String getLandlordRejectionReason() { return landlordRejectionReason; }
    public void setLandlordRejectionReason(String landlordRejectionReason) { this.landlordRejectionReason = landlordRejectionReason; }

    public String getTenantFullName() {
        if (representative != null && representative.getFullName() != null) return representative.getFullName();
        if (rentalRequest != null && rentalRequest.getFullName() != null) return rentalRequest.getFullName();
        return tenantUser != null ? tenantUser.getFullName() : null;
    }

    public String getTenantEmail() {
        if (representative != null && representative.getEmail() != null) return representative.getEmail();
        if (tenantUser != null && tenantUser.getEmail() != null) return tenantUser.getEmail();
        return null;
    }

    public String getTenantPhone() {
        if (representative != null && representative.getPhone() != null) return representative.getPhone();
        if (rentalRequest != null && rentalRequest.getPhone() != null) return rentalRequest.getPhone();
        return tenantUser != null ? tenantUser.getPhone() : null;
    }

    public String getTenantIdNumber() {
        if (representative != null) return representative.getIdNumber();
        return rentalRequest != null ? rentalRequest.getIdNumber() : null;
    }

    public String getTenantAddress() {
        return representative != null ? representative.getHometown() : null;
    }

    public String getLandlordFullName() {
        return room != null && room.getBuilding() != null && room.getBuilding().getOwner() != null
                ? room.getBuilding().getOwner().getFullName() : null;
    }

    public String getLandlordPhone() {
        return room != null && room.getBuilding() != null && room.getBuilding().getOwner() != null
                ? room.getBuilding().getOwner().getPhone() : null;
    }

    public Short getExpectedOccupants() {
        return rentalRequest != null ? rentalRequest.getExpectedOccupants() : null;
    }

    public String getRoomAmenities() {
        return rentalRequest != null && rentalRequest.getRoom() != null
                ? rentalRequest.getRoom().getAmenities() : null;
    }

    public String getStatusLabel() {
        if (status == null) return "Không xác định";
        return switch (status.toUpperCase(java.util.Locale.ROOT)) {
            case "DRAFT" -> "Bản nháp";
            case "SENT_TO_TENANT", "PENDING_TENANT_CONFIRMATION" -> "Chờ người thuê đồng ý";
            case "TENANT_ACCEPTED" -> "Người thuê đã đồng ý";
            case "TENANT_REJECTED", "REJECTED_BY_TENANT" -> "Người thuê từ chối";
            case "EXPIRED" -> "Đã hết hạn";
            case "TERMINATED" -> "Đã kết thúc";
            case "TERMINATION_REQUESTED" -> "Đang chờ thanh lý";
            case "ACTIVE", "HIỆU LỰC" -> "Đang hiệu lực";
            case "PENDING_DEPOSIT" -> "Chờ xử lý tiền cọc";
            case "PENDING_FINAL_LANDLORD_APPROVAL" -> "Chờ chủ trọ xác nhận cuối";
            case "REJECTED_BY_LANDLORD" -> "Chủ trọ từ chối";
            default -> status;
        };
    }

    public boolean isTerminationRequestAllowed() {
        if (actualEndDate != null || status == null) {
            return false;
        }
        return switch (status.toUpperCase(java.util.Locale.ROOT)) {
            case "ACTIVE", "HIỆU LỰC", "ĐANG HIỆU LỰC", "TENANT_ACCEPTED" -> true;
            default -> false;
        };
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
