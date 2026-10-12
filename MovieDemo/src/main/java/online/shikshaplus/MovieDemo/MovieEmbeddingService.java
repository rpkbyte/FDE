package online.shikshaplus.MovieDemo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MovieEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory cache taaki har search par JSON baar baar na padhna pade
    private List<MovieEmbedding> movieDatabase = new ArrayList<>();

    public MovieEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    // App start hote hi JSON read karega aur ek baar vectors bana kar memory me rakh lega
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("movie.json");
            InputStream inputStream = resource.getInputStream();
            List<Movie> movies = objectMapper.readValue(inputStream, new TypeReference<List<Movie>>() {});

            for (Movie movie : movies) {
                String textToEmbed = "Title: " + movie.title() + ". Description: " + movie.description();
                float[] vector = embeddingModel.embed(textToEmbed);
                movieDatabase.add(new MovieEmbedding(movie.title(), movie.description(), vector));
            }
            System.out.println("✅ All Movie Embeddings generated and loaded into memory!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Search function: User ki natural query ko match karega
    public List<SearchResult> searchMovies(String userQuery, int topK) {
        // 1. User ki query ka bhi embedding vector nikalo
        float[] queryVector = embeddingModel.embed(userQuery);

        List<SearchResult> results = new ArrayList<>();

        // 2. Har movie ke vector ke sath cosine similarity calculate karo
        for (MovieEmbedding movie : movieDatabase) {
            double similarity = calculateCosineSimilarity(queryVector, movie.embedding());
            results.add(new SearchResult(movie.title(), movie.description(), similarity));
        }

        // 3. Highest similarity score se sort karo aur top results return karo
        results.sort(Comparator.comparingDouble(SearchResult::similarity).reversed());
        return results.stream().limit(topK).toList();
    }

    // Cosine Similarity Formula Implementation
    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}