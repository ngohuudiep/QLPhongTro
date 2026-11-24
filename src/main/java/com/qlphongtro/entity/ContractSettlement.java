package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_settlements")
public class ContractSettlement {

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String DISPUTED = "DISPUTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Integer settlementId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_request_id", nullable = false, unique = true)
    private ContractTerminationRequest terminationRequest;

    @Column(name = "unpaid_invoice_amount", precision = 14, scale = 2)
    private BigDecimal unpaidInvoiceAmount;

    @Column(name = "paid_invoice_amount", precision = 14, scale = 2)
    private BigDecimal paidInvoiceAmount;

    @Column(name = "rent_through_move_out_amount", precision = 14, scale = 2)
    private BigDecimal rentThroughMoveOutAmount;

    @Column(name = "electricity_old_reading", precision = 14, scale = 3)
    private BigDecimal electricityOldReading;

    @Column(name = "electricity_new_reading", precision = 14, scale = 3)
    private BigDecimal electricityNewReading;

    @Column(name = "electricity_unit_price", precision = 14, scale = 2)
    private BigDecimal electricityUnitPrice;

    @Column(name = "electricity_amount", precision = 14, scale = 2)
    private BigDecimal electricityAmount;

    @Column(name = "water_old_reading", precision = 14, scale = 3)
    private BigDecimal waterOldReading;

    @Column(name = "water_new_reading", precision = 14, scale = 3)
    private BigDecimal waterNewReading;

    @Column(name = "water_unit_price", precision = 14, scale = 2)
    private BigDecimal waterUnitPrice;

    @Column(name = "water_amount", precision = 14, scale = 2)
    private BigDecimal waterAmount;

    @Column(name = "service_fee_amount", precision = 14, scale = 2)
    private BigDecimal serviceFeeAmount;

    @Column(name = "compensation_amount", precision = 14, scale = 2)
    private BigDecimal compensationAmount;

    @Column(name = "compensation_reason", length = 1000, columnDefinition = "NVARCHAR(1000)")
    private String compensationReason;

    @Column(name = "deposit_amount", precision = 14, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "deposit_deduction_amount", precision = 14, scale = 2)
    private BigDecimal depositDeductionAmount;

    @Column(name = "deposit_deduction_reason", length = 1000, columnDefinition = "NVARCHAR(1000)")
    private String depositDeductionReason;

    @Column(name = "deposit_refund_amount", precision = 14, scale = 2)
    private BigDecimal depositRefundAmount;

    @Column(name = "total_due_amount", precision = 14, scale = 2)
    private BigDecimal totalDueAmount;

    @Column(name = "room_handover_condition", length = 1500, columnDefinition = "NVARCHAR(1500)")
    private String roomHandoverCondition;

    @Column(name = "damage_notes", length = 1500, columnDefinition = "NVARCHAR(1500)")
    private String damageNotes;

    @Column(name = "notes", length = 2000, columnDefinition = "NVARCHAR(2000)")
    private String notes;

    @Column(name = "status", nullable = false, length = 30, columnDefinition = "NVARCHAR(30)")
    private String status = DRAFT;

    @Column(name = "landlord_confirmed_at")
    private LocalDateTime landlordConfirmedAt;

    @Column(name = "tenant_confirmed_at")
    private LocalDateTime tenantConfirmedAt;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Column(name = "dispute_reason", length = 1500, columnDefinition = "NVARCHAR(1500)")
    private String disputeReason;

    @Column(name = "tenant_payment_reference", length = 100, columnDefinition = "NVARCHAR(100)")
    private String tenantPaymentReference;

    @Column(name = "tenant_payment_confirmed_at")
    private LocalDateTime tenantPaymentConfirmedAt;

    @Column(name = "deposit_refund_reference", length = 100, columnDefinition = "NVARCHAR(100)")
    private String depositRefundReference;

    @Column(name = "deposit_refunded_at")
    private LocalDateTime depositRefundedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getElectricityConsumption() {
        return electricityOldReading == null || electricityNewReading == null
                ? null : electricityNewReading.subtract(electricityOldReading);
    }

    public BigDecimal getWaterConsumption() {
        return waterOldReading == null || waterNewReading == null
                ? null : waterNewReading.subtract(waterOldReading);
    }

    public static BigDecimal calculateAmount(BigDecimal consumption, BigDecimal unitPrice) {
        return consumption.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getSettlementId() { return settlementId; }
    public void setSettlementId(Integer settlementId) { this.settlementId = settlementId; }
    public ContractTerminationRequest getTerminationRequest() { return terminationRequest; }
    public void setTerminationRequest(ContractTerminationRequest terminationRequest) { this.terminationRequest = terminationRequest; }
    public BigDecimal getUnpaidInvoiceAmount() { return unpaidInvoiceAmount; }
    public void setUnpaidInvoiceAmount(BigDecimal unpaidInvoiceAmount) { this.unpaidInvoiceAmount = unpaidInvoiceAmount; }
    public BigDecimal getPaidInvoiceAmount() { return paidInvoiceAmount; }
    public void setPaidInvoiceAmount(BigDecimal paidInvoiceAmount) { this.paidInvoiceAmount = paidInvoiceAmount; }
    public BigDecimal getRentThroughMoveOutAmount() { return rentThroughMoveOutAmount; }
    public void setRentThroughMoveOutAmount(BigDecimal rentThroughMoveOutAmount) { this.rentThroughMoveOutAmount = rentThroughMoveOutAmount; }
    public BigDecimal getElectricityOldReading() { return electricityOldReading; }
    public void setElectricityOldReading(BigDecimal electricityOldReading) { this.electricityOldReading = electricityOldReading; }
    public BigDecimal getElectricityNewReading() { return electricityNewReading; }
    public void setElectricityNewReading(BigDecimal electricityNewReading) { this.electricityNewReading = electricityNewReading; }
    public BigDecimal getElectricityUnitPrice() { return electricityUnitPrice; }
    public void setElectricityUnitPrice(BigDecimal electricityUnitPrice) { this.electricityUnitPrice = electricityUnitPrice; }
    public BigDecimal getElectricityAmount() { return electricityAmount; }
    public void setElectricityAmount(BigDecimal electricityAmount) { this.electricityAmount = electricityAmount; }
    public BigDecimal getWaterOldReading() { return waterOldReading; }
    public void setWaterOldReading(BigDecimal waterOldReading) { this.waterOldReading = waterOldReading; }
    public BigDecimal getWaterNewReading() { return waterNewReading; }
    public void setWaterNewReading(BigDecimal waterNewReading) { this.waterNewReading = waterNewReading; }
    public BigDecimal getWaterUnitPrice() { return waterUnitPrice; }
    public void setWaterUnitPrice(BigDecimal waterUnitPrice) { this.waterUnitPrice = waterUnitPrice; }
    public BigDecimal getWaterAmount() { return waterAmount; }
    public void setWaterAmount(BigDecimal waterAmount) { this.waterAmount = waterAmount; }
    public BigDecimal getServiceFeeAmount() { return serviceFeeAmount; }
    public void setServiceFeeAmount(BigDecimal serviceFeeAmount) { this.serviceFeeAmount = serviceFeeAmount; }
    public BigDecimal getCompensationAmount() { return compensationAmount; }
    public void setCompensationAmount(BigDecimal compensationAmount) { this.compensationAmount = compensationAmount; }
    public String getCompensationReason() { return compensationReason; }
    public void setCompensationReason(String compensationReason) { this.compensationReason = compensationReason; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getDepositDeductionAmount() { return depositDeductionAmount; }
    public void setDepositDeductionAmount(BigDecimal depositDeductionAmount) { this.depositDeductionAmount = depositDeductionAmount; }
    public String getDepositDeductionReason() { return depositDeductionReason; }
    public void setDepositDeductionReason(String depositDeductionReason) { this.depositDeductionReason = depositDeductionReason; }
    public BigDecimal getDepositRefundAmount() { return depositRefundAmount; }
    public void setDepositRefundAmount(BigDecimal depositRefundAmount) { this.depositRefundAmount = depositRefundAmount; }
    public BigDecimal getTotalDueAmount() { return totalDueAmount; }
    public void setTotalDueAmount(BigDecimal totalDueAmount) { this.totalDueAmount = totalDueAmount; }
    public String getRoomHandoverCondition() { return roomHandoverCondition; }
    public void setRoomHandoverCondition(String roomHandoverCondition) { this.roomHandoverCondition = roomHandoverCondition; }
    public String getDamageNotes() { return damageNotes; }
    public void setDamageNotes(String damageNotes) { this.damageNotes = damageNotes; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLandlordConfirmedAt() { return landlordConfirmedAt; }
    public void setLandlordConfirmedAt(LocalDateTime landlordConfirmedAt) { this.landlordConfirmedAt = landlordConfirmedAt; }
    public LocalDateTime getTenantConfirmedAt() { return tenantConfirmedAt; }
    public void setTenantConfirmedAt(LocalDateTime tenantConfirmedAt) { this.tenantConfirmedAt = tenantConfirmedAt; }
    public LocalDateTime getDisputedAt() { return disputedAt; }
    public void setDisputedAt(LocalDateTime disputedAt) { this.disputedAt = disputedAt; }
    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }
    public String getTenantPaymentReference() { return tenantPaymentReference; }
    public void setTenantPaymentReference(String tenantPaymentReference) { this.tenantPaymentReference = tenantPaymentReference; }
    public LocalDateTime getTenantPaymentConfirmedAt() { return tenantPaymentConfirmedAt; }
    public void setTenantPaymentConfirmedAt(LocalDateTime tenantPaymentConfirmedAt) { this.tenantPaymentConfirmedAt = tenantPaymentConfirmedAt; }
    public String getDepositRefundReference() { return depositRefundReference; }
    public void setDepositRefundReference(String depositRefundReference) { this.depositRefundReference = depositRefundReference; }
    public LocalDateTime getDepositRefundedAt() { return depositRefundedAt; }
    public void setDepositRefundedAt(LocalDateTime depositRefundedAt) { this.depositRefundedAt = depositRefundedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
