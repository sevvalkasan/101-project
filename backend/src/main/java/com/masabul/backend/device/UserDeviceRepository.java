package com.masabul.backend.device;

import com.masabul.backend.user.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    Optional<UserDevice> findByFcmToken(String fcmToken);

    List<UserDevice> findByUserId(UUID userId);

    void deleteByUserAndFcmToken(User user, String fcmToken);

    void deleteByFcmTokenIn(Collection<String> fcmTokens);
}
