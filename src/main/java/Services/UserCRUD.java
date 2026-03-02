package Services;

import Entities.*;
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
        u.setFaceTemplate(rs.getString("face_template"));

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

    // Variante avec telephone+cin -> EN_ATTENTE (pour validation admin)
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
            pst.setString(2, email.trim().toLowerCase());
            pst.setString(3, (telephone == null || telephone.isBlank()) ? null : telephone.trim());
            pst.setString(4, (cin == null || cin.isBlank()) ? null : cin.trim());
            pst.setString(5, password);
            pst.setString(6, role.name());

            // ✅ IMPORTANT: admin doit valider
            pst.setBoolean(7, false);
            pst.setString(8, StatutVerification.EN_ATTENTE.name());

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

    // =========================================================
// GOOGLE LOGIN (OAuth) - SANS casser login() existant
// =========================================================

    public String loginWithGoogle(String email, String fullName) throws SQLException {
        Session.setCurrentUser(null);

        if (email == null || email.isBlank()) return "INVALID";
        email = email.trim().toLowerCase();

        // 1) chercher user
        User u = findByEmail(email);

        // 2) si n'existe pas -> créer un user minimal
        if (u == null) {
            createUserFromGoogle(fullName, email);
            u = findByEmail(email);
            if (u == null) return "ERROR_CREATE";
        }

        // ADMIN bypass (comme login())
        if (u.getRole() == Role.ADMIN) {
            Session.setCurrentUser(u);
            return "OK_ADMIN";
        }

        // verification (même logique que login())
        StatutVerification sv = u.getStatutVerification();

        // NON_VERIFIE => on autorise l'accès pour compléter profil
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

    private void createUserFromGoogle(String fullName, String email) throws SQLException {
        // Table: mot_de_passe NOT NULL => on met un mot de passe aléatoire (user se connecte via Google)
        String randomPass = randomPassword();

        // nom NOT NULL => si Google name vide, on prend la partie avant @
        String nom = (fullName != null && !fullName.isBlank())
                ? fullName.trim()
                : email.split("@")[0];

        // Choix cohérent avec ton flow : user doit compléter profil
        // => NON_VERIFIE + actif=true
        Role defaultRole = Role.INVESTISSEUR;

        String sql = "INSERT INTO users(nom, prenom, email, telephone, cin, date_naissance, nationalite, adresse, ville, " +
                "mot_de_passe, role, est_actif, statut_verification) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nom);
            pst.setString(2, null);      // prenom nullable
            pst.setString(3, email);
            pst.setString(4, null);      // telephone nullable
            pst.setString(5, null);      // cin nullable
            pst.setDate(6, null);        // date_naissance nullable
            pst.setString(7, null);      // nationalite nullable
            pst.setString(8, null);      // adresse nullable
            pst.setString(9, null);      // ville nullable
            pst.setString(10, randomPass);
            pst.setString(11, defaultRole.name());
            pst.setBoolean(12, true);
            pst.setString(13, StatutVerification.NON_VERIFIE.name());
            pst.executeUpdate();
        }
    }

    private static String randomPassword() {
        // simple et robuste pour "placeholder" (projet)
        java.security.SecureRandom r = new java.security.SecureRandom();
        byte[] bytes = new byte[24];
        r.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // =========================
// FACE - persistence
// =========================

    public void enableFaceForUser(int userId, String faceTemplate) throws SQLException {
        String sql = "UPDATE users SET face_enabled=1, face_template=?, face_enrolled_at=NOW(), face_fail_count=0, face_locked_until=NULL WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, faceTemplate);
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    public void resetFaceFails(int userId) throws SQLException {
        String sql = "UPDATE users SET face_fail_count=0, face_locked_until=NULL, face_last_login_at=NOW() WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        }
    }

    public void incrementFaceFail(int userId) throws SQLException {
        // +1 fail, et si >=5 alors lock 10 min
        String sql = """
        UPDATE users
        SET face_fail_count = face_fail_count + 1,
            face_locked_until = CASE
                WHEN (face_fail_count + 1) >= 5 THEN DATE_ADD(NOW(), INTERVAL 10 MINUTE)
                ELSE face_locked_until
            END
        WHERE id = ?
        """;
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        }
    }

    public boolean isFaceLocked(int userId) throws SQLException {
        String sql = "SELECT face_locked_until FROM users WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return false;
                Timestamp ts = rs.getTimestamp(1);
                return ts != null && ts.after(new Timestamp(System.currentTimeMillis()));
            }
        }
    }

    public boolean isProfileCompleted(int userId) {
        try {
            User u = findById(userId);
            if (u == null || u.getRole() == null) return false;

            if (u.getRole() == Role.ADMIN) return true;

            if (u.getRole() == Role.ENTREPRENEUR) {
                ProfilEntrepreneurCRUD peCrud = new ProfilEntrepreneurCRUD();
                ProfilEntrepreneur pe = peCrud.getByUserId(userId);
                if (pe == null) return false;

                // ✅ critères "profil complété" entrepreneur (ajuste si besoin)
                return notBlank(pe.getCinRectoUrl())
                        && notBlank(pe.getCinVersoUrl())
                        && notBlank(pe.getJustificatifDomicileUrl())
                        && notBlank(pe.getRib());
            }

            if (u.getRole() == Role.INVESTISSEUR) {
                ProfilInvestisseurCRUD piCrud = new ProfilInvestisseurCRUD();
                ProfilInvestisseur pi = piCrud.getByUserId(userId);
                if (pi == null) return false;

                // ✅ critères "profil complété" investisseur (ajuste si besoin)
                return pi.getBudgetTotal() != null
                        && pi.getTicketMoyenParProjet() != null
                        && notBlank(pi.getCinRectoUrl())
                        && notBlank(pi.getCinVersoUrl())
                        && notBlank(pi.getPhotoUrl());
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public void markProfileCompleted(int userId) throws SQLException {
        String sql = "UPDATE users SET profile_completed=1, profile_completed_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public List<User> getFaceEnabledUsers() throws SQLException {
        String sql = "SELECT * FROM users WHERE face_enabled=1 AND face_template IS NOT NULL";
        List<User> list = new ArrayList<>();
        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) list.add(mapUser(rs));
        }
        return list;
    }
    public String registerPending(String fullName, String email, String password, String roleStr,
                                  String telephone, String cin, String faceTemplate) throws SQLException {

        Role role;
        try {
            role = Role.valueOf(roleStr.trim().toUpperCase());
        } catch (Exception e) {
            return "ROLE_INVALID";
        }

        if (findByEmail(email) != null) return "EMAIL_EXISTS";

        boolean faceEnabled = faceTemplate != null && !faceTemplate.trim().isEmpty();

        String sql = "INSERT INTO users(nom, email, telephone, cin, mot_de_passe, role, est_actif, statut_verification, face_enabled, face_template) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, fullName);
            pst.setString(2, email.trim().toLowerCase());
            pst.setString(3, (telephone == null || telephone.isBlank()) ? null : telephone.trim());
            pst.setString(4, (cin == null || cin.isBlank()) ? null : cin.trim());
            pst.setString(5, password);
            pst.setString(6, role.name());

            // ✅ IMPORTANT: admin doit valider
            pst.setBoolean(7, false);
            pst.setString(8, StatutVerification.EN_ATTENTE.name());

            pst.setBoolean(9, faceEnabled);
            pst.setString(10, faceEnabled ? faceTemplate.trim() : null);

            pst.executeUpdate();
        }

        return "OK";
    }
}
