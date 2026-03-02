package Services;

import Entities.ModerationAction;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModerationActionCRUD {

    private final Connection conn;

    public ModerationActionCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    // appelée par AdminDashboardBridge.log(...)
    public void logAction(int adminId, String decision, String typeCible, int idCible, String commentaire) throws SQLException {
        String sql = "INSERT INTO moderation_actions(admin_id, type_cible, id_cible, decision, commentaire, cree_le) " +
                "VALUES (?,?,?,?,?, NOW())";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, adminId);
            pst.setString(2, typeCible);
            pst.setInt(3, idCible);
            pst.setString(4, decision);
            pst.setString(5, commentaire);
            pst.executeUpdate();
        }
    }


    public List<ModerationAction> getAll() throws SQLException {

        // Alias pour que ton AdminDashboardBridge.toHistoryJson continue sans changer
        String sql = "SELECT " +
                "id, admin_id, " +
                "decision AS action, " +
                "type_cible AS targetType, " +
                "id_cible AS targetId, " +
                "commentaire AS details, " +
                "cree_le AS createdAt " +
                "FROM moderation_actions " +
                "ORDER BY cree_le DESC";

        List<ModerationAction> list = new ArrayList<>();

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                ModerationAction m = new ModerationAction();
                m.setId(rs.getInt("id"));
                m.setAdminId(rs.getInt("admin_id"));
                m.setAction(rs.getString("action"));
                m.setTargetType(rs.getString("targetType"));
                m.setTargetId(rs.getInt("targetId"));
                m.setDetails(rs.getString("details"));
                m.setCreatedAt(rs.getTimestamp("createdAt"));

                list.add(m);
            }
        }
        return list;
    }
}
