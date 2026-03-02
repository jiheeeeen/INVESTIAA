package Controllers;

import Entities.Projet;
import Services.ProjetCRUD;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

public class ProjetController {

    @FXML private TextField idProjetField, entrepreneurIdField, titreField, secteurField, objectifField, dureeCampagneField;
    @FXML private TextField tauxField, dureeRembField;
    @FXML private ComboBox<String> statutBox, modeBox;
    @FXML private Label msgLabel;

    private final ProjetCRUD crud = new ProjetCRUD();

    @FXML
    public void initialize() {
        statutBox.getItems().setAll(
                "BROUILLON",
                "REFUSE",
                "EN_ATTENTE",
                "VALIDE",
                "INVESTISSEMENT_EN_COURS",
                "INVESTISSEMENT_TERMINE",
                "PROJET_EN_COURS"
        );
        modeBox.getItems().setAll("MENSUEL","TRIMESTRIEL","SEMESTRIEL","ANNU");
        statutBox.setValue("BROUILLON");
        modeBox.setValue("MENSUEL");
    }

    @FXML
    private void onAjouter() {
        try {
            Projet p = buildProjet(false);
            crud.ajouter(p);
            msgLabel.setText("✅ Projet ajouté ! id_projet=" + p.getIdProjet());
        } catch (Exception e) {
            msgLabel.setText("❌ Erreur ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onModifier() {
        try {
            String idTxt = idProjetField.getText().trim();
            if (idTxt.isEmpty()) {
                msgLabel.setText("⚠️ Donne l'ID du projet à modifier.");
                return;
            }

            int id = Integer.parseInt(idTxt);
            Projet exist = crud.getById(id);
            if (exist == null) {
                msgLabel.setText("⚠️ Projet introuvable: id_projet=" + id);
                return;
            }

            Projet p = buildProjet(true);
            p.setIdProjet(id);

            crud.modifier(p);
            msgLabel.setText("✅ Projet modifié ! id_projet=" + id);
        } catch (Exception e) {
            msgLabel.setText("❌ Erreur modif: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSupprimer() {
        try {
            String idTxt = idProjetField.getText().trim();
            if (idTxt.isEmpty()) {
                msgLabel.setText("⚠️ Donne l'ID du projet à supprimer.");
                return;
            }

            int id = Integer.parseInt(idTxt);
            crud.supprimer(id);
            msgLabel.setText("✅ Projet supprimé ! id_projet=" + id);
        } catch (Exception e) {
            msgLabel.setText("❌ Erreur suppression: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Projet buildProjet(boolean forUpdate) {
        Projet p = new Projet();

        // champs obligatoires
        p.setEntrepreneurId(Integer.parseInt(entrepreneurIdField.getText().trim()));
        p.setTitre(titreField.getText().trim());
        p.setSecteur(secteurField.getText().trim());
        p.setObjectifTnd(new BigDecimal(objectifField.getText().trim()));
        p.setDureeCampagneJours(Integer.parseInt(dureeCampagneField.getText().trim()));
        p.setStatut(statutBox.getValue());
        p.setModeRemboursement(modeBox.getValue());

        // optionnels
        String taux = tauxField.getText().trim();
        p.setTauxInteretPct(taux.isEmpty() ? null : new BigDecimal(taux));

        String dr = dureeRembField.getText().trim();
        p.setDureeRemboursementMois(dr.isEmpty() ? null : Integer.parseInt(dr));

        p.setDescriptionCourte("");
        p.setDescriptionLongue(null);
        p.setMargeBruteEstimeeTnd(null);
        p.setResultatNetEstimeTnd(null);

        return p;
    }
}
