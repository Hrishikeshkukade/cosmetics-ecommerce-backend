package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.entity.ChatMessage;
import com.cosmetics.ecommerce.entity.ChatRoom;
import com.cosmetics.ecommerce.repository.ChatMessageRepository;
import com.cosmetics.ecommerce.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatRoom getOrCreateChatRoom(Long userId, String userName) {
        return chatRoomRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ChatRoom room = new ChatRoom();
                    room.setUserId(userId);
                    room.setUserName(userName);
                    return chatRoomRepository.save(room);
                });
    }

    public ChatMessage saveMessage(ChatMessage message) {
        ChatMessage saved = chatMessageRepository.save(message);

        // Update chat room last message time
        ChatRoom room = chatRoomRepository.findById(message.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        room.setLastMessageAt(LocalDateTime.now());

        if (message.getSenderType() == ChatMessage.SenderType.USER) {
            room.setUnreadCount(room.getUnreadCount() + 1);
        }

        chatRoomRepository.save(room);

        return saved;
    }

    public List<ChatMessage> getChatMessages(Long chatRoomId) {
        return chatMessageRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);
    }

    public List<ChatRoom> getActiveChatRooms() {
        return chatRoomRepository.findByStatusOrderByLastMessageAtDesc(ChatRoom.Status.ACTIVE);
    }

    public void markMessagesAsRead(Long chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        room.setUnreadCount(0);
        chatRoomRepository.save(room);
    }
}
