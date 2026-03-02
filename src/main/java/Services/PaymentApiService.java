package Services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaymentApiService {

    private static final ConcurrentHashMap<String, String> TX_STATUS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> CONSUMED_CHECKOUT_SESSIONS = new ConcurrentHashMap<>();
    private static final Properties LOCAL_CONFIG = loadLocalConfig();

    public static final class PaymentResult {
        public final boolean approved;
        public final String transactionId;
        public final String status;
        public final String message;

        public PaymentResult(boolean approved, String transactionId, String status, String message) {
            this.approved = approved;
            this.transactionId = transactionId == null ? "" : transactionId;
            this.status = status == null ? "" : status;
            this.message = message == null ? "" : message;
        }
    }

    public static final class StripeSessionStatus {
        public final boolean ok;
        public final String sessionId;
        public final String paymentStatus;
        public final int remboursementId;
        public final double amount;
        public final String message;

        public StripeSessionStatus(boolean ok, String sessionId, String paymentStatus, int remboursementId, double amount, String message) {
            this.ok = ok;
            this.sessionId = safe(sessionId);
            this.paymentStatus = safe(paymentStatus);
            this.remboursementId = remboursementId;
            this.amount = amount;
            this.message = safe(message);
        }
    }

    public PaymentResult chargeRemboursement(int remboursementId, double amount, String paymentMethod, String reference, String cardToken) {
        if (amount <= 0 || Double.isNaN(amount)) {
            return new PaymentResult(false, "", "failed", "Montant de paiement invalide.");
        }

        String provider = config("PAYMENT_PROVIDER").toLowerCase();
        if ("stripe".equals(provider)) {
            PaymentResult stripe = chargeWithStripe(remboursementId, amount, paymentMethod, reference, cardToken);
            if (!stripe.transactionId.isEmpty()) {
                TX_STATUS.put(stripe.transactionId, stripe.status.isEmpty() ? (stripe.approved ? "succeeded" : "failed") : stripe.status);
            }
            return stripe;
        }

        return chargeWithGenericSandbox(remboursementId, amount, paymentMethod, reference, cardToken);
    }

    public String createStripeCheckoutSession(int remboursementId, double amount, String reference) {
        String secret = config("STRIPE_SECRET_KEY");
        if (secret.isEmpty()) {
            return "{\"ok\":false,\"message\":\"STRIPE_SECRET_KEY manquante.\"}";
        }
        if (isMaskedOrInvalidSecret(secret)) {
            return "{\"ok\":false,\"message\":\"STRIPE_SECRET_KEY invalide (masquee/incomplete). Utiliser la vraie cle sk_test_... depuis Stripe Dashboard.\"}";
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            return "{\"ok\":false,\"message\":\"Montant invalide.\"}";
        }
        try {
            long amountMinor = Math.max(1L, Math.round(amount * 100.0));
            String currency = stripeCurrency();
            String successUrl = config("STRIPE_SUCCESS_URL");
            String cancelUrl = config("STRIPE_CANCEL_URL");
            if (successUrl.isEmpty()) successUrl = "https://example.com/success?session_id={CHECKOUT_SESSION_ID}";
            if (cancelUrl.isEmpty()) cancelUrl = "https://example.com/cancel";

            String form = form("mode", "payment")
                    + "&" + form("payment_method_types[0]", "card")
                    + "&" + form("line_items[0][price_data][currency]", currency)
                    + "&" + form("line_items[0][price_data][product_data][name]", "Remboursement #" + remboursementId)
                    + "&" + form("line_items[0][price_data][unit_amount]", String.valueOf(amountMinor))
                    + "&" + form("line_items[0][quantity]", "1")
                    + "&" + form("client_reference_id", String.valueOf(remboursementId))
                    + "&" + form("metadata[remboursement_id]", String.valueOf(remboursementId))
                    + "&" + form("metadata[reference]", safe(reference))
                    + "&" + form("success_url", successUrl)
                    + "&" + form("cancel_url", cancelUrl);

            HttpURLConnection con = (HttpURLConnection) new URL("https://api.stripe.com/v1/checkout/sessions").openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            con.setRequestProperty("Authorization", "Bearer " + secret);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("User-Agent", "Investia-StripeCheckout/1.0");
            try (OutputStream os = con.getOutputStream()) {
                os.write(form.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();
            String body = readBody(con);
            String sid = extractJsonString(body, "id");
            String url = extractJsonString(body, "url");
            if (code >= 200 && code < 300 && !sid.isEmpty() && !url.isEmpty()) {
                TX_STATUS.put(sid, "open");
                return "{"
                        + "\"ok\":true"
                        + ",\"sessionId\":" + json(sid)
                        + ",\"checkoutUrl\":" + json(url)
                        + "}";
            }
            String err = extractNestedErrorMessage(body);
            if (err.isEmpty()) err = "Creation session Stripe echouee (HTTP " + code + ").";
            return "{\"ok\":false,\"message\":" + json(err) + "}";
        } catch (Exception e) {
            return "{\"ok\":false,\"message\":" + json("Erreur Stripe Checkout: " + safe(e.getMessage())) + "}";
        }
    }

    public String fetchStripeCheckoutSessionStatusJson(String sessionId) {
        StripeSessionStatus s = fetchStripeCheckoutSessionStatus(sessionId);
        if (!s.ok) {
            return "{\"ok\":false,\"status\":\"unknown\",\"message\":" + json(s.message) + "}";
        }
        return "{"
                + "\"ok\":true"
                + ",\"sessionId\":" + json(s.sessionId)
                + ",\"status\":" + json(s.paymentStatus)
                + ",\"remboursementId\":" + s.remboursementId
                + ",\"amount\":" + s.amount
                + "}";
    }

    public StripeSessionStatus fetchStripeCheckoutSessionStatus(String sessionId) {
        String sid = safe(sessionId);
        if (sid.isEmpty()) return new StripeSessionStatus(false, "", "unknown", 0, 0.0, "sessionId manquant.");
        String secret = config("STRIPE_SECRET_KEY");
        if (secret.isEmpty()) return new StripeSessionStatus(false, sid, "unknown", 0, 0.0, "STRIPE_SECRET_KEY manquante.");
        if (isMaskedOrInvalidSecret(secret)) {
            return new StripeSessionStatus(false, sid, "unknown", 0, 0.0, "STRIPE_SECRET_KEY invalide (masquee/incomplete).");
        }
        try {
            String endpoint = "https://api.stripe.com/v1/checkout/sessions/" + sid + "?expand[]=payment_intent";
            HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            con.setRequestProperty("Authorization", "Bearer " + secret);
            con.setRequestProperty("User-Agent", "Investia-StripeCheckout/1.0");
            String body = readBody(con);
            int code = con.getResponseCode();
            if (code >= 200 && code < 300) {
                String paymentStatus = extractJsonString(body, "payment_status");
                if (paymentStatus.isEmpty()) paymentStatus = "unknown";
                int remboursementId = toIntSafe(extractJsonString(body, "remboursement_id"));
                if (remboursementId <= 0) {
                    remboursementId = toIntSafe(extractJsonString(body, "client_reference_id"));
                }
                long amountMinor = toLongSafe(extractJsonNumber(body, "amount_total"));
                double amount = amountMinor > 0 ? (amountMinor / 100.0) : 0.0;
                TX_STATUS.put(sid, paymentStatus);
                return new StripeSessionStatus(true, sid, paymentStatus, remboursementId, amount, "");
            }
            String err = extractNestedErrorMessage(body);
            if (err.isEmpty()) err = "Lecture statut Stripe echouee (HTTP " + code + ").";
            return new StripeSessionStatus(false, sid, "unknown", 0, 0.0, err);
        } catch (Exception e) {
            return new StripeSessionStatus(false, sid, "unknown", 0, 0.0, "Erreur statut Stripe: " + safe(e.getMessage()));
        }
    }

    public boolean consumeStripeCheckoutSession(String sessionId) {
        String sid = safe(sessionId);
        if (sid.isEmpty()) return false;
        return CONSUMED_CHECKOUT_SESSIONS.putIfAbsent(sid, Boolean.TRUE) == null;
    }

    public String fetchStatus(String transactionId) {
        String tx = safe(transactionId);
        if (tx.isEmpty()) return "unknown";
        String cached = TX_STATUS.get(tx);
        if (cached != null && !cached.isEmpty()) return cached;

        String provider = config("PAYMENT_PROVIDER").toLowerCase();
        if ("stripe".equals(provider) && tx.startsWith("pi_")) {
            String secret = config("STRIPE_SECRET_KEY");
            if (secret.isEmpty()) return "unknown";
            try {
                HttpURLConnection con = (HttpURLConnection) new URL("https://api.stripe.com/v1/payment_intents/" + tx).openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(8000);
                con.setReadTimeout(12000);
                con.setRequestProperty("Authorization", "Bearer " + secret);
                String body = readBody(con);
                if (con.getResponseCode() >= 200 && con.getResponseCode() < 300) {
                    String status = extractJsonString(body, "status");
                    if (!status.isEmpty()) {
                        TX_STATUS.put(tx, status);
                        return status;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private PaymentResult chargeWithStripe(int remboursementId, double amount, String paymentMethod, String reference, String cardToken) {
        String secret = config("STRIPE_SECRET_KEY");
        if (secret.isEmpty()) {
            return new PaymentResult(false, "", "failed", "STRIPE_SECRET_KEY manquante.");
        }
        if (isMaskedOrInvalidSecret(secret)) {
            return new PaymentResult(false, "", "failed", "STRIPE_SECRET_KEY invalide (masquee/incomplete).");
        }
        try {
            long amountMinor = Math.max(1L, Math.round(amount * 100.0));
            String currency = stripeCurrency();
            String pm = resolveStripePaymentMethod(paymentMethod, cardToken);

            String form = form("amount", String.valueOf(amountMinor))
                    + "&" + form("currency", currency)
                    + "&" + form("confirm", "true")
                    + "&" + form("payment_method", pm)
                    + "&" + form("description", "Remboursement #" + remboursementId)
                    + "&" + form("metadata[remboursement_id]", String.valueOf(remboursementId))
                    + "&" + form("metadata[reference]", safe(reference));

            HttpURLConnection con = (HttpURLConnection) new URL("https://api.stripe.com/v1/payment_intents").openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            con.setRequestProperty("Authorization", "Bearer " + secret);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("User-Agent", "Investia-StripeTest/1.0");

            try (OutputStream os = con.getOutputStream()) {
                os.write(form.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();
            String body = readBody(con);
            String pi = extractJsonString(body, "id");
            String status = extractJsonString(body, "status");
            if (pi.isEmpty()) {
                pi = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            boolean approved = "succeeded".equalsIgnoreCase(status) || "processing".equalsIgnoreCase(status);
            if (code >= 200 && code < 300 && approved) {
                return new PaymentResult(true, pi, status, "Paiement Stripe test approuve.");
            }
            String err = extractNestedErrorMessage(body);
            if (err.isEmpty()) err = "Paiement Stripe refuse (HTTP " + code + ").";
            return new PaymentResult(false, pi, status.isEmpty() ? "failed" : status, err);
        } catch (Exception e) {
            return new PaymentResult(false, "", "failed", "Erreur Stripe: " + safe(e.getMessage()));
        }
    }

    private PaymentResult chargeWithGenericSandbox(int remboursementId, double amount, String paymentMethod, String reference, String cardToken) {
        String method = safe(paymentMethod).toUpperCase();
        String token = safe(cardToken).toUpperCase();
        String endpoint = config("PAYMENT_API_URL");
        String apiKey = config("PAYMENT_API_KEY");
        boolean strict = "true".equalsIgnoreCase(config("PAYMENT_API_STRICT"));
        if (endpoint.isEmpty()) {
            if ("CARTE_BANCAIRE".equals(method) && token.contains("DECLINE")) {
                return new PaymentResult(false, "", "failed", "Paiement carte refuse (carte test de refus).");
            }
            String tx = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            return new PaymentResult(true, tx, "succeeded", "Paiement valide (mode sandbox local).");
        }

        HttpURLConnection con = null;
        try {
            URL url = new URL(endpoint);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "Investia-PaymentClient/1.0");
            if (!apiKey.isEmpty()) con.setRequestProperty("Authorization", "Bearer " + apiKey);

            String payload = "{"
                    + "\"remboursementId\":" + remboursementId
                    + ",\"amount\":" + amount
                    + ",\"currency\":\"TND\""
                    + ",\"paymentMethod\":\"" + escape(paymentMethod) + "\""
                    + ",\"reference\":\"" + escape(reference) + "\""
                    + ",\"cardToken\":\"" + escape(cardToken) + "\""
                    + "}";
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = con.getOutputStream()) {
                os.write(body);
            }

            int status = con.getResponseCode();
            if (status >= 200 && status < 300) {
                String tx = con.getHeaderField("X-Transaction-Id");
                if (tx == null || tx.trim().isEmpty()) {
                    tx = "API-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                }
                return new PaymentResult(true, tx, "succeeded", "Paiement approuve par l'API.");
            }
            if (!strict) {
                String tx = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                return new PaymentResult(true, tx, "succeeded", "Fallback sandbox: API indisponible/refusee (HTTP " + status + ").");
            }
            return new PaymentResult(false, "", "failed", "API paiement refusee (HTTP " + status + ").");
        } catch (Exception e) {
            if (!strict) {
                String tx = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                return new PaymentResult(true, tx, "succeeded", "Fallback sandbox: " + safe(e.getMessage()));
            }
            return new PaymentResult(false, "", "failed", "Erreur API paiement: " + safe(e.getMessage()));
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String resolveStripePaymentMethod(String paymentMethod, String cardToken) {
        String method = safe(paymentMethod).toUpperCase();
        String token = safe(cardToken).toUpperCase();
        if ("CARTE_BANCAIRE".equals(method)) {
            if (token.contains("DECLINE")) return "pm_card_chargeDeclined";
            return "pm_card_visa";
        }
        if ("WALLET".equals(method)) return "pm_card_visa";
        if ("VIREMENT".equals(method)) return "pm_card_visa";
        return "pm_card_visa";
    }

    private static String form(String k, String v) throws Exception {
        return URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
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

    private static String extractNestedErrorMessage(String json) {
        String msg = extractJsonString(json, "message");
        if (!msg.isEmpty()) return msg;
        return "";
    }

    private static String extractJsonNumber(String json, String key) {
        if (json == null || json.isEmpty() || key == null || key.isEmpty()) return "";
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([-]?[0-9]+(?:\\.[0-9]+)?)");
        Matcher m = p.matcher(json);
        if (m.find()) return safe(m.group(1));
        return "";
    }

    private static String json(String s) {
        return "\"" + escape(s) + "\"";
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String config(String key) {
        String env = sanitizeKey(System.getenv(key));
        if (!env.isEmpty()) return env;
        String sys = sanitizeKey(System.getProperty(key));
        if (!sys.isEmpty()) return sys;

        String prop = sanitizeKey(LOCAL_CONFIG.getProperty(key));
        if (!prop.isEmpty()) return prop;

        // aliases to reduce configuration mistakes
        if ("STRIPE_SECRET_KEY".equals(key)) {
            String alt1 = sanitizeKey(System.getenv("STRIPE_API_KEY"));
            if (!alt1.isEmpty()) return alt1;
            String alt2 = sanitizeKey(System.getenv("STRIPE_SECRET"));
            if (!alt2.isEmpty()) return alt2;
            String alt3 = sanitizeKey(System.getProperty("STRIPE_API_KEY"));
            if (!alt3.isEmpty()) return alt3;
            String alt4 = sanitizeKey(System.getProperty("STRIPE_SECRET"));
            if (!alt4.isEmpty()) return alt4;
            String alt5 = sanitizeKey(LOCAL_CONFIG.getProperty("STRIPE_API_KEY"));
            if (!alt5.isEmpty()) return alt5;
            String alt6 = sanitizeKey(LOCAL_CONFIG.getProperty("STRIPE_SECRET"));
            if (!alt6.isEmpty()) return alt6;
        }
        return "";
    }

    private static boolean isMaskedOrInvalidSecret(String key) {
        String k = sanitizeKey(key);
        if (k.isEmpty()) return true;
        if (k.contains("*")) return true;
        if (!k.startsWith("sk_")) return true;
        return k.length() < 20;
    }

    private static String stripeCurrency() {
        String c = safe(config("STRIPE_CURRENCY")).toLowerCase();
        if (!c.matches("^[a-z]{3}$")) return "eur";
        return c;
    }

    private static String sanitizeKey(String s) {
        String k = safe(s);
        if ((k.startsWith("\"") && k.endsWith("\"")) || (k.startsWith("'") && k.endsWith("'"))) {
            k = safe(k.substring(1, Math.max(1, k.length() - 1)));
        }
        return k;
    }

    private static Properties loadLocalConfig() {
        Properties p = new Properties();
        try (InputStream in = PaymentApiService.class.getClassLoader().getResourceAsStream("payment.properties")) {
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

    private static int toIntSafe(String s) {
        try {
            return Integer.parseInt(safe(s));
        } catch (Exception e) {
            return 0;
        }
    }

    private static long toLongSafe(String s) {
        try {
            return Long.parseLong(safe(s));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
