package Services;

import Entities.Decision;
import Entities.ModerationAction;
import Entities.TargetType;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModerationActionCRUD {

    private final Connection conn;

    public ModerationActionCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    private ModerationAction map(ResultSet rs) throws SQLException {
        ModerationAction m = new ModerationAction();
        m.setId(rs.getInt("id"));
        m.setAdminId(rs.getInt("admin_id"));
        m.setAction(rs.getString("action"));
        m.setTargetType(rs.getString("target_type"));
        m.setTargetId(rs.getInt("target_id"));
        m.setDetails(rs.getString("details"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        return m;
    }

    // ✅ Pour compatibilité avec ton ancien code : modCRUD.add(new ModerationAction(...))
    public void add(ModerationAction a) throws SQLException {
        logAction(a.getAdminId(), a.getAction(), a.getTargetType(), a.getTargetId(), a.getDetails());
    }

    // ✅ Utilisée par AdminDashboardBridge
    public void logAction(int adminId, String action, String targetType, int targetId, String details) throws SQLException {
        String sql = "INSERT INTO moderation_actions(admin_id, action, target_type, target_id, details) VALUES (?,?,?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, adminId);
            pst.setString(2, action);
            pst.setString(3, targetType);
            pst.setInt(4, targetId);
            pst.setString(5, details);
            pst.executeUpdate();
        }
    }

    public List<ModerationAction> getAll() throws SQLException {
        String sql = "SELECT * FROM moderation_actions ORDER BY created_at DESC";
        List<ModerationAction> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                ModerationAction m = new ModerationAction();
                m.setId(rs.getInt("id"));
                m.setAdminId(rs.getInt("admin_id"));
                m.setAction(rs.getString("action"));
                m.setTargetType(rs.getString("target_type"));
                m.setTargetId(rs.getInt("target_id"));
                m.setDetails(rs.getString("details"));
                m.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(m);
            }
        }
        return list;
    }

    public List<ModerationAction> getByAdmin(int adminId) throws SQLException {
        String sql = "SELECT * FROM moderation_actions WHERE admin_id=? ORDER BY created_at DESC";
        List<ModerationAction> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, adminId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM moderation_actions WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }
}
