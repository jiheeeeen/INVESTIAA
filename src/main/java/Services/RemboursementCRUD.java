package Services;

import Entities.Remboursement;
import Entities.RemboursementStatut;
import Utils.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RemboursementCRUD implements InterfaceCRUD<Remboursement> {

    private Connection conn;

    public RemboursementCRUD() {
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
    public void ajouter(Remboursement r) throws SQLException {
        requireConn();
        String sql = "INSERT INTO remboursement (financement_id, date_echeance, montant_du, montant_paye, statut) VALUES (?,?,?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, r.getFinancementId());
            if (r.getDateEcheance() == null) pst.setNull(2, java.sql.Types.DATE);
            else pst.setDate(2, r.getDateEcheance());
            pst.setDouble(3, r.getMontantDu());
            pst.setDouble(4, r.getMontantPaye());
            pst.setString(5, normalizeDbStatut(r.getStatut()));
            int rows = pst.executeUpdate();
            if (rows <= 0) throw new SQLException("Insert failed: no rows affected.");
        }
    }

    @Override
    public void modifier(Remboursement r) throws SQLException {
        requireConn();
        String sql = "UPDATE remboursement SET financement_id=?, date_echeance=?, montant_du=?, montant_paye=?, statut=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, r.getFinancementId());
            if (r.getDateEcheance() == null) pst.setNull(2, java.sql.Types.DATE);
            else pst.setDate(2, r.getDateEcheance());
            pst.setDouble(3, r.getMontantDu());
            pst.setDouble(4, r.getMontantPaye());
            pst.setString(5, normalizeDbStatut(r.getStatut()));
            pst.setInt(6, r.getId());
            int rows = pst.executeUpdate();
            if (rows <= 0) throw new SQLException("Update failed: no rows affected (id=" + r.getId() + ").");
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        requireConn();
        String sql = "DELETE FROM remboursement WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            int rows = pst.executeUpdate();
            if (rows <= 0) throw new SQLException("Delete failed: no rows affected (id=" + id + ").");
        }
    }

    @Override
    public List<Remboursement> afficher() throws SQLException {
        requireConn();
        String sql = "SELECT id, financement_id, date_echeance, montant_du, montant_paye, statut FROM remboursement ORDER BY id DESC";
        List<Remboursement> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Remboursement> afficherParFinancement(int financementId) throws SQLException {
        requireConn();
        String sql = "SELECT id, financement_id, date_echeance, montant_du, montant_paye, statut FROM remboursement WHERE financement_id=? ORDER BY date_echeance DESC, id DESC";
        List<Remboursement> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, financementId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Remboursement getById(int id) throws SQLException {
        requireConn();
        String sql = "SELECT id, financement_id, date_echeance, montant_du, montant_paye, statut FROM remboursement WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void ajouterPaiement(int remboursementId, double montantAjoute) throws SQLException {
        requireConn();
        if (montantAjoute <= 0 || Double.isNaN(montantAjoute)) {
            throw new SQLException("Montant de paiement invalide.");
        }

        String select = "SELECT montant_du, montant_paye FROM remboursement WHERE id=?";
        double du;
        double paye;
        try (PreparedStatement pst = conn.prepareStatement(select)) {
            pst.setInt(1, remboursementId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) throw new SQLException("Remboursement introuvable (id=" + remboursementId + ").");
                du = rs.getDouble("montant_du");
                paye = rs.getDouble("montant_paye");
            }
        }

        double nouveauPaye = paye + montantAjoute;
        String statut = (du > 0 && nouveauPaye + 1e-9 >= du)
                ? RemboursementStatut.PAYE.name()
                : RemboursementStatut.EN_ATTENTE.name();

        String update = "UPDATE remboursement SET montant_paye=?, statut=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(update)) {
            pst.setDouble(1, nouveauPaye);
            pst.setString(2, statut);
            pst.setInt(3, remboursementId);
            int rows = pst.executeUpdate();
            if (rows <= 0) throw new SQLException("Update failed: no rows affected (id=" + remboursementId + ").");
        }
    }

    public void ajouterOuModifierParFinancementEtDate(Remboursement r) throws SQLException {
        requireConn();
        if (r == null) throw new SQLException("Remboursement null.");
        if (r.getFinancementId() <= 0) throw new SQLException("financement_id invalide.");
        if (r.getDateEcheance() == null) throw new SQLException("date_echeance obligatoire.");

        String sql = "SELECT id FROM remboursement WHERE financement_id=? AND date_echeance=? ORDER BY id DESC LIMIT 1";
        Integer existingId = null;
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, r.getFinancementId());
            pst.setDate(2, r.getDateEcheance());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) existingId = rs.getInt("id");
            }
        }

        if (existingId == null) {
            ajouter(r);
            return;
        }

        r.setId(existingId);
        modifier(r);
    }

    private static Remboursement mapRow(ResultSet rs) throws SQLException {
        Remboursement r = new Remboursement();
        r.setId(rs.getInt("id"));
        r.setFinancementId(rs.getInt("financement_id"));
        r.setDateEcheance(rs.getDate("date_echeance"));
        r.setMontantDu(rs.getDouble("montant_du"));
        r.setMontantPaye(rs.getDouble("montant_paye"));
        r.setStatut(normalizeDbStatut(rs.getString("statut")));
        return r;
    }

    private static String normalizeDbStatut(String raw) {
        return RemboursementStatut.from(raw).name();
    }

    // =========================================================
    // ✅ AJOUTS SANS CHANGER TA BASE / STRUCTURE
    // =========================================================

    // DTO simple pour crédit wallet
    public static class PaidCreditRow {
        private final int remboursementId;
        private final double montantPaye;
        private final java.sql.Date dateEcheance;

        public PaidCreditRow(int remboursementId, double montantPaye, java.sql.Date dateEcheance) {
            this.remboursementId = remboursementId;
            this.montantPaye = montantPaye;
            this.dateEcheance = dateEcheance;
        }

        public int getRemboursementId() { return remboursementId; }
        public double getMontantPaye() { return montantPaye; }
        public java.sql.Date getDateEcheance() { return dateEcheance; }
    }

    // ✅ récupérer les remboursements PAYE pour un investisseur
    // jointure: remboursement -> financement2 -> investissement (id_investisseur)
    public List<PaidCreditRow> getPaidCreditablesByInvestisseur(int idInvestisseur) throws SQLException {
        requireConn();
        String sql =
                "SELECT r.id AS remboursement_id, r.montant_paye, r.date_echeance " +
                        "FROM remboursement r " +
                        "JOIN financement2 f ON f.id_financement = r.financement_id " +
                        "JOIN investissement i ON i.id_investissement = f.id_investissement " +
                        "WHERE i.id_investisseur = ? " +
                        "  AND r.statut = 'PAYE' " +
                        "  AND r.montant_paye IS NOT NULL " +
                        "  AND r.montant_paye > 0";

        List<PaidCreditRow> rows = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idInvestisseur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("remboursement_id");
                    double mp = rs.getDouble("montant_paye");
                    java.sql.Date d = rs.getDate("date_echeance");
                    rows.add(new PaidCreditRow(id, mp, d));
                }
            }
        }
        return rows;
    }

    // ✅ pour éviter double crédit SANS changer la base:
    // après crédit on met montant_paye = 0 pour ces remboursements
    public int consumeMontantPayeByIds(List<Integer> ids) throws SQLException {
        requireConn();
        if (ids == null || ids.isEmpty()) return 0;

        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) in.append(",");
            in.append("?");
        }

        String sql = "UPDATE remboursement SET montant_paye = 0 WHERE id IN (" + in + ") AND statut='PAYE'";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                pst.setInt(i + 1, ids.get(i));
            }
            return pst.executeUpdate();
        }
    }
}