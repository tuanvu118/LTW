package com.BTL_JAVA.BTL.Service.Chat;

import com.BTL_JAVA.BTL.DTO.Request.Chat.ChatMessageRequest;
import com.BTL_JAVA.BTL.DTO.Response.Chat.ChatMessageResponse;
import com.BTL_JAVA.BTL.DTO.Response.Chat.SenderSummary;
import com.BTL_JAVA.BTL.Entity.Chat.Conversation;
import com.BTL_JAVA.BTL.Entity.Chat.Message;
import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.Repository.Chat.ChatMessageRepository;
import com.BTL_JAVA.BTL.Repository.Chat.ConversationRepository;
import com.BTL_JAVA.BTL.Repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageService {
    ConversationRepository conversationRepository;
    ChatMessageRepository chatMessageRepository;
    UserRepository userRepository;
    SimpMessagingTemplate messagingTemplate;


    SenderSummary toSenderSummary(User user) {
        return SenderSummary.builder()
                .senderId(user.getId())
                .avatar(user.getAvatar())
                .senderName(user.getFullName())
                .build();
    }

    private ChatMessageResponse toMessageResponse(Message m, Integer currentUserId) {
        return ChatMessageResponse.builder()
                .messageId(m.getMessageId())
                .conversationId(m.getConversation().getConversationId())
                .senderId(m.getSender().getId(  ))
                .senderSummary(toSenderSummary(m.getSender()))
                .content(m.getContent())
                .me(Objects.equals(m.getSender().getId(), currentUserId))
                .createdAt(m.getCreatedAt().toString())
                .build();
    }

    @Transactional
    public ChatMessageResponse send(ChatMessageRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn trống");
        }

        final int ADMIN_ID = ConversationService.DEFAULT_ADMIN_ID;
        User sender = userRepository.findById(req.getSenderId()).orElseThrow();

        if (req.getConversationId() == null) {
            throw new IllegalArgumentException("conversationId là bắt buộc khi gửi tin nhắn");
        }

        Conversation conv = conversationRepository.findById(req.getConversationId()).orElseThrow();

        // Quyền: user chỉ được gửi trong conversation thuộc về mình,
        // admin (ADMIN_ID) có thể gửi vào bất kỳ conversation nào.
        if (!Objects.equals(sender.getId(), ADMIN_ID)
                && !Objects.equals(conv.getUser().getId(), sender.getId())) {
            throw new IllegalArgumentException("conversationId không thuộc về user hiện tại");
        }

        // Lưu message
        Message m = new Message();
        m.setConversation(conv);
        m.setSender(sender);
        m.setContent(req.getContent());
        // createdAt do @CreationTimestamp lo
        chatMessageRepository.save(m);

        // Cập nhật conversation
        conv.setLastMessage(req.getContent());
        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        //public socket event to client is conversation
        ChatMessageResponse response = toMessageResponse(m, sender.getId());
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conv.getConversationId(),
                response
        );

        // “currentUserId” để set cờ me — ở đây mình coi người gửi là current
        return toMessageResponse(m, sender.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listByConversation(int conversationId, int viewerId) {
        Conversation conv = conversationRepository.findById(conversationId).orElseThrow();

        // User chỉ được xem conversation thuộc về mình; admin xem được tất cả.
        if (viewerId != ConversationService.DEFAULT_ADMIN_ID
                && conv.getUser().getId() != viewerId) {
            throw new IllegalArgumentException("viewerId không có quyền xem conversation này");
        }

        var messages = chatMessageRepository
                .findAllByConversationConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream()
                .map(m -> toMessageResponse(m, viewerId))
                .toList();
    }

}
