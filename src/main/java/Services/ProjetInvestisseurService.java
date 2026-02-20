package Services;

import Utils.JsonUtil;

public class ProjetInvestisseurService {

    private final ProjetInvestisseurDAO dao = new ProjetInvestisseurDAO();

    public String listProjetsJson() throws Exception {
        return JsonUtil.toJson(dao.findProjetCardsForInvestisseur());
    }
}
