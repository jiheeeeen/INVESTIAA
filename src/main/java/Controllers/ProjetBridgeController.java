package Controllers;

import Entities.ProfilEntrepreneur;
import Entities.Projet;
import Entities.Statut;
import Entities.User;
import Utils.Session;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjetBridgeController {
    private final ProjetWebContext context;

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
                sb.append(ProjetWebUtils.toListJson(mine.get(i)));
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
}
