package Entities;

import java.sql.Timestamp;

public class DemandeAnnulation {
    private int id;
    private int projetId;
    private String projetTitre;
    private String raison;
    private String statut;
    private Timestamp createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjetId() { return projetId; }
    public void setProjetId(int projetId) { this.projetId = projetId; }

    public String getProjetTitre() { return projetTitre; }
    public void setProjetTitre(String projetTitre) { this.projetTitre = projetTitre; }

    public String getRaison() { return raison; }
    public void setRaison(String raison) { this.raison = raison; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
