import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CustomerCartDialog extends JDialog {
    private DefaultTableModel model;
    private JLabel cartTotalLabel;
    private JTable table;

    public CustomerCartDialog(JFrame parent, int customerId) {
        super(parent, "My Cart", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        mainPanel.setPreferredSize(new Dimension(800, 440));

        // Top bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel title = new JLabel("My Cart");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
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

        // Table
        String[] columns = {"Product ID", "Name", "Price", "Quantity"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        loadCart(customerId);

        JScrollPane scrollPane = new JScrollPane(table);

        // Buttons
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.setBackground(primary);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteBtn.setFocusPainted(false);

        JButton orderBtn = new JButton("Order Selected");
        orderBtn.setBackground(new Color(0, 180, 80));
        orderBtn.setForeground(Color.WHITE);
        orderBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        orderBtn.setFocusPainted(false);

        JButton increaseQtyBtn = new JButton("+");
        increaseQtyBtn.setBackground(new Color(100, 150, 200));
        increaseQtyBtn.setForeground(Color.WHITE);
        increaseQtyBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        increaseQtyBtn.setFocusPainted(false);
        increaseQtyBtn.setPreferredSize(new Dimension(50, 30));

        JButton decreaseQtyBtn = new JButton("-");
        decreaseQtyBtn.setBackground(new Color(220, 150, 100));
        decreaseQtyBtn.setForeground(Color.WHITE);
        decreaseQtyBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        decreaseQtyBtn.setFocusPainted(false);
        decreaseQtyBtn.setPreferredSize(new Dimension(50, 30));

        cartTotalLabel = new JLabel("Cart Total: PKR 0.00");
        cartTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cartTotalLabel.setForeground(new Color(0, 120, 0));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Row selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateCartTotal();
        });

        // Delete selected
        deleteBtn.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                statusLabel.setText("Select a product to delete!");
                return;
            }
            try (Connection conn = DBConnection.getConnection()) {
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    int row = selectedRows[i];
                    int productId = (int) model.getValueAt(row, 0);
                    String query = "DELETE FROM cart WHERE customer_id=? AND product_id=?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setInt(1, customerId);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                    model.removeRow(row);
                }
                statusLabel.setForeground(new Color(0, 120, 0));
                statusLabel.setText("Selected items removed!");
                updateCartTotal();
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Increase quantity
        increaseQtyBtn.addActionListener(e -> updateQuantity(customerId, 1, statusLabel));

        // Decrease quantity
        decreaseQtyBtn.addActionListener(e -> updateQuantity(customerId, -1, statusLabel));

        // Order selected → CHECK DEFAULT ADDRESS BEFORE ORDERING
        orderBtn.addActionListener(e -> processOrder(customerId, statusLabel));

        // Bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(deleteBtn);
        btnPanel.add(orderBtn);
        btnPanel.add(increaseQtyBtn);
        btnPanel.add(decreaseQtyBtn);

        bottomPanel.add(btnPanel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        bottomPanel.add(cartTotalLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        bottomPanel.add(statusLabel);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void loadCart(int customerId) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT c.product_id, p.name, p.price, c.quantity FROM cart c JOIN products p ON c.product_id = p.product_id WHERE c.customer_id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void updateCartTotal() {
        double total = 0.0;
        int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            double price = (double) model.getValueAt(row, 2);
            int qty = (int) model.getValueAt(row, 3);
            total += price * qty;
        }
        cartTotalLabel.setText(String.format("Cart Total: PKR %.2f", total));
    }

    private void updateQuantity(int customerId, int delta, JLabel statusLabel) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            statusLabel.setText("Select a product to update quantity!");
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            for (int row : selectedRows) {
                int productId = (int) model.getValueAt(row, 0);
                int currentQty = (int) model.getValueAt(row, 3);
                int newQty = currentQty + delta;
                if (newQty < 1) {
                    statusLabel.setText("Quantity cannot be less than 1! Use Delete instead.");
                    continue;
                }

                // Check stock
                String stockCheck = "SELECT quantity FROM products WHERE product_id=?";
                PreparedStatement psStock = conn.prepareStatement(stockCheck);
                psStock.setInt(1, productId);
                ResultSet rsStock = psStock.executeQuery();
                if (rsStock.next()) {
                    int availableQty = rsStock.getInt("quantity");
                    if (newQty > availableQty) {
                        statusLabel.setText("Cannot increase beyond stock: " + availableQty);
                        continue;
                    }
                }

                // Update cart
                String updateQuery = "UPDATE cart SET quantity = ? WHERE customer_id=? AND product_id=?";
                PreparedStatement ps = conn.prepareStatement(updateQuery);
                ps.setInt(1, newQty);
                ps.setInt(2, customerId);
                ps.setInt(3, productId);
                ps.executeUpdate();
                model.setValueAt(newQty, row, 3);
            }
            statusLabel.setForeground(new Color(0, 120, 0));
            statusLabel.setText("Quantity updated!");
            updateCartTotal();
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    // --- NEW METHOD TO PROCESS ORDER WITH DEFAULT ADDRESS CONFIRMATION ---
    private void processOrder(int customerId, JLabel statusLabel) {
        try (Connection conn = DBConnection.getConnection()) {
            // 1. Count addresses
            String countQuery = "SELECT COUNT(*) AS count FROM addresses WHERE user_id=?";
            PreparedStatement countPs = conn.prepareStatement(countQuery);
            countPs.setInt(1, customerId);
            ResultSet countRs = countPs.executeQuery();
            int addressCount = 0;
            if (countRs.next()) {
                addressCount = countRs.getInt("count");
            }

            if (addressCount == 0) {
                JOptionPane.showMessageDialog(this,
                        "You have no addresses! Please add a default address first.",
                        "No Address", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. Check default address
            String defaultQuery = "SELECT address_id, full_address FROM addresses WHERE user_id=? AND is_default=TRUE";
            PreparedStatement defaultPs = conn.prepareStatement(defaultQuery);
            defaultPs.setInt(1, customerId);
            ResultSet defaultRs = defaultPs.executeQuery();

            if (!defaultRs.next()) {
                JOptionPane.showMessageDialog(this,
                        "You have addresses but none is set as default! Please set a default address.",
                        "No Default Address", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int defaultAddressId = defaultRs.getInt("address_id");
            String fullAddress = defaultRs.getString("full_address");

            // 3. Confirm with Yes/No
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Your order will be placed to the following default address:\n" + fullAddress + "\n\nProceed?",
                    "Confirm Default Address",
                    JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) return; // Stop if user says No

            // 4. Place order for selected items
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                statusLabel.setText("Select products to order!");
                return;
            }

            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int row = selectedRows[i];
                int productId = (int) model.getValueAt(row, 0);
                int qty = (int) model.getValueAt(row, 3);
                double price = (double) model.getValueAt(row, 2);
                double total = price * qty;

                // Check stock
                String stockCheck = "SELECT quantity, seller_id FROM products WHERE product_id=?";
                PreparedStatement stockPs = conn.prepareStatement(stockCheck);
                stockPs.setInt(1, productId);
                ResultSet stockRs = stockPs.executeQuery();
                if (!stockRs.next()) continue;
                int availableQty = stockRs.getInt("quantity");
                int sellerId = stockRs.getInt("seller_id");
                if (availableQty < qty) {
                    statusLabel.setText("Insufficient stock for " + model.getValueAt(row, 1));
                    continue;
                }

                // Insert order
                String orderQuery = "INSERT INTO orders (customer_id, address_id, total, payment_type) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, customerId);
                ps.setInt(2, defaultAddressId);
                ps.setDouble(3, total);
                ps.setString(4, "Cash on Delivery");
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                int orderId = 0;
                if (rs.next()) orderId = rs.getInt(1);

                // Insert order item
                String itemQuery = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
                PreparedStatement itemPs = conn.prepareStatement(itemQuery);
                itemPs.setInt(1, orderId);
                itemPs.setInt(2, productId);
                itemPs.setInt(3, qty);
                itemPs.setDouble(4, price);
                itemPs.executeUpdate();

                // Update product quantity
                String updateStock = "UPDATE products SET quantity = quantity - ? WHERE product_id = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateStock);
                updatePs.setInt(1, qty);
                updatePs.setInt(2, productId);
                updatePs.executeUpdate();

                // Insert into sales
                String salesQuery = "INSERT INTO sales (product_id, seller_id, customer_id, quantity, total_price) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement salesPs = conn.prepareStatement(salesQuery, Statement.RETURN_GENERATED_KEYS);
                salesPs.setInt(1, productId);
                salesPs.setInt(2, sellerId);
                salesPs.setInt(3, customerId);
                salesPs.setInt(4, qty);
                salesPs.setDouble(5, total);
                salesPs.executeUpdate();

                ResultSet saleRs = salesPs.getGeneratedKeys();
                if (saleRs.next()) {
                    int saleId = saleRs.getInt(1);
                    String paymentQuery = "INSERT INTO payments (sale_id, payment_method, card_number) VALUES (?, 'CashOnDelivery', NULL)";
                    PreparedStatement paymentPs = conn.prepareStatement(paymentQuery);
                    paymentPs.setInt(1, saleId);
                    paymentPs.executeUpdate();
                }

                // Remove from cart table
                String delCart = "DELETE FROM cart WHERE customer_id=? AND product_id=?";
                PreparedStatement delPs = conn.prepareStatement(delCart);
                delPs.setInt(1, customerId);
                delPs.setInt(2, productId);
                delPs.executeUpdate();

                model.removeRow(row);
            }

            table.clearSelection();
            cartTotalLabel.setText("Cart Total: PKR 0.00");
            statusLabel.setForeground(new Color(0, 120, 0));
            statusLabel.setText("Order placed successfully!");

        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }
}
