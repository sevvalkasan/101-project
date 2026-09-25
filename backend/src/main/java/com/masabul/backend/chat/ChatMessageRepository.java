package com.masabul.backend.chat;

import com.masabul.backend.table.OkeyTable;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByTableOrderByCreatedAtAsc(OkeyTable table, Pageable pageable);
}
