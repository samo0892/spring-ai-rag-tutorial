package com.example.rag.controller;

import java.io.IOException;
import java.util.Map;

import com.example.rag.service.DocumentIngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;

    public DocumentController(
        DocumentIngestionService ingestionService
    ) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> upload(
        @RequestParam("file") MultipartFile file
    ) throws IOException {

        int chunks = ingestionService.ingest(file);

        return Map.of(
            "message", "Dokument erfolgreich verarbeitet",
            "chunks", chunks
        );
    }
}
