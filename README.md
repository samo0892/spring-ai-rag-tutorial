# Spring AI RAG Tutorial

[![CI](https://github.com/samo0892/spring-ai-rag-tutorial/actions/workflows/ci.yml/badge.svg)](https://github.com/samo0892/spring-ai-rag-tutorial/actions/workflows/ci.yml)

Lauffähiger Begleitcode zum Artikel
**[RAG mit Spring Boot und Spring AI entwickeln: Ein vollständiges Java-Tutorial](https://www.sam-codes.com/blog/spring-ai-rag-java-tutorial)**.

Die Anwendung ist eine kleine Wissensdatenbank: Du lädst Dokumente hoch (PDF, Word,
Text, Markdown …), sie werden mit Apache Tika eingelesen, in Chunks zerlegt, mit
OpenAI eingebettet und in PostgreSQL mit pgvector gespeichert. Anschließend
beantwortet ein `ChatClient` mit `QuestionAnswerAdvisor` Fragen auf Basis dieser
Dokumente.

```text
POST /api/documents  →  DocumentController → DocumentIngestionService ─┐ add()
                                                                       ▼
                                              VectorStore (PostgreSQL + pgvector)
                                                                       ▲
POST /api/chat       →  RagController → RagService (ChatClient + QuestionAnswerAdvisor)
```

## Projektstruktur

| Datei | Artikel |
| --- | --- |
| [`pom.xml`](pom.xml) | Schritt 1 |
| [`compose.yaml`](compose.yaml) | Schritt 2 |
| [`application.yml`](src/main/resources/application.yml) | Schritt 3 |
| [`DocumentIngestionService`](src/main/java/com/example/rag/service/DocumentIngestionService.java) | Schritt 4 |
| [`DocumentController`](src/main/java/com/example/rag/controller/DocumentController.java) | Schritt 5 |
| [`DocumentSearchService`](src/main/java/com/example/rag/service/DocumentSearchService.java) | Schritt 6 |
| [`AiConfiguration`](src/main/java/com/example/rag/config/AiConfiguration.java) | Schritt 7 |
| [`RagService`](src/main/java/com/example/rag/service/RagService.java), [`RagController`](src/main/java/com/example/rag/controller/RagController.java) | Schritt 8 |
| [`samples/nimbus-produktdoku.md`](samples/nimbus-produktdoku.md) | fiktive Produktdoku zum Ausprobieren |

## Voraussetzungen

- Java 21 (z. B. Eclipse Temurin). Maven musst du nicht installieren, das Projekt bringt den Maven Wrapper (`./mvnw`) mit.
- Docker mit Docker Compose (oder Podman, siehe [unten](#mit-podman-statt-docker))
- Ein OpenAI API Key

## Anwendung starten

**1. Datenbank starten** (PostgreSQL 17 mit pgvector, siehe [`compose.yaml`](compose.yaml)):

```bash
docker compose up -d
```

**2. OpenAI API Key setzen.** Der Key wird ausschließlich aus der Umgebungsvariable
`OPENAI_API_KEY` gelesen und gehört nicht ins Repository:

```bash
export OPENAI_API_KEY="your-api-key"
```

Alternativ kannst du `.env.example` nach `.env` kopieren und den Key dort eintragen
(`.env` ist in `.gitignore` eingetragen). Spring Boot liest `.env` nicht automatisch,
lade die Datei also vor dem Start in deine Shell, z. B. mit `set -a; source .env; set +a`.

**3. Anwendung starten:**

```bash
./mvnw spring-boot:run
```

Beim ersten Start legt Spring AI die Extension `vector`, die Tabelle `vector_store`
mit einer Spalte `vector(1536)` und einen HNSW-Index an (`initialize-schema: true`).

> **Hinweis:** Ohne gesetzten `OPENAI_API_KEY` startet die Anwendung trotzdem. Upload
> und Chat schlagen dann aber beim ersten Aufruf von OpenAI mit HTTP 500 fehl
> (im Log: `401: Incorrect API key provided`).

## Ausprobieren

Beispieldokument hochladen:

```bash
curl -X POST \
  http://localhost:8080/api/documents \
  -F "file=@samples/nimbus-produktdoku.md"
```

```json
{
  "message": "Dokument erfolgreich verarbeitet",
  "chunks": 2
}
```

Die Zahl der Chunks hängt von Länge und Inhalt des Dokuments ab. Du kannst genauso
eigene PDF- oder Word-Dateien hochladen (maximal 10 MB).

Frage stellen:

```bash
curl -X POST \
  http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Wie authentifizieren sich Clients an der Nimbus REST API?"
  }'
```

Die Antwort sollte sich auf OAuth 2.0 und JWT Access Tokens aus der Beispieldoku
beziehen. Der genaue Wortlaut hängt vom Sprachmodell ab. Weitere Fragen, die die
Beispieldoku beantwortet:

- „Wann finden Deployments in Produktion statt?“
- „Wie lange werden Backups aufbewahrt?“
- „Was passiert, wenn ein Client das Rate Limit überschreitet?“

Eine Frage, zu der die Doku nichts enthält (z. B. „Welche Programmiersprache nutzt das
Frontend?“), sollte das Modell laut System Prompt als nicht beantwortbar kennzeichnen.

## Kosten der OpenAI API

Die Anwendung ruft kostenpflichtige OpenAI-Modelle auf: `text-embedding-3-small`
beim Hochladen (für jeden Chunk) und bei jeder Frage, `gpt-4o-mini` für jede Antwort.
Für das Beispieldokument und ein paar Fragen liegen die Kosten im Bereich von
Bruchteilen eines Cents. Große Dokumente oder viele Uploads kosten entsprechend mehr.
Die aktuellen Preise findest du unter <https://openai.com/api/pricing/>. Setze dir
im OpenAI-Dashboard am besten ein Ausgabenlimit.

## Tests

```bash
./mvnw verify
```

Der Integrationstest [`RagApplicationIntegrationTests`](src/test/java/com/example/rag/RagApplicationIntegrationTests.java)
startet die komplette Anwendung gegen ein echtes PostgreSQL mit pgvector
(Testcontainers, Image `pgvector/pgvector:pg17`). Chat- und Embedding-Modell werden durch
deterministische Fakes ersetzt ([`FakeAiModelsConfiguration`](src/test/java/com/example/rag/FakeAiModelsConfiguration.java)).
Die Tests brauchen deshalb **keinen OpenAI API Key** und verursachen keine Kosten,
aber eine laufende Container-Runtime (Docker oder Podman).

Geprüft wird:

- das Schema (`vector(1536)` und HNSW-Index) wird angelegt,
- der Upload der Beispieldatei speichert Chunks mit dem Metadatum `source`,
- eine Frage über `/api/chat` landet zusammen mit dem gefundenen Dokumentkontext
  im Prompt an das Chat-Modell.

Die GitHub Action [`ci.yml`](.github/workflows/ci.yml) führt `./mvnw verify` auf Java 21 aus.

## Mit Podman statt Docker

**Datenbank:** `podman compose up -d` funktioniert, sobald ein Compose-Provider
(`docker-compose` oder `podman-compose`) installiert ist. Ohne Provider startest du
dieselbe Datenbank direkt:

```bash
podman run -d --name spring-ai-postgres -e POSTGRES_DB=ragdb -e POSTGRES_USER=raguser -e POSTGRES_PASSWORD=ragpassword -p 5432:5432 -v postgres_data:/var/lib/postgresql/data docker.io/pgvector/pgvector:pg17
```

**Tests mit Testcontainers:** Testcontainers braucht Zugriff auf den Podman-Socket.
Auf macOS mit `podman machine` zeigt `podman machine inspect` den Socket-Pfad an,
den du als `DOCKER_HOST` setzt. Zusätzlich `TESTCONTAINERS_RYUK_DISABLED=true` setzen.

**Ohne lokal installiertes Java:** Build und Tests lassen sich komplett im Container
ausführen. Dieser Aufruf funktioniert mit einer *rootful* Podman Machine
(Standard auf macOS):

```bash
podman run --rm --security-opt label=disable --network host -v /run/podman/podman.sock:/var/run/docker.sock -e TESTCONTAINERS_RYUK_DISABLED=true -e TESTCONTAINERS_HOST_OVERRIDE=localhost -v "$PWD":/app -v rag-m2:/root/.m2 -w /app docker.io/library/eclipse-temurin:21-jdk ./mvnw -B verify
```

- `--security-opt label=disable` ist nötig, weil SELinux in der Podman Machine sonst
  den Zugriff auf das Maven-Cache-Volume `rag-m2` zwischen verschiedenen Containern blockiert.
- `--network host` und `TESTCONTAINERS_HOST_OVERRIDE=localhost` sorgen dafür, dass die
  Tests den von Testcontainers gestarteten pgvector-Container erreichen.

## Getestet mit

| Komponente | Version |
| --- | --- |
| Java | Eclipse Temurin 21.0.12 |
| Spring Boot | 4.0.8 |
| Spring AI | 2.0.1 |
| Maven (Wrapper) | 3.9.16 |
| PostgreSQL / pgvector | 17.11 / 0.8.7 (Image `pgvector/pgvector:pg17`) |
| Apache Tika | 3.3.1 (über Spring AI) |
| OpenAI Java SDK | 4.49.0 (über Spring AI) |
| Testcontainers | 2.0.5 (über Spring Boot) |
| Container-Runtime | Podman 6.0.2 (macOS) |

## Lizenz

[MIT](LICENSE) © Samed Baldede
