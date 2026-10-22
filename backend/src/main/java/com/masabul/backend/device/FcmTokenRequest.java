package com.masabul.backend.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FcmTokenRequest(
    @NotBlank
    @Size(max = 4096)
    String token,

    @NotNull
    DevicePlatform platform
) {
}
