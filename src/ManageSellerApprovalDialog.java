import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class ManageSellerApprovalDialog extends JDialog {
    private DefaultTableModel model;
    private JTextField searchField;
    private JComboBox<String> filterCombo;

    public ManageSellerApprovalDialog(JFrame parent) {
        super(parent, "Manage Seller Approvals", true);
        setUndecorated(true); // Keep undecorated but add a visible close button manually

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        mainPanel.setPreferredSize(new Dimension(900, 500));

        // Top panel with title + close button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Seller Approval Management");
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

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);

        // Search + Filter Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);

        searchField = new JTextField(20);
        filterCombo = new JComboBox<>(new String[]{"All", "Approved", "Reject"});
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(primary);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchBtn.setFocusPainted(false);

        searchBtn.addActionListener(e -> searchSellers());

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Filter:"));
        searchPanel.add(filterCombo);
        searchPanel.add(searchBtn);

        topPanel.add(searchPanel, BorderLayout.SOUTH);

        String[] columns = {"Seller ID", "Name", "Email", "Code", "Status", "Applied Date"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        loadSellers();

        JTable table = new JTable(model);
        table.setRowHeight(28);
        JScrollPane scrollPane = new JScrollPane(table);

        JButton approveBtn = new JButton("Approve");
        approveBtn.setBackground(new Color(0, 180, 80));
        approveBtn.setForeground(Color.WHITE);
        approveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        approveBtn.setFocusPainted(false);

        JButton rejectBtn = new JButton("Reject");
        rejectBtn.setBackground(new Color(220, 80, 80));
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rejectBtn.setFocusPainted(false);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setBackground(primary);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshBtn.setFocusPainted(false);

        approveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a seller!");
                return;
            }
            int sellerId = (int) model.getValueAt(row, 0);
            approveSeller(sellerId);
        });

        rejectBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a seller!");
                return;
            }
            int sellerId = (int) model.getValueAt(row, 0);
            String reason = JOptionPane.showInputDialog(this, "Rejection Reason:");
            if (reason != null && !reason.trim().isEmpty()) {
                rejectSeller(sellerId, reason);
            }
        });

        refreshBtn.addActionListener(e -> loadSellers());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        btnPanel.add(approveBtn);
        btnPanel.add(rejectBtn);
        btnPanel.add(refreshBtn);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void loadSellers() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT s.seller_id, u.name, u.email, s.seller_code, s.is_approved, s.rejection_reason, u.created_at " +
                    "FROM sellers s JOIN users u ON s.user_id = u.id " +
                    "ORDER BY s.is_approved ASC, u.created_at DESC";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String status;
                if (rs.getBoolean("is_approved")) {
                    status = "Approved";
                } else if (rs.getString("rejection_reason") != null) {
                    status = "Reject";
                } else {
                    status = "Pending";
                }

                model.addRow(new Object[]{
                        rs.getInt("seller_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("seller_code"),
                        status,
                        rs.getTimestamp("created_at")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void searchSellers() {
        model.setRowCount(0);
        String keyword = searchField.getText().trim();
        String filter = (String) filterCombo.getSelectedItem();

        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder query = new StringBuilder(
                    "SELECT s.seller_id, u.name, u.email, s.seller_code, s.is_approved, s.rejection_reason, u.created_at " +
                            "FROM sellers s JOIN users u ON s.user_id = u.id WHERE 1=1 ");

            if (!keyword.isEmpty()) {
                query.append("AND (u.name LIKE ? OR u.email LIKE ? OR s.seller_code LIKE ?) ");
            }

            if ("Approved".equals(filter)) {
                query.append("AND s.is_approved=TRUE ");
            } else if ("Reject".equals(filter)) {
                query.append("AND s.is_approved=FALSE AND s.rejection_reason IS NOT NULL ");
            }

            query.append("ORDER BY s.is_approved ASC, u.created_at DESC");

            PreparedStatement ps = conn.prepareStatement(query.toString());

            int paramIndex = 1;
            if (!keyword.isEmpty()) {
                ps.setString(paramIndex++, "%" + keyword + "%");
                ps.setString(paramIndex++, "%" + keyword + "%");
                ps.setString(paramIndex++, "%" + keyword + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String status;
                if (rs.getBoolean("is_approved")) {
                    status = "Approved";
                } else if (rs.getString("rejection_reason") != null) {
                    status = "Reject";
                } else {
                    status = "Pending";
                }

                model.addRow(new Object[]{
                        rs.getInt("seller_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("seller_code"),
                        status,
                        rs.getTimestamp("created_at")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void approveSeller(int sellerId) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "UPDATE sellers SET is_approved=TRUE, approval_date=NOW(), rejection_reason=NULL WHERE seller_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sellerId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Seller approved successfully!");
            loadSellers();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void rejectSeller(int sellerId, String reason) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "UPDATE sellers SET is_approved=FALSE, rejection_reason=? WHERE seller_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, reason);
            ps.setInt(2, sellerId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Seller rejected!");
            loadSellers();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
