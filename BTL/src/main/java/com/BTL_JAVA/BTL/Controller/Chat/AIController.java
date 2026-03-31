package com.BTL_JAVA.BTL.Controller.Chat;

import com.BTL_JAVA.BTL.Service.AIServices;
import com.BTL_JAVA.BTL.DTO.Request.Chat.ChatMessageRequest;
import com.BTL_JAVA.BTL.DTO.Request.Chat.AIRequest;
import com.BTL_JAVA.BTL.DTO.Response.Chat.AIResponse;
import com.BTL_JAVA.BTL.DTO.Response.Chat.ChatMessageResponse;
import com.BTL_JAVA.BTL.Service.Chat.ChatMessageService;
import com.BTL_JAVA.BTL.Service.Chat.ConversationService;
import com.BTL_JAVA.BTL.enums.ChatConversationType;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Objects;
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class AIController {
    AIServices aiServices;
    ChatMessageService chatMessageService;
    ConversationService conversationService;

    @PostMapping
    public AIResponse generateText(@RequestBody AIRequest request) {
        int currentUserId = getCurrentUserId();

        // Conversation AI cho user hiện tại.
        var conversation = conversationService.addConversation(currentUserId, ChatConversationType.AI);
        int conversationId = conversation.getConversationId();

        if (Objects.nonNull(request.getConversationId()) && request.getConversationId() != conversationId) {
            throw new IllegalArgumentException("conversationId không hợp lệ cho user hiện tại");
        }

        // 1) Lưu message user
        ChatMessageRequest userMsgReq = ChatMessageRequest.builder()
                .senderId(currentUserId)
                .content(request.getInput())
                .conversationId(conversationId)
                .build();
        chatMessageService.send(userMsgReq);

        // 2) Lấy history theo conversationId (đã bao gồm message user vừa gửi)
        List<ChatMessageResponse> historyMessages = chatMessageService.listByConversation(conversationId, currentUserId);
        List<Content> historyContents = historyMessages.stream()
                .map(m -> Content.builder()
                        .role(m.getSenderId() == ConversationService.DEFAULT_ADMIN_ID ? "model" : "user")
                        .parts(List.of(Part.fromText(m.getContent())))
                        .build())
                .toList();

        // 3) Gọi Gemini với history
        var aiResponse = aiServices.generateTextFromHistory(historyContents);

        // 4) Lưu message AI (coi AI = admin mặc định)
        ChatMessageRequest aiMsgReq = ChatMessageRequest.builder()
                .senderId(ConversationService.DEFAULT_ADMIN_ID)
                .receiverId(currentUserId)
                .content(aiResponse.getContent())
                .conversationId(conversationId)
                .build();
        chatMessageService.send(aiMsgReq);

        return AIResponse.builder()
                .conversationId(conversationId)
                .content(aiResponse.getContent())
                .build();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessageResponse> listMessages(
            @PathVariable int conversationId
    ) {
        int currentUserId = getCurrentUserId();
        return chatMessageService.listByConversation(conversationId, currentUserId);
    }

    private int getCurrentUserId() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return Integer.parseInt(userId);
    }
}
