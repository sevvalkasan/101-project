package com.masabul.backend.request;

import com.masabul.backend.table.HostSummaryResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TableRequestResponse(
    UUID requestId,
    UUID tableId,
    HostSummaryResponse user,
    TableRequestStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime respondedAt
) {

    public static TableRequestResponse from(TableRequest request) {
        return new TableRequestResponse(
            request.getId(),
            request.getTable().getId(),
            new HostSummaryResponse(
                request.getUser().getId(),
                request.getUser().getFullName(),
                request.getUser().getAvatarUrl()
            ),
            request.getStatus(),
            request.getCreatedAt(),
            request.getRespondedAt()
        );
    }
}
