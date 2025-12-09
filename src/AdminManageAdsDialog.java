import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class AdminManageAdsDialog extends JDialog {
    private DefaultTableModel model;
    private JTextField searchField;
    private JComboBox<String> filterBox;

    public AdminManageAdsDialog(JFrame parent) {
        super(parent, "Manage Advertisements", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        mainPanel.setPreferredSize(new Dimension(1000, 550));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Advertisement Management");
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
        filterBox = new JComboBox<>(new String[]{"All", "Approved", "Rejected", "Active", "Inactive"});
        JButton searchBtn = new JButton("Search");

        searchBtn.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            String filter = (String) filterBox.getSelectedItem();
            loadAds(keyword, filter);
        });

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Filter:"));
        searchPanel.add(filterBox);
        searchPanel.add(searchBtn);

        topPanel.add(searchPanel, BorderLayout.SOUTH);

        String[] columns = {"Ad ID", "Seller", "Title", "Start Date", "End Date", "Status", "Approved"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        loadAds("", "All");

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

        JButton activateBtn = new JButton("Activate");
        activateBtn.setBackground(primary);
        activateBtn.setForeground(Color.WHITE);
        activateBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        activateBtn.setFocusPainted(false);

        JButton deactivateBtn = new JButton("Deactivate");
        deactivateBtn.setBackground(new Color(150, 150, 150));
        deactivateBtn.setForeground(Color.WHITE);
        deactivateBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deactivateBtn.setFocusPainted(false);

        JButton viewBtn = new JButton("View Details");
        viewBtn.setBackground(new Color(100, 150, 200));
        viewBtn.setForeground(Color.WHITE);
        viewBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        viewBtn.setFocusPainted(false);

        // Button actions
        approveBtn.addActionListener(e -> updateSelectedAd(table, true));
        rejectBtn.addActionListener(e -> updateSelectedAd(table, false));
        activateBtn.addActionListener(e -> updateAdStatus(table, true));
        deactivateBtn.addActionListener(e -> updateAdStatus(table, false));
        viewBtn.addActionListener(e -> viewSelectedAd(table));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        btnPanel.add(approveBtn);
        btnPanel.add(rejectBtn);
        btnPanel.add(activateBtn);
        btnPanel.add(deactivateBtn);
        btnPanel.add(viewBtn);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void loadAds(String keyword, String filter) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder query = new StringBuilder(
                    "SELECT a.ad_id, u.name as seller_name, a.ad_title, a.start_date, a.end_date, " +
                            "a.is_active, a.is_approved " +
                            "FROM ads a " +
                            "JOIN sellers s ON a.seller_id = s.seller_id " +
                            "JOIN users u ON s.user_id = u.id WHERE 1=1 "
            );

            if (keyword != null && !keyword.isEmpty()) {
                query.append("AND (u.name LIKE ? OR a.ad_title LIKE ?) ");
            }

            if (filter != null && !filter.equals("All")) {
                switch (filter) {
                    case "Approved":
                        query.append("AND a.is_approved=1 ");
                        break;
                    case "Rejected":
                        query.append("AND a.is_approved=0 ");
                        break;
                    case "Active":
                        query.append("AND a.is_active=1 ");
                        break;
                    case "Inactive":
                        query.append("AND a.is_active=0 ");
                        break;
                }
            }

            query.append("ORDER BY a.ad_id DESC");

            PreparedStatement ps = conn.prepareStatement(query.toString());

            if (keyword != null && !keyword.isEmpty()) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("ad_id"),
                        rs.getString("seller_name"),
                        rs.getString("ad_title"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getBoolean("is_active") ? "Active" : "Inactive",
                        rs.getBoolean("is_approved") ? "Yes" : "Pending"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void updateSelectedAd(JTable table, boolean approved) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an ad!");
            return;
        }
        int adId = (int) model.getValueAt(row, 0);
        updateAdApproval(adId, approved);
    }

    private void updateAdApproval(int adId, boolean approved) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "UPDATE ads SET is_approved=?, approval_date=NOW() WHERE ad_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBoolean(1, approved);
            ps.setInt(2, adId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, approved ? "Ad approved!" : "Ad rejected!");
            loadAds(searchField.getText().trim(), (String) filterBox.getSelectedItem());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void updateAdStatus(JTable table, boolean active) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an ad!");
            return;
        }
        int adId = (int) model.getValueAt(row, 0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "UPDATE ads SET is_active=? WHERE ad_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBoolean(1, active);
            ps.setInt(2, adId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, active ? "Ad activated!" : "Ad deactivated!");
            loadAds(searchField.getText().trim(), (String) filterBox.getSelectedItem());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void viewSelectedAd(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an ad!");
            return;
        }
        int adId = (int) model.getValueAt(row, 0);
        viewAdDetails(adId);
    }

    private void viewAdDetails(int adId) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT a.ad_title, a.ad_content, a.start_date, a.end_date, " +
                    "a.is_active, a.is_approved, u.name as seller_name " +
                    "FROM ads a " +
                    "JOIN sellers s ON a.seller_id = s.seller_id " +
                    "JOIN users u ON s.user_id = u.id " +
                    "WHERE a.ad_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, adId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String details = String.format(
                        "Ad Title: %s\n\n" +
                                "Seller: %s\n\n" +
                                "Content:\n%s\n\n" +
                                "Duration: %s to %s\n\n" +
                                "Status: %s\n" +
                                "Approved: %s",
                        rs.getString("ad_title"),
                        rs.getString("seller_name"),
                        rs.getString("ad_content"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getBoolean("is_active") ? "Active" : "Inactive",
                        rs.getBoolean("is_approved") ? "Yes" : "No"
                );
                JOptionPane.showMessageDialog(this, details, "Ad Details", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
