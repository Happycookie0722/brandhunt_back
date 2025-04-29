package com.dev.BrandHunt.Entity;

import com.dev.BrandHunt.Constant.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity @Setter @Getter
public class MessageHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long originalMessageId;

    private Long userId;

    private String content;

    private LocalDateTime sendAt;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @CreationTimestamp
    private LocalDateTime archivedAt;
}
