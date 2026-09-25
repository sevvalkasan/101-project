package com.masabul.backend.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.table.OkeyTableRepository;
import com.masabul.backend.table.OkeyTableStatus;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TableRequestServiceTest {

    @Mock
    private TableRequestRepository requestRepository;

    @Mock
    private OkeyTableRepository tableRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TableRequestService requestService;

    @Test
    void sendsJoinRequestForOpenTable() {
        AuthenticatedUserPrincipal principal = principal();
        User host = user("host");
        User player = user("player");
        OkeyTable table = table(host, OkeyTableStatus.OPEN, 1);
        when(userService.getCurrentUser(principal)).thenReturn(player);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(requestRepository.existsByTableAndUser(table, player)).thenReturn(false);
        when(requestRepository.save(any(TableRequest.class))).thenAnswer(invocation -> {
            TableRequest request = invocation.getArgument(0);
            setId(request);
            setCreatedAt(request);
            return request;
        });

        TableRequestResponse response = requestService.sendRequest(principal, table.getId());

        assertThat(response.tableId()).isEqualTo(table.getId());
        assertThat(response.status()).isEqualTo(TableRequestStatus.PENDING);
        ArgumentCaptor<TableRequest> captor = ArgumentCaptor.forClass(TableRequest.class);
        verify(requestRepository).save(captor.capture());
        assertThat(captor.getValue().getTable()).isSameAs(table);
        assertThat(captor.getValue().getUser()).isSameAs(player);
    }

    @Test
    void hostCannotRequestOwnTable() {
        AuthenticatedUserPrincipal principal = principal();
        User host = user("host");
        OkeyTable table = table(host, OkeyTableStatus.OPEN, 1);
        when(userService.getCurrentUser(principal)).thenReturn(host);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> requestService.sendRequest(principal, table.getId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Host cannot request their own table");

        verify(requestRepository, never()).save(any());
    }

    @Test
    void onlyHostCanListRequests() {
        AuthenticatedUserPrincipal principal = principal();
        User host = user("host");
        User other = user("other");
        OkeyTable table = table(host, OkeyTableStatus.OPEN, 1);
        when(userService.getCurrentUser(principal)).thenReturn(other);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> requestService.listRequests(principal, table.getId()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void acceptRequestIncrementsPlayersAndFillsTableRejectingRemainingPendingRequests() {
        AuthenticatedUserPrincipal principal = principal();
        User host = user("host");
        User player = user("player");
        User pendingPlayer = user("pending");
        OkeyTable table = table(host, OkeyTableStatus.OPEN, 3);
        TableRequest accepted = request(table, player);
        TableRequest pending = request(table, pendingPlayer);
        when(userService.getCurrentUser(principal)).thenReturn(host);
        when(requestRepository.findDetailedById(accepted.getId())).thenReturn(Optional.of(accepted));
        when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
        when(requestRepository.findByTableAndStatus(table, TableRequestStatus.PENDING)).thenReturn(List.of(pending));

        TableRequestResponse response = requestService.respond(principal, accepted.getId(), RequestDecision.ACCEPT);

        assertThat(response.status()).isEqualTo(TableRequestStatus.ACCEPTED);
        assertThat(table.getCurrentPlayers()).isEqualTo(4);
        assertThat(table.getStatus()).isEqualTo(OkeyTableStatus.FULL);
        assertThat(pending.getStatus()).isEqualTo(TableRequestStatus.REJECTED);
    }

    @Test
    void rejectRequestDoesNotChangeTableCapacity() {
        AuthenticatedUserPrincipal principal = principal();
        User host = user("host");
        User player = user("player");
        OkeyTable table = table(host, OkeyTableStatus.OPEN, 1);
        TableRequest request = request(table, player);
        when(userService.getCurrentUser(principal)).thenReturn(host);
        when(requestRepository.findDetailedById(request.getId())).thenReturn(Optional.of(request));
        when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));

        TableRequestResponse response = requestService.respond(principal, request.getId(), RequestDecision.REJECT);

        assertThat(response.status()).isEqualTo(TableRequestStatus.REJECTED);
        assertThat(table.getCurrentPlayers()).isEqualTo(1);
        assertThat(table.getStatus()).isEqualTo(OkeyTableStatus.OPEN);
    }

    @Test
    void cancelTableRejectsPendingRequests() {
        AuthenticatedUserPrincipal principal = principal();
        User host = user("host");
        OkeyTable table = table(host, OkeyTableStatus.OPEN, 1);
        TableRequest pending = request(table, user("player"));
        when(userService.getCurrentUser(principal)).thenReturn(host);
        when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
        when(requestRepository.findByTableAndStatus(table, TableRequestStatus.PENDING)).thenReturn(List.of(pending));

        requestService.cancelTable(principal, table.getId());

        assertThat(table.getStatus()).isEqualTo(OkeyTableStatus.CANCELLED);
        assertThat(pending.getStatus()).isEqualTo(TableRequestStatus.REJECTED);
    }

    @Test
    void oneAvailableSpotConcurrentAcceptSimulationKeepsCapacityInvariant() throws Exception {
        User host = user("host");
        OkeyTable table = table(host, OkeyTableStatus.OPEN, 3);
        TableRequest first = request(table, user("first"));
        TableRequest second = request(table, user("second"));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Boolean> firstResult = executor.submit(() -> acceptAfterLatch(table, first, start));
        Future<Boolean> secondResult = executor.submit(() -> acceptAfterLatch(table, second, start));
        start.countDown();

        int acceptedCount = Boolean.TRUE.equals(firstResult.get()) ? 1 : 0;
        acceptedCount += Boolean.TRUE.equals(secondResult.get()) ? 1 : 0;
        executor.shutdown();

        assertThat(acceptedCount).isEqualTo(1);
        assertThat(table.getCurrentPlayers()).isEqualTo(4);
        assertThat(table.getStatus()).isEqualTo(OkeyTableStatus.FULL);
        assertThat(List.of(first.getStatus(), second.getStatus()))
            .containsExactlyInAnyOrder(TableRequestStatus.ACCEPTED, TableRequestStatus.REJECTED);
    }

    private boolean acceptAfterLatch(OkeyTable table, TableRequest request, CountDownLatch start) throws Exception {
        start.await();
        synchronized (table) {
            if (!table.hasAvailableSpot()) {
                request.rejectIfPending();
                return false;
            }
            request.accept();
            table.acceptPlayer();
            return true;
        }
    }

    private AuthenticatedUserPrincipal principal() {
        return new AuthenticatedUserPrincipal(UUID.randomUUID(), "firebase", "+905551112233");
    }

    private User user(String suffix) {
        User user = new User("firebase-" + suffix, "+90555" + Math.abs(suffix.hashCode() % 1_000_000), suffix, null, true);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private OkeyTable table(User host, OkeyTableStatus status, int currentPlayers) {
        OkeyTable table = new OkeyTable(
            host,
            "Okey",
            "Cafe",
            new GeometryFactory().createPoint(new Coordinate(29.0, 40.0))
        );
        ReflectionTestUtils.setField(table, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(table, "status", status);
        ReflectionTestUtils.setField(table, "currentPlayers", currentPlayers);
        return table;
    }

    private TableRequest request(OkeyTable table, User user) {
        TableRequest request = new TableRequest(table, user);
        setId(request);
        setCreatedAt(request);
        return request;
    }

    private void setId(TableRequest request) {
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
    }

    private void setCreatedAt(TableRequest request) {
        ReflectionTestUtils.setField(request, "createdAt", OffsetDateTime.now());
    }
}
