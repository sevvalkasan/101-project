package com.masabul.backend.user;

import com.masabul.backend.security.VerifiedFirebaseToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProvisioningService {

    private static final String TEMPORARY_FULL_NAME = "MasaBul Kullanıcısı";

    private final UserRepository userRepository;

    public UserProvisioningService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreateUser(VerifiedFirebaseToken token) {
        return userRepository.findByFirebaseUid(token.firebaseUid())
            .orElseGet(() -> userRepository.save(new User(
                token.firebaseUid(),
                normalizePhone(token.phone()),
                TEMPORARY_FULL_NAME,
                null,
                false
            )));
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Firebase token does not contain a phone number");
        }
        return phone;
    }
}
