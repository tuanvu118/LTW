package com.BTL_JAVA.BTL.Service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.errors.GenAiIOException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.BTL_JAVA.BTL.DTO.Request.Chat.AIRequest;
import com.BTL_JAVA.BTL.DTO.Response.Chat.AIResponse;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AIServices {
    Client client;
    Content systemInstruction;

    public AIServices(
            @Value("${GOOGLE_API_KEY:}") String googleApiKey,
            @Value("${GEMINI_API_KEY:}") String geminiApiKey,
            Content geminiSystemInstruction
    ) {
        String apiKey = (googleApiKey != null && !googleApiKey.isBlank())
                ? googleApiKey
                : geminiApiKey;

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Lỗi cấu hình: set GOOGLE_API_KEY (recommended) or GEMINI_API_KEY");
        }

        this.client = Client.builder().apiKey(apiKey).build();
        this.systemInstruction = geminiSystemInstruction;
    }

    public AIResponse generateTextFromTextInput(AIRequest request) {
        if (request == null || request.getInput() == null || request.getInput().isBlank()) {
            throw new IllegalArgumentException("AIRequest.input không được null/blank");
        }

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(systemInstruction)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3-flash-preview",
                    request.getInput(),
                    config
            );

            String text = response.text();
            if (text == null) {
                throw new IllegalStateException("Gemini trả về text response trống");
            }

            return AIResponse.builder()
                    .content(text)
                    .build();
        } catch (GenAiIOException e) {
            log.error("Gemini gọi thất bại: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini gọi thất bại: " + e.getMessage(), e);
        }
    }

    public AIResponse generateTextFromHistory(List<Content> history) {
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("Gemini history must not be null/empty");
        }

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(systemInstruction)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3-flash-preview",
                    history,
                    config
            );

            String text = response.text();
            if (text == null) {
                throw new IllegalStateException("Gemini trả về text response trống");
            }

            return AIResponse.builder()
                    .content(text)
                    .build();
        } catch (GenAiIOException e) {
            log.error("Gemini gọi thất bại: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini gọi thất bại: " + e.getMessage(), e);
        }
    }
}