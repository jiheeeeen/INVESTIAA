package Controllers;

import Entities.ProfilInvestisseur;
import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Entities.Investissement;
import Services.ProfilInvestisseurCRUD;
import Services.UserCRUD;
import Services.InvestissementCRUD;
import Utils.Session;
import Utils.sceneManager;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import netscape.javascript.JSObject;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import Entities.StatutVerification;

public class InvestisseurWebViewController {

    // ✅ Ouvrir la page Projets Investisseur (FXML)
    public String openProjetsInvestisseur() {
        try {
            javafx.application.Platform.runLater(() ->
                    sceneManager.switchTo("/projetViewInvestisseur.fxml", "Investia - Projets")
            );
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    @FXML private WebView webView;

    private final ProfilInvestisseurCRUD profilCrud = new ProfilInvestisseurCRUD();
    private final JavaBridge bridge = new JavaBridge();

    @FXML
    private void initialize() {
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                // ✅ DEBUG
                engine.executeScript("console.log('[JAVA] Bridge injected by InvestisseurWebViewController');");

                // ✅ bridge principal
                window.setMember("javaBridge", bridge);

                // ✅ bridge secondaire anti-écrasement (au cas où)
                window.setMember("javaBridgeInvest", bridge);

                engine.executeScript("window.__bridgeReady = true;");
            }
        });

