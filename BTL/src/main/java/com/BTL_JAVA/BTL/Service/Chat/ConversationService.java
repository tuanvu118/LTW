package com.BTL_JAVA.BTL.Service.Chat;

import com.BTL_JAVA.BTL.DTO.Response.Chat.ConversationItemResponse;
import com.BTL_JAVA.BTL.DTO.Response.Chat.SenderSummary;
import com.BTL_JAVA.BTL.Entity.Chat.Conversation;
import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.enums.ChatConversationType;
import com.BTL_JAVA.BTL.Repository.Chat.ConversationRepository;
import com.BTL_JAVA.BTL.Repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {
    ConversationRepository conversationRepository;
    UserRepository userRepository;

    public static final int DEFAULT_ADMIN_ID = 1;

    @Transactional
    public Conversation addConversation(int userId) {
        return addConversation(userId, ChatConversationType.ADMIN);
    }

    @Transactional
    public Conversation addConversation(int userId, ChatConversationType type) {
        if (type == null) {
            type = ChatConversationType.ADMIN;
        }

        if (type == ChatConversationType.ADMIN) {
            // Với dữ liệu cũ: conversationType có thể null -> coi như ADMIN để tránh tạo trùng.
            return conversationRepository
                    .findByUserAndAdminAndConversationTypeOrNull(userId, DEFAULT_ADMIN_ID, ChatConversationType.ADMIN)
                    .orElseGet(() -> conversationRepository.save(
                            Conversation.builder()
                                    .user(userRepository.findById(userId).orElseThrow())
                                    .admin(userRepository.findById(DEFAULT_ADMIN_ID).orElseThrow())
                                    .conversationType(ChatConversationType.ADMIN)
                                    .build()
                    ));
        }

        return conversationRepository
                .findByUser_IdAndAdmin_IdAndConversationType(userId, DEFAULT_ADMIN_ID, ChatConversationType.AI)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .user(userRepository.findById(userId).orElseThrow())
                                .admin(userRepository.findById(DEFAULT_ADMIN_ID).orElseThrow())
                                .conversationType(ChatConversationType.AI)
                                .build()
                ));
    }

    /**
     * Đảm bảo 1 user có đúng 2 conversation: ADMIN và AI.
     * Nếu đã tồn tại thì chỉ trả về, không tạo mới.
     */
    @Transactional
    public List<Conversation> ensureUserConversations(int userId) {
        Conversation adminConv = addConversation(userId, ChatConversationType.ADMIN);
        Conversation aiConv = addConversation(userId, ChatConversationType.AI);
        return List.of(adminConv, aiConv);
    }

    public SenderSummary toSenderSummary(User user) {
        return SenderSummary.builder()
                .senderId(user.getId())
                .avatar(user.getAvatar())
                .senderName(user.getFullName())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ConversationItemResponse> listConversationsForViewer(int viewerId) {
        boolean isAdminViewer = (viewerId == DEFAULT_ADMIN_ID);

        List<Conversation> conversations = isAdminViewer
                ? conversationRepository.findByAdmin_IdOrderByUpdatedAtDesc(viewerId)
                : conversationRepository.findByUser_IdOrderByUpdatedAtDesc(viewerId);

        return conversations.stream().map(c -> {
            User counterpart = isAdminViewer ? c.getUser() : c.getAdmin();
            return ConversationItemResponse.builder()
                    .conversationId(c.getConversationId())
                    .senderSummary(toSenderSummary(counterpart))
                    .lastMessage(c.getLastMessage())
                    .conversationType(
                            c.getConversationType() == null
                                    ? ChatConversationType.ADMIN.name()
                                    : c.getConversationType().name()
                    )
                    .updatedAt(c.getUpdatedAt())
                    .build();
        }).toList();
    }


}
