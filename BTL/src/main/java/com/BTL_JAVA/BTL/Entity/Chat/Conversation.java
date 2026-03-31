package com.BTL_JAVA.BTL.Entity.Chat;


import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.enums.ChatConversationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "conversation")
@Builder
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    int conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY )
    @JoinColumn(name = "admin_id", nullable = false)
    User admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type")
    ChatConversationType conversationType;

    // lastMessage có thể dài (đặc biệt khi lấy câu trả lời AI), nên lưu dạng TEXT/LONGTEXT.
    @Lob
    @Column(name = "last_message", columnDefinition = "LONGTEXT")
    String lastMessage;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

}
