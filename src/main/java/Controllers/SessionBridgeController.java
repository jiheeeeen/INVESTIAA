package Controllers;

import Controllers.WebAuthController;
import Entities.Role;
import Entities.User;
import Entities.ProfilEntrepreneur;
import Services.EntrepreneurNotificationCRUD;
import Services.InvestisseurNotificationCRUD;
import Services.ProfilEntrepreneurCRUD;
import Utils.Session;
import Utils.sceneManager;
import java.util.ArrayList;
import java.util.List;

public class SessionBridgeController {
    public String getCurrentUserName() {
        User u = Session.getCurrentUser();
        if (u == null) return "";

        String nom = u.getNom() == null ? "" : u.getNom().trim();
        String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
        String full = (nom + " " + prenom).trim();

        if (!full.isEmpty()) return full;
        if (u.getEmail() != null) return u.getEmail().trim();
        return "";
    }

    public String getCurrentUserId() {
        User u = Session.getCurrentUser();
        if (u == null) return "";
        return String.valueOf(u.getId());
    }

    public String getCurrentUserRole() {
        User u = Session.getCurrentUser();
        if (u == null || u.getRole() == null) return "";
        Role role = u.getRole();
        return role.name();
    }

    public String getEntrepreneurInvestmentNotificationsJson() {
        try {
            User u = Session.getCurrentUser();
            if (u == null || u.getRole() != Role.ENTREPRENEUR) return "[]";

            ProfilEntrepreneurCRUD profilCrud = new ProfilEntrepreneurCRUD();
            ProfilEntrepreneur profil = profilCrud.getByUserId(u.getId());
            if (profil == null) return "[]";

            int entrepreneurId = profil.getIdEntrepreneur();

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            EntrepreneurNotificationCRUD notifCrud = new EntrepreneurNotificationCRUD();
            List<EntrepreneurNotificationCRUD.NotificationRow> rows = notifCrud.listUnread(entrepreneurId);
            for (EntrepreneurNotificationCRUD.NotificationRow row : rows) {
                if (!first) sb.append(",");
                first = false;
                String titre = row.titreProjet == null ? "Projet" : row.titreProjet;
                String date = row.dateInvestissement == null ? "" : row.dateInvestissement.toString();
                sb.append("{")
                        .append("\"id\":").append(row.id).append(",")
                        .append("\"id_projet\":").append(row.idProjet).append(",")
                        .append("\"titre\":").append(ProjetWebUtils.jsonString(titre)).append(",")
                        .append("\"date\":").append(ProjetWebUtils.jsonString(date))
                        .append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String markEntrepreneurNotificationRead(String id) {
        try {
            int notifId = Integer.parseInt(id);
            new EntrepreneurNotificationCRUD().markRead(notifId);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String getEntrepreneurNotificationHistoryJson(String limitStr) {
        try {
            User u = Session.getCurrentUser();
            if (u == null || u.getRole() != Role.ENTREPRENEUR) return "{\"items\":[],\"total\":0}";

            ProfilEntrepreneurCRUD profilCrud = new ProfilEntrepreneurCRUD();
            ProfilEntrepreneur profil = profilCrud.getByUserId(u.getId());
            if (profil == null) return "{\"items\":[],\"total\":0}";

            int entrepreneurId = profil.getIdEntrepreneur();
            int limit = 5;
            try { limit = Integer.parseInt(limitStr); } catch (Exception ignored) {}
            if (limit <= 0) limit = 5;

            EntrepreneurNotificationCRUD notifCrud = new EntrepreneurNotificationCRUD();
            int total = notifCrud.countAll(entrepreneurId);
            List<EntrepreneurNotificationCRUD.NotificationRow> rows = notifCrud.listRecent(entrepreneurId, limit);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"total\":").append(total).append(",\"items\":[");
            boolean first = true;
            for (EntrepreneurNotificationCRUD.NotificationRow row : rows) {
                if (!first) sb.append(",");
                first = false;
                String titre = row.titreProjet == null ? "Projet" : row.titreProjet;
                String date = row.dateInvestissement == null ? "" : row.dateInvestissement.toString();
                sb.append("{")
                        .append("\"id\":").append(row.id).append(",")
                        .append("\"id_projet\":").append(row.idProjet).append(",")
                        .append("\"titre\":").append(ProjetWebUtils.jsonString(titre)).append(",")
                        .append("\"date\":").append(ProjetWebUtils.jsonString(date)).append(",")
                        .append("\"is_read\":").append(row.isRead ? "1" : "0")
                        .append("}");
            }
            sb.append("]}");
            return sb.toString();
        } catch (Exception e) {
            return "{\"items\":[],\"total\":0}";
        }
    }

    public String getInvestorProjectNotificationsJson() {
        try {
            User u = Session.getCurrentUser();
            if (u == null || u.getRole() != Role.INVESTISSEUR) return "[]";

            int investisseurId = u.getId();

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            InvestisseurNotificationCRUD notifCrud = new InvestisseurNotificationCRUD();
            List<InvestisseurNotificationCRUD.NotificationRow> rows = notifCrud.listUnread(investisseurId);
            for (InvestisseurNotificationCRUD.NotificationRow row : rows) {
                if (!first) sb.append(",");
                first = false;
                String titre = row.titreProjet == null ? "Projet" : row.titreProjet;
                String date = row.createdAt == null ? "" : row.createdAt.toLocalDateTime().toLocalDate().toString();
                sb.append("{")
                        .append("\"id\":").append(row.id).append(",")
                        .append("\"id_projet\":").append(row.idProjet).append(",")
                        .append("\"titre\":").append(ProjetWebUtils.jsonString(titre)).append(",")
                        .append("\"date\":").append(ProjetWebUtils.jsonString(date))
                        .append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String markInvestorNotificationRead(String id) {
        try {
            int notifId = Integer.parseInt(id);
            new InvestisseurNotificationCRUD().markRead(notifId);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String getInvestorNotificationHistoryJson(String limitStr) {
        try {
            User u = Session.getCurrentUser();
            if (u == null || u.getRole() != Role.INVESTISSEUR) return "{\"items\":[],\"total\":0}";

            int investisseurId = u.getId();
            int limit = 5;
            try { limit = Integer.parseInt(limitStr); } catch (Exception ignored) {}
            if (limit <= 0) limit = 5;

            InvestisseurNotificationCRUD notifCrud = new InvestisseurNotificationCRUD();
            int total = notifCrud.countAll(investisseurId);
            List<InvestisseurNotificationCRUD.NotificationRow> rows = notifCrud.listRecent(investisseurId, limit);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"total\":").append(total).append(",\"items\":[");
            boolean first = true;
            for (InvestisseurNotificationCRUD.NotificationRow row : rows) {
                if (!first) sb.append(",");
                first = false;
                String titre = row.titreProjet == null ? "Projet" : row.titreProjet;
                String date = row.createdAt == null ? "" : row.createdAt.toLocalDateTime().toLocalDate().toString();
                sb.append("{")
                        .append("\"id\":").append(row.id).append(",")
                        .append("\"id_projet\":").append(row.idProjet).append(",")
                        .append("\"titre\":").append(ProjetWebUtils.jsonString(titre)).append(",")
                        .append("\"date\":").append(ProjetWebUtils.jsonString(date)).append(",")
                        .append("\"is_read\":").append(row.isRead ? "1" : "0")
                        .append("}");
            }
            sb.append("]}");
            return sb.toString();
        } catch (Exception e) {
            return "{\"items\":[],\"total\":0}";
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

    public String goEvenements() {
        try {
            javafx.application.Platform.runLater(() ->
                    sceneManager.switchTo("/fxml/ajout_evenement.fxml", "Investia - Événements")
            );
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    public String goInvitations() {
        try {
            javafx.application.Platform.runLater(() ->
                    sceneManager.switchTo("/fxml/invitation.fxml", "Investia - Invitations")
            );
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

}
