package com.masabul.backend.table;

import com.masabul.backend.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface OkeyTableRepository extends JpaRepository<OkeyTable, UUID> {

    boolean existsByHostAndStatus(User host, OkeyTableStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from OkeyTable t where t.id = :id")
    Optional<OkeyTable> findByIdForUpdate(@Param("id") UUID id);

    Optional<OkeyTable> findFirstByHostAndStatusInOrderByCreatedAtDesc(
        User host,
        List<OkeyTableStatus> statuses
    );

    @Query(
        value = """
            SELECT
                t.id AS id,
                t.title AS title,
                t.venue_name AS venueName,
                u.id AS hostId,
                u.full_name AS hostFullName,
                u.avatar_url AS hostAvatarUrl,
                ST_Y(t.location::geometry) AS latitude,
                ST_X(t.location::geometry) AS longitude,
                ST_Distance(
                    t.location,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                ) AS distanceMeters,
                (t.max_players - t.current_players) AS availableSpots,
                t.created_at AS createdAt
            FROM okey_tables t
            JOIN users u ON u.id = t.host_id
            WHERE t.status = 'OPEN'
              AND t.current_players < t.max_players
              AND ST_DWithin(
                    t.location,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY distanceMeters ASC
            LIMIT :limit
            """,
        nativeQuery = true
    )
    List<NearbyTableProjection> findNearbyOpenTables(
        @Param("lat") double latitude,
        @Param("lng") double longitude,
        @Param("radiusMeters") int radiusMeters,
        @Param("limit") int limit
    );
}
