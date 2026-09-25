package com.masabul.backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class FirebaseAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    public FirebaseAdminConfig(
        @Value("${masabul.firebase.service-account-path:}") String serviceAccountPath,
        @Value("${masabul.firebase.service-account-json-base64:}") String serviceAccountJsonBase64
    ) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        GoogleCredentials credentials = loadCredentials(serviceAccountPath, serviceAccountJsonBase64);
        if (credentials == null) {
            log.warn("Firebase Admin SDK was not initialized because no credentials were configured");
            return;
        }
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build();
        FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials loadCredentials(String serviceAccountPath, String serviceAccountJsonBase64) throws IOException {
        if (serviceAccountJsonBase64 != null && !serviceAccountJsonBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountJsonBase64);
            return GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
        }
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            return GoogleCredentials.fromStream(new FileInputStream(serviceAccountPath));
        }
        return null;
    }
}
