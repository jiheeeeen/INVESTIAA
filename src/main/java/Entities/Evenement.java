package Entities;

import java.time.LocalDateTime;

public class Evenement {

    private int id;
    private int projectId;

    private String titre;
    private String description;

    private ModeEvenement mode;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    private String lieu;         // nullable
    private String meetingLink;  // nullable

    private int organisateurId;

    private Statut statut; // EN_ATTENTE / VALIDE / REFUSE

    public Evenement() {}

    public Evenement(int id, int projectId, String titre, String description, ModeEvenement mode,
                     LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, String meetingLink,
                     int organisateurId, Statut statut) {
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
        this.statut = statut;
    }

    public Evenement(int projectId, String titre, String description, ModeEvenement mode,
                     LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, String meetingLink,
                     int organisateurId, Statut statut) {
        this.projectId = projectId;
        this.titre = titre;
        this.description = description;
        this.mode = mode;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.meetingLink = meetingLink;
        this.organisateurId = organisateurId;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ModeEvenement getMode() { return mode; }
    public void setMode(ModeEvenement mode) { this.mode = mode; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public int getOrganisateurId() { return organisateurId; }
    public void setOrganisateurId(int organisateurId) { this.organisateurId = organisateurId; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", titre='" + titre + '\'' +
                ", mode=" + mode +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", organisateurId=" + organisateurId +
                ", statut=" + statut +
                '}';
    }
}
