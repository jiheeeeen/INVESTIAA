package Services;

import Utils.MyBD;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ProjetFluxCRUD {
    private final Connection conn;

    public ProjetFluxCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    public void ensureSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS projet_flux (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "projet_id INT NOT NULL," +
                "type_flux VARCHAR(16) NOT NULL," +
                "description TEXT NULL," +
                "montant DECIMAL(15,2) NOT NULL DEFAULT 0," +
                "date_flux DATE NOT NULL," +
                "created_by INT NULL," +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "CONSTRAINT chk_type_flux CHECK (type_flux IN ('CHARGE','GAIN'))," +
                "CONSTRAINT fk_projet_flux_projet FOREIGN KEY (projet_id) REFERENCES projet(id_projet) ON DELETE CASCADE" +
                ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    public int ajouter(int projetId, String typeFlux, String description, double montant, Date dateFlux, int createdBy) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO projet_flux (projet_id, type_flux, description, montant, date_flux, created_by) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projetId);
            ps.setString(2, typeFlux);
            ps.setString(3, description);
            ps.setDouble(4, montant);
            ps.setDate(5, dateFlux);
            if (createdBy > 0) ps.setInt(6, createdBy); else ps.setNull(6, java.sql.Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<FluxRow> listByProject(int projetId, String typeFlux, int limit) throws SQLException {
        ensureSchema();
        String sql = "SELECT id, projet_id, type_flux, description, montant, date_flux, created_by, created_at " +
                "FROM projet_flux WHERE projet_id=? AND type_flux=? ORDER BY date_flux DESC, id DESC LIMIT ?";
        List<FluxRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projetId);
            ps.setString(2, typeFlux);
            ps.setInt(3, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FluxRow r = new FluxRow();
                    r.id = rs.getInt("id");
                    r.projetId = rs.getInt("projet_id");
                    r.typeFlux = rs.getString("type_flux");
                    r.description = rs.getString("description");
                    r.montant = rs.getDouble("montant");
                    r.dateFlux = rs.getDate("date_flux");
                    r.createdBy = rs.getInt("created_by");
                    r.createdAt = rs.getTimestamp("created_at");
                    out.add(r);
                }
            }
        }
        return out;
    }

    public FluxRow getById(int id) throws SQLException {
        ensureSchema();
        String sql = "SELECT id, projet_id, type_flux, description, montant, date_flux, created_by, created_at FROM projet_flux WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                FluxRow r = new FluxRow();
                r.id = rs.getInt("id");
                r.projetId = rs.getInt("projet_id");
                r.typeFlux = rs.getString("type_flux");
                r.description = rs.getString("description");
                r.montant = rs.getDouble("montant");
                r.dateFlux = rs.getDate("date_flux");
                r.createdBy = rs.getInt("created_by");
                r.createdAt = rs.getTimestamp("created_at");
                return r;
            }
        }
    }

    public int updateById(int id, String description, double montant, Date dateFlux) throws SQLException {
        ensureSchema();
        String sql = "UPDATE projet_flux SET description=?, montant=?, date_flux=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setDouble(2, montant);
            ps.setDate(3, dateFlux);
            ps.setInt(4, id);
            return ps.executeUpdate();
        }
    }

    public int deleteById(int id) throws SQLException {
        ensureSchema();
        String sql = "DELETE FROM projet_flux WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    public static class FluxRow {
        public int id;
        public int projetId;
        public String typeFlux;
        public String description;
        public double montant;
        public Date dateFlux;
        public int createdBy;
        public Timestamp createdAt;
    }
}
