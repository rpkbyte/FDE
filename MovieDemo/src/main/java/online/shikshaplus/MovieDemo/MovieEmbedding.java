package online.shikshaplus.MovieDemo;

public record MovieEmbedding(
        String title,
        String description,
        float[] embedding
) {

}