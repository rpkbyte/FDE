package com.example.AiDemo.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:knowledge.json")
    private Resource knowledgeJson;

    @Value("${app.rag.similarity-threshold:0.50}")
    private double similarityThreshold;

//    @Value("${app.rag.top-k:4}")
//    private int topK;

    private static final String SERVER_FALLBACK_MESSAGE =
            "I do not have enough information in our knowledge base to answer this question. " +
                    "Please reach out to our NovaTech Solutions human support team for further assistance.";

    private static final String SYSTEM_PROMPT = """                                                 
                You are the official AI Support Assistant for NovaTech Solutions.
                Your role is to assist customers accurately, politely, and professionally.
 
                STRICT OPERATING RULES:
                1. ONLY answer using the verified information provided in the company knowledge base context.
                2. Keep your answers concise, well-structured, and helpful.
                """;

    public AiService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;

        // Build ChatClient with system instructions and RAG advisor
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Value("${app.rag.ingest-on-start:false}")
    private boolean ingestOnStart;

    @PostConstruct
    public void ingestKnowledgeBase() {
        if (!ingestOnStart) {
            System.out.println("Ingestion skipped: knowledge base is already active in Pinecone (set app.rag.ingest-on-start=true to re-sync).");
            return;
        }

        try {
            System.out.println("Reading Q&A enriched knowledge.json...");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(knowledgeJson.getInputStream());

            List<Document> documents = new ArrayList<>();

            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    String id = node.get("id").asText();
                    String contextText = node.get("contextText").asText();
                    String sourceFile = node.get("sourceFile").asText();
                    String documentName = node.get("document").asText();
                    String sectionName = node.get("section").asText();
                    String category = node.get("category").asText();

                    // Document with deterministic ID, full Q&A context, and rich metadata
                    Document doc = new Document(
                            id,
                            contextText,
                            Map.of(
                                    "source", sourceFile,
                                    "document", documentName,
                                    "section", sectionName,
                                    "category", category
                            )
                    );
                    documents.add(doc);
                }
            }

            if (!documents.isEmpty()) {
                System.out.println("Ingesting " + documents.size() + " Q&A enriched sections into Pinecone via Ollama...");
                vectorStore.accept(documents);
                System.out.println("Ingestion completed! " + documents.size() + " vectors ready in Pinecone.");
            }
        } catch (Exception e) {
            System.err.println("Error during knowledge ingestion: " + e.getMessage());
        }
    }

    public reactor.core.publisher.Flux<String> askStream(String message) {

        List<Document> similarDocuments = searchKnowledgeBase(message);

        if (similarDocuments.isEmpty()){
            return Flux.just(SERVER_FALLBACK_MESSAGE);
        }

        String context = formatContext(similarDocuments);

        return chatClient.prompt()
                .user(u -> u.text("""
                        Context infromation from knowledge base :
                        -----------------
                        {context}
                        -----------------
                        User Question: {query}
                        """)
                        .param("context",context)
                        .param("query",message))
                .stream()
                .content();
    }

    private List<Document> searchKnowledgeBase(String query){
        System.out.println("searchknowledgebase call");
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(4)
                .similarityThreshold(similarityThreshold)
                .build();
        List<Document> vector =vectorStore.similaritySearch(searchRequest);
        System.out.println("similarity search : " +vector);
        return vector;


    }
    private String formatContext(List<Document> documents){
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

}
