package com.BTL_JAVA.BTL.Controller.Chat;

import com.BTL_JAVA.BTL.DTO.Request.Chat.ChatMessageRequest;
import com.BTL_JAVA.BTL.DTO.Response.Chat.ChatMessageResponse;
import com.BTL_JAVA.BTL.Service.Chat.ChatMessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageController {
    ChatMessageService messageService;

    // Gửi tin nhắn vào 1 conversation cụ thể (ADMIN hoặc AI đều dùng được).
    // Request phải truyền conversationId; quyền được validate trong service.
    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@RequestBody ChatMessageRequest req) {
        return messageService.send(req);
    }

    // Lấy tất cả tin nhắn của 1 conversation.
    // User chỉ xem được conversation thuộc về mình; admin xem được tất cả.
    @GetMapping("/messages")
    public List<ChatMessageResponse> listMessages(
            @RequestParam int conversationId,
            @RequestParam int viewerId
    ) {
        return messageService.listByConversation(conversationId, viewerId);
    }
}
