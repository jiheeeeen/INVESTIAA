package Services;

import Entities.Evenement;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {

    private final Connection conn;

    public EvenementService() {
        conn = MyBD.getInstance().getConn();
    }

    // ══════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════
    public void ajouter(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenement " +
                "(projectId, titre, description, mode, dateDebut, dateFin, lieu, meetingLink, organisateurId) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1,       e.getProjectId());
        ps.setString(2,    e.getTitre());
        ps.setString(3,    e.getDescription());
        ps.setString(4,    e.getMode() == null ? null : e.getMode().toString());
        ps.setTimestamp(5, Timestamp.valueOf(e.getDateDebut()));
        ps.setTimestamp(6, Timestamp.valueOf(e.getDateFin()));
        ps.setString(7,    e.getLieu());
        ps.setString(8,    e.getMeetingLink());
        ps.setInt(9,       e.getOrganisateurId());
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════
    //  READ ALL
    // ══════════════════════════════════════════════
    public List<Evenement> getAll() throws SQLException {
        List<Evenement> list = new ArrayList<>();
        String sql = "SELECT * FROM evenement";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ══════════════════════════════════════════════
    //  READ BY ID
    // ══════════════════════════════════════════════
    public Evenement getById(int id) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    // ══════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════
    public void modifier(Evenement e) throws SQLException {
        String sql = "UPDATE evenement SET " +
                "projectId=?, titre=?, description=?, mode=?, " +
                "dateDebut=?, dateFin=?, lieu=?, meetingLink=?, organisateurId=? " +
                "WHERE id=?";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1,       e.getProjectId());
        ps.setString(2,    e.getTitre());
        ps.setString(3,    e.getDescription());
        ps.setString(4,    e.getMode() == null ? null : e.getMode().toString());
        ps.setTimestamp(5, Timestamp.valueOf(e.getDateDebut()));
        ps.setTimestamp(6, Timestamp.valueOf(e.getDateFin()));
        ps.setString(7,    e.getLieu());
        ps.setString(8,    e.getMeetingLink());
        ps.setInt(9,       e.getOrganisateurId());
        ps.setInt(10,      e.getId());
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════
    //  DELETE — supprime d'abord les invitations liées
    // ══════════════════════════════════════════════
    public void supprimer(int id) throws SQLException {
        // ✅ 1. Supprimer toutes les invitations de cet événement
        PreparedStatement psInv = conn.prepareStatement("DELETE FROM invitation WHERE evenementId=?");
        psInv.setInt(1, id);
        psInv.executeUpdate();

        // ✅ 2. Supprimer l'événement
        PreparedStatement psEv = conn.prepareStatement("DELETE FROM evenement WHERE id=?");
        psEv.setInt(1, id);
        psEv.executeUpdate();
    }

    // ══════════════════════════════════════════════
    //  MAP ROW — noms exacts de la table
    // ══════════════════════════════════════════════
    private Evenement mapRow(ResultSet rs) throws SQLException {
        Timestamp tsDebut = rs.getTimestamp("dateDebut");
        Timestamp tsFin   = rs.getTimestamp("dateFin");
        return new Evenement(
                rs.getInt("id"),
                rs.getInt("projectId"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("mode"),
                tsDebut != null ? tsDebut.toLocalDateTime() : null,
                tsFin   != null ? tsFin.toLocalDateTime()   : null,
                rs.getString("lieu"),
                rs.getString("meetingLink"),
                rs.getInt("organisateurId")
        );
    }
}
