package Controllers;

import Entities.ProfilEntrepreneur;
import Entities.Projet;
import Entities.Secteur;
import Entities.StatutCompte;
import Entities.StatutVerification;
import Entities.User;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public final class ProjetWebUtils {
    private ProjetWebUtils() {
    }

    public static String toListJson(Projet p) {
        return toListJsonWithStatus(p, mapStatusForUi(p.getStatut()));
    }

    public static String toListJsonWithStatus(Projet p, String statusOverride) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(p.getIdProjet()).append(",");
        sb.append("\"title\":").append(jsonString(p.getTitre())).append(",");
        sb.append("\"category\":").append(jsonString(p.getSecteur())).append(",");
        sb.append("\"short\":").append(jsonString(p.getDescriptionCourte())).append(",");
        sb.append("\"goal\":").append(p.getObjectifTnd() == null ? "0" : p.getObjectifTnd()).append(",");
        sb.append("\"odd\":").append(jsonString("")).append(",");
        sb.append("\"status\":").append(jsonString(statusOverride)).append(",");
        sb.append("\"updatedAt\":").append(jsonString(formatDate(p.getUpdatedAt()))).append(",");
        sb.append("\"url\":").append(jsonString("modifierProjet.html?id=" + p.getIdProjet()));
        sb.append("}");
        return sb.toString();
    }

    public static String toDetailJson(Projet p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"idProjet\":").append(p.getIdProjet()).append(",");
        sb.append("\"entrepreneurId\":").append(p.getEntrepreneurId()).append(",");
        sb.append("\"statut\":").append(jsonString(p.getStatut())).append(",");
        sb.append("\"titre\":").append(jsonString(p.getTitre())).append(",");
        sb.append("\"secteur\":").append(jsonString(p.getSecteur())).append(",");
        sb.append("\"descriptionCourte\":").append(jsonString(p.getDescriptionCourte())).append(",");
        sb.append("\"descriptionLongue\":").append(jsonString(p.getDescriptionLongue())).append(",");
        sb.append("\"objectifTnd\":").append(p.getObjectifTnd() == null ? "0" : p.getObjectifTnd()).append(",");
        sb.append("\"dureeCampagneJours\":").append(p.getDureeCampagneJours()).append(",");
        sb.append("\"modeRemboursement\":").append(jsonString(p.getModeRemboursement())).append(",");
        sb.append("\"tauxInteretPct\":").append(p.getTauxInteretPct() == null ? "null" : p.getTauxInteretPct()).append(",");
        sb.append("\"dureeRemboursementMois\":").append(p.getDureeRemboursementMois() == null ? "null" : p.getDureeRemboursementMois()).append(",");
        sb.append("\"margeBruteEstimeeTnd\":").append(p.getMargeBruteEstimeeTnd() == null ? "null" : p.getMargeBruteEstimeeTnd()).append(",");
        sb.append("\"resultatNetEstimeTnd\":").append(p.getResultatNetEstimeTnd() == null ? "null" : p.getResultatNetEstimeTnd()).append(",");
        sb.append("\"createdAt\":").append(jsonString(formatTimestamp(p.getCreatedAt()))).append(",");
        sb.append("\"updatedAt\":").append(jsonString(formatTimestamp(p.getUpdatedAt())));
        sb.append("}");
        return sb.toString();
    }

    public static String userToJson(User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(u.getId()).append(",");
        sb.append("\"nom\":").append(jsonString(u.getNom())).append(",");
        sb.append("\"prenom\":").append(jsonString(u.getPrenom())).append(",");
        sb.append("\"email\":").append(jsonString(u.getEmail())).append(",");
        sb.append("\"telephone\":").append(jsonString(u.getTelephone())).append(",");
        sb.append("\"cin\":").append(jsonString(u.getCin())).append(",");
        sb.append("\"dateNaissance\":").append(jsonString(u.getDateNaissance() == null ? null : u.getDateNaissance().toString())).append(",");
        sb.append("\"nationalite\":").append(jsonString(u.getNationalite())).append(",");
        sb.append("\"adresse\":").append(jsonString(u.getAdresse())).append(",");
        sb.append("\"ville\":").append(jsonString(u.getVille())).append(",");
        sb.append("\"role\":").append(jsonString(u.getRole() == null ? null : u.getRole().name())).append(",");
        sb.append("\"statutVerification\":").append(jsonString(u.getStatutVerification() == null ? null : u.getStatutVerification().name())).append(",");
        sb.append("\"active\":").append(u.isActive());
        sb.append("}");
        return sb.toString();
    }

    public static String profilToJson(ProfilEntrepreneur p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"idEntrepreneur\":").append(p.getIdEntrepreneur()).append(",");
        sb.append("\"idUser\":").append(p.getIdUser()).append(",");
        sb.append("\"adresse\":").append(jsonString(p.getAdresse())).append(",");
        sb.append("\"cinRectoUrl\":").append(jsonString(p.getCinRectoUrl())).append(",");
        sb.append("\"cinVersoUrl\":").append(jsonString(p.getCinVersoUrl())).append(",");
        sb.append("\"justificatifDomicileUrl\":").append(jsonString(p.getJustificatifDomicileUrl())).append(",");
        sb.append("\"rib\":").append(jsonString(p.getRib())).append(",");
        sb.append("\"accepteConditions\":").append(p.isAccepteConditions()).append(",");
        sb.append("\"statutCompte\":").append(jsonString(p.getStatutCompte() != null ? p.getStatutCompte().name() : null)).append(",");
        sb.append("\"statutVerification\":").append(jsonString(p.getStatutVerification() != null ? p.getStatutVerification().name() : null)).append(",");
        sb.append("\"dateVerification\":").append(jsonString(formatTimestamp(p.getDateVerification()))).append(",");
        sb.append("\"bio\":").append(jsonString(p.getBio())).append(",");
        sb.append("\"photoUrl\":").append(jsonString(p.getPhotoUrl())).append(",");
        sb.append("\"registreCommerceUrl\":").append(jsonString(p.getRegistreCommerceUrl())).append(",");
        sb.append("\"patenteUrl\":").append(jsonString(p.getPatenteUrl())).append(",");
        sb.append("\"matriculeFiscalUrl\":").append(jsonString(p.getMatriculeFiscalUrl())).append(",");
        sb.append("\"carteFiscaleUrl\":").append(jsonString(p.getCarteFiscaleUrl())).append(",");
        sb.append("\"secteurs\":").append(jsonString(joinSecteurs(p.getSecteurs())));
        sb.append("}");
        return sb.toString();
    }

    public static String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static String jsonString(String value) {
        if (value == null) return "null";
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    public static String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static int parseIntRequired(String value) {
        return Integer.parseInt(value.trim());
    }

    public static Integer parseIntOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : Integer.parseInt(trimmed);
    }

    public static BigDecimal parseBigDecimalRequired(String value) {
        return new BigDecimal(value.trim());
    }

    public static BigDecimal parseBigDecimalOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : new BigDecimal(trimmed);
    }

    public static boolean parseBoolean(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("on") || v.equals("yes");
    }

    public static StatutCompte parseStatutCompte(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return StatutCompte.valueOf(value.trim());
    }

    public static StatutVerification parseStatutVerification(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return StatutVerification.valueOf(value.trim());
    }

    public static Timestamp parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Timestamp.valueOf(value.trim());
    }

    public static Date parseSqlDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Date.valueOf(value.trim());
    }

    public static Set<Secteur> parseSecteurs(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        Set<Secteur> set = new HashSet<>();
        for (String s : csv.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) {
                set.add(Secteur.valueOf(v));
            }
        }
        return set.isEmpty() ? null : set;
    }

    public static String mergeBioWithAutreSecteur(String bio, String autreSecteur) {
        String cleanBio = removeAutreSecteurMarker(bio);
        if (autreSecteur == null || autreSecteur.isBlank()) {
            return cleanBio;
        }
        String marker = "[AUTRE_SECTEUR] " + autreSecteur.trim();
        if (cleanBio == null || cleanBio.isBlank()) {
            return marker;
        }
        return cleanBio + "\n" + marker;
    }

    public static String sanitizeDocType(String input) {
        if (input == null || input.isBlank()) return "document";
        String out = input.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        return out.isBlank() ? "document" : out;
    }

    public static String mapStatusForUi(String statut) {
        if (statut == null) return "DRAFT";
        switch (statut) {
            case "BROUILLON":
                return "DRAFT";
            case "EN_ATTENTE":
                return "PENDING";
            case "VALIDE":
                return "VALIDATED";
            case "EN_COURS":
                return "EN_COURS";
            case "REFUSE":
                return "REJECTED";
            default:
                return "DRAFT";
        }
    }

    private static String formatDate(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().toLocalDate().format(DateTimeFormatter.ISO_DATE);
    }

    private static String joinSecteurs(Set<Secteur> secteurs) {
        if (secteurs == null || secteurs.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Secteur s : secteurs) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s.name());
        }
        return sb.toString();
    }

    private static String removeAutreSecteurMarker(String bio) {
        if (bio == null || bio.isBlank()) return bio;
        StringBuilder kept = new StringBuilder();
        for (String line : bio.split("\\R")) {
            if (line.trim().startsWith("[AUTRE_SECTEUR]")) continue;
            if (kept.length() > 0) kept.append('\n');
            kept.append(line);
        }
        String out = kept.toString().trim();
        return out.isEmpty() ? null : out;
    }
}
