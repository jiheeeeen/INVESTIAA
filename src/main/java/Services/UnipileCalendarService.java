package Services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashSet;

public class UnipileCalendarService {
    private final String googleClientId;
    private final String googleClientSecret;
    private final String googleRefreshToken;
    private final String googleCalendarId;
    private final HttpClient httpClient;

    public UnipileCalendarService() {
        this.googleClientId = getenv("GOOGLE_CLIENT_ID", "");
        this.googleClientSecret = getenv("GOOGLE_CLIENT_SECRET", "");
        this.googleRefreshToken = getenv("GOOGLE_REFRESH_TOKEN", "");
        this.googleCalendarId = getenv("GOOGLE_CALENDAR_ID", "primary");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public boolean isConfigured() {
        return !googleClientId.isBlank() && !googleClientSecret.isBlank() && !googleRefreshToken.isBlank();
    }

    public String getCalendarEmbedUrl() {
        if (googleCalendarId == null || googleCalendarId.isBlank()) return "";
        String src = URLEncoder.encode(googleCalendarId, StandardCharsets.UTF_8);
        return "https://calendar.google.com/calendar/embed?src=" + src
                + "&ctz=Africa%2FTunis"
                + "&mode=WEEK";
    }

    public String createCalendarEventForTask(String title, String description, LocalDate startDate, LocalDate endDate) {
        if (!isConfigured()) return "ERROR:GOOGLE_CALENDAR_NOT_CONFIGURED";
        try {
            String accessToken = fetchAccessToken();
            if (accessToken.startsWith("ERROR:")) return accessToken;

            LocalDate start = startDate == null ? LocalDate.now() : startDate;
            LocalDate end = endDate == null ? start : endDate;
            if (end.isBefore(start)) end = start;

            String endpoint = "https://www.googleapis.com/calendar/v3/calendars/"
                    + URLEncoder.encode(googleCalendarId, StandardCharsets.UTF_8)
                    + "/events";
            String payload = "{"
                    + "\"summary\":\"" + esc(title) + "\","
                    + "\"description\":\"" + esc(description == null ? "" : description) + "\","
                    + "\"start\":{\"date\":\"" + start + "\"},"
                    + "\"end\":{\"date\":\"" + end.plusDays(1) + "\"}"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "ERROR:GOOGLE_HTTP_" + response.statusCode() + ":" + truncate(response.body(), 300);
            }
            String eventId = parseEventId(response.body());
            if (eventId.isBlank()) return "ERROR:GOOGLE_EVENT_ID_MISSING";
            return "OK:" + eventId;
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private String fetchAccessToken() {
        try {
            String body = "client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(googleClientSecret, StandardCharsets.UTF_8)
                    + "&refresh_token=" + URLEncoder.encode(googleRefreshToken, StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token";

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String payload = truncate(response.body(), 600);
                String oauthError = extractJsonString(payload, "error");
                if (response.statusCode() == 401 && "unauthorized_client".equalsIgnoreCase(oauthError)) {
                    return "ERROR:GOOGLE_OAUTH_UNAUTHORIZED_CLIENT:"
                            + " OAuth client not allowed. Verify GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET/GOOGLE_REFRESH_TOKEN belong to the same GCP OAuth app, "
                            + "Calendar API is enabled, and your account is allowed as a test user if consent screen is in testing mode.";
                }
                return "ERROR:GOOGLE_OAUTH_HTTP_" + response.statusCode() + ":" + payload;
            }
            String token = extractJsonString(response.body(), "access_token");
            return token.isBlank() ? "ERROR:GOOGLE_ACCESS_TOKEN_MISSING" : token;
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String parseEventId(String json) {
        return extractJsonString(json, "id");
    }

    private static String extractJsonString(String json, String key) {
        if (json == null || key == null || key.isBlank()) return "";
        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) return "";
        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) return "";
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return "";
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote <= firstQuote) return "";
        return json.substring(firstQuote + 1, secondQuote);
    }

    private static String getenv(String key, String def) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v.trim();
        String p = System.getProperty(key);
        if (p != null && !p.isBlank()) return p.trim();
        String dot = readFromDotEnv(key);
        if (dot != null && !dot.isBlank()) return dot.trim();
        return def;
    }

    private static String readFromDotEnv(String key) {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        Path current = Path.of("").toAbsolutePath().normalize();
        candidates.add(current.resolve(".env"));
        candidates.add(current.resolve("..").resolve(".env").normalize());
        candidates.add(current.resolve("..").resolve("..").resolve(".env").normalize());
        candidates.add(Path.of(System.getProperty("user.home"), ".env"));

        for (Path dotEnv : candidates) {
            if (!Files.exists(dotEnv)) continue;
            try {
                for (String rawLine : Files.readAllLines(dotEnv, StandardCharsets.UTF_8)) {
                    String line = rawLine == null ? "" : rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                    int idx = line.indexOf('=');
                    String k = line.substring(0, idx).trim();
                    if (!key.equals(k)) continue;
                    String value = line.substring(idx + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String esc(String s) {
        return (s == null ? "" : s)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
