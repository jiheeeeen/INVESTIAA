package Services;
import Entities.StatutVerification;
import Entities.User;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

public class EmailServiceBrevoSMTP {

    // ✅ Brevo SMTP (fixe)
    private static final String SMTP_HOST = "smtp-relay.brevo.com";
    private static final int SMTP_PORT = 587;

    // ✅ Mets tes valeurs ici OU mieux: variables d’environnement (recommandé)
    // BREVO_SMTP_LOGIN=xxxx@smtp-brevo.com
    // BREVO_SMTP_KEY=xxxxxxxx
    // BREVO_FROM_EMAIL=ton-email-verifie
    // BREVO_FROM_NAME=Investia
    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    private static final String SMTP_LOGIN = env("BREVO_SMTP_LOGIN", "a394d1001@smtp-brevo.com");
    private static final String SMTP_KEY   = env("BREVO_SMTP_KEY",   "YOUR_BREVO_SMTP_KEY");
    private static final String FROM_EMAIL = env("BREVO_FROM_EMAIL", "aloui.amine105@gmail.com"); // idéal : domaine vérifié
    private static final String FROM_NAME  = env("BREVO_FROM_NAME",  "investia");

    // =========================
    // Public API
    // =========================
    public static void sendVerificationDecisionAsync(User u, StatutVerification newStatus) {
        if (u == null) return;
        if (u.getEmail() == null || u.getEmail().isBlank()) return;

        new Thread(() -> {
            try {
                sendVerificationDecision(u, newStatus);
            } catch (Exception e) {
                // On ne bloque jamais le flow admin
                e.printStackTrace();
            }
        }, "brevo-email-thread").start();
    }

