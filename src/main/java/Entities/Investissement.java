package Entities;

import java.sql.Date;

public class Investissement {
    private int id_investissement;
    private double montant;
    private Date date_investissement;
    private int id_investisseur;
    private int id_projet;

    public Investissement() {}

    public Investissement(int id_investissement, double montant, Date date_investissement, int id_investisseur, int id_projet) {
        this.id_investissement = id_investissement;
        this.montant = montant;
        this.date_investissement = date_investissement;
        this.id_investisseur = id_investisseur;
        this.id_projet = id_projet;
    }

    public int getId_investissement() { return id_investissement; }
    public void setId_investissement(int id_investissement) { this.id_investissement = id_investissement; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public Date getDate_investissement() { return date_investissement; }
    public void setDate_investissement(Date date_investissement) { this.date_investissement = date_investissement; }

    public int getId_investisseur() { return id_investisseur; }
    public void setId_investisseur(int id_investisseur) { this.id_investisseur = id_investisseur; }

    public int getId_projet() { return id_projet; }
    public void setId_projet(int id_projet) { this.id_projet = id_projet; }
}
