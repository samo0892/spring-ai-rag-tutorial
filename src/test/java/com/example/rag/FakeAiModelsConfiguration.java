package com.example.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Ersetzt die OpenAI-Modelle im Test durch deterministische Fakes.
 */
@TestConfiguration(proxyBeanMethods = false)
class FakeAiModelsConfiguration {

    static final int DIMENSIONS = 1536;

    @Bean
    @Primary
    EmbeddingModel fakeEmbeddingModel() {
        return new FakeEmbeddingModel();
    }

    @Bean
    @Primary
    ChatModel fakeChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                // Gibt den kompletten Prompt-Inhalt (inkl. Kontext aus dem Advisor) zurueck.
                String echo = prompt.getUserMessage().getText();
                return new ChatResponse(List.of(new Generation(new AssistantMessage(echo))));
            }
        };
    }

    /**
     * Einfaches "Bag of Words"-Embedding: jedes Wort wird per Hash auf eine
     * Dimension abgebildet. Eine konstante Basiskomponente sorgt dafuer, dass
     * Texte mit gemeinsamen Woertern deutlich ueber dem Threshold von 0.7 liegen.
     */
    static final class FakeEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            List<String> inputs = request.getInstructions();
            for (int i = 0; i < inputs.size(); i++) {
                embeddings.add(new Embedding(embedText(inputs.get(i)), i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return embedText(document.getText());
        }

        @Override
        public int dimensions() {
            return DIMENSIONS;
        }

        private static float[] embedText(String text) {
            float[] vector = new float[DIMENSIONS];
            vector[0] = 3.0f;
            for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
                if (token.isBlank()) {
                    continue;
                }
                int hash = token.hashCode();
                vector[1 + Math.floorMod(hash, DIMENSIONS - 1)] += 0.1f;
            }
            double norm = 0;
            for (float v : vector) {
                norm += v * v;
            }
            float scale = (float) (1.0 / Math.sqrt(norm));
            for (int i = 0; i < vector.length; i++) {
                vector[i] *= scale;
            }
            return vector;
        }
    }
}
