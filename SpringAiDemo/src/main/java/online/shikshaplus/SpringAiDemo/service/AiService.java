package online.shikshaplus.SpringAiDemo.service;

import online.shikshaplus.SpringAiDemo.tool.CalculatorTool;
import online.shikshaplus.SpringAiDemo.tool.CurrencyExchangeTool;
import online.shikshaplus.SpringAiDemo.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiService {

    private final List<Message> history = java.util.Collections.synchronizedList(new ArrayList<>());

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder,
                     CalculatorTool calculatorTool,CurrencyExchangeTool currencyExchangeTool,
                     WeatherTool weatherTool
                     ) {
        // Register tools as defaultTools on the ChatClient instance
        this.chatClient = chatClientBuilder
                .defaultTools(calculatorTool,currencyExchangeTool,weatherTool)
                .build();
    }




    public synchronized String chat(String message) {
        final String SYSTEM_PROMPT = """
                You are ShikshaPlus intelligent AI assistant.
                You are equipped with real-time tools:
                1. Mathematical and arithmetic calculations (CalculatorTool).
                2. Live currency conversions and exchange rates (CurrencyExchangeTool).
                3. Current weather and multi-day forecasts for any city or location (WeatherTool).

                Always invoke the appropriate tool when the user asks for math calculations, currency rates/conversions, or weather information.
                Provide clear, accurate, and helpful answers. Support English and Hinglish.
                """;

        UserMessage userMessage = new UserMessage(message);
        history.add(userMessage);

        // Keep history manageable to prevent token overflow
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }

        try {
            String output = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .messages(history)
                    .call()
                    .content();

            if (output != null && !output.isBlank()) {
                history.add(new AssistantMessage(output));
                return output;
            }
            return "I could not generate a response. Please try again.";
        } catch (Exception e) {
            // Remove user message from history if the call failed so subsequent requests aren't corrupted
            history.remove(userMessage);

            String errMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            if (errMsg.contains("429") || errMsg.toLowerCase().contains("ratelimit")) {
                return "The AI provider is currently rate-limited (HTTP 429). The free model on OpenRouter has exceeded its temporary quota. Please wait a moment or switch to a different model in application.properties.";
            }
            return "Error while contacting AI provider: " + errMsg;
        }
    }

    public synchronized void resetHistory() {
        history.clear();
    }
}
