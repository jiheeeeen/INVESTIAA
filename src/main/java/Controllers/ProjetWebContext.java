package Controllers;

import Services.DemandeAnnulationCRUD;
import Services.ProfilEntrepreneurCRUD;
import Services.ProjetCRUD;
import Services.UserCRUD;
import javafx.scene.web.WebView;

import java.sql.Connection;

public class ProjetWebContext {
    private final WebView webView;
    private final ProjetCRUD projetCrud = new ProjetCRUD();
    private final DemandeAnnulationCRUD annulationCrud = new DemandeAnnulationCRUD();
    private final ProfilEntrepreneurCRUD profilCrud = new ProfilEntrepreneurCRUD();
    private final UserCRUD userCrud = new UserCRUD();

    public ProjetWebContext(WebView webView, Connection conn) {
        this.webView = webView;
    }

    public ProjetWebContext(WebView webView) {
        this(webView, null);
    }

    public WebView getWebView() {
        return webView;
    }

    public ProjetCRUD getProjetCrud() {
        return projetCrud;
    }

    public DemandeAnnulationCRUD getAnnulationCrud() {
        return annulationCrud;
    }

    public ProfilEntrepreneurCRUD getProfilCrud() {
        return profilCrud;
    }

    public UserCRUD getUserCrud() {
        return userCrud;
    }
}
