package com.masabul.backend.notification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.masabul.backend.device.DevicePlatform;
import com.masabul.backend.device.UserDevice;
import com.masabul.backend.device.UserDeviceRepository;
import com.masabul.backend.user.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private UserDeviceRepository deviceRepository;

    @Mock
    private PushGateway pushGateway;

    @InjectMocks
    private PushNotificationService notificationService;

    @Test
    void sendsToAllDevicesAndDeletesInvalidTokens() {
        UUID userId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        User user = new User("firebase", "+905000000000", "Test User", null, true);
        List<UserDevice> devices = List.of(
            new UserDevice(user, "valid", DevicePlatform.IOS),
            new UserDevice(user, "invalid", DevicePlatform.ANDROID)
        );
        when(deviceRepository.findByUserId(userId)).thenReturn(devices);
        when(pushGateway.send(
            List.of("valid", "invalid"),
            "MasaBul",
            "Yeni istek",
            Map.of("event", "JOIN_REQUEST", "tableId", tableId.toString())
        )).thenReturn(Set.of("invalid"));

        notificationService.notifyUser(
            userId,
            "Yeni istek",
            "JOIN_REQUEST",
            tableId
        );

        verify(deviceRepository).deleteByFcmTokenIn(Set.of("invalid"));
    }
}
