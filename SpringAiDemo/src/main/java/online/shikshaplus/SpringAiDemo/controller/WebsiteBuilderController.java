package online.shikshaplus.SpringAiDemo.controller;

import online.shikshaplus.SpringAiDemo.service.WebsiteBuilderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class WebsiteBuilderController {

    private final WebsiteBuilderService websiteBuilderService;

    public WebsiteBuilderController(WebsiteBuilderService websiteBuilderService) {
        this.websiteBuilderService = websiteBuilderService;
    }

    @PostMapping(value = "/website")
    public String askAiText(@RequestBody String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Please enter a message.";
        }
        return websiteBuilderService.chat(message);
    }

    @PostMapping("/reset-website-history")
    public ResponseEntity<Map<String, String>> resetHistory() {
        websiteBuilderService.resetHistory();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Conversation history cleared."));
    }

}
