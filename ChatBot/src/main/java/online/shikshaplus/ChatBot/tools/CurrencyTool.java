package online.shikshaplus.ChatBot.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class CurrencyTool {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CurrencyTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Tool(name = "currency", description = "Converts an amount between international currencies using live exchange rates (e.g. USD, EUR, INR, GBP, JPY, CAD, AUD).")
    public String convertCurrency(
            @ToolParam(description = "Numeric amount to convert, e.g. 100") double amount,
            @ToolParam(description = "3-letter base currency code, e.g. 'USD', 'EUR', 'INR'") String fromCurrency,
            @ToolParam(description = "3-letter target currency code, e.g. 'EUR', 'INR', 'GBP'") String toCurrency
    ) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.isBlank() || toCurrency.isBlank()) {
            return "Error: Both 'fromCurrency' and 'toCurrency' 3-letter codes must be provided.";
        }

        String from = fromCurrency.trim().toUpperCase();
        String to = toCurrency.trim().toUpperCase();

        if (from.equals(to)) {
            return String.format("%.2f %s = %.2f %s (1:1 exchange)", amount, from, amount, to);
        }

        try {
            // Use open, free Frankfurter foreign exchange rates API (backed by the European Central Bank)
            String url = String.format("https://api.frankfurter.dev/v1/latest?amount=%.2f&from=%s&to=%s", amount, from, to);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Kortex-ChatBot/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                JsonNode rates = json.get("rates");
                if (rates != null && rates.has(to)) {
                    double convertedAmount = rates.get(to).asDouble();
                    double rate = convertedAmount / amount;
                    return String.format("%.2f %s = %.2f %s (Rate: 1 %s = %.4f %s, Date: %s)",
                            amount, from, convertedAmount, to, from, rate, to, json.path("date").asText());
                }
            }

            // Fallback to open exchange API if Frankfurter doesn't have the currency pair (e.g., specific pairs)
            String fallbackUrl = "https://open.er-api.com/v6/latest/" + from;
            HttpRequest fallbackReq = HttpRequest.newBuilder()
                    .uri(URI.create(fallbackUrl))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> fbResponse = httpClient.send(fallbackReq, HttpResponse.BodyHandlers.ofString());
            if (fbResponse.statusCode() == 200) {
                JsonNode fbJson = objectMapper.readTree(fbResponse.body());
                JsonNode fbRates = fbJson.get("rates");
                if (fbRates != null && fbRates.has(to)) {
                    double rate = fbRates.get(to).asDouble();
                    double convertedAmount = amount * rate;
                    return String.format("%.2f %s = %.2f %s (Rate: 1 %s = %.4f %s)",
                            amount, from, convertedAmount, to, from, rate, to);
                }
            }

            return "Could not convert from " + from + " to " + to + ". Please check currency codes.";
        } catch (Exception e) {
            return "Currency conversion failed: " + e.getMessage();
        }
    }
}
