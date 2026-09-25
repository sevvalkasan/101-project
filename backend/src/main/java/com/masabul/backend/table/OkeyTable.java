package com.masabul.backend.table;

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
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "okey_tables")
public class OkeyTable {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "venue_name", nullable = false, length = 100)
    private String venueName;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "current_players", nullable = false)
    private int currentPlayers = 1;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "okey_table_status")
    private OkeyTableStatus status = OkeyTableStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected OkeyTable() {
    }

    public OkeyTable(User host, String title, String venueName, Point location) {
        this.host = host;
        this.title = title;
        this.venueName = venueName;
        this.location = location;
        this.currentPlayers = 1;
        this.maxPlayers = 4;
        this.status = OkeyTableStatus.OPEN;
    }

    public UUID getId() {
        return id;
    }

    public User getHost() {
        return host;
    }

    public String getTitle() {
        return title;
    }

    public String getVenueName() {
        return venueName;
    }

    public Point getLocation() {
        return location;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public OkeyTableStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isHostedBy(User user) {
        return host.getId().equals(user.getId());
    }

    public boolean isOpen() {
        return status == OkeyTableStatus.OPEN;
    }

    public boolean hasAvailableSpot() {
        return currentPlayers < maxPlayers;
    }

    public void acceptPlayer() {
        if (!isOpen()) {
            throw new IllegalStateException("Table is not open");
        }
        if (!hasAvailableSpot()) {
            throw new IllegalStateException("Table is full");
        }
        currentPlayers++;
        if (currentPlayers == maxPlayers) {
            status = OkeyTableStatus.FULL;
        }
    }

    public void cancel() {
        if (!isOpen()) {
            throw new IllegalStateException("Only open tables can be cancelled");
        }
        status = OkeyTableStatus.CANCELLED;
    }
}
