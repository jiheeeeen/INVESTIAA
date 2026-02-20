package Utils;

import java.util.List;

public class JsonUtil {

    // champs attendus par ton JS:
    // id, title, short, category, status, goal, updatedAt, odd
    public static class ProjetCard {
        public int id;
        public String title;
        public String shortDesc; // exporté en "short"
        public String category;
        public String status;
        public double goal;
        public String updatedAt;
        public String odd;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static String toJson(List<ProjetCard> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            ProjetCard p = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
                    .append("\"id\":").append(p.id).append(",")
                    .append("\"title\":\"").append(esc(p.title)).append("\",")
                    .append("\"short\":\"").append(esc(p.shortDesc)).append("\",")
                    .append("\"category\":\"").append(esc(p.category)).append("\",")
                    .append("\"status\":\"").append(esc(p.status)).append("\",")
                    .append("\"goal\":").append(p.goal).append(",")
                    .append("\"updatedAt\":\"").append(esc(p.updatedAt)).append("\",")
                    .append("\"odd\":\"").append(esc(p.odd)).append("\"")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}
