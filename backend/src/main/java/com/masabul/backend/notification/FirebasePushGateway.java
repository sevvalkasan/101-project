package com.masabul.backend.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FirebasePushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushGateway.class);
    private static final int MAX_MULTICAST_SIZE = 500;

    @Override
    public Set<String> send(
        List<String> tokens,
        String title,
        String body,
        Map<String, String> data
    ) {
        if (tokens.isEmpty() || FirebaseApp.getApps().isEmpty()) {
            return Set.of();
        }

        Set<String> invalidTokens = new HashSet<>();
        for (int start = 0; start < tokens.size(); start += MAX_MULTICAST_SIZE) {
            List<String> batchTokens = tokens.subList(
                start,
                Math.min(start + MAX_MULTICAST_SIZE, tokens.size())
            );
            sendBatch(batchTokens, title, body, data, invalidTokens);
        }
        return invalidTokens;
    }

    private void sendBatch(
        List<String> tokens,
        String title,
        String body,
        Map<String, String> data,
        Set<String> invalidTokens
    ) {
        MulticastMessage message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .build();
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            for (int index = 0; index < response.getResponses().size(); index++) {
                var sendResponse = response.getResponses().get(index);
                if (!sendResponse.isSuccessful() && isInvalid(sendResponse.getException())) {
                    invalidTokens.add(tokens.get(index));
                }
            }
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM batch delivery failed: {}", exception.getMessage());
        }
    }

    private boolean isInvalid(FirebaseMessagingException exception) {
        MessagingErrorCode code = exception.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED
            || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
