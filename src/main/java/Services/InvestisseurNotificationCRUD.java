package Services;

import Utils.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class InvestisseurNotificationCRUD {
    private final Connection conn;

    public InvestisseurNotificationCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    public void insertForAllInvestisseurs(int projetId) {
        String sql = "INSERT IGNORE INTO investisseur_notifications (investisseur_id, projet_id, is_read) " +
                "SELECT id, ?, 0 FROM users " +
                "WHERE role = 'INVESTISSEUR' AND statut_verification = 'VERIFIE' AND est_actif = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projetId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public List<NotificationRow> listUnread(int investisseurId) {
        List<NotificationRow> list = new ArrayList<>();
        String sql = "SELECT n.id, n.projet_id, n.created_at, p.titre " +
                "FROM investisseur_notifications n " +
                "LEFT JOIN projet p ON p.id_projet = n.projet_id " +
                "WHERE n.investisseur_id = ? AND n.is_read = 0 " +
                "ORDER BY n.created_at DESC, n.id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investisseurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificationRow row = new NotificationRow();
                    row.id = rs.getInt("id");
                    row.idProjet = rs.getInt("projet_id");
                    row.titreProjet = rs.getString("titre");
                    row.createdAt = rs.getTimestamp("created_at");
                    row.isRead = false;
                    list.add(row);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public List<NotificationRow> listRecent(int investisseurId, int limit) {
        List<NotificationRow> list = new ArrayList<>();
        String sql = "SELECT n.id, n.projet_id, n.created_at, n.is_read, p.titre " +
                "FROM investisseur_notifications n " +
                "LEFT JOIN projet p ON p.id_projet = n.projet_id " +
                "WHERE n.investisseur_id = ? " +
                "ORDER BY n.created_at DESC, n.id DESC " +
                "LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investisseurId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificationRow row = new NotificationRow();
                    row.id = rs.getInt("id");
                    row.idProjet = rs.getInt("projet_id");
                    row.titreProjet = rs.getString("titre");
                    row.createdAt = rs.getTimestamp("created_at");
                    row.isRead = rs.getInt("is_read") != 0;
                    list.add(row);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public int countAll(int investisseurId) {
        String sql = "SELECT COUNT(*) FROM investisseur_notifications WHERE investisseur_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investisseurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public void markRead(int notificationId) {
        String sql = "UPDATE investisseur_notifications SET is_read = 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public static class NotificationRow {
        public int id;
        public int idProjet;
        public String titreProjet;
        public Timestamp createdAt;
        public boolean isRead;
    }
}
