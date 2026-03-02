package Controllers;

import Entities.Investissement;
import Entities.User;
import Services.InvestissementCRUD;
import Services.ProfilInvestisseurCRUD;
import Utils.MyBD;
import Utils.Session;
import Utils.sceneManager; // ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¦ AJOUT MINIMAL

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaBridgeMesInvestissements {

    private final InvestissementCRUD investissementCRUD = new InvestissementCRUD();
    private final ProfilInvestisseurCRUD profilCrud = new ProfilInvestisseurCRUD();

    // ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¦ appelÃƒÆ’Ã‚Â©e dans HTML : investBridge.getMesInvestissementsJson()
    public String getMesInvestissementsJson() {
        try {
            User user = Session.getCurrentUser();
            if (user == null) return "[]";

            List<Investissement> list = null;

            // Chemin nominal: investissement lie a id_investisseur.
            try {
                int idInvestisseur = profilCrud.getIdInvestisseurByUserId(user.getId());
                list = investissementCRUD.afficherParInvestisseur(idInvestisseur);
            } catch (Exception ignored) {
                // Fallback ci-dessous.
            }

            // Fallback: certains schemas utilisent directement l'id utilisateur.
            if ((list == null || list.isEmpty()) && investissementCRUD.investorColumnIsUserId()) {
                list = investissementCRUD.afficherParInvestisseur(user.getId());
            }

            if (list == null) list = java.util.Collections.emptyList();

            // ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¦ cache titres projets (ÃƒÆ’Ã‚Â©vite refaire requÃƒÆ’Ã‚Âªte pour chaque ligne)
            Map<Integer, String> titres = loadProjetTitres(list);

            return toJson(list, titres);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    // ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¦ AJOUT MINIMAL : WALLET (pour navbar)
    public String openWalletInvestisseur() {
        try {
            javafx.application.Platform.runLater(() ->
                    sceneManager.switchTo("/web/wallet_investisseur_view.fxml", "Investia - Mon Wallet")
            );
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    // ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¦ AJOUT MINIMAL : alias (si navbar appelle openWallet)
    public String openWallet() {
        return openWalletInvestisseur();
    }

    private Map<Integer, String> loadProjetTitres(List<Investissement> list) {
        Map<Integer, String> map = new HashMap<>();
        if (list == null || list.isEmpty()) return map;

        Connection c = null;
        PreparedStatement ps = null;

        try {
            // ÃƒÂ¢Ã…Â¡Ã‚Â ÃƒÂ¯Ã‚Â¸Ã‚Â IMPORTANT: NE PAS fermer la connexion si MyBD renvoie une connexion singleton
            c = MyBD.getInstance().getConn();

            String sql = "SELECT id_projet, titre FROM projet WHERE id_projet = ?";
            ps = c.prepareStatement(sql);

            for (Investissement inv : list) {
                int pid = inv.getId_projet();
                if (pid <= 0 || map.containsKey(pid)) continue;

                ps.setInt(1, pid);

                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                    if (rs.next()) map.put(pid, rs.getString("titre"));
                    else map.put(pid, null);
                } finally {
                    try { if (rs != null) rs.close(); } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            // si erreur DB, on laisse map vide -> affichage fallback ID
            e.printStackTrace();
        } finally {
            // ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¦ On ferme seulement statement (PAS la connexion)
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }

        return map;
    }

    private String toJson(List<Investissement> list, Map<Integer, String> titres) {
        if (list == null || list.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Investissement inv = list.get(i);

            int id = inv.getId_investissement();
            double montant = inv.getMontant();
            String date = (inv.getDate_investissement() == null) ? "" : inv.getDate_investissement().toString();
            int projetId = inv.getId_projet();

            String titre = titres == null ? null : titres.get(projetId);

            sb.append("{")
                    .append("\"id\":").append(id).append(",")
                    .append("\"montant\":").append(montant).append(",")
                    .append("\"date\":\"").append(escape(date)).append("\",")
                    .append("\"projetId\":").append(projetId).append(",")
                    .append("\"projetTitre\":").append(jsonString(titre))
                    .append("}");

            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + escape(s) + "\"";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}


