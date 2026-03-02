package Services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GroqChatService {

    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public GroqChatService(String apiKey) {
        this(apiKey, "openai/gpt-oss-120b");
    }

    public GroqChatService(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("GROQ API key is required.");
        }
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? "openai/gpt-oss-120b" : model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String chat(String userMessage) throws IOException, InterruptedException {
        String payload = buildPayload(userMessage, false);
        String raw = callApi(payload);
        return extractAssistantContent(raw);
    }

    public String chatWithContext(String systemMessage, String contextData, String userMessage)
            throws IOException, InterruptedException {
        String payload = buildPayloadWithContext(systemMessage, contextData, userMessage, false);
        String raw = callApi(payload);
        return extractAssistantContent(raw);
    }

    public String chatRawStreamMode(String userMessage) throws IOException, InterruptedException {
        String payload = buildPayload(userMessage, true);
        return callApi(payload);
    }

    private String callApi(String jsonPayload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Groq API error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private String buildPayload(String userMessage, boolean stream) {
        String safeContent = escapeJson(userMessage == null ? "" : userMessage);
        return "{"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + safeContent + "\"}],"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"temperature\":1,"
                + "\"max_completion_tokens\":8192,"
                + "\"top_p\":1,"
                + "\"stream\":" + stream + ","
                + "\"reasoning_effort\":\"medium\","
                + "\"stop\":null"
                + "}";
    }

    private String buildPayloadWithContext(String systemMessage, String contextData, String userMessage, boolean stream) {
        String safeSystem = escapeJson(systemMessage == null ? "" : systemMessage);
        String safeContext = escapeJson(contextData == null ? "" : contextData);
        String safeContent = escapeJson(userMessage == null ? "" : userMessage);
        return "{"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + safeSystem + "\"},"
                + "{\"role\":\"system\",\"content\":\"" + safeContext + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + safeContent + "\"}"
                + "],"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"temperature\":1,"
                + "\"max_completion_tokens\":8192,"
                + "\"top_p\":1,"
                + "\"stream\":" + stream + ","
                + "\"reasoning_effort\":\"medium\","
                + "\"stop\":null"
                + "}";
    }

    private String extractAssistantContent(String json) {
        if (json == null || json.isBlank()) return "";

        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return json;

        int i = start + marker.length();
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (escaping) {
                switch (c) {
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    case '/': out.append('/'); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'u':
                        if (i + 4 <= json.length()) {
                            String hex = json.substring(i, i + 4);
                            try {
                                out.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                out.append("\\u").append(hex);
                                i += 4;
                            }
                        }
                        break;
                    default:
                        out.append(c);
                }
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
