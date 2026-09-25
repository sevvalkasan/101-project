package com.masabul.backend.table;

import java.util.UUID;

public record HostSummaryResponse(
    UUID id,
    String fullName,
    String avatarUrl
) {
}
