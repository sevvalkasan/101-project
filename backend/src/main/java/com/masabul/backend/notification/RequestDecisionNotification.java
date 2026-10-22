package com.masabul.backend.notification;

import com.masabul.backend.request.TableRequestStatus;
import java.util.UUID;

public record RequestDecisionNotification(
    UUID playerId,
    UUID tableId,
    TableRequestStatus status
) {
}
