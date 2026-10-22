package com.masabul.backend.table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record NearbyTableResponse(
    UUID id,
    String title,
    String venueName,
    HostSummaryResponse host,
    double latitude,
    double longitude,
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
            projection.getLatitude(),
            projection.getLongitude(),
            projection.getDistanceMeters(),
            projection.getAvailableSpots(),
            projection.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
