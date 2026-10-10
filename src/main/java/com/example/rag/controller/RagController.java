package com.example.rag.controller;

import java.util.Map;

import com.example.rag.service.RagService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping
    public Map<String, String> ask(
        @RequestBody QuestionRequest request
    ) {

        String answer = ragService.ask(request.question());

        return Map.of("answer", answer);
    }

    public record QuestionRequest(String question) {
    }
}
