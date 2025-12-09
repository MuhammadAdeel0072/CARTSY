import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterScreen extends JDialog {

    public RegisterScreen(JFrame parent) {
        super(parent, "Register", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(40, 40, 40, 40)));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(420, 450));

        // Top bar with close button
        JButton closeBtn = new JButton("X");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setForeground(primary);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 20));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setToolTipText("Close");
        closeBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        closeBtn.addActionListener(e -> dispose());
        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeBtn.setForeground(Color.RED);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeBtn.setForeground(primary);
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(closeBtn, BorderLayout.EAST);

        JLabel title = new JLabel("Register", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(primary);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Input fields
        JTextField nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(300, 38));
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        nameField.setBorder(BorderFactory.createTitledBorder("Name"));

        JTextField emailField = new JTextField();
        emailField.setMaximumSize(new Dimension(300, 38));
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        emailField.setBorder(BorderFactory.createTitledBorder("Email"));

        JPasswordField passwordField = new JPasswordField();
        passwordField.setMaximumSize(new Dimension(300, 38));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        passwordField.setBorder(BorderFactory.createTitledBorder("Password"));

        // ---- FIX: REMOVE ROLE DROPDOWN ----
        // User is automatically Customer
        String role = "Customer";

        // Register button
        JButton registerBtn = new JButton("Register");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 19));
        registerBtn.setBackground(primary);
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        registerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerBtn.setMaximumSize(new Dimension(200, 45));
        registerBtn.setBorder(BorderFactory.createLineBorder(primary, 18, true));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Error label
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 0, 0));
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setPreferredSize(new Dimension(350, 40));

        // ----------------------------
        // REGISTER BUTTON LOGIC
        // ----------------------------
        registerBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String pass = new String(passwordField.getPassword());

            // VALIDATION
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("All fields are required!");
                return;
            }

            if (!name.matches("^[A-Za-z ]+$")) {
                errorLabel.setText("Name must contain only letters!");
                return;
            }

            if (!email.matches("^[a-z]+[0-9]*@gmail\\.com$")) {
    errorLabel.setText("Invalid email format!");
    return;
}


            try (Connection conn = DBConnection.getConnection()) {
                if (conn != null) {

                    // NOTE: Seller registration REMOVED
                    // Only Customer will be inserted

                    String query = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, pass);
                    ps.setString(4, role);  // always "Customer"
                    ps.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Customer Registered Successfully!");
                    dispose();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                errorLabel.setText("Registration Failed! Email may already exist.");
            }
        });

        // ADD COMPONENTS
        panel.add(topPanel);
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 18)));
        panel.add(nameField);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(emailField);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(passwordField);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(registerBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(errorLabel);

        add(panel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}
