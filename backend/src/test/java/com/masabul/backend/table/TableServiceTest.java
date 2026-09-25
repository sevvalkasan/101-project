package com.masabul.backend.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

    @Mock
    private OkeyTableRepository tableRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TableService tableService;

    @Test
    void createsTableWithAuthenticatedUserAsHostAndLongitudeLatitudeOrdering() {
        AuthenticatedUserPrincipal principal = principal();
        User host = user();
        when(userService.getCurrentUser(principal)).thenReturn(host);
        when(tableRepository.existsByHostAndStatus(host, OkeyTableStatus.OPEN)).thenReturn(false);
        when(tableRepository.save(any(OkeyTable.class))).thenAnswer(invocation -> {
            OkeyTable table = invocation.getArgument(0);
            ReflectionTestUtils.setField(table, "id", UUID.randomUUID());
            return table;
        });

        tableService.createTable(principal, new CreateTableRequest("  Okey gecesi  ", "  Moda Cafe  ", 40.987, 29.037));

        ArgumentCaptor<OkeyTable> tableCaptor = ArgumentCaptor.forClass(OkeyTable.class);
        verify(tableRepository).save(tableCaptor.capture());

        OkeyTable saved = tableCaptor.getValue();
        Point location = saved.getLocation();
        assertThat(saved.getHost()).isSameAs(host);
        assertThat(saved.getTitle()).isEqualTo("Okey gecesi");
        assertThat(saved.getVenueName()).isEqualTo("Moda Cafe");
        assertThat(saved.getCurrentPlayers()).isEqualTo(1);
        assertThat(saved.getMaxPlayers()).isEqualTo(4);
        assertThat(saved.getStatus()).isEqualTo(OkeyTableStatus.OPEN);
        assertThat(location.getX()).isEqualTo(29.037);
        assertThat(location.getY()).isEqualTo(40.987);
        assertThat(location.getSRID()).isEqualTo(4326);
    }

    @Test
    void rejectsSecondOpenTableForSameHost() {
        AuthenticatedUserPrincipal principal = principal();
        User host = user();
        when(userService.getCurrentUser(principal)).thenReturn(host);
        when(tableRepository.existsByHostAndStatus(host, OkeyTableStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> tableService.createTable(
            principal,
            new CreateTableRequest("Title", "Venue", 40.0, 29.0)
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Host already has an active open table");

        verify(tableRepository, never()).save(any());
    }

    @Test
    void rejectsExcessiveNearbyRadius() {
        assertThatThrownBy(() -> tableService.findNearby(40.0, 29.0, 50_001, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("radius must be between 1 and 50000 meters");

        verify(tableRepository, never()).findNearbyOpenTables(any(Double.class), any(Double.class), any(Integer.class), any(Integer.class));
    }

    @Test
    void delegatesNearbySearchWithValidatedParameters() {
        when(tableRepository.findNearbyOpenTables(40.0, 29.0, 1_000, 20)).thenReturn(List.of());

        List<NearbyTableResponse> result = tableService.findNearby(40.0, 29.0, 1_000, 20);

        assertThat(result).isEmpty();
        verify(tableRepository).findNearbyOpenTables(40.0, 29.0, 1_000, 20);
    }

    private AuthenticatedUserPrincipal principal() {
        return new AuthenticatedUserPrincipal(UUID.randomUUID(), "firebase-1", "+905551112233");
    }

    private User user() {
        User user = new User("firebase-1", "+905551112233", "Host User", null, true);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
