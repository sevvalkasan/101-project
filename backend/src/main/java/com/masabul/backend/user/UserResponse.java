package com.masabul.backend.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String firebaseUid,
    String phone,
    String fullName,
    String avatarUrl,
    boolean profileCompleted,
    OffsetDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getFirebaseUid(),
            user.getPhone(),
            user.getFullName(),
            user.getAvatarUrl(),
            user.isProfileCompleted(),
            user.getCreatedAt()
        );
    }
}
