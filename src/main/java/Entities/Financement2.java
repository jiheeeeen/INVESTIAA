package Entities;

import java.sql.Timestamp;

public class Financement2 {

    private int id_financement;
    private int id_projet;
    private int id_investissement;

    private double montant;
    private double frais_pct;

    private String mode_paiement; // CARTE_BANCAIRE / VIREMENT / WALLET
    private String statut;        // EN_ATTENTE / CONFIRMED / ANNULE / REFUSE

    private double taux_interet_pct;
    private int duree_estimee_mois;

    private String note;

    private Timestamp created_at;
    private Timestamp updated_at;

    public Financement2() {
    }

    public Financement2(int id_projet,
                        int id_investissement,
                        double montant,
                        double frais_pct,
                        String mode_paiement,
                        String statut,
                        double taux_interet_pct,
                        int duree_estimee_mois,
                        String note) {
        this.id_projet = id_projet;
        this.id_investissement = id_investissement;
        this.montant = montant;
        this.frais_pct = frais_pct;
        this.mode_paiement = mode_paiement;
        this.statut = statut;
        this.taux_interet_pct = taux_interet_pct;
        this.duree_estimee_mois = duree_estimee_mois;
        this.note = note;
    }

    public Financement2(int id_financement,
                        int id_projet,
                        int id_investissement,
                        double montant,
                        double frais_pct,
                        String mode_paiement,
                        String statut,
                        double taux_interet_pct,
                        int duree_estimee_mois,
                        String note) {
        this.id_financement = id_financement;
        this.id_projet = id_projet;
        this.id_investissement = id_investissement;
        this.montant = montant;
        this.frais_pct = frais_pct;
        this.mode_paiement = mode_paiement;
        this.statut = statut;
        this.taux_interet_pct = taux_interet_pct;
        this.duree_estimee_mois = duree_estimee_mois;
        this.note = note;
    }

    public int getId_financement() {
        return id_financement;
    }

    public void setId_financement(int id_financement) {
        this.id_financement = id_financement;
    }

    public int getId_projet() {
        return id_projet;
    }

    public void setId_projet(int id_projet) {
        this.id_projet = id_projet;
    }

    public int getId_investissement() {
        return id_investissement;
    }

    public void setId_investissement(int id_investissement) {
        this.id_investissement = id_investissement;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public double getFrais_pct() {
        return frais_pct;
    }

    public void setFrais_pct(double frais_pct) {
        this.frais_pct = frais_pct;
    }

    public String getMode_paiement() {
        return mode_paiement;
    }

    public void setMode_paiement(String mode_paiement) {
        this.mode_paiement = mode_paiement;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public double getTaux_interet_pct() {
        return taux_interet_pct;
    }

    public void setTaux_interet_pct(double taux_interet_pct) {
        this.taux_interet_pct = taux_interet_pct;
    }

    public int getDuree_estimee_mois() {
        return duree_estimee_mois;
    }

    public void setDuree_estimee_mois(int duree_estimee_mois) {
        this.duree_estimee_mois = duree_estimee_mois;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public Timestamp getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Timestamp updated_at) {
        this.updated_at = updated_at;
    }
}

