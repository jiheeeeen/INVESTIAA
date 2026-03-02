package Controllers;

import Entities.Projet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProjectAnalysisService {

    public String analyseAsJson(Projet p) {

        double objectif = d(p.getObjectifTnd());
        double taux = d(p.getTauxInteretPct()); // peut être null
        int dureeMois = nvl(p.getDureeRemboursementMois(), 12);
        double marge = d(p.getMargeBruteEstimeeTnd());
        double net = d(p.getResultatNetEstimeTnd());
        int campagneJours = (p.getDureeCampagneJours() > 0) ? p.getDureeCampagneJours() : 30;

        String titre = s(p.getTitre());
        String secteur = s(p.getSecteur());

        // -------- KPI financiers --------
        double interets = objectif * (taux / 100.0) * (dureeMois / 12.0);
        double totalRemb = objectif + interets;
        double mensualite = (dureeMois > 0) ? (totalRemb / dureeMois) : 0.0;

        // net estimé (annuel) -> net mensuel
        double netMensuel = net / 12.0;

        double dscr = (mensualite > 0) ? (netMensuel / mensualite) : 0.0;
        double profitMargin = (objectif > 0) ? (net / objectif) : 0.0;
        double costRatio = (objectif > 0) ? (interets / objectif) : 0.0;

        // -------- Scores 0..1 (pour % décision) --------
        double sProfit = clamp01((profitMargin - 0.05) / 0.25);  // 5% -> 0, 30% -> 1
        double sDscr   = clamp01((dscr - 1.0) / 0.6);            // 1.0 -> 0, 1.6 -> 1
        double sCost   = clamp01(1.0 - (costRatio / 0.25));      // 0% -> 1, 25% -> 0
        double sCamp   = clamp01((campagneJours - 20.0) / 40.0); // 20j -> 0, 60j -> 1
        double sSect   = secteurScore(secteur);                  // 0..1

        // -------- Score global pondéré --------
        double score = (0.30 * sProfit) + (0.30 * sDscr) + (0.20 * sCost) + (0.15 * sCamp) + (0.05 * sSect);

        int beneficePct = clampInt((int) Math.round(score * 100.0), 0, 100);
        int risquePct = 100 - beneficePct;

        int confiancePct = computeConfidence(objectif, taux, dureeMois, marge, net, campagneJours);

        String reco = (beneficePct >= 70) ? "BENEFIQUE" : (beneficePct >= 50 ? "MITIGE" : "RISQUE");

        // -------- Radar (0..100) --------
        String radarLabels = "[\"Rentabilité\",\"Remboursement\",\"Coût\",\"Campagne\",\"Secteur\"]";
        String radarValues = "[" + r100(sProfit) + "," + r100(sDscr) + "," + r100(sCost) + "," + r100(sCamp) + "," + r100(sSect) + "]";

        // -------- Séries 12 mois (Net vs Mensualité) --------
        String[] months = {"Jan","Fév","Mar","Avr","Mai","Juin","Juil","Août","Sep","Oct","Nov","Déc"};
        List<Double> netSeries = new ArrayList<>();
        List<Double> mensSeries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            double factor = 0.85 + 0.25 * Math.sin((i / 11.0) * Math.PI); // courbe douce
            netSeries.add(netMensuel * factor);
            mensSeries.add(mensualite);
        }

        // -------- Contributions (bar chart) --------
        String contributions = "[" +
                obj("Rentabilité", (int)Math.round((sProfit - 0.5) * 40)) + "," +
                obj("Remboursement", (int)Math.round((sDscr - 0.5) * 40)) + "," +
                obj("Coût financement", (int)Math.round((sCost - 0.5) * 30)) + "," +
                obj("Durée campagne", (int)Math.round((sCamp - 0.5) * 25)) +
                "]";

        // -------- Explication courte --------
        List<String> explain = new ArrayList<>();
        explain.add("DSCR ≈ " + fmt(dscr) + " (≥ 1.2 recommandé)");
        explain.add("Coût financement ≈ " + fmt(costRatio * 100) + "% de l'objectif");
        explain.add("Marge nette ≈ " + fmt(profitMargin * 100) + "%");

        // -------- JSON final (format attendu par ton JS) --------
        return "{"
                + "\"project\":{"
                + "\"id\":" + p.getIdProjet() + ","
                + "\"titre\":\"" + esc(titre) + "\","
                + "\"secteur\":\"" + esc(secteur) + "\""
                + "},"
                + "\"decision\":{"
                + "\"benefice_pct\":" + beneficePct + ","
                + "\"risque_pct\":" + risquePct + ","
                + "\"confiance_pct\":" + confiancePct + ","
                + "\"recommandation\":\"" + reco + "\""
                + "},"
                + "\"radar\":{"
                + "\"labels\":" + radarLabels + ","
                + "\"values\":" + radarValues
                + "},"
                + "\"series\":{"
                + "\"labels\":" + toJsonArray(months) + ","
                + "\"netMensuel\":" + toJsonArrayNumbers(netSeries) + ","
                + "\"mensualite\":" + toJsonArrayNumbers(mensSeries)
                + "},"
                + "\"contributions\":" + contributions + ","
                + "\"explain\":" + toJsonArray(explain)
                + "}";
    }

    // ----------------- Helpers -----------------
    private double d(BigDecimal v) { return (v == null) ? 0.0 : v.doubleValue(); }
    private int nvl(Integer v, int def) { return (v == null || v <= 0) ? def : v; }
    private String s(String x) { return (x == null) ? "" : x; }

    private double clamp01(double x) { return (x < 0) ? 0 : Math.min(1, x); }
    private int clampInt(int x, int a, int b) { return Math.max(a, Math.min(b, x)); }
    private int r100(double v01) { return (int)Math.round(clamp01(v01) * 100.0); }

    private int computeConfidence(double objectif, double taux, int duree, double marge, double net, int campagne) {
        int ok = 0;
        if (objectif > 0) ok++;
        if (taux > 0) ok++;
        if (duree > 0) ok++;
        if (marge != 0) ok++;
        if (net != 0) ok++;
        if (campagne > 0) ok++;
        int pct = (int)Math.round((ok / 6.0) * 100.0);
        return Math.max(40, pct); // minimum 40%
    }

    private double secteurScore(String secteur) {
        if (secteur == null) return 0.60;
        String s = secteur.toLowerCase();
        if (s.contains("agri")) return 0.65;
        if (s.contains("health") || s.contains("med")) return 0.60;
        if (s.contains("fin")) return 0.55;
        if (s.contains("industrie")) return 0.60;
        if (s.contains("tech")) return 0.58;
        return 0.60;
    }

    private String obj(String label, int value) {
        return "{\"label\":\"" + esc(label) + "\",\"value\":" + value + "}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String fmt(double x) {
        return String.valueOf(Math.round(x * 100.0) / 100.0);
    }

    private String toJsonArray(String[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<arr.length;i++){
            sb.append("\"").append(esc(arr[i])).append("\"");
            if (i<arr.length-1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private String toJsonArray(List<String> arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<arr.size();i++){
            sb.append("\"").append(esc(arr.get(i))).append("\"");
            if (i<arr.size()-1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private String toJsonArrayNumbers(List<Double> arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<arr.size();i++){
            sb.append(Math.round(arr.get(i) * 100.0) / 100.0);
            if (i<arr.size()-1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}