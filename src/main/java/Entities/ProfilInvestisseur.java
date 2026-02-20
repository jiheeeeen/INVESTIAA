package Entities;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Set;

public class ProfilInvestisseur {

    private int idInvestisseur;
    private int idUser; // IMPORTANT (après ALTER TABLE)

    private BigDecimal budgetTotal;
    private BigDecimal budgetMensuel; // nullable
    private BigDecimal ticketMoyenParProjet;

    private String horizonInvestissement; // COURT / MOYEN / LONG
    private String bio; // nullable

    private boolean accepteConditions;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String cinRectoUrl;
    private String cinVersoUrl;
    private String photoUrl;

    private Set<String> secteurs; // AGRITECH, EDTECH, FINTECH, HEALTHTECH

    // getters/setters ...
    public int getIdInvestisseur() { return idInvestisseur; }
    public void setIdInvestisseur(int idInvestisseur) { this.idInvestisseur = idInvestisseur; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }

    public BigDecimal getBudgetMensuel() { return budgetMensuel; }
    public void setBudgetMensuel(BigDecimal budgetMensuel) { this.budgetMensuel = budgetMensuel; }

    public BigDecimal getTicketMoyenParProjet() { return ticketMoyenParProjet; }
    public void setTicketMoyenParProjet(BigDecimal ticketMoyenParProjet) { this.ticketMoyenParProjet = ticketMoyenParProjet; }

    public String getHorizonInvestissement() { return horizonInvestissement; }
    public void setHorizonInvestissement(String horizonInvestissement) { this.horizonInvestissement = horizonInvestissement; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isAccepteConditions() { return accepteConditions; }
    public void setAccepteConditions(boolean accepteConditions) { this.accepteConditions = accepteConditions; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getCinRectoUrl() { return cinRectoUrl; }
    public void setCinRectoUrl(String cinRectoUrl) { this.cinRectoUrl = cinRectoUrl; }

    public String getCinVersoUrl() { return cinVersoUrl; }
    public void setCinVersoUrl(String cinVersoUrl) { this.cinVersoUrl = cinVersoUrl; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Set<String> getSecteurs() { return secteurs; }
    public void setSecteurs(Set<String> secteurs) { this.secteurs = secteurs; }
}
