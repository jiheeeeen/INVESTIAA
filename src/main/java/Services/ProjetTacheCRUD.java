package Services;

import Utils.MyBD;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProjetTacheCRUD {
    private final Connection conn;

    public ProjetTacheCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    public void ensureSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS projet_tache (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "projet_id INT NOT NULL," +
                "titre VARCHAR(180) NOT NULL," +
                "description TEXT NULL," +
                "date_tache DATE NOT NULL," +
                "date_debut DATE NULL," +
                "date_fin DATE NULL," +
                "calendar_event_id VARCHAR(191) NULL," +
                "calendar_status VARCHAR(255) NULL," +
                "calendar_synced_at TIMESTAMP NULL," +
                "progression_delta DECIMAL(5,2) NOT NULL DEFAULT 0," +
                "cout_tache DECIMAL(15,2) NOT NULL DEFAULT 0," +
                "statut VARCHAR(32) NOT NULL DEFAULT 'TERMINE'," +
                "created_by INT NULL," +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "CONSTRAINT fk_projet_tache_projet FOREIGN KEY (projet_id) REFERENCES projet(id_projet) ON DELETE CASCADE" +
                ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
        migrateDateColumnsIfNeeded();
    }

    private void migrateDateColumnsIfNeeded() throws SQLException {
        if (!hasColumn("projet_tache", "date_debut")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE projet_tache ADD COLUMN date_debut DATE NULL");
            }
        }
        if (!hasColumn("projet_tache", "date_fin")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE projet_tache ADD COLUMN date_fin DATE NULL");
            }
        }
        if (!hasColumn("projet_tache", "calendar_event_id")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE projet_tache ADD COLUMN calendar_event_id VARCHAR(191) NULL");
            }
        }
        if (!hasColumn("projet_tache", "calendar_status")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE projet_tache ADD COLUMN calendar_status VARCHAR(255) NULL");
            }
        } else {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE projet_tache MODIFY COLUMN calendar_status VARCHAR(255) NULL");
            } catch (SQLException ignored) {
            }
        }
        if (!hasColumn("projet_tache", "calendar_synced_at")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE projet_tache ADD COLUMN calendar_synced_at TIMESTAMP NULL");
            }
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE projet_tache SET date_debut = COALESCE(date_debut, date_tache)");
            st.executeUpdate("UPDATE projet_tache SET date_fin = COALESCE(date_fin, date_tache)");
        }
    }

    private boolean hasColumn(String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    public int ajouter(int projetId,
                       String titre,
                       String description,
                       Date dateDebut,
                       Date dateFin,
                       double progressionDelta,
                       double coutTache,
                       int createdBy) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO projet_tache (projet_id, titre, description, date_tache, date_debut, date_fin, progression_delta, cout_tache, statut, created_by) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projetId);
            ps.setString(2, titre);
            ps.setString(3, description);
            ps.setDate(4, dateDebut);
            ps.setDate(5, dateDebut);
            ps.setDate(6, dateFin);
            ps.setDouble(7, progressionDelta);
            ps.setDouble(8, coutTache);
            ps.setString(9, "TERMINE");
            if (createdBy > 0) ps.setInt(10, createdBy); else ps.setNull(10, java.sql.Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<TacheRow> listByProject(int projetId, int limit) throws SQLException {
        ensureSchema();
        String sql = "SELECT id, projet_id, titre, description, date_tache, date_debut, date_fin, calendar_event_id, calendar_status, calendar_synced_at, progression_delta, cout_tache, statut, created_by, created_at " +
                "FROM projet_tache WHERE projet_id=? ORDER BY COALESCE(date_fin, date_debut, date_tache) DESC, id DESC LIMIT ?";
        List<TacheRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projetId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TacheRow t = new TacheRow();
                    t.id = rs.getInt("id");
                    t.projetId = rs.getInt("projet_id");
                    t.titre = rs.getString("titre");
                    t.description = rs.getString("description");
                    t.dateTache = rs.getDate("date_tache");
                    t.dateDebut = rs.getDate("date_debut");
                    t.dateFin = rs.getDate("date_fin");
                    t.calendarEventId = rs.getString("calendar_event_id");
                    t.calendarStatus = rs.getString("calendar_status");
                    t.calendarSyncedAt = rs.getTimestamp("calendar_synced_at");
                    t.progressionDelta = rs.getDouble("progression_delta");
                    t.coutTache = rs.getDouble("cout_tache");
                    t.statut = rs.getString("statut");
                    t.createdBy = rs.getInt("created_by");
                    t.createdAt = rs.getTimestamp("created_at");
                    rows.add(t);
                }
            }
        }
        return rows;
    }

    public TacheRow getById(int id) throws SQLException {
        ensureSchema();
        String sql = "SELECT id, projet_id, titre, description, date_tache, date_debut, date_fin, calendar_event_id, calendar_status, calendar_synced_at, progression_delta, cout_tache, statut, created_by, created_at FROM projet_tache WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                TacheRow t = new TacheRow();
                t.id = rs.getInt("id");
                t.projetId = rs.getInt("projet_id");
                t.titre = rs.getString("titre");
                t.description = rs.getString("description");
                t.dateTache = rs.getDate("date_tache");
                t.dateDebut = rs.getDate("date_debut");
                t.dateFin = rs.getDate("date_fin");
                t.calendarEventId = rs.getString("calendar_event_id");
                t.calendarStatus = rs.getString("calendar_status");
                t.calendarSyncedAt = rs.getTimestamp("calendar_synced_at");
                t.progressionDelta = rs.getDouble("progression_delta");
                t.coutTache = rs.getDouble("cout_tache");
                t.statut = rs.getString("statut");
                t.createdBy = rs.getInt("created_by");
                t.createdAt = rs.getTimestamp("created_at");
                return t;
            }
        }
    }

    public int updateById(int id, String titre, String description, Date dateDebut, Date dateFin, double progressionDelta, double coutTache) throws SQLException {
        ensureSchema();
        String sql = "UPDATE projet_tache SET titre=?, description=?, date_tache=?, date_debut=?, date_fin=?, progression_delta=?, cout_tache=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setString(2, description);
            ps.setDate(3, dateDebut);
            ps.setDate(4, dateDebut);
            ps.setDate(5, dateFin);
            ps.setDouble(6, progressionDelta);
            ps.setDouble(7, coutTache);
            ps.setInt(8, id);
            return ps.executeUpdate();
        }
    }

    public int deleteById(int id) throws SQLException {
        ensureSchema();
        String sql = "DELETE FROM projet_tache WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    public int markCalendarSync(int id, String eventId, String status) throws SQLException {
        ensureSchema();
        String sql = "UPDATE projet_tache SET calendar_event_id=?, calendar_status=?, calendar_synced_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, status);
            ps.setInt(3, id);
            return ps.executeUpdate();
        }
    }

    public static class TacheRow {
        public int id;
        public int projetId;
        public String titre;
        public String description;
        public Date dateTache;
        public Date dateDebut;
        public Date dateFin;
        public String calendarEventId;
        public String calendarStatus;
        public Timestamp calendarSyncedAt;
        public double progressionDelta;
        public double coutTache;
        public String statut;
        public int createdBy;
        public Timestamp createdAt;
    }
}
