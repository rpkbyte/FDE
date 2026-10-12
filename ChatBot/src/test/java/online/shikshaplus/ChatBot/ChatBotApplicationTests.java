package online.shikshaplus.ChatBot;

import online.shikshaplus.ChatBot.tools.CalculatorTool;
import online.shikshaplus.ChatBot.tools.TerminalTool;
import online.shikshaplus.ChatBot.tools.ToolRegistry;
import online.shikshaplus.ChatBot.tools.WebSearchTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ChatBotApplication.class)
class ChatBotApplicationTests {

    @Autowired
    private CalculatorTool calculatorTool;

    @Autowired
    private TerminalTool terminalTool;

    @Autowired
    private WebSearchTool webSearchTool;

    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void contextLoads() {
        assertNotNull(calculatorTool);
        assertNotNull(terminalTool);
        assertNotNull(webSearchTool);
        assertNotNull(toolRegistry);
    }

    @Test
    void testCalculatorTool() {
        String result = calculatorTool.calculate("sqrt(144) + 15 * 2");
        assertEquals("42", result);

        String divZero = calculatorTool.calculate("10 / 0");
        assertTrue(divZero.contains("Error"));
    }

    @Test
    void testTerminalToolSafety() {
        // Safe command
        String echoRes = terminalTool.executeCommand("echo test_kortex_terminal");
        assertTrue(echoRes.contains("test_kortex_terminal"));

        // Dangerous command blocked
        String blockedRes = terminalTool.executeCommand("rmdir /s /q test");
        assertTrue(blockedRes.contains("blocked by security policy"));
    }

    @Test
    void testWebSearchTool() {
        String result = webSearchTool.searchWeb("artificial intelligence");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testToolRegistrySavings() {
        assertEquals(5, toolRegistry.getAllToolInfos().size());

        // When no tools active, saved tokens should equal total token cost
        int total = toolRegistry.calculateTotalToolTokenCost();
        int savedWhenEmpty = toolRegistry.calculateSavedTokens(List.of());
        assertEquals(total, savedWhenEmpty);

        // When only calculator is active
        int savedWhenCalcOnly = toolRegistry.calculateSavedTokens(List.of("calculator"));
        assertTrue(savedWhenCalcOnly < total);
        assertTrue(savedWhenCalcOnly > 0);
    }
}
