package com.masabul.backend.chat;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/tables")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat/{tableId}")
    @SendTo("/topic/table/{tableId}")
    public ChatMessageResponse sendMessage(
        AuthenticatedUserPrincipal principal,
        @DestinationVariable UUID tableId,
        @Valid @Payload ChatMessageRequest request
    ) {
        return chatService.sendMessage(principal, tableId, request);
    }

    @GetMapping("/{tableId}/messages")
    public List<ChatMessageResponse> history(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @PathVariable UUID tableId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return chatService.history(principal, tableId, page, size);
    }
}
