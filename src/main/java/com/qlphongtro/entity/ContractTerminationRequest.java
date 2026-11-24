package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_termination_requests")
public class ContractTerminationRequest {

    public static final String PENDING_RESPONSE = "PENDING_RESPONSE";
    public static final String PENDING_SETTLEMENT = "PENDING_SETTLEMENT";
    public static final String DISPUTED = "DISPUTED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "termination_request_id")
    private Integer terminationRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_user_id", nullable = false)
    private User responder;

    @Column(name = "requester_role", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String requesterRole;

    @Column(name = "original_contract_status", nullable = false, length = 30, columnDefinition = "NVARCHAR(30)")
    private String originalContractStatus;

    @Column(name = "reason_code", nullable = false, length = 60, columnDefinition = "NVARCHAR(60)")
    private String reasonCode;

    @Column(name = "reason_details", nullable = false, length = 1500, columnDefinition = "NVARCHAR(1500)")
    private String reasonDetails;

    @Column(name = "notes", length = 2000, columnDefinition = "NVARCHAR(2000)")
    private String notes;

    @Column(name = "response_reason", length = 1500, columnDefinition = "NVARCHAR(1500)")
    private String responseReason;

    @Column(name = "expected_termination_date", nullable = false)
    private LocalDate expectedTerminationDate;

    @Column(name = "status", nullable = false, length = 30, columnDefinition = "NVARCHAR(30)")
    private String status = PENDING_RESPONSE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToOne(mappedBy = "terminationRequest", fetch = FetchType.LAZY)
    private ContractSettlement settlement;

    public Integer getTerminationRequestId() { return terminationRequestId; }
    public void setTerminationRequestId(Integer terminationRequestId) { this.terminationRequestId = terminationRequestId; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public User getResponder() { return responder; }
    public void setResponder(User responder) { this.responder = responder; }
    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String requesterRole) { this.requesterRole = requesterRole; }
    public String getOriginalContractStatus() { return originalContractStatus; }
    public void setOriginalContractStatus(String originalContractStatus) { this.originalContractStatus = originalContractStatus; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonDetails() { return reasonDetails; }
    public void setReasonDetails(String reasonDetails) { this.reasonDetails = reasonDetails; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getResponseReason() { return responseReason; }
    public void setResponseReason(String responseReason) { this.responseReason = responseReason; }
    public LocalDate getExpectedTerminationDate() { return expectedTerminationDate; }
    public void setExpectedTerminationDate(LocalDate expectedTerminationDate) { this.expectedTerminationDate = expectedTerminationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public ContractSettlement getSettlement() { return settlement; }
    public void setSettlement(ContractSettlement settlement) { this.settlement = settlement; }
}
