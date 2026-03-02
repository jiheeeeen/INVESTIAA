package Controllers;

import Entities.*;
import Services.*;
import Controllers.ProjectAnalysisService;
import Utils.Session;
import Utils.sceneManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import Services.EmailServiceBrevoSMTP;

public class AdminDashboardBridge {

    private final UserCRUD userCRUD = new UserCRUD();
    private final ProjetCRUD projetCRUD = new ProjetCRUD();
    private final EvenementCRUD evenementCRUD = new EvenementCRUD();
    private final ModerationActionCRUD historyCRUD = new ModerationActionCRUD();
    private final ProfilEntrepreneurCRUD profilCRUD = new ProfilEntrepreneurCRUD();

    // ✅ Annulation
    private final DemandeAnnulationCRUD annulationCRUD = new DemandeAnnulationCRUD();

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

            try {
                User u = userCRUD.findById(userId);
                EmailServiceBrevoSMTP.sendVerificationDecisionAsync(u, StatutVerification.VERIFIE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public String rejectAccount(int userId) {
        return wrapOk(() -> {
            userCRUD.setVerificationStatus(userId, StatutVerification.NON_VERIFIE, false);

            try { profilCRUD.updateVerificationByUserId(userId, StatutVerification.NON_VERIFIE); } catch (Exception ignored) {}

            safeLog(TargetType.COMPTE, userId, Decision.REFUSER, "REJECT_ACCOUNT_TO_NON_VERIFIE");

            try {
                User u = userCRUD.findById(userId);
                EmailServiceBrevoSMTP.sendVerificationDecisionAsync(u, StatutVerification.NON_VERIFIE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // =========================
    // Users
    // =========================
    public String createUser(String nom, String prenom, String email, String telephone,
                             String cin, String role, String statut_verification, String mot_de_passe) {
        return wrapOk(() -> {
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
            User before = null;
            try { before = userCRUD.findById(id); } catch (Exception ignored) {}

            userCRUD.updateUserAdminFields(id, nom, prenom, email, telephone, cin, role, statutVerification);
            safeLog(TargetType.USER, id, Decision.VALIDER, "UPDATE_USER");

            try {
                User after = userCRUD.findById(id);
                if (before != null && after != null
                        && before.getStatutVerification() != null
                        && after.getStatutVerification() != null
                        && !before.getStatutVerification().name().equals(after.getStatutVerification().name())) {

                    EmailServiceBrevoSMTP.sendVerificationDecisionAsync(after, after.getStatutVerification());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    public String getProjectAnalysisJson(int projectId) {
        return wrapJson(() -> {
            Projet target = null;
            for (Projet p : projetCRUD.afficher()) {
                if (p != null && p.getIdProjet() == projectId) { target = p; break; }
            }
            if (target == null) return "{}";
            return new ProjectAnalysisService().analyseAsJson(target);
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
    // ✅ Demandes d'annulation (NOUVEAU)
    // =========================

    public String getDemandesAnnulationEnAttente() {
        try {
            List<DemandeAnnulation> list = annulationCRUD.afficherEnAttente();
            return "{\"ok\":true,\"data\":" + toDemandesAnnulationJson(list) + "}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"ok\":false,\"data\":[]}";
        }
    }

    public String accepterDemandeAnnulation(int demandeId) {
        try {
            boolean ok = annulationCRUD.accepterDemandeEtSupprimerProjet(demandeId);

            if (ok) {
                // log (on ne connait pas forcément id_projet ici, mais on garde une trace)
                safeLog(TargetType.PROJET, 0, Decision.REFUSER, "ACCEPT_CANCEL_REQUEST_DELETE_PROJECT demandeId=" + demandeId);
                return "{\"ok\":true}";
            }
            return "{\"ok\":false,\"error\":\"ACCEPT_FAILED\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"ok\":false,\"error\":\"EXCEPTION\"}";
        }
    }

    public String refuserDemandeAnnulation(int demandeId) {
        try {
            boolean ok = annulationCRUD.refuserDemande(demandeId);

            if (ok) {
                safeLog(TargetType.PROJET, 0, Decision.REFUSER, "REFUSE_CANCEL_REQUEST demandeId=" + demandeId);
                return "{\"ok\":true}";
            }
            return "{\"ok\":false,\"error\":\"REFUSE_FAILED\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"ok\":false,\"error\":\"EXCEPTION\"}";
        }
    }

    private String toDemandesAnnulationJson(List<DemandeAnnulation> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            DemandeAnnulation d = list.get(i);
            sb.append("{")
                    .append("\"id\":").append(d.getId()).append(",")
                    .append("\"projetId\":").append(d.getProjetId()).append(",")
                    .append("\"titre\":\"").append(j(d.getProjetTitre())).append("\",")
                    .append("\"raison\":\"").append(j(d.getRaison())).append("\",")
                    .append("\"statut\":\"").append(j(d.getStatut())).append("\",")
                    .append("\"createdAt\":\"").append(ts(d.getCreatedAt())).append("\"")
                    .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
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

                    // ✅ mode est String maintenant
                    .append("\"mode\":\"").append(escape(e.getMode())).append("\",")

                    .append("\"dateDebut\":\"").append(ldt(e.getDateDebut())).append("\",")

                    // ✅ statut n'existe pas dans l'entité -> on le lit depuis la DB (helper)
                    .append("\"statut\":\"").append(escape(getEventStatutSafe(e.getId()))).append("\"")
                    .append("}");

            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
    private String getEventStatutSafe(int eventId) {
        try {
            return evenementCRUD.getStatutById(eventId);
        } catch (Exception e) {
            return "";
        }
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

    private static String j(String s){
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","");
    }

    private String ts(Timestamp t) { return (t == null) ? "" : t.toString(); }
    private String ldt(LocalDateTime t) { return (t == null) ? "" : t.toString(); }

    @FunctionalInterface interface SqlRunnable { void run() throws SQLException; }
    @FunctionalInterface interface SqlSupplier<T> { T get() throws SQLException; }
}