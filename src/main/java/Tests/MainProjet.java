package Tests;

import Entities.Projet;
import Services.ProjetCRUD;

import java.math.BigDecimal;
import java.util.List;

public class MainProjet {

    public static void main(String[] args) {
        try {
            ProjetCRUD crud = new ProjetCRUD();

            // 1) AJOUT
            Projet pr = new Projet();
            pr.setEntrepreneurId(10);

            pr.setStatut("BROUILLON");
            pr.setTitre("SmartFarm");
            pr.setSecteur("AgriTech");

            pr.setDescriptionCourte("Capteurs IoT pour optimiser l'irrigation");
            pr.setDescriptionLongue("Description longue du projet SmartFarm...");

            pr.setObjectifTnd(new BigDecimal("60000"));
            pr.setDureeCampagneJours(60);

            pr.setModeRemboursement("MENSUEL");
            pr.setTauxInteretPct(new BigDecimal("9.00"));
            pr.setDureeRemboursementMois(12);

            pr.setMargeBruteEstimeeTnd(new BigDecimal("72000"));
            pr.setResultatNetEstimeTnd(new BigDecimal("20000"));

            crud.ajouter(pr);
            System.out.println("Projet ajouté avec succès ! ID = " + pr.getIdProjet());

            // 2) AFFICHER
            List<Projet> list = crud.afficher();
            System.out.println("Liste projets (" + list.size() + ")");
            for (Projet p : list) {
                System.out.println(
                        p.getIdProjet() + " | " +
                                p.getTitre() + " | " +
                                p.getObjectifTnd() + " TND | " +
                                p.getStatut()
                );
            }

            // 3) MODIFIER
            int idAModifier = 16;

            Projet p = crud.getById(idAModifier);
            if (p == null) {
                System.out.println("Projet introuvable: " + idAModifier);
                return;
            }

            p.setStatut("VALIDE");
            p.setTitre("jiji");

            crud.modifier(p); // ✅ ici c'est p, pas pr
            System.out.println("Projet modifié !");

            // 4) SUPPRIMER (décommente si besoin)
             //crud.supprimer(pr.getIdProjet());
            int idASupprimer = 13;
            crud.supprimer(idASupprimer);
            System.out.println("Projet supprimé avec succès !");



        } catch (Exception e) {
            System.out.println("Erreur test : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
