import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UpdateProductDialog extends JDialog {

    public UpdateProductDialog(JFrame parent, int sellerId) {
        super(parent, "Update Product", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        // MAIN PANEL
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        mainPanel.setPreferredSize(new Dimension(820, 600));

        // -------------------- TOP PANEL ---------------------
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Update Product");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(primary);

        JButton closeBtn = new JButton("X");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setForeground(primary);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { closeBtn.setForeground(Color.RED); }
            public void mouseExited(MouseEvent evt) { closeBtn.setForeground(primary); }
        });

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // -------------------- TABLE PANEL ---------------------
        String[] cols = {"Product ID", "Name", "Description", "Price", "Quantity"};

        // ⛔ MAKE TABLE NON-EDITABLE (ONLY CHANGE ADDED)
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // table cells cannot be edited
            }
        };

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT product_id, name, description, price, quantity FROM products WHERE seller_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sellerId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading products: " + ex.getMessage());
        }

        JTable table = new JTable(model);
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(780, 250));
        mainPanel.add(tableScroll, BorderLayout.CENTER);

        // -------------------- UPDATE FORM PANEL ---------------------
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JTextField productIdField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField descField = new JTextField();
        JTextField priceField = new JTextField();

        Font f = new Font("Segoe UI", Font.PLAIN, 16);
        productIdField.setFont(f);
        nameField.setFont(f);
        descField.setFont(f);
        priceField.setFont(f);

        formPanel.add(labelAndField("Product ID (Required):", productIdField));
        formPanel.add(labelAndField("New Name:", nameField));
        formPanel.add(labelAndField("New Description:", descField));
        formPanel.add(labelAndField("New Price:", priceField));

        JButton updateBtn = new JButton("Update");
        updateBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        updateBtn.setBackground(primary);
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setFocusPainted(false);
        updateBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateBtn.setMaximumSize(new Dimension(200, 45));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        formPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        formPanel.add(updateBtn);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(errorLabel);

        mainPanel.add(formPanel, BorderLayout.SOUTH);

        // -------------------- TABLE CLICK FEATURE ---------------------
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    productIdField.setText(table.getValueAt(row, 0).toString());
                    nameField.setText(table.getValueAt(row, 1).toString());
                    descField.setText(table.getValueAt(row, 2).toString());
                    priceField.setText(table.getValueAt(row, 3).toString());
                }
            }
        });

        // -------------------- UPDATE FUNCTIONALITY ---------------------
        updateBtn.addActionListener(e -> {
            String pid = productIdField.getText().trim();
            String newName = nameField.getText().trim();
            String newDesc = descField.getText().trim();
            String newPrice = priceField.getText().trim();

            if (!pid.matches("^[0-9]+$")) {
                errorLabel.setText("Product ID must be numeric.");
                return;
            }

            if (!newName.isEmpty() && !newName.matches("^[A-Za-z0-9 ]+$")) {
                errorLabel.setText("Invalid name format.");
                return;
            }

            if (!newDesc.isEmpty() && !newDesc.matches("^[A-Za-z0-9 .,/_\\-+!@#$%&*()]+$")) {
                errorLabel.setText("Invalid description format.");
                return;
            }

            if (!newPrice.isEmpty() && !newPrice.matches("^[0-9]+$")) {
                errorLabel.setText("Price must be numeric.");
                return;
            }

            if (newName.isEmpty() && newDesc.isEmpty() && newPrice.isEmpty()) {
                errorLabel.setText("Enter at least one field to update.");
                return;
            }

            try {
                int productId = Integer.parseInt(pid);

                StringBuilder query = new StringBuilder("UPDATE products SET ");
                java.util.List<Object> params = new java.util.ArrayList<>();

                if (!newName.isEmpty()) { query.append("name=?, "); params.add(newName); }
                if (!newDesc.isEmpty()) { query.append("description=?, "); params.add(newDesc); }
                if (!newPrice.isEmpty()) { query.append("price=?, "); params.add(Integer.parseInt(newPrice)); }

                query.setLength(query.length() - 2); // remove last comma
                query.append(" WHERE product_id=? AND seller_id=?");
                params.add(productId);
                params.add(sellerId);

                try (Connection conn = DBConnection.getConnection()) {
                    PreparedStatement ps = conn.prepareStatement(query.toString());
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }

                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        errorLabel.setForeground(new Color(0, 128, 0)); // green success
                        errorLabel.setText("Product updated successfully!");
                        refreshTable(model, sellerId); // refresh table
                    } else {
                        errorLabel.setForeground(Color.RED);
                        errorLabel.setText("Product not found or not owned by you.");
                    }
                }

            } catch (Exception ex) {
                errorLabel.setForeground(Color.RED);
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    // -------------------- REFRESH TABLE ---------------------
    private void refreshTable(DefaultTableModel model, int sellerId) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT product_id, name, description, price, quantity FROM products WHERE seller_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error refreshing table: " + ex.getMessage());
        }
    }

    private JPanel labelAndField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 15));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        p.add(l);
        p.add(field);
        p.add(Box.createRigidArea(new Dimension(0, 7)));

        return p;
    }
}
