package Services;

import Entities.ProfilInvestisseur;
import Utils.MyBD;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfilInvestisseurCRUD {

    private final Connection cnx = MyBD.getInstance().getConn();
    private String cachedUserIdCol;

    public ProfilInvestisseur getByUserId(int idUser) throws SQLException {
        String userIdCol = resolveUserIdColumn();
        String sql = "SELECT * FROM investisseur WHERE " + userIdCol + " = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    // ✅ AJOUT : récupérer id_investisseur à partir de id_user (utile pour investissement)
    public int getIdInvestisseurByUserId(int idUser) throws SQLException {
        ProfilInvestisseur p = getByUserId(idUser);
        if (p == null) throw new SQLException("Profil investisseur introuvable pour id_user=" + idUser);
        return p.getIdInvestisseur();
    }

    // ===== upsert (insert si absent, update si existe) =====
    public void upsertForUser(ProfilInvestisseur p) throws SQLException {
        ProfilInvestisseur exist = getByUserId(p.getIdUser());
        if (exist == null) {
            ajouter(p);
        } else {
            modifier(p);
        }
    }

    public void deleteByUserId(int idUser) throws SQLException {
        String userIdCol = resolveUserIdColumn();
        String sql = "DELETE FROM investisseur WHERE " + userIdCol + " = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.executeUpdate();
        }
    }

    public void ajouter(ProfilInvestisseur p) throws SQLException {
        String userIdCol = resolveUserIdColumn();
        String sql = "INSERT INTO investisseur (" +
                userIdCol + ", budget_total, budget_mensuel, ticket_moyen_par_projet, horizon_investissement, bio, accepte_conditions," +
                "cin_recto_url, cin_verso_url, photo_url, secteurs" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdUser());
            ps.setBigDecimal(2, p.getBudgetTotal());
            setBigDecimalOrNull(ps, 3, p.getBudgetMensuel());
            ps.setBigDecimal(4, p.getTicketMoyenParProjet());
            ps.setString(5, p.getHorizonInvestissement());
            ps.setString(6, p.getBio());
            ps.setBoolean(7, p.isAccepteConditions());
            ps.setString(8, p.getCinRectoUrl());
            ps.setString(9, p.getCinVersoUrl());
            ps.setString(10, p.getPhotoUrl());
            ps.setString(11, joinSet(p.getSecteurs()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setIdInvestisseur(keys.getInt(1));
            }
        }
    }

    public void modifier(ProfilInvestisseur p) throws SQLException {
        String userIdCol = resolveUserIdColumn();
        String sql = "UPDATE investisseur SET " +
                "budget_total=?, budget_mensuel=?, ticket_moyen_par_projet=?, horizon_investissement=?, bio=?, accepte_conditions=?," +
                "cin_recto_url=?, cin_verso_url=?, photo_url=?, secteurs=? " +
                "WHERE " + userIdCol + "=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBigDecimal(1, p.getBudgetTotal());
            setBigDecimalOrNull(ps, 2, p.getBudgetMensuel());
            ps.setBigDecimal(3, p.getTicketMoyenParProjet());
            ps.setString(4, p.getHorizonInvestissement());
            ps.setString(5, p.getBio());
            ps.setBoolean(6, p.isAccepteConditions());
            ps.setString(7, p.getCinRectoUrl());
            ps.setString(8, p.getCinVersoUrl());
            ps.setString(9, p.getPhotoUrl());
            ps.setString(10, joinSet(p.getSecteurs()));
            ps.setInt(11, p.getIdUser());
            ps.executeUpdate();
        }
    }

    private ProfilInvestisseur map(ResultSet rs) throws SQLException {
        ProfilInvestisseur p = new ProfilInvestisseur();
        p.setIdInvestisseur(rs.getInt("id_investisseur"));

        // ⚠️ IMPORTANT: ta table doit contenir id_user
        String userIdCol = resolveUserIdColumn();
        p.setIdUser(rs.getInt(userIdCol));

        p.setBudgetTotal(rs.getBigDecimal("budget_total"));
        p.setBudgetMensuel(rs.getBigDecimal("budget_mensuel"));
        p.setTicketMoyenParProjet(rs.getBigDecimal("ticket_moyen_par_projet"));
        p.setHorizonInvestissement(rs.getString("horizon_investissement"));
        p.setBio(rs.getString("bio"));
        p.setAccepteConditions(rs.getBoolean("accepte_conditions"));
        p.setCinRectoUrl(rs.getString("cin_recto_url"));
        p.setCinVersoUrl(rs.getString("cin_verso_url"));
        p.setPhotoUrl(rs.getString("photo_url"));
        p.setSecteurs(splitSet(rs.getString("secteurs")));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }

    private static void setBigDecimalOrNull(PreparedStatement ps, int idx, BigDecimal v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.DECIMAL);
        else ps.setBigDecimal(idx, v);
    }

    private static String joinSet(Set<String> set) {
        if (set == null || set.isEmpty()) return null;
        return String.join(",", set);
    }

    private static Set<String> splitSet(String csv) {
        if (csv == null || csv.isBlank()) return null;
        Set<String> s = new HashSet<>();
        for (String part : csv.split(",")) {
            String v = part.trim();
            if (!v.isEmpty()) s.add(v);
        }
        return s.isEmpty() ? null : s;
    }

    private String resolveUserIdColumn() {
        if (cachedUserIdCol != null) return cachedUserIdCol;
        String fallback = "id_user";
        if (cnx == null) {
            cachedUserIdCol = fallback;
            return cachedUserIdCol;
        }
        try {
            List<String> cols = loadColumns("investisseur");
            cachedUserIdCol = firstExisting(cols, "id_user", "user_id", "id_utilisateur", "utilisateur_id", "idUser");
            if (cachedUserIdCol == null || cachedUserIdCol.isBlank()) cachedUserIdCol = fallback;
            return cachedUserIdCol;
        } catch (Exception e) {
            cachedUserIdCol = fallback;
            return cachedUserIdCol;
        }
    }

    private List<String> loadColumns(String table) throws SQLException {
        List<String> cols = new ArrayList<>();
        try {
            DatabaseMetaData meta = cnx.getMetaData();
            try (ResultSet rs = meta.getColumns(cnx.getCatalog(), null, table, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null) cols.add(name);
                }
            }
        } catch (Exception ignore) {
        }
        if (!cols.isEmpty()) return cols;

        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    if (name != null) cols.add(name);
                }
            }
        }
        if (!cols.isEmpty()) return cols;

        // Fallback: read column names from a simple SELECT
        String fallbackSql = "SELECT * FROM " + table + " LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(fallbackSql);
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
}