    // =========================
    // Core send (TEXT + HTML)
    // =========================
    private static void sendVerificationDecision(User u, StatutVerification newStatus) throws Exception {

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_LOGIN, SMTP_KEY);
            }
        });

        String fullName = buildFullName(u);

        EmailContent content = buildVerificationEmail(fullName, newStatus);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME, StandardCharsets.UTF_8.name()));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(u.getEmail()));
        msg.setSubject(content.subject, StandardCharsets.UTF_8.name());
        msg.setSentDate(new Date());

        // ✅ multipart alternative (meilleure délivrabilité)
        MimeMultipart multipart = new MimeMultipart("alternative");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(content.text, StandardCharsets.UTF_8.name());

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(content.html, "text/html; charset=UTF-8");

        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);

        msg.setContent(multipart);

        Transport.send(msg);
    }

    // =========================
    // Templates
    // =========================
    private static EmailContent buildVerificationEmail(String fullName, StatutVerification status) {

        String badgeBg, badgeText, title, intro, details, nextStep;

        switch (status) {
            case VERIFIE -> {
                title = "Compte vérifié";
                intro = "Bonne nouvelle : votre compte a été vérifié par l’administrateur.";
                details = "Vous pouvez désormais accéder à votre espace Investia et utiliser toutes les fonctionnalités.";
                nextStep = "Si vous rencontrez un problème de connexion, réessayez ou contactez-nous.";
                badgeBg = "#DCFCE7";
                badgeText = "#166534";
            }
            case REFUSE -> {
                title = "Vérification refusée";
                intro = "Votre demande de vérification a été refusée.";
                details = "Vous pouvez corriger vos informations et soumettre une nouvelle demande.";
                nextStep = "Si vous pensez qu’il s’agit d’une erreur, contactez le support.";
                badgeBg = "#FEE2E2";
                badgeText = "#991B1B";
            }
            case NON_VERIFIE -> {
                title = "Compte non vérifié";
                intro = "Votre compte est actuellement marqué comme non vérifié.";
                details = "Veuillez compléter / corriger vos informations puis soumettre à nouveau la vérification.";
                nextStep = "Une fois votre dossier complété, il sera réexaminé par l’administrateur.";
                badgeBg = "#FEF3C7";
                badgeText = "#92400E";
            }
            case EN_ATTENTE -> {
                title = "Demande en cours de traitement";
                intro = "Votre demande de vérification est bien enregistrée.";
                details = "Elle est actuellement en attente de validation par l’administrateur.";
                nextStep = "Vous recevrez un email dès qu’une décision sera prise.";
                badgeBg = "#DBEAFE";
                badgeText = "#1E40AF";
            }
            default -> {
                title = "Statut mis à jour";
                intro = "Votre statut de vérification a été mis à jour.";
                details = "Nouveau statut : " + status.name();
                nextStep = "Si vous avez des questions, contactez-nous.";
                badgeBg = "#E5E7EB";
                badgeText = "#111827";
            }
        }

        String subject = "Investia — " + title;

        String text =
                "Bonjour " + fullName + ",\n\n" +
                        intro + "\n" +
                        details + "\n\n" +
                        nextStep + "\n\n" +
                        "Cordialement,\n" +
                        "L’équipe Investia\n";

        String html =
                "<!doctype html>" +
                        "<html lang='fr'>" +
                        "<head>" +
                        "  <meta charset='utf-8' />" +
                        "  <meta name='viewport' content='width=device-width, initial-scale=1' />" +
                        "</head>" +
                        "<body style='margin:0; padding:0; background:#F6F8FC; font-family:Arial,Helvetica,sans-serif; color:#0B1220;'>" +

                        "  <table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background:#F6F8FC; padding:24px 12px;'>" +
                        "    <tr>" +
                        "      <td align='center'>" +

                        "        <table role='presentation' width='600' cellpadding='0' cellspacing='0' " +
                        "               style='width:600px; max-width:600px; background:#ffffff; border:1px solid rgba(15,23,42,0.10); border-radius:16px; overflow:hidden;'>" +

                        // Header
                        "          <tr>" +
                        "            <td style='padding:18px 20px; background:linear-gradient(180deg,#173A73,#0B1E3A); color:#ffffff;'>" +
                        "              <div style='font-size:16px; font-weight:800; letter-spacing:0.2px;'>INVESTIA</div>" +
                        "              <div style='margin-top:4px; font-size:12px; opacity:0.85;'>Notification de vérification</div>" +
                        "            </td>" +
                        "          </tr>" +

                        // Content
                        "          <tr>" +
                        "            <td style='padding:22px 20px;'>" +

                        "              <div style='font-size:18px; font-weight:800; margin:0 0 10px;'>Bonjour " + escapeHtml(fullName) + ",</div>" +

                        "              <div style='display:inline-block; padding:8px 12px; border-radius:999px; " +
                        "                          background:" + badgeBg + "; color:" + badgeText + "; font-weight:800; font-size:12px;'>" +
                        "                " + escapeHtml(title) +
                        "              </div>" +

                        "              <div style='margin-top:14px; font-size:14px; line-height:1.6; color:rgba(11,18,32,0.86);'>" +
                        "                <p style='margin:0 0 10px;'>" + escapeHtml(intro) + "</p>" +
                        "                <p style='margin:0 0 10px;'>" + escapeHtml(details) + "</p>" +
                        "                <p style='margin:0;'>" + escapeHtml(nextStep) + "</p>" +
                        "              </div>" +

                        "              <div style='margin-top:18px; padding:12px 14px; border-radius:12px; background:rgba(11,30,58,0.04); " +
                        "                          border:1px solid rgba(11,30,58,0.08); font-size:12px; color:rgba(11,18,32,0.70);'>" +
                        "                Cet email est automatique. Merci de ne pas répondre." +
                        "              </div>" +

                        "            </td>" +
                        "          </tr>" +

                        // Footer
                        "          <tr>" +
                        "            <td style='padding:14px 20px; border-top:1px solid rgba(15,23,42,0.08); background:#ffffff;'>" +
                        "              <div style='font-size:12px; color:rgba(11,18,32,0.62); line-height:1.5;'>" +
                        "                Cordialement,<br/>" +
                        "                <strong>L’équipe Investia</strong>" +
                        "              </div>" +
                        "            </td>" +
                        "          </tr>" +

                        "        </table>" +
                        "      </td>" +
                        "    </tr>" +
                        "  </table>" +

                        "</body></html>";

        return new EmailContent(subject, text, html);
    }

    // =========================
    // Helpers
    // =========================
    private static String buildFullName(User u) {
        String nom = safe(u.getNom());
        String prenom = safe(u.getPrenom());
        String full = (nom + " " + prenom).trim();
        return full.isBlank() ? "cher utilisateur" : full;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    private static class EmailContent {
        final String subject;
        final String text;
        final String html;

        EmailContent(String subject, String text, String html) {
            this.subject = subject;
            this.text = text;
            this.html = html;
        }
    }
}
