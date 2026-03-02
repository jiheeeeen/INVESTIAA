package Controllers;

import Entities.ProfilInvestisseur;
import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Services.ProfilInvestisseurCRUD;
import Services.UserCRUD;
import Utils.Session;
import Utils.sceneManager;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.web.WebView;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public class ProfilInvestisseurBridgeController {

    private final WebView webView;
    private final ProfilInvestisseurCRUD profilCrud = new ProfilInvestisseurCRUD();
    private final UserCRUD userCRUD = new UserCRUD();

    public ProfilInvestisseurBridgeController(WebView webView) {
        this.webView = webView;
    }

    // =========================================================
    // JSON profil (pour loadMe())
    // =========================================================
    public String getProfilInvestisseurJson() {
        try {
            User u = Session.getCurrentUser();
            if (u == null) return "null";

            ProfilInvestisseur p = profilCrud.getByUserId(u.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"idUser\":").append(u.getId()).append(",");
            sb.append("\"nom\":").append(jsonString(u.getNom())).append(",");
            sb.append("\"prenom\":").append(jsonString(u.getPrenom())).append(",");
            sb.append("\"email\":").append(jsonString(u.getEmail())).append(",");
            sb.append("\"telephone\":").append(jsonString(u.getTelephone())).append(",");
            sb.append("\"cin\":").append(jsonString(u.getCin())).append(",");

            if (p == null) {
                sb.append("\"budgetTotal\":null,");
                sb.append("\"budgetMensuel\":null,");
                sb.append("\"ticketMoyenParProjet\":null,");
                sb.append("\"horizonInvestissement\":\"COURT\",");
                sb.append("\"bio\":null,");
                sb.append("\"secteurs\":null,");
                sb.append("\"cinRectoUrl\":null,");
                sb.append("\"cinVersoUrl\":null,");
                sb.append("\"photoUrl\":null");
            } else {
                sb.append("\"budgetTotal\":").append(p.getBudgetTotal() == null ? "null" : p.getBudgetTotal()).append(",");
                sb.append("\"budgetMensuel\":").append(p.getBudgetMensuel() == null ? "null" : p.getBudgetMensuel()).append(",");
                sb.append("\"ticketMoyenParProjet\":").append(p.getTicketMoyenParProjet() == null ? "null" : p.getTicketMoyenParProjet()).append(",");
                sb.append("\"horizonInvestissement\":").append(jsonString(p.getHorizonInvestissement())).append(",");
                sb.append("\"bio\":").append(jsonString(p.getBio())).append(",");
                sb.append("\"secteurs\":").append(jsonString(p.getSecteurs() == null ? null : String.join(",", p.getSecteurs()))).append(",");
                sb.append("\"cinRectoUrl\":").append(jsonString(p.getCinRectoUrl())).append(",");
                sb.append("\"cinVersoUrl\":").append(jsonString(p.getCinVersoUrl())).append(",");
                sb.append("\"photoUrl\":").append(jsonString(p.getPhotoUrl()));
            }

            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "null";
        }
    }

    // =========================================================
    // Uploads
    // =========================================================
    public String uploadPdf(String docType) {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Charger un PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

            Window owner = (webView != null && webView.getScene() != null) ? webView.getScene().getWindow() : null;
            File selected = chooser.showOpenDialog(owner);
            if (selected == null) return "";

            String lower = selected.getName().toLowerCase();
            if (!lower.endsWith(".pdf")) return "ERROR:ONLY_PDF";

            Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "investisseurs", String.valueOf(current.getId()));
            Files.createDirectories(uploadDir);

            String safe = sanitizeDocType(docType);
            String filename = safe + "_" + System.currentTimeMillis() + ".pdf";
            Path dest = uploadDir.resolve(filename);

            Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toUri().toString();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String uploadImage(String docType) {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Charger une image");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
            );

            Window owner = (webView != null && webView.getScene() != null) ? webView.getScene().getWindow() : null;
            File selected = chooser.showOpenDialog(owner);
            if (selected == null) return "";

            String lower = selected.getName().toLowerCase();
            String ext;
            if (lower.endsWith(".png")) ext = ".png";
            else if (lower.endsWith(".jpg")) ext = ".jpg";
            else if (lower.endsWith(".jpeg")) ext = ".jpeg";
            else if (lower.endsWith(".webp")) ext = ".webp";
            else return "ERROR:ONLY_IMAGE";

            Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "investisseurs",
                    String.valueOf(current.getId()), "images");
            Files.createDirectories(uploadDir);

            String safe = sanitizeDocType(docType);
            String filename = safe + "_" + System.currentTimeMillis() + ext;
            Path dest = uploadDir.resolve(filename);

            Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toUri().toString();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    // =========================================================
    // Submit (Valider)
    // =========================================================
    public String submitProfileForVerification(
            String budgetTotal,
            String budgetMensuel,
            String ticketMoyenParProjet,
            String horizonInvestissement,
            String bio,
            String accepteConditions,
            String secteursCsv,
            String cinRectoUrl,
            String cinVersoUrl,
            String photoUrl
    ) {
        return saveProfilInvestisseur(
                budgetTotal, budgetMensuel, ticketMoyenParProjet, horizonInvestissement,
                bio, accepteConditions, secteursCsv, cinRectoUrl, cinVersoUrl, photoUrl
        );
    }

    public String saveProfilInvestisseur(String budgetTotal,
                                         String budgetMensuel,
                                         String ticketMoyenParProjet,
                                         String horizonInvestissement,
                                         String bio,
                                         String accepteConditions,
                                         String secteursCsv,
                                         String cinRectoUrl,
                                         String cinVersoUrl,
                                         String photoUrl) {
        try {
            User u = Session.getCurrentUser();
            if (u == null) return "ERROR:USER_NOT_CONNECTED";
            if (u.getRole() != Role.INVESTISSEUR) return "ERROR:ROLE_NOT_ALLOWED";

            if (isBlank(budgetTotal)) return "ERROR:budget_total_required";
            if (isBlank(ticketMoyenParProjet)) return "ERROR:ticket_required";
            if (isBlank(horizonInvestissement)) return "ERROR:horizon_required";
            if (!parseBoolean(accepteConditions)) return "ERROR:must_accept_conditions";

            if (isBlank(cinRectoUrl)) return "ERROR:CIN_RECTO_REQUIRED";
            if (isBlank(cinVersoUrl)) return "ERROR:CIN_VERSO_REQUIRED";
            if (isBlank(photoUrl)) return "ERROR:PHOTO_REQUIRED";

            ProfilInvestisseur p = new ProfilInvestisseur();
            p.setIdUser(u.getId());

            p.setBudgetTotal(new BigDecimal(budgetTotal.trim()));
            p.setBudgetMensuel(parseBigDecimalOptional(budgetMensuel));
            p.setTicketMoyenParProjet(new BigDecimal(ticketMoyenParProjet.trim()));
            p.setHorizonInvestissement(horizonInvestissement.trim());
            p.setBio(emptyToNull(bio));
            p.setAccepteConditions(true);
            p.setSecteurs(parseSecteurs(secteursCsv));

            p.setCinRectoUrl(cinRectoUrl.trim());
            p.setCinVersoUrl(cinVersoUrl.trim());
            p.setPhotoUrl(photoUrl.trim());

            profilCrud.upsertForUser(p);

            // ✅ demande vérification admin
            userCRUD.submitProfileForVerification(u.getId());
            u.setStatutVerification(StatutVerification.EN_ATTENTE);
            u.setActive(false);
            Session.setCurrentUser(u);

            // ✅ retour login + pending message
            Session.setCurrentUser(null);
            WebAuthController.openLoginPendingOnNextLoad();

            javafx.application.Platform.runLater(() ->
                    sceneManager.switchTo("/web_auth.fxml", "Investia - Connexion")
            );

            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    // =========================================================
    // Navigation
    // =========================================================
    public String logout() {
        try {
            Session.setCurrentUser(null);
            WebAuthController.openLoginOnNextLoad();
            javafx.application.Platform.runLater(() ->
                    sceneManager.switchTo("/web_auth.fxml", "Investia - Connexion")
            );
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    // =========================================================
    // Helpers
    // =========================================================
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String emptyToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("on") || v.equals("yes");
    }

    private static BigDecimal parseBigDecimalOptional(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : new BigDecimal(t);
    }

    private static Set<String> parseSecteurs(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        Set<String> set = new HashSet<>();
        for (String s : csv.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) set.add(v);
        }
        return set.isEmpty() ? null : set;
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private static String sanitizeDocType(String input) {
        if (input == null || input.isBlank()) return "document";
        String out = input.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        return out.isBlank() ? "document" : out;
    }
}