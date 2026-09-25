package com.masabul.backend.user;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(AuthenticatedUserPrincipal principal) {
        return userRepository.findById(principal.getInternalUserId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    public User updateCurrentUser(AuthenticatedUserPrincipal principal, UpdateUserProfileRequest request) {
        User user = getCurrentUser(principal);
        user.updateProfile(request.fullName().trim(), request.avatarUrl());
        return user;
    }
}
