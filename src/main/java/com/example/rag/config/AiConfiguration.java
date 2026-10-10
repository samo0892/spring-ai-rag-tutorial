package com.example.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    @Bean
    public ChatClient chatClient(
        ChatClient.Builder builder,
        VectorStore vectorStore
    ) {

        var searchRequest = SearchRequest.builder()
            .topK(5)
            .similarityThreshold(0.7)
            .build();

        var advisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(searchRequest)
            .build();

        return builder
            .defaultSystem("""
                Du bist ein technischer Wissensassistent.

                Beantworte Fragen ausschließlich anhand
                der bereitgestellten Dokumentinformationen.

                Wenn die Informationen nicht ausreichen,
                sage ausdrücklich, dass du die Frage
                anhand der verfügbaren Dokumente
                nicht beantworten kannst.

                Erfinde keine Fakten oder Quellen.
                Antworte auf Deutsch.
                """)
            .defaultAdvisors(advisor)
            .build();
    }
}
