package online.shikshaplus.ChatBot.tools;

import online.shikshaplus.ChatBot.dto.ToolInfo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ToolRegistry {

    private final TerminalTool terminalTool;
    private final CalculatorTool calculatorTool;
    private final WeatherTool weatherTool;
    private final CurrencyTool currencyTool;
    private final WebSearchTool webSearchTool;

    private final Map<String, ToolInfo> toolMetadata = new LinkedHashMap<>();
    private final Map<String, Object> toolInstances = new LinkedHashMap<>();

    public ToolRegistry(
            TerminalTool terminalTool,
            CalculatorTool calculatorTool,
            WeatherTool weatherTool,
            CurrencyTool currencyTool,
            WebSearchTool webSearchTool
    ) {
        this.terminalTool = terminalTool;
        this.calculatorTool = calculatorTool;
        this.weatherTool = weatherTool;
        this.currencyTool = currencyTool;
        this.webSearchTool = webSearchTool;

        registerTool("search", "Web Search", "Live internet search and real-time news retrieval.", "globe", 400, true, webSearchTool);
        registerTool("calculator", "Calculator", "Evaluates math expressions, formulas, and arithmetic.", "calculator", 300, true, calculatorTool);
        registerTool("weather", "Weather", "Live weather conditions for any location via Open-Meteo.", "weather", 350, true, weatherTool);
        registerTool("currency", "Currency", "Real-time foreign exchange currency converter.", "currency", 350, false, currencyTool);
        registerTool("terminal", "Terminal", "Safe command-line diagnostic execution.", "terminal", 400, false, terminalTool);
    }

    private void registerTool(String id, String name, String description, String icon, int tokenCost, boolean enabledByDefault, Object instance) {
        toolMetadata.put(id, new ToolInfo(id, name, description, icon, tokenCost, enabledByDefault));
        toolInstances.put(id, instance);
    }

    public List<ToolInfo> getAllToolInfos() {
        return new ArrayList<>(toolMetadata.values());
    }

    public ToolInfo getToolInfo(String id) {
        return toolMetadata.get(id);
    }

    public Object getToolInstance(String id) {
        return toolInstances.get(id);
    }

    public List<Object> resolveActiveToolInstances(List<String> activeToolIds) {
        List<Object> resolved = new ArrayList<>();
        if (activeToolIds == null || activeToolIds.isEmpty()) {
            return resolved;
        }

        for (String id : activeToolIds) {
            Object instance = toolInstances.get(id.toLowerCase().trim());
            if (instance != null) {
                resolved.add(instance);
            }
        }
        return resolved;
    }

    public int calculateTotalToolTokenCost() {
        return toolMetadata.values().stream().mapToInt(ToolInfo::getEstimatedTokenCost).sum();
    }

    public int calculateActiveToolTokenCost(List<String> activeToolIds) {
        if (activeToolIds == null || activeToolIds.isEmpty()) {
            return 0;
        }
        int cost = 0;
        for (String id : activeToolIds) {
            ToolInfo info = toolMetadata.get(id.toLowerCase().trim());
            if (info != null) {
                cost += info.getEstimatedTokenCost();
            }
        }
        return cost;
    }

    public int calculateSavedTokens(List<String> activeToolIds) {
        return calculateTotalToolTokenCost() - calculateActiveToolTokenCost(activeToolIds);
    }
}
