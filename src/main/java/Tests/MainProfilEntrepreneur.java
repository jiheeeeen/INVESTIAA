package Tests;

import Entities.*;
import Services.ProfilEntrepreneurCRUD;

import java.util.EnumSet;
import java.util.List;

public class MainProfilEntrepreneur {

    public static void main(String[] args) {
        try {
            ProfilEntrepreneurCRUD crud = new ProfilEntrepreneurCRUD();

            // 1) AJOUT

            ProfilEntrepreneur p1 = new ProfilEntrepreneur();
            p1.setAdresse("Rue 1, Tunis");
            p1.setRib("TN59 1000 0600 0000 0000 0000");
            p1.setCinRectoUrl("uploads/cin_recto_1.png");
            p1.setCinVersoUrl("uploads/cin_verso_1.png");
            p1.setJustificatifDomicileUrl("uploads/justif_1.pdf");
            p1.setAccepteConditions(true);
            p1.setStatutCompte(StatutCompte.ACTIF);
            p1.setStatutVerification(StatutVerification.NON_VERIFIE);
            p1.setPhotoUrl("uploads/photo_1.png");
            p1.setBio("Profil entrepreneur 1 ajouté automatiquement");
            p1.setRegistreCommerceUrl("uploads/rc_1.pdf");
            p1.setPatenteUrl("uploads/patente_1.pdf");
            p1.setMatriculeFiscalUrl("uploads/mf_1.pdf");
            p1.setCarteFiscaleUrl("uploads/cf_1.pdf");
            p1.setSecteurs(EnumSet.of(Secteur.FINTECH, Secteur.SERVICES));

            crud.ajouterAuto(p1);
            System.out.println("✅ Profil 1 ajouté ! id_entrepreneur=" + p1.getIdEntrepreneur() + " | id_user=" + p1.getIdUser());

            // 2) AJOUT (profil 2)
            ProfilEntrepreneur p2 = new ProfilEntrepreneur();
            p2.setAdresse("Avenue Habib Bourguiba, Sfax");
            p2.setRib("TN59 2000 0700 0000 0000 0000");
            p2.setCinRectoUrl("uploads/cin_recto_2.png");
            p2.setCinVersoUrl("uploads/cin_verso_2.png");
            p2.setJustificatifDomicileUrl("uploads/justif_2.pdf");
            p2.setAccepteConditions(true);
            p2.setStatutCompte(StatutCompte.ACTIF);
            p2.setStatutVerification(StatutVerification.EN_ATTENTE);
            p2.setPhotoUrl("uploads/photo_2.png");
            p2.setBio("Profil entrepreneur 2 ajouté automatiquement");
            p2.setRegistreCommerceUrl("uploads/rc_2.pdf");
            p2.setPatenteUrl("uploads/patente_2.pdf");
            p2.setMatriculeFiscalUrl("uploads/mf_2.pdf");
            p2.setCarteFiscaleUrl("uploads/cf_2.pdf");
            p2.setSecteurs(EnumSet.of(Secteur.AGRITECH, Secteur.GREENTECH));

            crud.ajouterAuto(p2);
            System.out.println("✅ Profil 2 ajouté ! id_entrepreneur=" + p2.getIdEntrepreneur() + " | id_user=" + p2.getIdUser());

            // 3) AFFICHER
            afficherListe(crud);

            // 4) MODIFIER profil
            int idAModifier = 7;
            ProfilEntrepreneur toUpdate = crud.getById(idAModifier);

            if (toUpdate != null) {
                toUpdate.setBio("jiheneeene !");
                toUpdate.setStatutVerification(StatutVerification.VERIFIE);
                toUpdate.setSecteurs(EnumSet.of(Secteur.EDTECH, Secteur.FINTECH));
                toUpdate.setAdresse("Rue 99, Ariana");

                crud.modifier(toUpdate);

                System.out.println("✅ Profil modifié ! id_entrepreneur=" + toUpdate.getIdEntrepreneur());
            } else {
                System.out.println("⚠️ Profil introuvable pour modification. id_entrepreneur=" + idAModifier);
            }


            afficherListe(crud);

            // 5) SUPPRIMER PAR id_user

            int userIdToDelete = 4;

            ProfilEntrepreneur profil = crud.getByUserId(userIdToDelete);

            if (profil == null) {
                System.out.println("⚠️ Aucun profil trouvé pour id_user=" + userIdToDelete + " => rien à supprimer.");
            } else {
                int idEntrepreneur = profil.getIdEntrepreneur();
                System.out.println("🗑️ Suppression profil id_entrepreneur=" + idEntrepreneur + " (id_user=" + userIdToDelete + ")");

                crud.supprimer(idEntrepreneur);

                ProfilEntrepreneur check = crud.getById(idEntrepreneur);
                if (check == null) {
                    System.out.println("✅ Suppression confirmée !");
                } else {
                    System.out.println("❌ Suppression échouée : profil existe encore !");
                }
            }

            afficherListe(crud);

        } catch (Exception e) {
            System.out.println("❌ Erreur test : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void afficherListe(ProfilEntrepreneurCRUD crud) throws Exception {
        List<ProfilEntrepreneur> list = crud.afficher();
        System.out.println("\n📌 Liste profils (" + list.size() + ")");
        for (ProfilEntrepreneur e : list) {
            System.out.println(
                    e.getIdEntrepreneur()
                            + " | id_user=" + e.getIdUser()
                            + " | " + e.getAdresse()
                            + " | " + e.getStatutCompte()
                            + " | " + e.getStatutVerification()
                            + " | secteurs=" + e.getSecteurs()
            );
        }
        System.out.println();
    }
}
