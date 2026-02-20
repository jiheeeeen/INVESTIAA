package Controllers;

import Services.ProjetInvestisseurService;

public class ProjetInvestisseurBridge {

    private final ProjetInvestisseurService service = new ProjetInvestisseurService();

    // JS : window.javaBridge.listProjets()
    public String listProjets() {
        try {
            return service.listProjetsJson();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
}
