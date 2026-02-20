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
    private String cachedIdCol;
    private String cachedAdminIdCol;
    private String cachedActionCol;
    private String cachedTargetTypeCol;
    private String cachedTargetIdCol;
    private String cachedDetailsCol;
    private String cachedCreatedAtCol;

    public ModerationActionCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    private ModerationAction map(ResultSet rs) throws SQLException {
        ModerationAction m = new ModerationAction();
        m.setId(getIntSafe(rs, resolveIdColumn()));
        m.setAdminId(getIntSafe(rs, resolveAdminIdColumn()));
        m.setAction(getStringSafe(rs, resolveActionColumn()));
        m.setTargetType(getStringSafe(rs, resolveTargetTypeColumn()));
        m.setTargetId(getIntSafe(rs, resolveTargetIdColumn()));
        m.setDetails(getStringSafe(rs, resolveDetailsColumn()));
        m.setCreatedAt(getTimestampSafe(rs, resolveCreatedAtColumn()));
        return m;
    }

    // ✅ Pour compatibilité avec ton ancien code : modCRUD.add(new ModerationAction(...))
    public void add(ModerationAction a) throws SQLException {
        logAction(a.getAdminId(), a.getAction(), a.getTargetType(), a.getTargetId(), a.getDetails());
    }

    // ✅ Utilisée par AdminDashboardBridge
    public void logAction(int adminId, String action, String targetType, int targetId, String details) throws SQLException {
        if (conn == null) return;

        String adminCol = resolveAdminIdColumn();
        String actionCol = resolveActionColumn();
        String typeCol = resolveTargetTypeColumn();
        String targetCol = resolveTargetIdColumn();
        String detailsCol = resolveDetailsColumn();
        String createdCol = resolveCreatedAtColumn();

        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();

        if (adminCol != null) { cols.add(adminCol); vals.add(adminId); }
        if (actionCol != null) { cols.add(actionCol); vals.add(action); }
        if (typeCol != null) { cols.add(typeCol); vals.add(targetType); }
        if (targetCol != null) { cols.add(targetCol); vals.add(targetId); }
        if (detailsCol != null) { cols.add(detailsCol); vals.add(details); }
        if (createdCol != null) { cols.add(createdCol); vals.add(new Timestamp(System.currentTimeMillis())); }

        if (cols.isEmpty()) return;

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO moderation_actions(");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append(cols.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < vals.size(); i++) {
                Object v = vals.get(i);
                if (v == null) {
                    pst.setNull(i + 1, Types.NULL);
                } else if (v instanceof Integer) {
                    pst.setInt(i + 1, (Integer) v);
                } else if (v instanceof Long) {
                    pst.setLong(i + 1, (Long) v);
                } else if (v instanceof Timestamp) {
                    pst.setTimestamp(i + 1, (Timestamp) v);
                } else {
                    pst.setString(i + 1, String.valueOf(v));
                }
            }
            pst.executeUpdate();
        }
    }

    public List<ModerationAction> getAll() throws SQLException {
        String createdCol = resolveCreatedAtColumn();
        String sql = createdCol == null
                ? "SELECT * FROM moderation_actions ORDER BY id DESC"
                : "SELECT * FROM moderation_actions ORDER BY " + createdCol + " DESC";
        List<ModerationAction> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

        while (rs.next()) {
            ModerationAction m = new ModerationAction();
            m.setId(getIntSafe(rs, resolveIdColumn()));
            m.setAdminId(getIntSafe(rs, resolveAdminIdColumn()));
            m.setAction(getStringSafe(rs, resolveActionColumn()));
            m.setTargetType(getStringSafe(rs, resolveTargetTypeColumn()));
            m.setTargetId(getIntSafe(rs, resolveTargetIdColumn()));
            m.setDetails(getStringSafe(rs, resolveDetailsColumn()));
            m.setCreatedAt(getTimestampSafe(rs, createdCol));
            list.add(m);
        }
        }
        return list;
    }

    public List<ModerationAction> getByAdmin(int adminId) throws SQLException {
        String createdCol = resolveCreatedAtColumn();
        String sql = createdCol == null
                ? "SELECT * FROM moderation_actions WHERE admin_id=? ORDER BY id DESC"
                : "SELECT * FROM moderation_actions WHERE admin_id=? ORDER BY " + createdCol + " DESC";
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

    private Timestamp getTimestampSafe(ResultSet rs, String col) {
        if (col == null || col.isBlank()) return null;
        try {
            return rs.getTimestamp(col);
        } catch (Exception e) {
            return null;
        }
    }

    private String getStringSafe(ResultSet rs, String col) {
        if (col == null || col.isBlank()) return null;
        try {
            return rs.getString(col);
        } catch (Exception e) {
            return null;
        }
    }

    private int getIntSafe(ResultSet rs, String col) {
        if (col == null || col.isBlank()) return 0;
        try {
            return rs.getInt(col);
        } catch (Exception e) {
            return 0;
        }
    }

    private String resolveCreatedAtColumn() throws SQLException {
        if (cachedCreatedAtCol != null) return cachedCreatedAtCol;
        if (conn == null) return null;
        List<String> cols = loadColumns("moderation_actions");
        cachedCreatedAtCol = firstExisting(cols, "created_at", "createdAt", "date_creation", "date", "cree_le");
        return cachedCreatedAtCol;
    }

    private String resolveIdColumn() throws SQLException {
        if (cachedIdCol != null) return cachedIdCol;
        if (conn == null) return null;
        List<String> cols = loadColumns("moderation_actions");
        cachedIdCol = firstExisting(cols, "id", "id_action", "action_id");
        return cachedIdCol;
    }

    private String resolveAdminIdColumn() throws SQLException {
        if (cachedAdminIdCol != null) return cachedAdminIdCol;
        if (conn == null) return null;
        List<String> cols = loadColumns("moderation_actions");
        cachedAdminIdCol = firstExisting(cols, "admin_id", "id_admin", "adminId");
        return cachedAdminIdCol;
    }

    private String resolveActionColumn() throws SQLException {
        if (cachedActionCol != null) return cachedActionCol;
        if (conn == null) return null;
        List<String> cols = loadColumns("moderation_actions");
        cachedActionCol = firstExisting(cols, "action", "decision", "type_action", "action_type", "type");
        return cachedActionCol;
    }

    private String resolveTargetTypeColumn() throws SQLException {
        if (cachedTargetTypeCol != null) return cachedTargetTypeCol;
        if (conn == null) return null;
        List<String> cols = loadColumns("moderation_actions");
        cachedTargetTypeCol = firstExisting(cols, "target_type", "type_cible", "targetType", "type_cible");
        return cachedTargetTypeCol;
    }

    private String resolveTargetIdColumn() throws SQLException {
        if (cachedTargetIdCol != null) return cachedTargetIdCol;
        if (conn == null) return null;
        List<String> cols = loadColumns("moderation_actions");
        cachedTargetIdCol = firstExisting(cols, "target_id", "id_cible", "targetId", "id_cible");
        return cachedTargetIdCol;
    }

    private String resolveDetailsColumn() throws SQLException {
        if (cachedDetailsCol != null) return cachedDetailsCol;
        if (conn == null) return null;
        List<String> cols = loadColumns("moderation_actions");
        cachedDetailsCol = firstExisting(cols, "details", "detail", "commentaire", "comment", "commentaire");
        return cachedDetailsCol;
    }

    private List<String> loadColumns(String table) throws SQLException {
        List<String> cols = new ArrayList<>();
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null) cols.add(name);
                }
            }
        } catch (Exception ignore) {
        }
        if (!cols.isEmpty()) return cols;

        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    if (name != null) cols.add(name);
                }
            }
        }
        if (!cols.isEmpty()) return cols;

        String fallbackSql = "SELECT * FROM " + table + " LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(fallbackSql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String name = meta.getColumnName(i);
                if (name != null) cols.add(name);
            }
        } catch (Exception ignore) {
        }
        return cols;
    }

    private static String firstExisting(List<String> cols, String... candidates) {
        if (cols == null || cols.isEmpty()) return null;
        for (String c : candidates) {
            for (String col : cols) {
                if (col.equalsIgnoreCase(c)) return col;
            }
        }
        return null;
    }
}
