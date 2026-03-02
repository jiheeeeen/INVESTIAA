package Services.Face;

import Entities.Role;
import Entities.User;
import Services.UserCRUD;
import Utils.Session;

import java.util.Base64;
import java.util.List;

public class FaceAuthManager {

    private final UserCRUD userCRUD;

    // Seuils pro par rôle (tu peux ajuster)
    private static final double TH_ADMIN  = 0.95;
    private static final double TH_INVEST = 0.90;
    private static final double TH_ENTR   = 0.88;

    public FaceAuthManager(UserCRUD userCRUD) {
        this.userCRUD = userCRUD;
    }

    public static class FaceLoginResult {
        public final boolean ok;
        public final String code;   // OK / LOCKED / NO_ENROLLED_USERS / NO_MATCH / ERROR
        public final User user;
        public final double score;

        // ✅ Constructeur complet (avec score)
        public FaceLoginResult(boolean ok, String code, User user, double score) {
            this.ok = ok;
            this.code = code;
            this.user = user;
            this.score = score;
        }

        // ✅ Constructeur demandé : (ok, code, user)
        public FaceLoginResult(boolean ok, String code, User user) {
            this(ok, code, user, 0.0);
        }
    }

    /**
     * Login par visage:
     * - compare template capturé vs templates en BD
     * - applique seuil par rôle
     * - gère lock + fail_count
     * - set Session si OK
     *
     * Retourne:
     * - ok=true,  code="OK", user != null
     * - ok=false, code="NO_ENROLLED_USERS"
     * - ok=false, code="LOCKED" (tous bloqués)
     * - ok=false, code="NO_MATCH"
     * - ok=false, code="ERROR"
     */
    public FaceLoginResult loginWithFace(String capturedTemplateB64) {
        try {
            byte[] captured = Base64.getDecoder().decode(capturedTemplateB64);

            List<User> candidates = userCRUD.getFaceEnabledUsers();
            if (candidates == null || candidates.isEmpty()) {
                return new FaceLoginResult(false, "NO_ENROLLED_USERS", null, 0);
            }

            User bestUser = null;
            double bestScore = -1;

            boolean anyLocked = false;
            boolean anyAvailable = false;

            for (User u : candidates) {
                if (u == null) continue;
                if (u.getFaceTemplate() == null || u.getFaceTemplate().isBlank()) continue;

                // locked ?
                if (userCRUD.isFaceLocked(u.getId())) {
                    anyLocked = true;
                    continue;
                }

                anyAvailable = true;

                byte[] tpl = Base64.getDecoder().decode(u.getFaceTemplate());
                double score = cosineSimilarity(captured, tpl);

                if (score > bestScore) {
                    bestScore = score;
                    bestUser = u;
                }
            }

            // Aucun user utilisable
            if (bestUser == null) {
                if (anyLocked && !anyAvailable) {
                    return new FaceLoginResult(false, "LOCKED", null, 0);
                }
                return new FaceLoginResult(false, "NO_MATCH", null, 0);
            }

            // ✅ seuil selon rôle (logique inchangée)
            double threshold = thresholdForRole(bestUser.getRole());
            if (bestScore >= threshold) {
                // ✅ success
                User matchedUser = bestUser;

                // reset fail count
                userCRUD.resetFaceFails(matchedUser.getId());

                // ✅ IMPORTANT : refresh depuis BD (statut_verification / active / autres champs)
                try {
                    User fresh = userCRUD.findById(matchedUser.getId());
                    if (fresh != null) matchedUser = fresh;
                } catch (Exception ignored) {
                    // on garde matchedUser comme il est si la lecture échoue
                }

                // set session
                Session.setCurrentUser(matchedUser);

                // ✅ comme tu as demandé : OK + user + score
                return new FaceLoginResult(true, "OK", matchedUser, bestScore);

            } else {
                // ❌ fail => increment fail_count pour ce user trouvé
                userCRUD.incrementFaceFail(bestUser.getId());

                // on renvoie NO_MATCH + user+score (utile debug)
                return new FaceLoginResult(false, "NO_MATCH", bestUser, bestScore);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new FaceLoginResult(false, "ERROR", null, 0);
        }
    }

    private double thresholdForRole(Role role) {
        if (role == null) return TH_ENTR;
        return switch (role) {
            case ADMIN -> TH_ADMIN;
            case INVESTISSEUR -> TH_INVEST;
            case ENTREPRENEUR -> TH_ENTR;
        };
    }

    /**
     * Cosine similarity entre 2 vecteurs bytes (grayscale 120*120).
     * Normalisation pour éviter dépendance lumière.
     */
    private double cosineSimilarity(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        if (n == 0) return 0;

        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            double va = (a[i] & 0xFF);
            double vb = (b[i] & 0xFF);
            dot += va * vb;
            na += va * va;
            nb += vb * vb;
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}