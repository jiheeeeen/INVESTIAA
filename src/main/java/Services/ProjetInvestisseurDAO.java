package Services;

import Utils.JsonUtil;
import Utils.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProjetInvestisseurDAO {

    public List<JsonUtil.ProjetCard> findProjetCardsForInvestisseur() throws Exception {
        List<JsonUtil.ProjetCard> list = new ArrayList<>();

        // ✅ Projets visibles investisseur: VALIDE + EN_ATTENTE (tu peux changer)
        String sql = """
            SELECT
                id_projet,
                titre,
                secteur,
                statut,
                description_courte,
                objectif_tnd,
                DATE_FORMAT(updated_at, '%Y-%m-%d') AS updatedAt
            FROM projet
            WHERE statut IN ('VALIDE','EN_ATTENTE')
            ORDER BY updated_at DESC
        """;

        Connection cnx = MyBD.getInstance().getConn();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JsonUtil.ProjetCard p = new JsonUtil.ProjetCard();
                p.id = rs.getInt("id_projet");
                p.title = rs.getString("titre");
                p.shortDesc = rs.getString("description_courte");
                p.category = rs.getString("secteur");
                p.status = mapStatus(rs.getString("statut"));
                p.goal = rs.getBigDecimal("objectif_tnd") == null ? 0 : rs.getBigDecimal("objectif_tnd").doubleValue();
                p.updatedAt = rs.getString("updatedAt");
                p.odd = "";
                list.add(p);
            }
        }
        return list;
    }

    // DB: BROUILLON / EN_ATTENTE / VALIDE / REFUSE
    // UI: DRAFT / PENDING / VALIDATED / REJECTED
    private String mapStatus(String dbStatus) {
        if (dbStatus == null) return "DRAFT";
        return switch (dbStatus.toUpperCase()) {
            case "BROUILLON" -> "DRAFT";
            case "EN_ATTENTE" -> "PENDING";
            case "VALIDE" -> "VALIDATED";
            case "REFUSE" -> "REJECTED";
            default -> "DRAFT";
        };
    }
}
