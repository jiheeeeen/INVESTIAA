package Entities;

import java.sql.Timestamp;
import java.util.Set;

public class ProfilEntrepreneur {

    // PK auto_increment
    private int idEntrepreneur;   // id_entrepreneur

    // FK vers users(id)
    private int idUser;           // id_user

    private String adresse;
    private String cinRectoUrl;
    private String cinVersoUrl;
    private String justificatifDomicileUrl;

    private String rib;
    private boolean accepteConditions;

    private StatutCompte statutCompte;
    private StatutVerification statutVerification;
    private Timestamp dateVerification;

    private String bio;
    private String photoUrl;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String registreCommerceUrl;
    private String patenteUrl;
    private String matriculeFiscalUrl;
    private String carteFiscaleUrl;

    private Set<Secteur> secteurs;

    public ProfilEntrepreneur() {}

    public int getIdEntrepreneur() { return idEntrepreneur; }
    public void setIdEntrepreneur(int idEntrepreneur) { this.idEntrepreneur = idEntrepreneur; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getCinRectoUrl() { return cinRectoUrl; }
    public void setCinRectoUrl(String cinRectoUrl) { this.cinRectoUrl = cinRectoUrl; }

    public String getCinVersoUrl() { return cinVersoUrl; }
    public void setCinVersoUrl(String cinVersoUrl) { this.cinVersoUrl = cinVersoUrl; }

    public String getJustificatifDomicileUrl() { return justificatifDomicileUrl; }
    public void setJustificatifDomicileUrl(String justificatifDomicileUrl) { this.justificatifDomicileUrl = justificatifDomicileUrl; }

    public String getRib() { return rib; }
    public void setRib(String rib) { this.rib = rib; }

    public boolean isAccepteConditions() { return accepteConditions; }
    public void setAccepteConditions(boolean accepteConditions) { this.accepteConditions = accepteConditions; }

    public StatutCompte getStatutCompte() { return statutCompte; }
    public void setStatutCompte(StatutCompte statutCompte) { this.statutCompte = statutCompte; }

    public StatutVerification getStatutVerification() { return statutVerification; }
    public void setStatutVerification(StatutVerification statutVerification) { this.statutVerification = statutVerification; }

    public Timestamp getDateVerification() { return dateVerification; }
    public void setDateVerification(Timestamp dateVerification) { this.dateVerification = dateVerification; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getRegistreCommerceUrl() { return registreCommerceUrl; }
    public void setRegistreCommerceUrl(String registreCommerceUrl) { this.registreCommerceUrl = registreCommerceUrl; }

    public String getPatenteUrl() { return patenteUrl; }
    public void setPatenteUrl(String patenteUrl) { this.patenteUrl = patenteUrl; }

    public String getMatriculeFiscalUrl() { return matriculeFiscalUrl; }
    public void setMatriculeFiscalUrl(String matriculeFiscalUrl) { this.matriculeFiscalUrl = matriculeFiscalUrl; }

    public String getCarteFiscaleUrl() { return carteFiscaleUrl; }
    public void setCarteFiscaleUrl(String carteFiscaleUrl) { this.carteFiscaleUrl = carteFiscaleUrl; }

    public Set<Secteur> getSecteurs() { return secteurs; }
    public void setSecteurs(Set<Secteur> secteurs) { this.secteurs = secteurs; }
}
