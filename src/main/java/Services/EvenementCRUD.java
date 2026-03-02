package Services;

import Entities.Evenement;
import Utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvenementCRUD {

    private final Connection conn;

    // Statuts (en DB)
    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_VALIDE     = "VALIDE";
    public static final String STATUT_REFUSE     = "REFUSE";

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
        e.setProjectId(rs.getInt("projectId"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));

        // ✅ mode est String dans l'entité
        e.setMode(rs.getString("mode"));

        e.setDateDebut(toLdt(rs.getTimestamp("dateDebut")));
        e.setDateFin(toLdt(rs.getTimestamp("dateFin")));

        e.setLieu(rs.getString("lieu"));
        e.setMeetingLink(rs.getString("meetingLink"));

        e.setOrganisateurId(rs.getInt("organisateurId"));

        // ✅ On ne fait rien avec "statut" ici car l'entité Evenement ne le contient pas
        // (mais la colonne peut exister et être utilisée dans les filtres/update)

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

            // ✅ mode String direct (ex: "EN_LIGNE" / "PRESENTIEL")
            pst.setString(4, e.getMode());

            pst.setTimestamp(5, toTs(e.getDateDebut()));
            pst.setTimestamp(6, toTs(e.getDateFin()));

            pst.setString(7, e.getLieu());
            pst.setString(8, e.getMeetingLink());

            pst.setInt(9, e.getOrganisateurId());

            // ✅ statut par défaut en DB
            pst.setString(10, STATUT_EN_ATTENTE);

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
    public String getStatutById(int id) throws SQLException {
        String sql = "SELECT statut FROM evenement WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getString("statut");
                return "";
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
            pst.setString(1, STATUT_EN_ATTENTE);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(mapEvent(rs));
            }
        }
        return list;
    }

    // --------- UPDATE statut (DB) ----------
    public void updateStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE evenement SET statut=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, statut);
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
        updateStatut(id, STATUT_VALIDE);
    }

    public void rejectEvent(int id) throws SQLException {
        updateStatut(id, STATUT_REFUSE);
    }
}