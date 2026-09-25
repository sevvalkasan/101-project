package com.masabul.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class User {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    private String firebaseUid;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "full_name", nullable = false, length = 60)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected User() {
    }

    public User(String firebaseUid, String phone, String fullName, String avatarUrl, boolean profileCompleted) {
        this.firebaseUid = firebaseUid;
        this.phone = phone;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.profileCompleted = profileCompleted;
    }

    public UUID getId() {
        return id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getPhone() {
        return phone;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateProfile(String fullName, String avatarUrl) {
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.profileCompleted = true;
    }
}
