package com.masabul.backend.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.table.OkeyTableRepository;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private OkeyTableRepository tableRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChatAuthorizationService chatAuthorizationService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void sendsMessageWhenUserCanAccessChatAndPersistsBeforeReturning() {
        AuthenticatedUserPrincipal principal = principal();
        User sender = user("sender");
        OkeyTable table = table(sender);
        when(userService.getCurrentUser(principal)).thenReturn(sender);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(chatAuthorizationService.canAccessChat(table, sender)).thenReturn(true);
        when(chatMessageRepository.saveAndFlush(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(message, "createdAt", OffsetDateTime.now());
            return message;
        });

        ChatMessageResponse response = chatService.sendMessage(
            principal,
            table.getId(),
            new ChatMessageRequest("  merhaba masa  ")
        );

        assertThat(response.message()).isEqualTo("merhaba masa");
        assertThat(response.tableId()).isEqualTo(table.getId());
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSender()).isSameAs(sender);
        assertThat(captor.getValue().getTable()).isSameAs(table);
    }

    @Test
    void rejectsMessageWhenUserIsNotMember() {
        AuthenticatedUserPrincipal principal = principal();
        User sender = user("sender");
        OkeyTable table = table(user("host"));
        when(userService.getCurrentUser(principal)).thenReturn(sender);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(chatAuthorizationService.canAccessChat(table, sender)).thenReturn(false);

        assertThatThrownBy(() -> chatService.sendMessage(principal, table.getId(), new ChatMessageRequest("hello")))
            .isInstanceOf(AccessDeniedException.class);

        verify(chatMessageRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsChronologicalHistoryForAuthorizedUser() {
        AuthenticatedUserPrincipal principal = principal();
        User user = user("player");
        OkeyTable table = table(user("host"));
        ChatMessage message = message(table, user, "ilk mesaj");
        when(userService.getCurrentUser(principal)).thenReturn(user);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(chatAuthorizationService.canAccessChat(table, user)).thenReturn(true);
        when(chatMessageRepository.findByTableOrderByCreatedAtAsc(any(OkeyTable.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(message)));

        List<ChatMessageResponse> history = chatService.history(principal, table.getId(), 0, 20);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).message()).isEqualTo("ilk mesaj");
    }

    @Test
    void rejectsInvalidHistoryPageSize() {
        assertThatThrownBy(() -> chatService.history(principal(), UUID.randomUUID(), 0, 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size must be between 1 and 100");
    }

    private AuthenticatedUserPrincipal principal() {
        return new AuthenticatedUserPrincipal(UUID.randomUUID(), "firebase", "+905551112233");
    }

    private User user(String suffix) {
        User user = new User("firebase-" + suffix, "+90555" + Math.abs(suffix.hashCode() % 1_000_000), suffix, null, true);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private OkeyTable table(User host) {
        OkeyTable table = new OkeyTable(
            host,
            "Okey",
            "Cafe",
            new GeometryFactory().createPoint(new Coordinate(29.0, 40.0))
        );
        ReflectionTestUtils.setField(table, "id", UUID.randomUUID());
        return table;
    }

    private ChatMessage message(OkeyTable table, User sender, String body) {
        ChatMessage message = new ChatMessage(table, sender, body);
        ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(message, "createdAt", OffsetDateTime.now());
        return message;
    }
}
