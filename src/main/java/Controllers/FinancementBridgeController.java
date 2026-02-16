package Controllers;

import Entities.Financement2;
import Entities.ProfilEntrepreneur;
import Entities.Projet;
import Entities.Remboursement;
import Services.Financement2CRUD;
import Services.ProfilEntrepreneurCRUD;
import Services.ProjetCRUD;
import Services.RemboursementCRUD;
import Utils.MyBD;
import Utils.Session;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
import javafx.scene.web.WebEngine;
import javafx.application.Platform;
import javafx.scene.web.WebView;

public class FinancementBridgeController {
    private final WebView webView;
    private final ProjetCRUD projetCrud;
    private final ProfilEntrepreneurCRUD profilCrud;
    private final Financement2CRUD financementCrud;
    private final RemboursementCRUD remboursementCrud;

    public FinancementBridgeController(ProjetWebContext context) {
        this.webView = context.getWebView();
        this.projetCrud = context.getProjetCrud();
        this.profilCrud = context.getProfilCrud();
        this.financementCrud = new Financement2CRUD();
        this.remboursementCrud = new RemboursementCRUD();
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

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) sb.append(",");
                InvestissementItem it = items.get(i);
                sb.append("{\"id\":").append(it.id).append(",\"investorId\":").append(it.investorId).append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
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
                    java.sql.Date nextDue = null;
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
                            java.sql.Timestamp c = rs.getTimestamp("created_at");
                            if (c != null) created = ProjetWebUtils.formatTimestamp(c);
                        } catch (Exception ignored) {
                        }
                        if (created.isEmpty()) {
                            try {
                                java.sql.Timestamp u = rs.getTimestamp("updated_at");
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
                if (d instanceof java.sql.Date) date = ((java.sql.Date) d).toString();
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

            Financement2 f = new Financement2(projectId, investissementId, montant, fraisPct, mode, statut, taux, duree, note);
            int newId = financementCrud.ajouterWithGeneratedId(f);
            if ("CONFIRMED".equalsIgnoreCase(statut) && newId > 0) {
                ensureMonthlyEcheancierExists(newId, montant, taux, duree);
            }
            result = ok();
        } catch (Throwable e) {
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

            Financement2 f = new Financement2(id, projectId, investissementId, montant, fraisPct, mode, statut, taux, duree, note);
            financementCrud.modifier(f);
            if ("CONFIRMED".equalsIgnoreCase(statut) && id > 0) {
                ensureMonthlyEcheancierExists(id, montant, taux, duree);
            }
            result = ok();
        } catch (Throwable e) {
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
            result = ok();
        } catch (Throwable e) {
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
            java.sql.Date dateEcheance = ProjetWebUtils.parseSqlDate(params.get("dateEcheance"));
            double montantDu = toDouble(params.get("montantDu"));
            double montantPaye = toDouble(params.get("montantPaye"));
            String statut = normalizeRemboursementStatus(params.get("statut"), montantDu, montantPaye);

            Remboursement r = new Remboursement(fkFinancementId, dateEcheance, montantDu, montantPaye, statut);
            remboursementCrud.ajouterOuModifierParFinancementEtDate(r);
            result = ok();
        } catch (Throwable e) {
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
            java.sql.Date dateEcheance = ProjetWebUtils.parseSqlDate(params.get("dateEcheance"));
            double montantDu = toDouble(params.get("montantDu"));
            double montantPaye = toDouble(params.get("montantPaye"));
            String statut = normalizeRemboursementStatus(params.get("statut"), montantDu, montantPaye);

            Remboursement r = new Remboursement(id, financementId, dateEcheance, montantDu, montantPaye, statut);
            remboursementCrud.modifier(r);
            result = ok();
        } catch (Throwable e) {
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
            result = ok();
        } catch (Throwable e) {
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
            remboursementCrud.ajouterPaiement(id, amount);
            result = ok();
        } catch (Throwable e) {
            result = err(e.getMessage());
        }
        sendResultToJs("onPayResult", result);
        return result;
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
        double total = principal * (1.0 + (rate / 100.0));
        if (!Double.isFinite(total) || total <= 0) total = Math.max(0.0, principal);

        // First due date: first day of next month
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);

        // Split into monthly dues, last installment adjusts rounding
        double base = round2(total / months);
        double remaining = round2(total);

        for (int i = 0; i < months; i++) {
            Calendar due = (Calendar) cal.clone();
            due.add(Calendar.MONTH, i);
            java.sql.Date date = new java.sql.Date(due.getTimeInMillis());

            double amountDu = (i == months - 1) ? round2(remaining) : base;
            remaining = round2(remaining - amountDu);

            Remboursement r = new Remboursement(fkFinancementId, date, amountDu, 0.0, "EN_ATTENTE");
            try {
                remboursementCrud.ajouter(r);
            } catch (SQLException ignored) {
                // best-effort; partial schedule is still better than none
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
        if (idCol == null || investorCol == null) return null;
        return new ColumnPair(idCol, investorCol);
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

        ColumnPair(String idCol, String investorCol) {
            this.idCol = idCol;
            this.investorCol = investorCol;
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
}
