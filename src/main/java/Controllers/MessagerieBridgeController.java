package Controllers;

import Entities.User;
import Services.MessagerieCRUD;
import Services.UserCRUD;
import Utils.Session;

import java.util.List;

public class MessagerieBridgeController {
    private final MessagerieCRUD messagerieCRUD = new MessagerieCRUD();
    private final UserCRUD userCRUD = new UserCRUD();

    public String getSelectedContactUserId() {
        try {
            int id = Session.getSelectedContactUserId();
            return id > 0 ? String.valueOf(id) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String setSelectedContactUserId(String id) {
        try {
            int uid = Integer.parseInt(id == null ? "" : id.trim());
            if (uid <= 0) return "ERROR:INVALID_USER_ID";
            Session.setSelectedContactUserId(uid);
            return "OK";
        } catch (Exception e) {
            return "ERROR:INVALID_USER_ID";
        }
    }

    public String getMessagerieContactsJson() {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "[]";
            List<MessagerieCRUD.ContactRow> list = messagerieCRUD.listContactsForUser(current.getId(), 200);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (MessagerieCRUD.ContactRow c : list) {
                User u = userCRUD.findById(c.otherUserId);
                if (u == null) continue;
                if (!first) sb.append(",");
                first = false;
                String nom = ((u.getNom() == null ? "" : u.getNom().trim()) + " " + (u.getPrenom() == null ? "" : u.getPrenom().trim())).trim();
                if (nom.isBlank()) nom = u.getEmail() == null ? ("User #" + u.getId()) : u.getEmail().trim();
                sb.append("{")
                        .append("\"user_id\":").append(u.getId()).append(",")
                        .append("\"nom\":").append(jsonString(nom)).append(",")
                        .append("\"email\":").append(jsonString(u.getEmail() == null ? "" : u.getEmail())).append(",")
                        .append("\"telephone\":").append(jsonString(u.getTelephone() == null ? "" : u.getTelephone())).append(",")
                        .append("\"last_message\":").append(jsonString(c.lastMessage == null ? "" : c.lastMessage)).append(",")
                        .append("\"last_at\":").append(jsonString(c.lastCreatedAt == null ? "" : c.lastCreatedAt.toString())).append(",")
                        .append("\"unread_count\":").append(c.unreadCount)
                        .append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String getConversationMessagesJson(String otherUserId) {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "[]";
            int otherId = Integer.parseInt(otherUserId == null ? "" : otherUserId.trim());
            if (otherId <= 0) return "[]";
            List<MessagerieCRUD.MessageRow> list = messagerieCRUD.listConversation(current.getId(), otherId, 1000);
            messagerieCRUD.markConversationRead(current.getId(), otherId);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (MessagerieCRUD.MessageRow m : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{")
                        .append("\"id\":").append(m.id).append(",")
                        .append("\"sender_user_id\":").append(m.senderUserId).append(",")
                        .append("\"receiver_user_id\":").append(m.receiverUserId).append(",")
                        .append("\"contenu\":").append(jsonString(m.contenu == null ? "" : m.contenu)).append(",")
                        .append("\"created_at\":").append(jsonString(m.createdAt == null ? "" : m.createdAt.toString()))
                        .append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public String sendMessageToUser(String otherUserId, String contenu) {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "ERROR:USER_NOT_CONNECTED";
            int otherId = Integer.parseInt(otherUserId == null ? "" : otherUserId.trim());
            if (otherId <= 0) return "ERROR:INVALID_USER_ID";
            if (otherId == current.getId()) return "ERROR:CANNOT_MESSAGE_SELF";
            String text = contenu == null ? "" : contenu.trim();
            if (text.isEmpty()) return "ERROR:EMPTY_MESSAGE";
            if (text.length() > 4000) text = text.substring(0, 4000);
            User other = userCRUD.findById(otherId);
            if (other == null) return "ERROR:USER_NOT_FOUND";
            messagerieCRUD.sendMessage(current.getId(), otherId, text);
            Session.setSelectedContactUserId(otherId);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    public String getUserSummaryJson(String userId) {
        try {
            int uid = Integer.parseInt(userId == null ? "" : userId.trim());
            User u = userCRUD.findById(uid);
            if (u == null) return "null";
            String nom = ((u.getNom() == null ? "" : u.getNom().trim()) + " " + (u.getPrenom() == null ? "" : u.getPrenom().trim())).trim();
            if (nom.isBlank()) nom = u.getEmail() == null ? ("User #" + u.getId()) : u.getEmail().trim();
            return "{"
                    + "\"user_id\":" + u.getId() + ","
                    + "\"nom\":" + jsonString(nom) + ","
                    + "\"email\":" + jsonString(u.getEmail() == null ? "" : u.getEmail()) + ","
                    + "\"telephone\":" + jsonString(u.getTelephone() == null ? "" : u.getTelephone()) + ","
                    + "\"role\":" + jsonString(u.getRole() == null ? "" : u.getRole().name())
                    + "}";
        } catch (Exception e) {
            return "null";
        }
    }

    public String getUnreadMessagesCount() {
        try {
            User current = Session.getCurrentUser();
            if (current == null) return "0";
            return String.valueOf(messagerieCRUD.countUnreadForUser(current.getId()));
        } catch (Exception e) {
            return "0";
        }
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
