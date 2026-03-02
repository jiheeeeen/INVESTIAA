package Entities;

import java.time.LocalDateTime;

public class Evenement {

    private int id;
    private int projectId;
    private String titre;
    private String description;
    private String mode; // EN_LIGNE ou PRESENTIEL
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private String meetingLink;
    private int organisateurId;
    private Statut statut;

    public Evenement() {}

    public Evenement(int projectId, String titre, String description, String mode,
                     LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, String meetingLink, int organisateurId) {
        this.projectId = projectId;
        this.titre = titre;
        this.description = description;
        this.mode = mode;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.meetingLink = meetingLink;
        this.organisateurId = organisateurId;
    }

    public Evenement(int id, int projectId, String titre, String description, String mode,
                     LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, String meetingLink, int organisateurId) {
        this.id = id;
        this.projectId = projectId;
        this.titre = titre;
        this.description = description;
        this.mode = mode;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.meetingLink = meetingLink;
        this.organisateurId = organisateurId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public int getOrganisateurId() {
        return organisateurId;
    }

    public void setOrganisateurId(int organisateurId) {
        this.organisateurId = organisateurId;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", mode='" + mode + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", lieu='" + lieu + '\'' +
                ", meetingLink='" + meetingLink + '\'' +
                ", organisateurId=" + organisateurId +
                ", statut=" + statut +
                '}';
    }


}
