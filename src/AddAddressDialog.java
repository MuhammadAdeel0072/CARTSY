import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AddAddressDialog extends JDialog {
    public AddAddressDialog(JDialog parent, int customerId, ManageAddressesDialog addressDialog) {
        super(parent, "Add New Address", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setPreferredSize(new Dimension(450, 550));

        // Top Panel for Title + Close Button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel title = new JLabel("Add Delivery Address");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(primary);
        JButton closeBtn = new JButton("X");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setForeground(primary);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);

        JTextField labelField = new JTextField();
        JTextArea addressArea = new JTextArea(3, 20);
        addressArea.setLineWrap(true);
        JTextField cityField = new JTextField();
        JTextField stateField = new JTextField();
        JTextField postalField = new JTextField();
        JTextField phoneField = new JTextField();
        JCheckBox defaultCheck = new JCheckBox("Set as default address");

        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 15);
        labelField.setFont(fieldFont);
        addressArea.setFont(fieldFont);
        cityField.setFont(fieldFont);
        stateField.setFont(fieldFont);
        postalField.setFont(fieldFont);
        phoneField.setFont(fieldFont);

        JButton saveBtn = new JButton("Save Address");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        saveBtn.setBackground(primary);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        saveBtn.addActionListener(e -> {
            String label = labelField.getText().trim();
            String address = addressArea.getText().trim();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String postal = postalField.getText().trim();
            String phone = phoneField.getText().trim();

            // Validation
            if (label.isEmpty() || address.isEmpty() || city.isEmpty() || phone.isEmpty()) {
                statusLabel.setText("Please fill all required fields!");
                return;
            }
            if (!label.matches("[a-zA-Z ]+")) {
                statusLabel.setText("Label must contain only letters!");
                return;
            }
            if (!city.matches("[a-zA-Z ]+")) {
                statusLabel.setText("City must contain only letters!");
                return;
            }
            if (!state.isEmpty() && !state.matches("[a-zA-Z ]+")) {
                statusLabel.setText("State must contain only letters!");
                return;
            }
            if (!postal.isEmpty() && !postal.matches("\\d+")) {
                statusLabel.setText("Postal code must be numbers!");
                return;
            }
            if (!phone.matches("^\\d{4}-\\d{7}$")) {
                statusLabel.setText("Phone format: 0XXX-XXXXXXX");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                if (defaultCheck.isSelected()) {
                    String resetQuery = "UPDATE addresses SET is_default=FALSE WHERE user_id=?";
                    PreparedStatement resetPs = conn.prepareStatement(resetQuery);
                    resetPs.setInt(1, customerId);
                    resetPs.executeUpdate();
                }

                String query = "INSERT INTO addresses (user_id, address_label, full_address, city, state, postal_code, phone, is_default) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, customerId);
                ps.setString(2, label);
                ps.setString(3, address);
                ps.setString(4, city);
                ps.setString(5, state);
                ps.setString(6, postal);
                ps.setString(7, phone);
                ps.setBoolean(8, defaultCheck.isSelected());
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Address added successfully!");
                addressDialog.loadAddresses();
                dispose();
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        mainPanel.add(topPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(createLabelField("Label (Home/Office):", labelField));
        mainPanel.add(createLabelArea("Full Address:*", addressArea));
        mainPanel.add(createLabelField("City:*", cityField));
        mainPanel.add(createLabelField("State:", stateField));
        mainPanel.add(createLabelField("Postal Code:", postalField));
        mainPanel.add(createLabelField("Phone:*", phoneField));
        mainPanel.add(defaultCheck);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(saveBtn);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(statusLabel);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private JPanel createLabelField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        p.add(l);
        p.add(field);
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        return p;
    }

    private JPanel createLabelArea(String label, JTextArea area) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        p.add(l);
        p.add(scroll);
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        return p;
    }
}
