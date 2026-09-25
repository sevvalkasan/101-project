package com.masabul.backend.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
    @NotBlank
    @Size(max = 500)
    String message
) {
}
