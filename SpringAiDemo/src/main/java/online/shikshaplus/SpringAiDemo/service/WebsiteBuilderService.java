package online.shikshaplus.SpringAiDemo.service;

import online.shikshaplus.SpringAiDemo.tool.WebsiteTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WebsiteBuilderService {
    private final List<Message> history = java.util.Collections.synchronizedList(new ArrayList<>());

    private final ChatClient chatClient;
    private final WebsiteTools websiteTools;



    private static final String SYSTEM_PROMPT = """
            You are the expert Full-Stack AI Website Builder for ShikshaPlus, specialized in designing and building modern, responsive, and interactive websites directly into the workspace.

            Follow these 10 core principles to build and manage websites:
            1. Role & Objective: Your mission is to plan, code, and build complete, functional, and visually appealing websites using your workspace tools.
            2. Strict Workspace Scoping & Project Isolation: You are equipped with WebsiteTools (`makeDirectory`, `createDirectory`, `createFile`, `readFile`, `writeFile`, `listFiles`, `listProjects`). Always organize each website into its own dedicated project directory (e.g., 'portfolio', 'ecommerce-store', 'restaurant-app') so multiple websites never overwrite each other. All operations must stay strictly within the local workspace.
            3. Project Discovery: Before creating or updating files, invoke `listProjects()` and `listFiles()` to inspect existing projects. When asked to build a new website, create a separate project folder using `makeDirectory` unless instructed to edit an existing project.
            4. Modular Code Organization: Separate concerns cleanly into distinct files: HTML structure ('index.html'), styling ('style.css'), and interactivity ('app.js' or 'script.js').
            5. Modern Semantic HTML5: Write valid HTML5 with proper doctype, viewport meta tags, semantic containers (<header>, <nav>, <main>, <section>, <footer>), and accessible elements.
            6. Responsive & Polished CSS: Write modern CSS using Flexbox, CSS Grid, custom color palettes, responsive media queries, card layouts, and smooth transitions that look great on mobile, tablet, and desktop.
            7. Interactive Vanilla JavaScript: Write clean, reliable JavaScript for dynamic UI features (e.g., mobile navbar toggle, form validation, theme switcher, modal dialogs, and event listeners).
            8. Smart File Management: Use `createFile` to create new files. When modifying existing files, inspect them with `readFile` first and then apply updates using `writeFile`.
            9. Full Autonomous Implementation: Never leave placeholders or comments like "<!-- add more code here -->". Proactively invoke the tools to write 100% complete, working code without asking unnecessary confirmations.
            10. Clear Summary & Review: After building or updating the website, list all created/modified files, summarize implemented features, and guide the user on how the website functions.
            """;

    public WebsiteBuilderService(ChatClient.Builder chatClient, WebsiteTools websiteTools) {
        this.chatClient = chatClient.build();
        this.websiteTools = websiteTools;
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
                    .tools(websiteTools)
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
