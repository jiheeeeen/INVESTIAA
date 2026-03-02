package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyBD {

    private Connection conn;

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/investiaa?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    private static MyBD instance;

    private MyBD() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = connectWithFallback();
            System.out.println("Connected to DB: " + conn.getMetaData().getURL());
        } catch (ClassNotFoundException e) {
            System.out.println("Driver MySQL introuvable : " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        }
    }

    private Connection connectWithFallback() throws SQLException {
        String overrideUrl = System.getProperty("db.url");
        String envUrl = System.getenv("DB_URL");
        String[] candidates = {
                overrideUrl,
                envUrl,
                DEFAULT_URL,
                "jdbc:mysql://localhost:3306/investia?useSSL=false&serverTimezone=UTC",
                "jdbc:mysql://localhost:3307/investiaa?useSSL=false&serverTimezone=UTC",
                "jdbc:mysql://localhost:3307/investia?useSSL=false&serverTimezone=UTC"
        };

        SQLException last = null;
        for (String url : candidates) {
            if (url == null || url.isBlank()) continue;
            try {
                return DriverManager.getConnection(url, USER, PASS);
            } catch (SQLException e) {
                last = e;
            }
        }
        throw (last != null) ? last : new SQLException("Aucune URL JDBC valide configuree.");
    }

    public static MyBD getInstance() {
        if (instance == null) {
            instance = new MyBD();
        }
        return instance;
    }

    public synchronized Connection getConn() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = connectWithFallback();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DB_CONNECTION_UNAVAILABLE. Configurez -Ddb.url=jdbc:mysql://host:port/db " +
                            "ou la variable d'environnement DB_URL. Cause: " + e.getMessage(), e);
        }
        return conn;
    }
}
