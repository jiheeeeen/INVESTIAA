package Controllers;

import Services.UserCRUD;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class LoginController {

    @FXML private WebView webView;

    private final UserCRUD userCRUD = new UserCRUD();

    public void initialize() {
        WebEngine engine = webView.getEngine();

        // IMPORTANT: ton fichier est dans resources/html/login.html
        engine.load(getClass().getResource("/html/login.html").toExternalForm());

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("app", new WebAuthController()); // bridge
            }
        });
    }
}
