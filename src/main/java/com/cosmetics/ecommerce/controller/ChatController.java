package com.cosmetics.ecommerce.controller;

import com.cosmetics.ecommerce.entity.ChatMessage;
import com.cosmetics.ecommerce.entity.ChatRoom;
import com.cosmetics.ecommerce.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;


    @PostMapping("/send")
    public ResponseEntity<ChatMessage> sendMessage(@RequestBody ChatMessage message) {
        ChatMessage savedMessage = chatService.saveMessage(message);
        messagingTemplate.convertAndSend(
                "/topic/messages/" + message.getChatRoomId(),
                savedMessage
        );
        return ResponseEntity.ok(savedMessage);
    }


//    @PostMapping("/send")
//    public ResponseEntity<ChatMessage> sendMessage(@RequestBody ChatMessage message) {
//        try {
//            ChatMessage savedMessage = chatService.saveMessage(message);
//
//            // Broadcast to WebSocket subscribers if available
//            messagingTemplate.convertAndSend(
//                    "/topic/messages/" + message.getChatRoomId(),
//                    savedMessage
//            );
//
//            return ResponseEntity.ok(savedMessage);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

    @PostMapping("/room")
    public ResponseEntity<ChatRoom> getOrCreateRoom(
            @RequestParam Long userId,
            @RequestParam String userName
    ) {
        return ResponseEntity.ok(chatService.getOrCreateChatRoom(userId, userName));
    }

    @GetMapping("/room/{chatRoomId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatMessages(@PathVariable Long chatRoomId) {
        return ResponseEntity.ok(chatService.getChatMessages(chatRoomId));
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ChatRoom>> getActiveChatRooms() {
        return ResponseEntity.ok(chatService.getActiveChatRooms());
    }

    @PostMapping("/room/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long chatRoomId) {
        chatService.markMessagesAsRead(chatRoomId);
        return ResponseEntity.ok().build();
    }

//    @MessageMapping("/chat/send")
//    public void sendMessage(ChatMessage message) {
//        ChatMessage saved = chatService.saveMessage(message);
//        messagingTemplate.convertAndSend("/topic/messages/" + message.getChatRoomId(), saved);
//    }
}
