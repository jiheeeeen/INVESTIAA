package Services;

import Entities.User;
import Utils.MyBD;
import Utils.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AuditService {
    private AuditService() {}

    public static void ensureAuditTable() {
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return;
        String sql = "CREATE TABLE IF NOT EXISTS audit_log ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "user_id INT NULL,"
                + "user_email VARCHAR(190) NULL,"
                + "module VARCHAR(80) NOT NULL,"
                + "action VARCHAR(120) NOT NULL,"
                + "level VARCHAR(20) NOT NULL DEFAULT 'INFO',"
                + "details TEXT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX idx_audit_user (user_id),"
                + "INDEX idx_audit_module (module),"
                + "INDEX idx_audit_level (level),"
                + "INDEX idx_audit_created (created_at)"
                + ")";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static void logCurrentUser(String module, String action, String details, String level) {
        User u = Session.getCurrentUser();
        int uid = u == null ? 0 : u.getId();
        String email = u == null ? "" : safe(u.getEmail());
        log(uid, email, module, action, details, level);
    }

    public static void log(int userId, String userEmail, String module, String action, String details, String level) {
        ensureAuditTable();
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return;
        String sql = "INSERT INTO audit_log (user_id, user_email, module, action, level, details) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            if (userId > 0) pst.setInt(1, userId);
            else pst.setNull(1, java.sql.Types.INTEGER);
            pst.setString(2, safe(userEmail));
            pst.setString(3, safe(module));
            pst.setString(4, safe(action));
            pst.setString(5, normalizeLevel(level));
            pst.setString(6, safe(details));
            pst.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static int countUnready() {
        ensureAuditTable();
        Connection conn = MyBD.getInstance().getConn();
        if (conn == null) return 0;
        try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) c FROM audit_log");
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        } catch (SQLException ignored) {
        }
        return 0;
    }

    private static String normalizeLevel(String level) {
        String s = safe(level).toUpperCase();
        if ("WARN".equals(s) || "ERROR".equals(s) || "CRITICAL".equals(s)) return s;
        return "INFO";
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
