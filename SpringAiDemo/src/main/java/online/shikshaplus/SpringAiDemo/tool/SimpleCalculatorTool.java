package online.shikshaplus.SpringAiDemo.tool;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class SimpleCalculatorTool {
    @Tool(description = "Performs basic and power arithmetic operations between two numbers.")
    public String calculate(
            @ToolParam(description = "The first operand") double a,
            @ToolParam(description = "The second operand") double b,
            @ToolParam(description = "Operation to perform: 'add', 'subtract', 'multiply', 'divide', 'power', 'modulo'") String operation) {
        try {
            double result = switch (operation.toLowerCase().trim()) {
                case "add", "+", "plus" -> a + b;
                case "subtract", "-", "minus" -> a - b;
                case "multiply", "*", "times" -> a * b;
                case "divide", "/", "div" -> {
                    if (b == 0) throw new ArithmeticException("Division by zero is not allowed.");
                    yield a / b;
                }
                case "power", "^", "pow" -> Math.pow(a, b);
                case "modulo", "%", "mod" -> {
                    if (b == 0) throw new ArithmeticException("Modulo by zero is not allowed.");
                    yield a % b;
                }
                default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
            };
            return formatResult(result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String formatResult(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.US, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
