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
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

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
