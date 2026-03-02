package Utils;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

public final class WebViewBridgeUtil {

    private WebViewBridgeUtil() {}

    /**
     * ✅ Injecte un bridge de NAVIGATION (navbar)
     * ⚠️ IMPORTANT : NE DOIT PAS écraser le bridge métier de la page.
     * - On met navBridge dans window.navBridge
     * - On garde window.javaBridge existant s'il existe déjà
     * - On déclenche __onJavaBridgeReady une seule fois
     */
    public static void injectNavBridge(WebEngine engine, Object navBridge) {
        if (engine == null || navBridge == null) return;

        try {
            JSObject window = (JSObject) engine.executeScript("window");

            // ✅ Nom clair pour la navbar
            window.setMember("navBridge", navBridge);

            // ✅ Compat: certains anciens HTML appellent javaBridge... on ne casse pas
            // MAIS on n'écrase pas si javaBridge existe déjà (bridge métier)
            safeExec(engine,
                    "try{ " +
                            "  if(!window.javaBridge){ window.javaBridge = window.navBridge; }" +
                            "  if(!window.java){ window.java = window.navBridge; }" +
                            "}catch(e){}"
            );

            // ✅ drapeau prêt
            safeExec(engine, "window.__bridgeReady = true;");

            // ✅ callback si navbar.js attend
            safeExec(engine,
                    "try{" +
                            "  if(!window.__bridgeReadyCalled){" +
                            "    window.__bridgeReadyCalled = true;" +
                            "    if(window.__onJavaBridgeReady) window.__onJavaBridgeReady();" +
                            "  }" +
                            "}catch(e){}"
            );

        } catch (Exception ignored) {}
    }

    /**
     * ✅ Injecte le bridge métier de la page (data / actions)
     */
    public static void injectInvestBridge(WebEngine engine, Object investBridge) {
        if (engine == null || investBridge == null) return;

        try {
            JSObject window = (JSObject) engine.executeScript("window");

            // ✅ alias clair pour tes HTML (tu utilises souvent investBridge)
            window.setMember("investBridge", investBridge);

        } catch (Exception ignored) {}
    }

    /**
     * ✅ Injecte tout :
     * - navBridge => window.navBridge + fallback javaBridge si absent
     * - investBridge => window.investBridge
     *
     * ⚠️ IMPORTANT : On NE fait plus setMember("javaBridge", navBridge) car ça écrase la page !
     */
    public static void injectAll(WebEngine engine, Object navBridge, Object investBridge) {
        injectNavBridge(engine, navBridge);
        injectInvestBridge(engine, investBridge);

        safeExec(engine,
                "console.log('[JAVA] Bridges injected => nav='+(!!window.navBridge)+' javaBridge='+(!!window.javaBridge)+' invest='+(!!window.investBridge));"
        );
    }

    public static void enableAlerts(WebEngine engine) {
        if (engine == null) return;

        engine.setOnAlert(event -> {
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Message");
            alert.setHeaderText(null);
            alert.setContentText(event.getData());
            alert.showAndWait();
        });

        engine.setConfirmHandler(message -> {
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText(message);

            java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
        });
    }

    public static void safeExec(WebEngine engine, String js) {
        try { engine.executeScript(js); } catch (Exception ignored) {}
    }
}