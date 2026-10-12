package online.shikshaplus.ChatBot.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class TerminalTool {

    // Blacklist of destructive command keywords
    private static final Set<String> BLOCKED_PATTERNS = new HashSet<>(Arrays.asList(
            "rmdir", "del", "erase", "format", "shutdown", "reboot", "reg", "taskkill",
            "takeown", "icacls", "mkfs", "dd", "rm -rf", "drop", "truncate"
    ));

    @Tool(name = "terminal", description = "Executes safe terminal / shell commands (e.g. 'ping 8.8.8.8', 'dir', 'whoami', 'node -v', 'java -version', 'systeminfo') with sandboxed output.")
    public String executeCommand(@ToolParam(description = "The terminal command to execute, e.g. 'ping 8.8.8.8', 'dir', 'node -v', 'whoami'") String command) {
        if (command == null || command.trim().isEmpty()) {
            return "Error: Command cannot be empty.";
        }

        String trimmed = command.trim();

        // Safety verification: check for dangerous commands
        String lower = trimmed.toLowerCase();
        for (String blocked : BLOCKED_PATTERNS) {
            if (lower.matches(".*\\b" + blocked + "\\b.*")) {
                return "Command execution blocked by security policy: dangerous operation '" + blocked + "' detected.";
            }
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder processBuilder;

        if (isWindows) {
            processBuilder = new ProcessBuilder("cmd.exe", "/c", trimmed);
        } else {
            processBuilder = new ProcessBuilder("sh", "-c", trimmed);
        }

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), isWindows ? "GBK" : StandardCharsets.UTF_8.name()))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    lineCount++;
                    // Prevent context overflow: truncate after 80 lines or 4000 chars
                    if (lineCount >= 80 || output.length() > 4000) {
                        output.append("\n... [Output truncated for length]");
                        break;
                    }
                }
            } catch (Exception e) {
                // Fallback to UTF-8 if charset reading fails
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && output.length() < 4000) {
                        output.append(line).append("\n");
                    }
                }
            }

            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return output + "\n[Command timed out after 5 seconds]";
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();
            if (result.isEmpty()) {
                return "[Command completed with exit code " + exitCode + " (no output)]";
            }
            return result;

        } catch (Exception e) {
            return "Error executing command '" + trimmed + "': " + e.getMessage();
        }
    }
}
