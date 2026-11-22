package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "Invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Integer invoiceId;

    @Column(name = "invoice_code", nullable = false, length = 30)
    private String invoiceCode;

    // Quan hệ ManyToOne tới Contracts
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    // Quan hệ 1-Nhiều với Payments (Lịch sử thanh toán của hóa đơn này)
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineItemId ASC")
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserNotification> notifications = new ArrayList<>();

    public Invoice() {
    }

    public Invoice(Integer invoiceId, String invoiceCode, Contract contract, LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate, BigDecimal totalAmount, String status, List<Payment> payments) {
        this.invoiceId = invoiceId;
        this.invoiceCode = invoiceCode;
        this.contract = contract;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dueDate = dueDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.payments = payments != null ? payments : new ArrayList<>();
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<InvoiceLineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<InvoiceLineItem> lineItems) {
        this.lineItems = lineItems == null ? new ArrayList<>() : lineItems;
    }

    public List<UserNotification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<UserNotification> notifications) {
        this.notifications = notifications == null ? new ArrayList<>() : notifications;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getEffectiveStatus() {
        if (isUnpaidStatus(status) && dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return "OVERDUE";
        }
        return status;
    }

    public String getStatusLabel() {
        String effectiveStatus = getEffectiveStatus();
        if (effectiveStatus == null) {
            return "Không xác định";
        }
        return switch (effectiveStatus.toUpperCase(Locale.ROOT)) {
            case "UNPAID", "CHƯA THANH TOÁN" -> "Chưa thanh toán";
            case "PAID", "ĐÃ THANH TOÁN" -> "Đã thanh toán";
            case "OVERDUE", "QUÁ HẠN" -> "Quá hạn";
            case "CANCELLED", "CANCELED", "ĐÃ HỦY" -> "Đã hủy";
            default -> effectiveStatus;
        };
    }

    public String getTenantFullName() {
        return contract != null ? contract.getTenantFullName() : null;
    }

    public BigDecimal getRoomCharge() {
        return sumLineItems("Tiền phòng");
    }

    public BigDecimal getElectricityCharge() {
        return sumLineItems("Tiền điện");
    }

    public BigDecimal getWaterCharge() {
        return sumLineItems("Tiền nước");
    }

    public BigDecimal getServiceCharge() {
        return sumLineItems(null, "Tiền phòng", "Tiền điện", "Tiền nước");
    }

    private BigDecimal sumLineItems(String exactName, String... excludedNames) {
        if (lineItems == null || lineItems.isEmpty()) {
            return null;
        }
        return lineItems.stream()
                .filter(line -> exactName == null
                        ? java.util.Arrays.stream(excludedNames)
                                .noneMatch(name -> name.equalsIgnoreCase(line.getName()))
                        : exactName.equalsIgnoreCase(line.getName()))
                .map(InvoiceLineItem::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean isUnpaidStatus(String value) {
        return "UNPAID".equalsIgnoreCase(value) || "CHƯA THANH TOÁN".equalsIgnoreCase(value);
    }

    public static class InvoiceBuilder {
        private Integer invoiceId;
        private String invoiceCode;
        private Contract contract;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private LocalDate dueDate;
        private BigDecimal totalAmount;
        private String status;
        private List<Payment> payments = new ArrayList<>();

        InvoiceBuilder() {}

        public InvoiceBuilder invoiceId(Integer invoiceId) {
            this.invoiceId = invoiceId;
            return this;
        }

        public InvoiceBuilder invoiceCode(String invoiceCode) {
            this.invoiceCode = invoiceCode;
            return this;
        }

        public InvoiceBuilder contract(Contract contract) {
            this.contract = contract;
            return this;
        }

        public InvoiceBuilder periodStart(LocalDate periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public InvoiceBuilder periodEnd(LocalDate periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public InvoiceBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public InvoiceBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public InvoiceBuilder status(String status) {
            this.status = status;
            return this;
        }

        public InvoiceBuilder payments(List<Payment> payments) {
            this.payments = payments;
            return this;
        }

        public Invoice build() {
            return new Invoice(invoiceId, invoiceCode, contract, periodStart, periodEnd, dueDate, totalAmount, status, payments);
        }
    }

    public static InvoiceBuilder builder() {
        return new InvoiceBuilder();
    }
}
