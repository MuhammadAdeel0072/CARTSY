import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class RunAdsDialog extends JDialog {

    private int sellerId;
    private DefaultTableModel tableModel;
    private JTable adsTable;
    private JTextField adContentField;
    private JTextField daysField;
    private JLabel statusLabel;
    private JComboBox<String> filterCombo;
    private Timer autoExpireTimer;

    private final Color PRIMARY = new Color(0, 153, 204);
    private JPanel chartPanel;

    public RunAdsDialog(JFrame parent, int sellerId) {
        super(parent, "Manage Ads", true);
        this.sellerId = sellerId;

        setUndecorated(true);
        setSize(1100, 700); // Increased size for better UX
        setLocationRelativeTo(parent);

        initUI();
        loadAds();
        loadAnalytics();

        // Auto-expire check every 10 seconds
        autoExpireTimer = new Timer();
        autoExpireTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAndExpireAds();
            }
        }, 0, 10000);

        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createLineBorder(PRIMARY, 2, true));
        add(mainPanel);

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        JLabel title = new JLabel("Run & Manage Ads");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);

        JButton closeBtn = new JButton("X");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setForeground(PRIMARY);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> {
            autoExpireTimer.cancel();
            dispose();
        });
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.RED); }
            public void mouseExited(MouseEvent e) { closeBtn.setForeground(PRIMARY); }
        });

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center panel with table and charts
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setDividerLocation(300);

        // Table
        tableModel = new DefaultTableModel(new String[]{"Ad ID", "Content", "Start Date", "End Date", "Status", "Boosted"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        adsTable = new JTable(tableModel);
        adsTable.setRowHeight(32);
        adsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        JScrollPane scrollPane = new JScrollPane(adsTable);

        adsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) table.getValueAt(row, 4);
                if (status.equals("Active")) c.setBackground(new Color(200, 255, 200));
                else if (status.equals("Expired")) c.setBackground(new Color(255, 200, 200));
                else c.setBackground(Color.WHITE);
                if (isSelected) c.setBackground(new Color(180, 220, 255));
                return c;
            }
        });

        centerSplit.setTopComponent(scrollPane);

        // Chart panel
        chartPanel = new JPanel(new GridLayout(1, 2));
        centerSplit.setBottomComponent(chartPanel);

        mainPanel.add(centerSplit, BorderLayout.CENTER);

        // Bottom panel - controls
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(new EmptyBorder(10,10,10,10));
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Input & buttons
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));

        adContentField = new JTextField();
        adContentField.setMaximumSize(new Dimension(400, 32));
        daysField = new JTextField();
        daysField.setMaximumSize(new Dimension(100, 32));

        JButton runBtn = new JButton("Run Ad");
        JButton editBtn = new JButton("Edit Ad");
        JButton deleteBtn = new JButton("Delete Ad");
        JButton boostBtn = new JButton("Boost Ad");

        JButton[] buttons = {runBtn, editBtn, deleteBtn, boostBtn};
        for (JButton b : buttons) {
            b.setBackground(PRIMARY);
            b.setForeground(Color.WHITE);
            b.setFont(new Font("Segoe UI", Font.BOLD, 14));
            b.setFocusPainted(false);
        }

        inputPanel.add(new JLabel("Ad Content: "));
        inputPanel.add(adContentField);
        inputPanel.add(Box.createRigidArea(new Dimension(10,0)));
        inputPanel.add(new JLabel("Days: "));
        inputPanel.add(daysField);
        inputPanel.add(Box.createRigidArea(new Dimension(10,0)));
        inputPanel.add(runBtn);
        inputPanel.add(Box.createRigidArea(new Dimension(5,0)));
        inputPanel.add(editBtn);
        inputPanel.add(Box.createRigidArea(new Dimension(5,0)));
        inputPanel.add(deleteBtn);
        inputPanel.add(Box.createRigidArea(new Dimension(5,0)));
        inputPanel.add(boostBtn);

        bottomPanel.add(inputPanel);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterCombo = new JComboBox<>(new String[]{"All", "Active", "Expired", "Boosted"});
        filterPanel.add(new JLabel("Filter Ads: "));
        filterPanel.add(filterCombo);
        bottomPanel.add(filterPanel);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bottomPanel.add(statusLabel);

        // Event listeners
        runBtn.addActionListener(e -> runAd());
        editBtn.addActionListener(e -> editAd());
        deleteBtn.addActionListener(e -> deleteAd());
        boostBtn.addActionListener(e -> boostAd());
        filterCombo.addActionListener(e -> {
            loadAds();
            loadAnalytics();
        });
    }

    private void runAd() {
        String content = adContentField.getText().trim();
        String daysStr = daysField.getText().trim();
        statusLabel.setForeground(Color.RED);

        if (content.isEmpty() || daysStr.isEmpty()) {
            statusLabel.setText("All fields are required!");
            return;
        }
        if (!content.matches("^[A-Za-z ]+$")) {
            statusLabel.setText("Ad content letters only!");
            return;
        }
        if (!daysStr.matches("^[0-9]+$")) {
            statusLabel.setText("Days must be number!");
            return;
        }
        int days = Integer.parseInt(daysStr);
        if (days > 30) { statusLabel.setText("Max 30 days without boost"); return; }

        try (Connection conn = DBConnection.getConnection()) {
            String query = "INSERT INTO ads (seller_id, ad_content, start_date, end_date, is_active, is_boosted) " +
                    "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), 1, 0)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sellerId);
            ps.setString(2, content);
            ps.setInt(3, days);
            ps.executeUpdate();

            statusLabel.setForeground(new Color(0,120,0));
            statusLabel.setText("Ad running successfully!");
            adContentField.setText("");
            daysField.setText("");
            loadAds();
            loadAnalytics();
        } catch (SQLException ex) { statusLabel.setText("Error: " + ex.getMessage()); }
    }

    private void editAd() {
        int row = adsTable.getSelectedRow();
        if (row == -1) {
            statusLabel.setText("Select an ad to edit!");
            return;
        }
        String status = (String) tableModel.getValueAt(row, 4);
        if (status.equals("Expired")) {
            statusLabel.setText("Cannot edit expired ad!");
            return;
        }

        int adId = (int) tableModel.getValueAt(row, 0);
        String currentContent = (String) tableModel.getValueAt(row, 1);
        String currentEndDate = (String) tableModel.getValueAt(row, 3);

        // Popup dialog
        JTextField newContentField = new JTextField(currentContent);
        JTextField newDaysField = new JTextField();
        newDaysField.setText(""); // user inputs new days
        JPanel panel = new JPanel(new GridLayout(2,2,5,5));
        panel.add(new JLabel("New Content:"));
        panel.add(newContentField);
        panel.add(new JLabel("Extend Days:"));
        panel.add(newDaysField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Ad", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String newContent = newContentField.getText().trim();
        String newDaysStr = newDaysField.getText().trim();
        if (newContent.isEmpty() || newDaysStr.isEmpty()) {
            statusLabel.setText("All fields are required!");
            return;
        }
        if (!newContent.matches("^[A-Za-z ]+$")) {
            statusLabel.setText("Ad content letters only!");
            return;
        }
        if (!newDaysStr.matches("^[0-9]+$")) {
            statusLabel.setText("Days must be number!");
            return;
        }
        int newDays = Integer.parseInt(newDaysStr);
        if (newDays < 1 || newDays > 30) {
            statusLabel.setText("Days must be between 1 and 30!");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE ads SET ad_content=?, end_date=DATE_ADD(end_date, INTERVAL ? DAY) WHERE ad_id=? AND seller_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newContent);
            ps.setInt(2, newDays);
            ps.setInt(3, adId);
            ps.setInt(4, sellerId);
            ps.executeUpdate();
            statusLabel.setForeground(new Color(0,120,0));
            statusLabel.setText("Ad updated successfully!");
            loadAds();
            loadAnalytics();
        } catch (SQLException ex) { statusLabel.setText("Error: "+ex.getMessage()); }
    }

    private void deleteAd() {
        int row = adsTable.getSelectedRow();
        if (row == -1) {
            statusLabel.setText("Select an ad to delete!");
            return;
        }
        int adId = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this ad?", "Delete Ad", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM ads WHERE ad_id=? AND seller_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, adId);
            ps.setInt(2, sellerId);
            ps.executeUpdate();
            statusLabel.setForeground(new Color(0,120,0));
            statusLabel.setText("Ad deleted successfully!");
            loadAds();
            loadAnalytics();
        } catch (SQLException ex) { statusLabel.setText("Error: "+ex.getMessage()); }
    }

    private void boostAd() {
        int row = adsTable.getSelectedRow();
        if (row == -1) {
            statusLabel.setText("Select an ad to boost!");
            return;
        }
        int adId = (int) tableModel.getValueAt(row, 0);

        // Ask for boost days
        String input = JOptionPane.showInputDialog(this, "Enter boost days (1-7):");
        if (input == null) return;
        if (!input.matches("^[1-7]$")) {
            statusLabel.setText("Boost days must be 1-7!");
            return;
        }
        int boostDays = Integer.parseInt(input);

        try (Connection conn = DBConnection.getConnection()) {
            String query = "UPDATE ads SET is_boosted=1, end_date=DATE_ADD(end_date, INTERVAL ? DAY) WHERE ad_id=? AND seller_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, boostDays);
            ps.setInt(2, adId);
            ps.setInt(3, sellerId);
            ps.executeUpdate();
            statusLabel.setForeground(new Color(0,120,0));
            statusLabel.setText("Ad boosted for "+boostDays+" days!");
            loadAds();
            loadAnalytics();
        } catch (SQLException ex) { statusLabel.setText("Error: "+ex.getMessage()); }
    }

    private void loadAds() {
        tableModel.setRowCount(0);
        String filter = (String) filterCombo.getSelectedItem();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT ad_id, ad_content, start_date, end_date, is_active, is_boosted FROM ads WHERE seller_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sellerId);
            ResultSet rs = ps.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            while (rs.next()) {
                String status = rs.getBoolean("is_active") ? "Active" : "Expired";
                boolean boosted = rs.getBoolean("is_boosted");

                if (!filter.equals("All")) {
                    if (filter.equals("Active") && !status.equals("Active")) continue;
                    if (filter.equals("Expired") && !status.equals("Expired")) continue;
                    if (filter.equals("Boosted") && !boosted) continue;
                }

                tableModel.addRow(new Object[]{
                        rs.getInt("ad_id"),
                        rs.getString("ad_content"),
                        sdf.format(rs.getTimestamp("start_date")),
                        sdf.format(rs.getTimestamp("end_date")),
                        status,
                        boosted ? "Yes" : "No"
                });
            }
        } catch (SQLException ex) { statusLabel.setText("Error loading ads: " + ex.getMessage()); }
    }

    private void checkAndExpireAds() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE ads SET is_active=0 WHERE end_date < NOW() AND seller_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sellerId);
            int updated = ps.executeUpdate();
            if (updated > 0) SwingUtilities.invokeLater(() -> {
                loadAds();
                loadAnalytics();
            });
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void loadAnalytics() {
        chartPanel.removeAll();
        int total=0, active=0, expired=0, boosted=0;

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT is_active, is_boosted FROM ads WHERE seller_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                total++;
                if (rs.getBoolean("is_active")) active++;
                else expired++;
                if (rs.getBoolean("is_boosted")) boosted++;
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        // Pie chart - Active vs Expired
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        pieDataset.setValue("Active", active);
        pieDataset.setValue("Expired", expired);
        JFreeChart pieChart = ChartFactory.createPieChart("Active vs Expired Ads", pieDataset, true, true, false);
        ChartPanel piePanel = new ChartPanel(pieChart);

        // Bar chart - Total vs Boosted
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        barDataset.addValue(total, "Ads", "Total");
        barDataset.addValue(boosted, "Ads", "Boosted");
        JFreeChart barChart = ChartFactory.createBarChart("Boosted Ads", "Type", "Count", barDataset);
        ChartPanel barPanel = new ChartPanel(barChart);

        chartPanel.add(piePanel);
        chartPanel.add(barPanel);
        chartPanel.revalidate();
        chartPanel.repaint();
    }
}
