package online.shikshaplus.SpringAiDemo.controller;

import online.shikshaplus.SpringAiDemo.service.AiService;
import online.shikshaplus.SpringAiDemo.service.SimpleAiService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class AIController {

    private final AiService aiService;
    private final SimpleAiService simpleAiService;

    public AIController(AiService aiService, SimpleAiService simpleAiService) {
        this.aiService = aiService;
        this.simpleAiService = simpleAiService;
    }


    @PostMapping(value = "/ask")
    public String askAiText(@RequestBody String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Please enter a message.";
        }
        return aiService.chat(message);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetHistory() {
        aiService.resetHistory();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Conversation history cleared."));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ShikshaPlus Support AI"));
    }
}
