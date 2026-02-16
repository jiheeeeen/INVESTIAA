package Entities;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Projet {

    // ====== Colonnes table projet ======
    private int idProjet;                 // id_projet
    private int entrepreneurId;           // entrepreneur_id

    private String statut;                // statut (BROUILLON, EN_ATTENTE, VALIDE, REFUSE)
    private String titre;                 // titre
    private String secteur;               // secteur

    private String descriptionCourte;     // description_courte
    private String descriptionLongue;     // description_longue (nullable)

    private BigDecimal objectifTnd;       // objectif_tnd
    private int dureeCampagneJours;       // duree_campagne_jours

    private String modeRemboursement;     // mode_remboursement (MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNU...)
    private BigDecimal tauxInteretPct;    // taux_interet_pct (nullable)
    private Integer dureeRemboursementMois; // duree_remboursement_mois (nullable)

    private BigDecimal margeBruteEstimeeTnd; // marge_brute_estimee_tnd (nullable)
    private BigDecimal resultatNetEstimeTnd; // resultat_net_estime_tnd (nullable)

    private Timestamp createdAt;          // created_at
    private Timestamp updatedAt;          // updated_at

    public Projet() {}

    // ====== Getters & Setters ======
    public int getIdProjet() { return idProjet; }
    public void setIdProjet(int idProjet) { this.idProjet = idProjet; }

    public int getEntrepreneurId() { return entrepreneurId; }
    public void setEntrepreneurId(int entrepreneurId) { this.entrepreneurId = entrepreneurId; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getSecteur() { return secteur; }
    public void setSecteur(String secteur) { this.secteur = secteur; }

    public String getDescriptionCourte() { return descriptionCourte; }
    public void setDescriptionCourte(String descriptionCourte) { this.descriptionCourte = descriptionCourte; }

    public String getDescriptionLongue() { return descriptionLongue; }
    public void setDescriptionLongue(String descriptionLongue) { this.descriptionLongue = descriptionLongue; }

    public BigDecimal getObjectifTnd() { return objectifTnd; }
    public void setObjectifTnd(BigDecimal objectifTnd) { this.objectifTnd = objectifTnd; }

    public int getDureeCampagneJours() { return dureeCampagneJours; }
    public void setDureeCampagneJours(int dureeCampagneJours) { this.dureeCampagneJours = dureeCampagneJours; }

    public String getModeRemboursement() { return modeRemboursement; }
    public void setModeRemboursement(String modeRemboursement) { this.modeRemboursement = modeRemboursement; }

    public BigDecimal getTauxInteretPct() { return tauxInteretPct; }
    public void setTauxInteretPct(BigDecimal tauxInteretPct) { this.tauxInteretPct = tauxInteretPct; }

    public Integer getDureeRemboursementMois() { return dureeRemboursementMois; }
    public void setDureeRemboursementMois(Integer dureeRemboursementMois) { this.dureeRemboursementMois = dureeRemboursementMois; }

    public BigDecimal getMargeBruteEstimeeTnd() { return margeBruteEstimeeTnd; }
    public void setMargeBruteEstimeeTnd(BigDecimal margeBruteEstimeeTnd) { this.margeBruteEstimeeTnd = margeBruteEstimeeTnd; }

    public BigDecimal getResultatNetEstimeTnd() { return resultatNetEstimeTnd; }
    public void setResultatNetEstimeTnd(BigDecimal resultatNetEstimeTnd) { this.resultatNetEstimeTnd = resultatNetEstimeTnd; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
