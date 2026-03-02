package Services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Properties;

public class MicrosoftGraphCalendarService {
    private static final Properties LOCAL_CONFIG = loadLocalConfig();
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;

    public boolean isConfigured() {
        String token = cfg("MS_GRAPH_ACCESS_TOKEN");
        return !token.isEmpty() && !cfg("MS_GRAPH_CALENDAR_ID").isEmpty();
    }

    public String createAllDayEvent(String transactionId, String subject, String bodyText, String dateIso) {
        if (!isConfigured()) {
            return "{\"ok\":false,\"message\":\"Microsoft Graph non configure (token invalide ou calendrier manquant).\"}";
        }
        try {
            LocalDate start = LocalDate.parse(dateIso);
            LocalDate end = start.plusDays(1);
            String endpoint = graphBasePath(false) + "/calendars/" + enc(cfg("MS_GRAPH_CALENDAR_ID")) + "/events";
            String payload = "{"
                    + "\"subject\":\"" + esc(subject) + "\","
                    + "\"isAllDay\":true,"
                    + "\"start\":{\"dateTime\":\"" + start + "T00:00:00\",\"timeZone\":\"UTC\"},"
                    + "\"end\":{\"dateTime\":\"" + end + "T00:00:00\",\"timeZone\":\"UTC\"},"
                    + "\"body\":{\"contentType\":\"Text\",\"content\":\"" + esc(bodyText) + "\"},"
                    + "\"transactionId\":\"" + esc(transactionId) + "\""
                    + "}";
            HttpResult r = postJson(endpoint, payload);
            if (r.code >= 200 && r.code < 300) return "{\"ok\":true}";
            if (isDuplicateEvent(r.code, r.body)) {
                return "{\"ok\":true,\"skipped\":true,\"message\":\"Evenement deja existant.\"}";
            }

            // Fallback: if /users/{id} fails, retry on /me.
            if (!cfg("MS_GRAPH_USER_ID").isEmpty() && isUserRouteIssue(r.code, r.body)) {
                String fallbackEndpoint = graphBasePath(true) + "/calendars/" + enc(cfg("MS_GRAPH_CALENDAR_ID")) + "/events";
                HttpResult f = postJson(fallbackEndpoint, payload);
                if (f.code >= 200 && f.code < 300) return "{\"ok\":true}";
                if (isDuplicateEvent(f.code, f.body)) {
                    return "{\"ok\":true,\"skipped\":true,\"message\":\"Evenement deja existant.\"}";
                }
                return "{\"ok\":false,\"message\":\"Graph HTTP " + f.code + ": " + esc(extractMessage(f.body)) + "\"}";
            }
            return "{\"ok\":false,\"message\":\"Graph HTTP " + r.code + ": " + esc(extractMessage(r.body)) + "\"}";
        } catch (Exception e) {
            return "{\"ok\":false,\"message\":\"Erreur Graph: " + esc(String.valueOf(e.getMessage())) + "\"}";
        }
    }

    public String updateAllDayEvent(String eventId, String subject, String bodyText, String dateIso) {
        if (!isConfigured()) {
            return "{\"ok\":false,\"message\":\"Microsoft Graph non configure (token invalide ou calendrier manquant).\"}";
        }
        try {
            String eid = eventId == null ? "" : eventId.trim();
            if (eid.isEmpty()) return "{\"ok\":false,\"message\":\"eventId manquant.\"}";
            LocalDate start = LocalDate.parse(dateIso);
            LocalDate end = start.plusDays(1);

            String endpoint = graphBasePath(false) + "/calendars/" + enc(cfg("MS_GRAPH_CALENDAR_ID")) + "/events/" + enc(eid);
            String payload = "{"
                    + "\"subject\":\"" + esc(subject) + "\","
                    + "\"isAllDay\":true,"
                    + "\"start\":{\"dateTime\":\"" + start + "T00:00:00\",\"timeZone\":\"UTC\"},"
                    + "\"end\":{\"dateTime\":\"" + end + "T00:00:00\",\"timeZone\":\"UTC\"},"
                    + "\"body\":{\"contentType\":\"Text\",\"content\":\"" + esc(bodyText) + "\"}"
                    + "}";
            HttpResult r = patchJson(endpoint, payload);
            if (r.code >= 200 && r.code < 300) return "{\"ok\":true}";

            if (!cfg("MS_GRAPH_USER_ID").isEmpty() && isUserRouteIssue(r.code, r.body)) {
                String fallbackEndpoint = graphBasePath(true) + "/calendars/" + enc(cfg("MS_GRAPH_CALENDAR_ID")) + "/events/" + enc(eid);
                HttpResult f = patchJson(fallbackEndpoint, payload);
                if (f.code >= 200 && f.code < 300) return "{\"ok\":true}";
                return "{\"ok\":false,\"message\":\"Graph HTTP " + f.code + ": " + esc(extractMessage(f.body)) + "\"}";
            }
            return "{\"ok\":false,\"message\":\"Graph HTTP " + r.code + ": " + esc(extractMessage(r.body)) + "\"}";
        } catch (Exception e) {
            return "{\"ok\":false,\"message\":\"Erreur Graph: " + esc(String.valueOf(e.getMessage())) + "\"}";
        }
    }

