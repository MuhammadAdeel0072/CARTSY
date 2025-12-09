import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AddProductDialog extends JDialog {
    public AddProductDialog(JFrame parent, int sellerId) {
        super(parent, "Add Product", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(18, 28, 18, 28)
        ));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setPreferredSize(new Dimension(400, 390));

        // Top bar with title and close button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Add Product");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(primary);

        JButton closeBtn = new JButton("X");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setForeground(primary);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setToolTipText("Close");
        closeBtn.addActionListener(e -> dispose());
        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeBtn.setForeground(Color.RED);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeBtn.setForeground(primary);
            }
        });

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);
        mainPanel.add(topPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextField nameField = new JTextField();
        JTextField descField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField qtyField = new JTextField();

        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 16);
        nameField.setFont(fieldFont);
        descField.setFont(fieldFont);
        priceField.setFont(fieldFont);
        qtyField.setFont(fieldFont);

        mainPanel.add(labelAndField("Product Name:", nameField));
        mainPanel.add(labelAndField("Description:", descField));
        mainPanel.add(labelAndField("Price:", priceField));
        mainPanel.add(labelAndField("Quantity:", qtyField));

        JButton addBtn = new JButton("Add Product");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        addBtn.setBackground(primary);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setMaximumSize(new Dimension(200, 40));
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addBtn.setBorder(BorderFactory.createLineBorder(primary, 16, true));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 0, 0));
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setPreferredSize(new Dimension(350, 40));

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(addBtn);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(errorLabel);

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            String priceStr = priceField.getText().trim();
            String qtyStr = qtyField.getText().trim();

            // ---- UPDATED VALIDATION -----

            // Product name: must include letters, digits optional, NO symbols
            if (!name.matches("^[A-Za-z0-9 ]*[A-Za-z][A-Za-z0-9 ]*$")) {
                errorLabel.setText("Product name must include letters (numbers optional)!");
                return;
            }

            // Description: must include letters, digits + symbols allowed
            if (!desc.matches("^[A-Za-z0-9 ,._\\-+()!?:;/]*[A-Za-z][A-Za-z0-9 ,._\\-+()!?:;/]*$")) {
                errorLabel.setText("Description must include letters (symbols allowed)!");
                return;
            }

            // Price: Must be integer
            if (!priceStr.matches("^[0-9]+$")) {
                errorLabel.setText("Price must be a number only!");
                return;
            }

            // Quantity: Must be integer
            if (!qtyStr.matches("^[0-9]+$")) {
                errorLabel.setText("Quantity must be a number only!");
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int qty = Integer.parseInt(qtyStr);

                try (Connection conn = DBConnection.getConnection()) {
                    String query = "INSERT INTO products (seller_id, name, description, price, quantity) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, sellerId);
                    ps.setString(2, name);
                    ps.setString(3, desc);
                    ps.setDouble(4, price);
                    ps.setInt(5, qty);
                    ps.executeUpdate();

                    // Track inventory addition
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        int productId = rs.getInt(1);
                        String inventoryQuery = "INSERT INTO inventory (product_id, change_type, quantity) VALUES (?, 'add', ?)";
                        PreparedStatement invPs = conn.prepareStatement(inventoryQuery);
                        invPs.setInt(1, productId);
                        invPs.setInt(2, qty);
                        invPs.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(this, "Product Added!");
                    dispose();
                }
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private JPanel labelAndField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 15));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.add(l);
        p.add(field);
        p.add(Box.createRigidArea(new Dimension(0, 7)));
        return p;
    }
}
