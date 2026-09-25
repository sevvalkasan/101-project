package com.masabul.backend.request;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    private final TableRequestService requestService;

    public RequestController(TableRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/{requestId}/respond")
    public TableRequestResponse respond(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @PathVariable UUID requestId,
        @Valid @RequestBody RespondToRequestBody body
    ) {
        return requestService.respond(principal, requestId, body.decision());
    }
}
