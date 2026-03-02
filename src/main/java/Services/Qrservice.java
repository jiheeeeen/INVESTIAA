package Services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de génération de QR Code avec ZXing.
 *
 * ✅ Dépendance à ajouter dans pom.xml :
 *   <dependency>
 *       <groupId>com.google.zxing</groupId>
 *       <artifactId>core</artifactId>
 *       <version>3.5.2</version>
 *   </dependency>
 *   <dependency>
 *       <groupId>com.google.zxing</groupId>
 *       <artifactId>javase</artifactId>
 *       <version>3.5.2</version>
 *   </dependency>
 */
public class Qrservice {

    // ══ Couleurs ══
    private static final int DARK  = new Color(11, 42, 85).getRGB();   // navy
    private static final int LIGHT = new Color(248, 250, 252).getRGB(); // light bg

    // ══════════════════════════════════════════════
    //  GÉNÉRER QR CODE → base64 PNG
    // ══════════════════════════════════════════════
    public static String genererBase64(String contenu, int taille) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix;
        try {
            matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, taille, taille, hints);
        } catch (WriterException e) {
            throw new Exception("Erreur génération QR : " + e.getMessage(), e);
        }

        // Convertir BitMatrix → BufferedImage avec couleurs personnalisées
        BufferedImage image = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < taille; x++) {
            for (int y = 0; y < taille; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? DARK : LIGHT);
            }
        }

        // Convertir image → PNG → Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}