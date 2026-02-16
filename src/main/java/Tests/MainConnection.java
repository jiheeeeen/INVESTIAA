package Tests;

import Utils.MyBD;
import java.sql.Connection;

public class MainConnection {
    public static void main(String[] args) {
        try {
            Connection conn = MyBD.getInstance().getConn();
            if (conn != null && !conn.isClosed()) {
                System.out.println("Connexion OK !");
            } else {
                System.out.println(" Connexion échouée.");
            }
        } catch (Exception e) {
            System.out.println(" Erreur connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
