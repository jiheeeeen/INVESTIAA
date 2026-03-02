package Services.auth;



import Entities.AuthUser;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class GoogleAuthService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of("openid", "email", "profile");

    public AuthUser login() throws Exception {
        // 1) Charger le JSON client OAuth depuis resources
        InputStream in = GoogleAuthService.class.getResourceAsStream("/oauth/google_client.json");
        if (in == null) {
            throw new IllegalStateException("Fichier OAuth introuvable: src/main/resources/oauth/google_client.json");
        }

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in, StandardCharsets.UTF_8));

        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // 2) Flow OAuth
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES
        ).setAccessType("offline") // autorise refresh token (optionnel)
                .build();

        // 3) Serveur local pour récupérer le code (loopback)
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(0) // 0 => choisit un port libre automatiquement
                .build();

        // 4) Autoriser (ouvre navigateur automatiquement)
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        // 5) Appeler l'endpoint userinfo (OpenID Connect)
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
        GenericUrl url = new GenericUrl("https://openidconnect.googleapis.com/v1/userinfo");
        HttpRequest request = requestFactory.buildGetRequest(url);
        HttpResponse response = request.execute();

        String json = response.parseAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = JSON_FACTORY.fromString(json, Map.class);

        String email = asString(data.get("email"));
        String name  = asString(data.get("name"));
        String sub   = asString(data.get("sub"));

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Email Google non retourné (vérifie les scopes email/profile).");
        }

        return new AuthUser(email, name != null ? name : "", sub != null ? sub : "");
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}