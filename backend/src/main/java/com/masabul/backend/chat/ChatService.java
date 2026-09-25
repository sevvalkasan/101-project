package com.masabul.backend.chat;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.table.OkeyTableRepository;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageRepository chatMessageRepository;
    private final OkeyTableRepository tableRepository;
    private final UserService userService;
    private final ChatAuthorizationService chatAuthorizationService;

    public ChatService(
        ChatMessageRepository chatMessageRepository,
        OkeyTableRepository tableRepository,
        UserService userService,
        ChatAuthorizationService chatAuthorizationService
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.tableRepository = tableRepository;
        this.userService = userService;
        this.chatAuthorizationService = chatAuthorizationService;
    }

    @Transactional
    public ChatMessageResponse sendMessage(AuthenticatedUserPrincipal principal, UUID tableId, ChatMessageRequest request) {
        User sender = userService.getCurrentUser(principal);
        OkeyTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new EntityNotFoundException("Table not found"));
        ensureCanAccessChat(table, sender);

        ChatMessage chatMessage = new ChatMessage(table, sender, request.message().trim());
        return ChatMessageResponse.from(chatMessageRepository.saveAndFlush(chatMessage));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history(AuthenticatedUserPrincipal principal, UUID tableId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        User user = userService.getCurrentUser(principal);
        OkeyTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new EntityNotFoundException("Table not found"));
        ensureCanAccessChat(table, user);

        return chatMessageRepository.findByTableOrderByCreatedAtAsc(
                table,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))
            )
            .stream()
            .map(ChatMessageResponse::from)
            .toList();
    }

    public void ensureCanAccessChat(OkeyTable table, User user) {
        if (!chatAuthorizationService.canAccessChat(table, user)) {
            throw new AccessDeniedException("Only host and accepted players can access table chat");
        }
    }
}
