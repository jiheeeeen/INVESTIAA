package Services;

import Entities.Evenement;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service d'export PDF pour la liste des événements.
 * Utilise OpenPDF (fork libre de iText 4 — licence LGPL).
 *
 * ✅ Dépendance à ajouter dans pom.xml :
 *   <dependency>
 *       <groupId>com.github.librepdf</groupId>
 *       <artifactId>openpdf</artifactId>
 *       <version>1.3.30</version>
 *   </dependency>
 */
public class PdfExportService {

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ══ Couleurs ══
    private static final Color NAVY      = new Color(11,  42,  85);
    private static final Color PRIMARY   = new Color(29,  78,  216);
    private static final Color ACCENT    = new Color(6,   182, 212);
    private static final Color SUCCESS   = new Color(22,  163, 74);
    private static final Color WARNING   = new Color(217, 119, 6);
    private static final Color MUTED     = new Color(100, 116, 139);
    private static final Color LIGHT_BG  = new Color(248, 250, 252);
    private static final Color BORDER    = new Color(226, 232, 240);
    private static final Color WHITE     = Color.WHITE;

    // ══════════════════════════════════════════════
    //  GÉNÉRER LE PDF
    // ══════════════════════════════════════════════
    public static void generer(List<Evenement> evenements, String cheminFichier) throws Exception {

        Document doc = new Document(PageSize.A4, 40, 40, 60, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(cheminFichier));

        // En-tête et pied de page personnalisés
        writer.setPageEvent(new HeaderFooter());

        doc.open();

        // ══ TITRE PRINCIPAL ══
        addTitle(doc, evenements.size());

        // ══ STATS RAPIDES ══
        addStats(doc, evenements);

        doc.add(Chunk.NEWLINE);

        // ══ TABLE DES ÉVÉNEMENTS ══
        if (evenements.isEmpty()) {
            addEmptyMessage(doc);
        } else {
            addTable(doc, evenements);
        }

        doc.close();
    }

