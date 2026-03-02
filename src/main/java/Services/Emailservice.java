package Services;

import Entities.Evenement;
import Entities.Invitation;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Service d'envoi d'email automatique lors d'une invitation.
 * Utilise Gmail SMTP (TLS port 587).
 *
 * ⚠️ CONFIGURATION REQUISE :
 *   1. Remplacez SENDER_EMAIL et SENDER_PASSWORD par vos identifiants Gmail.
 *   2. Activez "Mots de passe d'application" dans votre compte Google :
 *      → https://myaccount.google.com/apppasswords
 *      (Sécurité → Connexion → Mots de passe des applications)
 *   3. Ajoutez la dépendance JavaMail dans pom.xml :
 *      <dependency>
 *          <groupId>com.sun.mail</groupId>
 *          <artifactId>jakarta.mail</artifactId>
 *          <version>2.0.1</version>
 *      </dependency>
 */
public class Emailservice {

    // ══════════════════════════════════════════════
    //  ✅ Credentials lus depuis config.properties
    //     (fichier NON partagé — dans .gitignore)
    // ══════════════════════════════════════════════
    private static final String SENDER_EMAIL;
    private static final String SENDER_PASSWORD;
    private static final String SENDER_NAME = "Investia — Événements";

    static {
        java.util.Properties config = new java.util.Properties();
        try (java.io.InputStream in =
                     Emailservice.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                config.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("✅ config.properties chargé");
            } else {
                System.err.println("⚠️ config.properties introuvable dans resources/");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lecture config.properties : " + e.getMessage());
        }
        SENDER_EMAIL    = config.getProperty("mail.sender.email",    "");
        SENDER_PASSWORD = config.getProperty("mail.sender.password", "");
    }

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    // ══════════════════════════════════════════════
    //  ENVOYER EMAIL D'INVITATION
    // ══════════════════════════════════════════════
    public static void envoyerInvitation(Invitation inv, Evenement ev) {
        // Envoi dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                // ✅ Forcer UTF-8 pour tous les caractères spéciaux
                System.setProperty("mail.mime.charset",       "UTF-8");
                System.setProperty("mail.mime.encodefilename","true");

                Properties props = new Properties();
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");
                props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");
                props.put("mail.mime.charset",         "UTF-8");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME, "UTF-8"));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(inv.getEmail()));
                message.setSubject(javax.mail.internet.MimeUtility.encodeText("✉️ Vous êtes invité(e) à : " + ev.getTitre(), "UTF-8", "B"));
                message.setContent(buildHtmlBody(inv, ev), "text/html; charset=UTF-8");

                Transport.send(message);
                System.out.println("✅ Email envoyé à : " + inv.getEmail());

            } catch (Exception e) {
                // L'échec de l'email ne doit pas bloquer l'ajout de l'invitation
                System.err.println("⚠️ Échec envoi email à " + inv.getEmail() + " : " + e.getMessage());
            }
        }).start();
    }

    // ══════════════════════════════════════════════
    //  CORPS DE L'EMAIL (HTML)
    // ══════════════════════════════════════════════
    private static String buildHtmlBody(Invitation inv, Evenement ev) {
        boolean isOnline  = "EN_LIGNE".equals(ev.getMode() == null ? "" : ev.getMode().toString());
        String  modeLabel = isOnline ? "🌐 En ligne" : "🏢 Présentiel";
        String  lieuOrUrl = isOnline
                ? (ev.getMeetingLink() != null ? ev.getMeetingLink() : "—")
                : (ev.getLieu()        != null ? ev.getLieu()        : "—");
        String lieuLabel  = isOnline ? "Lien de réunion" : "Lieu";

        String dateDebut = ev.getDateDebut() != null ? ev.getDateDebut().format(DISPLAY_FMT) : "—";
        String dateFin   = ev.getDateFin()   != null ? ev.getDateFin().format(DISPLAY_FMT)   : "—";
        String dateInv   = inv.getDateInvitation() != null ? inv.getDateInvitation().format(DISPLAY_FMT) : "—";
        String role      = (inv.getRoleInvite() != null && !inv.getRoleInvite().isEmpty())
                ? inv.getRoleInvite() : "Invité(e)";

        String lienHtml = isOnline && ev.getMeetingLink() != null
                ? "<a href=\"" + ev.getMeetingLink() + "\" style=\"color:#1d4ed8;font-weight:700\">"
                + ev.getMeetingLink() + "</a>"
                : lieuOrUrl;

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'/></head><body style='"
                + "margin:0;padding:0;background:#f6f8fc;font-family:ui-sans-serif,system-ui,Arial,sans-serif'>"

                // Wrapper
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f6f8fc;padding:32px 16px'>"
                + "<tr><td align='center'>"
                + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>"

                // Header
                + "<tr><td style='background:linear-gradient(135deg,#1d4ed8,#06b6d4);border-radius:18px 18px 0 0;"
                + "padding:32px 32px 28px;text-align:center'>"
                + "<div style='font-size:36px;margin-bottom:10px'>📅</div>"
                + "<h1 style='margin:0;color:#fff;font-size:22px;font-weight:900'>Vous êtes invité(e) !</h1>"
                + "<p style='margin:8px 0 0;color:rgba(255,255,255,.80);font-size:14px'>"
                + "Invitation à l'événement <strong>" + escape(ev.getTitre()) + "</strong></p>"
                + "</td></tr>"

                // Corps
                + "<tr><td style='background:#fff;padding:28px 32px'>"

                // Bonjour
                + "<p style='margin:0 0 20px;font-size:15px;color:#0f172a'>"
                + "Bonjour,</p>"
                + "<p style='margin:0 0 24px;font-size:14px;color:#475569;line-height:1.6'>"
                + "Vous avez été invité(e) en tant que <strong>" + escape(role) + "</strong> "
                + "à l'événement suivant. Voici toutes les informations :</p>"

                // Carte événement
                + "<table width='100%' cellpadding='0' cellspacing='0' style='"
                + "background:#f8fafc;border:1px solid rgba(15,23,42,.10);border-radius:14px;"
                + "margin-bottom:24px'>"

                + infoRow("📌", "Événement",    escape(ev.getTitre()))
                + infoRow("🏷",  "Votre rôle",  escape(role))
                + infoRow("📡",  "Mode",         modeLabel)
                + infoRow("📅",  "Début",         dateDebut)
                + infoRow("🏁",  "Fin",           dateFin)
                + infoRow(isOnline ? "🔗" : "📍", lieuLabel, lienHtml)
                + infoRow("🗓",  "Date d'invitation", dateInv)

                + "</table>"

                // CTA si lien
                + (isOnline && ev.getMeetingLink() != null && !ev.getMeetingLink().isEmpty()
                ? "<div style='text-align:center;margin-bottom:24px'>"
                + "<a href='" + ev.getMeetingLink() + "' style='"
                + "display:inline-block;background:linear-gradient(135deg,#1d4ed8,#06b6d4);"
                + "color:#fff;text-decoration:none;padding:13px 28px;border-radius:12px;"
                + "font-weight:800;font-size:14px;box-shadow:0 8px 20px rgba(29,78,216,.25)'>"
                + "🔗 Rejoindre la réunion</a></div>"
                : "")

                + "<p style='margin:0;font-size:13px;color:#94a3b8;line-height:1.5'>"
                + "Si vous avez des questions, contactez l'organisateur de l'événement.</p>"

                + "</td></tr>"

                // Footer
                + "<tr><td style='background:#0f172a;border-radius:0 0 18px 18px;padding:18px 32px;text-align:center'>"
                + "<p style='margin:0;color:rgba(255,255,255,.50);font-size:12px'>"
                + "© Investia — Plateforme de gestion d'événements<br/>"
                + "Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>"
                + "</td></tr>"

                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private static String infoRow(String ico, String label, String value) {
        return "<tr>"
                + "<td style='padding:11px 16px;border-bottom:1px solid rgba(15,23,42,.06)'>"
                + "<span style='font-size:15px'>" + ico + "</span> "
                + "<span style='font-size:12px;font-weight:900;color:#64748b;text-transform:uppercase;"
                + "letter-spacing:.4px'>" + label + "</span>"
                + "</td>"
                + "<td style='padding:11px 16px;border-bottom:1px solid rgba(15,23,42,.06);"
                + "font-size:14px;font-weight:700;color:#0f172a'>" + value + "</td>"
                + "</tr>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
