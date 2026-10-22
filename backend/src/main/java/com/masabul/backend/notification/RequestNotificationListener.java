package com.masabul.backend.notification;

import com.masabul.backend.request.TableRequestStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RequestNotificationListener {

    private final PushNotificationService notificationService;

    public RequestNotificationListener(PushNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJoinRequest(JoinRequestNotification event) {
        notificationService.notifyUser(
            event.hostId(),
            "Bir oyuncu masanıza katılmak istiyor.",
            "JOIN_REQUEST",
            event.tableId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecision(RequestDecisionNotification event) {
        boolean accepted = event.status() == TableRequestStatus.ACCEPTED;
        notificationService.notifyUser(
            event.playerId(),
            accepted ? "Masaya kabul edildiniz." : "Katılım isteğiniz reddedildi.",
            accepted ? "REQUEST_ACCEPTED" : "REQUEST_REJECTED",
            event.tableId()
        );
    }
}
