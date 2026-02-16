package Entities;

import java.sql.Date;

public class Remboursement {
    private int id;
    private int financementId;
    private Date dateEcheance;
    private double montantDu;
    private double montantPaye;
    private RemboursementStatut statut = RemboursementStatut.EN_ATTENTE;

    public Remboursement() {
    }

    public Remboursement(int financementId, Date dateEcheance, double montantDu, double montantPaye, String statut) {
        this.financementId = financementId;
        this.dateEcheance = dateEcheance;
        this.montantDu = montantDu;
        this.montantPaye = montantPaye;
        this.statut = RemboursementStatut.from(statut);
    }

    public Remboursement(int financementId, Date dateEcheance, double montantDu, double montantPaye, RemboursementStatut statut) {
        this.financementId = financementId;
        this.dateEcheance = dateEcheance;
        this.montantDu = montantDu;
        this.montantPaye = montantPaye;
        this.statut = statut == null ? RemboursementStatut.EN_ATTENTE : statut;
    }

    public Remboursement(int id, int financementId, Date dateEcheance, double montantDu, double montantPaye, String statut) {
        this.id = id;
        this.financementId = financementId;
        this.dateEcheance = dateEcheance;
        this.montantDu = montantDu;
        this.montantPaye = montantPaye;
        this.statut = RemboursementStatut.from(statut);
    }

    public Remboursement(int id, int financementId, Date dateEcheance, double montantDu, double montantPaye, RemboursementStatut statut) {
        this.id = id;
        this.financementId = financementId;
        this.dateEcheance = dateEcheance;
        this.montantDu = montantDu;
        this.montantPaye = montantPaye;
        this.statut = statut == null ? RemboursementStatut.EN_ATTENTE : statut;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFinancementId() {
        return financementId;
    }

    public void setFinancementId(int financementId) {
        this.financementId = financementId;
    }

    public Date getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(Date dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public double getMontantDu() {
        return montantDu;
    }

    public void setMontantDu(double montantDu) {
        this.montantDu = montantDu;
    }

    public double getMontantPaye() {
        return montantPaye;
    }

    public void setMontantPaye(double montantPaye) {
        this.montantPaye = montantPaye;
    }

    public String getStatut() {
        return statut == null ? RemboursementStatut.EN_ATTENTE.name() : statut.name();
    }

    public RemboursementStatut getStatutEnum() {
        return statut == null ? RemboursementStatut.EN_ATTENTE : statut;
    }

    public void setStatut(String statut) {
        this.statut = RemboursementStatut.from(statut);
    }

    public void setStatut(RemboursementStatut statut) {
        this.statut = statut == null ? RemboursementStatut.EN_ATTENTE : statut;
    }
}
