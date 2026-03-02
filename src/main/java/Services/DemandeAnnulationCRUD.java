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

    // Constantes de statuts (évite les fautes)
    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_ACCEPTEE   = "ACCEPTEE";
    public static final String STATUT_REFUSEE    = "REFUSEE";

    public DemandeAnnulationCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    // =========================
    // TES FONCTIONS (INCHANGEES)
    // =========================
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

    // =========================
    // AJOUTS (ce qu’il faut)
    // =========================

    /**
     * (Optionnel mais utile) Vérifie si une demande EN_ATTENTE existe déjà pour ce projet.
     */
    public boolean existeDemandeEnAttentePourProjet(int projetId) throws SQLException {
        String sql = "SELECT 1 FROM demande_annulation WHERE projet_id=? AND statut=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projetId);
            ps.setString(2, STATUT_EN_ATTENTE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Demandes EN_ATTENTE (à afficher dans le dashboard admin)
     */
    public List<DemandeAnnulation> afficherEnAttente() throws SQLException {
        String sql = "SELECT d.id, d.projet_id, d.raison, d.statut, d.created_at, p.titre " +
                "FROM demande_annulation d " +
                "JOIN projet p ON p.id_projet = d.projet_id " +
                "WHERE d.statut=? " +
                "ORDER BY d.id DESC";

        List<DemandeAnnulation> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, STATUT_EN_ATTENTE);
            try (ResultSet rs = ps.executeQuery()) {
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
        }
        return list;
    }

    /**
     * Admin REFUSE : la demande devient REFUSEE, le projet reste valide.
     * @return true si mise à jour faite (si elle était EN_ATTENTE), sinon false.
     */
    public boolean refuserDemande(int demandeId) throws SQLException {
        String sql = "UPDATE demande_annulation SET statut=? WHERE id=? AND statut=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, STATUT_REFUSEE);
            ps.setInt(2, demandeId);
            ps.setString(3, STATUT_EN_ATTENTE);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Admin ACCEPTE : la demande devient ACCEPTEE + suppression du projet.
     * Transaction (commit/rollback) pour garantir la cohérence.
     * @return true si tout est OK, sinon false.
     */
    public boolean accepterDemandeEtSupprimerProjet(int demandeId) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            int projetId = getProjetIdDemandeEnAttente(demandeId);
            if (projetId <= 0) {
                conn.rollback();
                return false;
            }

            // 1) Marquer la demande acceptée (uniquement si EN_ATTENTE)
            if (!updateStatut(demandeId, STATUT_ACCEPTEE)) {
                conn.rollback();
                return false;
            }

            // 2) Supprimer le projet
            // IMPORTANT: si tu as des tables enfants, il faut ON DELETE CASCADE
            // ou supprimer les enfants avant. Ici on supprime juste la ligne projet.
            if (!supprimerProjet(projetId)) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(oldAutoCommit); } catch (SQLException ignore) {}
        }
    }

    // =========================
    // Helpers privés
    // =========================
    private int getProjetIdDemandeEnAttente(int demandeId) throws SQLException {
        String sql = "SELECT projet_id FROM demande_annulation WHERE id=? AND statut=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            ps.setString(2, STATUT_EN_ATTENTE);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("projet_id");
            }
        }
        return -1;
    }

    private boolean updateStatut(int demandeId, String newStatut) throws SQLException {
        String sql = "UPDATE demande_annulation SET statut=? WHERE id=? AND statut=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatut);
            ps.setInt(2, demandeId);
            ps.setString(3, STATUT_EN_ATTENTE);
            return ps.executeUpdate() == 1;
        }
    }

    private boolean supprimerProjet(int projetId) throws SQLException {
        String sql = "DELETE FROM projet WHERE id_projet=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projetId);
            return ps.executeUpdate() == 1;
        }
    }
}