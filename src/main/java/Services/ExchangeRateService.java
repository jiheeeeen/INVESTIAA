package Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExchangeRateService {

    public String getTndToEurUsdRatesJson() throws Exception {

        // ✅ API stable et gratuite
        String apiUrl = "https://open.er-api.com/v6/latest/TND";

        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("GET");

        // ✅ Important pour éviter blocage
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);

        int status = con.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("HTTP error code: " + status);
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream())
        );

        StringBuilder content = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            content.append(line);
        }

        in.close();
        con.disconnect();

        return content.toString();
    }
}