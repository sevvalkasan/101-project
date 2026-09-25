package com.masabul.backend.request;

import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "table_requests",
    uniqueConstraints = @UniqueConstraint(name = "uq_table_requests_table_user", columnNames = {"table_id", "user_id"})
)
public class TableRequest {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private OkeyTable table;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "table_request_status")
    private TableRequestStatus status = TableRequestStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    protected TableRequest() {
    }

    public TableRequest(OkeyTable table, User user) {
        this.table = table;
        this.user = user;
        this.status = TableRequestStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public OkeyTable getTable() {
        return table;
    }

    public User getUser() {
        return user;
    }

    public TableRequestStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public boolean isPending() {
        return status == TableRequestStatus.PENDING;
    }

    public void accept() {
        if (!isPending()) {
            throw new IllegalStateException("Request is not pending");
        }
        status = TableRequestStatus.ACCEPTED;
        respondedAt = OffsetDateTime.now();
    }

    public void reject() {
        if (!isPending()) {
            throw new IllegalStateException("Request is not pending");
        }
        status = TableRequestStatus.REJECTED;
        respondedAt = OffsetDateTime.now();
    }

    public void rejectIfPending() {
        if (isPending()) {
            status = TableRequestStatus.REJECTED;
            respondedAt = OffsetDateTime.now();
        }
    }
}
