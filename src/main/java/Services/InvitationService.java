package Services;

import Entities.Invitation;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvitationService {

    private final Connection conn;

    public InvitationService() {
        conn = MyBD.getInstance().getConn();
    }

    // ══════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════
    public void ajouter(Invitation inv) throws SQLException {
        String sql = "INSERT INTO invitation (evenementId, dateInvitation, roleInvite, email) VALUES (?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1,       inv.getEvenementId());
        ps.setTimestamp(2, Timestamp.valueOf(inv.getDateInvitation()));
        ps.setString(3,    inv.getRoleInvite());
        ps.setString(4,    inv.getEmail());
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════
    //  READ ALL
    // ══════════════════════════════════════════════
    public List<Invitation> getAll() throws SQLException {
        List<Invitation> list = new ArrayList<>();
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM invitation");
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ══════════════════════════════════════════════
    //  READ BY EVENEMENT
    // ══════════════════════════════════════════════
    public List<Invitation> getByEvenement(int evenementId) throws SQLException {
        List<Invitation> list = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM invitation WHERE evenementId=?");
        ps.setInt(1, evenementId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ══════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════
    public void modifier(Invitation inv) throws SQLException {
        String sql = "UPDATE invitation SET evenementId=?, dateInvitation=?, roleInvite=?, email=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1,       inv.getEvenementId());
        ps.setTimestamp(2, Timestamp.valueOf(inv.getDateInvitation()));
        ps.setString(3,    inv.getRoleInvite());
        ps.setString(4,    inv.getEmail());
        ps.setInt(5,       inv.getId());
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════
    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM invitation WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════
    //  MAP ROW
    // ══════════════════════════════════════════════
    private Invitation mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("dateInvitation");
        return new Invitation(
                rs.getInt("id"),
                rs.getInt("evenementId"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("roleInvite"),
                rs.getString("email")
        );
    }
}