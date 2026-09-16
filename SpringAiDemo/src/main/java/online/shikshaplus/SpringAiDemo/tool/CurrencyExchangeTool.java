package online.shikshaplus.SpringAiDemo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class CurrencyExchangeTool {

    private final RestClient restClient;

    public CurrencyExchangeTool() {
        this.restClient = RestClient.builder().build();
    }

    @Tool(description = "Converts an amount from one currency to another using live exchange rates.")
    public String convertCurrency(
            @ToolParam(description = "Amount of money to convert (e.g. 100, 25.5)") double amount,
            @ToolParam(description = "Source 3-letter currency code (e.g. USD, EUR, INR, GBP)") String from,
            @ToolParam(description = "Target 3-letter currency code (e.g. INR, USD, EUR, JPY)") String to) {
        System.out.println("convertCurrency tool call");
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            return "Error: Both source ('from') and target ('to') currency codes must be provided.";
        }

        String fromUpper = from.trim().toUpperCase();
        String toUpper = to.trim().toUpperCase();
        double validAmount = amount <= 0 ? 1.0 : amount;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("https://open.er-api.com/v6/latest/{from}", fromUpper)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !"success".equalsIgnoreCase((String) response.get("result"))) {
                return "Error: Could not retrieve exchange rates for currency: " + fromUpper;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            if (rates == null || !rates.containsKey(toUpper)) {
                return "Error: Target currency code not supported: " + toUpper;
            }

            Number rateValue = (Number) rates.get(toUpper);
            double rate = rateValue.doubleValue();
            double converted = validAmount * rate;

            return String.format("%.2f %s = %.2f %s (Exchange rate: 1 %s = %.4f %s)",
                    validAmount, fromUpper, converted, toUpper, fromUpper, rate, toUpper);

        } catch (Exception e) {
            return "Error fetching exchange rate: " + e.getMessage();
        }
    }

    @Tool(description = "Gets the current exchange rate from a base currency to a target currency.")
    public String getExchangeRate(
            @ToolParam(description = "Source 3-letter currency code (e.g. USD, EUR, INR)") String from,
            @ToolParam(description = "Target 3-letter currency code (e.g. INR, USD, EUR)") String to) {
        return convertCurrency(1.0, from, to);
    }
}