    public String listEvents(String startIsoDate, String endIsoDateExclusive) {
        if (!isConfigured()) {
            return "{\"ok\":false,\"value\":[],\"message\":\"Microsoft Graph non configure (MS_GRAPH_ACCESS_TOKEN / MS_GRAPH_CALENDAR_ID manquants).\"}";
        }
        try {
            String start = startIsoDate + "T00:00:00Z";
            String end = endIsoDateExclusive + "T00:00:00Z";
            String endpoint = graphBasePath(false) + "/calendars/" + enc(cfg("MS_GRAPH_CALENDAR_ID"))
                    + "/events?" + buildEventsQuery(start, end);
            HttpResult r = getJson(endpoint);
            if (r.code >= 200 && r.code < 300) {
                String body = (r.body == null || r.body.isEmpty()) ? "{\"value\":[]}" : r.body;
                return mergeOkTrue(body);
            }

            // Fallback: if /users/{id} fails, retry on /me.
            if (!cfg("MS_GRAPH_USER_ID").isEmpty() && isUserRouteIssue(r.code, r.body)) {
                String fallbackEndpoint = graphBasePath(true) + "/calendars/" + enc(cfg("MS_GRAPH_CALENDAR_ID"))
                        + "/events?" + buildEventsQuery(start, end);
                HttpResult f = getJson(fallbackEndpoint);
                if (f.code >= 200 && f.code < 300) {
                    String body = (f.body == null || f.body.isEmpty()) ? "{\"value\":[]}" : f.body;
                    return mergeOkTrue(body);
                }
                return "{\"ok\":false,\"value\":[],\"message\":\"Graph HTTP " + f.code + ": " + esc(extractMessage(f.body)) + "\"}";
            }
            return "{\"ok\":false,\"value\":[],\"message\":\"Graph HTTP " + r.code + ": " + esc(extractMessage(r.body)) + "\"}";
        } catch (Exception e) {
            return "{\"ok\":false,\"value\":[],\"message\":\"Erreur Graph: " + esc(String.valueOf(e.getMessage())) + "\"}";
        }
    }

    private String graphBasePath(boolean forceMe) {
        if (forceMe) return "https://graph.microsoft.com/v1.0/me";
        String userId = cfg("MS_GRAPH_USER_ID");
        if (userId.isEmpty()) return "https://graph.microsoft.com/v1.0/me";
        return "https://graph.microsoft.com/v1.0/users/" + enc(userId);
    }

    private static String cfg(String key) {
        String env = System.getenv(key);
        if (env != null && !env.trim().isEmpty()) return env.trim();
        String sys = System.getProperty(key);
        if (sys != null && !sys.trim().isEmpty()) return sys.trim();
        String prop = LOCAL_CONFIG.getProperty(key);
        if (prop != null && !prop.trim().isEmpty()) return prop.trim();
        return "";
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static String encFilter(String s) {
        return (s == null ? "" : s).replace("'", "''");
    }

    private static String q(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return "";
        }
    }

