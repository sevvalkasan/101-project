package com.masabul.backend.request;

import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableRequestRepository extends JpaRepository<TableRequest, UUID> {

    boolean existsByTableAndUser(OkeyTable table, User user);

    boolean existsByTableAndUserAndStatus(OkeyTable table, User user, TableRequestStatus status);

    List<TableRequest> findByTableOrderByCreatedAtAsc(OkeyTable table);

    List<TableRequest> findByTableAndStatus(OkeyTable table, TableRequestStatus status);

    @Query("""
        select r
        from TableRequest r
        join fetch r.table t
        join fetch t.host
        where r.user = :user and r.status = :status
        order by r.respondedAt desc
        """)
    List<TableRequest> findDetailedByUserAndStatus(
        @Param("user") User user,
        @Param("status") TableRequestStatus status
    );

    @Query("""
        select r
        from TableRequest r
        join fetch r.table t
        join fetch t.host
        join fetch r.user
        where r.id = :id
        """)
    Optional<TableRequest> findDetailedById(@Param("id") UUID id);
}
