package com.example.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final ChatClient chatClient;

    public RagService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String ask(String question) {

        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
