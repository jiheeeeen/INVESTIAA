package Services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class IpInfoLocationService {
    public static final class Result {
        public final boolean ok;
        public final double lat;
        public final double lon;
        public final String city;
        public final String region;
        public final String country;
        public final String ip;
        public final String message;

        public Result(boolean ok, double lat, double lon, String city, String region, String country, String ip, String message) {
            this.ok = ok;
            this.lat = lat;
            this.lon = lon;
            this.city = city == null ? "" : city;
            this.region = region == null ? "" : region;
            this.country = country == null ? "" : country;
            this.ip = ip == null ? "" : ip;
            this.message = message == null ? "" : message;
        }
    }

    public Result locate(String token) {
        String tk = safe(token);
        if (tk.isEmpty()) {
            return new Result(false, 0, 0, "", "", "", "", "IPINFO_TOKEN manquant.");
        }
        try {
            String url = "https://ipinfo.io/json?token=" + URLEncoder.encode(tk, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                return new Result(false, 0, 0, "", "", "", "", "IPInfo HTTP " + res.statusCode());
            }
            String body = safe(res.body());
            if (body.isEmpty()) {
                return new Result(false, 0, 0, "", "", "", "", "Reponse IPInfo vide.");
            }

            String loc = extractJsonString(body, "loc");
            double lat = Double.NaN;
            double lon = Double.NaN;
            if (!loc.isEmpty() && loc.contains(",")) {
                String[] parts = loc.split(",", 2);
                try { lat = Double.parseDouble(parts[0].trim()); } catch (Exception ignored) {}
                try { lon = Double.parseDouble(parts[1].trim()); } catch (Exception ignored) {}
            }

            String city = extractJsonString(body, "city");
            String region = extractJsonString(body, "region");
            String country = extractJsonString(body, "country");
            String ip = extractJsonString(body, "ip");

            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                return new Result(false, 0, 0, city, region, country, ip, "Coordonnees introuvables dans IPInfo.");
            }
            return new Result(true, lat, lon, city, region, country, ip, "OK");
        } catch (Exception e) {
            return new Result(false, 0, 0, "", "", "", "", safe(e.getMessage()));
        }
    }

    private static String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\"";
        int i = json.indexOf(marker);
        if (i < 0) return "";
        int c = json.indexOf(':', i + marker.length());
        if (c < 0) return "";
        int s = json.indexOf('"', c + 1);
        if (s < 0) return "";
        int e = s + 1;
        while (e < json.length()) {
            char ch = json.charAt(e);
            if (ch == '"' && json.charAt(e - 1) != '\\') break;
            e++;
        }
        if (e >= json.length()) return "";
        return json.substring(s + 1, e);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
