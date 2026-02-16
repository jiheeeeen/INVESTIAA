package Controllers;

import Entities.DemandeAnnulation;
import Entities.ProfilEntrepreneur;
import Entities.Projet;
import Entities.User;
import Utils.Session;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnnulationBridgeController {
    private final ProjetWebContext context;

    public AnnulationBridgeController(ProjetWebContext context) {
        this.context = context;
    }

    public String addDemandeAnnulation(String projetId, String raison) {
        try {
            DemandeAnnulation d = new DemandeAnnulation();
            d.setProjetId(ProjetWebUtils.parseIntRequired(projetId));
            d.setRaison(ProjetWebUtils.emptyToNull(raison));
            d.setStatut("EN_ATTENTE");
            context.getAnnulationCrud().ajouter(d);
            return "OK:" + d.getId();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String listDemandesAnnulation() {
        try {
            List<DemandeAnnulation> list = context.getAnnulationCrud().afficher();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                DemandeAnnulation d = list.get(i);
                sb.append("{");
                sb.append("\"id\":").append(d.getId()).append(",");
                sb.append("\"projetId\":").append(d.getProjetId()).append(",");
                sb.append("\"projetTitre\":").append(ProjetWebUtils.jsonString(d.getProjetTitre())).append(",");
                sb.append("\"raison\":").append(ProjetWebUtils.jsonString(d.getRaison())).append(",");
                sb.append("\"statut\":").append(ProjetWebUtils.jsonString(d.getStatut())).append(",");
                sb.append("\"createdAt\":").append(ProjetWebUtils.jsonString(ProjetWebUtils.formatTimestamp(d.getCreatedAt())));
                sb.append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (SQLException e) {
            return "[]";
        }
    }

    public String listMyDemandesAnnulation() {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "[]";

            ProfilEntrepreneur profil = context.getProfilCrud().getByUserId(current.getId());
            if (profil == null) return "[]";

            int myEntrepreneurId = profil.getIdEntrepreneur();
            List<Projet> projets = context.getProjetCrud().afficher();
            Set<Integer> myProjetIds = new HashSet<>();
            for (Projet p : projets) {
                if (p.getEntrepreneurId() == myEntrepreneurId) {
                    myProjetIds.add(p.getIdProjet());
                }
            }

            List<DemandeAnnulation> list = context.getAnnulationCrud().afficher();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (DemandeAnnulation d : list) {
                if (!myProjetIds.contains(d.getProjetId())) continue;
                if (!first) sb.append(",");
                first = false;
                sb.append("{");
                sb.append("\"id\":").append(d.getId()).append(",");
                sb.append("\"projetId\":").append(d.getProjetId()).append(",");
                sb.append("\"projetTitre\":").append(ProjetWebUtils.jsonString(d.getProjetTitre())).append(",");
                sb.append("\"raison\":").append(ProjetWebUtils.jsonString(d.getRaison())).append(",");
                sb.append("\"statut\":").append(ProjetWebUtils.jsonString(d.getStatut())).append(",");
                sb.append("\"createdAt\":").append(ProjetWebUtils.jsonString(ProjetWebUtils.formatTimestamp(d.getCreatedAt())));
                sb.append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }
}
