package Controllers;

import Entities.Evenement;
import Entities.Invitation;
import Entities.User;
import Services.Emailservice;
import Services.EvenementService;
import Services.InvitationService;
import Services.Qrservice;
import Utils.Session;
import Utils.sceneManager;
import org.example.ProjetWebViewController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.stage.Window;
import netscape.javascript.JSObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bridge Java ↔ JS pour invitation.html
 * Exposé sous window.invBridge
 */
public class InvitationBridge {

    private final InvitationService invService = new InvitationService();
    private final EvenementService evService = new EvenementService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WebEngine engine;
    private JSObject jsWindow;

    public InvitationBridge(WebEngine engine) {
        this.engine = engine;
    }

    public void inject() {
        jsWindow = (JSObject) engine.executeScript("window");
        jsWindow.setMember("invBridge", this);
        // Compatibilité navbar global
        jsWindow.setMember("javaBridge", this);
        jsWindow.setMember("javaBridgeInvest", this);
        System.out.println("✅ InvitationBridge injecté");
    }

    public String getCurrentUserName() {
        User u = Session.getCurrentUser();
        if (u == null) return "";
        String nom = u.getNom() == null ? "" : u.getNom().trim();
        String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
        String full = (nom + " " + prenom).trim();
        if (!full.isEmpty()) return full;
        return u.getEmail() == null ? "" : u.getEmail().trim();
    }

    public String getCurrentUserRole() {
        User u = Session.getCurrentUser();
        return (u == null || u.getRole() == null) ? "" : u.getRole().name();
    }

