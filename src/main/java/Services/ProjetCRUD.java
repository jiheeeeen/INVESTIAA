package Services;

import Entities.Projet;
import Entities.Statut;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProjetCRUD implements InterfaceCRUD<Projet> {

    private final Connection conn;

    public ProjetCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    @Override
    public void ajouter(Projet p) throws SQLException {

        String sql = "INSERT INTO projet (" +
                "entrepreneur_id, statut, titre, secteur, description_courte, description_longue, " +
                "objectif_tnd, duree_campagne_jours, mode_remboursement, taux_interet_pct, duree_remboursement_mois, " +
                "marge_brute_estimee_tnd, resultat_net_estime_tnd" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, p.getEntrepreneurId());
            ps.setString(2, p.getStatut() != null ? p.getStatut() : "BROUILLON");

            ps.setString(3, p.getTitre());
            ps.setString(4, p.getSecteur());
            ps.setString(5, p.getDescriptionCourte());

            if (p.getDescriptionLongue() != null) ps.setString(6, p.getDescriptionLongue());
            else ps.setNull(6, Types.LONGVARCHAR);

            ps.setBigDecimal(7, p.getObjectifTnd());
            ps.setInt(8, p.getDureeCampagneJours());

            ps.setString(9, p.getModeRemboursement() != null ? p.getModeRemboursement() : "MENSUEL");

            if (p.getTauxInteretPct() != null) ps.setBigDecimal(10, p.getTauxInteretPct());
            else ps.setNull(10, Types.DECIMAL);

            if (p.getDureeRemboursementMois() != null) ps.setInt(11, p.getDureeRemboursementMois());
            else ps.setNull(11, Types.INTEGER);

            if (p.getMargeBruteEstimeeTnd() != null) ps.setBigDecimal(12, p.getMargeBruteEstimeeTnd());
            else ps.setNull(12, Types.DECIMAL);

            if (p.getResultatNetEstimeTnd() != null) ps.setBigDecimal(13, p.getResultatNetEstimeTnd());
            else ps.setNull(13, Types.DECIMAL);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setIdProjet(rs.getInt(1)); // id_projet
            }
        }
    }

    @Override
    public void modifier(Projet p) throws SQLException {

        String sql = "UPDATE projet SET " +
                "entrepreneur_id=?, statut=?, titre=?, secteur=?, description_courte=?, description_longue=?, " +
                "objectif_tnd=?, duree_campagne_jours=?, mode_remboursement=?, taux_interet_pct=?, duree_remboursement_mois=?, " +
                "marge_brute_estimee_tnd=?, resultat_net_estime_tnd=? " +
                "WHERE id_projet=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, p.getEntrepreneurId());
            ps.setString(2, p.getStatut());

            ps.setString(3, p.getTitre());
            ps.setString(4, p.getSecteur());
            ps.setString(5, p.getDescriptionCourte());

            if (p.getDescriptionLongue() != null) ps.setString(6, p.getDescriptionLongue());
            else ps.setNull(6, Types.LONGVARCHAR);

            ps.setBigDecimal(7, p.getObjectifTnd());
            ps.setInt(8, p.getDureeCampagneJours());

            ps.setString(9, p.getModeRemboursement());

            if (p.getTauxInteretPct() != null) ps.setBigDecimal(10, p.getTauxInteretPct());
            else ps.setNull(10, Types.DECIMAL);

            if (p.getDureeRemboursementMois() != null) ps.setInt(11, p.getDureeRemboursementMois());
            else ps.setNull(11, Types.INTEGER);

            if (p.getMargeBruteEstimeeTnd() != null) ps.setBigDecimal(12, p.getMargeBruteEstimeeTnd());
            else ps.setNull(12, Types.DECIMAL);

            if (p.getResultatNetEstimeTnd() != null) ps.setBigDecimal(13, p.getResultatNetEstimeTnd());
            else ps.setNull(13, Types.DECIMAL);

            ps.setInt(14, p.getIdProjet()); // id_projet

            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int idProjet) throws SQLException {
        String sql = "DELETE FROM projet WHERE id_projet=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            int rows = ps.executeUpdate();

        }
    }


    @Override
    public List<Projet> afficher() throws SQLException {
        String sql = "SELECT * FROM projet ORDER BY id_projet DESC";
        List<Projet> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Projet getById(int idProjet) throws SQLException {
        String sql = "SELECT * FROM projet WHERE id_projet=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private Projet mapRow(ResultSet rs) throws SQLException {
        Projet p = new Projet();

        p.setIdProjet(rs.getInt("id_projet"));
        p.setEntrepreneurId(rs.getInt("entrepreneur_id"));
        p.setStatut(rs.getString("statut"));

        p.setTitre(rs.getString("titre"));
        p.setSecteur(rs.getString("secteur"));
        p.setDescriptionCourte(rs.getString("description_courte"));
        p.setDescriptionLongue(rs.getString("description_longue"));

        p.setObjectifTnd(rs.getBigDecimal("objectif_tnd"));
        p.setDureeCampagneJours(rs.getInt("duree_campagne_jours"));

        p.setModeRemboursement(rs.getString("mode_remboursement"));
        p.setTauxInteretPct(rs.getBigDecimal("taux_interet_pct"));

        int dr = rs.getInt("duree_remboursement_mois");
        p.setDureeRemboursementMois(rs.wasNull() ? null : dr);

        p.setMargeBruteEstimeeTnd(rs.getBigDecimal("marge_brute_estimee_tnd"));
        p.setResultatNetEstimeTnd(rs.getBigDecimal("resultat_net_estime_tnd"));

        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));

        return p;
    }
    public void updateStatut(int idProjet, Statut statut) throws SQLException {
        String sql = "UPDATE projet SET statut=? WHERE id_projet=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, statut.name());
            pst.setInt(2, idProjet);
            pst.executeUpdate();
        }
    }

    // --------- DELETE ----------
    public void deleteProject(int idProjet) throws SQLException {
        String sql = "DELETE FROM projet WHERE id_projet=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idProjet);
            pst.executeUpdate();
        }
    }

    // --------- ADMIN actions ----------
    public void acceptProject(int idProjet) throws SQLException {
        updateStatut(idProjet, Statut.VALIDE);
    }

    public void rejectProject(int idProjet) throws SQLException {
        updateStatut(idProjet, Statut.REFUSE);
    }

    // ✅✅ FIX ICI
    public List<Projet> getPendingProjects() throws SQLException {
        String sql = "SELECT * FROM projet WHERE statut = ? ORDER BY created_at DESC";
        List<Projet> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, Statut.EN_ATTENTE.name());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs)); // ✅ FIX
            }
        }
        return list;
    }

}

