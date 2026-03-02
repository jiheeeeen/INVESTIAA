package Controllers;

import Entities.Financement2;
import Entities.ProfilEntrepreneur;
import Entities.Projet;
import Entities.Remboursement;
import Services.ExchangeRateService;
import Services.Financement2CRUD;
import Services.MicrosoftGraphCalendarService;
import Services.PaymentApiService;
import Services.ProfilEntrepreneurCRUD;
import Services.ProjetCRUD;
import Services.RemboursementCRUD;
import Services.SignatureApiService;
import Services.AuditService;
import Services.NativeLocationService;
import Services.ExternalGeoCaptureService;
import Services.IpInfoLocationService;
import Utils.MyBD;
import Utils.Session;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Properties;
import java.util.Base64;
import javafx.stage.FileChooser;
import javafx.scene.web.WebEngine;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;

public class FinancementBridgeController {
    private static final Properties LOCAL_CONFIG = loadLocalConfig();
    private final WebView webView;
    private final ProjetCRUD projetCrud;
    private final ProfilEntrepreneurCRUD profilCrud;
    private final Financement2CRUD financementCrud;
    private final RemboursementCRUD remboursementCrud;
    private final ExchangeRateService exchangeRateService;
    private final PaymentApiService paymentApiService;
    private final SignatureApiService signatureApiService;
    private final MicrosoftGraphCalendarService microsoftGraphCalendarService;
    private final NativeLocationService nativeLocationService;
    private final IpInfoLocationService ipInfoLocationService;

    public FinancementBridgeController(ProjetWebContext context) {
        this.webView = context.getWebView();
        this.projetCrud = context.getProjetCrud();
        this.profilCrud = context.getProfilCrud();
        this.financementCrud = new Financement2CRUD();
        this.remboursementCrud = new RemboursementCRUD();
        this.exchangeRateService = new ExchangeRateService();
        this.paymentApiService = new PaymentApiService();
        this.signatureApiService = new SignatureApiService();
        this.microsoftGraphCalendarService = new MicrosoftGraphCalendarService();
        this.nativeLocationService = new NativeLocationService();
        this.ipInfoLocationService = new IpInfoLocationService();
        try {
            ensureSignatureReceiptTables();
            ensureEntrepreneurWorkspaceTables();
            AuditService.ensureAuditTable();
        } catch (Exception ignored) {
        }
    }

    public String getTndToEurUsdRatesJson() {
        try {
            return exchangeRateService.getTndToEurUsdRatesJson();
        } catch (Exception e) {
            String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
            return "{\"error\":true,\"message\":\"" + msg + "\"}";
        }
    }

    public String getNativeLocationJson() {
        try {
            return nativeLocationService.getNativeLocationJson();
        } catch (Exception e) {
            String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
            return "{\"ok\":false,\"message\":\"" + msg + "\"}";
        }
    }

    public String getIpInfoLocationJson() {
        try {
            String token = toStringOrEmpty(LOCAL_CONFIG.getProperty("IPINFO_TOKEN"));
            IpInfoLocationService.Result r = ipInfoLocationService.locate(token);
            return "{"
                    + "\"ok\":" + (r.ok ? "true" : "false")
                    + ",\"lat\":" + r.lat
                    + ",\"lon\":" + r.lon
                    + ",\"city\":" + ProjetWebUtils.jsonString(r.city)
                    + ",\"region\":" + ProjetWebUtils.jsonString(r.region)
                    + ",\"country\":" + ProjetWebUtils.jsonString(r.country)
                    + ",\"ip\":" + ProjetWebUtils.jsonString(r.ip)
                    + ",\"source\":\"ipinfo\""
                    + ",\"message\":" + ProjetWebUtils.jsonString(r.message)
                    + "}";
        } catch (Exception e) {
            return "{\"ok\":false,\"message\":" + ProjetWebUtils.jsonString(toStringOrEmpty(e.getMessage())) + "}";
        }
    }

