package Controllers;

import Utils.MyBD;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;

public class EntrepreneurWebViewController {

    @FXML private WebView webView;

    private ProjetWebContext context;

    @FXML
    private void initialize() {
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        // ✅ Construire le context (adapte si ton ProjetWebContext a déjà un getInstance)
        context = new ProjetWebContext(webView, MyBD.getInstance().getConn());

        ProfilBridgeController profilBridge = new ProfilBridgeController(context);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                // ✅ EXACTEMENT comme ton getBridge() cherche
                window.setMember("javaBridge", profilBridge);
                window.setMember("javaBridgeUpload", profilBridge);
                window.setMember("javaBridgeInvest", profilBridge); // au cas où (ne gêne pas)
                window.setMember("app", profilBridge);              // au cas où
                window.setMember("Bridge", profilBridge);

                engine.executeScript("window.__bridgeReady = true;");
                engine.executeScript("console.log('✅ ProfilBridge injected', window.javaBridge, window.Bridge);");
            }
        });

        // ✅ charger la page entrepreneur (racine resources)
        URL page = getClass().getResource("/completerInfos.html");
        if (page == null) {
            // fallback si jamais elle est dans /html/
            page = getClass().getResource("/html/completerInfos.html");
        }

        if (page != null) {
            engine.load(page.toExternalForm());
        } else {
            engine.loadContent("<h3>Erreur: completerInfos.html introuvable (resources)</h3>");
        }
    }
}