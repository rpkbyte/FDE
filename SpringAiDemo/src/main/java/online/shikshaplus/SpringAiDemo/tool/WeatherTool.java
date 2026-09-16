package online.shikshaplus.SpringAiDemo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class WeatherTool {

    private final RestClient restClient;

    public WeatherTool() {
        this.restClient = RestClient.builder().build();
    }

    @Tool(description = "Fetches current weather information including temperature, weather conditions, and wind speed for a specified city or location.")
    public String getCurrentWeather(
            @ToolParam(description = "City or location name, e.g. 'Delhi', 'London', 'New York', 'Tokyo'") String location,
            @ToolParam(description = "Temperature unit: 'celsius' or 'fahrenheit' (default is celsius)") String unit) {
        System.out.println("weather tool call");
        if (location == null || location.isBlank()) {
            return "Error: Location must be specified.";
        }

        String tempUnit = (unit != null && unit.toLowerCase().contains("fahrenheit")) ? "fahrenheit" : "celsius";
        String unitSymbol = "fahrenheit".equals(tempUnit) ? "°F" : "°C";

        try {
            // Step 1: Geocode city name to coordinates
            @SuppressWarnings("unchecked")
            Map<String, Object> geoResponse = restClient.get()
                    .uri("https://geocoding-api.open-meteo.com/v1/search?name={name}&count=1&language=en&format=json", location.trim())
                    .retrieve()
                    .body(Map.class);

            if (geoResponse == null || !geoResponse.containsKey("results")) {
                return "Could not find location: " + location;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) geoResponse.get("results");
            if (results == null || results.isEmpty()) {
                return "Could not find location: " + location;
            }

            Map<String, Object> topResult = results.get(0);
            String cityName = (String) topResult.get("name");
            String country = (String) topResult.get("country");
            Number lat = (Number) topResult.get("latitude");
            Number lon = (Number) topResult.get("longitude");

            // Step 2: Fetch current weather for the coordinates
            @SuppressWarnings("unchecked")
            Map<String, Object> weatherResponse = restClient.get()
                    .uri("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true&temperature_unit={unit}",
                            lat, lon, tempUnit)
                    .retrieve()
                    .body(Map.class);

            if (weatherResponse == null || !weatherResponse.containsKey("current_weather")) {
                return "Could not retrieve weather data for " + location;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> currentWeather = (Map<String, Object>) weatherResponse.get("current_weather");
            Number temp = (Number) currentWeather.get("temperature");
            Number windSpeed = (Number) currentWeather.get("windspeed");
            Number weatherCode = (Number) currentWeather.get("weathercode");

            String condition = decodeWeatherCode(weatherCode != null ? weatherCode.intValue() : 0);
            String locationLabel = country != null ? cityName + ", " + country : cityName;

            return String.format("Current weather in %s: %.1f%s, %s (Wind Speed: %.1f km/h)",
                    locationLabel, temp.doubleValue(), unitSymbol, condition, windSpeed.doubleValue());

        } catch (Exception e) {
            return "Error fetching weather for " + location + ": " + e.getMessage();
        }
    }

    private String decodeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow fall";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Clear";
        };
    }
}
