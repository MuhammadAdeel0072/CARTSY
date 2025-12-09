import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class SearchProductsDialog extends JDialog {
    private int customerId;

    public SearchProductsDialog(JFrame parent, int customerId) {
        super(parent, "Search Products", true);
        this.customerId = customerId;
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        mainPanel.setPreferredSize(new Dimension(900, 600));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Search Products");
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

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);

        JTextField searchField = new JTextField(30);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(primary);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchBtn.setFocusPainted(false);

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        // Results panel
        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new GridLayout(0, 3, 15, 15));
        resultsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Search action
        searchBtn.addActionListener(e -> searchProducts(resultsPanel, searchField.getText().trim()));
        searchField.addActionListener(e -> searchBtn.doClick());

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(searchPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);

        // Initial load
        searchProducts(resultsPanel, "");

        setVisible(true);
    }

    private void searchProducts(JPanel resultsPanel, String searchTerm) {
        resultsPanel.removeAll();

        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder query = new StringBuilder(
                    "SELECT product_id, name, description, price, quantity, category FROM products WHERE is_active=TRUE"
            );

            if (!searchTerm.isEmpty()) {
                query.append(" AND (name LIKE ? OR description LIKE ?)");
            }

            query.append(" ORDER BY name ASC");

            PreparedStatement ps = conn.prepareStatement(query.toString());
            if (!searchTerm.isEmpty()) {
                String likePattern = "%" + searchTerm + "%";
                ps.setString(1, likePattern);
                ps.setString(2, likePattern);
            }

            ResultSet rs = ps.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                int productId = rs.getInt("product_id");
                String name = rs.getString("name");
                String desc = rs.getString("description");
                double price = rs.getDouble("price");
                int qty = rs.getInt("quantity");
                String cat = rs.getString("category");

                JPanel productCard = createProductCard(productId, name, desc, price, qty, cat);
                resultsPanel.add(productCard);
            }

            if (!found) {
                JLabel noResults = new JLabel("No products found");
                noResults.setFont(new Font("Segoe UI", Font.BOLD, 18));
                noResults.setForeground(Color.GRAY);
                resultsPanel.add(noResults);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JPanel createProductCard(int productId, String name, String desc, double price, int qty, String category) {
        Color primary = new Color(0, 153, 204);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(245, 247, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setPreferredSize(new Dimension(250, 200));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLabel.setForeground(primary);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel categoryLabel = new JLabel("[" + category + "]");
        categoryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        categoryLabel.setForeground(Color.GRAY);
        categoryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLabel = new JLabel("PKR " + price);
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        priceLabel.setForeground(new Color(0, 120, 0));
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stockLabel = new JLabel("Stock: " + qty);
        stockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stockLabel.setForeground(qty > 10 ? new Color(0, 120, 0) : Color.RED);
        stockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton detailsBtn = new JButton("View Details");
        detailsBtn.setBackground(primary);
        detailsBtn.setForeground(Color.WHITE);
        detailsBtn.setFocusPainted(false);
        detailsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsBtn.addActionListener(e -> new ProductDetailsDialog(this, customerId, productId));

        card.add(nameLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(categoryLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(priceLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(stockLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(detailsBtn);

        return card;
    }
}
