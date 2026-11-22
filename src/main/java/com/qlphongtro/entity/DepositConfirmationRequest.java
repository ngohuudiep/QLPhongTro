package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_confirmation_requests", indexes = {
        @Index(name = "IX_deposit_confirmation_requests_landlord_status",
                columnList = "landlord_user_id, request_status, requested_at")
})
public class DepositConfirmationRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_user_id", nullable = false)
    private User tenantUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_user_id", nullable = false)
    private User landlordUser;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "request_status", nullable = false, length = 20)
    private String status = STATUS_PENDING;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_user_id")
    private User respondedBy;

    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(MAX)")
    private String rejectionReason;

    @Column(name = "response_reference", length = 50)
    private String responseReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public User getTenantUser() { return tenantUser; }
    public void setTenantUser(User tenantUser) { this.tenantUser = tenantUser; }
    public User getLandlordUser() { return landlordUser; }
    public void setLandlordUser(User landlordUser) { this.landlordUser = landlordUser; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public User getRespondedBy() { return respondedBy; }
    public void setRespondedBy(User respondedBy) { this.respondedBy = respondedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getResponseReference() { return responseReference; }
    public void setResponseReference(String responseReference) { this.responseReference = responseReference; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public String getStatusLabel() {
        if (status == null) return "Không xác định";
        return switch (status.toUpperCase(java.util.Locale.ROOT)) {
            case STATUS_PENDING -> "Chờ chủ trọ xác nhận";
            case STATUS_APPROVED -> "Đã xác nhận nhận tiền";
            case STATUS_REJECTED -> "Bị từ chối";
            default -> status;
        };
    }
}
