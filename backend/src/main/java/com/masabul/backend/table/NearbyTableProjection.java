package com.masabul.backend.table;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NearbyTableProjection {

    UUID getId();

    String getTitle();

    String getVenueName();

    UUID getHostId();

    String getHostFullName();

    String getHostAvatarUrl();

    Double getDistanceMeters();

    Integer getAvailableSpots();

    OffsetDateTime getCreatedAt();
}
