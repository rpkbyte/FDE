package online.shikshaplus.ChatBot.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatRequest {
    private String message;
    private String conversationId;
    private List<String> activeTools = new ArrayList<>();
    private String apiKey;
    private String baseUrl;
    private String model;
    private String systemPrompt;

    public ChatRequest() {
    }

    public ChatRequest(String message, List<String> activeTools) {
        this.message = message;
        this.activeTools = activeTools;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<String> getActiveTools() {
        return activeTools != null ? activeTools : new ArrayList<>();
    }

    public void setActiveTools(List<String> activeTools) {
        this.activeTools = activeTools;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
