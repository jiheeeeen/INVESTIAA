package Entities;

import java.sql.Date;
import java.sql.Timestamp;

public class User {

    private int id;

    private String nom;
    private String prenom;
    private String email;

    private String telephone;
    private String cin;
    private Date dateNaissance;

    private String nationalite;
    private String adresse;
    private String ville;

    private String password; // mot_de_passe
    private Role role;

    private boolean active; // est_actif

    private StatutVerification statutVerification;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {
        // defaults sûrs
        this.active = true;
        this.statutVerification = StatutVerification.NON_VERIFIE;
    }

    // Constructeur minimal (login/admin)
    public User(int id, String nom, String email, String password, Role role) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // --- getters/setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public Date getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(Date dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getNationalite() { return nationalite; }
    public void setNationalite(String nationalite) { this.nationalite = nationalite; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public StatutVerification getStatutVerification() { return statutVerification; }
    public void setStatutVerification(StatutVerification statutVerification) { this.statutVerification = statutVerification; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', email='" + email + "', role=" + role +
                ", active=" + active + ", statutVerification=" + statutVerification + "}";
    }
}
