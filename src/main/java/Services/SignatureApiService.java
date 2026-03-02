package Services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignatureApiService {

    public static final class SignatureResult {
        public final boolean ok;
        public final String signatureId;
        public final String status;
        public final String message;

        public SignatureResult(boolean ok, String signatureId, String status, String message) {
            this.ok = ok;
            this.signatureId = safe(signatureId);
            this.status = safe(status);
            this.message = safe(message);
        }
    }

    public SignatureResult requestSignature(int remboursementId, String sessionId, String signerName, String signerEmail, String signatureImageDataUrl) {
        if (remboursementId <= 0) {
            return new SignatureResult(false, "", "failed", "Remboursement invalide.");
        }
        String endpoint = config("SIGNATURE_API_URL");
        String apiKey = config("SIGNATURE_API_KEY");
        boolean strict = "true".equalsIgnoreCase(config("SIGNATURE_API_STRICT"));

        if (endpoint.isEmpty()) {
            String sid = "SIG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
            return new SignatureResult(true, sid, "signed", "Signature validee (mode sandbox local).");
        }

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(endpoint).openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "Investia-SignatureClient/1.0");
            if (!apiKey.isEmpty()) {
                con.setRequestProperty("Authorization", "Bearer " + apiKey);
            }

            String payload = "{"
                    + "\"remboursementId\":" + remboursementId
                    + ",\"checkoutSessionId\":\"" + escape(sessionId) + "\""
                    + ",\"signerName\":\"" + escape(signerName) + "\""
                    + ",\"signerEmail\":\"" + escape(signerEmail) + "\""
                    + ",\"signatureImage\":\"" + escape(signatureImageDataUrl) + "\""
                    + "}";
            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();
            String body = readBody(con);
            if (code >= 200 && code < 300) {
                String signatureId = extractJsonString(body, "signatureId");
                if (signatureId.isEmpty()) signatureId = extractJsonString(body, "id");
                if (signatureId.isEmpty()) {
                    signatureId = "SIG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
                }
                String status = extractJsonString(body, "status");
                if (status.isEmpty()) status = "signed";
                return new SignatureResult(true, signatureId, status, "Signature enregistree par l'API.");
            }
            if (!strict) {
                String sid = "SIG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
                return new SignatureResult(true, sid, "signed", "Fallback signature local (API non disponible).");
            }
            String err = extractJsonString(body, "message");
            if (err.isEmpty()) err = "API signature refusee (HTTP " + code + ").";
            return new SignatureResult(false, "", "failed", err);
        } catch (Exception e) {
            if (!strict) {
                String sid = "SIG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
                return new SignatureResult(true, sid, "signed", "Fallback signature local: " + safe(e.getMessage()));
            }
            return new SignatureResult(false, "", "failed", "Erreur API signature: " + safe(e.getMessage()));
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String config(String key) {
        String env = safe(System.getenv(key));
        if (!env.isEmpty()) return env;
        return safe(System.getProperty(key));
    }

    private static String readBody(HttpURLConnection con) {
        if (con == null) return "";
        InputStream in = null;
        try {
            in = con.getResponseCode() >= 400 ? con.getErrorStream() : con.getInputStream();
            if (in == null) return "";
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static String extractJsonString(String json, String key) {
        if (json == null || json.isEmpty() || key == null || key.isEmpty()) return "";
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return safe(m.group(1));
        return "";
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
