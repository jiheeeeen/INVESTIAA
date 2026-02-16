package Services;

import Entities.*;
import Utils.MyBD;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ProfilEntrepreneurCRUD implements InterfaceCRUD<ProfilEntrepreneur> {

    private final Connection conn;

    public ProfilEntrepreneurCRUD() {
        this.conn = MyBD.getInstance().getConn();
    }

    // ===== Helpers secteurs <-> String(MySQL SET) =====
    private String secteursToDb(Set<Secteur> secteurs) {
        if (secteurs == null || secteurs.isEmpty()) return null;
        return secteurs.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private Set<Secteur> dbToSecteurs(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) return null;
        Set<Secteur> set = new HashSet<>();
        for (String s : dbValue.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) set.add(Secteur.valueOf(v));
        }
        return set;
    }

    // ✅ Prend un entrepreneur (users.role='ENTREPRENEUR') qui n’a pas encore de profil
    public Integer getEntrepreneurUserIdWithoutProfil() throws SQLException {
        String sql =
                "SELECT u.id " +
                        "FROM users u " +
                        "LEFT JOIN profil_entrepreneur p ON p.id_user = u.id " +
                        "WHERE u.role='ENTREPRENEUR' AND p.id_user IS NULL " +
                        "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("id");
        }
        return null;
    }

    public ProfilEntrepreneur getByUserId(int idUser) throws SQLException {
        String sql = "SELECT * FROM profil_entrepreneur WHERE id_user=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void upsertForCurrentUser(ProfilEntrepreneur p) throws SQLException {
        ProfilEntrepreneur existing = getByUserId(p.getIdUser());
        if (existing == null) {
            ajouter(p);
            return;
        }
        p.setIdEntrepreneur(existing.getIdEntrepreneur());
        modifier(p);
    }

    public void updateVerificationByUserId(int userId, StatutVerification statut) throws SQLException {
        String sql = "UPDATE profil_entrepreneur SET statut_verification=?, date_verification=? WHERE id_user=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            if (statut == StatutVerification.VERIFIE || statut == StatutVerification.REFUSE) {
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void ajouter(ProfilEntrepreneur p) throws SQLException {

        String sql = "INSERT INTO profil_entrepreneur (" +
                "id_user, adresse, cin_recto_url, cin_verso_url, justificatif_domicile_url, rib, " +
                "accepte_conditions, statut_compte, statut_verification, date_verification, " +
                "bio, photo_url, registre_commerce_url, patente_url, matricule_fiscal_url, carte_fiscale_url, secteurs" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, p.getIdUser());

            ps.setString(2, p.getAdresse());
            ps.setString(3, p.getCinRectoUrl());
            ps.setString(4, p.getCinVersoUrl());
            ps.setString(5, p.getJustificatifDomicileUrl());
            ps.setString(6, p.getRib());

            ps.setBoolean(7, p.isAccepteConditions());

            ps.setString(8, (p.getStatutCompte() != null ? p.getStatutCompte().name() : StatutCompte.ACTIF.name()));
            ps.setString(9, (p.getStatutVerification() != null ? p.getStatutVerification().name() : StatutVerification.NON_VERIFIE.name()));

            if (p.getDateVerification() != null) ps.setTimestamp(10, p.getDateVerification());
            else ps.setNull(10, Types.TIMESTAMP);

            ps.setString(11, p.getBio());
            ps.setString(12, p.getPhotoUrl());

            ps.setString(13, p.getRegistreCommerceUrl());
            ps.setString(14, p.getPatenteUrl());
            ps.setString(15, p.getMatriculeFiscalUrl());
            ps.setString(16, p.getCarteFiscaleUrl());

            String secteursDb = secteursToDb(p.getSecteurs());
            if (secteursDb != null) ps.setString(17, secteursDb);
            else ps.setNull(17, Types.VARCHAR);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setIdEntrepreneur(rs.getInt(1));
            }
        }
    }

    // ✅ Ajout automatique: choisit un entrepreneur libre
    public void ajouterAuto(ProfilEntrepreneur p) throws SQLException {
        Integer idUser = getEntrepreneurUserIdWithoutProfil();
        if (idUser == null) {
            throw new SQLException("Aucun user ENTREPRENEUR libre trouvé (tous ont déjà un profil, ou aucun entrepreneur).");
        }
        p.setIdUser(idUser);
        ajouter(p);
    }

    @Override
    public void modifier(ProfilEntrepreneur p) throws SQLException {

        String sql = "UPDATE profil_entrepreneur SET " +
                "id_user=?, adresse=?, cin_recto_url=?, cin_verso_url=?, justificatif_domicile_url=?, rib=?, " +
                "accepte_conditions=?, statut_compte=?, statut_verification=?, date_verification=?, " +
                "bio=?, photo_url=?, registre_commerce_url=?, patente_url=?, matricule_fiscal_url=?, carte_fiscale_url=?, secteurs=? " +
                "WHERE id_entrepreneur=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, p.getIdUser());

            ps.setString(2, p.getAdresse());
            ps.setString(3, p.getCinRectoUrl());
            ps.setString(4, p.getCinVersoUrl());
            ps.setString(5, p.getJustificatifDomicileUrl());
            ps.setString(6, p.getRib());

            ps.setBoolean(7, p.isAccepteConditions());

            ps.setString(8, (p.getStatutCompte() != null ? p.getStatutCompte().name() : StatutCompte.ACTIF.name()));
            ps.setString(9, (p.getStatutVerification() != null ? p.getStatutVerification().name() : StatutVerification.NON_VERIFIE.name()));

            if (p.getDateVerification() != null) ps.setTimestamp(10, p.getDateVerification());
            else ps.setNull(10, Types.TIMESTAMP);

            ps.setString(11, p.getBio());
            ps.setString(12, p.getPhotoUrl());

            ps.setString(13, p.getRegistreCommerceUrl());
            ps.setString(14, p.getPatenteUrl());
            ps.setString(15, p.getMatriculeFiscalUrl());
            ps.setString(16, p.getCarteFiscaleUrl());

            String secteursDb = secteursToDb(p.getSecteurs());
            if (secteursDb != null) ps.setString(17, secteursDb);
            else ps.setNull(17, Types.VARCHAR);

            ps.setInt(18, p.getIdEntrepreneur());

            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int idEntrepreneur) throws SQLException {
        String sql = "DELETE FROM profil_entrepreneur WHERE id_entrepreneur=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEntrepreneur);
            int rows = ps.executeUpdate();
            System.out.println("DEBUG delete rows = " + rows);
        }
    }


    @Override
    public List<ProfilEntrepreneur> afficher() throws SQLException {
        String sql = "SELECT * FROM profil_entrepreneur ORDER BY id_entrepreneur DESC";
        List<ProfilEntrepreneur> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public ProfilEntrepreneur getById(int idEntrepreneur) throws SQLException {
        String sql = "SELECT * FROM profil_entrepreneur WHERE id_entrepreneur=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEntrepreneur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private ProfilEntrepreneur mapRow(ResultSet rs) throws SQLException {
        ProfilEntrepreneur p = new ProfilEntrepreneur();

        p.setIdEntrepreneur(rs.getInt("id_entrepreneur"));
        p.setIdUser(rs.getInt("id_user"));

        p.setAdresse(rs.getString("adresse"));
        p.setCinRectoUrl(rs.getString("cin_recto_url"));
        p.setCinVersoUrl(rs.getString("cin_verso_url"));
        p.setJustificatifDomicileUrl(rs.getString("justificatif_domicile_url"));

        p.setRib(rs.getString("rib"));
        p.setAccepteConditions(rs.getBoolean("accepte_conditions"));

        String sc = rs.getString("statut_compte");
        p.setStatutCompte(sc != null ? StatutCompte.valueOf(sc) : StatutCompte.ACTIF);

        String sv = rs.getString("statut_verification");
        p.setStatutVerification(sv != null ? StatutVerification.valueOf(sv) : StatutVerification.NON_VERIFIE);

        p.setDateVerification(rs.getTimestamp("date_verification"));

        p.setBio(rs.getString("bio"));
        p.setPhotoUrl(rs.getString("photo_url"));

        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));

        p.setRegistreCommerceUrl(rs.getString("registre_commerce_url"));
        p.setPatenteUrl(rs.getString("patente_url"));
        p.setMatriculeFiscalUrl(rs.getString("matricule_fiscal_url"));
        p.setCarteFiscaleUrl(rs.getString("carte_fiscale_url"));

        p.setSecteurs(dbToSecteurs(rs.getString("secteurs")));

        return p;
    }

}
