package com.example.rag.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class DocumentSearchService {

    private final VectorStore vectorStore;

    public DocumentSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> search(String query) {

        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(5)
            .similarityThreshold(0.7)
            .build();

        return vectorStore.similaritySearch(request);
    }
}
