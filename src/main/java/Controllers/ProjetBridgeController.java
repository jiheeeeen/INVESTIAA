package Controllers;

import Entities.Investissement;
import Entities.ProfilEntrepreneur;
import Entities.Projet;
import Entities.Statut;
import Entities.User;
import Services.InvestissementCRUD;
import Services.ProfilInvestisseurCRUD;
import Services.UserCRUD;
import Utils.Session;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjetBridgeController {
    private final ProjetWebContext context;
    private final InvestissementCRUD investissementCRUD = new InvestissementCRUD();
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
                String status = ProjetWebUtils.mapStatusForUi(p.getStatut());
                try {
                    if (investissementCRUD.countParProjet(p.getIdProjet()) > 0) {
                        status = "EN_COURS";
                    }
                } catch (Exception ignored) {}
                sb.append(ProjetWebUtils.toListJsonWithStatus(p, status));
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
            return "ERROR:" + e.getMessage();
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
}
