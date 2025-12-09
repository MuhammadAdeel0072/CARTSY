import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

class RemoveSellerDialog extends JDialog {

    private JTable sellersTable;
    private JTextField sellerCodeField;

    RemoveSellerDialog(JFrame parent) {
        super(parent, "Remove Seller", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(18, 28, 18, 28)
        ));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setPreferredSize(new Dimension(600, 450));

        // TOP BAR
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Remove Seller");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(primary);

        JButton closeBtn = new JButton("X");
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setForeground(primary);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
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

        // TABLE PANEL
        String[] columns = {"ID", "Seller Name", "CNIC", "Seller Code"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        sellersTable = new JTable(model);
        sellersTable.setRowHeight(28);
        sellersTable.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        sellersTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));

        JScrollPane scrollPane = new JScrollPane(sellersTable);
        scrollPane.setPreferredSize(new Dimension(550, 200));
        mainPanel.add(scrollPane);

        loadSellers(model);

        // Auto-fill seller code on row click
        sellersTable.getSelectionModel().addListSelectionListener(e -> {
            int row = sellersTable.getSelectedRow();
            if (row >= 0) {
                String code = sellersTable.getValueAt(row, 3).toString();
                sellerCodeField.setText(code);
            }
        });

        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        sellerCodeField = new JTextField();
        sellerCodeField.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        mainPanel.add(labelAndField("Enter Seller Code to Remove:", sellerCodeField));

        JButton removeBtn = new JButton("Remove Seller");
        removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 17));
        removeBtn.setBackground(primary);
        removeBtn.setForeground(Color.WHITE);
        removeBtn.setFocusPainted(false);
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeBtn.setMaximumSize(new Dimension(200, 40));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 0, 0));
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(removeBtn);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(errorLabel);

        // REMOVE BUTTON LOGIC
        removeBtn.addActionListener(e -> {
            String sellerCode = sellerCodeField.getText().trim();
            if (sellerCode.isEmpty()) {
                errorLabel.setText("Seller Code is required!");
                return;
            }

            int option = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to remove this seller?",
                    "Confirm Remove",
                    JOptionPane.YES_NO_OPTION);

            if (option != JOptionPane.YES_OPTION) return;

            try (Connection conn = DBConnection.getConnection()) {
                String query =
                        "DELETE u FROM users u " +
                        "JOIN sellers s ON u.id = s.user_id " +
                        "WHERE s.seller_code = ?";

                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, sellerCode);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Seller Removed Successfully!");

                    // Refresh table
                    model.setRowCount(0);
                    loadSellers(model);

                    sellerCodeField.setText("");
                } else {
                    errorLabel.setText("Seller Code not found!");
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

    private void loadSellers(DefaultTableModel model) {
        try (Connection conn = DBConnection.getConnection()) {
            String query =
                    "SELECT u.id, u.name, u.cnic, s.seller_code " +
                    "FROM users u JOIN sellers s ON u.id = s.user_id";

            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("cnic"),
                        rs.getString("seller_code")
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
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
