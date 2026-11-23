package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    public static final String STATUS_VISIBLE = "VISIBLE";
    public static final String STATUS_HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_user_id", nullable = false)
    private User tenantUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_user_id", nullable = false)
    private User landlordUser;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", nullable = false, columnDefinition = "NVARCHAR(2000)")
    private String comment;

    @Column(name = "reply_comment", columnDefinition = "NVARCHAR(2000)")
    private String replyComment;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_VISIBLE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Review() {}

    public Review(Contract contract, Room room, User tenantUser, User landlordUser, Integer rating, String comment) {
        this.contract = contract;
        this.room = room;
        this.tenantUser = tenantUser;
        this.landlordUser = landlordUser;
        this.rating = rating;
        this.comment = comment;
        this.status = STATUS_VISIBLE;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }

    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getTenantUser() { return tenantUser; }
    public void setTenantUser(User tenantUser) { this.tenantUser = tenantUser; }

    public User getLandlordUser() { return landlordUser; }
    public void setLandlordUser(User landlordUser) { this.landlordUser = landlordUser; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getReplyComment() { return replyComment; }
    public void setReplyComment(String replyComment) { this.replyComment = replyComment; }

    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isVisible() {
        return STATUS_VISIBLE.equalsIgnoreCase(this.status);
    }
}
