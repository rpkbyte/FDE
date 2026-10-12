package online.shikshaplus.ChatBot.controller;

import online.shikshaplus.ChatBot.dto.ChatRequest;
import online.shikshaplus.ChatBot.dto.StreamEvent;
import online.shikshaplus.ChatBot.dto.ToolInfo;
import online.shikshaplus.ChatBot.service.ChatService;
import online.shikshaplus.ChatBot.tools.ToolRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final ToolRegistry toolRegistry;

    public ChatController(ChatService chatService, ToolRegistry toolRegistry) {
        this.chatService = chatService;
        this.toolRegistry = toolRegistry;
    }

    /**
     * Lists all available on-demand tools with descriptions and token cost weights.
     */
    @GetMapping("/tools")
    public List<ToolInfo> getAvailableTools() {
        return toolRegistry.getAllToolInfos();
    }

    /**
     * Real-time streaming chat endpoint using Server-Sent Events (SSE).
     * Yields token-by-token stream, tool invocation telemetry, and token metrics.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamEvent>> streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }

    /**
     * System status endpoint.
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("appName", "Kortex");
        status.put("version", "1.0.0");
        status.put("streamingSupported", true);
        status.put("apiKeyConfigured", chatService.isRealKeyConfigured());
        status.put("activeModel", chatService.getConfiguredModel());
        status.put("availableTools", toolRegistry.getAllToolInfos().size());
        status.put("maxPotentialTokenSavingsPerPrompt", toolRegistry.calculateTotalToolTokenCost());
        return status;
    }
}
