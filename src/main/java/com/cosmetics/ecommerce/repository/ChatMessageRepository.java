package com.cosmetics.ecommerce.repository;

import com.cosmetics.ecommerce.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomIdOrderByTimestampAsc(Long chatRoomId);
    Long countByChatRoomIdAndIsReadFalse(Long chatRoomId);
}
