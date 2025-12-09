import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ManageInventoryDialog extends JDialog {
    public ManageInventoryDialog(JFrame parent, int sellerId) {
        super(parent, "Manage Inventory", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        mainPanel.setPreferredSize(new Dimension(700, 440));

        // ---------- TOP PANEL ----------
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Manage Inventory");
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

        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { closeBtn.setForeground(Color.RED); }
            public void mouseExited(java.awt.event.MouseEvent evt) { closeBtn.setForeground(primary); }
        });

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // ---------- TABLE ----------
        String[] columns = {"Product ID", "Name", "Quantity"};

        // MAKE TABLE NON-EDITABLE
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Disable editing
            }
        };

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT product_id, name, quantity FROM products WHERE seller_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sellerId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getInt("quantity")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }

        JTable table = new JTable(model);

        // BOLD TABLE HEADER
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(table);

        // ---------- UPDATE PANEL ----------
        JPanel updatePanel = new JPanel();
        updatePanel.setLayout(new BoxLayout(updatePanel, BoxLayout.X_AXIS));
        updatePanel.setOpaque(false);

        JTextField productIdField = new JTextField();
        productIdField.setMaximumSize(new Dimension(100, 32));
        productIdField.setEditable(false); // NOT EDITABLE

        JTextField qtyField = new JTextField();
        qtyField.setMaximumSize(new Dimension(100, 32));
        qtyField.setEditable(true); // This should remain editable

        JButton updateBtn = new JButton("Update Quantity");
        updateBtn.setBackground(primary);
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        updateBtn.setFocusPainted(false);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 0, 0));
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        errorLabel.setPreferredSize(new Dimension(350, 40));

        updatePanel.add(new JLabel("Product ID: "));
        updatePanel.add(productIdField);
        updatePanel.add(Box.createRigidArea(new Dimension(10, 0)));
        updatePanel.add(new JLabel("New Qty: "));
        updatePanel.add(qtyField);
        updatePanel.add(Box.createRigidArea(new Dimension(10, 0)));
        updatePanel.add(updateBtn);

        // ---------- AUTO-FILL WHEN ROW CLICKED ----------
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    productIdField.setText(model.getValueAt(selectedRow, 0).toString());
                    qtyField.setText(model.getValueAt(selectedRow, 2).toString());
                }
            }
        });

        // ---------- BOTTOM PANEL ----------
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.add(updatePanel);
        bottomPanel.add(errorLabel);

        // Add everything
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // ---------- UPDATE BUTTON ACTION ----------
        updateBtn.addActionListener(e -> {
            String productIdStr = productIdField.getText().trim();
            String qtyStr = qtyField.getText().trim();

            if (productIdStr.isEmpty() || qtyStr.isEmpty()) {
                errorLabel.setText("Both fields required!");
                return;
            }

            try {
                int productId = Integer.parseInt(productIdStr);
                int newQty = Integer.parseInt(qtyStr);

                try (Connection conn = DBConnection.getConnection()) {
                    String getOldQty = "SELECT quantity FROM products WHERE product_id=? AND seller_id=?";
                    PreparedStatement getPs = conn.prepareStatement(getOldQty);
                    getPs.setInt(1, productId);
                    getPs.setInt(2, sellerId);
                    ResultSet rs = getPs.executeQuery();

                    if (!rs.next()) {
                        errorLabel.setText("Product not found or not yours!");
                        return;
                    }

                    int oldQty = rs.getInt("quantity");
                    int diff = newQty - oldQty;

                    String updateQuery = "UPDATE products SET quantity=? WHERE product_id=? AND seller_id=?";
                    PreparedStatement ps = conn.prepareStatement(updateQuery);
                    ps.setInt(1, newQty);
                    ps.setInt(2, productId);
                    ps.setInt(3, sellerId);
                    int rows = ps.executeUpdate();

                    if (rows > 0) {
                        model.setValueAt(newQty, table.getSelectedRow(), 2); // update UI
                        errorLabel.setForeground(new Color(0, 120, 0));
                        errorLabel.setText("Quantity updated!");
                    }
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
}
