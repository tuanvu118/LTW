package com.BTL_JAVA.BTL.Controller.Chat;

import com.BTL_JAVA.BTL.DTO.Response.Chat.ConversationItemResponse;
import com.BTL_JAVA.BTL.Entity.Chat.Conversation;
import com.BTL_JAVA.BTL.Service.Chat.ConversationService;
import com.BTL_JAVA.BTL.enums.ChatConversationType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat/conversations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationController {
    ConversationService conversationService;

    /**
     * API cho USER: tạo/đảm bảo đủ 2 conversation (ADMIN + AI) và trả về thông tin.
     * Nếu đã tồn tại thì chỉ trả về, không tạo mới trong DB.
     */
    @PostMapping("/ensure")
    public List<ConversationItemResponse> ensureUserConversations(@RequestParam int userId) {
        List<Conversation> conversations = conversationService.ensureUserConversations(userId);
        return conversations.stream().map(c -> {
            boolean isAdminType = c.getConversationType() == null
                    || c.getConversationType() == ChatConversationType.ADMIN;
            return ConversationItemResponse.builder()
                    .conversationId(c.getConversationId())
                    .lastMessage(c.getLastMessage())
                    .updatedAt(c.getUpdatedAt())
                    .senderSummary(
                            isAdminType
                                    ? conversationService.toSenderSummary(c.getAdmin())
                                    : conversationService.toSenderSummary(c.getAdmin())
                    )
                    .conversationType(
                            c.getConversationType() == null
                                    ? ChatConversationType.ADMIN.name()
                                    : c.getConversationType().name()
                    )
                    .build();
        }).toList();
    }

    /**
     * API cho ADMIN: lấy tất cả conversation (ADMIN + AI).
     * viewerId bắt buộc là admin (DEFAULT_ADMIN_ID).
     */
    @GetMapping("/admin")
    public List<ConversationItemResponse> listAllForAdmin(@RequestParam int viewerId) {
        return conversationService.listConversationsForViewer(viewerId);
    }
}