    // ══════════════════════════════════════════════
    //  TITRE
    // ══════════════════════════════════════════════
    private static void addTitle(Document doc, int count) throws Exception {

        // Bande bleue de titre
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(20f);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(NAVY);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(22f);

        Paragraph title = new Paragraph();
        title.add(new Chunk("📅  Rapport des Événements\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, WHITE)));
        title.add(new Chunk("Investia — Plateforme de crowdlending   •   " + count + " événement(s)",
                FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(148, 163, 184))));

        titleCell.addElement(title);
        headerTable.addCell(titleCell);
        doc.add(headerTable);
    }

    // ══════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════
    private static void addStats(Document doc, List<Evenement> evs) throws Exception {
        long online     = evs.stream().filter(e -> "EN_LIGNE".equals(modeOf(e))).count();
        long presentiel = evs.stream().filter(e -> "PRESENTIEL".equals(modeOf(e))).count();

        PdfPTable stats = new PdfPTable(3);
        stats.setWidthPercentage(100);
        stats.setWidths(new float[]{1f, 1f, 1f});
        stats.setSpacingAfter(10f);

        stats.addCell(statCell("📋 Total",        String.valueOf(evs.size()), PRIMARY));
        stats.addCell(statCell("🌐 En ligne",      String.valueOf(online),     ACCENT));
        stats.addCell(statCell("🏢 Présentiel",    String.valueOf(presentiel), WARNING));

        doc.add(stats);
    }

    private static PdfPCell statCell(String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(1f);
        cell.setBackgroundColor(LIGHT_BG);
        cell.setPadding(14f);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n",
                FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED)));
        p.add(new Chunk(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, color)));
        cell.addElement(p);
        return cell;
    }

    // ══════════════════════════════════════════════
    //  TABLE DES ÉVÉNEMENTS
    // ══════════════════════════════════════════════
    private static void addTable(Document doc, List<Evenement> evs) throws Exception {

        // En-tête de section
        Paragraph sectionTitle = new Paragraph("Liste des événements",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, NAVY));
        sectionTitle.setSpacingBefore(8f);
        sectionTitle.setSpacingAfter(10f);
        doc.add(sectionTitle);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 2.5f, 1.2f, 1.5f, 1.5f, 1.8f});
        table.setSpacingBefore(4f);
        table.setHeaderRows(1);

        // ── En-têtes colonnes ──
        String[] headers = {"#", "Titre", "Mode", "Date début", "Date fin", "Lieu / Lien"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, WHITE)));
            hCell.setBackgroundColor(PRIMARY);
            hCell.setBorder(Rectangle.NO_BORDER);
            hCell.setPadding(9f);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        // ── Lignes ──
        for (int i = 0; i < evs.size(); i++) {
            Evenement ev = evs.get(i);
            boolean isAlt = (i % 2 == 1);
            Color rowBg = isAlt ? LIGHT_BG : WHITE;
            boolean isOnline = "EN_LIGNE".equals(modeOf(ev));

            table.addCell(bodyCell(String.valueOf(i + 1), rowBg, Element.ALIGN_CENTER, MUTED));
            table.addCell(bodyCell(safe(ev.getTitre()), rowBg, Element.ALIGN_LEFT, NAVY));
            table.addCell(modeCell(isOnline, rowBg));
            table.addCell(bodyCell(fmt(ev.getDateDebut()), rowBg, Element.ALIGN_CENTER, MUTED));
            table.addCell(bodyCell(fmt(ev.getDateFin()),   rowBg, Element.ALIGN_CENTER, MUTED));
            table.addCell(bodyCell(
                    isOnline ? safe(ev.getMeetingLink()) : safe(ev.getLieu()),
                    rowBg, Element.ALIGN_LEFT, isOnline ? PRIMARY : SUCCESS
            ));
        }

        doc.add(table);
    }

    private static PdfPCell bodyCell(String text, Color bg, int align, Color textColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 9, textColor)));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell modeCell(boolean isOnline, Color bg) {
        String label   = isOnline ? "En ligne" : "Présentiel";
        Color  tagBg   = isOnline ? new Color(219, 234, 254) : new Color(254, 243, 199);
        Color  tagText = isOnline ? PRIMARY : WARNING;

        PdfPCell wrapper = new PdfPCell();
        wrapper.setBackgroundColor(bg);
        wrapper.setBorderColor(BORDER);
        wrapper.setBorderWidth(0.5f);
        wrapper.setPadding(6f);
        wrapper.setHorizontalAlignment(Element.ALIGN_CENTER);
        wrapper.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPTable tag = new PdfPTable(1);
        tag.setWidthPercentage(90);
        PdfPCell tagCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, tagText)));
        tagCell.setBackgroundColor(tagBg);
        tagCell.setBorder(Rectangle.NO_BORDER);
        tagCell.setPadding(4f);
        tagCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tag.addCell(tagCell);

        wrapper.addElement(tag);
        return wrapper;
    }

    private static void addEmptyMessage(Document doc) throws Exception {
        Paragraph empty = new Paragraph("Aucun événement à afficher.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, MUTED));
        empty.setAlignment(Element.ALIGN_CENTER);
        empty.setSpacingBefore(40f);
        doc.add(empty);
    }

    // ══════════════════════════════════════════════
    //  EN-TÊTE ET PIED DE PAGE
    // ══════════════════════════════════════════════
    static class HeaderFooter extends PdfPageEventHelper {
        private static final DateTimeFormatter DATE_FMT =
                DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();

            // Ligne de pied de page
            cb.setColorStroke(BORDER);
            cb.setLineWidth(0.5f);
            cb.moveTo(doc.left(), doc.bottom() - 10);
            cb.lineTo(doc.right(), doc.bottom() - 10);
            cb.stroke();

            // Texte pied de page
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Investia — Rapport généré le " +
                            java.time.LocalDateTime.now().format(DATE_FMT), footerFont),
                    doc.left(), doc.bottom() - 22, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), footerFont),
                    doc.right(), doc.bottom() - 22, 0);
        }
    }

    // ══════════════════════════════════════════════
    //  UTILS
    // ══════════════════════════════════════════════
    private static String safe(String s)  { return s == null || s.isEmpty() ? "—" : s; }
    private static String modeOf(Evenement ev) {
        if (ev == null || ev.getMode() == null) return "";
        return ev.getMode().toString();
    }
    private static String fmt(java.time.LocalDateTime ldt) {
        return ldt == null ? "—" : ldt.format(DISPLAY);
    }
}
