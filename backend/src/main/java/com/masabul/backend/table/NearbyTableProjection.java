package com.masabul.backend.table;

import java.time.Instant;
import java.util.UUID;

public interface NearbyTableProjection {

    UUID getId();

    String getTitle();

    String getVenueName();

    UUID getHostId();

    String getHostFullName();

    String getHostAvatarUrl();

    Double getLatitude();

    Double getLongitude();

    Double getDistanceMeters();

    Integer getAvailableSpots();

    Instant getCreatedAt();
}
