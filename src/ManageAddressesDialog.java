import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class ManageAddressesDialog extends JDialog {
    private int customerId;
    private DefaultTableModel model;

    public ManageAddressesDialog(JFrame parent, int customerId) {
        super(parent, "Manage Addresses", true);
        this.customerId = customerId;
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        mainPanel.setPreferredSize(new Dimension(800, 500));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("My Delivery Addresses");
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

        String[] columns = {"ID", "Label", "Address", "City", "Phone", "Default"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        loadAddresses();

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JButton addBtn = new JButton("Add New Address");
        addBtn.setBackground(primary);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addBtn.setFocusPainted(false);

        JButton setDefaultBtn = new JButton("Set as Default");
        setDefaultBtn.setBackground(new Color(0, 180, 80));
        setDefaultBtn.setForeground(Color.WHITE);
        setDefaultBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        setDefaultBtn.setFocusPainted(false);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(new Color(220, 80, 80));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteBtn.setFocusPainted(false);

        addBtn.addActionListener(e -> new AddAddressDialog(this, customerId, this));

        setDefaultBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select an address!");
                return;
            }
            int addressId = (int) model.getValueAt(row, 0);
            setDefaultAddress(addressId);
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select an address!");
                return;
            }
            int addressId = (int) model.getValueAt(row, 0);
            deleteAddress(addressId);
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        btnPanel.add(addBtn);
        btnPanel.add(setDefaultBtn);
        btnPanel.add(deleteBtn);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    public void loadAddresses() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT address_id, address_label, full_address, city, phone, is_default FROM addresses WHERE user_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("address_id"),
                        rs.getString("address_label"),
                        rs.getString("full_address"),
                        rs.getString("city"),
                        rs.getString("phone"),
                        rs.getBoolean("is_default") ? "Yes" : "No"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void setDefaultAddress(int addressId) {
        try (Connection conn = DBConnection.getConnection()) {
            String resetQuery = "UPDATE addresses SET is_default=FALSE WHERE user_id=?";
            PreparedStatement resetPs = conn.prepareStatement(resetQuery);
            resetPs.setInt(1, customerId);
            resetPs.executeUpdate();

            String setQuery = "UPDATE addresses SET is_default=TRUE WHERE address_id=?";
            PreparedStatement setPs = conn.prepareStatement(setQuery);
            setPs.setInt(1, addressId);
            setPs.executeUpdate();

            JOptionPane.showMessageDialog(this, "Default address updated!");
            loadAddresses();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void deleteAddress(int addressId) {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this address?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection()) {
                String query = "DELETE FROM addresses WHERE address_id=?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, addressId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Address deleted!");
                loadAddresses();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
}
