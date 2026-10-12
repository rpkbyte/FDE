package online.shikshaplus.ChatBot.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTool {

    @Tool(name = "calculator", description = "Evaluates mathematical and arithmetic expressions. Supports +, -, *, /, %, ^, sqrt, sin, cos, tan, log, abs, and parentheses.")
    public String calculate(@ToolParam(description = "Mathematical expression to evaluate, e.g. 'sqrt(144) + 15 * 2' or '(100 - 32) * 5 / 9'") String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "Error: Expression cannot be empty.";
        }
        try {
            double result = evaluateExpression(expression);
            // Format cleanly: if integer, show without decimal
            if (result == (long) result) {
                return String.format("%d", (long) result);
            } else {
                return String.format("%.6f", result).replaceAll("0+$", "").replaceAll("\\.$", "");
            }
        } catch (Exception e) {
            return "Error evaluating expression: " + e.getMessage();
        }
    }

    /**
     * Fast, safe recursive descent parser for mathematical expressions.
     */
    public double evaluateExpression(String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new IllegalArgumentException("Unexpected character: '" + (char) ch + "'");
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor | term `%` factor
            // factor = `+` factor | `-` factor | `(` expression `)` | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    } else if (eat('%')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Modulo by zero");
                        x %= divisor;
                    } else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    if (!eat(')')) throw new IllegalArgumentException("Missing closing parenthesis ')'");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
                    while (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') nextChar();
                    String func = str.substring(startPos, this.pos).toLowerCase();
                    if (func.equals("pi")) {
                        x = Math.PI;
                    } else if (func.equals("e")) {
                        x = Math.E;
                    } else {
                        double arg = parseFactor();
                        x = switch (func) {
                            case "sqrt" -> Math.sqrt(arg);
                            case "sin" -> Math.sin(Math.toRadians(arg));
                            case "cos" -> Math.cos(Math.toRadians(arg));
                            case "tan" -> Math.tan(Math.toRadians(arg));
                            case "log" -> Math.log10(arg);
                            case "ln" -> Math.log(arg);
                            case "abs" -> Math.abs(arg);
                            case "round" -> (double) Math.round(arg);
                            case "ceil" -> Math.ceil(arg);
                            case "floor" -> Math.floor(arg);
                            default -> throw new IllegalArgumentException("Unknown function or constant: " + func);
                        };
                    }
                } else {
                    throw new IllegalArgumentException("Unexpected token: '" + (char) ch + "'");
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }
}
