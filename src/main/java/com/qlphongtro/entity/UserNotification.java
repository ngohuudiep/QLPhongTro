package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_notifications")
public class UserNotification {

    public static final String TYPE_INVOICE = "INVOICE";
    public static final String TYPE_RENTAL_REQUEST = "RENTAL_REQUEST";
    public static final String TYPE_CONTRACT = "CONTRACT";
    public static final String TYPE_DEPOSIT = "DEPOSIT";
    public static final String TYPE_CONTRACT_TERMINATION = "CONTRACT_TERMINATION";
    public static final String TYPE_ACCOUNT_REVIEW = "ACCOUNT_REVIEW";
    public static final String TYPE_REVIEW = "REVIEW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_request_id")
    private BookingRequest rentalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_request_id")
    private ContractTerminationRequest terminationRequest;

    @Column(name = "notification_type", nullable = false, length = 30)
    private String notificationType = TYPE_INVOICE;

    @Column(name = "title", nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public BookingRequest getRentalRequest() { return rentalRequest; }
    public void setRentalRequest(BookingRequest rentalRequest) { this.rentalRequest = rentalRequest; }
    public ContractTerminationRequest getTerminationRequest() { return terminationRequest; }
    public void setTerminationRequest(ContractTerminationRequest terminationRequest) { this.terminationRequest = terminationRequest; }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }
}