    public String openProjetsEntrepreneur() {
        try {
            ProjetWebViewController.openEntrepreneurPageOnNextLoad("projet_view.html");
            Platform.runLater(() -> sceneManager.switchTo("/projet_view.fxml", "Investia - Projets"));
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    public String openContactEntrepreneur() {
        try {
            ProjetWebViewController.openEntrepreneurPageOnNextLoad("contact.html");
            Platform.runLater(() -> sceneManager.switchTo("/projet_view.fxml", "Investia - Contact"));
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    public String logout() {
        try {
            Session.setCurrentUser(null);
            WebAuthController.openLoginOnNextLoad();
            Platform.runLater(() -> sceneManager.switchTo("/web_auth.fxml", "Investia - Connexion"));
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  NAVIGATION â†’ page Ã‰vÃ©nements
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public void goEvenements() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ajout_evenement.fxml"));
                Parent root  = loader.load();
                Stage  stage = (Stage) Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
                if (stage != null) { stage.setTitle("Investia â€” Ã‰vÃ©nements"); stage.getScene().setRoot(root); }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  NAVIGATION â†’ page Chatbot
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public void goChatbot() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chatbot.fxml"));
                Parent root  = loader.load();
                Stage  stage = (Stage) Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
                if (stage != null) { stage.setTitle("Investia â€” Chatbot"); stage.getScene().setRoot(root); }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  âœ… OUVRIR URL DANS LE NAVIGATEUR SYSTÃˆME
    //  AppelÃ© depuis JS : window.invBridge.openUrl(url)
    //  Ã‰vite d'ouvrir une nouvelle fenÃªtre JavaFX
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public void openUrl(String url) {
        Platform.runLater(() -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  GET ALL EVENEMENTS + INVITATIONS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public String getAllData() {
        return wrapJson(() -> {
            List<Evenement>  evs  = evService.getAll();
            List<Invitation> invs = invService.getAll();

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < evs.size(); i++) {
                Evenement ev = evs.get(i);
                sb.append("{")
                        .append("\"evenement\":{")
                        .append("\"id\":").append(ev.getId()).append(",")
                        .append("\"titre\":\"").append(esc(ev.getTitre())).append("\",")
                        .append("\"mode\":\"").append(esc(ev.getMode() == null ? "" : ev.getMode().toString())).append("\",")
                        .append("\"dateDebut\":\"").append(ldt(ev.getDateDebut())).append("\",")
                        .append("\"lieu\":\"").append(esc(ev.getLieu())).append("\",")
                        .append("\"meetingLink\":\"").append(esc(ev.getMeetingLink())).append("\"")
                        .append("},")
                        .append("\"invitations\":[");

                boolean first = true;
                for (Invitation inv : invs) {
                    if (inv.getEvenementId() == ev.getId()) {
                        if (!first) sb.append(",");
                        sb.append("{")
                                .append("\"id\":").append(inv.getId()).append(",")
                                .append("\"evenementId\":").append(inv.getEvenementId()).append(",")
                                .append("\"email\":\"").append(esc(inv.getEmail())).append("\",")
                                .append("\"roleInvite\":\"").append(esc(inv.getRoleInvite())).append("\",")
                                .append("\"dateInvitation\":\"").append(ldt(inv.getDateInvitation())).append("\"")
                                .append("}");
                        first = false;
                    }
                }
                sb.append("]}");
                if (i < evs.size() - 1) sb.append(",");
            }
            return sb.append("]").toString();
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  AJOUTER INVITATION + ENVOI EMAIL AUTO
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public String ajouterInvitation(int evenementId, String dateInvitation, String roleInvite, String email) {
        return wrapOk(() -> {
            Invitation inv = new Invitation(evenementId, LocalDateTime.parse(dateInvitation, FMT), roleInvite, email);
            invService.ajouter(inv);
            Evenement ev = evService.getById(evenementId);
            if (ev != null) Emailservice.envoyerInvitation(inv, ev);
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  MODIFIER INVITATION
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public String modifierInvitation(int id, int evenementId, String dateInvitation, String roleInvite, String email) {
        return wrapOk(() -> {
            Invitation inv = new Invitation(id, evenementId, LocalDateTime.parse(dateInvitation, FMT), roleInvite, email);
            invService.modifier(inv);
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  GÃ‰NÃ‰RER QR CODE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    private String currentQrBase64 = null;

    public void generateQr(String content) {
        Platform.runLater(() -> {
            try {
                String base64 = Qrservice.genererBase64(content, 220);
                currentQrBase64 = base64;
                engine.executeScript("showQrImage('" + base64 + "')");
            } catch (Exception e) {
                e.printStackTrace();
                engine.executeScript("showQrImage('ERROR')");
            }
        });
    }

    public void downloadQr(String nomEvenement) {
        Platform.runLater(() -> {
            try {
                if (currentQrBase64 == null) {
                    engine.executeScript("toast('QR Code non gÃ©nÃ©rÃ©', 'error')");
                    return;
                }
                String safeName = nomEvenement.replaceAll("[^a-zA-Z0-9_-]", "_");
                String bureau   = System.getProperty("user.home") + java.io.File.separator + "Downloads";
                java.io.File dossier = new java.io.File(bureau);
                if (!dossier.exists()) dossier = new java.io.File(System.getProperty("user.home"));

                java.io.File file = new java.io.File(dossier, "qrcode_" + safeName + ".png");
                int n = 1;
                while (file.exists()) {
                    file = new java.io.File(dossier, "qrcode_" + safeName + "_" + n + ".png");
                    n++;
                }
                byte[] bytes = java.util.Base64.getDecoder().decode(currentQrBase64);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) { fos.write(bytes); }
                engine.executeScript("toast('âœ… QR Code sauvegardÃ© dans TÃ©lÃ©chargements : " + file.getName() + "', 'success')");
                System.out.println("âœ… QR Code sauvegardÃ© : " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                engine.executeScript("toast('âŒ Erreur sauvegarde : " + e.getMessage() + "', 'error')");
            }
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  SUPPRIMER INVITATION
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public String supprimerInvitation(int id) {
        return wrapOk(() -> invService.supprimer(id));
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  HELPERS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    private String wrapOk(SqlRunnable fn) {
        try { fn.run(); return "OK"; }
        catch (Exception e) { e.printStackTrace(); return "ERROR:" + (e.getMessage()==null?"UNKNOWN":e.getMessage()); }
    }

    private String wrapJson(SqlSupplier<String> fn) {
        try { String o = fn.get(); return o==null?"[]":o; }
        catch (Exception e) { e.printStackTrace(); return "[]"; }
    }

    private String esc(String s) {
        if (s==null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","");
    }

    private String ldt(LocalDateTime t) { return t==null?"":t.format(FMT); }

    @FunctionalInterface interface SqlRunnable    { void run() throws Exception; }
    @FunctionalInterface interface SqlSupplier<T> { T    get() throws Exception; }
}


