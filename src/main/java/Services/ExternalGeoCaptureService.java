package Services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public final class ExternalGeoCaptureService {
    private static final Map<String, GeoState> STATES = new ConcurrentHashMap<>();
    private static volatile HttpServer server;
    private static volatile int port = 0;

    public static final class SessionStart {
        public final boolean ok;
        public final String token;
        public final String url;
        public final String message;

        public SessionStart(boolean ok, String token, String url, String message) {
            this.ok = ok;
            this.token = token == null ? "" : token;
            this.url = url == null ? "" : url;
            this.message = message == null ? "" : message;
        }
    }

    public static final class Status {
        public final boolean ok;
        public final String token;
        public final String state; // pending|success|error|expired|unknown
        public final double lat;
        public final double lon;
        public final String message;
        public final long updatedAt;

        public Status(boolean ok, String token, String state, double lat, double lon, String message, long updatedAt) {
            this.ok = ok;
            this.token = token == null ? "" : token;
            this.state = state == null ? "unknown" : state;
            this.lat = lat;
            this.lon = lon;
            this.message = message == null ? "" : message;
            this.updatedAt = updatedAt;
        }
    }

    private static final class GeoState {
        volatile String state = "pending";
        volatile double lat = 0.0;
        volatile double lon = 0.0;
        volatile String message = "En attente de position.";
        volatile long createdAt = System.currentTimeMillis();
        volatile long updatedAt = System.currentTimeMillis();
    }

    private ExternalGeoCaptureService() {}

    public static synchronized SessionStart startSession() {
        try {
            ensureServer();
            String token = randomToken();
            GeoState st = new GeoState();
            STATES.put(token, st);
            String url = "http://127.0.0.1:" + port + "/geo/capture?token=" + token;
            return new SessionStart(true, token, url, "Session geolocalisation creee.");
        } catch (Exception e) {
            return new SessionStart(false, "", "", "Erreur geolocalisation externe: " + safe(e.getMessage()));
        }
    }

    public static Status getStatus(String token) {
        String t = safe(token);
        if (t.isEmpty()) return new Status(false, "", "unknown", 0, 0, "Token manquant.", 0);
        GeoState st = STATES.get(t);
        if (st == null) return new Status(false, t, "unknown", 0, 0, "Session introuvable.", 0);
        long now = System.currentTimeMillis();
        if ("pending".equals(st.state) && now - st.createdAt > 5 * 60_000L) {
            st.state = "expired";
            st.message = "Session expiree.";
            st.updatedAt = now;
        }
        return new Status("success".equals(st.state), t, st.state, st.lat, st.lon, st.message, st.updatedAt);
    }

    private static synchronized void ensureServer() throws IOException {
        if (server != null) return;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/geo/capture", new CapturePageHandler());
        server.createContext("/geo/report", new ReportHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private static final class CapturePageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String token = parseQueryParam(ex.getRequestURI().getRawQuery(), "token");
            GeoState st = STATES.get(token);
            if (st == null) {
                write(ex, 404, "text/plain; charset=utf-8", "Session invalide.");
                return;
            }
            String body = "<!doctype html><html><head><meta charset='utf-8'><title>Geo Capture</title>"
                    + "<style>body{font-family:Arial;padding:24px;background:#f4f7fb;color:#0b2a55}button{padding:10px 14px;border-radius:10px;border:none;background:#1f4d8c;color:#fff;font-weight:700;cursor:pointer}#s{margin-top:12px}</style>"
                    + "</head><body><h2>Partager votre position</h2><p>Cliquez sur le bouton puis acceptez la permission.</p>"
                    + "<button id='b'>Partager ma position</button><div id='s'>En attente...</div>"
                    + "<script>const token='" + jsSafe(token) + "';const s=document.getElementById('s');"
                    + "function send(lat,lon,msg){fetch('/geo/report?token='+encodeURIComponent(token),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({lat:lat,lon:lon,message:msg||''})}).then(()=>{s.textContent='Position transmise. Vous pouvez fermer cette page.';});}"
                    + "document.getElementById('b').onclick=function(){if(!navigator.geolocation){send(null,null,'Geolocalisation non supportee');return;}s.textContent='Localisation en cours...';navigator.geolocation.getCurrentPosition(function(p){send(p.coords.latitude,p.coords.longitude,'OK');},function(e){send(null,null,'Erreur GPS code '+(e&&e.code?e.code:'?'));},{enableHighAccuracy:true,timeout:30000,maximumAge:0});};"
                    + "setTimeout(function(){document.getElementById('b').click();},300);"
                    + "</script></body></html>";
            write(ex, 200, "text/html; charset=utf-8", body);
        }
    }

    private static final class ReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String token = parseQueryParam(ex.getRequestURI().getRawQuery(), "token");
            GeoState st = STATES.get(token);
            if (st == null) {
                write(ex, 404, "application/json; charset=utf-8", "{\"ok\":false,\"message\":\"session invalide\"}");
                return;
            }
            String raw = readBody(ex.getRequestBody());
            double lat = extractNumber(raw, "lat");
            double lon = extractNumber(raw, "lon");
            String msg = extractString(raw, "message");

            if (Double.isFinite(lat) && Double.isFinite(lon)) {
                st.lat = lat;
                st.lon = lon;
                st.state = "success";
                st.message = "Position recue.";
                st.updatedAt = System.currentTimeMillis();
                write(ex, 200, "application/json; charset=utf-8", "{\"ok\":true}");
                return;
            }
            st.state = "error";
            st.message = safe(msg.isEmpty() ? "Position non recue." : msg);
            st.updatedAt = System.currentTimeMillis();
            write(ex, 200, "application/json; charset=utf-8", "{\"ok\":false}");
        }
    }

    private static String parseQueryParam(String query, String key) {
        String q = safe(query);
        if (q.isEmpty()) return "";
        for (String part : q.split("&")) {
            int i = part.indexOf('=');
            if (i <= 0) continue;
            String k = decode(part.substring(0, i));
            if (!key.equals(k)) continue;
            return decode(part.substring(i + 1));
        }
        return "";
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s == null ? "" : s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static String readBody(InputStream in) throws IOException {
        byte[] bytes = in.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static double extractNumber(String json, String key) {
        String marker = "\"" + key + "\"";
        int i = json.indexOf(marker);
        if (i < 0) return Double.NaN;
        int c = json.indexOf(':', i + marker.length());
        if (c < 0) return Double.NaN;
        int j = c + 1;
        while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
        int k = j;
        while (k < json.length()) {
            char ch = json.charAt(k);
            if (!(Character.isDigit(ch) || ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E')) break;
            k++;
        }
        try { return Double.parseDouble(json.substring(j, k)); } catch (Exception e) { return Double.NaN; }
    }

    private static String extractString(String json, String key) {
        String marker = "\"" + key + "\"";
        int i = json.indexOf(marker);
        if (i < 0) return "";
        int c = json.indexOf(':', i + marker.length());
        if (c < 0) return "";
        int s = json.indexOf('"', c + 1);
        if (s < 0) return "";
        int e = json.indexOf('"', s + 1);
        if (e < 0) return "";
        return json.substring(s + 1, e);
    }

    private static void write(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] bytes = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String randomToken() {
        byte[] b = new byte[18];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String jsSafe(String s) {
        return safe(s).replace("\\", "\\\\").replace("'", "\\'");
    }
}
