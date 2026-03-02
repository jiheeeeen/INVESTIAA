package Controllers;

import Entities.Investissement;
import Entities.ProfilEntrepreneur;
import Entities.Projet;
import Entities.Statut;
import Entities.User;
import Services.InvestissementCRUD;
import Services.ProjetFluxCRUD;
import Services.ProjetSuiviCRUD;
import Services.ProjetTacheCRUD;
import Services.ProfilInvestisseurCRUD;
import Services.UnipileCalendarService;
import Services.UserCRUD;
import Utils.MyBD;
import Utils.Session;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjetBridgeController {
    private static final String PDFLAYER_ENDPOINT = "https://api.pdflayer.com/api/convert";
    private static final String PDFLAYER_FALLBACK_ACCESS_KEY = "a41b941c189a66a1078333d9930fa986";

    private final ProjetWebContext context;
    private final InvestissementCRUD investissementCRUD = new InvestissementCRUD();
    private final ProjetSuiviCRUD projetSuiviCRUD = new ProjetSuiviCRUD();
    private final ProjetTacheCRUD projetTacheCRUD = new ProjetTacheCRUD();
    private final ProjetFluxCRUD projetFluxCRUD = new ProjetFluxCRUD();
    private final UnipileCalendarService unipileCalendarService = new UnipileCalendarService();
    private final ProfilInvestisseurCRUD profilInvestisseurCRUD = new ProfilInvestisseurCRUD();

    public ProjetBridgeController(ProjetWebContext context) {
        this.context = context;
    }

    public String listProjets() {
        try {
            List<Projet> list = context.getProjetCrud().afficher();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(ProjetWebUtils.toListJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        } catch (SQLException e) {
            return "[]";
        }
    }

    public String listMyProjets() {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "[]";

            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
            if (profil == null) return "[]";

            int myEntrepreneurId = profil.getIdEntrepreneur();
            List<Projet> all = context.getProjetCrud().afficher();
            List<Projet> mine = new ArrayList<>();
            for (Projet p : all) {
                if (p.getEntrepreneurId() == myEntrepreneurId) {
                    mine.add(p);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < mine.size(); i++) {
                if (i > 0) sb.append(",");
                Projet p = mine.get(i);
                try {
                    String synced = context.getProjetCrud().syncProjectStatusWithFunding(p.getIdProjet());
                    if (synced != null && !synced.isBlank()) p.setStatut(synced);
                } catch (Exception ignored) {}
                sb.append(ProjetWebUtils.toListJson(p));
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getProjetById(String id) {
        try {
            int projectId = Integer.parseInt(id);
            Projet p = context.getProjetCrud().getById(projectId);
            if (p == null) return "null";
            return ProjetWebUtils.toDetailJson(p);
        } catch (Exception e) {
            return "null";
        }
    }

    public String addProjet(String entrepreneurId,
                            String statut,
                            String titre,
                            String secteur,
                            String descriptionCourte,
                            String descriptionLongue,
                            String objectifTnd,
                            String dureeCampagneJours,
                            String modeRemboursement,
                            String tauxInteretPct,
                            String dureeRemboursementMois,
                            String margeBruteEstimeeTnd,
                            String resultatNetEstimeTnd) {
        try {
            Projet p = new Projet();
            p.setEntrepreneurId(ProjetWebUtils.parseIntRequired(entrepreneurId));
            p.setStatut(ProjetWebUtils.emptyToNull(statut));
            p.setTitre(ProjetWebUtils.emptyToNull(titre));
            p.setSecteur(ProjetWebUtils.emptyToNull(secteur));
            p.setDescriptionCourte(ProjetWebUtils.emptyToNull(descriptionCourte));
            p.setDescriptionLongue(ProjetWebUtils.emptyToNull(descriptionLongue));
            p.setObjectifTnd(ProjetWebUtils.parseBigDecimalRequired(objectifTnd));
            p.setDureeCampagneJours(ProjetWebUtils.parseIntRequired(dureeCampagneJours));
            p.setModeRemboursement(ProjetWebUtils.emptyToNull(modeRemboursement));
            p.setTauxInteretPct(ProjetWebUtils.parseBigDecimalOptional(tauxInteretPct));
            p.setDureeRemboursementMois(ProjetWebUtils.parseIntOptional(dureeRemboursementMois));
            p.setMargeBruteEstimeeTnd(ProjetWebUtils.parseBigDecimalOptional(margeBruteEstimeeTnd));
            p.setResultatNetEstimeTnd(ProjetWebUtils.parseBigDecimalOptional(resultatNetEstimeTnd));
            context.getProjetCrud().ajouter(p);
            return "OK:" + p.getIdProjet();
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
            return "ERROR:" + msg;
        }
    }

    public String updateProjet(String idProjet,
                               String entrepreneurId,
                               String statut,
                               String titre,
                               String secteur,
                               String descriptionCourte,
                               String descriptionLongue,
                               String objectifTnd,
                               String dureeCampagneJours,
                               String modeRemboursement,
                               String tauxInteretPct,
                               String dureeRemboursementMois,
                               String margeBruteEstimeeTnd,
                               String resultatNetEstimeTnd) {
        try {
            Projet p = new Projet();
            p.setIdProjet(ProjetWebUtils.parseIntRequired(idProjet));
            p.setEntrepreneurId(ProjetWebUtils.parseIntRequired(entrepreneurId));
            p.setStatut(ProjetWebUtils.emptyToNull(statut));
            p.setTitre(ProjetWebUtils.emptyToNull(titre));
            p.setSecteur(ProjetWebUtils.emptyToNull(secteur));
            p.setDescriptionCourte(ProjetWebUtils.emptyToNull(descriptionCourte));
            p.setDescriptionLongue(ProjetWebUtils.emptyToNull(descriptionLongue));
            p.setObjectifTnd(ProjetWebUtils.parseBigDecimalRequired(objectifTnd));
            p.setDureeCampagneJours(ProjetWebUtils.parseIntRequired(dureeCampagneJours));
            p.setModeRemboursement(ProjetWebUtils.emptyToNull(modeRemboursement));
            p.setTauxInteretPct(ProjetWebUtils.parseBigDecimalOptional(tauxInteretPct));
            p.setDureeRemboursementMois(ProjetWebUtils.parseIntOptional(dureeRemboursementMois));
            p.setMargeBruteEstimeeTnd(ProjetWebUtils.parseBigDecimalOptional(margeBruteEstimeeTnd));
            p.setResultatNetEstimeTnd(ProjetWebUtils.parseBigDecimalOptional(resultatNetEstimeTnd));
            context.getProjetCrud().modifier(p);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String publishProjet(String idProjet) {
        try {
            int pid = ProjetWebUtils.parseIntRequired(idProjet);
            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";

            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
            if (profil == null) return "ERROR:PROFIL_NOT_FOUND";

            Projet p = context.getProjetCrud().getById(pid);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";
            if (p.getEntrepreneurId() != profil.getIdEntrepreneur()) return "ERROR:FORBIDDEN";

            if (!"BROUILLON".equalsIgnoreCase(p.getStatut())) {
                return "ERROR:ONLY_DRAFT_CAN_BE_PUBLISHED";
            }

            context.getProjetCrud().updateStatut(pid, Statut.EN_ATTENTE);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String getInvestissementsByProjetJson(String idProjet) {
        try {
            int pid = Integer.parseInt(idProjet);
            List<Investissement> list = investissementCRUD.afficherParProjet(pid);
            UserCRUD userCRUD = context.getUserCrud();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Investissement x : list) {
                if (!first) sb.append(",");
                first = false;
                String investorName = "";
                try {
                    int userId = profilInvestisseurCRUD.getUserIdByInvestisseurId(x.getId_investisseur());
                    if (userId > 0) {
                        var u = userCRUD.findById(userId);
                        if (u != null) {
                            String nom = u.getNom() == null ? "" : u.getNom().trim();
                            String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
                            String full = (nom + " " + prenom).trim();
                            investorName = full.isEmpty() ? (u.getEmail() == null ? "" : u.getEmail().trim()) : full;
                        }
                    }
                } catch (Exception ignored) {}
                if (investorName == null || investorName.isBlank()) {
                    investorName = "Investisseur #" + x.getId_investisseur();
                }
                sb.append("{")
                        .append("\"id_investisseur\":").append(x.getId_investisseur()).append(",")
                        .append("\"montant\":").append(x.getMontant()).append(",")
                        .append("\"date_investissement\":").append(ProjetWebUtils.jsonString(x.getDate_investissement() == null ? "" : x.getDate_investissement().toString())).append(",")
                        .append("\"investisseur\":").append(ProjetWebUtils.jsonString(investorName))
                        .append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getInvestisseursContactParProjetJson() {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "[]";

            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
            if (profil == null) return "[]";

            List<Projet> myProjects = context.getProjetCrud().afficher();
            int entrepreneurId = profil.getIdEntrepreneur();
            UserCRUD userCRUD = context.getUserCrud();

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean firstProject = true;

            for (Projet p : myProjects) {
                if (p.getEntrepreneurId() != entrepreneurId) continue;

                List<Investissement> investments = investissementCRUD.afficherParProjet(p.getIdProjet());
                Map<Integer, String> contactsByInvestorId = new LinkedHashMap<>();
                Set<Integer> seenInvestorIds = new HashSet<>();

                for (Investissement inv : investments) {
                    int investorId = inv.getId_investisseur();
                    if (!seenInvestorIds.add(investorId)) continue;

                    String fullName = "";
                    String email = "";
                    String telephone = "";
                    int userId = 0;
                    try {
                        userId = profilInvestisseurCRUD.getUserIdByInvestisseurId(investorId);
                        if (userId > 0) {
                            User u = userCRUD.findById(userId);
                            if (u != null) {
                                String nom = u.getNom() == null ? "" : u.getNom().trim();
                                String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
                                fullName = (nom + " " + prenom).trim();
                                email = u.getEmail() == null ? "" : u.getEmail().trim();
                                telephone = u.getTelephone() == null ? "" : u.getTelephone().trim();
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    if (fullName.isBlank()) fullName = "Investisseur #" + investorId;
                    contactsByInvestorId.put(
                            investorId,
                            "{"
                                    + "\"id_investisseur\":" + investorId + ","
                                    + "\"id_user\":" + userId + ","
                                    + "\"nom\":" + ProjetWebUtils.jsonString(fullName) + ","
                                    + "\"email\":" + ProjetWebUtils.jsonString(email) + ","
                                    + "\"telephone\":" + ProjetWebUtils.jsonString(telephone)
                                    + "}"
                    );
                }

                if (!firstProject) sb.append(",");
                firstProject = false;
                sb.append("{")
                        .append("\"id_projet\":").append(p.getIdProjet()).append(",")
                        .append("\"titre\":").append(ProjetWebUtils.jsonString(p.getTitre())).append(",")
                        .append("\"investisseurs\":[");

                boolean firstInvestor = true;
                for (String investorJson : contactsByInvestorId.values()) {
                    if (!firstInvestor) sb.append(",");
                    firstInvestor = false;
                    sb.append(investorJson);
                }
                sb.append("]}");
            }

            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String exportCurrentPagePdfWithPdflayer(String documentUrl,
                                                   String documentHtml,
                                                   String pageSize,
                                                   String marginTop,
                                                   String marginBottom,
                                                   String marginLeft,
                                                   String marginRight) {
        try {
            String accessKey = resolvePdflayerAccessKey();
            if (accessKey.isBlank()) return "ERROR:PDFLAYER_ACCESS_KEY_MISSING";

            String size = (pageSize == null || pageSize.isBlank()) ? "A4" : pageSize.trim();
            String url = documentUrl == null ? "" : documentUrl.trim();
            String html = documentHtml == null ? "" : documentHtml.trim();

            boolean hasPublicUrl = url.startsWith("http://") || url.startsWith("https://");
            if (!hasPublicUrl && html.isBlank()) {
                return "ERROR:DOCUMENT_URL_MUST_BE_PUBLIC_OR_HTML_REQUIRED";
            }

            StringBuilder form = new StringBuilder();
            appendForm(form, "access_key", accessKey);
            appendForm(form, "page_size", size);

            if (hasPublicUrl) {
                appendForm(form, "document_url", url);
            } else {
                appendForm(form, "document_html", html);
            }

            if (marginTop != null && !marginTop.isBlank()) appendForm(form, "margin_top", marginTop.trim());
            if (marginBottom != null && !marginBottom.isBlank()) appendForm(form, "margin_bottom", marginBottom.trim());
            if (marginLeft != null && !marginLeft.isBlank()) appendForm(form, "margin_left", marginLeft.trim());
            if (marginRight != null && !marginRight.isBlank()) appendForm(form, "margin_right", marginRight.trim());

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            String endpointWithKey = PDFLAYER_ENDPOINT
                    + "?access_key="
                    + URLEncoder.encode(accessKey, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpointWithKey))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body() == null ? new byte[0] : response.body();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String msg = new String(body, StandardCharsets.UTF_8);
                if (msg.length() > 300) msg = msg.substring(0, 300);
                return "ERROR:PDFLAYER_HTTP_" + response.statusCode() + ":" + msg;
            }

            String contentType = response.headers().firstValue("content-type").orElse("");
            String bodyText = new String(body, StandardCharsets.UTF_8).trim();
            if (contentType.contains("application/json")
                    || bodyText.startsWith("{\"success\":false")
                    || bodyText.startsWith("{\"error\":")) {
                if (bodyText.length() > 500) bodyText = bodyText.substring(0, 500);
                return "ERROR:PDFLAYER_API:" + bodyText;
            }

            Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "pdfs");
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve("projet_" + System.currentTimeMillis() + ".pdf");

            Files.write(target, body);
            String absPath = target.toAbsolutePath().toString();
            boolean opened = openPdfFile(target);
            return (opened ? "OK_OPENED:" : "OK_SAVED_ONLY:") + absPath;
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String addSuiviTache(String idProjet,
                                String titre,
                                String description,
                                String dateDebut,
                                String dateFin,
                                String progressionDelta,
                                String coutTache) {
        try {
            int pid = ProjetWebUtils.parseIntRequired(idProjet);
            String t = ProjetWebUtils.emptyToNull(titre);
            if (t == null) return "ERROR:TITRE_REQUIRED";

            Projet p = context.getProjetCrud().getById(pid);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";

            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";
            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
            if (profil == null) return "ERROR:PROFIL_NOT_FOUND";
            if (profil.getIdEntrepreneur() != p.getEntrepreneurId()) return "ERROR:FORBIDDEN";

            Date dateDebutSql = (dateDebut == null || dateDebut.trim().isEmpty())
                    ? Date.valueOf(LocalDate.now())
                    : Date.valueOf(dateDebut.trim());
            Date dateFinSql = (dateFin == null || dateFin.trim().isEmpty())
                    ? dateDebutSql
                    : Date.valueOf(dateFin.trim());
            if (dateFinSql.before(dateDebutSql)) return "ERROR:DATE_FIN_AVANT_DATE_DEBUT";
            double delta = (progressionDelta == null || progressionDelta.trim().isEmpty()) ? 0 : Double.parseDouble(progressionDelta.trim());
            double cout = (coutTache == null || coutTache.trim().isEmpty()) ? 0 : Double.parseDouble(coutTache.trim());

            projetSuiviCRUD.ensureForProject(pid, p.getObjectifTnd());
            int newId = projetTacheCRUD.ajouter(pid, t, ProjetWebUtils.emptyToNull(description), dateDebutSql, dateFinSql, delta, cout, current.getId());
            projetSuiviCRUD.applyTaskUpdate(pid, delta, cout);
            return newId > 0 ? "OK:" + newId : "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String updateSuiviTache(String tacheId,
                                   String titre,
                                   String description,
                                   String dateDebut,
                                   String dateFin,
                                   String progressionDelta,
                                   String coutTache) {
        try {
            int tid = ProjetWebUtils.parseIntRequired(tacheId);
            ProjetTacheCRUD.TacheRow row = projetTacheCRUD.getById(tid);
            if (row == null) return "ERROR:TASK_NOT_FOUND";
            Projet p = context.getProjetCrud().getById(row.projetId);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";
            String auth = checkOwnership(p);
            if (auth != null) return auth;

            String t = ProjetWebUtils.emptyToNull(titre);
            if (t == null) return "ERROR:TITRE_REQUIRED";
            Date dateDebutSql = (dateDebut == null || dateDebut.trim().isEmpty()) ? Date.valueOf(LocalDate.now()) : Date.valueOf(dateDebut.trim());
            Date dateFinSql = (dateFin == null || dateFin.trim().isEmpty()) ? dateDebutSql : Date.valueOf(dateFin.trim());
            if (dateFinSql.before(dateDebutSql)) return "ERROR:DATE_FIN_AVANT_DATE_DEBUT";
            double delta = (progressionDelta == null || progressionDelta.trim().isEmpty()) ? 0 : Double.parseDouble(progressionDelta.trim());
            double cout = (coutTache == null || coutTache.trim().isEmpty()) ? 0 : Double.parseDouble(coutTache.trim());

            int n = projetTacheCRUD.updateById(tid, t, ProjetWebUtils.emptyToNull(description), dateDebutSql, dateFinSql, delta, cout);
            recomputeSuiviFromTasks(row.projetId);
            return n > 0 ? "OK" : "ERROR:UPDATE_FAILED";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String deleteSuiviTache(String tacheId) {
        try {
            int tid = ProjetWebUtils.parseIntRequired(tacheId);
            ProjetTacheCRUD.TacheRow row = projetTacheCRUD.getById(tid);
            if (row == null) return "ERROR:TASK_NOT_FOUND";
            Projet p = context.getProjetCrud().getById(row.projetId);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";
            String auth = checkOwnership(p);
            if (auth != null) return auth;
            int n = projetTacheCRUD.deleteById(tid);
            recomputeSuiviFromTasks(row.projetId);
            return n > 0 ? "OK" : "ERROR:DELETE_FAILED";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String getSuiviProjetJson(String idProjet) {
        try {
            int pid = ProjetWebUtils.parseIntRequired(idProjet);
            Projet p = context.getProjetCrud().getById(pid);
            if (p == null) return "{}";

            projetSuiviCRUD.ensureForProject(pid, p.getObjectifTnd());
            ProjetSuiviCRUD.SuiviRow suivi = projetSuiviCRUD.getByProjectId(pid);
            List<ProjetTacheCRUD.TacheRow> taches = projetTacheCRUD.listByProject(pid, 50);
            List<ProjetFluxCRUD.FluxRow> charges = projetFluxCRUD.listByProject(pid, "CHARGE", 100);
            List<ProjetFluxCRUD.FluxRow> gains = projetFluxCRUD.listByProject(pid, "GAIN", 100);

            double budgetAlloue = suivi == null || suivi.budgetAlloue == null ? 0 : suivi.budgetAlloue.doubleValue();
            double budgetConsomme = suivi == null || suivi.budgetConsomme == null ? 0 : suivi.budgetConsomme.doubleValue();
            double avancement = suivi == null ? 0 : suivi.avancementPct;

            String dateDebut = (suivi != null && suivi.dateDebutReelle != null) ? suivi.dateDebutReelle.toString() : "";
            String dateFinCible = (suivi != null && suivi.dateFinCible != null) ? suivi.dateFinCible.toString() : "";
            String updatedAt = (suivi != null && suivi.updatedAt != null) ? ProjetWebUtils.formatTimestamp(suivi.updatedAt) : "";

            long daysLeft = 0;
            if (!dateFinCible.isBlank()) {
                daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dateFinCible));
            }

            double totalConfirmed = totalConfirmedByProject(pid);
            RepayStats repay = getRepayStats(pid);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"project\":{")
                    .append("\"id\":").append(p.getIdProjet()).append(",")
                    .append("\"title\":").append(ProjetWebUtils.jsonString(p.getTitre())).append(",")
                    .append("\"sector\":").append(ProjetWebUtils.jsonString(p.getSecteur())).append(",")
                    .append("\"status\":").append(ProjetWebUtils.jsonString(p.getStatut())).append(",")
                    .append("\"startDate\":").append(ProjetWebUtils.jsonString(dateDebut)).append(",")
                    .append("\"targetEndDate\":").append(ProjetWebUtils.jsonString(dateFinCible))
                    .append("},");
            sb.append("\"execution\":{")
                    .append("\"progressPct\":").append(avancement).append(",")
                    .append("\"daysLeft\":").append(daysLeft).append(",")
                    .append("\"budgetAllocated\":").append(budgetAlloue).append(",")
                    .append("\"budgetConsumed\":").append(budgetConsomme).append(",")
                    .append("\"lastUpdate\":").append(ProjetWebUtils.jsonString(updatedAt))
                    .append("},");
            sb.append("\"finance\":{")
                    .append("\"confirmedTotal\":").append(totalConfirmed)
                    .append("},");
            sb.append("\"repayments\":{")
                    .append("\"nextDue\":").append(ProjetWebUtils.jsonString(repay.nextDue)).append(",")
                    .append("\"totalPaid\":").append(repay.totalPaid).append(",")
                    .append("\"remaining\":").append(repay.remaining).append(",")
                    .append("\"onTimeRate\":").append(repay.onTimeRate)
                    .append("},");
            sb.append("\"tasks\":[");
            for (int i = 0; i < taches.size(); i++) {
                if (i > 0) sb.append(",");
                ProjetTacheCRUD.TacheRow t = taches.get(i);
                sb.append("{")
                        .append("\"id\":").append(t.id).append(",")
                        .append("\"title\":").append(ProjetWebUtils.jsonString(t.titre)).append(",")
                        .append("\"description\":").append(ProjetWebUtils.jsonString(t.description)).append(",")
                        .append("\"date\":").append(ProjetWebUtils.jsonString(t.dateDebut == null ? (t.dateTache == null ? "" : t.dateTache.toString()) : t.dateDebut.toString())).append(",")
                        .append("\"dateStart\":").append(ProjetWebUtils.jsonString(t.dateDebut == null ? (t.dateTache == null ? "" : t.dateTache.toString()) : t.dateDebut.toString())).append(",")
                        .append("\"dateEnd\":").append(ProjetWebUtils.jsonString(t.dateFin == null ? (t.dateTache == null ? "" : t.dateTache.toString()) : t.dateFin.toString())).append(",")
                        .append("\"calendarEventId\":").append(ProjetWebUtils.jsonString(t.calendarEventId)).append(",")
                        .append("\"calendarStatus\":").append(ProjetWebUtils.jsonString(t.calendarStatus)).append(",")
                        .append("\"calendarSyncedAt\":").append(ProjetWebUtils.jsonString(t.calendarSyncedAt == null ? "" : ProjetWebUtils.formatTimestamp(t.calendarSyncedAt))).append(",")
                        .append("\"progressDelta\":").append(t.progressionDelta).append(",")
                        .append("\"cost\":").append(t.coutTache).append(",")
                        .append("\"status\":").append(ProjetWebUtils.jsonString(t.statut))
                        .append("}");
            }
            sb.append("]");
            sb.append(",\"charges\":[");
            for (int i = 0; i < charges.size(); i++) {
                if (i > 0) sb.append(",");
                ProjetFluxCRUD.FluxRow f = charges.get(i);
                sb.append("{")
                        .append("\"id\":").append(f.id).append(",")
                        .append("\"description\":").append(ProjetWebUtils.jsonString(f.description)).append(",")
                        .append("\"amount\":").append(f.montant).append(",")
                        .append("\"date\":").append(ProjetWebUtils.jsonString(f.dateFlux == null ? "" : f.dateFlux.toString()))
                        .append("}");
            }
            sb.append("],\"gains\":[");
            for (int i = 0; i < gains.size(); i++) {
                if (i > 0) sb.append(",");
                ProjetFluxCRUD.FluxRow f = gains.get(i);
                sb.append("{")
                        .append("\"id\":").append(f.id).append(",")
                        .append("\"description\":").append(ProjetWebUtils.jsonString(f.description)).append(",")
                        .append("\"amount\":").append(f.montant).append(",")
                        .append("\"date\":").append(ProjetWebUtils.jsonString(f.dateFlux == null ? "" : f.dateFlux.toString()))
                        .append("}");
            }
            double totalCharges = charges.stream().mapToDouble(x -> x.montant).sum();
            double totalGains = gains.stream().mapToDouble(x -> x.montant).sum();
            sb.append("],\"operations\":{")
                    .append("\"totalCharges\":").append(totalCharges).append(",")
                    .append("\"totalGains\":").append(totalGains).append(",")
                    .append("\"net\":").append(totalGains - totalCharges)
                    .append("}");
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public String addSuiviCharge(String idProjet, String description, String montant, String dateFlux) {
        return addSuiviFlux(idProjet, description, montant, dateFlux, "CHARGE");
    }

    public String addSuiviGain(String idProjet, String description, String montant, String dateFlux) {
        return addSuiviFlux(idProjet, description, montant, dateFlux, "GAIN");
    }

    public String updateSuiviCharge(String fluxId, String description, String montant, String dateFlux) {
        return updateSuiviFlux(fluxId, description, montant, dateFlux, "CHARGE");
    }

    public String updateSuiviGain(String fluxId, String description, String montant, String dateFlux) {
        return updateSuiviFlux(fluxId, description, montant, dateFlux, "GAIN");
    }

    public String deleteSuiviCharge(String fluxId) {
        return deleteSuiviFlux(fluxId, "CHARGE");
    }

    public String deleteSuiviGain(String fluxId) {
        return deleteSuiviFlux(fluxId, "GAIN");
    }

    private String addSuiviFlux(String idProjet, String description, String montant, String dateFlux, String typeFlux) {
        try {
            int pid = ProjetWebUtils.parseIntRequired(idProjet);
            Projet p = context.getProjetCrud().getById(pid);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";

            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";
            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
            if (profil == null) return "ERROR:PROFIL_NOT_FOUND";
            if (profil.getIdEntrepreneur() != p.getEntrepreneurId()) return "ERROR:FORBIDDEN";

            String desc = ProjetWebUtils.emptyToNull(description);
            if (desc == null) return "ERROR:DESCRIPTION_REQUIRED";
            double amount = (montant == null || montant.trim().isEmpty()) ? 0 : Double.parseDouble(montant.trim());
            if (amount < 0) return "ERROR:AMOUNT_NEGATIVE";
            Date dt = (dateFlux == null || dateFlux.trim().isEmpty()) ? Date.valueOf(LocalDate.now()) : Date.valueOf(dateFlux.trim());

            int id = projetFluxCRUD.ajouter(pid, typeFlux, desc, amount, dt, current.getId());
            return id > 0 ? "OK:" + id : "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String syncTaskToCalendar(String tacheId) {
        try {
            int tid = ProjetWebUtils.parseIntRequired(tacheId);
            ProjetTacheCRUD.TacheRow t = projetTacheCRUD.getById(tid);
            if (t == null) return "ERROR:TASK_NOT_FOUND";
            Projet p = context.getProjetCrud().getById(t.projetId);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";
            String auth = checkOwnership(p);
            if (auth != null) return auth;

            if (!unipileCalendarService.isConfigured()) return "ERROR:GOOGLE_CALENDAR_NOT_CONFIGURED";
            LocalDate start = t.dateDebut == null ? LocalDate.now() : t.dateDebut.toLocalDate();
            LocalDate end = t.dateFin == null ? start : t.dateFin.toLocalDate();
            String title = "[Projet #" + p.getIdProjet() + "] " + (t.titre == null ? "Tache" : t.titre);
            String desc = (t.description == null ? "" : t.description) + "\nProjet: " + (p.getTitre() == null ? "" : p.getTitre());
            String res = unipileCalendarService.createCalendarEventForTask(title, desc, start, end);
            if (res != null && res.startsWith("OK:")) {
                String eventId = res.substring(3);
                projetTacheCRUD.markCalendarSync(tid, eventId, "SYNCED");
                return "OK:" + eventId;
            }
            String status = "ERROR";
            if (res != null && !res.isBlank()) {
                status = res.length() > 120 ? res.substring(0, 120) : res;
            }
            projetTacheCRUD.markCalendarSync(tid, null, status);
            return res == null ? "ERROR:SYNC_FAILED" : res;
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String syncProjectTasksToCalendar(String idProjet) {
        try {
            int pid = ProjetWebUtils.parseIntRequired(idProjet);
            Projet p = context.getProjetCrud().getById(pid);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";
            String auth = checkOwnership(p);
            if (auth != null) return auth;
            if (!unipileCalendarService.isConfigured()) return "ERROR:GOOGLE_CALENDAR_NOT_CONFIGURED";
            List<ProjetTacheCRUD.TacheRow> tasks = projetTacheCRUD.listByProject(pid, 1000);
            int ok = 0;
            int ko = 0;
            for (ProjetTacheCRUD.TacheRow t : tasks) {
                String r = syncTaskToCalendar(String.valueOf(t.id));
                if (r != null && r.startsWith("OK")) ok++; else ko++;
            }
            return "OK:SYNCED=" + ok + ";FAILED=" + ko;
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String getCalendarConfigStatus() {
        return unipileCalendarService.isConfigured() ? "OK:CONFIGURED" : "ERROR:GOOGLE_CALENDAR_NOT_CONFIGURED";
    }

    public String getCalendarEmbedUrl() {
        if (!unipileCalendarService.isConfigured()) return "";
        return unipileCalendarService.getCalendarEmbedUrl();
    }

    private String updateSuiviFlux(String fluxId, String description, String montant, String dateFlux, String typeFlux) {
        try {
            int fid = ProjetWebUtils.parseIntRequired(fluxId);
            ProjetFluxCRUD.FluxRow row = projetFluxCRUD.getById(fid);
            if (row == null) return "ERROR:FLUX_NOT_FOUND";
            if (!typeFlux.equalsIgnoreCase(row.typeFlux)) return "ERROR:FLUX_TYPE_MISMATCH";
            Projet p = context.getProjetCrud().getById(row.projetId);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";
            String auth = checkOwnership(p);
            if (auth != null) return auth;

            String desc = ProjetWebUtils.emptyToNull(description);
            if (desc == null) return "ERROR:DESCRIPTION_REQUIRED";
            double amount = (montant == null || montant.trim().isEmpty()) ? 0 : Double.parseDouble(montant.trim());
            if (amount < 0) return "ERROR:AMOUNT_NEGATIVE";
            Date dt = (dateFlux == null || dateFlux.trim().isEmpty()) ? Date.valueOf(LocalDate.now()) : Date.valueOf(dateFlux.trim());
            int n = projetFluxCRUD.updateById(fid, desc, amount, dt);
            return n > 0 ? "OK" : "ERROR:UPDATE_FAILED";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private String deleteSuiviFlux(String fluxId, String typeFlux) {
        try {
            int fid = ProjetWebUtils.parseIntRequired(fluxId);
            ProjetFluxCRUD.FluxRow row = projetFluxCRUD.getById(fid);
            if (row == null) return "ERROR:FLUX_NOT_FOUND";
            if (!typeFlux.equalsIgnoreCase(row.typeFlux)) return "ERROR:FLUX_TYPE_MISMATCH";
            Projet p = context.getProjetCrud().getById(row.projetId);
            if (p == null) return "ERROR:PROJECT_NOT_FOUND";
            String auth = checkOwnership(p);
            if (auth != null) return auth;
            int n = projetFluxCRUD.deleteById(fid);
            return n > 0 ? "OK" : "ERROR:DELETE_FAILED";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private String checkOwnership(Projet p) throws SQLException {
        User current = Session.getCurrentUser();
        if (current == null) return "ERROR:USER_NOT_CONNECTED";
        ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
        if (profil == null) return "ERROR:PROFIL_NOT_FOUND";
        if (profil.getIdEntrepreneur() != p.getEntrepreneurId()) return "ERROR:FORBIDDEN";
        return null;
    }

    private void recomputeSuiviFromTasks(int projetId) throws SQLException {
        List<ProjetTacheCRUD.TacheRow> tasks = projetTacheCRUD.listByProject(projetId, 10000);
        double totalProgress = 0;
        double totalCost = 0;
        for (ProjetTacheCRUD.TacheRow t : tasks) {
            totalProgress += t.progressionDelta;
            totalCost += t.coutTache;
        }
        if (totalProgress < 0) totalProgress = 0;
        if (totalProgress > 100) totalProgress = 100;
        projetSuiviCRUD.setExecutionValues(projetId, totalProgress, totalCost);
    }

    private double totalConfirmedByProject(int projectId) {
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return 0;
        String sql = "SELECT COALESCE(SUM(montant),0) AS total FROM financement2 WHERE id_projet=? AND UPPER(COALESCE(statut,''))='CONFIRMED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private RepayStats getRepayStats(int projectId) {
        RepayStats out = new RepayStats();
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return out;
        String sql = "SELECT " +
                "COALESCE(SUM(r.montant_paye),0) AS totalPaid, " +
                "COALESCE(SUM(r.montant_du - r.montant_paye),0) AS remaining, " +
                "MIN(CASE WHEN UPPER(COALESCE(r.statut,''))='EN_ATTENTE' THEN r.date_echeance ELSE NULL END) AS nextDue, " +
                "COALESCE(SUM(CASE WHEN UPPER(COALESCE(r.statut,''))='PAYE' THEN 1 ELSE 0 END),0) AS paidCount, " +
                "COUNT(r.id) AS totalCount " +
                "FROM remboursement r JOIN financement2 f ON r.financement_id=f.id_financement WHERE f.id_projet=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.totalPaid = rs.getDouble("totalPaid");
                    out.remaining = rs.getDouble("remaining");
                    Date d = rs.getDate("nextDue");
                    out.nextDue = d == null ? "" : d.toString();
                    int paid = rs.getInt("paidCount");
                    int total = rs.getInt("totalCount");
                    out.onTimeRate = total <= 0 ? 0 : Math.round((paid * 10000.0 / total)) / 100.0;
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static class RepayStats {
        double totalPaid = 0;
        double remaining = 0;
        String nextDue = "";
        double onTimeRate = 0;
    }

    private static String resolvePdflayerAccessKey() {
        String fromEnv = System.getenv("PDFLAYER_ACCESS_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
        String fromProperty = System.getProperty("PDFLAYER_ACCESS_KEY");
        if (fromProperty != null && !fromProperty.isBlank()) return fromProperty.trim();
        return PDFLAYER_FALLBACK_ACCESS_KEY;
    }

    private static void appendForm(StringBuilder form, String key, String value) {
        if (form.length() > 0) form.append('&');
        form.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        form.append('=');
        form.append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
    }

    private static boolean openPdfFile(Path path) {
        if (path == null) return false;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop d = Desktop.getDesktop();
                if (d.isSupported(Desktop.Action.OPEN)) {
                    d.open(path.toFile());
                    return true;
                }
                if (d.isSupported(Desktop.Action.BROWSE)) {
                    d.browse(path.toUri());
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return openPdfFileWithSystemCommand(path);
    }

    private static boolean openPdfFileWithSystemCommand(Path path) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            Process process;
            if (os.contains("win")) {
                process = new ProcessBuilder("cmd", "/c", "start", "", path.toAbsolutePath().toString()).start();
            } else if (os.contains("mac")) {
                process = new ProcessBuilder("open", path.toAbsolutePath().toString()).start();
            } else {
                process = new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start();
            }
            return process != null;
        } catch (Exception ignored) {
            return false;
        }
    }

}
