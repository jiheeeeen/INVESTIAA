package Services;

import Entities.DemandeAnnulation;
import Utils.MyBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DemandeAnnulationCRUD {
    private final Connection conn;

    public DemandeAnnulationCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    public void ajouter(DemandeAnnulation d) throws SQLException {
        String sql = "INSERT INTO demande_annulation (projet_id, raison, statut, created_at) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getProjetId());
            ps.setString(2, d.getRaison());
            ps.setString(3, d.getStatut() != null ? d.getStatut() : "EN_ATTENTE");
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) d.setId(rs.getInt(1));
            }
        }
    }

    public List<DemandeAnnulation> afficher() throws SQLException {
        String sql = "SELECT d.id, d.projet_id, d.raison, d.statut, d.created_at, p.titre " +
                "FROM demande_annulation d " +
                "JOIN projet p ON p.id_projet = d.projet_id " +
                "ORDER BY d.id DESC";
        List<DemandeAnnulation> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DemandeAnnulation d = new DemandeAnnulation();
                d.setId(rs.getInt("id"));
                d.setProjetId(rs.getInt("projet_id"));
                d.setRaison(rs.getString("raison"));
                d.setStatut(rs.getString("statut"));
                d.setCreatedAt(rs.getTimestamp("created_at"));
                d.setProjetTitre(rs.getString("titre"));
                list.add(d);
            }
        }
        return list;
    }
}
