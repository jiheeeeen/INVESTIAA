package Services;

import Entities.Financement2;
import Entities.Investissement;
import Entities.Invitation;
import Entities.Evenement;
import Entities.ProfilEntrepreneur;
import Entities.ProfilInvestisseur;
import Entities.Projet;
import Entities.Remboursement;
import Entities.Role;
import Entities.User;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class ChatbotProjectContextService {

    private final ProjetCRUD projetCrud;
    private final InvestissementCRUD investissementCrud;
    private final Financement2CRUD financementCrud;
    private final InvitationService invitationService;
    private final EvenementService evenementService;
    private final RemboursementCRUD remboursementCrud;
    private final ProfilInvestisseurCRUD profilInvestisseurCrud;
    private final ProfilEntrepreneurCRUD profilEntrepreneurCrud;

    public ChatbotProjectContextService() {
        this.projetCrud = new ProjetCRUD();
        this.investissementCrud = new InvestissementCRUD();
        this.financementCrud = new Financement2CRUD();
        this.invitationService = new InvitationService();
        this.evenementService = new EvenementService();
        this.remboursementCrud = new RemboursementCRUD();
        this.profilInvestisseurCrud = new ProfilInvestisseurCRUD();
        this.profilEntrepreneurCrud = new ProfilEntrepreneurCRUD();
    }

    public String buildContext(User currentUser) {
        if (currentUser == null) {
            return "Aucun utilisateur connecté.";
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("Contexte utilisateur connecte:\n");
        ctx.append("- id_user: ").append(currentUser.getId()).append("\n");
        ctx.append("- nom: ").append(safe(currentUser.getPrenom())).append(" ").append(safe(currentUser.getNom())).append("\n");
        ctx.append("- role: ").append(currentUser.getRole() == null ? "INCONNU" : currentUser.getRole().name()).append("\n");
        ctx.append("- verification: ").append(currentUser.getStatutVerification() == null ? "INCONNU" : currentUser.getStatutVerification().name()).append("\n");

        try {
            Role role = currentUser.getRole();
            if (role == Role.ENTREPRENEUR) {
                appendEntrepreneurContext(ctx, currentUser.getId());
            } else if (role == Role.INVESTISSEUR) {
                appendInvestisseurContext(ctx, currentUser.getId());
            } else {
                appendAdminContext(ctx);
            }
        } catch (Exception e) {
            ctx.append("\n[Note] Impossible de charger tout le contexte DB: ").append(e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
        return ctx.toString();
    }

    public String tryQuickAnswer(User currentUser, String message) {
        if (currentUser == null || message == null) return null;
        String q = message.toLowerCase(Locale.ROOT).trim();
        try {
            if (q.contains("budget total")) {
                ProfilInvestisseur p = profilInvestisseurCrud.getByUserId(currentUser.getId());
                if (p == null || p.getBudgetTotal() == null) return "Votre budget total n'est pas renseigne dans votre profil.";
                return "Votre budget total est de " + p.getBudgetTotal() + ".";
            }
            if (q.contains("budget mensuel")) {
                ProfilInvestisseur p = profilInvestisseurCrud.getByUserId(currentUser.getId());
                if (p == null || p.getBudgetMensuel() == null) return "Votre budget mensuel n'est pas renseigne dans votre profil.";
                return "Votre budget mensuel est de " + p.getBudgetMensuel() + ".";
            }
            if (q.contains("projet") && (q.contains("publ") || q.contains("publie"))) {
                List<Projet> projects = projetCrud.afficher();
                int published = 0;
                for (Projet p : projects) {
                    String st = p.getStatut() == null ? "" : p.getStatut().toUpperCase(Locale.ROOT);
                    if ("VALIDE".equals(st)
                            || "EN_COURS".equals(st)
                            || "INVESTISSEMENT_EN_COURS".equals(st)
                            || "INVESTISSEMENT_TERMINE".equals(st)
                            || "PROJET_EN_COURS".equals(st)) published++;
                }
                return "Il y a " + published + " projets publies sur la plateforme.";
            }

            if (currentUser.getRole() == Role.INVESTISSEUR) {
                int idInvestisseur = profilInvestisseurCrud.getIdInvestisseurByUserId(currentUser.getId());
                if (idInvestisseur <= 0) return "Profil investisseur introuvable.";

                if ((q.contains("liste") || q.contains("projets") || q.contains("projet"))
                        && (q.contains("investisseur") || q.contains("disponible") || q.contains("statut"))) {
                    List<Projet> projects = projetCrud.afficher();
                    List<String> lines = new ArrayList<>();
                    int shown = 0;
                    for (Projet p : projects) {
                        String st = p.getStatut() == null ? "" : p.getStatut().toUpperCase(Locale.ROOT);
                        if (!"VALIDE".equals(st)
                                && !"EN_COURS".equals(st)
                                && !"INVESTISSEMENT_EN_COURS".equals(st)
                                && !"PROJET_EN_COURS".equals(st)) continue;
                        lines.add("- #" + p.getIdProjet() + " | " + safe(p.getTitre()) + " | statut=" + safe(p.getStatut()));
                        shown++;
                        if (shown >= 10) break;
                    }
                    if (lines.isEmpty()) return "Aucun projet visible actuellement pour la vue investisseur.";
                    return "Projets visibles (max 10):\n" + String.join("\n", lines);
                }

                if (q.contains("investissement")) {
                    List<Investissement> investments = investissementCrud.afficherParInvestisseur(idInvestisseur);
                    if (investments == null || investments.isEmpty()) {
                        return "Vous n'avez encore aucun investissement enregistre.";
                    }
                    double total = 0.0;
                    List<String> lines = new ArrayList<>();
                    int shown = 0;
                    for (Investissement inv : investments) {
                        total += inv.getMontant();
                        lines.add("- #" + inv.getId_investissement()
                                + " | projet=" + inv.getId_projet()
                                + " | montant=" + String.format(Locale.ROOT, "%.2f", inv.getMontant())
                                + " | date=" + (inv.getDate_investissement() == null ? "" : inv.getDate_investissement()));
                        shown++;
                        if (shown >= 10) break;
                    }
                    return "Vous avez " + investments.size() + " investissement(s), total="
                            + String.format(Locale.ROOT, "%.2f", total) + ".\n"
                            + "Derniers investissements (max 10):\n" + String.join("\n", lines);
                }

                if (q.contains("financement")) {
                    List<Investissement> investments = investissementCrud.afficherParInvestisseur(idInvestisseur);
                    Set<Integer> investedIds = new HashSet<>();
                    for (Investissement inv : investments) investedIds.add(inv.getId_investissement());

                    List<Financement2> allFinancements = financementCrud.afficher();
                    List<Financement2> mine = new ArrayList<>();
                    for (Financement2 f : allFinancements) {
                        if (investedIds.contains(f.getId_investissement())) mine.add(f);
                    }
                    if (mine.isEmpty()) return "Aucun financement associe a vos investissements pour le moment.";

                    int confirmed = 0;
                    double total = 0.0;
                    List<String> lines = new ArrayList<>();
                    int shown = 0;
                    for (Financement2 f : mine) {
                        String st = f.getStatut() == null ? "" : f.getStatut().toUpperCase(Locale.ROOT);
                        if ("CONFIRMED".equals(st)) confirmed++;
                        total += f.getMontant();
                        lines.add("- #" + f.getId_financement()
                                + " | projet=" + f.getId_projet()
                                + " | montant=" + String.format(Locale.ROOT, "%.2f", f.getMontant())
                                + " | statut=" + safe(f.getStatut()));
                        shown++;
                        if (shown >= 10) break;
                    }
                    return "Financements associes: " + mine.size()
                            + " (confirmes: " + confirmed + "), total="
                            + String.format(Locale.ROOT, "%.2f", total) + ".\n"
                            + "Derniers financements (max 10):\n" + String.join("\n", lines);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void appendEntrepreneurContext(StringBuilder ctx, int userId) throws Exception {
        ProfilEntrepreneur profil = profilEntrepreneurCrud.getByUserId(userId);
        if (profil == null) {
            ctx.append("\nContexte metier entrepreneur:\n");
            ctx.append("- profil_entrepreneur: introuvable pour cet utilisateur\n");
            return;
        }
        int entrepreneurId = profil.getIdEntrepreneur();

        List<Projet> allProjects = projetCrud.afficher();
        List<Projet> mine = new ArrayList<>();
        for (Projet p : allProjects) {
            if (p.getEntrepreneurId() == entrepreneurId) mine.add(p);
        }

        int total = mine.size();
        int valides = 0;
        int enCours = 0;
        int attente = 0;
        int brouillon = 0;
        int refuses = 0;

        for (Projet p : mine) {
            String statut = p.getStatut() == null ? "" : p.getStatut().toUpperCase(Locale.ROOT);
            if ("VALIDE".equals(statut)) valides++;
            else if ("EN_COURS".equals(statut)
                    || "INVESTISSEMENT_EN_COURS".equals(statut)
                    || "INVESTISSEMENT_TERMINE".equals(statut)
                    || "PROJET_EN_COURS".equals(statut)) enCours++;
            else if ("EN_ATTENTE".equals(statut)) attente++;
            else if ("BROUILLON".equals(statut)) brouillon++;
            else if ("REFUSE".equals(statut)) refuses++;
        }

        ctx.append("\nContexte metier entrepreneur:\n");
        ctx.append("- id_entrepreneur: ").append(entrepreneurId).append("\n");
        ctx.append("- projets_total: ").append(total).append("\n");
        ctx.append("- projets_valides: ").append(valides).append("\n");
        ctx.append("- projets_en_cours: ").append(enCours).append("\n");
        ctx.append("- projets_publies: ").append(valides + enCours).append("\n");
        ctx.append("- projets_en_attente: ").append(attente).append("\n");
        ctx.append("- projets_brouillon: ").append(brouillon).append("\n");
        ctx.append("- projets_refuses: ").append(refuses).append("\n");

        int limit = Math.min(5, mine.size());
        if (limit > 0) {
            ctx.append("- derniers_projets:\n");
            for (int i = 0; i < limit; i++) {
                Projet p = mine.get(i);
                ctx.append("  * #").append(p.getIdProjet())
                        .append(" | ").append(safe(p.getTitre()))
                        .append(" | statut=").append(safe(p.getStatut()))
                        .append(" | objectif=").append(p.getObjectifTnd() == null ? 0 : p.getObjectifTnd())
                        .append("\n");
            }
        }

        Set<Integer> myProjectIds = new HashSet<>();
        for (Projet p : mine) {
            myProjectIds.add(p.getIdProjet());
        }

        List<Evenement> allEvents = evenementService.getAll();
        List<Evenement> myEvents = new ArrayList<>();
        Map<Integer, String> projetTitreById = new HashMap<>();
        for (Projet p : mine) {
            projetTitreById.put(p.getIdProjet(), safe(p.getTitre()));
        }
        for (Evenement ev : allEvents) {
            if (myProjectIds.contains(ev.getProjectId())) myEvents.add(ev);
        }
        myEvents.sort(Comparator.comparing(Evenement::getDateDebut, Comparator.nullsLast(Comparator.reverseOrder())));

        Set<Integer> myEventIds = new HashSet<>();
        for (Evenement ev : myEvents) {
            myEventIds.add(ev.getId());
        }

        List<Invitation> allInvitations = invitationService.getAll();
        List<Invitation> myInvitations = new ArrayList<>();
        int invitationsInvestisseur = 0;
        int invitationsEntrepreneur = 0;
        for (Invitation inv : allInvitations) {
            if (!myEventIds.contains(inv.getEvenementId())) continue;
            myInvitations.add(inv);
            String roleInvite = safe(inv.getRoleInvite()).toUpperCase(Locale.ROOT);
            if (roleInvite.contains("INVEST")) invitationsInvestisseur++;
            if (roleInvite.contains("ENTREPREN")) invitationsEntrepreneur++;
        }
        myInvitations.sort(Comparator.comparing(Invitation::getDateInvitation, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Financement2> allFinancements = financementCrud.afficher();
        Set<Integer> myFinancementIds = new HashSet<>();
        for (Financement2 f : allFinancements) {
            if (myProjectIds.contains(f.getId_projet())) {
                myFinancementIds.add(f.getId_financement());
            }
        }

        List<Remboursement> allRemboursements = remboursementCrud.afficher();
        List<Remboursement> myRemboursements = new ArrayList<>();
        int rembPayes = 0;
        int rembEnAttente = 0;
        double rembMontantDu = 0.0;
        double rembMontantPaye = 0.0;
        for (Remboursement r : allRemboursements) {
            if (!myFinancementIds.contains(r.getFinancementId())) continue;
            myRemboursements.add(r);
            rembMontantDu += r.getMontantDu();
            rembMontantPaye += r.getMontantPaye();
            String st = safe(r.getStatut()).toUpperCase(Locale.ROOT);
            if ("PAYE".equals(st)) rembPayes++;
            else rembEnAttente++;
        }
        myRemboursements.sort(Comparator.comparing(Remboursement::getDateEcheance, Comparator.nullsLast(Comparator.reverseOrder())));

        ctx.append("- evenements_total: ").append(myEvents.size()).append("\n");
        ctx.append("- invitations_total: ").append(myInvitations.size()).append("\n");
        ctx.append("- invitations_investisseurs: ").append(invitationsInvestisseur).append("\n");
        ctx.append("- invitations_entrepreneurs: ").append(invitationsEntrepreneur).append("\n");
        ctx.append("- remboursements_total: ").append(myRemboursements.size()).append("\n");
        ctx.append("- remboursements_payes: ").append(rembPayes).append("\n");
        ctx.append("- remboursements_en_attente: ").append(rembEnAttente).append("\n");
        ctx.append("- remboursements_montant_du_total: ").append(String.format(Locale.ROOT, "%.2f", rembMontantDu)).append("\n");
        ctx.append("- remboursements_montant_paye_total: ").append(String.format(Locale.ROOT, "%.2f", rembMontantPaye)).append("\n");

        int eventLimit = Math.min(5, myEvents.size());
        if (eventLimit > 0) {
            ctx.append("- derniers_evenements:\n");
            for (int i = 0; i < eventLimit; i++) {
                Evenement ev = myEvents.get(i);
                String projectTitle = projetTitreById.getOrDefault(ev.getProjectId(), "Projet #" + ev.getProjectId());
                ctx.append("  * #").append(ev.getId())
                        .append(" | ").append(safe(ev.getTitre()))
                        .append(" | projet=").append(projectTitle)
                        .append(" | debut=").append(ev.getDateDebut() == null ? "" : ev.getDateDebut())
                        .append("\n");
            }
        }

        int invitationLimit = Math.min(5, myInvitations.size());
        if (invitationLimit > 0) {
            ctx.append("- dernieres_invitations:\n");
            for (int i = 0; i < invitationLimit; i++) {
                Invitation inv = myInvitations.get(i);
                ctx.append("  * #").append(inv.getId())
                        .append(" | evenement=").append(inv.getEvenementId())
                        .append(" | role=").append(safe(inv.getRoleInvite()))
                        .append(" | email=").append(safe(inv.getEmail()))
                        .append(" | date=").append(inv.getDateInvitation() == null ? "" : inv.getDateInvitation())
                        .append("\n");
            }
        }

        int remboursementLimit = Math.min(5, myRemboursements.size());
        if (remboursementLimit > 0) {
            ctx.append("- derniers_remboursements:\n");
            for (int i = 0; i < remboursementLimit; i++) {
                Remboursement r = myRemboursements.get(i);
                ctx.append("  * #").append(r.getId())
                        .append(" | financement=").append(r.getFinancementId())
                        .append(" | echeance=").append(r.getDateEcheance() == null ? "" : r.getDateEcheance())
                        .append(" | du=").append(String.format(Locale.ROOT, "%.2f", r.getMontantDu()))
                        .append(" | paye=").append(String.format(Locale.ROOT, "%.2f", r.getMontantPaye()))
                        .append(" | statut=").append(safe(r.getStatut()))
                        .append("\n");
            }
        }
    }

    private void appendInvestisseurContext(StringBuilder ctx, int userId) throws Exception {
        ProfilInvestisseur profil = profilInvestisseurCrud.getByUserId(userId);
        if (profil == null) {
            ctx.append("\nContexte metier investisseur:\n");
            ctx.append("- profil_investisseur: introuvable\n");
            return;
        }

        int idInvestisseur;
        try {
            idInvestisseur = profilInvestisseurCrud.getIdInvestisseurByUserId(userId);
        } catch (Exception e) {
            ctx.append("\nContexte metier investisseur:\n");
            ctx.append("- profil_investisseur: introuvable\n");
            return;
        }

        List<Investissement> investments = investissementCrud.afficherParInvestisseur(idInvestisseur);
        double totalInvesti = 0.0;
        Set<Integer> investedProjectIds = new HashSet<>();
        Set<Integer> investedIds = new HashSet<>();
        for (Investissement inv : investments) {
            totalInvesti += inv.getMontant();
            investedProjectIds.add(inv.getId_projet());
            investedIds.add(inv.getId_investissement());
        }

        List<Financement2> financements = financementCrud.afficher();
        List<Projet> projects = projetCrud.afficher();
        int finCount = 0;
        int finConfirmed = 0;
        for (Financement2 f : financements) {
            if (!investedIds.contains(f.getId_investissement())) continue;
            finCount++;
            String st = f.getStatut() == null ? "" : f.getStatut().toUpperCase(Locale.ROOT);
            if ("CONFIRMED".equals(st)) finConfirmed++;
        }
        int publishedPlatform = 0;
        int validatedPlatform = 0;
        int enCoursPlatform = 0;
        for (Projet p : projects) {
            String st = p.getStatut() == null ? "" : p.getStatut().toUpperCase(Locale.ROOT);
            if ("VALIDE".equals(st)) {
                validatedPlatform++;
                publishedPlatform++;
            } else if ("EN_COURS".equals(st)
                    || "INVESTISSEMENT_EN_COURS".equals(st)
                    || "INVESTISSEMENT_TERMINE".equals(st)
                    || "PROJET_EN_COURS".equals(st)) {
                enCoursPlatform++;
                publishedPlatform++;
            }
        }

        ctx.append("\nContexte metier investisseur:\n");
        ctx.append("- id_investisseur: ").append(idInvestisseur).append("\n");
        ctx.append("- budget_total: ").append(profil.getBudgetTotal() == null ? "null" : profil.getBudgetTotal()).append("\n");
        ctx.append("- budget_mensuel: ").append(profil.getBudgetMensuel() == null ? "null" : profil.getBudgetMensuel()).append("\n");
        ctx.append("- ticket_moyen_par_projet: ").append(profil.getTicketMoyenParProjet() == null ? "null" : profil.getTicketMoyenParProjet()).append("\n");
        ctx.append("- horizon_investissement: ").append(safe(profil.getHorizonInvestissement())).append("\n");
        ctx.append("- projets_publies_plateforme: ").append(publishedPlatform).append("\n");
        ctx.append("- projets_valides_plateforme: ").append(validatedPlatform).append("\n");
        ctx.append("- projets_en_cours_plateforme: ").append(enCoursPlatform).append("\n");
        ctx.append("- investissements_total: ").append(investments.size()).append("\n");
        ctx.append("- montant_total_investi: ").append(String.format(Locale.ROOT, "%.2f", totalInvesti)).append("\n");
        ctx.append("- projets_distincts_investis: ").append(investedProjectIds.size()).append("\n");
        ctx.append("- financements_associes: ").append(finCount).append("\n");
        ctx.append("- financements_confirmes: ").append(finConfirmed).append("\n");

        int limit = Math.min(5, investments.size());
        if (limit > 0) {
            ctx.append("- derniers_investissements:\n");
            for (int i = 0; i < limit; i++) {
                Investissement inv = investments.get(i);
                ctx.append("  * #").append(inv.getId_investissement())
                        .append(" | projet=").append(inv.getId_projet())
                        .append(" | montant=").append(String.format(Locale.ROOT, "%.2f", inv.getMontant()))
                        .append(" | date=").append(inv.getDate_investissement() == null ? "" : inv.getDate_investissement())
                        .append("\n");
            }
        }
    }

    private void appendAdminContext(StringBuilder ctx) throws Exception {
        List<Projet> projects = projetCrud.afficher();
        List<Financement2> financements = financementCrud.afficher();
        ctx.append("\nContexte metier admin:\n");
        ctx.append("- projets_total_plateforme: ").append(projects.size()).append("\n");
        ctx.append("- financements_total_plateforme: ").append(financements.size()).append("\n");
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
