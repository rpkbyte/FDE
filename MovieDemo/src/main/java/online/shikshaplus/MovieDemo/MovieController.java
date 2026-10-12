package online.shikshaplus.MovieDemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieEmbeddingService service;

    public MovieController(MovieEmbeddingService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public List<SearchResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int top) {
        return service.searchMovies(query, top);
    }
}