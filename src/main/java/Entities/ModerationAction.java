package Entities;

import java.sql.Timestamp;

public class ModerationAction {
    private int id;
    private int adminId;
    private String action;       // ex: ACCEPT_COMPTE / REFUSE_PROJET ...
    private String targetType;   // USER / PROJET / EVENEMENT
    private int targetId;
    private String details;
    private Timestamp createdAt;

    public ModerationAction() {}

    public ModerationAction(int adminId, String action, String targetType, int targetId, String details) {
        this.adminId = adminId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
