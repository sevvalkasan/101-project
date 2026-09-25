package com.masabul.backend.chat;

import com.masabul.backend.request.TableRequestRepository;
import com.masabul.backend.request.TableRequestStatus;
import com.masabul.backend.table.OkeyTable;
import com.masabul.backend.user.User;
import org.springframework.stereotype.Service;

@Service
public class ChatAuthorizationService {

    private final TableRequestRepository requestRepository;

    public ChatAuthorizationService(TableRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public boolean canAccessChat(OkeyTable table, User user) {
        return table.isHostedBy(user)
            || requestRepository.existsByTableAndUserAndStatus(table, user, TableRequestStatus.ACCEPTED);
    }
}
