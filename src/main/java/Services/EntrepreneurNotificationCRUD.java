package Services;

import Utils.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EntrepreneurNotificationCRUD {
    private final Connection conn;

    public EntrepreneurNotificationCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    public void insert(int entrepreneurId, int investissementId) {
        String sql = "INSERT INTO entrepreneur_notifications (entrepreneur_id, investissement_id, is_read) VALUES (?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entrepreneurId);
            ps.setInt(2, investissementId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public List<NotificationRow> listUnread(int entrepreneurId) {
        List<NotificationRow> list = new ArrayList<>();
        String sql = "SELECT n.id, n.investissement_id, n.created_at, i.id_projet, i.date_investissement, p.titre " +
                "FROM entrepreneur_notifications n " +
                "JOIN investissement i ON i.id_investissement = n.investissement_id " +
                "LEFT JOIN projet p ON p.id_projet = i.id_projet " +
                "WHERE n.entrepreneur_id = ? AND n.is_read = 0 " +
                "ORDER BY n.created_at DESC, n.id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificationRow row = new NotificationRow();
                    row.id = rs.getInt("id");
                    row.investissementId = rs.getInt("investissement_id");
                    row.idProjet = rs.getInt("id_projet");
                    row.titreProjet = rs.getString("titre");
                    row.dateInvestissement = rs.getDate("date_investissement");
                    row.createdAt = rs.getTimestamp("created_at");
                    row.isRead = false;
                    list.add(row);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public List<NotificationRow> listRecent(int entrepreneurId, int limit) {
        List<NotificationRow> list = new ArrayList<>();
        String sql = "SELECT n.id, n.investissement_id, n.created_at, n.is_read, i.id_projet, i.date_investissement, p.titre " +
                "FROM entrepreneur_notifications n " +
                "JOIN investissement i ON i.id_investissement = n.investissement_id " +
                "LEFT JOIN projet p ON p.id_projet = i.id_projet " +
                "WHERE n.entrepreneur_id = ? " +
                "ORDER BY n.created_at DESC, n.id DESC " +
                "LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificationRow row = new NotificationRow();
                    row.id = rs.getInt("id");
                    row.investissementId = rs.getInt("investissement_id");
                    row.idProjet = rs.getInt("id_projet");
                    row.titreProjet = rs.getString("titre");
                    row.dateInvestissement = rs.getDate("date_investissement");
                    row.createdAt = rs.getTimestamp("created_at");
                    row.isRead = rs.getInt("is_read") != 0;
                    list.add(row);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public int countAll(int entrepreneurId) {
        String sql = "SELECT COUNT(*) FROM entrepreneur_notifications WHERE entrepreneur_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entrepreneurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public void markRead(int notificationId) {
        String sql = "UPDATE entrepreneur_notifications SET is_read = 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public static class NotificationRow {
        public int id;
        public int investissementId;
        public int idProjet;
        public String titreProjet;
        public java.sql.Date dateInvestissement;
        public Timestamp createdAt;
        public boolean isRead;
    }
}