    public String startExternalGeoCaptureFromJs() {
        try {
            ExternalGeoCaptureService.SessionStart s = ExternalGeoCaptureService.startSession();
            return "{"
                    + "\"ok\":" + (s.ok ? "true" : "false")
                    + ",\"token\":" + ProjetWebUtils.jsonString(s.token)
                    + ",\"url\":" + ProjetWebUtils.jsonString(s.url)
                    + ",\"message\":" + ProjetWebUtils.jsonString(s.message)
                    + "}";
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    public String getExternalGeoCaptureStatusFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            String token = toStringOrEmpty(params.get("token"));
            ExternalGeoCaptureService.Status st = ExternalGeoCaptureService.getStatus(token);
            return "{"
                    + "\"ok\":" + (st.ok ? "true" : "false")
                    + ",\"token\":" + ProjetWebUtils.jsonString(st.token)
                    + ",\"state\":" + ProjetWebUtils.jsonString(st.state)
                    + ",\"lat\":" + st.lat
                    + ",\"lon\":" + st.lon
                    + ",\"updatedAt\":" + st.updatedAt
                    + ",\"message\":" + ProjetWebUtils.jsonString(st.message)
                    + "}";
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    public String getProjectsJson() {
        try {
            List<Projet> list = projetCrud.afficher();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                Projet p = list.get(i);
                sb.append("{");
                sb.append("\"id\":").append(p.getIdProjet()).append(",");
                sb.append("\"name\":").append(ProjetWebUtils.jsonString(p.getTitre()));
                sb.append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getMyProjectsJson() {
        try {
            var current = Session.getCurrentUser();
            if (current == null) return "[]";
            List<Integer> ownerIds = getCurrentEntrepreneurOwnerIds();
            if (ownerIds.isEmpty()) return "[]";

            List<Projet> list = projetCrud.afficher();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Projet p : list) {
                if (!ownerIds.contains(p.getEntrepreneurId())) continue;
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":").append(p.getIdProjet()).append(",\"name\":").append(ProjetWebUtils.jsonString(p.getTitre())).append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getProjectDashboardJson(int projectId) {
        try {
            Projet p = projetCrud.getById(projectId);
            if (p == null) return "{}";
            double objective = p.getObjectifTnd() == null ? 0.0 : p.getObjectifTnd().doubleValue();
            double raised = financementCrud.totalConfirmeParProjet(projectId);
            double remaining = objective > 0 ? Math.max(0.0, objective - raised) : 0.0;
            int progressPct = (objective > 0.0) ? (int) Math.round((raised / objective) * 100.0) : 0;
            if (progressPct < 0) progressPct = 0;
            if (progressPct > 100) progressPct = 100;

            return "{"
                    + "\"id\":" + p.getIdProjet()
                    + ",\"name\":" + ProjetWebUtils.jsonString(p.getTitre())
                    + ",\"objective\":" + objective
                    + ",\"raised\":" + raised
                    + ",\"remaining\":" + remaining
                    + ",\"progressPct\":" + progressPct
                    + "}";
        } catch (Exception e) {
            return "{}";
        }
    }

    public String getFinancementsByProjectJson(int projectId) {
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return "[]";
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT id_financement AS id, id_investissement AS investId FROM financement2 WHERE id_projet=? ORDER BY id_financement DESC")) {
            pst.setInt(1, projectId);
            List<Integer> ids = new ArrayList<>();
            List<Integer> investIds = new ArrayList<>();
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                    investIds.add(rs.getInt("investId"));
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"id\":").append(ids.get(i)).append(",\"investId\":").append(investIds.get(i)).append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getInvestissementsJson() {
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return "[]";
        try {
            ColumnPair cols = detectInvestissementColumns(conn);
            if (cols == null) return "[]";

            String sql = "SELECT " + cols.idCol + " AS id, " + cols.investorCol + " AS investorId FROM investissement ORDER BY " + cols.idCol + " DESC";
            List<InvestissementItem> items = new ArrayList<>();
            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    items.add(new InvestissementItem(rs.getInt("id"), rs.getInt("investorId")));
                }
            }

            Map<Integer, String> nameCache = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) sb.append(",");
                InvestissementItem it = items.get(i);
                String investorName = resolveInvestorDisplayName(conn, it.investorId, nameCache);
                sb.append("{\"id\":").append(it.id)
                        .append(",\"investorId\":").append(it.investorId)
                        .append(",\"investorName\":").append(ProjetWebUtils.jsonString(investorName))
                        .append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    private String resolveInvestorDisplayName(Connection conn, int investorId, Map<Integer, String> cache) {
        if (investorId <= 0) return "";
        if (cache != null && cache.containsKey(investorId)) {
            return toStringOrEmpty(cache.get(investorId));
        }
        String name = "";
        // Case 1: investissement stores direct user id
        name = queryUserNameByUserId(conn, investorId);
        if (name.isEmpty()) {
            // Case 2: investissement stores investisseur id -> investisseur.id_user -> user
            name = queryUserNameByInvestisseurId(conn, investorId);
        }
        if (name.isEmpty()) {
            // Case 3: fallback fields directly on investisseur table
            name = queryInvestisseurName(conn, investorId);
        }
        if (name.isEmpty()) name = "Investisseur";
        if (cache != null) cache.put(investorId, name);
        return name;
    }

    private String queryUserNameByUserId(Connection conn, int userId) {
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT nom, prenom, email FROM user WHERE id=? LIMIT 1")) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String full = (toStringOrEmpty(rs.getString("nom")) + " " + toStringOrEmpty(rs.getString("prenom"))).trim();
                    if (!full.isEmpty()) return full;
                    String nom = toStringOrEmpty(rs.getString("nom"));
                    if (!nom.isEmpty()) return nom;
                    String mail = toStringOrEmpty(rs.getString("email"));
                    if (!mail.isEmpty()) return mail;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String queryUserNameByInvestisseurId(Connection conn, int investisseurId) {
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT u.nom, u.prenom, u.email " +
                        "FROM investisseur iv LEFT JOIN user u ON u.id = iv.id_user " +
                        "WHERE iv.id_investisseur=? LIMIT 1")) {
            pst.setInt(1, investisseurId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String full = (toStringOrEmpty(rs.getString("nom")) + " " + toStringOrEmpty(rs.getString("prenom"))).trim();
                    if (!full.isEmpty()) return full;
                    String nom = toStringOrEmpty(rs.getString("nom"));
                    if (!nom.isEmpty()) return nom;
                    String mail = toStringOrEmpty(rs.getString("email"));
                    if (!mail.isEmpty()) return mail;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String queryInvestisseurName(Connection conn, int investisseurId) {
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT nom, prenom, email FROM investisseur WHERE id_investisseur=? LIMIT 1")) {
            pst.setInt(1, investisseurId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String full = (toStringOrEmpty(rs.getString("nom")) + " " + toStringOrEmpty(rs.getString("prenom"))).trim();
                    if (!full.isEmpty()) return full;
                    String nom = toStringOrEmpty(rs.getString("nom"));
                    if (!nom.isEmpty()) return nom;
                    String mail = toStringOrEmpty(rs.getString("email"));
                    if (!mail.isEmpty()) return mail;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public String getCurrentInvestorInvestissementsByProjectJson() {
        var current = Session.getCurrentUser();
        if (current == null) return "[]";
        if (current.getRole() == null || !"INVESTISSEUR".equals(current.getRole().name())) return "[]";

        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return "[]";
        try {
            ColumnPair cols = detectInvestissementColumns(conn);
            if (cols == null || cols.projectCol == null || cols.montantCol == null) return "[]";

            List<Integer> investorKeys = resolveInvestorLookupValues(conn, cols, current.getId());
            if (investorKeys.isEmpty()) return "[]";
            String inClause = buildInClause(investorKeys.size());

            String dateOrder = (cols.dateCol == null || cols.dateCol.trim().isEmpty())
                    ? ""
                    : "i." + cols.dateCol + " DESC, ";
            String sql =
                    "SELECT i." + cols.idCol + " AS investId, i." + cols.projectCol + " AS projectId, i." + cols.montantCol + " AS amount, " +
                            "p.titre AS projectName " +
                            "FROM investissement i " +
                            "LEFT JOIN projet p ON p.id_projet = i." + cols.projectCol + " " +
                            "WHERE i." + cols.investorCol + " IN (" + inClause + ") " +
                            "ORDER BY " + dateOrder + "i." + cols.idCol + " DESC";

            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Integer key : investorKeys) {
                    pst.setInt(idx++, key);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append("{")
                                .append("\"projectId\":").append(rs.getInt("projectId")).append(",")
                                .append("\"projectName\":").append(ProjetWebUtils.jsonString(rs.getString("projectName"))).append(",")
                                .append("\"investId\":").append(rs.getInt("investId")).append(",")
                                .append("\"amount\":").append(rs.getDouble("amount"))
                                .append("}");
                    }
                }
                sb.append("]");
                return sb.toString();
            }
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getFinancementRiskCheckJson(int projectId, int investId, double amount) {
        var current = Session.getCurrentUser();
        if (current == null) return "{\"ok\":false,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"USER_NOT_CONNECTED\",\"message\":\"Utilisateur non connecte.\"}";
        if (current.getRole() == null || !"INVESTISSEUR".equals(current.getRole().name())) {
            return "{\"ok\":false,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"ROLE_FORBIDDEN\",\"message\":\"Action reservee a l'investisseur.\"}";
        }
        if (projectId <= 0 || investId <= 0 || amount <= 0) {
            return "{\"ok\":true,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"INVALID_INPUT\",\"message\":\"Donnees invalides.\"}";
        }

        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return "{\"ok\":false,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"DB_UNAVAILABLE\",\"message\":\"Connexion base indisponible.\"}";
        try {
            ColumnPair cols = detectInvestissementColumns(conn);
            if (cols == null || cols.projectCol == null) {
                return "{\"ok\":true,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"INVEST_SCHEMA_INVALID\",\"message\":\"Schema investissement invalide.\"}";
            }

            List<Integer> investorKeys = resolveInvestorLookupValues(conn, cols, current.getId());
            if (investorKeys.isEmpty()) {
                return "{\"ok\":true,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"INVESTOR_NOT_FOUND\",\"message\":\"Profil investisseur introuvable.\"}";
            }

            String inClause = buildInClause(investorKeys.size());
            String sql = "SELECT " + cols.projectCol + " AS projectId FROM investissement WHERE " + cols.idCol + "=? AND " + cols.investorCol + " IN (" + inClause + ") LIMIT 1";
            boolean ownerOk = false;
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, investId);
                int idx = 2;
                for (Integer key : investorKeys) pst.setInt(idx++, key);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        ownerOk = (rs.getInt("projectId") == projectId);
                    }
                }
            }
            if (!ownerOk) {
                return "{\"ok\":true,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"INVEST_NOT_OWNED\",\"message\":\"Investissement non autorise pour ce projet.\"}";
            }

            if (financementCrud.existsByInvestissementId(investId)) {
                return "{\"ok\":true,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"DUPLICATE_FINANCEMENT\",\"message\":\"Ce financement existe deja pour cet investissement.\"}";
            }

            Projet p = projetCrud.getById(projectId);
            if (p == null) {
                return "{\"ok\":true,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"PROJECT_NOT_FOUND\",\"message\":\"Projet introuvable.\"}";
            }

            double objective = p.getObjectifTnd() == null ? 0.0 : p.getObjectifTnd().doubleValue();
            double raised = financementCrud.totalConfirmeParProjet(projectId);
            if (objective > 0.0 && raised + amount > objective * 1.20) {
                return "{\"ok\":true,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"OVER_OBJECTIVE\",\"message\":\"Montant depasse trop l'objectif du projet.\"}";
            }

            String level = "LOW";
            String decision = "ALLOW";
            String message = "Risque faible.";
            if (objective > 0.0) {
                double ratio = amount / objective;
                if (ratio >= 0.40) {
                    level = "MEDIUM";
                    decision = "REVIEW";
                    message = "Risque moyen: enregistrer en EN_ATTENTE recommande.";
                }
                if (ratio >= 0.70) {
                    level = "HIGH";
                    decision = "BLOCK";
                    message = "Risque eleve: montant trop grand par rapport a l'objectif.";
                }
            }

            boolean canCreate = !"BLOCK".equals(decision);
            return "{"
                    + "\"ok\":true"
                    + ",\"canCreate\":" + (canCreate ? "true" : "false")
                    + ",\"decision\":" + ProjetWebUtils.jsonString(decision)
                    + ",\"level\":" + ProjetWebUtils.jsonString(level)
                    + ",\"message\":" + ProjetWebUtils.jsonString(message)
                    + "}";
        } catch (Exception e) {
            return "{\"ok\":false,\"canCreate\":false,\"decision\":\"BLOCK\",\"code\":\"RISK_CHECK_ERROR\",\"message\":" + ProjetWebUtils.jsonString(e.getMessage()) + "}";
        }
    }

    public String getSmartRepaymentPlanJson(double amount, double ratePct) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return "{\"ok\":false,\"message\":\"Montant invalide.\"}";
        }
        if (!Double.isFinite(ratePct) || ratePct < 0) {
            ratePct = 0.0;
        }

        int[] durations = new int[] {12, 18, 24};
        double total = amount * (1.0 + (ratePct / 100.0));
        if (!Double.isFinite(total) || total <= 0) total = amount;

        StringBuilder alts = new StringBuilder();
        alts.append("[");
        int bestMonths = durations[0];
        double bestMonthly = round2(total / bestMonths);
        for (int i = 0; i < durations.length; i++) {
            int months = durations[i];
            double monthly = round2(total / months);
            String pressure = monthly > amount * 0.10 ? "HIGH" : (monthly > amount * 0.06 ? "MEDIUM" : "LOW");
            if (i > 0) alts.append(",");
            alts.append("{")
                    .append("\"durationMonths\":").append(months).append(",")
                    .append("\"monthlyDue\":").append(monthly).append(",")
                    .append("\"pressureLevel\":").append(ProjetWebUtils.jsonString(pressure))
                    .append("}");
            if ("MEDIUM".equals(pressure)) {
                bestMonths = months;
                bestMonthly = monthly;
            }
        }
        alts.append("]");

        double totalToRepay = round2(total);
        return "{"
                + "\"ok\":true"
                + ",\"recommended\":{"
                + "\"durationMonths\":" + bestMonths
                + ",\"monthlyDue\":" + bestMonthly
                + ",\"totalToRepay\":" + totalToRepay
                + "}"
                + ",\"alternatives\":" + alts
                + "}";
    }

    public String getFinancementsByProjectWithRembStatusJson(int projectId) {
        if (!isProjectOwnedByCurrentEntrepreneur(projectId)) {
            return "[]";
        }
        ensureSchedulesForProject(projectId);

        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return "[]";

        String sql =
                "SELECT f.id_financement AS id, f.id_investissement AS investId, f.montant AS amount, f.statut AS finStatus, " +
                        "COALESCE((" +
                        "  SELECT r2.id FROM remboursement r2 " +
                        "  WHERE r2.financement_id = f.id_financement AND r2.statut='EN_ATTENTE' " +
                        "  ORDER BY r2.date_echeance ASC, r2.id ASC LIMIT 1" +
                        "), (" +
                        "  SELECT r3.id FROM remboursement r3 " +
                        "  WHERE r3.financement_id = f.id_financement " +
                        "  ORDER BY r3.date_echeance DESC, r3.id DESC LIMIT 1" +
                        "), 0) AS detailRembId, " +
                        "COALESCE((" +
                        "  SELECT r4.statut FROM remboursement r4 " +
                        "  WHERE r4.financement_id = f.id_financement " +
                        "  ORDER BY r4.date_echeance DESC, r4.id DESC LIMIT 1" +
                        "), '') AS latestRembStatus, " +
                        "COUNT(r.id) AS remCount, " +
                        "COALESCE(SUM(CASE WHEN r.statut='EN_ATTENTE' THEN 1 ELSE 0 END),0) AS pendingCount, " +
                        "MIN(CASE WHEN r.statut='EN_ATTENTE' THEN r.date_echeance ELSE NULL END) AS nextDue " +
                        "FROM financement2 f " +
                        "LEFT JOIN remboursement r ON r.financement_id = f.id_financement " +
                        "WHERE f.id_projet=? AND UPPER(COALESCE(f.statut,'')) NOT IN ('ANNULE','REFUSE') " +
                        "GROUP BY f.id_financement, f.id_investissement, f.montant, f.statut " +
                        "ORDER BY f.id_financement DESC";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, projectId);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    int pending = rs.getInt("pendingCount");
                    Date nextDue = null;
                    try {
                        nextDue = rs.getDate("nextDue");
                    } catch (Exception ignored) {
                    }
                    String next = nextDue == null ? "" : nextDue.toString();
                    int remCount = rs.getInt("remCount");
                    String finStatus = rs.getString("finStatus");
                    String latestRembStatus = rs.getString("latestRembStatus");
                    String rembStatus;
                    if (remCount <= 0) {
                        // No echeances yet -> still waiting to be paid.
                        rembStatus = "EN_ATTENTE";
                    } else if (pending > 0) {
                        rembStatus = "EN_ATTENTE";
                    } else if ("PAYE".equalsIgnoreCase(latestRembStatus)) {
                        rembStatus = "PAYE";
                    } else {
                        rembStatus = "EN_ATTENTE";
                    }
                    sb.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"investId\":").append(rs.getInt("investId")).append(",")
                            .append("\"amount\":").append(rs.getDouble("amount")).append(",")
                            .append("\"detailRembId\":").append(rs.getInt("detailRembId")).append(",")
                            .append("\"finStatus\":").append(ProjetWebUtils.jsonString(finStatus)).append(",")
                            .append("\"rembStatus\":").append(ProjetWebUtils.jsonString(rembStatus)).append(",")
                            .append("\"pendingCount\":").append(pending).append(",")
                            .append("\"nextDue\":").append(ProjetWebUtils.jsonString(next))
                            .append("}");
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getFinancementsJson() {
        try {
            List<Financement2> list = financementCrud.afficher();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(financementToJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getFinancementsForCurrentInvestorJson() {
        var current = Session.getCurrentUser();
        if (current == null) return "[]";
        if (current.getRole() == null || !"INVESTISSEUR".equals(current.getRole().name())) return "[]";

        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return "[]";
        try {
            ColumnPair cols = detectInvestissementColumns(conn);
            if (cols == null) return "[]";
            List<Integer> investorKeys = resolveInvestorLookupValues(conn, cols, current.getId());
            if (investorKeys.isEmpty()) return "[]";
            String inClause = buildInClause(investorKeys.size());

            String sql =
                    "SELECT f.id_financement, f.id_projet, p.titre AS projectName, f.id_investissement, " +
                            "f.montant, f.statut, f.mode_paiement, f.created_at, f.updated_at, " +
                            "COUNT(r.id) AS remCount, " +
                            "COALESCE(SUM(CASE WHEN r.statut='EN_ATTENTE' THEN 1 ELSE 0 END),0) AS pendingCount " +
                            "FROM financement2 f " +
                            "JOIN investissement i ON f.id_investissement = i." + cols.idCol + " " +
                            "LEFT JOIN projet p ON f.id_projet = p.id_projet " +
                            "LEFT JOIN remboursement r ON r.financement_id = f.id_financement " +
                            "WHERE i." + cols.investorCol + " IN (" + inClause + ") AND UPPER(COALESCE(f.statut,'')) NOT IN ('ANNULE','REFUSE') " +
                            "GROUP BY f.id_financement, f.id_projet, p.titre, f.id_investissement, f.montant, f.statut, f.mode_paiement, f.created_at, f.updated_at " +
                            "ORDER BY f.id_financement DESC";

            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Integer key : investorKeys) {
                    pst.setInt(idx++, key);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;

                        String created = "";
                        try {
                            Timestamp c = rs.getTimestamp("created_at");
                            if (c != null) created = ProjetWebUtils.formatTimestamp(c);
                        } catch (Exception ignored) {
                        }
                        if (created.isEmpty()) {
                            try {
                                Timestamp u = rs.getTimestamp("updated_at");
                                if (u != null) created = ProjetWebUtils.formatTimestamp(u);
                            } catch (Exception ignored) {
                            }
                        }

                        int remCount = rs.getInt("remCount");
                        int pendingCount = rs.getInt("pendingCount");
                        String finStatus = normalizeFinancementStatus(rs.getString("statut"));
                        String rembStatus;
                        if ("EN_ATTENTE".equalsIgnoreCase(finStatus) || remCount <= 0 || pendingCount > 0) {
                            rembStatus = "EN_ATTENTE";
                        } else {
                            rembStatus = "PAYE";
                        }

                        sb.append("{")
                                .append("\"id\":").append(rs.getInt("id_financement")).append(",")
                                .append("\"projectId\":").append(rs.getInt("id_projet")).append(",")
                                .append("\"projectName\":").append(ProjetWebUtils.jsonString(rs.getString("projectName"))).append(",")
                                .append("\"investId\":").append(rs.getInt("id_investissement")).append(",")
                                .append("\"amount\":").append(rs.getDouble("montant")).append(",")
                                .append("\"status\":").append(ProjetWebUtils.jsonString(finStatus)).append(",")
                                .append("\"rembStatus\":").append(ProjetWebUtils.jsonString(rembStatus)).append(",")
                                .append("\"method\":").append(ProjetWebUtils.jsonString(rs.getString("mode_paiement"))).append(",")
                                .append("\"date\":").append(ProjetWebUtils.jsonString(created))
                                .append("}");
                    }
                }
                sb.append("]");
                return sb.toString();
            }
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getRemboursementsByProjectJson(int projectId) {
        // Entrepreneur screen: only allow listing for the connected entrepreneur's projects.
        if (!isProjectOwnedByCurrentEntrepreneur(projectId)) {
            return "[]";
        }

        // Fill the list "from financement2": ensure schedules exist for confirmed financements of this project.
        ensureSchedulesForProject(projectId);

        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return "[]";
        String sql =
                "SELECT r.id, r.financement_id, r.date_echeance, r.montant_du, r.montant_paye, r.statut " +
                        "FROM remboursement r JOIN financement2 f ON r.financement_id = f.id_financement " +
                        "WHERE f.id_projet=? ORDER BY r.date_echeance DESC, r.id DESC";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, projectId);
            List<Remboursement> list = new ArrayList<>();
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Remboursement r = new Remboursement();
                    r.setId(rs.getInt("id"));
                    r.setFinancementId(rs.getInt("financement_id"));
                    r.setDateEcheance(rs.getDate("date_echeance"));
                    r.setMontantDu(rs.getDouble("montant_du"));
                    r.setMontantPaye(rs.getDouble("montant_paye"));
                    r.setStatut(rs.getString("statut"));
                    list.add(r);
                }
            }
            return remboursementsToJson(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getRemboursementsForCurrentInvestorJson() {
        var current = Session.getCurrentUser();
        if (current == null) return "[]";
        if (current.getRole() == null || !"INVESTISSEUR".equals(current.getRole().name())) return "[]";

        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return "[]";
        try {
            ColumnPair cols = detectInvestissementColumns(conn);
            if (cols == null) return "[]";
            List<Integer> investorKeys = resolveInvestorLookupValues(conn, cols, current.getId());
            if (investorKeys.isEmpty()) return "[]";
            String inClause = buildInClause(investorKeys.size());

            // Ensure schedules exist for the connected investor's confirmed financements.
            ensureSchedulesForInvestor(investorKeys, cols);

            String sql =
                    "SELECT r.id, r.financement_id, r.date_echeance, r.montant_du, r.montant_paye, r.statut, " +
                            "f.id_projet AS projectId, p.titre AS projectName " +
                            "FROM remboursement r " +
                            "JOIN financement2 f ON r.financement_id = f.id_financement " +
                            "JOIN investissement i ON f.id_investissement = i." + cols.idCol + " " +
                            "JOIN projet p ON f.id_projet = p.id_projet " +
                            "WHERE i." + cols.investorCol + " IN (" + inClause + ") " +
                            "ORDER BY r.date_echeance DESC, r.id DESC";

            List<Map<String, Object>> rows = new ArrayList<>();
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Integer key : investorKeys) {
                    pst.setInt(idx++, key);
                }
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", rs.getInt("id"));
                        row.put("financementId", rs.getInt("financement_id"));
                        row.put("dateEcheance", rs.getDate("date_echeance"));
                        row.put("montantDu", rs.getDouble("montant_du"));
                        row.put("montantPaye", rs.getDouble("montant_paye"));
                        row.put("statut", rs.getString("statut"));
                        row.put("projectId", rs.getInt("projectId"));
                        row.put("projectName", rs.getString("projectName"));
                        rows.add(row);
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < rows.size(); i++) {
                if (i > 0) sb.append(",");
                Map<String, Object> r = rows.get(i);
                String date = "";
                Object d = r.get("dateEcheance");
                if (d instanceof Date) date = ((Date) d).toString();
                sb.append("{")
                        .append("\"id\":").append(r.get("id")).append(",")
                        .append("\"financementId\":").append(r.get("financementId")).append(",")
                        .append("\"projectId\":").append(r.get("projectId")).append(",")
                        .append("\"projectName\":").append(ProjetWebUtils.jsonString((String) r.get("projectName"))).append(",")
                        .append("\"dateEcheance\":").append(ProjetWebUtils.jsonString(date)).append(",")
                        .append("\"montantDu\":").append(r.get("montantDu")).append(",")
                        .append("\"montantPaye\":").append(r.get("montantPaye")).append(",")
                        .append("\"statut\":").append(ProjetWebUtils.jsonString((String) r.get("statut")))
                        .append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getFinancementByIdJson(int id) {
        try {
            Financement2 f = financementCrud.getById(id);
            if (f == null) return "{}";
            return financementToJson(f);
        } catch (Exception e) {
            return "{}";
        }
    }

    public String createFinancementFromJs(String payload) {
        String result;
        try {
            Map<String, String> params = parseFormData(payload);
            int projectId = toInt(params.get("projectId"));
            int investissementId = toInt(params.get("investId"));
            double montant = toDouble(params.get("amount"));
            double fraisPct = toDouble(params.get("feesPct"));
            String mode = toStringOrEmpty(params.get("method"));
            String statut = normalizeFinancementStatus(params.get("status"));
            double taux = toDouble(params.get("rate"));
            int duree = toInt(params.get("duration"));
            String note = params.get("note");

            if (investissementId > 0 && financementCrud.existsByInvestissementId(investissementId)) {
                result = err("Ce financement existe deja pour cet investissement.");
                sendResultToJs("onCreateResult", result);
                return result;
            }

            Financement2 f = new Financement2(projectId, investissementId, montant, fraisPct, mode, statut, taux, duree, note);
            int newId = financementCrud.ajouterWithGeneratedId(f);
            if ("CONFIRMED".equalsIgnoreCase(statut) && newId > 0) {
                ensureMonthlyEcheancierExists(newId, montant, taux, duree);
            }
            AuditService.logCurrentUser("FINANCEMENT", "CREATE_FINANCEMENT",
                    "id=" + newId + ", projectId=" + projectId + ", amount=" + round2(montant), "INFO");
            result = ok();
        } catch (Throwable e) {
            AuditService.logCurrentUser("FINANCEMENT", "CREATE_FINANCEMENT_FAILED", e.getMessage(), "WARN");
            result = err(e.getMessage());
        }
        sendResultToJs("onCreateResult", result);
        return result;
    }

    public String updateFinancementFromJs(String payload) {
        String result;
        try {
            Map<String, String> params = parseFormData(payload);
            int id = toInt(params.get("id"));
            int projectId = toInt(params.get("projectId"));
            int investissementId = toInt(params.get("investId"));
            double montant = toDouble(params.get("amount"));
            double fraisPct = toDouble(params.get("feesPct"));
            String mode = toStringOrEmpty(params.get("method"));
            String statut = normalizeFinancementStatus(params.get("status"));
            double taux = toDouble(params.get("rate"));
            int duree = toInt(params.get("duration"));
            String note = params.get("note");

            if (investissementId > 0 && financementCrud.existsByInvestissementIdExcluding(investissementId, id)) {
                result = err("Un autre financement existe deja pour cet investissement.");
                sendResultToJs("onUpdateResult", result);
                return result;
            }

            Financement2 f = new Financement2(id, projectId, investissementId, montant, fraisPct, mode, statut, taux, duree, note);
            financementCrud.modifier(f);
            if ("CONFIRMED".equalsIgnoreCase(statut) && id > 0) {
                ensureMonthlyEcheancierExists(id, montant, taux, duree);
            }
            AuditService.logCurrentUser("FINANCEMENT", "UPDATE_FINANCEMENT",
                    "id=" + id + ", projectId=" + projectId + ", amount=" + round2(montant), "INFO");
            result = ok();
        } catch (Throwable e) {
            AuditService.logCurrentUser("FINANCEMENT", "UPDATE_FINANCEMENT_FAILED", e.getMessage(), "WARN");
            result = err(e.getMessage());
        }
        sendResultToJs("onUpdateResult", result);
        return result;
    }

    public String deleteFinancementFromJs(String payload) {
        String result;
        try {
            Map<String, String> params = parseFormData(payload);
            int id = toInt(params.get("id"));
            financementCrud.supprimer(id);
            AuditService.logCurrentUser("FINANCEMENT", "DELETE_FINANCEMENT", "id=" + id, "WARN");
            result = ok();
        } catch (Throwable e) {
            AuditService.logCurrentUser("FINANCEMENT", "DELETE_FINANCEMENT_FAILED", e.getMessage(), "WARN");
            result = err(e.getMessage());
        }
        sendResultToJs("onDeleteResult", result);
        return result;
    }

    public String deleteFinancement(int id) {
        String result;
        try {
            financementCrud.supprimer(id);
            result = ok();
        } catch (Throwable e) {
            result = err(e.getMessage());
        }
        sendResultToJs("onDeleteResult", result);
        return result;
    }

    public String getRemboursementsJson() {
        try {
            List<Remboursement> list = remboursementCrud.afficher();
            return remboursementsToJson(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getRemboursementsByFinancementJson(int financementId) {
        try {
            int fkFinancementId = resolveRemboursementFinancementId(financementId);
            List<Remboursement> list = remboursementCrud.afficherParFinancement(fkFinancementId);
            return remboursementsToJson(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getRemboursementByIdJson(int id) {
        try {
            Remboursement r = remboursementCrud.getById(id);
            if (r == null) return "{}";
            return remboursementToJson(r);
        } catch (Exception e) {
            return "{}";
        }
    }

    public String createRemboursementFromJs(String payload) {
        String result;
        try {
            Map<String, String> params = parseFormData(payload);
            int financementId = toInt(params.get("financementId"));
            int fkFinancementId = resolveRemboursementFinancementId(financementId);
            Date dateEcheance = ProjetWebUtils.parseSqlDate(params.get("dateEcheance"));
            double montantDu = toDouble(params.get("montantDu"));
            double montantPaye = toDouble(params.get("montantPaye"));
            String statut = normalizeRemboursementStatus(params.get("statut"), montantDu, montantPaye);

            Remboursement r = new Remboursement(fkFinancementId, dateEcheance, montantDu, montantPaye, statut);
            remboursementCrud.ajouterOuModifierParFinancementEtDate(r);
            AuditService.logCurrentUser("REMBOURSEMENT", "CREATE_REMBOURSEMENT",
                    "financementId=" + fkFinancementId + ", date=" + dateEcheance + ", montantDu=" + round2(montantDu), "INFO");
            result = ok();
        } catch (Throwable e) {
            AuditService.logCurrentUser("REMBOURSEMENT", "CREATE_REMBOURSEMENT_FAILED", e.getMessage(), "WARN");
            result = err(e.getMessage());
        }
        sendResultToJs("onCreateResult", result);
        return result;
    }

    public String updateRemboursementFromJs(String payload) {
        String result;
        try {
            Map<String, String> params = parseFormData(payload);
            int id = toInt(params.get("id"));
            int financementId = toInt(params.get("financementId"));
            Date dateEcheance = ProjetWebUtils.parseSqlDate(params.get("dateEcheance"));
            double montantDu = toDouble(params.get("montantDu"));
            double montantPaye = toDouble(params.get("montantPaye"));
            String statut = normalizeRemboursementStatus(params.get("statut"), montantDu, montantPaye);

            Remboursement r = new Remboursement(id, financementId, dateEcheance, montantDu, montantPaye, statut);
            remboursementCrud.modifier(r);
            AuditService.logCurrentUser("REMBOURSEMENT", "UPDATE_REMBOURSEMENT",
                    "id=" + id + ", financementId=" + financementId + ", montantPaye=" + round2(montantPaye), "INFO");
            result = ok();
        } catch (Throwable e) {
            AuditService.logCurrentUser("REMBOURSEMENT", "UPDATE_REMBOURSEMENT_FAILED", e.getMessage(), "WARN");
            result = err(e.getMessage());
        }
        sendResultToJs("onUpdateResult", result);
        return result;
    }

    public String deleteRemboursementFromJs(String payload) {
        String result;
        try {
            Map<String, String> params = parseFormData(payload);
            int id = toInt(params.get("id"));
            remboursementCrud.supprimer(id);
            AuditService.logCurrentUser("REMBOURSEMENT", "DELETE_REMBOURSEMENT", "id=" + id, "WARN");
            result = ok();
        } catch (Throwable e) {
            AuditService.logCurrentUser("REMBOURSEMENT", "DELETE_REMBOURSEMENT_FAILED", e.getMessage(), "WARN");
            result = err(e.getMessage());
        }
        sendResultToJs("onDeleteResult", result);
        return result;
    }

    public String payRemboursementFromJs(String payload) {
        String result;
        try {
            Map<String, String> params = parseFormData(payload);
            int id = toInt(params.get("id"));
            double amount = toDouble(params.get("amount"));
            String paymentMethod = toStringOrEmpty(params.get("paymentMethod"));
            String reference = toStringOrEmpty(params.get("reference"));
            String cardToken = toStringOrEmpty(params.get("cardToken"));

            if ("CARTE_BANCAIRE".equalsIgnoreCase(paymentMethod)
                    && "stripe".equalsIgnoreCase(config("PAYMENT_PROVIDER"))) {
                result = err("Utiliser Stripe Checkout pour payer par carte.");
                sendResultToJs("onPayResult", result);
                return result;
            }

            PaymentApiService.PaymentResult pay = paymentApiService.chargeRemboursement(id, amount, paymentMethod, reference, cardToken);
            if (!pay.approved) {
                result = err(pay.message);
                sendResultToJs("onPayResult", result);
                return result;
            }

            remboursementCrud.ajouterPaiement(id, amount);
            AuditService.logCurrentUser("PAYMENT", "PAY_REMBOURSEMENT",
                    "remboursementId=" + id + ", amount=" + round2(amount) + ", method=" + paymentMethod, "INFO");
            result = "{\"ok\":true,\"transactionId\":" + ProjetWebUtils.jsonString(pay.transactionId)
                    + ",\"status\":" + ProjetWebUtils.jsonString(pay.status)
                    + ",\"message\":" + ProjetWebUtils.jsonString(pay.message) + "}";
        } catch (Throwable e) {
            AuditService.logCurrentUser("PAYMENT", "PAY_REMBOURSEMENT_FAILED", e.getMessage(), "WARN");
            result = err(e.getMessage());
        }
        sendResultToJs("onPayResult", result);
        return result;
    }

    public String getPaymentStatusFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            String tx = toStringOrEmpty(params.get("transactionId"));
            String status = paymentApiService.fetchStatus(tx);
            return "{\"ok\":true,\"transactionId\":" + ProjetWebUtils.jsonString(tx)
                    + ",\"status\":" + ProjetWebUtils.jsonString(status) + "}";
        } catch (Throwable e) {
            return err(e.getMessage());
        }
    }

    public String createStripeCheckoutSessionFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            int id = toInt(params.get("id"));
            double amount = toDouble(params.get("amount"));
            String reference = toStringOrEmpty(params.get("reference"));
            String res = paymentApiService.createStripeCheckoutSession(id, amount, reference);
            if (res != null && res.contains("\"ok\":true")) {
                AuditService.logCurrentUser("PAYMENT", "STRIPE_CHECKOUT_SESSION_CREATED",
                        "remboursementId=" + id + ", amount=" + round2(amount), "INFO");
            } else {
                AuditService.logCurrentUser("PAYMENT", "STRIPE_CHECKOUT_SESSION_FAILED",
                        extractJsonField(res, "message"), "WARN");
            }
            return res;
        } catch (Throwable e) {
            AuditService.logCurrentUser("PAYMENT", "STRIPE_CHECKOUT_SESSION_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String confirmStripeCheckoutPaymentFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            String sessionId = toStringOrEmpty(params.get("sessionId"));
            PaymentApiService.StripeSessionStatus st = paymentApiService.fetchStripeCheckoutSessionStatus(sessionId);
            if (!st.ok) {
                return err(st.message);
            }
            if (!"paid".equalsIgnoreCase(st.paymentStatus)) {
                return "{\"ok\":false,\"status\":" + ProjetWebUtils.jsonString(st.paymentStatus)
                        + ",\"message\":\"Paiement non confirme (statut Stripe).\"}";
            }
            if (st.remboursementId <= 0) {
                return err("Session Stripe invalide: remboursement_id manquant.");
            }
            if (st.amount <= 0) {
                return err("Session Stripe invalide: montant manquant.");
            }
            if (!paymentApiService.consumeStripeCheckoutSession(sessionId)) {
                return "{\"ok\":false,\"status\":\"paid\",\"message\":\"Session Stripe deja traitee.\"}";
            }

            Remboursement r = remboursementCrud.getById(st.remboursementId);
            if (r == null) {
                return err("Remboursement introuvable.");
            }

            double restant = r.getMontantDu() - r.getMontantPaye();
            if (restant <= 0) {
                return "{\"ok\":false,\"status\":\"already_paid\",\"message\":\"Remboursement deja solde.\"}";
            }

            double amountToApply = Math.min(st.amount, restant);
            if (amountToApply <= 0) {
                return err("Montant Stripe invalide pour ce remboursement.");
            }

            remboursementCrud.ajouterPaiement(st.remboursementId, amountToApply);
            AuditService.logCurrentUser("PAYMENT", "STRIPE_PAYMENT_CONFIRMED",
                    "sessionId=" + sessionId + ", remboursementId=" + st.remboursementId + ", amount=" + round2(amountToApply), "INFO");
            return "{"
                    + "\"ok\":true"
                    + ",\"status\":\"paid\""
                    + ",\"sessionId\":" + ProjetWebUtils.jsonString(sessionId)
                    + ",\"amountApplied\":" + amountToApply
                    + ",\"message\":\"Paiement Stripe confirme et enregistre.\""
                    + "}";
        } catch (Throwable e) {
            AuditService.logCurrentUser("PAYMENT", "STRIPE_PAYMENT_CONFIRM_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String createSignatureForPaymentFromJs(String payload) {
        try {
            var current = Session.getCurrentUser();
            if (current == null) return err("Utilisateur non connecte.");
            if (current.getRole() == null || !"ENTREPRENEUR".equals(current.getRole().name())) {
                return err("Signature reservee a l'entrepreneur.");
            }

            Map<String, String> params = parseFormData(payload);
            int remboursementId = toInt(params.get("id"));
            String sessionId = toStringOrEmpty(params.get("sessionId"));
            String signerName = toStringOrEmpty(params.get("signerName"));
            String signerEmail = toStringOrEmpty(params.get("signerEmail"));
            String signatureImage = toStringOrEmpty(params.get("signatureImage"));

            if (remboursementId <= 0) return err("ID remboursement invalide.");
            if (signerName.isEmpty()) signerName = toStringOrEmpty(current.getNom()) + " " + toStringOrEmpty(current.getPrenom());
            if (signerEmail.isEmpty()) signerEmail = toStringOrEmpty(current.getEmail());
            if (signatureImage.isEmpty() || !signatureImage.startsWith("data:image/")) {
                return err("Signature electronique manquante.");
            }

            Remboursement r = remboursementCrud.getById(remboursementId);
            if (r == null) return err("Remboursement introuvable.");
            if (r.getMontantPaye() <= 0) {
                return err("Aucun paiement detecte. Signature impossible.");
            }

            SignatureApiService.SignatureResult sig = signatureApiService.requestSignature(remboursementId, sessionId, signerName, signerEmail, signatureImage);
            if (!sig.ok) return err(sig.message);
            saveSignatureRecord(remboursementId, sig.signatureId, signerName, signerEmail, signatureImage, sig.status);
            AuditService.logCurrentUser("SIGNATURE", "CREATE_SIGNATURE",
                    "remboursementId=" + remboursementId + ", signatureId=" + sig.signatureId, "INFO");

            return "{"
                    + "\"ok\":true"
                    + ",\"signatureId\":" + ProjetWebUtils.jsonString(sig.signatureId)
                    + ",\"status\":" + ProjetWebUtils.jsonString(sig.status)
                    + ",\"message\":" + ProjetWebUtils.jsonString(sig.message)
                    + "}";
        } catch (Throwable e) {
            AuditService.logCurrentUser("SIGNATURE", "CREATE_SIGNATURE_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String downloadPaymentReportFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            int remboursementId = toInt(params.get("id"));
            String sessionId = toStringOrEmpty(params.get("sessionId"));
            String signatureId = toStringOrEmpty(params.get("signatureId"));
            String signerName = toStringOrEmpty(params.get("signerName"));
            String signerEmail = toStringOrEmpty(params.get("signerEmail"));
            String signatureImage = toStringOrEmpty(params.get("signatureImage"));
            if (remboursementId <= 0) return err("ID remboursement invalide.");

            Remboursement r = remboursementCrud.getById(remboursementId);
            if (r == null) return err("Remboursement introuvable.");

            Financement2 f = null;
            Projet p = null;
            try {
                f = financementCrud.getById(r.getFinancementId());
                if (f != null) p = projetCrud.getById(f.getId_projet());
            } catch (Exception ignored) {
            }

            var current = Session.getCurrentUser();
            String actor = current == null ? "N/A" : (toStringOrEmpty(current.getNom()) + " " + toStringOrEmpty(current.getPrenom())).trim();
            String actorEmail = current == null ? "" : toStringOrEmpty(current.getEmail());

            String effectiveSigner = signerName.isEmpty() ? actor : signerName;
            String effectiveEmail = signerEmail.isEmpty() ? actorEmail : signerEmail;
            String projectLabel = p == null ? ("#" + (f == null ? 0 : f.getId_projet())) : toStringOrEmpty(p.getTitre());
            String statut = r.getMontantDu() > 0 && r.getMontantPaye() + 1e-9 >= r.getMontantDu() ? "PAYE" : "EN_ATTENTE";
            int echeanceNo = resolveEcheanceNumber(r);
            String reportPayload = "{"
                    + "\"remboursementId\":" + r.getId()
                    + ",\"echeanceNo\":" + echeanceNo
                    + ",\"project\":" + ProjetWebUtils.jsonString(projectLabel)
                    + ",\"montantDu\":" + round2(r.getMontantDu())
                    + ",\"montantPaye\":" + round2(r.getMontantPaye())
                    + ",\"statut\":" + ProjetWebUtils.jsonString(statut)
                    + ",\"dateEcheance\":" + ProjetWebUtils.jsonString(r.getDateEcheance() == null ? "-" : r.getDateEcheance().toString())
                    + ",\"sessionId\":" + ProjetWebUtils.jsonString(sessionId)
                    + ",\"signatureId\":" + ProjetWebUtils.jsonString(signatureId)
                    + ",\"signerName\":" + ProjetWebUtils.jsonString(effectiveSigner)
                    + ",\"signerEmail\":" + ProjetWebUtils.jsonString(effectiveEmail)
                    + "}";

            String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
            File out = chooseReportFile("recu-paiement-" + remboursementId + "-" + ts + ".pdf");
            if (out == null) return err("Telechargement annule.");
            writeReceiptPdf(out, remboursementId, echeanceNo, projectLabel, r.getMontantDu(), r.getMontantPaye(), statut,
                    r.getDateEcheance() == null ? "-" : r.getDateEcheance().toString(), signatureId, effectiveSigner, effectiveEmail, signatureImage);
            int generatedBy = current == null ? 0 : current.getId();
            String fileHash = sha256FileHex(out);
            saveReceiptRecord(remboursementId, signatureId, sessionId, reportPayload, out.getAbsolutePath(), "PDF", fileHash, generatedBy);
            AuditService.logCurrentUser("REPORT", "DOWNLOAD_RECEIPT",
                    "remboursementId=" + remboursementId + ", path=" + out.getAbsolutePath(), "INFO");

            return "{"
                    + "\"ok\":true"
                    + ",\"path\":" + ProjetWebUtils.jsonString(out.getAbsolutePath())
                    + ",\"sha256\":" + ProjetWebUtils.jsonString(fileHash)
                    + ",\"message\":\"Rapport telecharge avec succes.\""
                    + "}";
        } catch (Throwable e) {
            AuditService.logCurrentUser("REPORT", "DOWNLOAD_RECEIPT_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String getPaymentDocumentsForCurrentEntrepreneurJson() {
        try {
            var current = Session.getCurrentUser();
            if (current == null || current.getRole() == null || !"ENTREPRENEUR".equals(current.getRole().name())) return "[]";
            List<Integer> ownerIds = getCurrentEntrepreneurOwnerIds();
            if (ownerIds.isEmpty()) return "[]";
            ensureSchedulesForEntrepreneur(ownerIds);

            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return "[]";
            ensureSignatureReceiptTables();
            String in = buildInClause(ownerIds.size());
            String sql = "SELECT rp.id AS recuId, rp.remboursement_id AS remboursementId, rp.signature_id AS signatureId, rp.stripe_session_id AS sessionId, "
                    + "rp.file_path AS filePath, rp.file_type AS fileType, rp.report_hash_sha256 AS reportHash, rp.generated_at AS generatedAt, "
                    + "r.financement_id AS financementId, p.titre AS projectName, se.signer_name AS signerName, se.signer_email AS signerEmail "
                    + "FROM recu_paiement rp "
                    + "JOIN remboursement r ON r.id = rp.remboursement_id "
                    + "LEFT JOIN financement2 f ON f.id_financement = r.financement_id "
                    + "LEFT JOIN projet p ON p.id_projet = f.id_projet "
                    + "LEFT JOIN signature_electronique se ON se.signature_id = rp.signature_id AND se.remboursement_id = rp.remboursement_id "
                    + "WHERE p.id_entrepreneur IN (" + in + ") "
                    + "ORDER BY rp.generated_at DESC, rp.id DESC";
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Integer id : ownerIds) pst.setInt(idx++, id);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append("{")
                                .append("\"recuId\":").append(rs.getInt("recuId")).append(",")
                                .append("\"remboursementId\":").append(rs.getInt("remboursementId")).append(",")
                                .append("\"financementId\":").append(rs.getInt("financementId")).append(",")
                                .append("\"projectName\":").append(ProjetWebUtils.jsonString(rs.getString("projectName"))).append(",")
                                .append("\"signatureId\":").append(ProjetWebUtils.jsonString(rs.getString("signatureId"))).append(",")
                                .append("\"sessionId\":").append(ProjetWebUtils.jsonString(rs.getString("sessionId"))).append(",")
                                .append("\"signerName\":").append(ProjetWebUtils.jsonString(rs.getString("signerName"))).append(",")
                                .append("\"signerEmail\":").append(ProjetWebUtils.jsonString(rs.getString("signerEmail"))).append(",")
                                .append("\"filePath\":").append(ProjetWebUtils.jsonString(rs.getString("filePath"))).append(",")
                                .append("\"fileType\":").append(ProjetWebUtils.jsonString(rs.getString("fileType"))).append(",")
                                .append("\"reportHash\":").append(ProjetWebUtils.jsonString(rs.getString("reportHash"))).append(",")
                                .append("\"generatedAt\":").append(ProjetWebUtils.jsonString(String.valueOf(rs.getTimestamp("generatedAt"))))
                                .append("}");
                    }
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getPaymentDocumentsByFinancementJson(int financementId) {
        try {
            if (financementId <= 0) return "[]";
            var current = Session.getCurrentUser();
            if (current == null) return "[]";

            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return "[]";
            ensureSignatureReceiptTables();

            List<Integer> ownerIds = new ArrayList<>();
            boolean entrepreneur = current.getRole() != null && "ENTREPRENEUR".equals(current.getRole().name());
            if (entrepreneur) {
                ownerIds = getCurrentEntrepreneurOwnerIds();
            }

            StringBuilder sql = new StringBuilder(
                    "SELECT rp.id AS recuId, rp.remboursement_id AS remboursementId, rp.signature_id AS signatureId, rp.stripe_session_id AS sessionId, " +
                    "rp.file_path AS filePath, rp.file_type AS fileType, rp.report_hash_sha256 AS reportHash, rp.generated_at AS generatedAt, " +
                    "r.financement_id AS financementId, p.titre AS projectName, se.signer_name AS signerName, se.signer_email AS signerEmail " +
                    "FROM recu_paiement rp " +
                    "JOIN remboursement r ON r.id = rp.remboursement_id " +
                    "LEFT JOIN financement2 f ON f.id_financement = r.financement_id " +
                    "LEFT JOIN projet p ON p.id_projet = f.id_projet " +
                    "LEFT JOIN signature_electronique se ON se.signature_id = rp.signature_id AND se.remboursement_id = rp.remboursement_id " +
                    "WHERE r.financement_id=? "
            );

            if (entrepreneur && !ownerIds.isEmpty()) {
                sql.append("AND (p.id_entrepreneur IN (").append(buildInClause(ownerIds.size())).append(") OR p.id_entrepreneur IS NULL) ");
            }
            sql.append("ORDER BY rp.generated_at DESC, rp.id DESC");

            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                pst.setInt(idx++, financementId);
                if (entrepreneur && !ownerIds.isEmpty()) {
                    for (Integer ownerId : ownerIds) pst.setInt(idx++, ownerId);
                }
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        if (!first) out.append(",");
                        first = false;
                        out.append("{")
                                .append("\"recuId\":").append(rs.getInt("recuId")).append(",")
                                .append("\"remboursementId\":").append(rs.getInt("remboursementId")).append(",")
                                .append("\"financementId\":").append(rs.getInt("financementId")).append(",")
                                .append("\"projectName\":").append(ProjetWebUtils.jsonString(rs.getString("projectName"))).append(",")
                                .append("\"signatureId\":").append(ProjetWebUtils.jsonString(rs.getString("signatureId"))).append(",")
                                .append("\"sessionId\":").append(ProjetWebUtils.jsonString(rs.getString("sessionId"))).append(",")
                                .append("\"signerName\":").append(ProjetWebUtils.jsonString(rs.getString("signerName"))).append(",")
                                .append("\"signerEmail\":").append(ProjetWebUtils.jsonString(rs.getString("signerEmail"))).append(",")
                                .append("\"filePath\":").append(ProjetWebUtils.jsonString(rs.getString("filePath"))).append(",")
                                .append("\"fileType\":").append(ProjetWebUtils.jsonString(rs.getString("fileType"))).append(",")
                                .append("\"reportHash\":").append(ProjetWebUtils.jsonString(rs.getString("reportHash"))).append(",")
                                .append("\"generatedAt\":").append(ProjetWebUtils.jsonString(String.valueOf(rs.getTimestamp("generatedAt"))))
                                .append("}");
                    }
                }
            }
            out.append("]");
            return out.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String verifyReceiptIntegrityFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            int recuId = toInt(params.get("recuId"));
            if (recuId <= 0) return err("ID recu invalide.");
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return err("Connexion DB indisponible.");
            ensureSignatureReceiptTables();
            String sql = "SELECT file_path, report_hash_sha256 FROM recu_paiement WHERE id=?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, recuId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) return err("Recu introuvable.");
                    String path = toStringOrEmpty(rs.getString("file_path"));
                    String expected = toStringOrEmpty(rs.getString("report_hash_sha256"));
                    if (path.isEmpty()) return err("Chemin recu vide.");
                    File f = new File(path);
                    if (!f.exists()) return "{\"ok\":false,\"valid\":false,\"message\":\"Fichier recu absent.\"}";
                    String actual = sha256FileHex(f);
                    boolean valid = !expected.isEmpty() && expected.equalsIgnoreCase(actual);
                    return "{"
                            + "\"ok\":true"
                            + ",\"valid\":" + (valid ? "true" : "false")
                            + ",\"expected\":" + ProjetWebUtils.jsonString(expected)
                            + ",\"actual\":" + ProjetWebUtils.jsonString(actual)
                            + ",\"message\":" + ProjetWebUtils.jsonString(valid ? "Integrite validee." : "Integrite invalide.")
                            + "}";
                }
            }
        } catch (Throwable e) {
            return err(e.getMessage());
        }
    }

    public String getPaymentDocumentsByRemboursementsFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            String idsRaw = toStringOrEmpty(params.get("ids"));
            if (idsRaw.isEmpty()) return "[]";

            List<Integer> rembIds = new ArrayList<>();
            for (String part : idsRaw.split(",")) {
                int v = toInt(part);
                if (v > 0) rembIds.add(v);
            }
            if (rembIds.isEmpty()) return "[]";

            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return "[]";
            ensureSignatureReceiptTables();

            String in = buildInClause(rembIds.size());
            String sql = "SELECT rp.id AS recuId, rp.remboursement_id AS remboursementId, rp.signature_id AS signatureId, rp.stripe_session_id AS sessionId, "
                    + "rp.file_path AS filePath, rp.file_type AS fileType, rp.report_hash_sha256 AS reportHash, rp.generated_at AS generatedAt, "
                    + "se.signer_name AS signerName, se.signer_email AS signerEmail "
                    + "FROM recu_paiement rp "
                    + "LEFT JOIN signature_electronique se ON se.signature_id = rp.signature_id AND se.remboursement_id = rp.remboursement_id "
                    + "WHERE rp.remboursement_id IN (" + in + ") "
                    + "ORDER BY rp.generated_at DESC, rp.id DESC";

            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Integer id : rembIds) pst.setInt(idx++, id);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        if (!first) out.append(",");
                        first = false;
                        out.append("{")
                                .append("\"recuId\":").append(rs.getInt("recuId")).append(",")
                                .append("\"remboursementId\":").append(rs.getInt("remboursementId")).append(",")
                                .append("\"signatureId\":").append(ProjetWebUtils.jsonString(rs.getString("signatureId"))).append(",")
                                .append("\"sessionId\":").append(ProjetWebUtils.jsonString(rs.getString("sessionId"))).append(",")
                                .append("\"signerName\":").append(ProjetWebUtils.jsonString(rs.getString("signerName"))).append(",")
                                .append("\"signerEmail\":").append(ProjetWebUtils.jsonString(rs.getString("signerEmail"))).append(",")
                                .append("\"filePath\":").append(ProjetWebUtils.jsonString(rs.getString("filePath"))).append(",")
                                .append("\"fileType\":").append(ProjetWebUtils.jsonString(rs.getString("fileType"))).append(",")
                                .append("\"reportHash\":").append(ProjetWebUtils.jsonString(rs.getString("reportHash"))).append(",")
                                .append("\"generatedAt\":").append(ProjetWebUtils.jsonString(String.valueOf(rs.getTimestamp("generatedAt"))))
                                .append("}");
                    }
                }
            }
            out.append("]");
            return out.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getAuditLogsJsonFromJs(String payload) {
        try {
            var current = Session.getCurrentUser();
            if (current == null) return "[]";
            AuditService.ensureAuditTable();
            Map<String, String> params = parseFormData(payload);
            String module = toStringOrEmpty(params.get("module")).toUpperCase(Locale.ROOT);
            String level = toStringOrEmpty(params.get("level")).toUpperCase(Locale.ROOT);
            String q = toStringOrEmpty(params.get("q")).toLowerCase(Locale.ROOT);
            int limit = toInt(params.get("limit"));
            if (limit <= 0 || limit > 500) limit = 200;

            boolean isAdmin = current.getRole() != null && "ADMIN".equalsIgnoreCase(current.getRole().name());
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return "[]";

            StringBuilder sql = new StringBuilder(
                    "SELECT id, user_id, user_email, module, action, level, details, created_at FROM audit_log WHERE 1=1 ");
            if (!isAdmin) sql.append("AND user_id=? ");
            if (!module.isEmpty() && !"ALL".equals(module)) sql.append("AND module=? ");
            if (!level.isEmpty() && !"ALL".equals(level)) sql.append("AND level=? ");
            if (!q.isEmpty()) sql.append("AND (LOWER(action) LIKE ? OR LOWER(details) LIKE ? OR LOWER(user_email) LIKE ?) ");
            sql.append("ORDER BY created_at DESC, id DESC LIMIT ?");

            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (!isAdmin) pst.setInt(idx++, current.getId());
                if (!module.isEmpty() && !"ALL".equals(module)) pst.setString(idx++, module);
                if (!level.isEmpty() && !"ALL".equals(level)) pst.setString(idx++, level);
                if (!q.isEmpty()) {
                    String like = "%" + q + "%";
                    pst.setString(idx++, like);
                    pst.setString(idx++, like);
                    pst.setString(idx++, like);
                }
                pst.setInt(idx, limit);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        if (!first) out.append(",");
                        first = false;
                        out.append("{")
                                .append("\"id\":").append(rs.getLong("id")).append(",")
                                .append("\"userId\":").append(rs.getInt("user_id")).append(",")
                                .append("\"userEmail\":").append(ProjetWebUtils.jsonString(rs.getString("user_email"))).append(",")
                                .append("\"module\":").append(ProjetWebUtils.jsonString(rs.getString("module"))).append(",")
                                .append("\"action\":").append(ProjetWebUtils.jsonString(rs.getString("action"))).append(",")
                                .append("\"level\":").append(ProjetWebUtils.jsonString(rs.getString("level"))).append(",")
                                .append("\"details\":").append(ProjetWebUtils.jsonString(rs.getString("details"))).append(",")
                                .append("\"createdAt\":").append(ProjetWebUtils.jsonString(String.valueOf(rs.getTimestamp("created_at"))))
                                .append("}");
                    }
                }
            }
            out.append("]");
            return out.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getRemboursementCalendarForCurrentEntrepreneurJson() {
        try {
            var current = Session.getCurrentUser();
            if (current == null || current.getRole() == null || !"ENTREPRENEUR".equals(current.getRole().name())) {
                return "[]";
            }
            List<Integer> ownerIds = getCurrentEntrepreneurOwnerIds();
            if (ownerIds.isEmpty()) return "[]";

            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return "[]";
            String ownerCol = detectProjetOwnerColumn(conn);
            if (ownerCol == null) return "[]";

            String in = buildInClause(ownerIds.size());
            String sql = "SELECT r.id AS remboursementId, r.financement_id AS financementId, r.date_echeance AS dateEcheance, "
                    + "r.montant_du AS montantDu, r.montant_paye AS montantPaye, p.titre AS projectTitle "
                    + "FROM remboursement r "
                    + "LEFT JOIN financement2 f ON f.id_financement = r.financement_id "
                    + "LEFT JOIN projet p ON p.id_projet = f.id_projet "
                    + "WHERE p." + ownerCol + " IN (" + in + ") "
                    + "ORDER BY r.date_echeance ASC, r.id ASC";

            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Integer id : ownerIds) pst.setInt(idx++, id);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        int remboursementId = rs.getInt("remboursementId");
                        String date = rs.getDate("dateEcheance") == null ? "" : rs.getDate("dateEcheance").toString();
                        double du = rs.getDouble("montantDu");
                        double paye = rs.getDouble("montantPaye");
                        if (paye < 0) paye = 0;
                        if (du > 0 && paye > du) paye = du;
                        String status = (du > 0 && paye + 1e-9 >= du) ? "PAYE" : "EN_ATTENTE";
                        String project = toStringOrEmpty(rs.getString("projectTitle"));

                        if (!first) out.append(",");
                        first = false;
                        out.append("{")
                                .append("\"id\":").append(remboursementId).append(",")
                                .append("\"date\":").append(ProjetWebUtils.jsonString(date)).append(",")
                                .append("\"status\":").append(ProjetWebUtils.jsonString(status)).append(",")
                                .append("\"project\":").append(ProjetWebUtils.jsonString(project)).append(",")
                                .append("\"montantDu\":").append(round2(du)).append(",")
                                .append("\"montantPaye\":").append(round2(paye))
                                .append("}");
                    }
                }
            }
            out.append("]");
            return out.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String syncRemboursementCalendarToMicrosoftFromJs() {
        try {
            if (!microsoftGraphCalendarService.isConfigured()) {
                AuditService.logCurrentUser("CALENDAR", "SYNC_OUTLOOK_FAILED", "Graph non configure", "WARN");
                return "{\"ok\":false,\"synced\":0,\"total\":0,\"message\":\"Microsoft Graph non configure. Variables: MS_GRAPH_ACCESS_TOKEN, MS_GRAPH_CALENDAR_ID (optionnel MS_GRAPH_USER_ID).\"}";
            }
            String raw = getRemboursementCalendarForCurrentEntrepreneurJson();
            List<Map<String, String>> items = parseCalendarRows(raw);
            int total = items.size();
            if (total == 0) {
                AuditService.logCurrentUser("CALENDAR", "SYNC_OUTLOOK_EMPTY", "Aucune echeance a synchroniser", "INFO");
                return "{\"ok\":true,\"synced\":0,\"total\":0,\"message\":\"Aucune echeance a synchroniser (verifier vos remboursements entrepreneur).\"}";
            }
            String minDate = "9999-12-31";
            String maxDate = "0000-01-01";
            for (Map<String, String> it : items) {
                String d = toStringOrEmpty(it.get("date"));
                if (d.isEmpty()) continue;
                if (d.compareTo(minDate) < 0) minDate = d;
                if (d.compareTo(maxDate) > 0) maxDate = d;
            }
            if (!"9999-12-31".equals(minDate) && !"0000-01-01".equals(maxDate)) {
                try {
                    java.time.LocalDate mx = java.time.LocalDate.parse(maxDate).plusDays(1);
                    maxDate = mx.toString();
                } catch (Exception ignored) {}
            }
            String existingRaw = ("9999-12-31".equals(minDate) || "0000-01-01".equals(maxDate))
                    ? "{\"value\":[]}"
                    : microsoftGraphCalendarService.listEvents(minDate, maxDate);
            Map<String, MsEventLite> existingByRid = parseMicrosoftEventsByRid(existingRaw);

            int synced = 0;
            int skipped = 0;
            int failed = 0;
            int updated = 0;
            StringBuilder failures = new StringBuilder("[");
            boolean firstFailure = true;
            for (Map<String, String> it : items) {
                String rid = toStringOrEmpty(it.get("id"));
                String date = toStringOrEmpty(it.get("date"));
                String status = toStringOrEmpty(it.get("status"));
                String project = toStringOrEmpty(it.get("project"));
                String du = toStringOrEmpty(it.get("montantDu"));
                String paye = toStringOrEmpty(it.get("montantPaye"));
                if (rid.isEmpty() || date.isEmpty()) continue;

                String subject = "[INVESTIA][" + (status.isEmpty() ? "EN_ATTENTE" : status) + "][RID:" + rid + "] " + (project.isEmpty() ? "Remboursement" : project);
                String desc = "Remboursement ID: " + rid + " | Montant paye: " + paye + " / " + du + " TND";
                String tx = "investia-remb-" + rid;
                String expectedSig = buildSyncSignature(date, status, paye, du, project);
                MsEventLite existing = existingByRid.get(rid);
                String r;
                if (existing != null && expectedSig.equals(existing.signature)) {
                    r = "{\"ok\":true,\"skipped\":true,\"message\":\"Evenement deja a jour.\"}";
                } else if (existing != null && !toStringOrEmpty(existing.eventId).isEmpty()) {
                    r = microsoftGraphCalendarService.updateAllDayEvent(existing.eventId, subject, desc, date);
                    if (r.contains("\"ok\":true")) {
                        updated++;
                    } else {
                        // Fallback: if PATCH update fails on this runtime/account, retry create flow.
                        // If event already exists, provider returns duplicate and we treat it as skipped.
                        String createFallback = microsoftGraphCalendarService.createAllDayEvent(tx, subject, desc, date);
                        if (createFallback.contains("\"ok\":true")) {
                            r = createFallback;
                        }
                    }
                } else {
                    r = microsoftGraphCalendarService.createAllDayEvent(tx, subject, desc, date);
                }
                if (r.contains("\"ok\":true")) {
                    synced++;
                    if (r.contains("\"skipped\":true")) skipped++;
                } else {
                    failed++;
                    String m = extractJsonField(r, "message");
                    if (!firstFailure) failures.append(",");
                    firstFailure = false;
                    failures.append("{")
                            .append("\"rid\":").append(ProjetWebUtils.jsonString(rid)).append(",")
                            .append("\"message\":").append(ProjetWebUtils.jsonString(m))
                            .append("}");
                }
            }
            failures.append("]");
            boolean ok = (synced > 0 || total == 0);
            int created = Math.max(0, synced - skipped - updated);
            String msg = "Synchronisation Microsoft terminee. Crees: " + created + ", mis a jour: " + updated + ", deja existants: " + skipped + ", erreurs: " + failed + ".";
            AuditService.logCurrentUser("CALENDAR", ok ? "SYNC_OUTLOOK_SUCCESS" : "SYNC_OUTLOOK_PARTIAL",
                    "total=" + total + ", synced=" + synced + ", failed=" + failed, ok ? "INFO" : "WARN");
            return "{"
                    + "\"ok\":" + (ok ? "true" : "false")
                    + ",\"synced\":" + synced
                    + ",\"created\":" + created
                    + ",\"updated\":" + updated
                    + ",\"skipped\":" + skipped
                    + ",\"failed\":" + failed
                    + ",\"total\":" + total
                    + ",\"message\":" + ProjetWebUtils.jsonString(msg)
                    + ",\"failures\":" + failures
                    + "}";
        } catch (Exception e) {
            AuditService.logCurrentUser("CALENDAR", "SYNC_OUTLOOK_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String getCalendarSyncDiagnosticsFromJs() {
        try {
            var current = Session.getCurrentUser();
            if (current == null || current.getRole() == null || !"ENTREPRENEUR".equals(current.getRole().name())) {
                return "{\"ok\":false,\"message\":\"Utilisateur entrepreneur non connecte.\"}";
            }
            List<Integer> ownerIds = getCurrentEntrepreneurOwnerIds();
            if (ownerIds.isEmpty()) {
                return "{\"ok\":true,\"projects\":0,\"financements\":0,\"remboursements\":0,\"paye\":0,\"enAttente\":0,\"message\":\"Aucun profil entrepreneur lie.\"}";
            }
            ensureSchedulesForEntrepreneur(ownerIds);

            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return "{\"ok\":false,\"message\":\"Connexion base indisponible.\"}";
            String ownerCol = detectProjetOwnerColumn(conn);
            if (ownerCol == null) return "{\"ok\":false,\"message\":\"Colonne proprietaire projet introuvable.\"}";

            String in = buildInClause(ownerIds.size());
            int projects = 0;
            int financements = 0;
            int remboursements = 0;
            int paye = 0;
            int enAttente = 0;

            String qProjects = "SELECT COUNT(*) AS c FROM projet WHERE " + ownerCol + " IN (" + in + ")";
            try (PreparedStatement pst = conn.prepareStatement(qProjects)) {
                int idx = 1;
                for (Integer id : ownerIds) pst.setInt(idx++, id);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) projects = rs.getInt("c");
                }
            }

            String qFin = "SELECT COUNT(*) AS c FROM financement2 f JOIN projet p ON p.id_projet=f.id_projet WHERE p." + ownerCol + " IN (" + in + ")";
            try (PreparedStatement pst = conn.prepareStatement(qFin)) {
                int idx = 1;
                for (Integer id : ownerIds) pst.setInt(idx++, id);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) financements = rs.getInt("c");
                }
            }

            String qRemb = "SELECT COUNT(*) AS c, "
                    + "SUM(CASE WHEN COALESCE(r.montant_paye,0) >= COALESCE(r.montant_du,0) AND COALESCE(r.montant_du,0) > 0 THEN 1 ELSE 0 END) AS payeCount "
                    + "FROM remboursement r "
                    + "JOIN financement2 f ON f.id_financement=r.financement_id "
                    + "JOIN projet p ON p.id_projet=f.id_projet "
                    + "WHERE p." + ownerCol + " IN (" + in + ")";
            try (PreparedStatement pst = conn.prepareStatement(qRemb)) {
                int idx = 1;
                for (Integer id : ownerIds) pst.setInt(idx++, id);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        remboursements = rs.getInt("c");
                        paye = rs.getInt("payeCount");
                    }
                }
            }
            enAttente = Math.max(0, remboursements - paye);

            return "{"
                    + "\"ok\":true"
                    + ",\"projects\":" + projects
                    + ",\"financements\":" + financements
                    + ",\"remboursements\":" + remboursements
                    + ",\"paye\":" + paye
                    + ",\"enAttente\":" + enAttente
                    + ",\"ownerKeys\":" + ownerIds.size()
                    + ",\"graphConfigured\":" + (microsoftGraphCalendarService.isConfigured() ? "true" : "false")
                    + "}";
        } catch (Exception e) {
            return "{\"ok\":false,\"message\":" + ProjetWebUtils.jsonString(toStringOrEmpty(e.getMessage())) + "}";
        }
    }

    public String getMicrosoftCalendarEventsFromJs(String payload) {
        try {
            if (!microsoftGraphCalendarService.isConfigured()) {
                return "{\"ok\":false,\"value\":[],\"message\":\"Microsoft Graph non configure (MS_GRAPH_ACCESS_TOKEN / MS_GRAPH_CALENDAR_ID).\"}";
            }
            Map<String, String> p = parseFormData(payload);
            String start = toStringOrEmpty(p.get("startDate"));
            String end = toStringOrEmpty(p.get("endDate"));
            if (start.isEmpty() || end.isEmpty()) {
                return "{\"ok\":false,\"value\":[],\"message\":\"Periode invalide pour chargement Microsoft Calendar.\"}";
            }
            return microsoftGraphCalendarService.listEvents(start, end);
        } catch (Exception e) {
            return "{\"ok\":false,\"value\":[],\"message\":" + ProjetWebUtils.jsonString(toStringOrEmpty(e.getMessage())) + "}";
        }
    }

    public String getEntrepreneurNotesJson() {
        try {
            int userId = getCurrentEntrepreneurUserId();
            if (userId <= 0) return "[]";
            ensureEntrepreneurWorkspaceTables();
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return "[]";
            String sql = "SELECT id, title, content, created_at, updated_at "
                    + "FROM entrepreneur_note WHERE entrepreneur_user_id=? ORDER BY updated_at DESC, id DESC";
            StringBuilder sb = new StringBuilder("[");
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append("{")
                                .append("\"id\":").append(rs.getInt("id")).append(",")
                                .append("\"title\":").append(ProjetWebUtils.jsonString(toStringOrEmpty(rs.getString("title")))).append(",")
                                .append("\"content\":").append(ProjetWebUtils.jsonString(toStringOrEmpty(rs.getString("content")))).append(",")
                                .append("\"createdAt\":").append(ProjetWebUtils.jsonString(ProjetWebUtils.formatTimestamp(rs.getTimestamp("created_at")))).append(",")
                                .append("\"updatedAt\":").append(ProjetWebUtils.jsonString(ProjetWebUtils.formatTimestamp(rs.getTimestamp("updated_at"))))
                                .append("}");
                    }
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String createEntrepreneurNoteFromJs(String payload) {
        try {
            int userId = getCurrentEntrepreneurUserId();
            if (userId <= 0) return err("Acces reserve a l'entrepreneur connecte.");
            Map<String, String> p = parseFormData(payload);
            String title = toStringOrEmpty(p.get("title"));
            String content = toStringOrEmpty(p.get("content"));
            if (title.isEmpty() && content.isEmpty()) return err("Note vide.");
            if (title.length() > 190) title = title.substring(0, 190);
            ensureEntrepreneurWorkspaceTables();
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return err("Connexion DB indisponible.");
            String sql = "INSERT INTO entrepreneur_note (entrepreneur_user_id, title, content) VALUES (?,?,?)";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, userId);
                pst.setString(2, title);
                pst.setString(3, content);
                int rows = pst.executeUpdate();
                if (rows <= 0) return err("Ajout note echoue.");
            }
            AuditService.logCurrentUser("NOTES", "CREATE_NOTE",
                    "title=" + (title.isEmpty() ? "(sans titre)" : title), "INFO");
            return ok();
        } catch (Exception e) {
            AuditService.logCurrentUser("NOTES", "CREATE_NOTE_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String updateEntrepreneurNoteFromJs(String payload) {
        try {
            int userId = getCurrentEntrepreneurUserId();
            if (userId <= 0) return err("Acces reserve a l'entrepreneur connecte.");
            Map<String, String> p = parseFormData(payload);
            int id = toInt(p.get("id"));
            String title = toStringOrEmpty(p.get("title"));
            String content = toStringOrEmpty(p.get("content"));
            if (id <= 0) return err("ID note invalide.");
            if (title.length() > 190) title = title.substring(0, 190);
            ensureEntrepreneurWorkspaceTables();
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return err("Connexion DB indisponible.");
            String sql = "UPDATE entrepreneur_note "
                    + "SET title=?, content=?, updated_at=CURRENT_TIMESTAMP "
                    + "WHERE id=? AND entrepreneur_user_id=?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, title);
                pst.setString(2, content);
                pst.setInt(3, id);
                pst.setInt(4, userId);
                int rows = pst.executeUpdate();
                if (rows <= 0) return err("Note introuvable ou non autorisee.");
            }
            AuditService.logCurrentUser("NOTES", "UPDATE_NOTE", "id=" + id, "INFO");
            return ok();
        } catch (Exception e) {
            AuditService.logCurrentUser("NOTES", "UPDATE_NOTE_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String deleteEntrepreneurNoteFromJs(String payload) {
        try {
            int userId = getCurrentEntrepreneurUserId();
            if (userId <= 0) return err("Acces reserve a l'entrepreneur connecte.");
            Map<String, String> p = parseFormData(payload);
            int id = toInt(p.get("id"));
            if (id <= 0) return err("ID note invalide.");
            ensureEntrepreneurWorkspaceTables();
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return err("Connexion DB indisponible.");
            String sql = "DELETE FROM entrepreneur_note WHERE id=? AND entrepreneur_user_id=?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, id);
                pst.setInt(2, userId);
                int rows = pst.executeUpdate();
                if (rows <= 0) return err("Note introuvable ou non autorisee.");
            }
            AuditService.logCurrentUser("NOTES", "DELETE_NOTE", "id=" + id, "WARN");
            return ok();
        } catch (Exception e) {
            AuditService.logCurrentUser("NOTES", "DELETE_NOTE_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String getEntrepreneurCalcHistoryJson() {
        try {
            int userId = getCurrentEntrepreneurUserId();
            if (userId <= 0) return "[]";
            ensureEntrepreneurWorkspaceTables();
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return "[]";
            String sql = "SELECT id, scenario_label, principal, annual_rate_pct, duration_months, monthly_payment, total_payment, total_interest, created_at "
                    + "FROM entrepreneur_calc_history WHERE entrepreneur_user_id=? ORDER BY id DESC";
            StringBuilder sb = new StringBuilder("[");
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append("{")
                                .append("\"id\":").append(rs.getInt("id")).append(",")
                                .append("\"label\":").append(ProjetWebUtils.jsonString(toStringOrEmpty(rs.getString("scenario_label")))).append(",")
                                .append("\"principal\":").append(rs.getDouble("principal")).append(",")
                                .append("\"annualRatePct\":").append(rs.getDouble("annual_rate_pct")).append(",")
                                .append("\"durationMonths\":").append(rs.getInt("duration_months")).append(",")
                                .append("\"monthlyPayment\":").append(rs.getDouble("monthly_payment")).append(",")
                                .append("\"totalPayment\":").append(rs.getDouble("total_payment")).append(",")
                                .append("\"totalInterest\":").append(rs.getDouble("total_interest")).append(",")
                                .append("\"createdAt\":").append(ProjetWebUtils.jsonString(ProjetWebUtils.formatTimestamp(rs.getTimestamp("created_at"))))
                                .append("}");
                    }
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String saveEntrepreneurCalcFromJs(String payload) {
        try {
            int userId = getCurrentEntrepreneurUserId();
            if (userId <= 0) return err("Acces reserve a l'entrepreneur connecte.");
            Map<String, String> p = parseFormData(payload);
            String label = toStringOrEmpty(p.get("label"));
            if (label.length() > 190) label = label.substring(0, 190);
            double principal = Math.max(0.0, toDouble(p.get("principal")));
            double annualRatePct = Math.max(0.0, toDouble(p.get("annualRatePct")));
            int durationMonths = Math.max(1, toInt(p.get("durationMonths")));
            double monthlyPayment = Math.max(0.0, toDouble(p.get("monthlyPayment")));
            double totalPayment = Math.max(0.0, toDouble(p.get("totalPayment")));
            double totalInterest = Math.max(0.0, toDouble(p.get("totalInterest")));
            ensureEntrepreneurWorkspaceTables();
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return err("Connexion DB indisponible.");
            String sql = "INSERT INTO entrepreneur_calc_history "
                    + "(entrepreneur_user_id, scenario_label, principal, annual_rate_pct, duration_months, monthly_payment, total_payment, total_interest) "
                    + "VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, userId);
                pst.setString(2, label);
                pst.setDouble(3, round2(principal));
                pst.setDouble(4, round2(annualRatePct));
                pst.setInt(5, durationMonths);
                pst.setDouble(6, round2(monthlyPayment));
                pst.setDouble(7, round2(totalPayment));
                pst.setDouble(8, round2(totalInterest));
                int rows = pst.executeUpdate();
                if (rows <= 0) return err("Sauvegarde calcul echouee.");
            }
            AuditService.logCurrentUser("CALCULATOR", "SAVE_CALC_SCENARIO",
                    "label=" + (label.isEmpty() ? "(sans nom)" : label) + ", principal=" + round2(principal), "INFO");
            return ok();
        } catch (Exception e) {
            AuditService.logCurrentUser("CALCULATOR", "SAVE_CALC_SCENARIO_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String deleteEntrepreneurCalcFromJs(String payload) {
        try {
            int userId = getCurrentEntrepreneurUserId();
            if (userId <= 0) return err("Acces reserve a l'entrepreneur connecte.");
            Map<String, String> p = parseFormData(payload);
            int id = toInt(p.get("id"));
            if (id <= 0) return err("ID calcul invalide.");
            ensureEntrepreneurWorkspaceTables();
            Connection conn = MyBD.getInstance().getConn();
            if (conn == null) return err("Connexion DB indisponible.");
            String sql = "DELETE FROM entrepreneur_calc_history WHERE id=? AND entrepreneur_user_id=?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, id);
                pst.setInt(2, userId);
                int rows = pst.executeUpdate();
                if (rows <= 0) return err("Ligne introuvable ou non autorisee.");
            }
            AuditService.logCurrentUser("CALCULATOR", "DELETE_CALC_SCENARIO", "id=" + id, "WARN");
            return ok();
        } catch (Exception e) {
            AuditService.logCurrentUser("CALCULATOR", "DELETE_CALC_SCENARIO_FAILED", e.getMessage(), "WARN");
            return err(e.getMessage());
        }
    }

    public String printFileFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            String path = toStringOrEmpty(params.get("path"));
            if (path.isEmpty()) return err("Chemin fichier vide.");
            File f = new File(path);
            if (!f.exists()) return err("Fichier introuvable.");
            if (!Desktop.isDesktopSupported()) return err("Desktop non supporte.");
            Desktop.getDesktop().print(f);
            return ok();
        } catch (Throwable e) {
            return err(e.getMessage());
        }
    }

    public String openExternalUrl(String payload) {
        try {
            String url = "";
            String raw = toStringOrEmpty(payload).trim();
            if (!raw.isEmpty()) {
                if (raw.startsWith("http://") || raw.startsWith("https://")) {
                    // Support direct URL payload (used by contact pages).
                    url = raw;
                } else {
                    Map<String, String> params = parseFormData(raw);
                    url = toStringOrEmpty(params.get("url"));
                }
            }
            if (url.isEmpty()) return err("URL vide.");
            if (!Desktop.isDesktopSupported()) return err("Desktop non supporte.");
            Desktop.getDesktop().browse(URI.create(url));
            return ok();
        } catch (Throwable e) {
            return err(e.getMessage());
        }
    }

    public String openReceiptDocumentFromJs(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            int recuId = toInt(params.get("recuId"));
            String path = toStringOrEmpty(params.get("path"));

            if (path.isEmpty() && recuId > 0) {
                Connection conn = MyBD.getInstance().getConn();
                if (conn != null) {
                    try (PreparedStatement pst = conn.prepareStatement("SELECT file_path FROM recu_paiement WHERE id=?")) {
                        pst.setInt(1, recuId);
                        try (ResultSet rs = pst.executeQuery()) {
                            if (rs.next()) path = toStringOrEmpty(rs.getString("file_path"));
                        }
                    }
                }
            }

            if (path.isEmpty()) return err("Chemin document vide.");

            File f = resolveReadableFile(path);
            if (f == null || !f.exists()) {
                return err("Fichier introuvable pour ce recu.");
            }
            if (!Desktop.isDesktopSupported()) return err("Desktop non supporte.");
            Desktop.getDesktop().open(f);
            return "{"
                    + "\"ok\":true"
                    + ",\"path\":" + ProjetWebUtils.jsonString(f.getAbsolutePath())
                    + "}";
        } catch (Throwable e) {
            return err(e.getMessage());
        }
    }

    public String openStripeCheckoutPopup(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);
            String url = toStringOrEmpty(params.get("url"));
            if (url.isEmpty()) return err("URL vide.");

            Platform.runLater(() -> {
                try {
                    Stage stage = new Stage();
                    stage.setTitle("Paiement carte - Stripe Checkout");
                    WebView checkoutView = new WebView();
                    WebEngine checkoutEngine = checkoutView.getEngine();
                    checkoutEngine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                        String loc = toStringOrEmpty(newLoc);
                        if (loc.isEmpty()) return;
                        String lower = loc.toLowerCase(Locale.ROOT);
                        boolean success = lower.contains("stripe-success") || (lower.contains("success") && lower.contains("session_id="));
                        boolean cancel = lower.contains("stripe-cancel") || lower.contains("cancel");
                        if (!success && !cancel) return;

                        String sessionId = extractQueryParam(loc, "session_id");
                        String json = "{"
                                + "\"ok\":true"
                                + ",\"status\":" + ProjetWebUtils.jsonString(success ? "success" : "cancel")
                                + ",\"sessionId\":" + ProjetWebUtils.jsonString(sessionId)
                                + ",\"url\":" + ProjetWebUtils.jsonString(loc)
                                + "}";
                        sendResultToJs("onStripeCheckoutPopupResult", json);
                        try {
                            stage.close();
                        } catch (Throwable ignored) {
                        }
                    });
                    checkoutEngine.load(url);
                    stage.setScene(new Scene(checkoutView, 980, 760));
                    try {
                        if (webView != null && webView.getScene() != null) {
                            Window owner = webView.getScene().getWindow();
                            if (owner != null) {
                                stage.initOwner(owner);
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                    stage.show();
                } catch (Throwable ignored) {
                }
            });
            return ok();
        } catch (Throwable e) {
            return err(e.getMessage());
        }
    }

    private static String extractQueryParam(String url, String key) {
        try {
            URI uri = URI.create(url);
            String q = uri.getRawQuery();
            if (q == null || q.isEmpty()) return "";
            String[] pairs = q.split("&");
            for (String pair : pairs) {
                if (pair == null || pair.isEmpty()) continue;
                int idx = pair.indexOf('=');
                String k = idx >= 0 ? pair.substring(0, idx) : pair;
                String v = idx >= 0 ? pair.substring(idx + 1) : "";
                String dk = URLDecoder.decode(k, StandardCharsets.UTF_8);
                if (key.equals(dk)) {
                    return URLDecoder.decode(v, StandardCharsets.UTF_8);
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static File resolveReadableFile(String rawPath) {
        String p = toStringOrEmpty(rawPath);
        if (p.isEmpty()) return null;

        try {
            p = URLDecoder.decode(p, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }

        if (p.startsWith("file:///")) {
            try {
                return new File(URI.create(p));
            } catch (Exception ignored) {
                p = p.substring("file:///".length());
            }
        } else if (p.startsWith("file://")) {
            try {
                return new File(URI.create(p));
            } catch (Exception ignored) {
                p = p.substring("file://".length());
            }
        }

        File direct = new File(p);
        if (direct.exists()) return direct;

        String name = direct.getName();
        if (name == null || name.isEmpty()) return direct;
        File inCwd = new File(System.getProperty("user.dir", "."), name);
        if (inCwd.exists()) return inCwd;
        File inDownloads = new File(System.getProperty("user.home", "."), "Downloads" + File.separator + name);
        if (inDownloads.exists()) return inDownloads;
        return direct;
    }

    private void ensureSignatureReceiptTables() throws SQLException {
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return;
        String sqlSignature = "CREATE TABLE IF NOT EXISTS signature_electronique ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "remboursement_id INT NOT NULL,"
                + "signature_id VARCHAR(120) NOT NULL,"
                + "signer_name VARCHAR(190) NOT NULL,"
                + "signer_email VARCHAR(190) NULL,"
                + "signature_image LONGTEXT NOT NULL,"
                + "signature_sha256 VARCHAR(64) NULL,"
                + "api_status VARCHAR(60) NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX idx_sig_remb (remboursement_id),"
                + "INDEX idx_sig_id (signature_id)"
                + ")";
        String sqlReceipt = "CREATE TABLE IF NOT EXISTS recu_paiement ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "remboursement_id INT NOT NULL,"
                + "signature_id VARCHAR(120) NULL,"
                + "stripe_session_id VARCHAR(255) NULL,"
                + "report_html LONGTEXT NOT NULL,"
                + "file_path VARCHAR(1024) NULL,"
                + "file_type VARCHAR(32) NULL,"
                + "report_hash_sha256 VARCHAR(64) NULL,"
                + "generated_by_user_id INT NULL,"
                + "generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX idx_recu_remb (remboursement_id)"
                + ")";
        try (PreparedStatement pst1 = conn.prepareStatement(sqlSignature);
             PreparedStatement pst2 = conn.prepareStatement(sqlReceipt)) {
            pst1.executeUpdate();
            pst2.executeUpdate();
        }
        ensureColumnExists(conn, "signature_electronique", "signature_sha256", "VARCHAR(64) NULL");
        ensureColumnExists(conn, "recu_paiement", "file_type", "VARCHAR(32) NULL");
        ensureColumnExists(conn, "recu_paiement", "report_hash_sha256", "VARCHAR(64) NULL");
    }

    private void ensureEntrepreneurWorkspaceTables() throws SQLException {
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return;
        String notes = "CREATE TABLE IF NOT EXISTS entrepreneur_note ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "entrepreneur_user_id INT NOT NULL,"
                + "title VARCHAR(190) NULL,"
                + "content LONGTEXT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "INDEX idx_note_user (entrepreneur_user_id),"
                + "INDEX idx_note_updated (updated_at)"
                + ")";
        String calc = "CREATE TABLE IF NOT EXISTS entrepreneur_calc_history ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "entrepreneur_user_id INT NOT NULL,"
                + "scenario_label VARCHAR(190) NULL,"
                + "principal DOUBLE NOT NULL,"
                + "annual_rate_pct DOUBLE NOT NULL,"
                + "duration_months INT NOT NULL,"
                + "monthly_payment DOUBLE NOT NULL,"
                + "total_payment DOUBLE NOT NULL,"
                + "total_interest DOUBLE NOT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX idx_calc_user (entrepreneur_user_id),"
                + "INDEX idx_calc_created (created_at)"
                + ")";
        try (PreparedStatement p1 = conn.prepareStatement(notes);
             PreparedStatement p2 = conn.prepareStatement(calc)) {
            p1.executeUpdate();
            p2.executeUpdate();
        }
    }

    private int getCurrentEntrepreneurUserId() {
        try {
            var current = Session.getCurrentUser();
            if (current == null || current.getRole() == null) return 0;
            if (!"ENTREPRENEUR".equals(current.getRole().name())) return 0;
            return current.getId();
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveSignatureRecord(int remboursementId, String signatureId, String signerName, String signerEmail, String signatureImage, String apiStatus) throws SQLException {
        ensureSignatureReceiptTables();
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) throw new SQLException("Connexion DB indisponible (signature).");
        String sigHash = sha256Hex(signatureImage.getBytes(StandardCharsets.UTF_8));
        String sql = "INSERT INTO signature_electronique (remboursement_id, signature_id, signer_name, signer_email, signature_image, signature_sha256, api_status) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, remboursementId);
            pst.setString(2, signatureId);
            pst.setString(3, signerName);
            pst.setString(4, signerEmail);
            pst.setString(5, signatureImage);
            pst.setString(6, sigHash);
            pst.setString(7, apiStatus);
            int rows = pst.executeUpdate();
            if (rows <= 0) throw new SQLException("Echec enregistrement signature.");
        }
    }

    private void saveReceiptRecord(int remboursementId, String signatureId, String sessionId, String reportHtml, String filePath, String fileType, String reportHash, int generatedByUserId) throws SQLException {
        ensureSignatureReceiptTables();
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) throw new SQLException("Connexion DB indisponible (recu).");
        String sql = "INSERT INTO recu_paiement (remboursement_id, signature_id, stripe_session_id, report_html, file_path, file_type, report_hash_sha256, generated_by_user_id) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, remboursementId);
            pst.setString(2, signatureId);
            pst.setString(3, sessionId);
            pst.setString(4, reportHtml);
            pst.setString(5, filePath);
            pst.setString(6, fileType);
            pst.setString(7, reportHash);
            if (generatedByUserId > 0) pst.setInt(8, generatedByUserId);
            else pst.setNull(8, Types.INTEGER);
            int rows = pst.executeUpdate();
            if (rows <= 0) throw new SQLException("Echec enregistrement recu.");
        }
    }

    private File chooseReportFile(String suggestedName) {
        try {
            if (!Platform.isFxApplicationThread()) return null;
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer le rapport de paiement");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF (*.pdf)", "*.pdf"));
            chooser.setInitialFileName(suggestedName);
            Window owner = null;
            try {
                if (webView != null && webView.getScene() != null) {
                    owner = webView.getScene().getWindow();
                }
            } catch (Exception ignored) {
            }
            return chooser.showSaveDialog(owner);
        } catch (Exception e) {
            return null;
        }
    }

    private void writeReceiptPdf(File out, int remboursementId, int echeanceNo, String projectLabel, double montantDu, double montantPaye,
                                 String statut, String dateEcheance, String signatureId, String signerName,
                                 String signerEmail, String signatureImageDataUrl) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 42, 42);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();
            doc.add(new Paragraph("Recu de paiement - Investia", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            doc.add(new Paragraph("Date generation: " + new Timestamp(System.currentTimeMillis())));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{32f, 68f});
            addCell(table, "Recu N", true); addCell(table, String.valueOf(remboursementId), false);
            addCell(table, "Echeance N", true); addCell(table, echeanceNo > 0 ? String.valueOf(echeanceNo) : "N/A", false);
            addCell(table, "Projet", true); addCell(table, projectLabel, false);
            addCell(table, "Montant du", true); addCell(table, round2(montantDu) + " TND", false);
            addCell(table, "Montant paye", true); addCell(table, round2(montantPaye) + " TND", false);
            addCell(table, "Statut", true); addCell(table, statut, false);
            addCell(table, "Date echeance", true); addCell(table, dateEcheance, false);
            addCell(table, "Signature ID", true); addCell(table, signatureId.isEmpty() ? "N/A" : signatureId, false);
            addCell(table, "Signataire", true); addCell(table, signerName, false);
            addCell(table, "Email signataire", true); addCell(table, signerEmail.isEmpty() ? "N/A" : signerEmail, false);
            doc.add(table);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Signature electronique:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

            byte[] sigBytes = decodeDataImage(signatureImageDataUrl);
            if (sigBytes.length > 0) {
                Image sig = Image.getInstance(sigBytes);
                sig.scaleToFit(280f, 120f);
                doc.add(sig);
            } else {
                doc.add(new Paragraph("Non fournie"));
            }
            // Must close while stream is still open, otherwise PDF remains incomplete.
            doc.close();
        } catch (Exception e) {
            try {
                if (doc.isOpen()) doc.close();
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    private int resolveEcheanceNumber(Remboursement r) {
        if (r == null || r.getId() <= 0 || r.getFinancementId() <= 0 || r.getDateEcheance() == null) return 0;
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return 0;
        String sql = "SELECT COUNT(*) AS n FROM remboursement " +
                "WHERE financement_id=? AND (date_echeance < ? OR (date_echeance = ? AND id <= ?))";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, r.getFinancementId());
            pst.setDate(2, r.getDateEcheance());
            pst.setDate(3, r.getDateEcheance());
            pst.setInt(4, r.getId());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("n");
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static void addCell(PdfPTable table, String text, boolean key) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text));
        c.setPadding(7f);
        if (key) c.setBackgroundColor(new java.awt.Color(240, 244, 252));
        table.addCell(c);
    }

    private static byte[] decodeDataImage(String dataUrl) {
        String s = toStringOrEmptyStatic(dataUrl);
        if (!s.startsWith("data:image/")) return new byte[0];
        int comma = s.indexOf(',');
        if (comma <= 0 || comma >= s.length() - 1) return new byte[0];
        try {
            return Base64.getDecoder().decode(s.substring(comma + 1));
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static String sha256Hex(byte[] bytes) throws SQLException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes == null ? new byte[0] : bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new SQLException("SHA-256 indisponible: " + e.getMessage(), e);
        }
    }

    private static String sha256FileHex(File f) throws Exception {
        byte[] bytes = Files.readAllBytes(f.toPath());
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static void ensureColumnExists(Connection conn, String table, String column, String ddl) throws SQLException {
        if (conn == null) return;
        if (tableHasColumn(conn, table, column)) return;
        try (PreparedStatement pst = conn.prepareStatement("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl)) {
            pst.executeUpdate();
        }
    }

    private static boolean tableHasColumn(Connection conn, String table, String column) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static String toStringOrEmptyStatic(String s) {
        return s == null ? "" : s.trim();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void ensureMonthlyEcheancierExists(int financementId, double principal, double tauxPct, int dureeMois) {
        if (financementId <= 0) return;
        int months = dureeMois > 0 ? dureeMois : 12;

        int fkFinancementId;
        try {
            fkFinancementId = resolveRemboursementFinancementId(financementId);
        } catch (Exception e) {
            return;
        }

        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return;

        try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) AS c FROM remboursement WHERE financement_id=?")) {
            pst.setInt(1, fkFinancementId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next() && rs.getInt("c") > 0) {
                    return; // already exists
                }
            }
        } catch (SQLException ignored) {
            return;
        }

        double rate = Double.isFinite(tauxPct) ? Math.max(0.0, tauxPct) : 0.0;
        double principalSafe = Double.isFinite(principal) ? Math.max(0.0, principal) : 0.0;
        double totalWithInterest = principalSafe * (1.0 + rate / 100.0);
        double monthly = months > 0 ? (totalWithInterest / months) : totalWithInterest;
        if (!Double.isFinite(monthly) || monthly <= 0) monthly = 0.0;

        Calendar cal = Calendar.getInstance();
        Date today = new Date(System.currentTimeMillis());
        for (int i = 1; i <= months; i++) {
            cal.setTime(today);
            cal.add(Calendar.MONTH, i);
            Date dueDate = new Date(cal.getTimeInMillis());
            Remboursement rRow = new Remboursement();
            rRow.setFinancementId(fkFinancementId);
            rRow.setDateEcheance(dueDate);
            rRow.setMontantDu(round2(monthly));
            rRow.setMontantPaye(0.0);
            rRow.setStatut("EN_ATTENTE");
            try {
                remboursementCrud.ajouterOuModifierParFinancementEtDate(rRow);
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isProjectOwnedByCurrentEntrepreneur(int projectId) {
        try {
            var current = Session.getCurrentUser();
            if (current == null || current.getRole() == null) return false;
            if (!"ENTREPRENEUR".equals(current.getRole().name())) return false;
            List<Integer> ownerIds = getCurrentEntrepreneurOwnerIds();
            if (ownerIds.isEmpty()) return false;
            Projet p = projetCrud.getById(projectId);
            if (p == null) return false;
            return ownerIds.contains(p.getEntrepreneurId());
        } catch (Exception e) {
            return false;
        }
    }

    private List<Integer> getCurrentEntrepreneurOwnerIds() {
        List<Integer> ids = new ArrayList<>();
        try {
            var current = Session.getCurrentUser();
            if (current == null) return ids;
            if (current.getRole() == null || !"ENTREPRENEUR".equals(current.getRole().name())) return ids;

            ProfilEntrepreneur profil = profilCrud.getByUserId(current.getId());
            if (profil != null) {
                ids.add(profil.getIdEntrepreneur());
            }
            // Fallback for datasets where `projet.entrepreneur_id` stores direct user id.
            if (!ids.contains(current.getId())) {
                ids.add(current.getId());
            }
        } catch (Exception ignored) {
        }
        return ids;
    }

    private void ensureSchedulesForProject(int projectId) {
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return;

        String sql = "SELECT id_financement, montant, taux_interet_pct, duree_estimee_mois " +
                "FROM financement2 WHERE id_projet=? AND UPPER(COALESCE(statut,'')) IN ('CONFIRMED','EN_ATTENTE','PENDING')";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, projectId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int fid = rs.getInt("id_financement");
                    double montant = rs.getDouble("montant");
                    double taux = rs.getDouble("taux_interet_pct");
                    int duree = rs.getInt("duree_estimee_mois");
                    ensureMonthlyEcheancierExists(fid, montant, taux, duree);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void ensureSchedulesForEntrepreneur(List<Integer> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) return;
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return;
        String ownerCol = detectProjetOwnerColumn(conn);
        if (ownerCol == null) return;
        String inClause = buildInClause(ownerIds.size());
        String sql = "SELECT id_projet FROM projet WHERE " + ownerCol + " IN (" + inClause + ")";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : ownerIds) {
                pst.setInt(idx++, id);
            }
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int pid = rs.getInt("id_projet");
                    if (pid > 0) ensureSchedulesForProject(pid);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String detectProjetOwnerColumn(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> cols = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "projet", null)) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME"));
                }
            }
            if (cols.isEmpty()) return null;
            return firstExisting(cols,
                    "id_entrepreneur",
                    "entrepreneur_id",
                    "id_user",
                    "user_id",
                    "id_utilisateur",
                    "utilisateur_id");
        } catch (Exception e) {
            return null;
        }
    }

    private void ensureSchedulesForInvestor(List<Integer> investorKeys, ColumnPair cols) {
        if (investorKeys == null || investorKeys.isEmpty()) return;
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return;
        String inClause = buildInClause(investorKeys.size());

        String sql = "SELECT f.id_financement, f.montant, f.taux_interet_pct, f.duree_estimee_mois " +
                "FROM financement2 f JOIN investissement i ON f.id_investissement = i." + cols.idCol + " " +
                "WHERE i." + cols.investorCol + " IN (" + inClause + ") AND UPPER(COALESCE(f.statut,'')) IN ('CONFIRMED','EN_ATTENTE','PENDING')";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer key : investorKeys) {
                pst.setInt(idx++, key);
            }
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int fid = rs.getInt("id_financement");
                    double montant = rs.getDouble("montant");
                    double taux = rs.getDouble("taux_interet_pct");
                    int duree = rs.getInt("duree_estimee_mois");
                    ensureMonthlyEcheancierExists(fid, montant, taux, duree);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private int resolveRemboursementFinancementId(int financement2Id) throws SQLException {
        if (financement2Id <= 0) {
            throw new SQLException("ID financement invalide.");
        }
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) {
            throw new SQLException("Database connection is null.");
        }

        // If FK does not target legacy table, keep current id.
        if (!isRemboursementFkToLegacyFinancement(conn)) {
            return financement2Id;
        }

        String legacyIdCol = detectLegacyFinancementIdColumn(conn);
        if (legacyIdCol == null) {
            return financement2Id;
        }

        if (existsById(conn, "financement", legacyIdCol, financement2Id)) {
            return financement2Id;
        }

        Financement2 f2 = financementCrud.getById(financement2Id);
        if (f2 == null) {
            throw new SQLException("Financement2 introuvable (id_financement=" + financement2Id + ").");
        }

        tryInsertLegacyFinancement(conn, legacyIdCol, financement2Id, f2);

        if (existsById(conn, "financement", legacyIdCol, financement2Id)) {
            return financement2Id;
        }

        throw new SQLException("Impossible de synchroniser financement(id=" + financement2Id + ") pour la FK remboursement.");
    }

    private boolean isRemboursementFkToLegacyFinancement(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getImportedKeys(conn.getCatalog(), null, "remboursement")) {
                while (rs.next()) {
                    String fkCol = rs.getString("FKCOLUMN_NAME");
                    String pkTable = rs.getString("PKTABLE_NAME");
                    if (fkCol != null && pkTable != null
                            && fkCol.equalsIgnoreCase("financement_id")
                            && pkTable.equalsIgnoreCase("financement")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private String detectLegacyFinancementIdColumn(Connection conn) {
        return detectTableIdColumn(conn, "financement", "id", "id_financement", "financement_id");
    }

    private boolean existsById(Connection conn, String table, String idCol, int idValue) {
        String sql = "SELECT 1 FROM `" + table + "` WHERE `" + idCol + "`=? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idValue);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void tryInsertLegacyFinancement(Connection conn, String idCol, int idValue, Financement2 f2) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        List<ColumnDef> cols = new ArrayList<>();
        Map<String, String> columnTypeByName = loadColumnTypes(conn, "financement");
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "financement", null)) {
            while (rs.next()) {
                ColumnDef c = new ColumnDef();
                c.name = rs.getString("COLUMN_NAME");
                c.type = rs.getInt("DATA_TYPE");
                c.typeName = rs.getString("TYPE_NAME");
                c.columnType = columnTypeByName.get(c.name.toLowerCase(Locale.ROOT));
                c.nullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                c.defaultExpr = rs.getString("COLUMN_DEF");
                String auto = null;
                try {
                    auto = rs.getString("IS_AUTOINCREMENT");
                } catch (Exception ignored) {
                }
                c.autoIncrement = "YES".equalsIgnoreCase(auto);
                cols.add(c);
            }
        }
        if (cols.isEmpty()) {
            throw new SQLException("Table financement introuvable ou sans colonnes.");
        }

        Map<String, Object> values = new HashMap<>();
        Timestamp nowTs = new Timestamp(System.currentTimeMillis());
        Date nowDate = new Date(System.currentTimeMillis());

        for (ColumnDef c : cols) {
            String col = c.name;
            String key = col.toLowerCase(Locale.ROOT);

            if (key.equals(idCol.toLowerCase(Locale.ROOT))) {
                values.put(col, idValue);
                continue;
            }

            Object mapped = null;
            if (key.equals("id_projet") || key.equals("projet_id")) mapped = f2.getId_projet();
            else if (key.equals("id_investissement") || key.equals("investissement_id")) mapped = f2.getId_investissement();
            else if (key.equals("montant")) mapped = f2.getMontant();
            else if (key.equals("frais_pct")) mapped = f2.getFrais_pct();
            else if (key.equals("mode_paiement")) mapped = f2.getMode_paiement();
            else if (key.equals("statut")) mapped = resolveLegacyFinancementStatusValue(c, f2);
            else if (key.equals("taux_interet_pct")) mapped = f2.getTaux_interet_pct();
            else if (key.equals("duree_estimee_mois")) mapped = f2.getDuree_estimee_mois();
            else if (key.equals("note")) mapped = f2.getNote();
            else if (key.equals("type")) mapped = resolveLegacyTypeValue(c, f2);
            else if (key.equals("created_at")) mapped = nowTs;
            else if (key.equals("updated_at")) mapped = nowTs;
            else if (key.equals("date") || key.equals("date_financement")) mapped = nowDate;

            if (mapped != null) {
                values.put(col, mapped);
                continue;
            }

            if (!c.nullable && c.defaultExpr == null && !c.autoIncrement) {
                values.put(col, fallbackValueForColumn(c, key, nowDate, nowTs, f2));
            }
        }

        if (!values.containsKey(idCol)) {
            values.put(idCol, idValue);
        }

        List<String> insertCols = new ArrayList<>(values.keySet());
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO `financement` (");
        for (int i = 0; i < insertCols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("`").append(insertCols.get(i)).append("`");
        }
        sql.append(") VALUES (");
        for (int i = 0; i < insertCols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < insertCols.size(); i++) {
                String col = insertCols.get(i);
                Object v = values.get(col);
                if (v == null) {
                    pst.setNull(i + 1, Types.NULL);
                } else if (v instanceof Integer) {
                    pst.setInt(i + 1, (Integer) v);
                } else if (v instanceof Long) {
                    pst.setLong(i + 1, (Long) v);
                } else if (v instanceof Double) {
                    pst.setDouble(i + 1, (Double) v);
                } else if (v instanceof Float) {
                    pst.setFloat(i + 1, (Float) v);
                } else if (v instanceof Boolean) {
                    pst.setBoolean(i + 1, (Boolean) v);
                } else if (v instanceof Date) {
                    pst.setDate(i + 1, (Date) v);
                } else if (v instanceof Timestamp) {
                    pst.setTimestamp(i + 1, (Timestamp) v);
                } else {
                    pst.setString(i + 1, String.valueOf(v));
                }
            }
            pst.executeUpdate();
        }
    }

    private static Object fallbackValueForColumn(ColumnDef col, String key, Date nowDate, Timestamp nowTs, Financement2 f2) {
        List<String> enumValues = getEnumOrSetValues(col);
        if (!enumValues.isEmpty()) {
            if ("type".equals(key)) {
                Object resolved = resolveLegacyTypeValue(col, f2);
                if (resolved != null) return resolved;
            }
            String matched = firstMatchingEnumValue(enumValues,
                    f2 == null ? null : f2.getMode_paiement(),
                    f2 == null ? null : f2.getStatut(),
                    "FINANCEMENT",
                    "INVESTISSEMENT",
                    "STANDARD",
                    "EN_ATTENTE",
                    "PAYE");
            return matched == null ? enumValues.get(0) : matched;
        }

        int sqlType = col == null ? Types.VARCHAR : col.type;
        switch (sqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.BIGINT:
                return 0;
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                return 0.0;
            case Types.BIT:
            case Types.BOOLEAN:
                return false;
            case Types.DATE:
                return nowDate;
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return nowTs;
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                return "00:00:00";
            default:
                if ("type".equals(key)) {
                    return "FINANCEMENT";
                }
                return "";
        }
    }

    private static Object resolveLegacyTypeValue(ColumnDef col, Financement2 f2) {
        int sqlType = col == null ? Types.VARCHAR : col.type;
        switch (sqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.BIGINT:
                return 0;
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                return 0.0;
            case Types.BIT:
            case Types.BOOLEAN:
                return false;
            default:
                break;
        }

        List<String> enumValues = getEnumOrSetValues(col);
        String mode = f2 == null ? null : f2.getMode_paiement();
        String statut = f2 == null ? null : f2.getStatut();
        String preferred = firstMatchingEnumValue(enumValues,
                mode,
                statut,
                "FINANCEMENT",
                "INVESTISSEMENT",
                "PROJET",
                "STANDARD",
                "CARTE_BANCAIRE",
                "VIREMENT",
                "WALLET",
                "EN_ATTENTE",
                "PAYE");
        if (preferred != null) return preferred;
        if (!enumValues.isEmpty()) return enumValues.get(0);
        if (mode != null && !mode.trim().isEmpty()) return mode.trim();
        return "FINANCEMENT";
    }

    private static Object resolveLegacyFinancementStatusValue(ColumnDef col, Financement2 f2) {
        int sqlType = col == null ? Types.VARCHAR : col.type;
        String normalized = normalizeFinancementStatus(f2 == null ? null : f2.getStatut());
        if (sqlType == Types.INTEGER || sqlType == Types.SMALLINT || sqlType == Types.TINYINT || sqlType == Types.BIGINT) {
            return "CONFIRMED".equalsIgnoreCase(normalized) ? 1 : 0;
        }

        List<String> enumValues = getEnumOrSetValues(col);
        if (!enumValues.isEmpty()) {
            String candidate = firstMatchingEnumValue(enumValues,
                    normalized,
                    "CONFIRMED",
                    "EN_ATTENTE",
                    "ANNULE",
                    "REFUSE",
                    "PENDING",
                    "PAYE",
                    "PAID",
                    "ACCEPTE",
                    "VALIDE");
            return candidate == null ? enumValues.get(0) : candidate;
        }

        return normalized;
    }

    private static List<String> getEnumOrSetValues(ColumnDef col) {
        if (col == null) return new ArrayList<>();
        List<String> values = parseEnumOrSetValues(col.columnType);
        if (!values.isEmpty()) return values;
        return parseEnumOrSetValues(col.typeName);
    }

    private static String firstMatchingEnumValue(List<String> enumValues, String... candidates) {
        if (enumValues == null || enumValues.isEmpty() || candidates == null) return null;
        for (String candidate : candidates) {
            String token = canonicalToken(candidate);
            if (token.isEmpty()) continue;
            for (String v : enumValues) {
                if (canonicalToken(v).equals(token)) return v;
            }
        }
        return null;
    }

    private static String canonicalToken(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch)) out.append(ch);
        }
        return out.toString();
    }

    private static List<String> parseEnumOrSetValues(String typeName) {
        List<String> values = new ArrayList<>();
        if (typeName == null) return values;
        String t = typeName.trim();
        String lower = t.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("enum(") || lower.startsWith("set("))) return values;

        boolean inQuote = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == '\'') {
                if (inQuote && i + 1 < t.length() && t.charAt(i + 1) == '\'') {
                    current.append('\'');
                    i++;
                    continue;
                }
                inQuote = !inQuote;
                if (!inQuote) {
                    values.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            if (inQuote) current.append(ch);
        }
        return values;
    }

    private static Map<String, String> loadColumnTypes(Connection conn, String table) {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, table);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String type = rs.getString("COLUMN_TYPE");
                    if (name != null) map.put(name.toLowerCase(Locale.ROOT), type);
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private List<Integer> resolveInvestorLookupValues(Connection conn, ColumnPair cols, int currentUserId) {
        LinkedHashSet<Integer> keys = new LinkedHashSet<>();
        if (currentUserId > 0) keys.add(currentUserId);
        try {
            String investorIdCol = detectTableIdColumn(conn, "investisseur", "id_investisseur", "id", "investisseur_id");
            String investorUserCol = detectTableIdColumn(conn, "investisseur", "id_user", "user_id", "id_utilisateur", "utilisateur_id");
            if (investorIdCol != null && investorUserCol != null) {
                String sql = "SELECT " + investorIdCol + " AS investorKey FROM investisseur WHERE " + investorUserCol + "=?";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setInt(1, currentUserId);
                    try (ResultSet rs = pst.executeQuery()) {
                        while (rs.next()) {
                            int key = rs.getInt("investorKey");
                            if (key > 0) keys.add(key);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(keys);
    }

    private static String detectTableIdColumn(Connection conn, String table, String... candidates) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> cols = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, null)) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME"));
                }
            }
            if (cols.isEmpty()) return null;
            return firstExisting(cols, candidates);
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildInClause(int size) {
        if (size <= 0) return "NULL";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private void sendResultToJs(String fn, String json) {
        if (webView == null) return;
        String safeJsonArg = json == null ? "null" : json;
        Platform.runLater(() -> {
            try {
                if (webView.getScene() == null) return;
                WebEngine engine = webView.getEngine();
                if (engine == null) return;
                String location = engine.getLocation();
                if (location == null || location.isEmpty()) return;
                engine.executeScript("if (window && typeof window." + fn + " === 'function') window." + fn + "(" + safeJsonArg + ");");
            } catch (Throwable ignored) {
            }
        });
    }

    private static String ok() {
        return "{\"ok\":true}";
    }

    private static String err(String message) {
        String m = message == null ? "Erreur" : message;
        return "{\"ok\":false,\"message\":" + ProjetWebUtils.jsonString(m) + "}";
    }

    private static String normalizeFinancementStatus(String raw) {
        if (raw == null) return "EN_ATTENTE";
        String v = raw.trim();
        if (v.equalsIgnoreCase("PENDING")) return "EN_ATTENTE";
        if (v.isEmpty()) return "EN_ATTENTE";
        return v;
    }

    private static String normalizeRemboursementStatus(String raw, double du, double paye) {
        if (du > 0 && paye + 1e-9 >= du) return "PAYE";
        if (raw == null || raw.trim().isEmpty()) return "EN_ATTENTE";
        String v = raw.trim();
        return v.equalsIgnoreCase("PAYE") ? "PAYE" : "EN_ATTENTE";
    }

    private static String financementToJson(Financement2 f) {
        String date = "";
        if (f.getCreated_at() != null) {
            date = ProjetWebUtils.formatTimestamp(f.getCreated_at());
        } else if (f.getUpdated_at() != null) {
            date = ProjetWebUtils.formatTimestamp(f.getUpdated_at());
        }
        return "{"
                + "\"id\":" + f.getId_financement()
                + ",\"projectId\":" + f.getId_projet()
                + ",\"investId\":" + f.getId_investissement()
                + ",\"amount\":" + f.getMontant()
                + ",\"feesPct\":" + f.getFrais_pct()
                + ",\"status\":" + ProjetWebUtils.jsonString(normalizeFinancementStatus(f.getStatut()))
                + ",\"method\":" + ProjetWebUtils.jsonString(f.getMode_paiement())
                + ",\"rate\":" + f.getTaux_interet_pct()
                + ",\"duration\":" + f.getDuree_estimee_mois()
                + ",\"note\":" + ProjetWebUtils.jsonString(f.getNote())
                + ",\"date\":" + ProjetWebUtils.jsonString(date)
                + "}";
    }

    private static String remboursementsToJson(List<Remboursement> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(remboursementToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String remboursementToJson(Remboursement r) {
        String date = r.getDateEcheance() == null ? "" : r.getDateEcheance().toString();
        return "{"
                + "\"id\":" + r.getId()
                + ",\"financementId\":" + r.getFinancementId()
                + ",\"dateEcheance\":" + ProjetWebUtils.jsonString(date)
                + ",\"montantDu\":" + r.getMontantDu()
                + ",\"montantPaye\":" + r.getMontantPaye()
                + ",\"statut\":" + ProjetWebUtils.jsonString(r.getStatut())
                + "}";
    }

    private static Map<String, String> parseFormData(String payload) {
        Map<String, String> map = new HashMap<>();
        if (payload == null || payload.trim().isEmpty()) return map;
        String[] pairs = payload.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            int idx = pair.indexOf('=');
            String k = idx >= 0 ? pair.substring(0, idx) : pair;
            String v = idx >= 0 ? pair.substring(idx + 1) : "";
            String key = urlDecode(k);
            String val = urlDecode(v);
            map.put(key, val);
        }
        return map;
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static int toInt(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        return Integer.parseInt(s.trim());
    }

    private static double toDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        return Double.parseDouble(s.trim());
    }

    private static String toStringOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String config(String key) {
        String env = toStringOrEmpty(System.getenv(key));
        if (!env.isEmpty()) return env.toLowerCase(Locale.ROOT);
        String sys = toStringOrEmpty(System.getProperty(key));
        if (!sys.isEmpty()) return sys.toLowerCase(Locale.ROOT);
        return toStringOrEmpty(LOCAL_CONFIG.getProperty(key)).toLowerCase(Locale.ROOT);
    }

    private static Properties loadLocalConfig() {
        Properties p = new Properties();
        try (var in = FinancementBridgeController.class.getClassLoader().getResourceAsStream("payment.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {
        }
        try {
            Path external = Paths.get("payment.properties");
            if (Files.exists(external)) {
                try (var in = Files.newInputStream(external)) {
                    p.load(in);
                }
            }
        } catch (Exception ignored) {
        }
        return p;
    }

    private static String extractJsonField(String json, String field) {
        if (json == null || field == null || field.isEmpty()) return "";
        String marker = "\"" + field + "\"";
        int i = json.indexOf(marker);
        if (i < 0) return "";
        int colon = json.indexOf(':', i + marker.length());
        if (colon < 0) return "";
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return "";
        int end = json.indexOf('"', start + 1);
        if (end < 0) return "";
        return json.substring(start + 1, end);
    }

    private static List<Map<String, String>> parseCalendarRows(String jsonArray) {
        List<Map<String, String>> out = new ArrayList<>();
        String s = toStringOrEmptyStatic(jsonArray);
        if (s.isEmpty() || "[]".equals(s)) return out;
        int i = 0;
        while (i < s.length()) {
            int start = s.indexOf('{', i);
            if (start < 0) break;
            int depth = 0;
            int end = -1;
            for (int j = start; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) { end = j; break; }
                }
            }
            if (end < 0) break;
            String obj = s.substring(start, end + 1);
            Map<String, String> m = new HashMap<>();
            m.put("id", extractJsonField(obj, "id"));
            m.put("date", extractJsonField(obj, "date"));
            m.put("status", extractJsonField(obj, "status"));
            m.put("project", extractJsonField(obj, "project"));
            m.put("montantDu", extractJsonNumberField(obj, "montantDu"));
            m.put("montantPaye", extractJsonNumberField(obj, "montantPaye"));
            out.add(m);
            i = end + 1;
        }
        return out;
    }

    private Map<String, MsEventLite> parseMicrosoftEventsByRid(String json) {
        Map<String, MsEventLite> out = new HashMap<>();
        String s = toStringOrEmptyStatic(json);
        if (s.isEmpty()) return out;
        int valuesIdx = s.indexOf("\"value\"");
        if (valuesIdx < 0) return out;
        int arrStart = s.indexOf('[', valuesIdx);
        if (arrStart < 0) return out;
        int i = arrStart;
        while (i < s.length()) {
            int start = s.indexOf('{', i);
            if (start < 0) break;
            int depth = 0;
            int end = -1;
            for (int j = start; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { end = j; break; }
                }
                if (c == ']' && depth == 0) break;
            }
            if (end < 0) break;
            String obj = s.substring(start, end + 1);
            String subject = extractJsonField(obj, "subject");
            String rid = extractRid(subject);
            if (!rid.isEmpty()) {
                String eventId = extractJsonField(obj, "id");
                String bodyPreview = extractJsonField(obj, "bodyPreview");
                String date = extractFirstDateIso(obj);
                String status = subject.toUpperCase(Locale.ROOT).contains("[PAYE]") ? "PAYE" : "EN_ATTENTE";
                String paye = extractMetric(bodyPreview, 1);
                String du = extractMetric(bodyPreview, 2);
                String project = subject.replaceAll("(?i)\\[INVESTIA\\]|\\[PAYE\\]|\\[EN_ATTENTE\\]|\\[RID:\\d+\\]", "").trim();
                MsEventLite e = new MsEventLite(eventId, buildSyncSignature(date, status, paye, du, project));
                out.put(rid, e);
            }
            i = end + 1;
        }
        return out;
    }

    private static String extractRid(String subject) {
        String s = toStringOrEmptyStatic(subject);
        int i = s.toUpperCase(Locale.ROOT).indexOf("[RID:");
        if (i < 0) return "";
        int j = s.indexOf("]", i);
        if (j < 0) return "";
        String p = s.substring(i + 5, j).trim();
        for (int k = 0; k < p.length(); k++) {
            if (!Character.isDigit(p.charAt(k))) return "";
        }
        return p;
    }

    private static String extractFirstDateIso(String objJson) {
        String dt = extractJsonField(objJson, "dateTime");
        if (!dt.isEmpty() && dt.length() >= 10) return dt.substring(0, 10);
        String d = extractJsonField(objJson, "date");
        if (!d.isEmpty() && d.length() >= 10) return d.substring(0, 10);
        return "";
    }

    private static String extractMetric(String bodyPreview, int which) {
        String s = toStringOrEmptyStatic(bodyPreview);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Montant\\s+paye:\\s*([0-9.]+)\\s*/\\s*([0-9.]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(s);
        if (!m.find()) return "";
        return which == 2 ? toStringOrEmptyStatic(m.group(2)) : toStringOrEmptyStatic(m.group(1));
    }

    private static String buildSyncSignature(String date, String status, String paye, String du, String project) {
        return toStringOrEmptyStatic(date) + "|"
                + toStringOrEmptyStatic(status).toUpperCase(Locale.ROOT) + "|"
                + toStringOrEmptyStatic(paye) + "|"
                + toStringOrEmptyStatic(du) + "|"
                + toStringOrEmptyStatic(project).toUpperCase(Locale.ROOT);
    }

    private static String extractJsonNumberField(String json, String field) {
        if (json == null || field == null || field.isEmpty()) return "";
        String marker = "\"" + field + "\"";
        int i = json.indexOf(marker);
        if (i < 0) return "";
        int colon = json.indexOf(':', i + marker.length());
        if (colon < 0) return "";
        int j = colon + 1;
        while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
        int k = j;
        while (k < json.length()) {
            char c = json.charAt(k);
            if (!(Character.isDigit(c) || c == '-' || c == '+' || c == '.')) break;
            k++;
        }
        if (k <= j) return "";
        return json.substring(j, k).trim();
    }

    private static ColumnPair detectInvestissementColumns(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        List<String> cols = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "investissement", null)) {
            while (rs.next()) {
                cols.add(rs.getString("COLUMN_NAME"));
            }
        }
        if (cols.isEmpty()) return null;

        String idCol = firstExisting(cols, "id_investissement", "id", "investissement_id");
        String investorCol = firstExisting(cols, "id_investisseur", "investisseur_id", "investor_id", "idInvestor", "investorId", "id_user", "user_id", "id_utilisateur", "utilisateur_id");
        String projectCol = firstExisting(cols, "id_projet", "projet_id", "project_id", "idProjet");
        String montantCol = firstExisting(cols, "montant", "amount");
        String dateCol = firstExisting(cols, "date_investissement", "date", "created_at", "date_financement");
        if (idCol == null || investorCol == null) return null;
        return new ColumnPair(idCol, investorCol, projectCol, montantCol, dateCol);
    }

    private static String firstExisting(List<String> cols, String... candidates) {
        for (String c : candidates) {
            for (String col : cols) {
                if (col.equalsIgnoreCase(c)) return col;
            }
        }
        return null;
    }

    private static final class ColumnPair {
        final String idCol;
        final String investorCol;
        final String projectCol;
        final String montantCol;
        final String dateCol;

        ColumnPair(String idCol, String investorCol, String projectCol, String montantCol, String dateCol) {
            this.idCol = idCol;
            this.investorCol = investorCol;
            this.projectCol = projectCol;
            this.montantCol = montantCol;
            this.dateCol = dateCol;
        }
    }

    private static final class ColumnDef {
        String name;
        int type;
        String typeName;
        String columnType;
        boolean nullable;
        String defaultExpr;
        boolean autoIncrement;
    }

    private static final class InvestissementItem {
        final int id;
        final int investorId;

        InvestissementItem(int id, int investorId) {
            this.id = id;
            this.investorId = investorId;
        }
    }

    private static final class MsEventLite {
        final String eventId;
        final String signature;

        MsEventLite(String eventId, String signature) {
            this.eventId = toStringOrEmptyStatic(eventId);
            this.signature = toStringOrEmptyStatic(signature);
        }
    }
}

