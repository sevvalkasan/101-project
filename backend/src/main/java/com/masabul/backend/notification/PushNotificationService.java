package com.masabul.backend.notification;

import com.masabul.backend.device.UserDevice;
import com.masabul.backend.device.UserDeviceRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationService {

    private final UserDeviceRepository deviceRepository;
    private final PushGateway pushGateway;

    public PushNotificationService(UserDeviceRepository deviceRepository, PushGateway pushGateway) {
        this.deviceRepository = deviceRepository;
        this.pushGateway = pushGateway;
    }

    @Transactional
    public void notifyUser(
        UUID userId,
        String body,
        String event,
        UUID tableId
    ) {
        List<String> tokens = deviceRepository.findByUserId(userId)
            .stream()
            .map(UserDevice::getFcmToken)
            .toList();
        Set<String> invalidTokens = pushGateway.send(
            tokens,
            "MasaBul",
            body,
            Map.of("event", event, "tableId", tableId.toString())
        );
        if (!invalidTokens.isEmpty()) {
            deviceRepository.deleteByFcmTokenIn(invalidTokens);
        }
    }
}
