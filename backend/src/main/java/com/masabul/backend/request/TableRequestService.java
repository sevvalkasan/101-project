package com.masabul.backend.request;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.table.OkeyTableRepository;
import com.masabul.backend.table.OkeyTableStatus;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableRequestService {

    private final TableRequestRepository requestRepository;
    private final OkeyTableRepository tableRepository;
    private final UserService userService;

    public TableRequestService(
        TableRequestRepository requestRepository,
        OkeyTableRepository tableRepository,
        UserService userService
    ) {
        this.requestRepository = requestRepository;
        this.tableRepository = tableRepository;
        this.userService = userService;
    }

    @Transactional
    public TableRequestResponse sendRequest(AuthenticatedUserPrincipal principal, UUID tableId) {
        User user = userService.getCurrentUser(principal);
        OkeyTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new EntityNotFoundException("Table not found"));

        if (table.isHostedBy(user)) {
            throw new IllegalStateException("Host cannot request their own table");
        }
        if (!table.isOpen()) {
            throw new IllegalStateException("Only open tables accept requests");
        }
        if (!table.hasAvailableSpot()) {
            throw new IllegalStateException("Table is full");
        }
        if (requestRepository.existsByTableAndUser(table, user)) {
            throw new IllegalStateException("User already has a request for this table");
        }

        return TableRequestResponse.from(requestRepository.save(new TableRequest(table, user)));
    }

    @Transactional(readOnly = true)
    public List<TableRequestResponse> listRequests(AuthenticatedUserPrincipal principal, UUID tableId) {
        User host = userService.getCurrentUser(principal);
        OkeyTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new EntityNotFoundException("Table not found"));
        ensureHost(table, host);

        return requestRepository.findByTableOrderByCreatedAtAsc(table)
            .stream()
            .map(TableRequestResponse::from)
            .toList();
    }

    @Transactional
    public TableRequestResponse respond(AuthenticatedUserPrincipal principal, UUID requestId, RequestDecision decision) {
        User host = userService.getCurrentUser(principal);
        TableRequest request = requestRepository.findDetailedById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Request not found"));
        OkeyTable table = tableRepository.findByIdForUpdate(request.getTable().getId())
            .orElseThrow(() -> new EntityNotFoundException("Table not found"));

        ensureHost(table, host);
        if (!request.isPending()) {
            throw new IllegalStateException("Request is not pending");
        }

        if (decision == RequestDecision.REJECT) {
            request.reject();
            return TableRequestResponse.from(request);
        }

        if (!table.isOpen()) {
            throw new IllegalStateException("Table is not open");
        }
        if (!table.hasAvailableSpot()) {
            throw new IllegalStateException("Table is full");
        }

        request.accept();
        table.acceptPlayer();
        if (table.getStatus() == OkeyTableStatus.FULL) {
            rejectRemainingPendingRequests(table, request.getId());
        }

        return TableRequestResponse.from(request);
    }

    @Transactional
    public void cancelTable(AuthenticatedUserPrincipal principal, UUID tableId) {
        User host = userService.getCurrentUser(principal);
        OkeyTable table = tableRepository.findByIdForUpdate(tableId)
            .orElseThrow(() -> new EntityNotFoundException("Table not found"));

        ensureHost(table, host);
        table.cancel();
        requestRepository.findByTableAndStatus(table, TableRequestStatus.PENDING)
            .forEach(TableRequest::rejectIfPending);
    }

    private void rejectRemainingPendingRequests(OkeyTable table, UUID acceptedRequestId) {
        requestRepository.findByTableAndStatus(table, TableRequestStatus.PENDING)
            .stream()
            .filter(request -> !request.getId().equals(acceptedRequestId))
            .forEach(TableRequest::rejectIfPending);
    }

    private void ensureHost(OkeyTable table, User user) {
        if (!table.isHostedBy(user)) {
            throw new AccessDeniedException("Only the table host can perform this operation");
        }
    }
}
