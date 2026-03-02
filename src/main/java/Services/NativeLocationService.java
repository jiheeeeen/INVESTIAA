package Services;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class NativeLocationService {

    public String getNativeLocationJson() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return "{\"ok\":false,\"message\":\"Localisation native Windows non disponible sur cet OS.\"}";
        }
        try {
            String script =
                    "$ErrorActionPreference='Stop';" +
                    "Add-Type -AssemblyName System.Runtime.WindowsRuntime;" +
                    "$null=[Windows.Devices.Geolocation.Geolocator,Windows.Devices.Geolocation,ContentType=WindowsRuntime];" +
                    "$g=New-Object Windows.Devices.Geolocation.Geolocator;" +
                    "$g.DesiredAccuracyInMeters=50;" +
                    "$g.ReportInterval=0;" +
                    "$p=$g.GetGeopositionAsync().AsTask().GetAwaiter().GetResult();" +
                    "$lat=$p.Coordinate.Point.Position.Latitude;" +
                    "$lon=$p.Coordinate.Point.Position.Longitude;" +
                    "Write-Output ($lat.ToString([System.Globalization.CultureInfo]::InvariantCulture)+'|'+$lon.ToString([System.Globalization.CultureInfo]::InvariantCulture));";

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean done = p.waitFor(20, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return "{\"ok\":false,\"message\":\"Timeout localisation native Windows.\"}";
            }

            String out = readAll(p.getInputStream()).trim();
            int pipe = out.lastIndexOf('|');
            if (pipe <= 0 || pipe >= out.length() - 1) {
                return "{\"ok\":false,\"message\":\"Localisation native indisponible: " + jsonSafe(out) + "\"}";
            }

            double lat = parseDoubleSafe(out.substring(0, pipe));
            double lon = parseDoubleSafe(out.substring(pipe + 1));
            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                return "{\"ok\":false,\"message\":\"Coordonnees natives invalides.\"}";
            }

            return "{"
                    + "\"ok\":true"
                    + ",\"source\":\"windows-native\""
                    + ",\"lat\":" + lat
                    + ",\"lon\":" + lon
                    + "}";
        } catch (Exception e) {
            return "{\"ok\":false,\"message\":\"Echec localisation native: " + jsonSafe(e.getMessage()) + "\"}";
        }
    }

    private static double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble((s == null ? "" : s.trim()).replace(',', '.'));
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) >= 0) bos.write(buf, 0, n);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static String jsonSafe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "'").replace("\r", " ").replace("\n", " ").trim();
    }
}
