package Services;

import Entities.Financement2;
import Utils.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class Financement2CRUD implements InterfaceCRUD<Financement2> {

    private Connection conn;

    public Financement2CRUD() {
        conn = MyBD.getInstance().getConn();
    }

    private Connection requireConn() throws SQLException {
        if (conn == null || conn.isClosed() || !conn.isValid(2)) {
            conn = MyBD.getInstance().getConn();
        }
        if (conn == null) {
            throw new SQLException("Database connection is null.");
        }
        try {
            if (!conn.getAutoCommit()) conn.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
        return conn;
    }

    @Override
    public void ajouter(Financement2 f) throws SQLException {
        ajouterWithGeneratedId(f);
    }

    public int ajouterWithGeneratedId(Financement2 f) throws SQLException {
        requireConn();
        String req = "INSERT INTO financement2 (id_projet, id_investissement, montant, frais_pct, mode_paiement, statut, " +
                "taux_interet_pct, duree_estimee_mois, note) VALUES (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, f.getId_projet());
            pst.setInt(2, f.getId_investissement());
            pst.setDouble(3, f.getMontant());
            pst.setDouble(4, f.getFrais_pct());
            pst.setString(5, f.getMode_paiement());
            pst.setString(6, f.getStatut());
            pst.setDouble(7, f.getTaux_interet_pct());
            pst.setInt(8, f.getDuree_estimee_mois());
            if (f.getNote() == null) {
                pst.setNull(9, Types.VARCHAR);
            } else {
                pst.setString(9, f.getNote());
            }
            int rows = pst.executeUpdate();
            if (rows <= 0) {
                throw new SQLException("Insert failed: no rows affected.");
            }
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    f.setId_financement(id);
                    return id;
                }
            }
        }

        // Fallback for drivers that don't return generated keys.
        try (PreparedStatement pst = conn.prepareStatement("SELECT LAST_INSERT_ID()")) {
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    f.setId_financement(id);
                    return id;
                }
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    @Override
    public void modifier(Financement2 f) throws SQLException {
        requireConn();
        String req = "UPDATE financement2 SET id_projet=?, id_investissement=?, montant=?, frais_pct=?, mode_paiement=?, statut=?, " +
                "taux_interet_pct=?, duree_estimee_mois=?, note=? WHERE id_financement=?";

        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, f.getId_projet());
            pst.setInt(2, f.getId_investissement());
            pst.setDouble(3, f.getMontant());
            pst.setDouble(4, f.getFrais_pct());
            pst.setString(5, f.getMode_paiement());
            pst.setString(6, f.getStatut());
            pst.setDouble(7, f.getTaux_interet_pct());
            pst.setInt(8, f.getDuree_estimee_mois());
            pst.setString(9, f.getNote());
            pst.setInt(10, f.getId_financement());
            int rows = pst.executeUpdate();
            if (rows <= 0) {
                throw new SQLException("Update failed: no rows affected (id_financement=" + f.getId_financement() + ").");
            }
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        requireConn();
        String req = "DELETE FROM financement2 WHERE id_financement=?";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, id);
            int rows = pst.executeUpdate();
            if (rows <= 0) {
                throw new SQLException("Delete failed: no rows affected (id_financement=" + id + ").");
            }
        }
    }

    @Override
    public List<Financement2> afficher() throws SQLException {
        requireConn();
        String req = "SELECT * FROM financement2 ORDER BY id_financement DESC";
        List<Financement2> liste = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Financement2 f = mapRow(rs);
                liste.add(f);
            }
        }
        return liste;
    }

    public Financement2 getById(int id) throws SQLException {
        requireConn();
        String sql = "SELECT * FROM financement2 WHERE id_financement=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Financement2 getLatestByInvestissementId(int investissementId) throws SQLException {
        requireConn();
        String sql = "SELECT * FROM financement2 WHERE id_investissement=? ORDER BY id_financement DESC LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, investissementId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public boolean existsByInvestissementId(int investissementId) throws SQLException {
        requireConn();
        String sql = "SELECT 1 FROM financement2 WHERE id_investissement=? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, investissementId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByInvestissementIdExcluding(int investissementId, int financementId) throws SQLException {
        requireConn();
        String sql = "SELECT 1 FROM financement2 WHERE id_investissement=? AND id_financement<>? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, investissementId);
            pst.setInt(2, financementId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    public double totalConfirmeParProjet(int idProjet) throws SQLException {
        requireConn();
        String req = "SELECT COALESCE(SUM(montant),0) AS total FROM financement2 WHERE id_projet=? AND statut='CONFIRMED'";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, idProjet);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0;
    }

    private static Financement2 mapRow(ResultSet rs) throws SQLException {
        Financement2 f = new Financement2();
        f.setId_financement(rs.getInt("id_financement"));
        f.setId_projet(rs.getInt("id_projet"));
        f.setId_investissement(rs.getInt("id_investissement"));
        f.setMontant(rs.getDouble("montant"));
        f.setFrais_pct(rs.getDouble("frais_pct"));
        f.setMode_paiement(rs.getString("mode_paiement"));
        f.setStatut(rs.getString("statut"));
        f.setTaux_interet_pct(rs.getDouble("taux_interet_pct"));
        f.setDuree_estimee_mois(rs.getInt("duree_estimee_mois"));
        f.setNote(rs.getString("note"));
        f.setCreated_at(rs.getTimestamp("created_at"));
        f.setUpdated_at(rs.getTimestamp("updated_at"));
        return f;
    }
}
