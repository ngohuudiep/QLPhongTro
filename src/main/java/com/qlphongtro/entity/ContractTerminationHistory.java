package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_termination_histories")
public class ContractTerminationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_request_id", nullable = false)
    private ContractTerminationRequest terminationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "previous_status", length = 30, columnDefinition = "NVARCHAR(30)")
    private String previousStatus;

    @Column(name = "new_status", length = 30, columnDefinition = "NVARCHAR(30)")
    private String newStatus;

    @Column(name = "details", length = 2000, columnDefinition = "NVARCHAR(2000)")
    private String details;

    @Column(name = "happened_at", nullable = false)
    private LocalDateTime happenedAt = LocalDateTime.now();

    public Integer getHistoryId() { return historyId; }
    public void setHistoryId(Integer historyId) { this.historyId = historyId; }
    public ContractTerminationRequest getTerminationRequest() { return terminationRequest; }
    public void setTerminationRequest(ContractTerminationRequest terminationRequest) { this.terminationRequest = terminationRequest; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getHappenedAt() { return happenedAt; }
    public void setHappenedAt(LocalDateTime happenedAt) { this.happenedAt = happenedAt; }
}
