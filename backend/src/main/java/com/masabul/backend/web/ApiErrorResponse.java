package com.masabul.backend.web;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String message,
    List<String> validationErrors,
    String path
) {
}
