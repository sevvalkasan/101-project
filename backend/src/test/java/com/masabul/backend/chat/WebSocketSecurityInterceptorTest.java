package com.masabul.backend.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.security.FirebaseTokenVerificationException;
import com.masabul.backend.security.FirebaseTokenVerifier;
import com.masabul.backend.security.VerifiedFirebaseToken;
import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.table.OkeyTableRepository;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserProvisioningService;
import com.masabul.backend.user.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityInterceptorTest {

    @Mock
    private FirebaseTokenVerifier tokenVerifier;

    @Mock
    private UserProvisioningService userProvisioningService;

    @Mock
    private UserService userService;

    @Mock
    private OkeyTableRepository tableRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private WebSocketSecurityInterceptor interceptor;

    @Test
    void authenticatesConnectWithFirebaseBearerToken() {
        User user = user("socket");
        when(tokenVerifier.verify("valid-token")).thenReturn(new VerifiedFirebaseToken("firebase-socket", "+905551112233"));
        when(userProvisioningService.findOrCreateUser(new VerifiedFirebaseToken("firebase-socket", "+905551112233")))
            .thenReturn(user);

        Message<?> result = interceptor.preSend(connect("Bearer valid-token"), null);
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) StompHeaderAccessor.wrap(result).getUser();

        assertThat(principal.getInternalUserId()).isEqualTo(user.getId());
        assertThat(principal.getFirebaseUid()).isEqualTo("firebase-socket");
    }

    @Test
    void rejectsConnectWithoutToken() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), null))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsConnectWithInvalidToken() {
        when(tokenVerifier.verify("bad-token")).thenThrow(new FirebaseTokenVerificationException("bad"));

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer bad-token"), null))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void verifiesMembershipOnSubscribe() {
        User user = user("subscriber");
        OkeyTable table = table(user("host"));
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(user.getId(), user.getFirebaseUid(), user.getPhone());
        when(userService.getCurrentUser(principal)).thenReturn(user);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        Message<?> message = stomp(StompCommand.SUBSCRIBE, "/topic/table/" + table.getId(), principal);

        interceptor.preSend(message, null);
    }

    @Test
    void sendRequiresAuthenticatedPrincipal() {
        Message<?> message = stomp(StompCommand.SEND, "/app/chat/" + UUID.randomUUID(), null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(AccessDeniedException.class);
    }

    private Message<?> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> stomp(StompCommand command, String destination, AuthenticatedUserPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
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
}
