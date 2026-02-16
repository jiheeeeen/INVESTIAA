package Services;

import Entities.Evenement;
import Entities.ModeEvenement;
import Entities.Statut;
import Utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvenementCRUD {

    private final Connection conn;

    public EvenementCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    // ---------- Helpers ----------
    private Timestamp toTs(LocalDateTime dt) {
        return (dt == null) ? null : Timestamp.valueOf(dt);
    }

    private LocalDateTime toLdt(Timestamp ts) {
        return (ts == null) ? null : ts.toLocalDateTime();
    }

    private Evenement mapEvent(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setId(rs.getInt("id"));

        // ✅ IMPORTANT: colonne = projectId (pas projetId)
        e.setProjectId(rs.getInt("projectId"));

        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));

        String mode = rs.getString("mode");
        if (mode != null) e.setMode(ModeEvenement.valueOf(mode));

        e.setDateDebut(toLdt(rs.getTimestamp("dateDebut")));
        e.setDateFin(toLdt(rs.getTimestamp("dateFin")));

        e.setLieu(rs.getString("lieu"));
        e.setMeetingLink(rs.getString("meetingLink"));

        e.setOrganisateurId(rs.getInt("organisateurId"));

        String statut = rs.getString("statut");
        if (statut != null) e.setStatut(Statut.valueOf(statut));

        return e;
    }

    // --------- CREATE ----------
    public int addEvent(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenement(projectId, titre, description, mode, dateDebut, dateFin, lieu, meetingLink, organisateurId, statut) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, e.getProjectId());
            pst.setString(2, e.getTitre());
            pst.setString(3, e.getDescription());

            pst.setString(4, e.getMode() == null ? null : e.getMode().name());
            pst.setTimestamp(5, toTs(e.getDateDebut()));
            pst.setTimestamp(6, toTs(e.getDateFin()));

            pst.setString(7, e.getLieu());
            pst.setString(8, e.getMeetingLink());

            pst.setInt(9, e.getOrganisateurId());
            pst.setString(10, e.getStatut() == null ? Statut.EN_ATTENTE.name() : e.getStatut().name());

            pst.executeUpdate();

            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                return 0;
            }
        }
    }

    // --------- READ ----------
    public Evenement findById(int id) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return null;
                return mapEvent(rs);
            }
        }
    }

    public List<Evenement> getAll() throws SQLException {
        String sql = "SELECT * FROM evenement ORDER BY dateDebut DESC";
        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) list.add(mapEvent(rs));
        }
        return list;
    }

    public List<Evenement> getPendingEvents() throws SQLException {
        String sql = "SELECT * FROM evenement WHERE statut=? ORDER BY dateDebut DESC";
        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, Statut.EN_ATTENTE.name());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(mapEvent(rs));
            }
        }
        return list;
    }

    // --------- UPDATE ----------
    public void updateEvent(Evenement e) throws SQLException {
        String sql = "UPDATE evenement SET projectId=?, titre=?, description=?, mode=?, dateDebut=?, dateFin=?, lieu=?, meetingLink=?, organisateurId=?, statut=? " +
                "WHERE id=?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, e.getProjectId());
            pst.setString(2, e.getTitre());
            pst.setString(3, e.getDescription());

            pst.setString(4, e.getMode() == null ? null : e.getMode().name());
            pst.setTimestamp(5, toTs(e.getDateDebut()));
            pst.setTimestamp(6, toTs(e.getDateFin()));

            pst.setString(7, e.getLieu());
            pst.setString(8, e.getMeetingLink());

            pst.setInt(9, e.getOrganisateurId());
            pst.setString(10, e.getStatut() == null ? Statut.EN_ATTENTE.name() : e.getStatut().name());

            pst.setInt(11, e.getId());
            pst.executeUpdate();
        }
    }

    public void updateStatut(int id, Statut statut) throws SQLException {
        String sql = "UPDATE evenement SET statut=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, statut.name());
            pst.setInt(2, id);
            pst.executeUpdate();
        }
    }

    // --------- DELETE ----------
    public void deleteEvent(int id) throws SQLException {
        String sql = "DELETE FROM evenement WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    // --------- ADMIN actions ----------
    public void acceptEvent(int id) throws SQLException {
        updateStatut(id, Statut.VALIDE);
    }

    public void rejectEvent(int id) throws SQLException {
        updateStatut(id, Statut.REFUSE);
    }
}
