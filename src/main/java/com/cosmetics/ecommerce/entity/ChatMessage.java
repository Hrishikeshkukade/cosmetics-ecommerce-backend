package com.cosmetics.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String senderName;

    @Enumerated(EnumType.STRING)
    private SenderType senderType; // USER or ADMIN

    @Column(length = 2000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private Boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    public enum SenderType {
        USER,
        ADMIN
    }
}
