package Controllers;

import Entities.*;
import Services.*;
import Utils.Session;
import Utils.sceneManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardBridge {

    private final UserCRUD userCRUD = new UserCRUD();
    private final ProjetCRUD projetCRUD = new ProjetCRUD();
    private final EvenementCRUD evenementCRUD = new EvenementCRUD();
    private final ModerationActionCRUD historyCRUD = new ModerationActionCRUD();
    private final ProfilEntrepreneurCRUD profilCRUD = new ProfilEntrepreneurCRUD();

    public String ping() {
        return "OK_PING";
    }

    // =========================
    // JSON
    // =========================
    public String getPendingAccountsJson() { return wrapJson(() -> toPendingAccountsJson(userCRUD.getPendingAccounts())); }
    public String getAllUsersJson()        { return wrapJson(() -> toUsersJson(userCRUD.getAllUsers())); }
    public String getPendingProjectsJson() { return wrapJson(() -> toProjectsJson(projetCRUD.getPendingProjects())); }
    public String getAllProjectsJson()     { return wrapJson(() -> toProjectsJson(projetCRUD.afficher())); }
    public String getPendingEventsJson()   { return wrapJson(() -> toEventsJson(evenementCRUD.getPendingEvents())); }
    public String getAllEventsJson()       { return wrapJson(() -> toEventsJson(evenementCRUD.getAll())); }
    public String getHistoryJson()         { return wrapJson(() -> toHistoryJson(historyCRUD.getAll())); }

    // =========================
    // Comptes
    // =========================
    public String acceptAccount(int userId) {
        return wrapOk(() -> {
            userCRUD.acceptAccount(userId);
            try { profilCRUD.updateVerificationByUserId(userId, StatutVerification.VERIFIE); } catch (Exception ignored) {}
            safeLog(TargetType.COMPTE, userId, Decision.VALIDER, "ACCEPT_ACCOUNT");
        });
    }

    public String rejectAccount(int userId) {
        return wrapOk(() -> {
            userCRUD.setVerificationStatus(userId, StatutVerification.NON_VERIFIE, false);
            try { profilCRUD.updateVerificationByUserId(userId, StatutVerification.NON_VERIFIE); } catch (Exception ignored) {}
            safeLog(TargetType.COMPTE, userId, Decision.REFUSER, "REJECT_ACCOUNT_TO_NON_VERIFIE");
        });
    }

    // =========================
    // Users
    // =========================
    public String createUser(String nom, String prenom, String email, String telephone,
                             String cin, String role, String statut_verification, String mot_de_passe) {
        return wrapOk(() -> {
            // ✅ selon ta table: est_actif existe et default=1, mais on le force à 1 proprement
            userCRUD.createUserAdmin(nom, prenom, email, telephone, cin, role, statut_verification, mot_de_passe, 1);
            safeLog(TargetType.USER, 0, Decision.VALIDER, "CREATE_USER");
        });
    }

    public String deleteUser(int userId) {
        return wrapOk(() -> {
            userCRUD.deleteUser(userId);
            safeLog(TargetType.USER, userId, Decision.REFUSER, "DELETE_USER");
        });
    }

    public String updateUser(int id, String nom, String prenom, String email, String telephone,
                             String cin, String role, String statutVerification) {
        return wrapOk(() -> {
            userCRUD.updateUserAdminFields(id, nom, prenom, email, telephone, cin, role, statutVerification);
            safeLog(TargetType.USER, id, Decision.VALIDER, "UPDATE_USER");
        });
    }

    // =========================
    // Projets
    // =========================
    public String acceptProject(int idProjet) {
        return wrapOk(() -> {
            projetCRUD.acceptProject(idProjet);
            safeLog(TargetType.PROJET, idProjet, Decision.VALIDER, "ACCEPT_PROJECT");
        });
    }

    public String rejectProject(int idProjet) {
        return wrapOk(() -> {
            projetCRUD.rejectProject(idProjet);
            safeLog(TargetType.PROJET, idProjet, Decision.REFUSER, "REJECT_PROJECT");
        });
    }

    public String deleteProject(int idProjet) {
        return wrapOk(() -> {
            projetCRUD.deleteProject(idProjet);
            safeLog(TargetType.PROJET, idProjet, Decision.REFUSER, "DELETE_PROJECT");
        });
    }

    // =========================
    // Events
    // =========================
    public String acceptEvent(int idEvent) {
        return wrapOk(() -> {
            evenementCRUD.acceptEvent(idEvent);
            safeLog(TargetType.EVENEMENT, idEvent, Decision.VALIDER, "ACCEPT_EVENT");
        });
    }

    public String rejectEvent(int idEvent) {
        return wrapOk(() -> {
            evenementCRUD.rejectEvent(idEvent);
            safeLog(TargetType.EVENEMENT, idEvent, Decision.REFUSER, "REJECT_EVENT");
        });
    }

    public String deleteEvent(int idEvent) {
        return wrapOk(() -> {
            evenementCRUD.deleteEvent(idEvent);
            safeLog(TargetType.EVENEMENT, idEvent, Decision.REFUSER, "DELETE_EVENT");
        });
    }

    // =========================
    // Logout
    // =========================
    public void logout() {
        Session.setCurrentUser(null);
        WebAuthController.openLoginOnNextLoad();
        sceneManager.switchTo("/web_auth.fxml", "Investia - Connexion");
    }

    // =========================
    // LOG
    // =========================
    private void safeLog(TargetType targetType, int targetId, Decision decision, String details) {
        try {
            User admin = Session.getCurrentUser();
            int adminId = (admin != null) ? admin.getId() : 0;
            historyCRUD.logAction(adminId, decision.name(), targetType.name(), targetId, details);
        } catch (Exception ignored) {}
    }

    // =========================
    // JSON builders
    // =========================
    private String toPendingAccountsJson(List<User> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            User u = list.get(i);
            sb.append("{")
                    .append("\"id\":").append(u.getId()).append(",")
                    .append("\"email\":\"").append(escape(u.getEmail())).append("\",")
                    .append("\"role\":\"").append(u.getRole() != null ? u.getRole().name() : "").append("\",")
                    .append("\"active\":").append(u.isActive()).append(",")
                    .append("\"statutVerification\":\"").append(u.getStatutVerification() != null ? u.getStatutVerification().name() : "").append("\"")
                    .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private String toUsersJson(List<User> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            User u = list.get(i);
            sb.append("{")
                    .append("\"id\":").append(u.getId()).append(",")
                    .append("\"nom\":\"").append(escape(u.getNom())).append("\",")
                    .append("\"prenom\":\"").append(escape(u.getPrenom())).append("\",")
                    .append("\"email\":\"").append(escape(u.getEmail())).append("\",")
                    .append("\"telephone\":\"").append(escape(u.getTelephone())).append("\",")
                    .append("\"cin\":\"").append(escape(u.getCin())).append("\",")
                    .append("\"role\":\"").append(u.getRole() != null ? u.getRole().name() : "").append("\",")
                    .append("\"statutVerification\":\"").append(u.getStatutVerification() != null ? u.getStatutVerification().name() : "").append("\"")
                    .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private String toProjectsJson(List<Projet> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Projet p = list.get(i);
            sb.append("{")
                    .append("\"id\":").append(p.getIdProjet()).append(",")
                    .append("\"titre\":\"").append(escape(p.getTitre())).append("\",")
                    .append("\"secteur\":\"").append(escape(p.getSecteur())).append("\",")
                    .append("\"statut\":\"").append(p.getStatut() != null ? p.getStatut() : "").append("\"")
                    .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private String toEventsJson(List<Evenement> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Evenement e = list.get(i);
            sb.append("{")
                    .append("\"id\":").append(e.getId()).append(",")
                    .append("\"titre\":\"").append(escape(e.getTitre())).append("\",")
                    .append("\"mode\":\"").append(e.getMode() != null ? e.getMode().name() : "").append("\",")
                    .append("\"dateDebut\":\"").append(ldt(e.getDateDebut())).append("\",")
                    .append("\"statut\":\"").append(e.getStatut() != null ? e.getStatut().name() : "").append("\"")
                    .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private String toHistoryJson(List<ModerationAction> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            ModerationAction h = list.get(i);
            sb.append("{")
                    .append("\"id\":").append(h.getId()).append(",")
                    .append("\"action\":\"").append(escape(h.getAction())).append("\",")
                    .append("\"details\":\"").append(escape(h.getDetails())).append("\",")
                    .append("\"createdAt\":\"").append(ts(h.getCreatedAt())).append("\"")
                    .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    // =========================
    // Helpers
    // =========================
    private String wrapOk(SqlRunnable fn) {
        try { fn.run(); return "OK"; }
        catch (Exception e) { e.printStackTrace(); return "ERR_" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage()); }
    }

    private String wrapJson(SqlSupplier<String> fn) {
        try {
            String out = fn.get();
            return (out == null) ? "[]" : out;
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String ts(Timestamp t) { return (t == null) ? "" : t.toString(); }
    private String ldt(LocalDateTime t) { return (t == null) ? "" : t.toString(); }

    @FunctionalInterface interface SqlRunnable { void run() throws SQLException; }
    @FunctionalInterface interface SqlSupplier<T> { T get() throws SQLException; }
}
