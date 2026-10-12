package online.shikshaplus.ChatBot.service;

import online.shikshaplus.ChatBot.dto.ChatRequest;
import online.shikshaplus.ChatBot.dto.StreamEvent;
import online.shikshaplus.ChatBot.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ToolRegistry toolRegistry;
    private final CalculatorTool calculatorTool;
    private final WeatherTool weatherTool;
    private final CurrencyTool currencyTool;
    private final TerminalTool terminalTool;
    private final WebSearchTool webSearchTool;

    @Autowired(required = false)
    private ChatModel chatModel;

    @Value("${spring.ai.openai.api-key:demo-key}")
    private String configuredApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String configuredBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:openrouter/free}")
    private String configuredModel;

    public ChatService(
            ToolRegistry toolRegistry,
            CalculatorTool calculatorTool,
            WeatherTool weatherTool,
            CurrencyTool currencyTool,
            TerminalTool terminalTool,
            WebSearchTool webSearchTool
    ) {
        this.toolRegistry = toolRegistry;
        this.calculatorTool = calculatorTool;
        this.weatherTool = weatherTool;
        this.currencyTool = currencyTool;
        this.terminalTool = terminalTool;
        this.webSearchTool = webSearchTool;
    }

    public boolean isRealKeyConfigured() {
        return configuredApiKey != null
                && !configuredApiKey.equals("demo-key")
                && !configuredApiKey.isBlank();
    }

    public String getConfiguredModel() {
        return configuredModel;
    }

    /**
     * Primary streaming chat method.
     * Dynamically orchestrates tools, emits real-time SSE telemetry, and streams AI response.
     */
    public Flux<ServerSentEvent<StreamEvent>> streamChat(ChatRequest request) {
        List<String> activeTools = request.getActiveTools() != null ? request.getActiveTools() : List.of();
        int savedTokens = toolRegistry.calculateSavedTokens(activeTools);
        int activeToolTokenCost = toolRegistry.calculateActiveToolTokenCost(activeTools);
        int estimatedPromptTokens = estimateTokens(request.getMessage()) + activeToolTokenCost + 40;

        String apiKeyToUse = (request.getApiKey() != null && !request.getApiKey().isBlank())
                ? request.getApiKey().trim()
                : configuredApiKey;

        boolean hasRealKey = apiKeyToUse != null && !apiKeyToUse.equals("demo-key") && !apiKeyToUse.isBlank();

        if (hasRealKey && chatModel != null) {
            return executeWithRealModel(request, activeTools, estimatedPromptTokens, savedTokens);
        } else {
            return executeSimulation(request, activeTools, estimatedPromptTokens, savedTokens);
        }
    }

    /**
     * Executes request with the real AI model (OpenRouter / OpenAI / Groq / etc.)
     * and live dynamic tool execution with real-time UI telemetry events.
     */
    private Flux<ServerSentEvent<StreamEvent>> executeWithRealModel(
            ChatRequest request,
            List<String> activeToolIds,
            int estimatedPromptTokens,
            int savedTokens
    ) {
        Sinks.Many<ServerSentEvent<StreamEvent>> sink = Sinks.many().unicast().onBackpressureBuffer();
        String msg = request.getMessage() != null ? request.getMessage().trim() : "";
        String lower = msg.toLowerCase();

        new Thread(() -> {
            try {
                // 1. Try to match and execute any active tools
                ToolExecutionResult toolResult = tryExecuteActiveTool(msg, lower, activeToolIds, sink);

                // 2. If user needed a tool that was deactivated in the UI, show token optimization message
                if (toolResult.isToolDeactivated()) {
                    streamWords(toolResult.deactivatedMessage(), sink);
                    emitCompletionMetrics(sink, estimatedPromptTokens, 60, savedTokens);
                    return;
                }

                // 3. Build prompt for ChatClient
                StringBuilder promptText = new StringBuilder();
                if (toolResult.hasExecuted()) {
                    promptText.append("User Query: \"").append(msg).append("\"\n\n");
                    promptText.append("Real-Time Live Tool Execution Data [").append(toolResult.toolName()).append("]:\n");
                    promptText.append("```\n").append(toolResult.output()).append("\n```\n\n");
                    promptText.append("Instructions: Synthesize a clear, helpful, direct response for the user using this live verified data. Maintain the user's language (e.g. Hindi if asked in Hindi, English if asked in English).");
                } else {
                    promptText.append(msg);
                }

                String systemPrompt = (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank())
                        ? request.getSystemPrompt()
                        : "You are Kortex, an ultra-fast, token-efficient AI assistant. Provide concise, helpful, and direct answers.";

                ChatClient chatClient = ChatClient.builder(chatModel).build();
                StringBuilder fullResponse = new StringBuilder();

                chatClient.prompt()
                        .system(systemPrompt)
                        .user(promptText.toString())
                        .stream()
                        .content()
                        .subscribe(
                                chunk -> {
                                    fullResponse.append(chunk);
                                    sink.tryEmitNext(ServerSentEvent.<StreamEvent>builder()
                                            .data(StreamEvent.token(chunk))
                                            .build());
                                },
                                error -> {
                                    log.warn("Real LLM stream failed ({}), falling back to local synthesis: {}", error.getMessage(), error.getClass().getSimpleName());
                                    try {
                                        if (toolResult.hasExecuted()) {
                                            String fallbackText = "\n\n*(Note: Live data fetched via " + toolResult.toolName() + ", AI response completed locally)*\n\n" + toolResult.output();
                                            streamWords(fallbackText, sink);
                                        } else {
                                            streamWords("\n\n⚠️ AI Model returned an error: " + error.getMessage() + ". Please check your API key or model availability.", sink);
                                        }
                                    } catch (Exception ignored) {}
                                    emitCompletionMetrics(sink, estimatedPromptTokens, estimateTokens(fullResponse.toString()), savedTokens);
                                },
                                () -> {
                                    emitCompletionMetrics(sink, estimatedPromptTokens, estimateTokens(fullResponse.toString()), savedTokens);
                                }
                        );

            } catch (Exception e) {
                log.error("Execution error in real model flow: {}", e.getMessage(), e);
                executeSimulationInSink(request, activeToolIds, estimatedPromptTokens, savedTokens, sink);
            }
        }).start();

        return sink.asFlux();
    }

    /**
     * Smart fallback simulation engine that runs tools live and generates realistic responses.
     */
    private Flux<ServerSentEvent<StreamEvent>> executeSimulation(
            ChatRequest request,
            List<String> activeToolIds,
            int estimatedPromptTokens,
            int savedTokens
    ) {
        Sinks.Many<ServerSentEvent<StreamEvent>> sink = Sinks.many().unicast().onBackpressureBuffer();
        new Thread(() -> executeSimulationInSink(request, activeToolIds, estimatedPromptTokens, savedTokens, sink)).start();
        return sink.asFlux();
    }

    private void executeSimulationInSink(
            ChatRequest request,
            List<String> activeToolIds,
            int estimatedPromptTokens,
            int savedTokens,
            Sinks.Many<ServerSentEvent<StreamEvent>> sink
    ) {
        String msg = request.getMessage() != null ? request.getMessage().trim() : "";
        String lower = msg.toLowerCase();

        try {
            Thread.sleep(80);
            ToolExecutionResult toolResult = tryExecuteActiveTool(msg, lower, activeToolIds, sink);

            if (toolResult.isToolDeactivated()) {
                streamWords(toolResult.deactivatedMessage(), sink);
            } else if (toolResult.hasExecuted()) {
                String narrative = switch (toolResult.toolName()) {
                    case "web_search" -> "### Real-Time Live Web Results\n\n" + toolResult.output() +
                            "\n\n*Verified live via Google News RSS & Wikipedia APIs without stale training-cutoff limitations.*";
                    case "weather" -> "### Live Meteorological Report\n\n> " + toolResult.output() +
                            "\n\n*Live meteorological data fetched dynamically from Open-Meteo REST service.*";
                    case "calculator" -> "Calculation completed:\n\n**" + toolResult.output() + "**";
                    case "currency" -> "### Live Exchange Conversion\n\n**" + toolResult.output() +
                            "**\n\n*Exchange rates sourced from European Central Bank reference points.*";
                    case "terminal" -> "Executed terminal command:\n\n```sh\n" + toolResult.output() + "\n```";
                    default -> toolResult.output();
                };
                streamWords(narrative, sink);
            } else {
                handleGeneralConversation(msg, lower, activeToolIds, savedTokens, sink);
            }

            emitCompletionMetrics(sink, estimatedPromptTokens, 85, savedTokens);

        } catch (Exception e) {
            log.error("Simulation error: {}", e.getMessage(), e);
            sink.tryEmitNext(ServerSentEvent.<StreamEvent>builder()
                    .data(StreamEvent.error("Error: " + e.getMessage()))
                    .build());
            sink.tryEmitComplete();
        }
    }

    /**
     * Checks if the message requires a tool and executes it if active.
     */
    private ToolExecutionResult tryExecuteActiveTool(
            String msg,
            String lower,
            List<String> activeToolIds,
            Sinks.Many<ServerSentEvent<StreamEvent>> sink
    ) throws InterruptedException {

        // 1. Live Web Search & Browsing
        if (isSearchQuery(lower, msg)) {
            if (!isToolActive("search", activeToolIds)) {
                return ToolExecutionResult.deactivated("search",
                        "The **Web Search** tool is currently deactivated to minimize prompt tokens.\n\n" +
                        "*Token Optimization*: Toggle on the `[Web Search]` pill in the toolbar above if you want me to browse live internet information and real-time news.");
            }
            String query = extractSearchQuery(msg);
            emitToolCall(sink, "web_search", "{\"query\": \"" + query + "\"}");
            Thread.sleep(250);
            String result = webSearchTool.searchWeb(query);
            emitToolResult(sink, "web_search", result);
            Thread.sleep(150);
            return ToolExecutionResult.success("web_search", "{\"query\": \"" + query + "\"}", result);
        }

        // 2. Weather
        if (isWeatherQuery(lower)) {
            if (!isToolActive("weather", activeToolIds)) {
                return ToolExecutionResult.deactivated("weather",
                        "The **Live Weather** tool is currently turned **OFF** for this request to optimize tokens.\n\n" +
                        "*Token Optimization*: Turn on the `[Weather]` pill in the tool selector toolbar if you want to fetch live meteorological data.");
            }
            String city = extractCity(msg);
            emitToolCall(sink, "weather", "{\"location\": \"" + city + "\"}");
            Thread.sleep(250);
            String result = weatherTool.getWeather(city);
            emitToolResult(sink, "weather", result);
            Thread.sleep(150);
            return ToolExecutionResult.success("weather", "{\"location\": \"" + city + "\"}", result);
        }

        // 3. Calculator
        if (isMathQuery(lower, msg)) {
            if (!isToolActive("calculator", activeToolIds)) {
                return ToolExecutionResult.deactivated("calculator",
                        "The **Calculator** tool is currently deactivated to minimize prompt tokens.\n\n" +
                        "To evaluate mathematical formulas and expressions accurately, toggle on the `[Calculator]` pill.");
            }
            String expr = extractMathExpression(msg);
            emitToolCall(sink, "calculator", "{\"expression\": \"" + expr + "\"}");
            Thread.sleep(200);
            String result = calculatorTool.calculate(expr);
            emitToolResult(sink, "calculator", "Result: " + result);
            Thread.sleep(150);
            return ToolExecutionResult.success("calculator", "{\"expression\": \"" + expr + "\"}", "Result: " + result);
        }

        // 4. Currency
        if (isCurrencyQuery(lower)) {
            if (!isToolActive("currency", activeToolIds)) {
                return ToolExecutionResult.deactivated("currency",
                        "The **Currency Converter** tool is deactivated to conserve tokens.\n\n" +
                        "Toggle the `[Currency]` pill in the toolbar to activate real-time foreign exchange lookups.");
            }
            double amount = 100.0;
            String from = "USD";
            String to = "INR";

            Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]{3})\\s+(?:to|in)\\s+([a-zA-Z]{3})", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(msg);
            if (m.find()) {
                amount = Double.parseDouble(m.group(1));
                from = m.group(2).toUpperCase();
                to = m.group(3).toUpperCase();
            } else if (lower.contains("usd") && lower.contains("eur")) {
                from = "USD";
                to = "EUR";
            }

            String args = "{\"amount\": " + amount + ", \"from\": \"" + from + "\", \"to\": \"" + to + "\"}";
            emitToolCall(sink, "currency", args);
            Thread.sleep(250);
            String result = currencyTool.convertCurrency(amount, from, to);
            emitToolResult(sink, "currency", result);
            Thread.sleep(150);
            return ToolExecutionResult.success("currency", args, result);
        }

        // 5. Terminal
        if (isTerminalQuery(lower, msg)) {
            if (!isToolActive("terminal", activeToolIds)) {
                return ToolExecutionResult.deactivated("terminal",
                        "The **Terminal** diagnostic tool is currently inactive.\n\n" +
                        "Turn on the `[Terminal]` toggle in the toolbar to enable controlled shell diagnostics.");
            }
            String cmd = extractCommand(msg);
            emitToolCall(sink, "terminal", "{\"command\": \"" + cmd + "\"}");
            Thread.sleep(250);
            String result = terminalTool.executeCommand(cmd);
            emitToolResult(sink, "terminal", result);
            Thread.sleep(150);
            return ToolExecutionResult.success("terminal", "{\"command\": \"" + cmd + "\"}", result);
        }

        return ToolExecutionResult.notApplicable();
    }

    private boolean isToolActive(String toolId, List<String> activeToolIds) {
        if (activeToolIds == null) return false;
        return activeToolIds.stream().anyMatch(t -> t.equalsIgnoreCase(toolId) || (toolId.equals("search") && t.equalsIgnoreCase("web_search")));
    }

    private void emitToolCall(Sinks.Many<ServerSentEvent<StreamEvent>> sink, String name, String args) {
        sink.tryEmitNext(ServerSentEvent.<StreamEvent>builder()
                .data(StreamEvent.toolCall(name, args))
                .build());
    }

    private void emitToolResult(Sinks.Many<ServerSentEvent<StreamEvent>> sink, String name, String result) {
        sink.tryEmitNext(ServerSentEvent.<StreamEvent>builder()
                .data(StreamEvent.toolResult(name, result))
                .build());
    }

    private void emitCompletionMetrics(Sinks.Many<ServerSentEvent<StreamEvent>> sink, int promptTokens, int completionTokens, int savedTokens) {
        sink.tryEmitNext(ServerSentEvent.<StreamEvent>builder()
                .data(StreamEvent.metrics(promptTokens, Math.max(completionTokens, 25), savedTokens))
                .build());
        sink.tryEmitNext(ServerSentEvent.<StreamEvent>builder()
                .data(StreamEvent.done())
                .build());
        sink.tryEmitComplete();
    }

    private boolean isSearchQuery(String lower, String original) {
        if (original.startsWith("http://") || original.startsWith("https://") || lower.contains("http://") || lower.contains("https://")) {
            return true;
        }
        return lower.contains("search")
                || lower.contains("browse")
                || lower.contains("news")
                || lower.contains("latest")
                || lower.contains("recent")
                || lower.contains("today")
                || lower.contains("current")
                || lower.contains("khabar")
                || lower.contains("samachar")
                || lower.contains("taaza")
                || lower.contains("dhoondo")
                || lower.contains("find out")
                || lower.contains("score")
                || lower.contains("stock price");
    }

    private boolean isWeatherQuery(String lower) {
        return lower.contains("weather")
                || lower.contains("temperature")
                || lower.contains("forecast")
                || lower.contains("climate")
                || lower.contains("mausam")
                || lower.contains("taapman")
                || lower.contains("tapman")
                || lower.contains("baarish")
                || lower.contains("barish")
                || lower.contains("humidity");
    }

    private boolean isMathQuery(String lower, String original) {
        if (lower.contains("calculate") || lower.contains("sqrt") || lower.contains("sin") || lower.contains("cos") || lower.contains("tan") || lower.contains("log") || lower.contains("hisaab") || lower.contains("hisab") || lower.contains("jod") || lower.contains("guna") || lower.contains("bhag") || lower.contains("solve")) {
            return true;
        }
        if (lower.contains("kitna hota") || lower.contains("kitna hoga") || lower.contains("what is") || lower.contains("equals")) {
            if (lower.matches(".*\\d.*[+\\-*/%^].*")) return true;
        }
        return original.matches("^[0-9\\s+\\-*/%^().sqrtasincoetanlog]+$") && original.matches(".*\\d.*");
    }

    private boolean isCurrencyQuery(String lower) {
        return lower.contains("convert")
                || lower.contains("currency")
                || lower.contains("exchange rate")
                || lower.contains("dollar")
                || lower.contains("rupee")
                || lower.contains("rupaye")
                || lower.contains("usd")
                || lower.contains("eur")
                || lower.contains("inr")
                || lower.contains("gbp")
                || lower.contains("jpy")
                || lower.contains("badlo");
    }

    private boolean isTerminalQuery(String lower, String msg) {
        return lower.startsWith("run ")
                || lower.startsWith("exec ")
                || lower.contains("ping ")
                || lower.equals("dir")
                || lower.equals("ls")
                || lower.contains("whoami")
                || lower.contains("systeminfo")
                || lower.contains("node -v")
                || lower.contains("java -version")
                || lower.contains("terminal")
                || lower.contains("cmd");
    }

    private void handleGeneralConversation(String msg, String lower, List<String> activeToolIds, int savedTokens, Sinks.Many<ServerSentEvent<StreamEvent>> sink) throws InterruptedException {
        int activeCount = activeToolIds.size();
        String response = "Hello! I am **Kortex**, your high-performance, token-optimized AI assistant.\n\n" +
                "### Current Session State:\n" +
                "- **Active Tools**: " + (activeCount == 0 ? "None (*Zero tool schema overhead!*)" : String.join(", ", activeToolIds)) + "\n" +
                "- **Tokens Saved**: ~" + savedTokens + " tokens per request by omitting unused tool schemas.\n\n" +
                "### Available On-Demand Tools:\n" +
                "1. `[Web Search]`: Real-time internet browsing and live news updates.\n" +
                "2. `[Calculator]`: Math and algebraic evaluator (`sqrt(256) + 40 * 3`).\n" +
                "3. `[Weather]`: Live global weather via Open-Meteo.\n" +
                "4. `[Currency]`: Live foreign exchange rate converter.\n" +
                "5. `[Terminal]`: Safe shell command executor (`node -v`, `ping 8.8.8.8`).\n\n" +
                "You can select any tools in the toolbar to activate them on-demand!";

        streamWords(response, sink);
    }

    private void streamWords(String text, Sinks.Many<ServerSentEvent<StreamEvent>> sink) throws InterruptedException {
        if (text == null) return;
        String[] words = text.split("(?<=\\s)|(?<=\\n)");
        for (String word : words) {
            sink.tryEmitNext(ServerSentEvent.<StreamEvent>builder()
                    .data(StreamEvent.token(word))
                    .build());
            Thread.sleep(16);
        }
    }

    private String extractCity(String msg) {
        // Pattern 1: "delhi ka mausam", "mumbai weather", "tokyo temperature"
        Pattern p1 = Pattern.compile("([a-zA-Z]+)\\s+(?:ka\\s+)?(?:mausam|weather|temperature|taapman|tapman)", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(msg);
        if (m1.find()) {
            return m1.group(1).trim();
        }

        // Pattern 2: "weather in delhi", "mausam delhi ka", "temperature of tokyo"
        Pattern p2 = Pattern.compile("(?:weather|mausam|temperature|taapman|forecast|climate)\\s+(?:in|of|at|for|ka|ke)?\\s*([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(msg);
        if (m2.find()) {
            String c = m2.group(1).trim();
            if (!c.equalsIgnoreCase("kaisa") && !c.equalsIgnoreCase("kya") && !c.equalsIgnoreCase("today") && !c.equalsIgnoreCase("now") && !c.equalsIgnoreCase("right")) {
                return c;
            }
        }

        // Pattern 3: "\\b(?:in|for|at|of)\\b\\s+([a-zA-Z]+)"
        Pattern p3 = Pattern.compile("\\b(?:in|for|at|of)\\b\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        Matcher m3 = p3.matcher(msg);
        if (m3.find()) {
            return m3.group(1).trim();
        }

        return "Delhi";
    }

    private String extractMathExpression(String msg) {
        Matcher m = Pattern.compile("([0-9\\s+\\-*/%^().sqrtasincoetanlogPIE]+)").matcher(msg);
        String longest = "";
        while (m.find()) {
            String candidate = m.group(1).trim();
            if (candidate.matches(".*\\d.*") && candidate.length() > longest.length()) {
                longest = candidate;
            }
        }
        if (!longest.isEmpty()) {
            return longest;
        }
        String cleaned = msg.replaceAll("(?i)(calculate|what is|compute|evaluate|equal to|equals|hisaab|kitna hoga|kitna hota hai|batao)", "").trim();
        return cleaned.replaceAll("[?]", "").trim();
    }

    private String extractCommand(String msg) {
        if (msg.startsWith("run ") || msg.startsWith("exec ")) {
            return msg.substring(4).trim();
        }
        if (msg.toLowerCase().contains("node -v")) return "node -v";
        if (msg.toLowerCase().contains("java -version")) return "java -version";
        if (msg.toLowerCase().contains("systeminfo")) return "systeminfo";
        if (msg.toLowerCase().contains("whoami")) return "whoami";
        if (msg.toLowerCase().contains("dir")) return "dir";
        if (msg.toLowerCase().contains("ping")) return "ping 8.8.8.8";
        return msg.trim();
    }

    private String extractSearchQuery(String msg) {
        String cleaned = msg.replaceAll("(?i)\\b(search for|search the web for|browse|look up|search|find latest news on|latest news on|latest news about|news on|news about|what is the latest on|aaj ki taaza khabar|taaza khabar|samachar|khabar|batao|kya hai)\\b", "").trim();
        cleaned = cleaned.replaceAll("[?.!]", "").trim();
        return cleaned.isEmpty() ? msg.trim() : cleaned;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 3.8);
    }

    private record ToolExecutionResult(
            boolean executed,
            boolean toolDeactivated,
            String toolName,
            String args,
            String output,
            String deactivatedMessage
    ) {
        public static ToolExecutionResult notApplicable() {
            return new ToolExecutionResult(false, false, null, null, null, null);
        }

        public static ToolExecutionResult success(String name, String args, String output) {
            return new ToolExecutionResult(true, false, name, args, output, null);
        }

        public static ToolExecutionResult deactivated(String name, String message) {
            return new ToolExecutionResult(false, true, name, null, null, message);
        }

        public boolean hasExecuted() {
            return executed;
        }

        public boolean isToolDeactivated() {
            return toolDeactivated;
        }
    }
}

