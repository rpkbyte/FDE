package online.shikshaplus.ChatBot.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebSearchTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebSearchTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Tool(name = "web_search", description = "Searches the live web and real-time news for current events, fresh updates, and facts, or fetches live web content from URLs.")
    public String searchWeb(@ToolParam(description = "Search query or URL to browse, e.g. 'latest AI news today', 'Nvidia stock news', 'https://github.com'") String query) {
        if (query == null || query.trim().isEmpty()) {
            return "Error: Search query cannot be empty.";
        }

        String trimmed = query.trim();

        // 1. If query is a direct URL, fetch the webpage content
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return fetchWebpageContent(trimmed);
        }

        StringBuilder results = new StringBuilder();

        // 2. Query Live Real-Time News via Google News RSS
        List<String> newsItems = fetchLiveNews(trimmed);
        if (!newsItems.isEmpty()) {
            results.append("### Real-Time Live News & Updates:\n");
            for (String item : newsItems) {
                results.append(item).append("\n\n");
            }
        }

        // 3. Query Factual Knowledge via Wikipedia Search API
        List<String> wikiItems = fetchWikiKnowledge(trimmed);
        if (!wikiItems.isEmpty()) {
            results.append("### Web Encyclopedia Knowledge:\n");
            for (String item : wikiItems) {
                results.append(item).append("\n\n");
            }
        }

        if (results.length() == 0) {
            return "No real-time search results found for query: '" + trimmed + "'.";
        }

        return results.toString().trim();
    }

    private List<String> fetchLiveNews(String query) {
        List<String> items = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://news.google.com/rss/search?q=" + encoded + "&hl=en-US&gl=US&ceid=US:en";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Kortex/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(response.body()));

                NodeList itemNodes = doc.getElementsByTagName("item");
                int count = Math.min(itemNodes.getLength(), 4);

                for (int i = 0; i < count; i++) {
                    Element elem = (Element) itemNodes.item(i);
                    String title = getTagValue("title", elem);
                    String pubDate = getTagValue("pubDate", elem);
                    String link = getTagValue("link", elem);

                    if (title != null && !title.isBlank()) {
                        items.add(String.format("- **%s**\n  *Date:* %s\n  *Source URL:* %s",
                                title.trim(),
                                pubDate != null ? pubDate.trim() : "Recent",
                                link != null ? link.trim() : "Web"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Live news fetch error for query '{}': {}", query, e.getMessage());
        }
        return items;
    }

    private List<String> fetchWikiKnowledge(String query) {
        List<String> items = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" + encoded + "&format=json&utf8=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "KortexAI/1.0 (https://kortex.local)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode searchArr = root.path("query").path("search");

                if (searchArr.isArray()) {
                    int count = Math.min(searchArr.size(), 2);
                    for (int i = 0; i < count; i++) {
                        JsonNode item = searchArr.get(i);
                        String title = item.path("title").asText();
                        String snippet = item.path("snippet").asText()
                                .replaceAll("<[^>]*>", "")
                                .replaceAll("&quot;", "\"")
                                .replaceAll("&#039;", "'")
                                .replaceAll("&amp;", "&");

                        items.add(String.format("- **%s**: %s...", title, snippet));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Wiki knowledge fetch error for query '{}': {}", query, e.getMessage());
        }
        return items;
    }

    private String fetchWebpageContent(String urlStr) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Kortex/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "Failed to fetch webpage: HTTP " + response.statusCode();
            }

            String body = response.body();
            // Clean HTML tags and scripts
            String cleanText = body.replaceAll("(?s)<script.*?</script>", "")
                    .replaceAll("(?s)<style.*?</style>", "")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (cleanText.length() > 2000) {
                cleanText = cleanText.substring(0, 2000) + "... [Truncated]";
            }

            return "### Content extracted from " + urlStr + ":\n\n" + cleanText;

        } catch (Exception e) {
            return "Error browsing webpage " + urlStr + ": " + e.getMessage();
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0 && nodeList.item(0).getFirstChild() != null) {
            return nodeList.item(0).getFirstChild().getNodeValue();
        }
        return null;
    }
}
