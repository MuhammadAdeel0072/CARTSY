import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

class UpdateSellerTierDialog extends JDialog {

    UpdateSellerTierDialog(JFrame parent) {
        super(parent, "Update Seller Tier", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(18, 28, 18, 28)
        ));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setPreferredSize(new Dimension(500, 480));

        // TOP BAR
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Update Seller Tier");
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

        // TABLE
        String[] columns = {"ID", "Seller Name", "CNIC", "Seller Code"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 15));
        table.setEnabled(true);  // Allow row selection

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(450, 200));
        mainPanel.add(scrollPane);

        // Load table content
        loadSellers(model);

        // INPUT FIELDS
        JTextField sellerCodeField = new JTextField();
        sellerCodeField.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JComboBox<String> tierBox = new JComboBox<>(new String[]{"Basic", "Silver", "Gold", "Platinum"});
        tierBox.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        mainPanel.add(labelAndField("Enter Seller Code:", sellerCodeField));
        mainPanel.add(labelAndField("Select New Tier:", tierBox));

        // CLICK ROW → AUTO FILL SELLER CODE
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    String code = table.getValueAt(selectedRow, 3).toString();
                    sellerCodeField.setText(code);  // Set seller code automatically
                }
            }
        });

        // BUTTONS
        JButton updateBtn = new JButton("Update Tier");
        updateBtn.setFont(new Font("Segoe UI", Font.BOLD, 17));
        updateBtn.setBackground(primary);
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setBorder(BorderFactory.createLineBorder(primary, 14, true));
        updateBtn.setFocusPainted(false);
        updateBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateBtn.setMaximumSize(new Dimension(180, 40));
        updateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 0, 0));
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(updateBtn);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(errorLabel);

        // UPDATE ACTION
        updateBtn.addActionListener(e -> {
            String sellerCode = sellerCodeField.getText().trim();
            String newTier = (String) tierBox.getSelectedItem();

            if (sellerCode.isEmpty()) {
                errorLabel.setText("Seller Code required!");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                String query = "UPDATE sellers SET tier = ? WHERE seller_code = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, newTier);
                ps.setString(2, sellerCode);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Tier updated successfully!");
                    model.setRowCount(0);
                    loadSellers(model);
                    sellerCodeField.setText("");
                    errorLabel.setText(" ");
                } else {
                    errorLabel.setText("Seller not found!");
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

    // Load sellers table
    private void loadSellers(DefaultTableModel model) {
        try (Connection conn = DBConnection.getConnection()) {

            String query =
                    "SELECT u.id AS user_id, u.name, u.cnic, s.seller_code " +
                            "FROM sellers s " +
                            "JOIN users u ON s.user_id = u.id " +
                            "ORDER BY u.id";

            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("cnic"),
                        rs.getString("seller_code")
                });
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading sellers: " + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel labelAndField(String label, JComponent field) {
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
