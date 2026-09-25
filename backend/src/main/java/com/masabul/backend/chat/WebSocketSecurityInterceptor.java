package com.masabul.backend.chat;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.security.FirebaseTokenVerificationException;
import com.masabul.backend.security.FirebaseTokenVerifier;
import com.masabul.backend.security.VerifiedFirebaseToken;
import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.table.OkeyTableRepository;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserProvisioningService;
import com.masabul.backend.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final Pattern TABLE_TOPIC = Pattern.compile("^/topic/table/([0-9a-fA-F-]{36})$");
    private static final Pattern CHAT_DESTINATION = Pattern.compile("^/app/chat/([0-9a-fA-F-]{36})$");

    private final FirebaseTokenVerifier tokenVerifier;
    private final UserProvisioningService userProvisioningService;
    private final UserService userService;
    private final OkeyTableRepository tableRepository;
    private final ChatService chatService;

    public WebSocketSecurityInterceptor(
        FirebaseTokenVerifier tokenVerifier,
        UserProvisioningService userProvisioningService,
        UserService userService,
        OkeyTableRepository tableRepository,
        ChatService chatService
    ) {
        this.tokenVerifier = tokenVerifier;
        this.userProvisioningService = userProvisioningService;
        this.userService = userService;
        this.tableRepository = tableRepository;
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            accessor.setUser(authenticate(accessor));
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            UUID tableId = extractTableId(command, accessor.getDestination());
            if (tableId != null) {
                ensureChatMembership(accessor.getUser(), tableId);
            }
        }

        return message;
    }

    private Principal authenticate(StompHeaderAccessor accessor) {
        String token = bearerToken(accessor);
        if (token == null) {
            throw new AccessDeniedException("Missing Firebase ID token");
        }

        try {
            VerifiedFirebaseToken verifiedToken = tokenVerifier.verify(token);
            User user = userProvisioningService.findOrCreateUser(verifiedToken);
            return new AuthenticatedUserPrincipal(user.getId(), user.getFirebaseUid(), user.getPhone());
        } catch (FirebaseTokenVerificationException | IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid Firebase ID token", exception);
        }
    }

    private String bearerToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null) {
            authorization = accessor.getFirstNativeHeader("authorization");
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    private UUID extractTableId(StompCommand command, String destination) {
        if (destination == null) {
            return null;
        }
        Pattern pattern = command == StompCommand.SUBSCRIBE ? TABLE_TOPIC : CHAT_DESTINATION;
        Matcher matcher = pattern.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        return UUID.fromString(matcher.group(1));
    }

    private void ensureChatMembership(Principal principal, UUID tableId) {
        if (!(principal instanceof AuthenticatedUserPrincipal authenticatedUser)) {
            throw new AccessDeniedException("Authentication required");
        }

        User user = userService.getCurrentUser(authenticatedUser);
        OkeyTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new EntityNotFoundException("Table not found"));
        chatService.ensureCanAccessChat(table, user);
    }
}
