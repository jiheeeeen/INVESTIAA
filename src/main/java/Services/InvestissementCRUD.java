package Services;

import Entities.Investissement;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestissementCRUD {

    private final Connection conn;
    private String cachedIdCol;
    private String cachedInvestisseurCol;
    private String cachedProjetCol;
    private String cachedMontantCol;
    private String cachedDateCol;

    public InvestissementCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    // ==========================
    // CREATE
    // ==========================
    public void ajouter(Investissement inv) throws SQLException {
        String montantCol = resolveMontantColumn();
        String dateCol = resolveDateColumn();
        String investCol = resolveInvestisseurColumn();
        String projetCol = resolveProjetColumn();
        String sql;
        if (projetCol == null) {
            sql = "INSERT INTO investissement (" + montantCol + ", " + dateCol + ", " + investCol + ") VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDouble(1, inv.getMontant());
                ps.setDate(2, inv.getDate_investissement());
                ps.setInt(3, inv.getId_investisseur());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) inv.setId_investissement(rs.getInt(1));
                }
            }
            return;
        }

        sql = "INSERT INTO investissement (" + montantCol + ", " + dateCol + ", " + investCol + ", " + projetCol + ") VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, inv.getMontant());
            ps.setDate(2, inv.getDate_investissement());
            ps.setInt(3, inv.getId_investisseur());
            ps.setInt(4, inv.getId_projet());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) inv.setId_investissement(rs.getInt(1));
            }
        }
    }

    // ==========================
    // UPDATE
    // ==========================
    public void modifier(Investissement inv) throws SQLException {
        String montantCol = resolveMontantColumn();
        String dateCol = resolveDateColumn();
        String investCol = resolveInvestisseurColumn();
        String projetCol = resolveProjetColumn();
        String idCol = resolveIdColumn();
        String sql;
        if (projetCol == null) {
            sql = "UPDATE investissement SET " + montantCol + "=?, " + dateCol + "=?, " + investCol + "=? WHERE " + idCol + "=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, inv.getMontant());
                ps.setDate(2, inv.getDate_investissement());
                ps.setInt(3, inv.getId_investisseur());
                ps.setInt(4, inv.getId_investissement());
                ps.executeUpdate();
            }
            return;
        }

        sql = "UPDATE investissement SET " + montantCol + "=?, " + dateCol + "=?, " + investCol + "=?, " + projetCol + "=? WHERE " + idCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, inv.getMontant());
            ps.setDate(2, inv.getDate_investissement());
            ps.setInt(3, inv.getId_investisseur());
            ps.setInt(4, inv.getId_projet());
            ps.setInt(5, inv.getId_investissement());
            ps.executeUpdate();
        }
    }

    // ✅ update partiel (souvent utile)
    public void modifierMontantEtDate(int idInvestissement, double montant, Date dateInv) throws SQLException {
        String montantCol = resolveMontantColumn();
        String dateCol = resolveDateColumn();
        String idCol = resolveIdColumn();
        String sql = "UPDATE investissement SET " + montantCol + "=?, " + dateCol + "=? WHERE " + idCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, montant);
            ps.setDate(2, dateInv);
            ps.setInt(3, idInvestissement);
            ps.executeUpdate();
        }
    }

    // ==========================
    // DELETE
    // ==========================
    public void supprimer(int id) throws SQLException {
        String idCol = resolveIdColumn();
        String sql = "DELETE FROM investissement WHERE " + idCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ✅ delete sécurisé (par investisseur)
    public void supprimerParInvestisseur(int idInvestissement, int idInvestisseur) throws SQLException {
        String idCol = resolveIdColumn();
        String investCol = resolveInvestisseurColumn();
        String sql = "DELETE FROM investissement WHERE " + idCol + "=? AND " + investCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idInvestissement);
            ps.setInt(2, idInvestisseur);
            ps.executeUpdate();
        }
    }

    // ==========================
    // READ
    // ==========================
    public Investissement getById(int id) throws SQLException {
        String idCol = resolveIdColumn();
        String sql = "SELECT * FROM investissement WHERE " + idCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public boolean existe(int idInvestissement) throws SQLException {
        String idCol = resolveIdColumn();
        String sql = "SELECT 1 FROM investissement WHERE " + idCol + "=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idInvestissement);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Investissement> afficherAll() throws SQLException {
        String dateCol = resolveDateColumn();
        String idCol = resolveIdColumn();
        String sql = "SELECT * FROM investissement ORDER BY " + dateCol + " DESC, " + idCol + " DESC";
        List<Investissement> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Investissement> afficherParProjet(int idProjet) throws SQLException {
        String projetCol = resolveProjetColumn();
        String dateCol = resolveDateColumn();
        String idCol = resolveIdColumn();
        if (projetCol == null) {
            return afficherAll();
        }
        String sql = "SELECT * FROM investissement WHERE " + projetCol + "=? ORDER BY " + dateCol + " DESC, " + idCol + " DESC";
        List<Investissement> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Investissement> afficherParInvestisseur(int idInvestisseur) throws SQLException {
        String investCol = resolveInvestisseurColumn();
        String dateCol = resolveDateColumn();
        String idCol = resolveIdColumn();
        String sql = "SELECT * FROM investissement WHERE " + investCol + "=? ORDER BY " + dateCol + " DESC, " + idCol + " DESC";
        List<Investissement> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idInvestisseur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ✅ très utile : historique d’un investisseur sur un projet
    public List<Investissement> afficherParProjetEtInvestisseur(int idProjet, int idInvestisseur) throws SQLException {
        String projetCol = resolveProjetColumn();
        String investCol = resolveInvestisseurColumn();
        String dateCol = resolveDateColumn();
        String idCol = resolveIdColumn();
        if (projetCol == null) {
            return afficherParInvestisseur(idInvestisseur);
        }
        String sql = "SELECT * FROM investissement WHERE " + projetCol + "=? AND " + investCol + "=? ORDER BY " + dateCol + " DESC, " + idCol + " DESC";
        List<Investissement> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            ps.setInt(2, idInvestisseur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ==========================
    // STATS (optionnel)
    // ==========================
    public int countParProjet(int idProjet) throws SQLException {
        String projetCol = resolveProjetColumn();
        if (projetCol == null) return 0;
        String sql = "SELECT COUNT(*) FROM investissement WHERE " + projetCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        }
    }

    public double sumMontantParProjet(int idProjet) throws SQLException {
        String montantCol = resolveMontantColumn();
        String projetCol = resolveProjetColumn();
        if (projetCol == null) return 0.0;
        String sql = "SELECT COALESCE(SUM(" + montantCol + "),0) FROM investissement WHERE " + projetCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
                return 0.0;
            }
        }
    }

    public double sumMontantParProjetEtInvestisseur(int idProjet, int idInvestisseur) throws SQLException {
        String montantCol = resolveMontantColumn();
        String projetCol = resolveProjetColumn();
        String investCol = resolveInvestisseurColumn();
        if (projetCol == null) return 0.0;
        String sql = "SELECT COALESCE(SUM(" + montantCol + "),0) FROM investissement WHERE " + projetCol + "=? AND " + investCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            ps.setInt(2, idInvestisseur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
                return 0.0;
            }
        }
    }

    // ==========================
    // MAP helper
    // ==========================
    private static Investissement map(ResultSet rs) throws SQLException {
        Investissement inv = new Investissement();
        inv.setId_investissement(rs.getInt(resolveIdColumnStatic(rs)));
        inv.setMontant(rs.getDouble(resolveMontantColumnStatic(rs)));
        inv.setDate_investissement(rs.getDate(resolveDateColumnStatic(rs)));
        inv.setId_investisseur(rs.getInt(resolveInvestisseurColumnStatic(rs)));
        String projCol = resolveProjetColumnStatic(rs);
        inv.setId_projet(projCol == null ? 0 : rs.getInt(projCol));
        return inv;
    }

    private String resolveIdColumn() throws SQLException {
        if (cachedIdCol != null) return cachedIdCol;
        cachedIdCol = resolveColumn("investissement", "id_investissement", "id", "investissement_id");
        return cachedIdCol;
    }

    private String resolveInvestisseurColumn() throws SQLException {
        if (cachedInvestisseurCol != null) return cachedInvestisseurCol;
        cachedInvestisseurCol = resolveColumn("investissement", "id_investisseur", "investisseur_id", "investor_id", "id_user", "user_id", "id_utilisateur", "utilisateur_id");
        return cachedInvestisseurCol;
    }

    private String resolveProjetColumn() throws SQLException {
        if (cachedProjetCol != null) return cachedProjetCol;
        cachedProjetCol = resolveColumn("investissement", "id_projet", "projet_id", "project_id", "idProjet");
        return cachedProjetCol;
    }

    private String resolveMontantColumn() throws SQLException {
        if (cachedMontantCol != null) return cachedMontantCol;
        cachedMontantCol = resolveColumn("investissement", "montant", "amount");
        return cachedMontantCol;
    }

    private String resolveDateColumn() throws SQLException {
        if (cachedDateCol != null) return cachedDateCol;
        cachedDateCol = resolveColumn("investissement", "date_investissement", "date", "created_at", "date_financement");
        return cachedDateCol;
    }

    private String resolveColumn(String table, String... candidates) throws SQLException {
        if (conn == null) return candidates[0];
        List<String> cols = loadColumns(table);
        String found = firstExisting(cols, candidates);
        return (found == null || found.isBlank()) ? null : found;
    }

    private List<String> loadColumns(String table) throws SQLException {
        List<String> cols = new ArrayList<>();
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null) cols.add(name);
                }
            }
        } catch (Exception ignore) {
        }
        if (!cols.isEmpty()) return cols;

        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    if (name != null) cols.add(name);
                }
            }
        }
        if (!cols.isEmpty()) return cols;

        // Fallback: read column names from a simple SELECT (works even without INFORMATION_SCHEMA privileges)
        String fallbackSql = "SELECT * FROM " + table + " LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(fallbackSql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String name = meta.getColumnName(i);
                if (name != null) cols.add(name);
            }
        } catch (Exception ignore) {
        }
        return cols;
    }

    private static String firstExisting(List<String> cols, String... candidates) {
        if (cols == null || cols.isEmpty()) return null;
        for (String c : candidates) {
            for (String col : cols) {
                if (col.equalsIgnoreCase(c)) return col;
            }
        }
        return null;
    }

    private static String resolveIdColumnStatic(ResultSet rs) throws SQLException {
        return resolveColumnFromResultSet(rs, "id_investissement", "id", "investissement_id");
    }

    private static String resolveInvestisseurColumnStatic(ResultSet rs) throws SQLException {
        return resolveColumnFromResultSet(rs, "id_investisseur", "investisseur_id", "investor_id", "id_user", "user_id", "id_utilisateur", "utilisateur_id");
    }

    public String getInvestisseurColumnName() throws SQLException {
        return resolveInvestisseurColumn();
    }

    public boolean investorColumnIsUserId() throws SQLException {
        String col = resolveInvestisseurColumn();
        if (col == null) return false;
        String c = col.trim().toLowerCase();
        return c.equals("id_user") || c.equals("user_id") || c.equals("id_utilisateur") || c.equals("utilisateur_id");
    }

    private static String resolveProjetColumnStatic(ResultSet rs) throws SQLException {
        return resolveColumnFromResultSet(rs, "id_projet", "projet_id", "project_id", "idProjet");
    }

    private static String resolveMontantColumnStatic(ResultSet rs) throws SQLException {
        return resolveColumnFromResultSet(rs, "montant", "amount");
    }

    private static String resolveDateColumnStatic(ResultSet rs) throws SQLException {
        return resolveColumnFromResultSet(rs, "date_investissement", "date", "created_at", "date_financement");
    }

    private static String resolveColumnFromResultSet(ResultSet rs, String... candidates) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        for (String candidate : candidates) {
            for (int i = 1; i <= count; i++) {
                if (meta.getColumnLabel(i).equalsIgnoreCase(candidate) || meta.getColumnName(i).equalsIgnoreCase(candidate)) {
                    return meta.getColumnLabel(i);
                }
            }
        }
        return null;
    }
}
