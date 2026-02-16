package Services;

import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Utils.MyBD;
import Utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserCRUD {

    private final Connection conn;

    public UserCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    // =========================================================
    // Mapper ResultSet -> User
    // =========================================================
    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setTelephone(rs.getString("telephone"));
        u.setCin(rs.getString("cin"));
        u.setDateNaissance(rs.getDate("date_naissance"));
        u.setNationalite(rs.getString("nationalite"));
        u.setAdresse(rs.getString("adresse"));
        u.setVille(rs.getString("ville"));

        u.setPassword(rs.getString("mot_de_passe"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("est_actif"));
        u.setStatutVerification(StatutVerification.valueOf(rs.getString("statut_verification")));

        // Ces colonnes doivent exister dans ta table users
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));

        return u;
    }

    // =========================================================
    // READ
    // =========================================================
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return null;
                return mapUser(rs);
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return null;
                return mapUser(rs);
            }
        }
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) list.add(mapUser(rs));
        }
        return list;
    }

    public List<User> getVerifiedUsers() throws SQLException {
        String sql = "SELECT * FROM users WHERE statut_verification = ? ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, StatutVerification.VERIFIE.name());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(mapUser(rs));
            }
        }
        return list;
    }

    public List<User> getPendingAccounts() throws SQLException {
        String sql = "SELECT * FROM users WHERE statut_verification = ? ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, StatutVerification.EN_ATTENTE.name());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(mapUser(rs));
            }
        }
        return list;
    }

    // =========================================================
    // CREATE (Admin)
    // =========================================================
    public int addUser(User u) throws SQLException {
        if (findByEmail(u.getEmail()) != null) {
            throw new SQLException("EMAIL_EXISTS");
        }

        String sql = "INSERT INTO users(nom, prenom, email, telephone, cin, date_naissance, nationalite, adresse, ville, " +
                "mot_de_passe, role, est_actif, statut_verification) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, u.getNom());
            pst.setString(2, u.getPrenom());
            pst.setString(3, u.getEmail());
            pst.setString(4, u.getTelephone());
            pst.setString(5, u.getCin());
            pst.setDate(6, u.getDateNaissance() == null ? null : new java.sql.Date(u.getDateNaissance().getTime()));
            pst.setString(7, u.getNationalite());
            pst.setString(8, u.getAdresse());
            pst.setString(9, u.getVille());
            pst.setString(10, u.getPassword()); // plus tard: hash
            pst.setString(11, u.getRole().name());
            pst.setBoolean(12, u.isActive());
            pst.setString(13, u.getStatutVerification().name());

            pst.executeUpdate();
            try (ResultSet keys = pst.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    // =========================================================
    // UPDATE (Admin)
    // =========================================================
    public void updateUser(User u) throws SQLException {
        String sql = "UPDATE users SET nom=?, prenom=?, telephone=?, cin=?, date_naissance=?, nationalite=?, adresse=?, ville=? " +
                "WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, u.getNom());
            pst.setString(2, u.getPrenom());
            pst.setString(3, u.getTelephone());
            pst.setString(4, u.getCin());
            pst.setDate(5, u.getDateNaissance() == null ? null : new java.sql.Date(u.getDateNaissance().getTime()));
            pst.setString(6, u.getNationalite());
            pst.setString(7, u.getAdresse());
            pst.setString(8, u.getVille());
            pst.setInt(9, u.getId());
            pst.executeUpdate();
        }
    }

    public void updateUserRole(int userId, Role role) throws SQLException {
        String sql = "UPDATE users SET role=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, role.name());
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    public void setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET est_actif=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setBoolean(1, active);
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    public void updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET mot_de_passe=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, newPassword); // plus tard: hash
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    /**
     * ✅ Méthode SIMPLE pour modifier statut_verification + est_actif (utilisée par le Dashboard)
     */
    public void setVerificationStatus(int userId, StatutVerification sv, boolean actif) throws SQLException {
        String sql = "UPDATE users SET statut_verification=?, est_actif=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, sv.name());
            pst.setBoolean(2, actif);
            pst.setInt(3, userId);
            pst.executeUpdate();
        }
    }

    /**
     * ✅ Validation admin: accepte (VERIFIE + actif=true)
     */
    public void acceptAccount(int userId) throws SQLException {
        setVerificationStatus(userId, StatutVerification.VERIFIE, true);
    }

    /**
     * ✅ Validation admin: refuse (REFUSE + actif=false)
     */
    public void rejectAccount(int userId) throws SQLException {
        setVerificationStatus(userId, StatutVerification.REFUSE, false);
    }

    /**
     * ✅ Admin: update champs visibles dans le dashboard (nom/prenom/tel/cin/role/statut)
     */
    public void updateUserAdminFields(int id, String nom, String prenom, String email,
                                      String telephone, String cin, String roleStr, String statutStr) throws SQLException {

        Role role = Role.valueOf(roleStr.trim().toUpperCase());
        StatutVerification sv = StatutVerification.valueOf(statutStr.trim().toUpperCase());


        // 2) email sécurisé
        if (email == null || email.trim().isEmpty()) {
            throw new SQLException("EMAIL_REQUIRED");
        }
        email = email.trim().toLowerCase();

        // email unique (sauf pour le même user)
        String check = "SELECT id FROM users WHERE email=? AND id<>?";
        try (PreparedStatement pst = conn.prepareStatement(check)) {
            pst.setString(1, email);
            pst.setInt(2, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) throw new SQLException("EMAIL_EXISTS");
            }
        }


        String sql = "UPDATE users SET nom=?, prenom=?, email=?, telephone=?, cin=?, role=?, statut_verification=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nom);
            pst.setString(2, prenom);
            pst.setString(3, email);
            pst.setString(4, telephone);
            pst.setString(5, cin);
            pst.setString(6, role.name());
            pst.setString(7, sv.name());
            pst.setInt(8, id);
            pst.executeUpdate();
        }

        if (sv == StatutVerification.VERIFIE) setActive(id, true);
        if (sv == StatutVerification.REFUSE || sv == StatutVerification.EN_ATTENTE) setActive(id, false);
    }
    // =========================
    // CREATE USER (ajout dans la base)
    // =========================
    public void createUserAdmin(String nom, String prenom, String email, String telephone,
                                String cin, String role, String statut_verification,
                                String motDePasse, int estActif) throws SQLException {

        String sql = "INSERT INTO users (nom, prenom, email, telephone, cin, mot_de_passe, role, est_actif, statut_verification) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, (prenom == null || prenom.isBlank()) ? null : prenom);
            ps.setString(3, email);
            ps.setString(4, (telephone == null || telephone.isBlank()) ? null : telephone);
            ps.setString(5, (cin == null || cin.isBlank()) ? null : cin);
            ps.setString(6, motDePasse); // (recommandé: hash)
            ps.setString(7, role);
            ps.setInt(8, estActif);      // ✅ correspond à est_actif
            ps.setString(9, statut_verification); // ✅ correspond à statut_verification
            ps.executeUpdate();
        }
    }






    // =========================================================
    // DELETE (Admin)
    // =========================================================
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        }
    }

    // =========================================================
    // INSCRIPTION (Pending) - 2 variantes
    // =========================================================

    // Variante simple (nom+email+pass+role) -> EN_ATTENTE
    public String registerPending(String fullName, String email, String password, String roleStr) throws SQLException {
        Role role;
        try {
            role = Role.valueOf(roleStr.trim().toUpperCase());
        } catch (Exception e) {
            return "ROLE_INVALID";
        }

        if (findByEmail(email) != null) return "EMAIL_EXISTS";

        String sql = "INSERT INTO users(nom, email, mot_de_passe, role, est_actif, statut_verification) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, fullName);
            pst.setString(2, email);
            pst.setString(3, password);
            pst.setString(4, role.name());
            pst.setBoolean(5, false);
            pst.setString(6, StatutVerification.EN_ATTENTE.name());
            pst.executeUpdate();
        }
        return "OK";
    }

    // Variante avec telephone+cin -> NON_VERIFIE (comme dans ton code)
    public String registerPending(String fullName, String email, String password, String roleStr,
                                  String telephone, String cin) throws SQLException {

        Role role;
        try {
            role = Role.valueOf(roleStr.trim().toUpperCase());
        } catch (Exception e) {
            return "ROLE_INVALID";
        }

        if (findByEmail(email) != null) return "EMAIL_EXISTS";

        String sql = "INSERT INTO users(nom, email, telephone, cin, mot_de_passe, role, est_actif, statut_verification) " +
                "VALUES (?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, fullName);
            pst.setString(2, email);
            pst.setString(3, (telephone == null || telephone.isBlank()) ? null : telephone.trim());
            pst.setString(4, (cin == null || cin.isBlank()) ? null : cin.trim());
            pst.setString(5, password);
            pst.setString(6, role.name());
            pst.setBoolean(7, true);
            pst.setString(8, StatutVerification.NON_VERIFIE.name());
            pst.executeUpdate();
        }

        return "OK";
    }

    public void submitProfileForVerification(int userId) throws SQLException {
        setVerificationStatus(userId, StatutVerification.EN_ATTENTE, false);
    }

    // =========================================================
    // LOGIN
    // =========================================================
    public String login(String email, String password) throws SQLException {
        Session.setCurrentUser(null);
        if (email == null || password == null) return "INVALID";

        User u = findByEmail(email.trim().toLowerCase());
        if (u == null) return "INVALID";

        if (u.getPassword() == null || !u.getPassword().equals(password)) return "INVALID";

        // ADMIN bypass
        if (u.getRole() == Role.ADMIN) {
            Session.setCurrentUser(u);
            return "OK_ADMIN";
        }

        // verification
        StatutVerification sv = u.getStatutVerification();
        if (sv == StatutVerification.NON_VERIFIE) {
            Session.setCurrentUser(u);
            return "OK_NEED_PROFILE";
        }
        if (sv == StatutVerification.EN_ATTENTE) return "PENDING";
        if (sv == StatutVerification.REFUSE) return "REFUSED";
        if (sv != StatutVerification.VERIFIE) return "PENDING";

        // actif
        if (!u.isActive()) return "INACTIVE";

        Session.setCurrentUser(u);
        return "OK_USER";
    }
}
