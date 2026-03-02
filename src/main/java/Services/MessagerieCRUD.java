package Services;

import Utils.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MessagerieCRUD {
    private final Connection conn = MyBD.getInstance().getConn();

    public MessagerieCRUD() {
        ensureSchema();
    }

    private void ensureSchema() {
        if (conn == null) return;
        String sql = """
                CREATE TABLE IF NOT EXISTS messagerie_messages (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  sender_user_id INT NOT NULL,
                  receiver_user_id INT NOT NULL,
                  contenu TEXT NOT NULL,
                  is_read TINYINT(1) NOT NULL DEFAULT 0,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_sender_created (sender_user_id, created_at),
                  INDEX idx_receiver_created (receiver_user_id, created_at),
                  INDEX idx_pair_created (sender_user_id, receiver_user_id, created_at)
                )
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public void sendMessage(int senderUserId, int receiverUserId, String contenu) throws Exception {
        String sql = "INSERT INTO messagerie_messages(sender_user_id, receiver_user_id, contenu, is_read) VALUES (?,?,?,0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderUserId);
            ps.setInt(2, receiverUserId);
            ps.setString(3, contenu);
            ps.executeUpdate();
        }
    }

    public List<MessageRow> listConversation(int userA, int userB, int limit) throws Exception {
        String sql = """
                SELECT id, sender_user_id, receiver_user_id, contenu, is_read, created_at
                FROM messagerie_messages
                WHERE (sender_user_id=? AND receiver_user_id=?)
                   OR (sender_user_id=? AND receiver_user_id=?)
                ORDER BY created_at ASC, id ASC
                LIMIT ?
                """;
        List<MessageRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);
            ps.setInt(5, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MessageRow m = new MessageRow();
                    m.id = rs.getLong("id");
                    m.senderUserId = rs.getInt("sender_user_id");
                    m.receiverUserId = rs.getInt("receiver_user_id");
                    m.contenu = rs.getString("contenu");
                    m.isRead = rs.getBoolean("is_read");
                    m.createdAt = rs.getTimestamp("created_at");
                    out.add(m);
                }
            }
        }
        return out;
    }

    public List<ContactRow> listContactsForUser(int userId, int limit) throws Exception {
        String sql = """
                SELECT
                  x.other_user_id,
                  x.last_message,
                  x.last_created_at,
                  COALESCE(u.unread_count, 0) AS unread_count
                FROM (
                  SELECT
                    CASE
                      WHEN sender_user_id = ? THEN receiver_user_id
                      ELSE sender_user_id
                    END AS other_user_id,
                    SUBSTRING_INDEX(
                      GROUP_CONCAT(contenu ORDER BY created_at DESC, id DESC SEPARATOR '\\n'),
                      '\\n',
                      1
                    ) AS last_message,
                    MAX(created_at) AS last_created_at
                  FROM messagerie_messages
                  WHERE sender_user_id = ? OR receiver_user_id = ?
                  GROUP BY other_user_id
                ) x
                LEFT JOIN (
                  SELECT sender_user_id AS other_user_id, COUNT(*) AS unread_count
                  FROM messagerie_messages
                  WHERE receiver_user_id = ? AND is_read = 0
                  GROUP BY sender_user_id
                ) u ON u.other_user_id = x.other_user_id
                ORDER BY x.last_created_at DESC
                LIMIT ?
                """;
        List<ContactRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, userId);
            ps.setInt(5, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ContactRow c = new ContactRow();
                    c.otherUserId = rs.getInt("other_user_id");
                    c.lastMessage = rs.getString("last_message");
                    c.lastCreatedAt = rs.getTimestamp("last_created_at");
                    c.unreadCount = rs.getInt("unread_count");
                    out.add(c);
                }
            }
        }
        return out;
    }

    public void markConversationRead(int currentUserId, int otherUserId) throws Exception {
        String sql = "UPDATE messagerie_messages SET is_read=1 WHERE receiver_user_id=? AND sender_user_id=? AND is_read=0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, otherUserId);
            ps.executeUpdate();
        }
    }

    public int countUnreadForUser(int userId) throws Exception {
        String sql = "SELECT COUNT(*) AS c FROM messagerie_messages WHERE receiver_user_id=? AND is_read=0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        }
        return 0;
    }

    public static class MessageRow {
        public long id;
        public int senderUserId;
        public int receiverUserId;
        public String contenu;
        public boolean isRead;
        public Timestamp createdAt;
    }

    public static class ContactRow {
        public int otherUserId;
        public String lastMessage;
        public Timestamp lastCreatedAt;
        public int unreadCount;
    }
}
