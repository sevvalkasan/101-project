package com.masabul.backend.table;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.request.TableRequestResponse;
import com.masabul.backend.request.TableRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/tables")
public class TableController {

    private final TableService tableService;
    private final TableRequestService requestService;

    public TableController(TableService tableService, TableRequestService requestService) {
        this.tableService = tableService;
        this.requestService = requestService;
    }

    @PostMapping
    public TableResponse createTable(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @Valid @RequestBody CreateTableRequest request
    ) {
        return tableService.createTable(principal, request);
    }

    @GetMapping("/nearby")
    public List<NearbyTableResponse> nearby(
        @RequestParam("lat") @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @RequestParam("lng") @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @RequestParam("radius") @Min(1) @Max(50_000) int radiusMeters,
        @RequestParam("limit") @Min(1) @Max(100) int limit
    ) {
        return tableService.findNearby(latitude, longitude, radiusMeters, limit);
    }

    @PostMapping("/{tableId}/requests")
    public TableRequestResponse sendRequest(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @PathVariable java.util.UUID tableId
    ) {
        return requestService.sendRequest(principal, tableId);
    }

    @GetMapping("/{tableId}/requests")
    public List<TableRequestResponse> listRequests(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @PathVariable java.util.UUID tableId
    ) {
        return requestService.listRequests(principal, tableId);
    }

    @PostMapping("/{tableId}/cancel")
    public void cancelTable(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @PathVariable java.util.UUID tableId
    ) {
        requestService.cancelTable(principal, tableId);
    }
}
