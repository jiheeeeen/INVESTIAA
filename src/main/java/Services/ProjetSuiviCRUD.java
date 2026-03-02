package Services;

import Utils.MyBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ProjetSuiviCRUD {
    private final Connection conn;

    public ProjetSuiviCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    public void ensureSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS projet_suivi (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "projet_id INT NOT NULL UNIQUE," +
                "date_debut_reelle DATE NULL," +
                "date_fin_cible DATE NULL," +
                "avancement_global_pct DECIMAL(5,2) NOT NULL DEFAULT 0," +
                "budget_alloue DECIMAL(15,2) NOT NULL DEFAULT 0," +
                "budget_consomme DECIMAL(15,2) NOT NULL DEFAULT 0," +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "CONSTRAINT fk_projet_suivi_projet FOREIGN KEY (projet_id) REFERENCES projet(id_projet) ON DELETE CASCADE" +
                ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    public void ensureForProject(int projetId, BigDecimal budgetAlloue) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO projet_suivi (projet_id, date_debut_reelle, avancement_global_pct, budget_alloue, budget_consomme) " +
                "VALUES (?, CURDATE(), 0, ?, 0) " +
                "ON DUPLICATE KEY UPDATE budget_alloue=VALUES(budget_alloue)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projetId);
            ps.setBigDecimal(2, budgetAlloue == null ? BigDecimal.ZERO : budgetAlloue);
            ps.executeUpdate();
        }
    }

    public void applyTaskUpdate(int projetId, double progressionDelta, double coutTache) throws SQLException {
        String sql = "UPDATE projet_suivi " +
                "SET avancement_global_pct = LEAST(100, GREATEST(0, COALESCE(avancement_global_pct,0) + ?)), " +
                "budget_consomme = GREATEST(0, COALESCE(budget_consomme,0) + ?), updated_at = NOW() " +
                "WHERE projet_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, progressionDelta);
            ps.setDouble(2, coutTache);
            ps.setInt(3, projetId);
            ps.executeUpdate();
        }
    }

    public void setExecutionValues(int projetId, double avancementPct, double budgetConsomme) throws SQLException {
        String sql = "UPDATE projet_suivi SET avancement_global_pct=?, budget_consomme=?, updated_at=NOW() WHERE projet_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, Math.max(0, Math.min(100, avancementPct)));
            ps.setDouble(2, Math.max(0, budgetConsomme));
            ps.setInt(3, projetId);
            ps.executeUpdate();
        }
    }

    public SuiviRow getByProjectId(int projetId) throws SQLException {
        ensureSchema();
        String sql = "SELECT projet_id, date_debut_reelle, date_fin_cible, avancement_global_pct, budget_alloue, budget_consomme, updated_at " +
                "FROM projet_suivi WHERE projet_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                SuiviRow row = new SuiviRow();
                row.projetId = rs.getInt("projet_id");
                row.dateDebutReelle = rs.getDate("date_debut_reelle");
                row.dateFinCible = rs.getDate("date_fin_cible");
                row.avancementPct = rs.getDouble("avancement_global_pct");
                row.budgetAlloue = rs.getBigDecimal("budget_alloue");
                row.budgetConsomme = rs.getBigDecimal("budget_consomme");
                row.updatedAt = rs.getTimestamp("updated_at");
                return row;
            }
        }
    }

    public static class SuiviRow {
        public int projetId;
        public Date dateDebutReelle;
        public Date dateFinCible;
        public double avancementPct;
        public BigDecimal budgetAlloue;
        public BigDecimal budgetConsomme;
        public Timestamp updatedAt;
    }
}
