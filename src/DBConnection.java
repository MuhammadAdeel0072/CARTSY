import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/cartsydb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "";//your DB username
    private static final String PASS = "";//your DB password

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, 
                "MySQL JDBC Driver not found!\nPlease add mysql-connector-java to your classpath.", 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (java.sql.SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Database Connection Failed!\n" +
                "Error: " + e.getMessage() + "\n\n" +
                "Please check:\n" +
                "1. MySQL server is running\n" +
                "2. Database 'cartsydb' exists\n" +
                "3. Username and password are correct\n" +
                "4. MySQL is running on localhost:3306", 
                "Database Connection Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Unexpected Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return null;
    }
}
