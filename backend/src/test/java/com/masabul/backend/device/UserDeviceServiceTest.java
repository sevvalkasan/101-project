package com.masabul.backend.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock
    private UserDeviceRepository deviceRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserDeviceService deviceService;

    @Test
    void registersTrimmedTokenForAuthenticatedUser() {
        AuthenticatedUserPrincipal principal = principal();
        User user = user("current");
        when(userService.getCurrentUser(principal)).thenReturn(user);
        when(deviceRepository.findByFcmToken("token-1")).thenReturn(Optional.empty());

        deviceService.register(
            principal,
            new FcmTokenRequest("  token-1  ", DevicePlatform.IOS)
        );

        ArgumentCaptor<UserDevice> captor = ArgumentCaptor.forClass(UserDevice.class);
        verify(deviceRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getFcmToken()).isEqualTo("token-1");
        assertThat(captor.getValue().getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    void reassignsExistingTokenToCurrentUser() {
        AuthenticatedUserPrincipal principal = principal();
        User previous = user("previous");
        User current = user("current");
        UserDevice existing = new UserDevice(previous, "token-1", DevicePlatform.ANDROID);
        when(userService.getCurrentUser(principal)).thenReturn(current);
        when(deviceRepository.findByFcmToken("token-1")).thenReturn(Optional.of(existing));

        deviceService.register(
            principal,
            new FcmTokenRequest("token-1", DevicePlatform.IOS)
        );

        verify(deviceRepository).save(existing);
        assertThat(existing.getUser()).isSameAs(current);
        assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    private AuthenticatedUserPrincipal principal() {
        return new AuthenticatedUserPrincipal(UUID.randomUUID(), "firebase-current", "+905000000000");
    }

    private User user(String suffix) {
        return new User(
            "firebase-" + suffix,
            "+90500000000" + suffix.length(),
            "Test User",
            null,
            true
        );
    }
}
