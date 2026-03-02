package Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GNewsService {

    private final String API_KEY = "1c3eec5090ef7d86f40bf63471300722";

    public String getBusinessTopHeadlinesJson(int max) {
        try {
            String urlStr =
                    "https://gnews.io/api/v4/top-headlines"
                            + "?category=business"
                            + "&lang=fr"
                            + "&max=" + max
                            + "&apikey=" + API_KEY;

            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int code = con.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream()
            ));

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) content.append(line);
            in.close();

            if (code < 200 || code >= 300) {
                String msg = content.toString().replace("\"", "'");
                return "{\"error\":true,\"status\":" + code + ",\"message\":\"" + msg + "\",\"articles\":[]}";
            }

            return content.toString();

        } catch (Exception e) {
            String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
            return "{\"error\":true,\"message\":\"" + msg + "\",\"articles\":[]}";
        }
    }
}