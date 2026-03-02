package Services.Face;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class FaceTemplateService {

    private final CascadeClassifier faceCascade;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    public FaceTemplateService() {
        this.faceCascade = new CascadeClassifier(loadCascadeToTempFile());
        if (this.faceCascade.empty()) {
            throw new RuntimeException("Impossible de charger haarcascade_frontalface_default.xml");
        }
    }

    /**
     * Capture une image depuis webcam et retourne un "template" simple (base64)
     * Pro: on retourne une version normalisée du visage (grayscale + resize),
     * puis encodée en Base64 (à stocker en BD).
     */
    public String captureAndBuildTemplate() throws Exception {
        Mat face = captureFaceMatFromWebcam(0, 3000);
        if (face == null) {
            throw new IllegalStateException("Aucun visage détecté. Réessaie en bonne lumière.");
        }

        // normalisation (grayscale + resize)
        Mat norm = normalizeFace(face);

        // convertir en bytes (raw) pour faire un template simple
        byte[] bytes = matToBytes(norm);

        return Base64.getEncoder().encodeToString(bytes);
    }

    private Mat captureFaceMatFromWebcam(int camIndex, int timeoutMs) throws Exception {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(camIndex);
        grabber.start();

        long start = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - start < timeoutMs) {
                Frame frame = grabber.grab();
                if (frame == null) continue;

                Mat mat = converter.convert(frame);
                if (mat == null || mat.empty()) continue;

                Mat gray = new Mat();
                opencv_imgproc.cvtColor(mat, gray, opencv_imgproc.COLOR_BGR2GRAY);
                opencv_imgproc.equalizeHist(gray, gray);

                Rect faceRect = detectLargestFace(gray);
                if (faceRect != null) {
                    // crop visage
                    Mat face = new Mat(gray, faceRect).clone();
                    return face;
                }
            }
        } finally {
            grabber.stop();
            grabber.release();
        }
        return null;
    }

    private Rect detectLargestFace(Mat gray) {
        RectVector faces = new RectVector();
        faceCascade.detectMultiScale(gray, faces, 1.1, 3, 0,
                new Size(80, 80), new Size());

        if (faces.size() == 0) return null;

        Rect best = null;
        long bestArea = -1;
        for (int i = 0; i < faces.size(); i++) {
            Rect r = faces.get(i);
            long area = (long) r.width() * r.height();
            if (area > bestArea) {
                bestArea = area;
                best = r;
            }
        }
        return best;
    }

    private Mat normalizeFace(Mat faceGray) {
        Mat resized = new Mat();
        opencv_imgproc.resize(faceGray, resized, new Size(120, 120));
        // léger blur pour stabilité
        opencv_imgproc.GaussianBlur(resized, resized, new Size(3, 3), 0);
        return resized;
    }

    private byte[] matToBytes(Mat m) {
        // Mat gray 120x120 => 14400 bytes (1 canal)
        int total = (int) (m.total() * m.channels());
        byte[] data = new byte[total];
        m.data().get(data);
        return data;
    }

    /**
     * Charge le cascade depuis resources vers un fichier temporaire (obligatoire pour CascadeClassifier).
     */
    private String loadCascadeToTempFile() {
        try {
            var url = getClass().getResource("/face/haarcascade_frontalface_default.xml");
            if (url == null) {
                throw new RuntimeException("Fichier cascade introuvable dans resources: /face/haarcascade_frontalface_default.xml");
            }
            byte[] bytes = url.openStream().readAllBytes();

            File tmp = File.createTempFile("haarcascade_frontalface_default", ".xml");
            tmp.deleteOnExit();
            Files.write(tmp.toPath(), bytes);
            return tmp.getAbsolutePath();

        } catch (IOException e) {
            throw new RuntimeException("Erreur chargement cascade: " + e.getMessage(), e);
        }
    }
}