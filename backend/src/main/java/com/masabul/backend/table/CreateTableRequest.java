package com.masabul.backend.table;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(
    @NotBlank
    @Size(max = 100)
    String title,

    @NotBlank
    @Size(max = 100)
    String venueName,

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    Double latitude,

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    Double longitude
) {
}
