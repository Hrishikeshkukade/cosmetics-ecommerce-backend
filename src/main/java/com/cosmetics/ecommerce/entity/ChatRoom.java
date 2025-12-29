package com.cosmetics.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private String userName;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private LocalDateTime createdAt;

    private LocalDateTime lastMessageAt;

    private Integer unreadCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastMessageAt = LocalDateTime.now();
    }

    public enum Status {
        ACTIVE,
        CLOSED
    }
}
