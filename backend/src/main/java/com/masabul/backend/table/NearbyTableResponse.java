package com.masabul.backend.table;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NearbyTableResponse(
    UUID id,
    String title,
    String venueName,
    HostSummaryResponse host,
    double distanceMeters,
    int availableSpots,
    OffsetDateTime createdAt
) {

    public static NearbyTableResponse from(NearbyTableProjection projection) {
        return new NearbyTableResponse(
            projection.getId(),
            projection.getTitle(),
            projection.getVenueName(),
            new HostSummaryResponse(
                projection.getHostId(),
                projection.getHostFullName(),
                projection.getHostAvatarUrl()
            ),
            projection.getDistanceMeters(),
            projection.getAvailableSpots(),
            projection.getCreatedAt()
        );
    }
}
