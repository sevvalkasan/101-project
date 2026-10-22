package com.masabul.backend.device;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class UserDeviceController {

    private final UserDeviceService deviceService;

    public UserDeviceController(UserDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> register(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @Valid @RequestBody FcmTokenRequest request
    ) {
        deviceService.register(principal, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/fcm-token")
    public ResponseEntity<Void> unregister(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @Valid @RequestBody FcmTokenRequest request
    ) {
        deviceService.unregister(principal, request);
        return ResponseEntity.noContent().build();
    }
}
