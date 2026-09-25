package com.masabul.backend.table;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TableResponse(
    UUID id,
    String title,
    String venueName,
    HostSummaryResponse host,
    int currentPlayers,
    int maxPlayers,
    int availableSpots,
    OkeyTableStatus status,
    OffsetDateTime createdAt
) {

    public static TableResponse from(OkeyTable table) {
        return new TableResponse(
            table.getId(),
            table.getTitle(),
            table.getVenueName(),
            new HostSummaryResponse(
                table.getHost().getId(),
                table.getHost().getFullName(),
                table.getHost().getAvatarUrl()
            ),
            table.getCurrentPlayers(),
            table.getMaxPlayers(),
            table.getMaxPlayers() - table.getCurrentPlayers(),
            table.getStatus(),
            table.getCreatedAt()
        );
    }
}
