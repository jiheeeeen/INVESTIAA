package Controllers;

import Entities.Evenement;
import Services.EvenementService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import Services.PdfExportService;
import Services.InvestisseurNotificationCRUD;
import Controllers.SessionBridgeController;
import javafx.stage.FileChooser;
import org.example.ProjetWebViewController;
import Utils.sceneManager;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bridge Java â†” JS pour ajoutEvenement.html.
 * ExposÃ© sous window.javaBridge.
 */
public class EvenementBridge {

    private final EvenementService service = new EvenementService();
    private final InvestisseurNotificationCRUD investisseurNotificationCRUD = new InvestisseurNotificationCRUD();
    private final SessionBridgeController sessionBridge = new SessionBridgeController();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WebEngine engine;
    // âœ… RÃ©fÃ©rence forte â€” empÃªche le GC
    private JSObject jsWindow;

    public EvenementBridge(WebEngine engine) {
        this.engine = engine;
    }

    public void inject() {
        jsWindow = (JSObject) engine.executeScript("window");
        jsWindow.setMember("javaBridge", this);
        System.out.println("âœ… EvenementBridge injectÃ©");
    }

    public String getCurrentUserName() { return sessionBridge.getCurrentUserName(); }
    public String getCurrentUserId() { return sessionBridge.getCurrentUserId(); }
    public String getCurrentUserRole() { return sessionBridge.getCurrentUserRole(); }
    public String logout() { return sessionBridge.logout(); }
    public String getEntrepreneurInvestmentNotificationsJson() { return sessionBridge.getEntrepreneurInvestmentNotificationsJson(); }
    public String markEntrepreneurNotificationRead(String id) { return sessionBridge.markEntrepreneurNotificationRead(id); }
    public String getEntrepreneurNotificationHistoryJson(String limit) { return sessionBridge.getEntrepreneurNotificationHistoryJson(limit); }
    public String getInvestorProjectNotificationsJson() { return sessionBridge.getInvestorProjectNotificationsJson(); }
    public String markInvestorNotificationRead(String id) { return sessionBridge.markInvestorNotificationRead(id); }
    public String getInvestorNotificationHistoryJson(String limit) { return sessionBridge.getInvestorNotificationHistoryJson(limit); }

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

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  NAVIGATION â†’ Chatbot
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public void goChatbot() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chatbot.fxml"));
                Parent root  = loader.load();
                Stage  stage = (Stage) Window.getWindows().stream()
                        .filter(Window::isShowing).findFirst().orElse(null);
                if (stage != null) {
                    stage.setTitle("Investia â€” Assistant IA");
                    stage.getScene().setRoot(root);
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  NAVIGATION â†’ Invitations
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public void goInvitations() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/invitation.fxml"));
                Parent root  = loader.load();
                Stage  stage = (Stage) Window.getWindows().stream()
                        .filter(Window::isShowing).findFirst().orElse(null);
                if (stage != null) {
                    stage.setTitle("Investia â€” Invitations");
                    stage.getScene().setRoot(root);
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  âœ… OUVRIR URL DANS LE NAVIGATEUR SYSTÃˆME
    //  AppelÃ© depuis JS : window.javaBridge.openUrl(url)
    //  Ã‰vite d'ouvrir une nouvelle fenÃªtre JavaFX qui plante l'app
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
    //  OUVRIR FENÃŠTRE MODIFIER
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public void openModifierWindow(int id, String titre, int projectId, int organisateurId,
                                   String description, String mode, String dateDebut, String dateFin,
                                   String lieu, String meetingLink) {

        Runnable onSuccess = () -> Platform.runLater(() -> {
            try {
                String json = getAll();
                engine.executeScript("renderTable(" + json + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Modifierevenementcontroller ctrl = new Modifierevenementcontroller(
                id, titre, projectId, organisateurId,
                description, mode, dateDebut, dateFin,
                lieu, meetingLink, onSuccess
        );
        ctrl.show();
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  AJOUTER
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public String ajouter(String titre, int projectId, String description,
                          String mode, String dateDebut, String dateFin,
                          String lieu, String meetingLink, int organisateurId) {
        return wrapOk(() -> {
            Evenement e = new Evenement(
                    projectId, titre, description, mode,
                    LocalDateTime.parse(dateDebut, FMT),
                    LocalDateTime.parse(dateFin,   FMT),
                    lieu, meetingLink, organisateurId
            );
            service.ajouter(e);
            investisseurNotificationCRUD.insertForAllInvestisseurs(projectId);
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  EXPORT PDF
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public void exportPdf() {
        Platform.runLater(() -> {
            try {
                FileChooser fc = new FileChooser();
                fc.setTitle("Enregistrer le rapport PDF");
                fc.setInitialFileName("evenements_investia.pdf");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

                Stage stage = (Stage) Window.getWindows().stream()
                        .filter(Window::isShowing).findFirst().orElse(null);

                File file = fc.showSaveDialog(stage);
                if (file == null) return;

                PdfExportService.generer(service.getAll(), file.getAbsolutePath());

                engine.executeScript("onPdfExported('OK', '"
                        + file.getAbsolutePath().replace("\\", "/").replace("'", "\\'") + "')");

            } catch (Exception e) {
                e.printStackTrace();
                engine.executeScript("onPdfExported('ERROR', '" + e.getMessage() + "')");
            }
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  SUPPRIMER
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public String supprimer(int id) {
        return wrapOk(() -> service.supprimer(id));
    }

    public String modifier(int id, String titre, int projectId, String description,
                           String mode, String dateDebut, String dateFin,
                           String lieu, String meetingLink, int organisateurId) {
        return wrapOk(() -> {
            Evenement e = new Evenement(
                    id, projectId, titre, description, mode,
                    LocalDateTime.parse(dateDebut, FMT),
                    LocalDateTime.parse(dateFin, FMT),
                    lieu, meetingLink, organisateurId
            );
            service.modifier(e);
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  GET ALL
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public String getAll() {
        return wrapJson(() -> {
            List<Evenement> list = service.getAll();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Evenement e = list.get(i);
                sb.append("{")
                        .append("\"id\":").append(e.getId()).append(",")
                        .append("\"projectId\":").append(e.getProjectId()).append(",")
                        .append("\"organisateurId\":").append(e.getOrganisateurId()).append(",")
                        .append("\"titre\":\"").append(escape(e.getTitre())).append("\",")
                        .append("\"description\":\"").append(escape(e.getDescription())).append("\",")
                        .append("\"mode\":\"").append(escape(e.getMode() == null ? "" : e.getMode().toString())).append("\",")
                        .append("\"lieu\":\"").append(escape(e.getLieu())).append("\",")
                        .append("\"meetingLink\":\"").append(escape(e.getMeetingLink())).append("\",")
                        .append("\"dateDebut\":\"").append(ldt(e.getDateDebut())).append("\",")
                        .append("\"dateFin\":\"").append(ldt(e.getDateFin())).append("\"")
                        .append("}");
                if (i < list.size() - 1) sb.append(",");
            }
            return sb.append("]").toString();
        });
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

    private String escape(String s) {
        if (s==null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","");
    }

    private String ldt(LocalDateTime t) { return t==null?"":t.format(FMT); }

    @FunctionalInterface interface SqlRunnable   { void run() throws Exception; }
    @FunctionalInterface interface SqlSupplier<T>{ T    get() throws Exception; }
}
