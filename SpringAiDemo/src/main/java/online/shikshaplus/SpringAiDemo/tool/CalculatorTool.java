package online.shikshaplus.SpringAiDemo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTool {

//    @Tool(description = "Evaluates an advanced mathematical expression string including +, -, *, /, ^ (power), % (modulus), parentheses, and functions like sqrt, sin, cos, tan, log, ln, abs.")
//    public String evaluateExpression(@ToolParam(description = "The mathematical expression to evaluate, e.g. 'sqrt(16) + 4 * (3 - 1)^2'") String expression) {
//        try {
//            double result = ExpressionEvaluator.eval(expression);
//            return String.format("%s = %s", expression, formatResult(result));
//        } catch (Exception e) {
//            return "Error evaluating expression '" + expression + "': " + e.getMessage();
//        }
//    }

    @Tool(description = "Performs basic and power arithmetic operations between two numbers.")
    public String calculate(
            @ToolParam(description = "The first operand") double a,
            @ToolParam(description = "The second operand") double b,
            @ToolParam(description = "Operation to perform: 'add', 'subtract', 'multiply', 'divide', 'power', 'modulo'") String operation) {
        System.out.println("calculator tool call");
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

    //    @Tool(description = "Calculates the square root of a given number.")
//    public String squareRoot(@ToolParam(description = "The number to calculate square root for (must be non-negative)") double number) {
//        if (number < 0) {
//            return "Error: Square root of a negative number is not a real number.";
//        }
//        return formatResult(Math.sqrt(number));
//    }
//
//    @Tool(description = "Calculates the percentage of a given value (e.g. what is X percent of Y).")
//    public String calculatePercentage(
//            @ToolParam(description = "The base value") double value,
//            @ToolParam(description = "The percentage rate (e.g. 15 for 15%)") double percentage) {
//        double result = (value * percentage) / 100.0;
//        return formatResult(result);
//    }
//
//    @Tool(description = "Calculates the factorial of a non-negative integer (n!).")
//    public String factorial(@ToolParam(description = "Non-negative integer (n <= 170)") int n) {
//        if (n < 0) {
//            return "Error: Factorial is not defined for negative numbers.";
//        }
//        if (n > 170) {
//            return "Error: Factorial exceeds double floating-point capacity.";
//        }
//        double result = 1.0;
//        for (int i = 2; i <= n; i++) {
//            result *= i;
//        }
//        return formatResult(result);
//    }
//
//    @Tool(description = "Calculates trigonometric functions: sin, cos, tan in degrees or radians.")
//    public String trigonometry(
//            @ToolParam(description = "Trigonometric function: 'sin', 'cos', 'tan'") String function,
//            @ToolParam(description = "The angle value") double angle,
//            @ToolParam(description = "Unit of angle: 'deg' for degrees, 'rad' for radians") String unit) {
//        try {
//            double radians = "rad".equalsIgnoreCase(unit) ? angle : Math.toRadians(angle);
//            double result = switch (function.toLowerCase().trim()) {
//                case "sin" -> Math.sin(radians);
//                case "cos" -> Math.cos(radians);
//                case "tan" -> {
//                    if (Math.abs(Math.cos(radians)) < 1e-15) {
//                        throw new ArithmeticException("Tangent is undefined for this angle (cos is 0).");
//                    }
//                    yield Math.tan(radians);
//                }
//                default -> throw new IllegalArgumentException("Unsupported function: " + function);
//            };
//            return formatResult(result);
//        } catch (Exception e) {
//            return "Error: " + e.getMessage();
//        }
//    }
//
//    @Tool(description = "Calculates natural log (ln), log base 10, or log with a custom base.")
//    public String logarithm(
//            @ToolParam(description = "The input value (must be > 0)") double number,
//            @ToolParam(description = "Log base (e.g. 'e' for natural log, '10' for common log, or any custom positive base != 1)") String base) {
//        if (number <= 0) {
//            return "Error: Logarithm is undefined for non-positive numbers.";
//        }
//        try {
//            double result;
//            if ("e".equalsIgnoreCase(base.trim()) || "ln".equalsIgnoreCase(base.trim())) {
//                result = Math.log(number);
//            } else {
//                double baseVal = Double.parseDouble(base);
//                if (baseVal <= 0 || baseVal == 1) {
//                    return "Error: Logarithm base must be positive and not equal to 1.";
//                }
//                result = Math.log(number) / Math.log(baseVal);
//            }
//            return formatResult(result);
//        } catch (Exception e) {
//            return "Error: " + e.getMessage();
//        }
//    }
//
//    @Tool(description = "Calculates compound interest and total accumulated amount.")
//    public String compoundInterest(
//            @ToolParam(description = "Principal amount") double principal,
//            @ToolParam(description = "Annual interest rate in percentage (e.g. 7.5 for 7.5%)") double ratePercent,
//            @ToolParam(description = "Number of times interest is compounded per year (e.g. 12 for monthly, 1 for annually)") int compoundingFrequencyPerYear,
//            @ToolParam(description = "Time duration in years") double years) {
//        if (principal < 0 || ratePercent < 0 || compoundingFrequencyPerYear <= 0 || years < 0) {
//            return "Error: Principal, rate, compounding frequency, and years must be positive.";
//        }
//        double r = ratePercent / 100.0;
//        double amount = principal * Math.pow(1 + (r / compoundingFrequencyPerYear), compoundingFrequencyPerYear * years);
//        double totalInterest = amount - principal;
//        return String.format("Total Amount: %s, Total Interest: %s", formatResult(amount), formatResult(totalInterest));
//    }
//
    private static String formatResult(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.US, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }


//    /**
//     * Recursive descent parser to safely evaluate arithmetic and mathematical expressions.
//     */
//    private static class ExpressionEvaluator {
//
//        public static double eval(final String str) {
//            return new Object() {
//                int pos = -1, ch;
//
//                void nextChar() {
//                    ch = (++pos < str.length()) ? str.charAt(pos) : -1;
//                }
//
//                boolean eat(int charToEat) {
//                    while (ch == ' ') nextChar();
//                    if (ch == charToEat) {
//                        nextChar();
//                        return true;
//                    }
//                    return false;
//                }
//
//                double parse() {
//                    nextChar();
//                    double x = parseExpression();
//                    if (pos < str.length()) throw new IllegalArgumentException("Unexpected character: " + (char) ch);
//                    return x;
//                }
//
//                double parseExpression() {
//                    double x = parseTerm();
//                    for (;;) {
//                        if (eat('+')) x += parseTerm();
//                        else if (eat('-')) x -= parseTerm();
//                        else return x;
//                    }
//                }
//
//                double parseTerm() {
//                    double x = parseFactor();
//                    for (;;) {
//                        if (eat('*')) x *= parseFactor();
//                        else if (eat('/')) {
//                            double divisor = parseFactor();
//                            if (divisor == 0) throw new ArithmeticException("Division by zero");
//                            x /= divisor;
//                        } else if (eat('%')) {
//                            double divisor = parseFactor();
//                            if (divisor == 0) throw new ArithmeticException("Modulo by zero");
//                            x %= divisor;
//                        } else return x;
//                    }
//                }
//
//                double parseFactor() {
//                    if (eat('+')) return +parseFactor();
//                    if (eat('-')) return -parseFactor();
//
//                    double x;
//                    int startPos = this.pos;
//                    if (eat('(')) {
//                        x = parseExpression();
//                        if (!eat(')')) throw new IllegalArgumentException("Missing ')'");
//                    } else if ((ch >= '0' && ch <= '9') || ch == '.') {
//                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
//                        x = Double.parseDouble(str.substring(startPos, this.pos));
//                    } else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
//                        while (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') nextChar();
//                        String func = str.substring(startPos, this.pos).toLowerCase();
//                        if (eat('(')) {
//                            x = parseExpression();
//                            if (!eat(')')) throw new IllegalArgumentException("Missing ')' after function argument: " + func);
//                        } else {
//                            x = parseFactor();
//                        }
//                        x = switch (func) {
//                            case "sqrt" -> Math.sqrt(x);
//                            case "sin" -> Math.sin(Math.toRadians(x));
//                            case "cos" -> Math.cos(Math.toRadians(x));
//                            case "tan" -> Math.tan(Math.toRadians(x));
//                            case "abs" -> Math.abs(x);
//                            case "log" -> Math.log10(x);
//                            case "ln" -> Math.log(x);
//                            case "exp" -> Math.exp(x);
//                            default -> throw new IllegalArgumentException("Unknown function: " + func);
//                        };
//                    } else {
//                        throw new IllegalArgumentException("Unexpected character: " + (char) ch);
//                    }
//
//                    if (eat('^')) x = Math.pow(x, parseFactor());
//
//                    return x;
//                }
//            }.parse();
//        }
//    }
//}
}