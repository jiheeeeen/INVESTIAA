package Controllers;



import Entities.Role;
import Entities.User;
import Services.UserCRUD;
import Services.Face.FaceAuthManager;
import Services.Face.FaceTemplateService;
import Utils.Session;
import Entities.StatutVerification;

public class FaceBridge {

    private final UserCRUD userCRUD;
    private final FaceTemplateService templateService;
    private final FaceAuthManager authManager;

    private final java.util.function.Consumer<String> redirectHtml; // path

    public FaceBridge(UserCRUD userCRUD, java.util.function.Consumer<String> redirectHtml) {
        this.userCRUD = userCRUD;
        this.redirectHtml = redirectHtml;
        this.templateService = new FaceTemplateService();
        this.authManager = new FaceAuthManager(userCRUD);
    }

    // =========================
    // 1) EnrÃ´lement (profil)
    // =========================
    public void enrollFace() {
        try {
            User current = Session.getCurrentUser();
            if (current == null) {
                jsAlert("Vous devez Ãªtre connectÃ© pour activer Face Login.");
                return;
            }

            // capture 3 fois pour stabilitÃ© (mÃ©tier pro)
            String t1 = templateService.captureAndBuildTemplate();
            String t2 = templateService.captureAndBuildTemplate();
            String t3 = templateService.captureAndBuildTemplate();

            // moyenne simple: on prend le plus long (pragmatique)
            String finalTpl = (t1.length() >= t2.length() && t1.length() >= t3.length()) ? t1 : (t2.length() >= t3.length() ? t2 : t3);

            userCRUD.enableFaceForUser(current.getId(), finalTpl);

            jsAlert("âœ… Face Login activÃ© avec succÃ¨s.");
        } catch (Exception ex) {
            ex.printStackTrace();
            jsAlert("âŒ Erreur enrÃ´lement face: " + ex.getMessage());
        }
    }

    // =========================
    // 2) Login par visage
    // =========================
    public void faceLogin() {
        try {
            String tpl = templateService.captureAndBuildTemplate();
            FaceAuthManager.FaceLoginResult res = authManager.loginWithFace(tpl);

            if (res == null || !res.ok || res.user == null) {
                if (res != null && "NO_ENROLLED_USERS".equals(res.code)) {
                    jsAlert("Aucun compte n'a activÃ© Face Login.");
                } else if (res != null && "LOCKED".equals(res.code)) {
                    jsAlert("AccÃ¨s bloquÃ© temporairement. RÃ©essayez plus tard.");
                } else if (res != null && "NO_MATCH".equals(res.code)) {
                    jsAlert("Visage non reconnu.");
                } else {
                    jsAlert("Erreur Face Login.");
                }
                return;
            }

            User u = res.user;

            // 1) Admin
            if (u.getRole() == Role.ADMIN) {
                redirectHtml.accept("/html/admin_dashboard.html");
                return;
            }

            // 2) Profil non complÃ©tÃ© -> page selon rÃ´le
            boolean completed = userCRUD.isProfileCompleted(u.getId());
            if (!completed) {
                if (u.getRole() == Role.INVESTISSEUR) {
                    redirectHtml.accept("/completerInfos_investisseur.html");
                } else {
                    redirectHtml.accept("/completerInfos.html");
                }
                return;
            }

            // 3) Infos complÃ©tÃ©es MAIS pas encore validÃ©/actif -> login pending
            if (u.getStatutVerification() != StatutVerification.VERIFIE || !u.isActive()) {
                redirectHtml.accept("/html/login.html?pending=1");
                return;
            }

            // 4) OK -> accueil selon rÃ´le
            if (u.getRole() == Role.INVESTISSEUR) {
                redirectHtml.accept("/web/accueil_investisseur.html");
            } else {
                redirectHtml.accept("/accueil.html");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            jsAlert("âŒ Erreur login face: " + ex.getMessage());
        }
    }

    // Bridge simple: lâ€™alert cÃ´tÃ© WebView est gÃ©rÃ© par WebAuthController via engine.setOnAlert
    private void jsAlert(String msg) {
        // on utilise alert JS (WebView capte via setOnAlert)
        // on va juste imprimer et laisser le HTML appeler alert si besoin.
        System.out.println("FACE => " + msg);
        // si tu veux forcer alert, on peut le faire via engine.executeScript,
        // mais ici on reste simple (log) car ton WebView affiche dÃ©jÃ .
    }
    public String captureForSignup() {
        try {
            return templateService.captureAndBuildTemplate();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "ERROR:" + (ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }
}
