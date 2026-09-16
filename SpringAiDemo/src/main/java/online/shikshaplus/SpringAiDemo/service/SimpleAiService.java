package online.shikshaplus.SpringAiDemo.service;

import online.shikshaplus.SpringAiDemo.tool.SimpleCalculatorTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SimpleAiService {
    private static final String SYSTEM_PROMPT = """
            You are ShikshaPlus intelligent AI assistant supporting English and Hinglish.
            You are equipped with a calculator tool for arithmetic and mathematical operations.
            Always invoke the calculation tool for any math queries to ensure 100% accurate results.
            Never calculate or guess math answers manually; rely strictly on the tool output to respond.
            """;

    private final List<Message> history = java.util.Collections.synchronizedList(new ArrayList<>());

    private final ChatClient chatClient;
    private final SimpleCalculatorTool simpleCalculatorTool;


    public SimpleAiService(ChatClient.Builder chatClient, SimpleCalculatorTool simpleCalculatorTool) {
        this.chatClient = chatClient.build();
        this.simpleCalculatorTool = simpleCalculatorTool;
    }


    public synchronized String chat(String message) {
        UserMessage userMessage = new UserMessage(message);
        history.add(userMessage);

        // Keep history manageable to prevent token overflow
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }

        try {
            String output = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .tools(simpleCalculatorTool)
                    .messages(history)
                    .call()
                    .content();

            if (output != null && !output.isBlank()) {
                history.add(new AssistantMessage(output));
                return output;
            }
            return "I could not generate a response. Please try again.";
        } catch (Exception e) {
            // Remove user message from history if the call failed
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
