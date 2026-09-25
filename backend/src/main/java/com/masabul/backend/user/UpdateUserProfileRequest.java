package com.masabul.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    @NotBlank
    @Size(max = 60)
    String fullName,

    @Size(max = 2048)
    String avatarUrl
) {
}
