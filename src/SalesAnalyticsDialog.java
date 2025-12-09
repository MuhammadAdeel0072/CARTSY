import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SalesAnalyticsDialog extends JDialog {

    public SalesAnalyticsDialog(JFrame parent, int sellerId) {
        super(parent, "Sales Analytics", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        mainPanel.setPreferredSize(new Dimension(800, 400)); // Slightly wider for address

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Sales Analytics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
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

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // --------------------------------------------------------
        // NON EDITABLE MODEL  ✔
        // --------------------------------------------------------
        String[] columns = {"Sale ID", "Product", "Quantity", "Total Price", "Date", "Address"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // <-- MAKES TABLE NOT EDITABLE
            }
        };

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT s.sale_id, p.name AS product, s.quantity, s.total_price, s.sale_date, " +
                    "a.full_address AS address " +
                    "FROM sales s " +
                    "JOIN products p ON s.product_id = p.product_id " +
                    "JOIN addresses a ON s.customer_id = a.user_id AND a.is_default = TRUE " +
                    "WHERE p.seller_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("sale_id"),
                        rs.getString("product"),
                        rs.getInt("quantity"),
                        rs.getDouble("total_price"),
                        rs.getTimestamp("sale_date"),
                        rs.getString("address")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }

        JTable table = new JTable(model);

        // --- Make column headers bold ---
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(table);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}
