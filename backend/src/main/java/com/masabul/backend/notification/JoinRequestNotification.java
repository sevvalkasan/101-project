package com.masabul.backend.notification;

import java.util.UUID;

public record JoinRequestNotification(UUID hostId, UUID tableId) {
}