    private static String buildEventsQuery(String start, String end) {
        String filter = "start/dateTime ge '" + encFilter(start) + "' and start/dateTime lt '" + encFilter(end) + "'";
        return "$top=200"
                + "&$select=" + q("id,subject,start,end,bodyPreview,categories")
                + "&$orderby=" + q("start/dateTime asc")
                + "&$filter=" + q(filter);
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static String readBody(HttpURLConnection con) {
        if (con == null) return "";
        InputStream in = null;
        try {
            in = con.getResponseCode() >= 400 ? con.getErrorStream() : con.getInputStream();
            if (in == null) return "";
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }

    private static String extractMessage(String body) {
        if (body == null) return "";
        int i = body.indexOf("\"message\"");
        if (i < 0) return body;
        int q1 = body.indexOf('"', i + 9);
        int q2 = q1 < 0 ? -1 : body.indexOf('"', q1 + 1);
        return (q1 >= 0 && q2 > q1) ? body.substring(q1 + 1, q2) : body;
    }

    private static HttpResult postJson(String endpoint, String payload) throws Exception {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setRequestProperty("Authorization", "Bearer " + cfg("MS_GRAPH_ACCESS_TOKEN"));
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            return new HttpResult(con.getResponseCode(), readBody(con));
        } catch (SocketTimeoutException firstTimeout) {
            // one retry on timeout
            HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setRequestProperty("Authorization", "Bearer " + cfg("MS_GRAPH_ACCESS_TOKEN"));
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            return new HttpResult(con.getResponseCode(), readBody(con));
        }
    }

    private static HttpResult getJson(String endpoint) throws Exception {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setRequestProperty("Authorization", "Bearer " + cfg("MS_GRAPH_ACCESS_TOKEN"));
            con.setRequestProperty("Accept", "application/json");
            return new HttpResult(con.getResponseCode(), readBody(con));
        } catch (SocketTimeoutException firstTimeout) {
            // one retry on timeout
            HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setRequestProperty("Authorization", "Bearer " + cfg("MS_GRAPH_ACCESS_TOKEN"));
            con.setRequestProperty("Accept", "application/json");
            return new HttpResult(con.getResponseCode(), readBody(con));
        }
    }

    private static HttpResult patchJson(String endpoint, String payload) throws Exception {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
            con.setRequestMethod("PATCH");
            con.setDoOutput(true);
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setRequestProperty("Authorization", "Bearer " + cfg("MS_GRAPH_ACCESS_TOKEN"));
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            return new HttpResult(con.getResponseCode(), readBody(con));
        } catch (java.net.ProtocolException pe) {
            // Fallback for some JDKs: use method override
            HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setRequestProperty("Authorization", "Bearer " + cfg("MS_GRAPH_ACCESS_TOKEN"));
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            return new HttpResult(con.getResponseCode(), readBody(con));
        }
    }

    private static boolean isUserRouteIssue(int code, String body) {
        if (code == 404) return true;
        String b = body == null ? "" : body.toLowerCase();
        return b.contains("resource") && b.contains("not found");
    }

    private static boolean isDuplicateEvent(int code, String body) {
        if (code < 400) return false;
        String b = body == null ? "" : body.toLowerCase();
        return b.contains("transactionid")
                || b.contains("duplicate")
                || b.contains("already exists")
                || b.contains("is already in use");
    }

    private static String mergeOkTrue(String body) {
        String b = body == null ? "" : body.trim();
        if (b.isEmpty() || "{}".equals(b)) return "{\"ok\":true,\"value\":[]}";
        if (b.startsWith("{")) {
            if (b.contains("\"ok\"")) return b;
            return "{\"ok\":true," + b.substring(1);
        }
        return "{\"ok\":true,\"value\":[]}";
    }

    private static Properties loadLocalConfig() {
        Properties p = new Properties();
        try (InputStream in = MicrosoftGraphCalendarService.class.getClassLoader().getResourceAsStream("payment.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {
        }
        try {
            Path external = Paths.get("payment.properties");
            if (Files.exists(external)) {
                try (InputStream in = Files.newInputStream(external)) {
                    p.load(in);
                }
            }
        } catch (Exception ignored) {
        }
        return p;
    }

    private static final class HttpResult {
        final int code;
        final String body;

        HttpResult(int code, String body) {
            this.code = code;
            this.body = body == null ? "" : body;
        }
    }
}
