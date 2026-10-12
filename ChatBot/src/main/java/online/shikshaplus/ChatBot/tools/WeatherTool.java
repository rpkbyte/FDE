package online.shikshaplus.ChatBot.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class WeatherTool {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WeatherTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Tool(name = "weather", description = "Retrieves live weather conditions and temperature for any city or location worldwide.")
    public String getWeather(@ToolParam(description = "City or location name, e.g. 'London', 'New York', 'Tokyo', 'Mumbai'") String location) {
        if (location == null || location.trim().isEmpty()) {
            return "Error: Location cannot be empty.";
        }

        try {
            // 1. Geocode location to lat/lon using Open-Meteo Geocoding API (free, no API key needed)
            String encodedLoc = URLEncoder.encode(location.trim(), StandardCharsets.UTF_8);
            String geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedLoc + "&count=1&language=en&format=json";

            HttpRequest geoRequest = HttpRequest.newBuilder()
                    .uri(URI.create(geocodingUrl))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Kortex-ChatBot/1.0")
                    .GET()
                    .build();

            HttpResponse<String> geoResponse = httpClient.send(geoRequest, HttpResponse.BodyHandlers.ofString());
            if (geoResponse.statusCode() != 200) {
                return "Error geocoding location: HTTP " + geoResponse.statusCode();
            }

            JsonNode geoJson = objectMapper.readTree(geoResponse.body());
            JsonNode results = geoJson.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return "Location '" + location + "' not found. Please specify a valid city name.";
            }

            JsonNode firstMatch = results.get(0);
            double latitude = firstMatch.get("latitude").asDouble();
            double longitude = firstMatch.get("longitude").asDouble();
            String resolvedName = firstMatch.get("name").asText();
            String country = firstMatch.has("country") ? firstMatch.get("country").asText() : "";
            String admin1 = firstMatch.has("admin1") ? firstMatch.get("admin1").asText() : "";

            // 2. Fetch current weather from Open-Meteo Forecast API
            String forecastUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&wind_speed_unit=kmh",
                    latitude, longitude
            );

            HttpRequest weatherRequest = HttpRequest.newBuilder()
                    .uri(URI.create(forecastUrl))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Kortex-ChatBot/1.0")
                    .GET()
                    .build();

            HttpResponse<String> weatherResponse = httpClient.send(weatherRequest, HttpResponse.BodyHandlers.ofString());
            if (weatherResponse.statusCode() != 200) {
                return "Error fetching weather data: HTTP " + weatherResponse.statusCode();
            }

            JsonNode weatherJson = objectMapper.readTree(weatherResponse.body());
            JsonNode current = weatherJson.get("current");
            if (current == null) {
                return "Weather data unavailable for " + resolvedName;
            }

            double temp = current.get("temperature_2m").asDouble();
            double apparentTemp = current.get("apparent_temperature").asDouble();
            int humidity = current.get("relative_humidity_2m").asInt();
            double windSpeed = current.get("wind_speed_10m").asDouble();
            int weatherCode = current.get("weather_code").asInt();
            String weatherDesc = getWeatherDescription(weatherCode);

            return String.format(
                    "Location: %s%s, %s | Temperature: %.1f°C (Feels like %.1f°C) | Condition: %s | Humidity: %d%% | Wind Speed: %.1f km/h",
                    resolvedName,
                    admin1.isEmpty() ? "" : " (" + admin1 + ")",
                    country,
                    temp,
                    apparentTemp,
                    weatherDesc,
                    humidity,
                    windSpeed
            );
        } catch (Exception e) {
            return "Failed to fetch weather for " + location + ": " + e.getMessage();
        }
    }

    private String getWeatherDescription(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow fall";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Cloudy (" + code + ")";
        };
    }
}
