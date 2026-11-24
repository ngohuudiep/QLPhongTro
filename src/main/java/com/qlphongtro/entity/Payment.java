package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    // Quan hệ ManyToOne tới Invoices
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "transaction_code", length = 80)
    private String transactionCode;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "provider", length = 30)
    private String provider;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt = LocalDateTime.now();

    @Column(name = "method", nullable = false, length = 15)
    private String method;

    @Column(name = "reference_no", length = 50)
    private String referenceNo;

    public Payment() {
    }

    public Payment(Integer paymentId, Invoice invoice, BigDecimal amount, LocalDateTime paidAt, String method, String referenceNo) {
        this.paymentId = paymentId;
        this.invoice = invoice;
        this.amount = amount;
        this.paidAt = paidAt != null ? paidAt : LocalDateTime.now();
        this.method = method;
        this.referenceNo = referenceNo;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getStatusLabel() {
        if (status == null) return "Không xác định";
        return switch (status.toUpperCase(java.util.Locale.ROOT)) {
            case "PENDING" -> "Chưa thanh toán / chờ xác nhận";
            case "PROCESSING" -> "Đang xử lý";
            case "SUCCESS" -> "Đã thanh toán";
            case "FAILED", "FAILURE" -> "Thất bại";
            default -> status;
        };
    }

    public static class PaymentBuilder {
        private Integer paymentId;
        private Invoice invoice;
        private BigDecimal amount;
        private LocalDateTime paidAt = LocalDateTime.now();
        private String method;
        private String referenceNo;

        PaymentBuilder() {}

        public PaymentBuilder paymentId(Integer paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public PaymentBuilder invoice(Invoice invoice) {
            this.invoice = invoice;
            return this;
        }

        public PaymentBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public PaymentBuilder method(String method) {
            this.method = method;
            return this;
        }

        public PaymentBuilder referenceNo(String referenceNo) {
            this.referenceNo = referenceNo;
            return this;
        }

        public Payment build() {
            return new Payment(paymentId, invoice, amount, paidAt, method, referenceNo);
        }
    }

    public static PaymentBuilder builder() {
        return new PaymentBuilder();
    }
}
