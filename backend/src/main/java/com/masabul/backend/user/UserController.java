package com.masabul.backend.user;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return UserResponse.from(userService.getCurrentUser(principal));
    }

    @PutMapping("/me")
    public UserResponse updateMe(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return UserResponse.from(userService.updateCurrentUser(principal, request));
    }
}
