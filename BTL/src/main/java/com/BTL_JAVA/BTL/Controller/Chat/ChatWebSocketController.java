package com.BTL_JAVA.BTL.Controller.Chat;

import com.BTL_JAVA.BTL.DTO.Request.Chat.ChatMessageRequest;
import com.BTL_JAVA.BTL.DTO.Response.Chat.ChatMessageResponse;
import com.BTL_JAVA.BTL.Service.Chat.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;

    // Client sẽ send đến /app/chat.sendMessage
    @MessageMapping("/chat.messages.send")
    public void handleSendMessage(ChatMessageRequest request) {
        try {
            ChatMessageResponse resp = chatMessageService.send(request);
            log.info("Message sent via WS: {}", resp.getMessageId());
        } catch (Exception e) {
            log.error("Error when handling WS message", e);
        }
    }
}
