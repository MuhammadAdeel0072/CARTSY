import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class SelectSellerChatDialog extends JDialog {

    private int customerId;

    public SelectSellerChatDialog(JFrame parent, int customerId) {
        super(parent, "Select Seller to Chat", true);
        this.customerId = customerId;
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        mainPanel.setPreferredSize(new Dimension(600, 400));

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Select Seller to Chat");
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

        // Table of sellers
        String[] columns = {"Seller ID", "Seller Name", "Seller Code", "Tier"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT u.id, u.name, s.seller_code, s.tier FROM users u " +
                           "JOIN sellers s ON u.id = s.user_id WHERE u.role='Seller' AND s.is_approved=TRUE";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("seller_code"),
                        rs.getString("tier")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }

        JTable table = new JTable(model);
        table.setRowHeight(30);

        // Make table header bold
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(table);

        // Button to open chat
        JButton chatBtn = new JButton("Start Chat");
        chatBtn.setBackground(primary);
        chatBtn.setForeground(Color.WHITE);
        chatBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chatBtn.setFocusPainted(false);

        chatBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a seller!");
                return;
            }
            int sellerId = (int) model.getValueAt(row, 0);
            // Open ChatDialog with selected seller immediately
            new ChatDialog((JFrame) this.getParent(), customerId, "Customer", sellerId);
        });

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(chatBtn);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}