        URL html = resolveInvestisseurPage();
        if (html != null) engine.load(html.toExternalForm());
        else engine.loadContent("<h2>Erreur: page investisseur introuvable</h2>");
    }

    // ✅ Pages dans src/main/resources (racine)
    private URL resolveInvestisseurPage() {
        User current = Session.getCurrentUser();

        System.out.println("DEBUG INVEST VIEW: " + (current == null ? "null" :
                ("id=" + current.getId() + " role=" + current.getRole() +
                        " verif=" + current.getStatutVerification() + " active=" + current.isActive())));

        if (current == null) return getClass().getResource("/attenteValidation.html");

        if (current.getRole() != Role.INVESTISSEUR) {
            return getClass().getResource("/accueil.html");
        }

        if (current.getStatutVerification() == StatutVerification.VERIFIE && current.isActive()) {
            return getClass().getResource("/accueil_investisseur.html");
        }

        if (current.getStatutVerification() == StatutVerification.NON_VERIFIE) {
            return getClass().getResource("/completerInfos_investisseur.html");
        }

        return getClass().getResource("/attenteValidation.html");
    }

    // =========================================================
    // BRIDGE JS <-> JAVA
    // =========================================================
    public class JavaBridge {

        private final InvestissementCRUD investissementCRUD = new InvestissementCRUD();
        private final Services.ProjetCRUD projetCRUD = new Services.ProjetCRUD();

        // ✅ Ouvrir écran investissement (FXML) pour le projet sélectionné
        public String openInvestir(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "ERROR:ID_PROJET_REQUIRED";

                int pid = Integer.parseInt(idProjet.trim());

                // ✅ stocker l'id projet pour l'écran suivant
                Session.setSelectedProjetId(pid);

                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investissement_form.fxml", "Investia - Investir")
                );

                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ✅ AJOUT : utilisé par investissement_form.html (header)
        public String getSelectedProjetId() {
            try {
                int pid = Session.getSelectedProjetId();
                return pid <= 0 ? "" : String.valueOf(pid);
            } catch (Exception e) {
                return "";
            }
        }

        // ✅ AJOUT : utilisé par investissement_form.html (username)
        public String getCurrentUserName() {
            User u = Session.getCurrentUser();
            if (u == null) return "";
            String nom = u.getNom() == null ? "" : u.getNom().trim();
            String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
            String full = (nom + " " + prenom).trim();
            if (!full.isEmpty()) return full;
            return u.getEmail() == null ? "" : u.getEmail().trim();
        }

        // ✅ AJOUT : utilisé par investissement_form.html (table)
        public String listInvestissementsByProjet(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "[]";
                int pid = Integer.parseInt(idProjet.trim());

                List<Investissement> list = investissementCRUD.afficherParProjet(pid);

                StringBuilder out = new StringBuilder();
                out.append("[");
                boolean first = true;
                for (Investissement x : list) {
                    if (!first) out.append(",");
                    first = false;

                    out.append("{");
                    out.append("\"id_investissement\":").append(x.getId_investissement()).append(",");
                    out.append("\"montant\":").append(x.getMontant()).append(",");
                    out.append("\"date_investissement\":").append(json(x.getDate_investissement() == null ? "" : x.getDate_investissement().toString())).append(",");
                    out.append("\"id_investisseur\":").append(x.getId_investisseur());
                    out.append("}");
                }
                out.append("]");
                return out.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "[]";
            }
        }

        // ✅ ANCIENNE méthode (NE PAS SUPPRIMER)
        public String createInvestissement(String montant) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                String pidStr = getSelectedProjetId();
                if (pidStr == null || pidStr.isBlank()) return "ERROR:ID_PROJET_REQUIRED";

                if (montant == null || montant.trim().isEmpty()) return "ERROR:MONTANT_REQUIRED";

                double m = Double.parseDouble(montant.trim());
                if (m <= 0) return "ERROR:MONTANT_INVALID";

                int pid = Integer.parseInt(pidStr);

                // ✅ récupérer id_investisseur depuis table investisseur (via id_user)
                ProfilInvestisseur profil = profilCrud.getByUserId(u.getId());
                if (profil == null) return "ERROR:PROFIL_INVESTISSEUR_NOT_FOUND";
                int idInvestisseur = profil.getIdInvestisseur();

                Investissement inv = new Investissement();
                inv.setMontant(m);
                inv.setDate_investissement(new Date(System.currentTimeMillis()));
                inv.setId_investisseur(idInvestisseur);
                inv.setId_projet(pid);

                investissementCRUD.ajouter(inv);

                return "OK:" + inv.getId_investissement();
            } catch (NumberFormatException e) {
                return "ERROR:INVALID_NUMBER";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ✅ NOUVELLE surcharge compatible HTML (4 params) — NE CASSE RIEN
        public String createInvestissement(String montant, String taux, String duree, String statut) {
            // On valide juste, mais ton Entity/table ne stocke pas ces champs (normal)
            try {
                if (taux == null || taux.trim().isEmpty()) taux = "0";
                if (duree == null || duree.trim().isEmpty()) duree = "1";
                if (statut == null || statut.trim().isEmpty()) statut = "EN_ATTENTE";

                // validations soft
                Double.parseDouble(taux.trim());
                Integer.parseInt(duree.trim());

                // on garde ta logique d’ajout existante
                return createInvestissement(montant);
            } catch (NumberFormatException ex) {
                return "ERROR:INVALID_NUMBER";
            }
        }

        // ✅ utilisé par investissement_form.html (btnBack)
        public String backToProjets() {
            try {
                javafx.application.Platform.runLater(() ->
                        Utils.sceneManager.switchTo("/investisseur_projets_view.fxml", "Investia - Projets")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        public String openProjetsInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        Utils.sceneManager.switchTo("/investisseur_projets_view.fxml", "Investia - Projets")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        public String goAccueilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        Utils.sceneManager.switchTo("/investisseur_view.fxml", "Investia - Accueil Investisseur")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // ✅ LISTE PROJETS pour investisseur (appelée par projet_view_investisseur.html)
        public String listProjets() {
            try {
                java.sql.Connection c = Utils.MyBD.getInstance().getConn();

                String sql = """
            SELECT id_projet, statut, titre, secteur, description_courte, objectif_tnd, updated_at
            FROM projet
            ORDER BY COALESCE(updated_at, created_at) DESC
        """;

                java.sql.PreparedStatement ps = c.prepareStatement(sql);
                java.sql.ResultSet rs = ps.executeQuery();

                StringBuilder out = new StringBuilder();
                out.append("[");

                boolean first = true;
                while (rs.next()) {
                    if (!first) out.append(",");
                    first = false;

                    int id = rs.getInt("id_projet");
                    String statutDb = rs.getString("statut");
                    String titre = rs.getString("titre");
                    String secteur = rs.getString("secteur");
                    String desc = rs.getString("description_courte");
                    java.math.BigDecimal objectif = rs.getBigDecimal("objectif_tnd");
                    java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");

                    String statusUi = mapStatutToUi(statutDb);

                    out.append("{");
                    out.append("\"id\":").append(id).append(",");
                    out.append("\"status\":").append(json(statusUi)).append(",");
                    out.append("\"title\":").append(json(titre)).append(",");
                    out.append("\"category\":").append(json(secteur)).append(",");
                    out.append("\"short\":").append(json(desc)).append(",");
                    out.append("\"goal\":").append(objectif == null ? "0" : objectif.toPlainString()).append(",");
                    out.append("\"updatedAt\":").append(json(updatedAt == null ? "" : updatedAt.toString()));
                    out.append("}");
                }

                out.append("]");

                rs.close();
                ps.close();

                return out.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "[]";
            }
        }

        // ✅ détails projet pour detailsInvestisseur.html
        public String getProjetById(String id) {
            try {
                int projectId = Integer.parseInt(id);
                Entities.Projet p = projetCRUD.getById(projectId);
                if (p == null) return "null";
                return Controllers.ProjetWebUtils.toDetailJson(p);
            } catch (Exception e) {
                return "null";
            }
        }

        private String mapStatutToUi(String db) {
            if (db == null) return "DRAFT";
            String s = db.trim().toUpperCase();

            if (s.equals("BROUILLON")) return "DRAFT";
            if (s.equals("EN_ATTENTE")) return "PENDING";
            if (s.equals("VALIDE")) return "VALIDATED";
            if (s.equals("REFUSE")) return "REJECTED";

            if (s.equals("DRAFT") || s.equals("PENDING") || s.equals("VALIDATED") || s.equals("REJECTED")) return s;

            return "DRAFT";
        }

        private String json(String s) {
            if (s == null) return "null";
            String esc = s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
            return "\"" + esc + "\"";
        }

        public String openProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/profil_investisseur_view.fxml", "Investia - Mon Profil (Investisseur)")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openEditProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/edit_profil_investisseur_view.fxml", "Investia - Modifier Profil")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String getCurrentUserId() {
            User u = Session.getCurrentUser();
            return u == null ? "" : String.valueOf(u.getId());
        }

        public String getCurrentUserRole() {
            User u = Session.getCurrentUser();
            return (u == null || u.getRole() == null) ? "" : u.getRole().name();
        }

        public String getProfilInvestisseurJson() {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "null";

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("{");
                    sb.append("\"idUser\":").append(u.getId()).append(",");
                    sb.append("\"nom\":").append(jsonString(u.getNom())).append(",");
                    sb.append("\"prenom\":").append(jsonString(u.getPrenom())).append(",");
                    sb.append("\"email\":").append(jsonString(u.getEmail())).append(",");
                    sb.append("\"telephone\":").append(jsonString(u.getTelephone())).append(",");
                    sb.append("\"cin\":").append(jsonString(u.getCin())).append(",");
                    sb.append("\"budgetTotal\":null,");
                    sb.append("\"budgetMensuel\":null,");
                    sb.append("\"ticketMoyenParProjet\":null,");
                    sb.append("\"horizonInvestissement\":\"MOYEN\",");
                    sb.append("\"bio\":null,");
                    sb.append("\"secteurs\":null,");
                    sb.append("\"cinRectoUrl\":null,");
                    sb.append("\"cinVersoUrl\":null,");
                    sb.append("\"photoUrl\":null");
                    sb.append("}");
                    return sb.toString();
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"idUser\":").append(u.getId()).append(",");

                sb.append("\"nom\":").append(jsonString(u.getNom())).append(",");
                sb.append("\"prenom\":").append(jsonString(u.getPrenom())).append(",");
                sb.append("\"email\":").append(jsonString(u.getEmail())).append(",");
                sb.append("\"telephone\":").append(jsonString(u.getTelephone())).append(",");
                sb.append("\"cin\":").append(jsonString(u.getCin())).append(",");

                sb.append("\"budgetTotal\":").append(p.getBudgetTotal() == null ? "null" : p.getBudgetTotal()).append(",");
                sb.append("\"budgetMensuel\":").append(p.getBudgetMensuel() == null ? "null" : p.getBudgetMensuel()).append(",");
                sb.append("\"ticketMoyenParProjet\":").append(p.getTicketMoyenParProjet() == null ? "null" : p.getTicketMoyenParProjet()).append(",");
                sb.append("\"horizonInvestissement\":").append(jsonString(p.getHorizonInvestissement())).append(",");
                sb.append("\"bio\":").append(jsonString(p.getBio())).append(",");
                sb.append("\"secteurs\":").append(jsonString(p.getSecteurs() == null ? null : String.join(",", p.getSecteurs()))).append(",");
                sb.append("\"cinRectoUrl\":").append(jsonString(p.getCinRectoUrl())).append(",");
                sb.append("\"cinVersoUrl\":").append(jsonString(p.getCinVersoUrl())).append(",");
                sb.append("\"photoUrl\":").append(jsonString(p.getPhotoUrl()));
                sb.append("}");

                return sb.toString();
            } catch (Exception e) {
                return "null";
            }
        }

        public String updateProfilInvestisseur(
                String nom,
                String prenom,
                String email,
                String telephone,
                String cin,
                String budgetTotal,
                String budgetMensuel,
                String ticketMoyenParProjet,
                String horizonInvestissement,
                String bio,
                String secteursCsv
        ) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";
                if (u.getRole() != Role.INVESTISSEUR) return "ERROR:ROLE_NOT_ALLOWED";

                if (isBlank(email)) return "ERROR:EMAIL_REQUIRED";
                if (isBlank(ticketMoyenParProjet)) return "ERROR:TICKET_REQUIRED";
                if (isBlank(budgetTotal)) return "ERROR:BUDGET_TOTAL_REQUIRED";
                if (isBlank(horizonInvestissement)) horizonInvestissement = "MOYEN";

                u.setNom(emptyToNull(nom));
                u.setPrenom(emptyToNull(prenom));
                u.setEmail(email.trim());
                u.setTelephone(emptyToNull(telephone));
                u.setCin(emptyToNull(cin));

                UserCRUD userCrud = new UserCRUD();
                userCrud.updateUser(u);
                Session.setCurrentUser(u);

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null) {
                    p = new ProfilInvestisseur();
                    p.setIdUser(u.getId());
                }

                p.setBudgetTotal(new BigDecimal(budgetTotal.trim()));
                p.setBudgetMensuel(parseBigDecimalOptional(budgetMensuel));
                p.setTicketMoyenParProjet(new BigDecimal(ticketMoyenParProjet.trim()));
                p.setHorizonInvestissement(horizonInvestissement.trim());
                p.setBio(emptyToNull(bio));
                p.setSecteurs(parseSecteurs(secteursCsv));

                profilCrud.upsertForUser(p);

                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/profil_investisseur_view.fxml", "Investia - Mon Profil (Investisseur)")
                );

                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
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

                if (budgetTotal == null || budgetTotal.trim().isEmpty()) return "ERROR:budget_total_required";
                if (ticketMoyenParProjet == null || ticketMoyenParProjet.trim().isEmpty()) return "ERROR:ticket_required";
                if (horizonInvestissement == null || horizonInvestissement.trim().isEmpty()) return "ERROR:horizon_required";
                if (!parseBoolean(accepteConditions)) return "ERROR:must_accept_conditions";

                if (cinRectoUrl == null || cinRectoUrl.trim().isEmpty()) return "ERROR:CIN_RECTO_REQUIRED";
                if (cinVersoUrl == null || cinVersoUrl.trim().isEmpty()) return "ERROR:CIN_VERSO_REQUIRED";
                if (photoUrl == null || photoUrl.trim().isEmpty()) return "ERROR:PHOTO_REQUIRED";

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

                // Demande de verification admin (meme logique qu'entrepreneur)
                UserCRUD userCRUD = new UserCRUD();
                userCRUD.submitProfileForVerification(u.getId());
                u.setStatutVerification(StatutVerification.EN_ATTENTE);
                u.setActive(false);
                Session.setCurrentUser(u);

                javafx.application.Platform.runLater(() -> {
                    WebEngine engine = webView.getEngine();
                    URL page = getClass().getResource("/attenteValidation.html");
                    if (page != null) engine.load(page.toExternalForm());
                    else engine.loadContent("<h2>attenteValidation.html introuvable (racine resources)</h2>");
                });

                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

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

                Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "investisseurs", String.valueOf(current.getId()), "images");
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
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

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
