package Entities;

import java.util.Locale;

public enum RemboursementStatut {
    EN_ATTENTE,
    PAYE;

    public static RemboursementStatut from(String raw) {
        if (raw == null) return EN_ATTENTE;
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (v.isEmpty()) return EN_ATTENTE;
        if (v.equals("PENDING")) return EN_ATTENTE;
        if (v.equals("PAID")) return PAYE;
        return v.equals("PAYE") ? PAYE : EN_ATTENTE;
    }
}
