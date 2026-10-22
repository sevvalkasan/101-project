package com.masabul.backend.device;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDeviceService {

    private final UserDeviceRepository deviceRepository;
    private final UserService userService;

    public UserDeviceService(UserDeviceRepository deviceRepository, UserService userService) {
        this.deviceRepository = deviceRepository;
        this.userService = userService;
    }

    @Transactional
    public void register(AuthenticatedUserPrincipal principal, FcmTokenRequest request) {
        User user = userService.getCurrentUser(principal);
        String token = request.token().trim();
        UserDevice device = deviceRepository.findByFcmToken(token)
            .map(existing -> {
                existing.reassign(user, request.platform());
                return existing;
            })
            .orElseGet(() -> new UserDevice(user, token, request.platform()));
        deviceRepository.save(device);
    }

    @Transactional
    public void unregister(AuthenticatedUserPrincipal principal, FcmTokenRequest request) {
        User user = userService.getCurrentUser(principal);
        deviceRepository.deleteByUserAndFcmToken(user, request.token().trim());
    }
}
