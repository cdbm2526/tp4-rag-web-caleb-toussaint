package ht.mbds.calebtoussaint.tp4ragwebcalebtoussaint.llm;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ressources partagees pour le RAG : construites une seule fois au demarrage
 * de l'application (portee application), pour eviter de re-ingerer les PDF
 * a chaque nouvelle conversation.
 */
@ApplicationScoped
public class RagResources implements Serializable {

    private ChatModel chatModel;
    private ContentRetriever contentRetrieverRag;
    private ContentRetriever contentRetrieverLivret;
    private Map<ContentRetriever, String> descriptions;

    /**
     * Construit le modele de chat et ingere les 2 documents PDF au demarrage
     * de l'application.
     */
    @PostConstruct
    public void init() {
        String cle = System.getenv("GEMINI_KEY");

        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(cle)
                .modelName("gemini-2.5-flash")
                .temperature(0.7)
                .build();

        EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(cle)
                .modelName("gemini-embedding-001")
                .outputDimensionality(768)
                .timeout(Duration.ofSeconds(60))
                .build();

        this.contentRetrieverRag = creerContentRetriever("rag.pdf", embeddingModel);
        this.contentRetrieverLivret = creerContentRetriever("livret.pdf", embeddingModel);

        this.descriptions = new HashMap<>();
        this.descriptions.put(contentRetrieverRag,
                "Support de cours sur le fine-tuning et le RAG (Retrieval-Augmented Generation) pour les LLMs");
        this.descriptions.put(contentRetrieverLivret,
                "Livret etudiant du programme MBDS 2025-2026");
    }

    private ContentRetriever creerContentRetriever(String nomFichier, EmbeddingModel embeddingModel) {
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
        Document document = ClassPathDocumentLoader.loadDocument(nomFichier, parser);

        DocumentSplitter splitter = DocumentSplitters.recursive(2000, 200);
        List<TextSegment> segments = splitter.split(document);

        Response<List<Embedding>> embeddingsResponse = embeddingModel.embedAll(segments);
        List<Embedding> embeddings = embeddingsResponse.content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.5)
                .build();
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public Map<ContentRetriever, String> getDescriptions() {
        return descriptions;
    }
}