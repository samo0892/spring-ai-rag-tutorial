package com.example.rag.service;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingest(MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                "Die Datei darf nicht leer sein."
            );
        }

        var resource = new ByteArrayResource(file.getBytes());

        var reader = new TikaDocumentReader(resource);

        List<Document> documents = reader.read();

        String filename = file.getOriginalFilename() == null
            ? "unknown"
            : file.getOriginalFilename();

        documents.forEach(document ->
            document.getMetadata().put(
                "source", filename
            )
        );

        var splitter = TokenTextSplitter.builder()
            .withChunkSize(500)
            .withMinChunkSizeChars(200)
            .withMinChunkLengthToEmbed(10)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            .build();

        List<Document> chunks = splitter.apply(documents);

        vectorStore.add(chunks);

        return chunks.size();
    }
}
