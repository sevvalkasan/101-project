package com.masabul.backend.chat;

import com.masabul.backend.table.HostSummaryResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageResponse(
    UUID id,
    UUID tableId,
    HostSummaryResponse sender,
    String message,
    OffsetDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
            chatMessage.getId(),
            chatMessage.getTable().getId(),
            new HostSummaryResponse(
                chatMessage.getSender().getId(),
                chatMessage.getSender().getFullName(),
                chatMessage.getSender().getAvatarUrl()
            ),
            chatMessage.getMessage(),
            chatMessage.getCreatedAt()
        );
    }
}
