package Controllers;

import Entities.ProfilEntrepreneur;
import Entities.Role;
import Entities.StatutCompte;
import Entities.StatutVerification;
import Entities.User;
import Utils.Session;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class ProfilBridgeController {
    private final ProjetWebContext context;

    public ProfilBridgeController(ProjetWebContext context) {
        this.context = context;
    }

    public String getCurrentUserWithProfil() {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "{\"user\":null,\"profil\":null}";

            User user = context.getUserCrud().findById(current.getId());
            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"user\":").append(user == null ? "null" : ProjetWebUtils.userToJson(user)).append(",");
            sb.append("\"profil\":").append(profil == null ? "null" : ProjetWebUtils.profilToJson(profil));
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "{\"user\":null,\"profil\":null}";
        }
    }

    public String getCurrentEntrepreneurId() {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "";
            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
            if (profil == null) return "";
            return String.valueOf(profil.getIdEntrepreneur());
        } catch (Exception e) {
            return "";
        }
    }

    public String updateCurrentUserAndProfil(String nom,
                                             String prenom,
                                             String telephone,
                                             String cin,
                                             String dateNaissance,
                                             String nationalite,
                                             String adresseUser,
                                             String ville,
                                             String adresseProfil,
                                             String rib,
                                             String bio,
                                             String photoUrl,
                                             String secteursCsv,
                                             String autreSecteur) {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";

            User user = context.getUserCrud().findById(current.getId());
            if (user == null) return "ERROR:USER_NOT_FOUND";

            user.setNom(keepIfBlank(nom, user.getNom()));
            user.setPrenom(keepIfBlank(prenom, user.getPrenom()));
            user.setTelephone(keepIfBlank(telephone, user.getTelephone()));
            user.setCin(keepIfBlank(cin, user.getCin()));
            user.setDateNaissance(dateOrExisting(dateNaissance, user.getDateNaissance()));
            user.setNationalite(keepIfBlank(nationalite, user.getNationalite()));
            user.setAdresse(keepIfBlank(adresseUser, user.getAdresse()));
            user.setVille(keepIfBlank(ville, user.getVille()));
            context.getUserCrud().updateUser(user);

            // Keep Session user in sync so navbar updates immediately without relogin.
            current.setNom(user.getNom());
            current.setPrenom(user.getPrenom());
            current.setTelephone(user.getTelephone());
            current.setCin(user.getCin());
            current.setDateNaissance(user.getDateNaissance());
            current.setNationalite(user.getNationalite());
            current.setAdresse(user.getAdresse());
            current.setVille(user.getVille());

            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
            if (profil == null) {
                profil = new ProfilEntrepreneur();
                profil.setIdUser(current.getId());
                profil.setAccepteConditions(true);
                profil.setStatutCompte(StatutCompte.ACTIF);
                profil.setStatutVerification(current.getStatutVerification());
                profil.setDateVerification(null);
            }

            profil.setAdresse(ProjetWebUtils.emptyToNull(adresseProfil));
            profil.setRib(ProjetWebUtils.emptyToNull(rib));
            profil.setBio(ProjetWebUtils.mergeBioWithAutreSecteur(ProjetWebUtils.emptyToNull(bio), ProjetWebUtils.emptyToNull(autreSecteur)));
            profil.setPhotoUrl(ProjetWebUtils.emptyToNull(photoUrl));
            profil.setSecteurs(ProjetWebUtils.parseSecteurs(secteursCsv));
            context.getProfilCrud().upsertForCurrentUser(profil);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String submitProfileForVerification(String adresse,
                                               String cinRectoUrl,
                                               String cinVersoUrl,
                                               String justificatifDomicileUrl,
                                               String rib,
                                               String bio,
                                               String registreCommerceUrl,
                                               String patenteUrl,
                                               String matriculeFiscalUrl,
                                               String carteFiscaleUrl,
                                               String secteursCsv,
                                               String autreSecteur) {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";
            if (current.getRole() != Role.ENTREPRENEUR) return "ERROR:ROLE_NOT_ALLOWED";
            ProfilEntrepreneur existing = context.getProfilCrud().getByUserId(current.getId());

            ProfilEntrepreneur p = new ProfilEntrepreneur();
            if (existing != null) {
                p.setIdEntrepreneur(existing.getIdEntrepreneur());
            }
            p.setIdUser(current.getId());
            p.setAdresse(keepIfBlank(adresse, existing == null ? null : existing.getAdresse()));
            p.setCinRectoUrl(ProjetWebUtils.emptyToNull(cinRectoUrl));
            p.setCinVersoUrl(ProjetWebUtils.emptyToNull(cinVersoUrl));
            p.setJustificatifDomicileUrl(ProjetWebUtils.emptyToNull(justificatifDomicileUrl));
            p.setRib(keepIfBlank(rib, existing == null ? null : existing.getRib()));
            p.setAccepteConditions(true);
            p.setStatutCompte(StatutCompte.ACTIF);
            p.setStatutVerification(StatutVerification.EN_ATTENTE);
            p.setDateVerification(null);
            p.setBio(ProjetWebUtils.mergeBioWithAutreSecteur(ProjetWebUtils.emptyToNull(bio), ProjetWebUtils.emptyToNull(autreSecteur)));
            p.setPhotoUrl(existing == null ? null : existing.getPhotoUrl());
            p.setRegistreCommerceUrl(ProjetWebUtils.emptyToNull(registreCommerceUrl));
            p.setPatenteUrl(ProjetWebUtils.emptyToNull(patenteUrl));
            p.setMatriculeFiscalUrl(ProjetWebUtils.emptyToNull(matriculeFiscalUrl));
            p.setCarteFiscaleUrl(ProjetWebUtils.emptyToNull(carteFiscaleUrl));
            p.setSecteurs(ProjetWebUtils.parseSecteurs(secteursCsv) != null
                    ? ProjetWebUtils.parseSecteurs(secteursCsv)
                    : (existing == null ? null : existing.getSecteurs()));

            context.getProfilCrud().upsertForCurrentUser(p);
            context.getUserCrud().submitProfileForVerification(current.getId());
            current.setStatutVerification(StatutVerification.EN_ATTENTE);
            current.setActive(false);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String uploadPdf(String docType) {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Charger un PDF");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF", "*.pdf")
            );

            Window owner = resolveOwnerWindow();
            File selected = chooser.showOpenDialog(owner);
            if (selected == null) return "";

            String lower = selected.getName().toLowerCase();
            if (!lower.endsWith(".pdf")) return "ERROR:ONLY_PDF";

            Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "profils", String.valueOf(current.getId()));
            Files.createDirectories(uploadDir);

            String safeDocType = ProjetWebUtils.sanitizeDocType(docType);
            String filename = safeDocType + "_" + System.currentTimeMillis() + ".pdf";
            Path destination = uploadDir.resolve(filename);

            Files.copy(selected.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination.toUri().toString();
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

            Window owner = resolveOwnerWindow();
            File selected = chooser.showOpenDialog(owner);
            if (selected == null) return "";

            String lower = selected.getName().toLowerCase();
            String ext = ".jpg";
            if (lower.endsWith(".png")) ext = ".png";
            else if (lower.endsWith(".jpg")) ext = ".jpg";
            else if (lower.endsWith(".jpeg")) ext = ".jpeg";
            else if (lower.endsWith(".webp")) ext = ".webp";
            else return "ERROR:ONLY_IMAGE";

            Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "profils", String.valueOf(current.getId()), "images");
            Files.createDirectories(uploadDir);

            String safeDocType = ProjetWebUtils.sanitizeDocType(docType);
            String filename = safeDocType + "_" + System.currentTimeMillis() + ext;
            Path destination = uploadDir.resolve(filename);

            Files.copy(selected.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination.toUri().toString();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String openFile(String input) {
        try {
            URI uri = toUri(input);
            if (uri == null) return "ERROR:EMPTY_PATH";
            if (!Desktop.isDesktopSupported()) return "ERROR:DESKTOP_NOT_SUPPORTED";
            Desktop.getDesktop().browse(uri);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String readImageAsDataUrl(String input) {
        try {
            Path p = toPath(input);
            if (p == null || !Files.exists(p)) return "";
            String name = p.getFileName().toString().toLowerCase();
            String mime = "image/jpeg";
            if (name.endsWith(".png")) mime = "image/png";
            else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) mime = "image/jpeg";
            else if (name.endsWith(".webp")) mime = "image/webp";
            byte[] bytes = Files.readAllBytes(p);
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String normalizeLocalFileUri(String input) {
        try {
            if (input == null || input.isBlank()) return "";
            Path p = toPath(input);
            if (p != null && Files.exists(p)) {
                return p.toUri().toString();
            }
            String s = input.trim();
            if (s.startsWith("file:") || s.startsWith("http://") || s.startsWith("https://")) {
                return s;
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public String getProfilById(String idEntrepreneur) {
        try {
            int id = Integer.parseInt(idEntrepreneur);
            ProfilEntrepreneur p = context.getProfilCrud().getById(id);
            if (p == null) return "null";
            return ProjetWebUtils.profilToJson(p);
        } catch (Exception e) {
            return "null";
        }
    }

    public String getProfilByUserId(String idUser) {
        try {
            int id = Integer.parseInt(idUser);
            ProfilEntrepreneur p = context.getProfilCrud().getByUserId(id);
            if (p == null) return "null";
            return ProjetWebUtils.profilToJson(p);
        } catch (Exception e) {
            return "null";
        }
    }

    public String addProfil(String idUser,
                            String adresse,
                            String cinRectoUrl,
                            String cinVersoUrl,
                            String justificatifDomicileUrl,
                            String rib,
                            String accepteConditions,
                            String statutCompte,
                            String statutVerification,
                            String dateVerification,
                            String bio,
                            String photoUrl,
                            String registreCommerceUrl,
                            String patenteUrl,
                            String matriculeFiscalUrl,
                            String carteFiscaleUrl,
                            String secteursCsv) {
        try {
            ProfilEntrepreneur p = new ProfilEntrepreneur();
            p.setIdUser(ProjetWebUtils.parseIntRequired(idUser));
            p.setAdresse(ProjetWebUtils.emptyToNull(adresse));
            p.setCinRectoUrl(ProjetWebUtils.emptyToNull(cinRectoUrl));
            p.setCinVersoUrl(ProjetWebUtils.emptyToNull(cinVersoUrl));
            p.setJustificatifDomicileUrl(ProjetWebUtils.emptyToNull(justificatifDomicileUrl));
            p.setRib(ProjetWebUtils.emptyToNull(rib));
            p.setAccepteConditions(ProjetWebUtils.parseBoolean(accepteConditions));
            p.setStatutCompte(ProjetWebUtils.parseStatutCompte(statutCompte));
            p.setStatutVerification(ProjetWebUtils.parseStatutVerification(statutVerification));
            p.setDateVerification(ProjetWebUtils.parseTimestamp(dateVerification));
            p.setBio(ProjetWebUtils.emptyToNull(bio));
            p.setPhotoUrl(ProjetWebUtils.emptyToNull(photoUrl));
            p.setRegistreCommerceUrl(ProjetWebUtils.emptyToNull(registreCommerceUrl));
            p.setPatenteUrl(ProjetWebUtils.emptyToNull(patenteUrl));
            p.setMatriculeFiscalUrl(ProjetWebUtils.emptyToNull(matriculeFiscalUrl));
            p.setCarteFiscaleUrl(ProjetWebUtils.emptyToNull(carteFiscaleUrl));
            p.setSecteurs(ProjetWebUtils.parseSecteurs(secteursCsv));
            context.getProfilCrud().ajouter(p);
            return "OK:" + p.getIdEntrepreneur();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String updateProfil(String idEntrepreneur,
                               String idUser,
                               String adresse,
                               String cinRectoUrl,
                               String cinVersoUrl,
                               String justificatifDomicileUrl,
                               String rib,
                               String accepteConditions,
                               String statutCompte,
                               String statutVerification,
                               String dateVerification,
                               String bio,
                               String photoUrl,
                               String registreCommerceUrl,
                               String patenteUrl,
                               String matriculeFiscalUrl,
                               String carteFiscaleUrl,
                               String secteursCsv) {
        try {
            ProfilEntrepreneur p = new ProfilEntrepreneur();
            p.setIdEntrepreneur(ProjetWebUtils.parseIntRequired(idEntrepreneur));
            p.setIdUser(ProjetWebUtils.parseIntRequired(idUser));
            p.setAdresse(ProjetWebUtils.emptyToNull(adresse));
            p.setCinRectoUrl(ProjetWebUtils.emptyToNull(cinRectoUrl));
            p.setCinVersoUrl(ProjetWebUtils.emptyToNull(cinVersoUrl));
            p.setJustificatifDomicileUrl(ProjetWebUtils.emptyToNull(justificatifDomicileUrl));
            p.setRib(ProjetWebUtils.emptyToNull(rib));
            p.setAccepteConditions(ProjetWebUtils.parseBoolean(accepteConditions));
            p.setStatutCompte(ProjetWebUtils.parseStatutCompte(statutCompte));
            p.setStatutVerification(ProjetWebUtils.parseStatutVerification(statutVerification));
            p.setDateVerification(ProjetWebUtils.parseTimestamp(dateVerification));
            p.setBio(ProjetWebUtils.emptyToNull(bio));
            p.setPhotoUrl(ProjetWebUtils.emptyToNull(photoUrl));
            p.setRegistreCommerceUrl(ProjetWebUtils.emptyToNull(registreCommerceUrl));
            p.setPatenteUrl(ProjetWebUtils.emptyToNull(patenteUrl));
            p.setMatriculeFiscalUrl(ProjetWebUtils.emptyToNull(matriculeFiscalUrl));
            p.setCarteFiscaleUrl(ProjetWebUtils.emptyToNull(carteFiscaleUrl));
            p.setSecteurs(ProjetWebUtils.parseSecteurs(secteursCsv));
            context.getProfilCrud().modifier(p);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private Window resolveOwnerWindow() {
        WebView webView = context.getWebView();
        if (webView != null && webView.getScene() != null) {
            return webView.getScene().getWindow();
        }
        return null;
    }

    private URI toUri(String input) {
        if (input == null || input.isBlank()) return null;
        String s = input.trim();
        if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file:")) {
            return URI.create(s);
        }
        return Path.of(s).toUri();
    }

    private Path toPath(String input) {
        if (input == null || input.isBlank()) return null;
        String s = input.trim();
        if (s.startsWith("file:")) return Path.of(URI.create(s));
        return Path.of(s);
    }

    private String keepIfBlank(String incoming, String existing) {
        String v = ProjetWebUtils.emptyToNull(incoming);
        return v == null ? existing : v;
    }

    private java.sql.Date dateOrExisting(String incoming, java.sql.Date existing) {
        java.sql.Date parsed = ProjetWebUtils.parseSqlDate(incoming);
        return parsed == null ? existing : parsed;
    }
}
