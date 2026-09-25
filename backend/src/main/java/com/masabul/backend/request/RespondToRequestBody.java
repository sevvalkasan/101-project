package com.masabul.backend.request;

import jakarta.validation.constraints.NotNull;

public record RespondToRequestBody(
    @NotNull
    RequestDecision decision
) {
}
