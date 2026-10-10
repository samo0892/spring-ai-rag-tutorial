package com.example.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Startet die komplette Anwendung gegen ein echtes PostgreSQL mit pgvector
 * (Testcontainers). Chat- und Embedding-Modell werden durch Fakes ersetzt,
 * daher laeuft der Test ohne OpenAI-API-Key und ohne Kosten.
 */
@SpringBootTest(properties = "spring.ai.openai.api-key=test-key-not-used")
@Import(FakeAiModelsConfiguration.class)
@Testcontainers
class RagApplicationIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:pg17")
            .asCompatibleSubstituteFor("postgres")
    );

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbcTemplate.update("DELETE FROM vector_store");
    }

    @Test
    void schemaWirdMitVektorspalteUndHnswIndexAngelegt() {
        String columnType = jdbcTemplate.queryForObject(
            """
            SELECT format_type(a.atttypid, a.atttypmod)
            FROM pg_attribute a
            WHERE a.attrelid = 'vector_store'::regclass
              AND a.attname = 'embedding'
            """,
            String.class
        );
        Integer hnswIndexes = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM pg_indexes
            WHERE tablename = 'vector_store' AND indexdef ILIKE '%USING hnsw%'
            """,
            Integer.class
        );

        assertThat(columnType).isEqualTo("vector(1536)");
        assertThat(hnswIndexes).isEqualTo(1);
    }

    @Test
    void dokumentHochladenUndFrageBeantworten() throws Exception {
        byte[] content = Files.readAllBytes(Path.of("samples/nimbus-produktdoku.md"));
        var file = new MockMultipartFile(
            "file", "nimbus-produktdoku.md", "text/markdown", content
        );

        mockMvc.perform(multipart("/api/documents").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Dokument erfolgreich verarbeitet"))
            .andExpect(jsonPath("$.chunks", greaterThan(0)));

        Integer stored = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM vector_store WHERE metadata->>'source' = 'nimbus-produktdoku.md'",
            Integer.class
        );
        assertThat(stored).isPositive();

        // Der Fake-ChatModel gibt den Prompt zurueck, den der QuestionAnswerAdvisor
        // gebaut hat. So pruefen wir, dass gefundener Kontext im Prompt landet.
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"question": "Wie authentifiziert sich ein Client an der Nimbus REST API?"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer", containsString("Wie authentifiziert sich ein Client")))
            .andExpect(jsonPath("$.answer", containsString("OAuth 2.0")));
    }
}
