package Entities;

import java.time.LocalDateTime;

public class Invitation {

    private int           id;
    private int           evenementId;
    private LocalDateTime dateInvitation;
    private String        roleInvite;
    private String        email;

    // ── Constructeur complet ──
    public Invitation(int id, int evenementId, LocalDateTime dateInvitation, String roleInvite, String email) {
        this.id             = id;
        this.evenementId    = evenementId;
        this.dateInvitation = dateInvitation;
        this.roleInvite     = roleInvite;
        this.email          = email;
    }

    // ── Constructeur sans id (pour ajouter) ──
    public Invitation(int evenementId, LocalDateTime dateInvitation, String roleInvite, String email) {
        this.evenementId    = evenementId;
        this.dateInvitation = dateInvitation;
        this.roleInvite     = roleInvite;
        this.email          = email;
    }

    public int           getId()             { return id; }
    public int           getEvenementId()    { return evenementId; }
    public LocalDateTime getDateInvitation() { return dateInvitation; }
    public String        getRoleInvite()     { return roleInvite; }
    public String        getEmail()          { return email; }

    public void setId(int id)                             { this.id = id; }
    public void setEvenementId(int evenementId)           { this.evenementId = evenementId; }
    public void setDateInvitation(LocalDateTime d)        { this.dateInvitation = d; }
    public void setRoleInvite(String roleInvite)          { this.roleInvite = roleInvite; }
    public void setEmail(String email)                    { this.email = email; }
}