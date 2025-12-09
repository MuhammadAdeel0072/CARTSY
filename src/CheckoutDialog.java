import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class CheckoutDialog extends JDialog {
    public CheckoutDialog(JFrame parent, int customerId, DefaultTableModel cartModel) {
        super(parent, "Checkout", true);
        setUndecorated(true);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(18, 28, 18, 28)
        ));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setPreferredSize(new Dimension(400, 350));

        JLabel title = new JLabel("Checkout");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(primary);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel totalLabel = new JLabel();
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Calculate total
        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            Object priceObj = cartModel.getValueAt(i, 2);
            Object qtyObj = cartModel.getValueAt(i, 3);
            double price = (priceObj instanceof Number) ? ((Number) priceObj).doubleValue() : Double.parseDouble(priceObj.toString());
            int qty = (qtyObj instanceof Number) ? ((Number) qtyObj).intValue() : Integer.parseInt(qtyObj.toString());
            total += price * qty;
        }
        totalLabel.setText("Total: PKR " + total);

        // Load customer's addresses
        JComboBox<String> addressBox = new JComboBox<>();
        addressBox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        addressBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        
        try (Connection conn = DBConnection.getConnection()) {
            String addressQuery = "SELECT address_id, address_label, full_address, city FROM addresses WHERE user_id = ? ORDER BY is_default DESC";
            PreparedStatement addressPs = conn.prepareStatement(addressQuery);
            addressPs.setInt(1, customerId);
            ResultSet addressRs = addressPs.executeQuery();
            
            while (addressRs.next()) {
                int addressId = addressRs.getInt("address_id");
                String label = addressRs.getString("address_label");
                String address = addressRs.getString("full_address");
                String city = addressRs.getString("city");
                addressBox.addItem(addressId + " - " + label + " (" + city + ")");
            }
            
            if (addressBox.getItemCount() == 0) {
                addressBox.addItem("No address found - Add one first");
            }
        } catch (Exception ex) {
            addressBox.addItem("Error loading addresses");
        }

        String[] paymentOptions = {"Cash on Delivery", "Online Payment"};
        JComboBox<String> paymentBox = new JComboBox<>(paymentOptions);
        paymentBox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        paymentBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JTextField cardField = new JTextField();
        cardField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        cardField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cardField.setVisible(false);

        paymentBox.addActionListener(e -> {
            cardField.setVisible(paymentBox.getSelectedIndex() == 1);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        JButton payBtn = new JButton("Confirm & Pay");
        payBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        payBtn.setBackground(primary);
        payBtn.setForeground(Color.WHITE);
        payBtn.setFocusPainted(false);
        payBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        payBtn.addActionListener(e -> {
            // Recalculate total in case cart changed
            double currentTotal = 0;
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                Object priceObj = cartModel.getValueAt(i, 2);
                Object qtyObj = cartModel.getValueAt(i, 3);
                double price = (priceObj instanceof Number) ? ((Number) priceObj).doubleValue() : Double.parseDouble(priceObj.toString());
                int qty = (qtyObj instanceof Number) ? ((Number) qtyObj).intValue() : Integer.parseInt(qtyObj.toString());
                currentTotal += price * qty;
            }
            totalLabel.setText("Total: PKR " + currentTotal);

            // Validate address selection
            String selectedAddress = (String) addressBox.getSelectedItem();
            if (selectedAddress == null || selectedAddress.contains("No address") || selectedAddress.contains("Error loading")) {
                statusLabel.setText("Please add a delivery address first!");
                return;
            }
            
            // Extract address_id from selection
            int addressId = 0;
            try {
                addressId = Integer.parseInt(selectedAddress.split(" - ")[0]);
            } catch (Exception ex) {
                statusLabel.setText("Invalid address selected!");
                return;
            }

            String paymentType = (String) paymentBox.getSelectedItem();
            String cardInfo = cardField.getText().trim();
            if (paymentType.equals("Online Payment") && cardInfo.isEmpty()) {
                statusLabel.setText("Enter card info!");
                return;
            }
            if (cartModel.getRowCount() == 0) {
                statusLabel.setText("Cart is empty!");
                return;
            }
            try (Connection conn = DBConnection.getConnection()) {
                // FIX #3: Validate stock availability before processing order
                for (int i = 0; i < cartModel.getRowCount(); i++) {
                    int productId = Integer.parseInt(cartModel.getValueAt(i, 0).toString());
                    int requestedQty = Integer.parseInt(cartModel.getValueAt(i, 3).toString());
                    
                    String stockCheck = "SELECT quantity FROM products WHERE product_id=?";
                    PreparedStatement stockPs = conn.prepareStatement(stockCheck);
                    stockPs.setInt(1, productId);
                    ResultSet stockRs = stockPs.executeQuery();
                    
                    if (stockRs.next()) {
                        int availableQty = stockRs.getInt("quantity");
                        if (availableQty < requestedQty) {
                            statusLabel.setText("Insufficient stock for product ID: " + productId);
                            return;
                        }
                    } else {
                        statusLabel.setText("Product not found: " + productId);
                        return;
                    }
                }
                
                // Insert order with address
                String orderQuery = "INSERT INTO orders (customer_id, address_id, total, payment_type, card_info) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, customerId);
                ps.setInt(2, addressId);
                ps.setDouble(3, currentTotal);
                ps.setString(4, paymentType);
                ps.setString(5, paymentType.equals("Online Payment") ? cardInfo : null);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                int orderId = 0;
                if (rs.next()) orderId = rs.getInt(1);

                // Insert order items, update stock, insert sales, and clear cart
                for (int i = 0; i < cartModel.getRowCount(); i++) {
                    int productId = Integer.parseInt(cartModel.getValueAt(i, 0).toString());
                    int qty = Integer.parseInt(cartModel.getValueAt(i, 3).toString());
                    double price = Double.parseDouble(cartModel.getValueAt(i, 2).toString());
                    double totalPrice = price * qty;
                    
                    // Insert order item
                    String itemQuery = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
                    PreparedStatement itemPs = conn.prepareStatement(itemQuery);
                    itemPs.setInt(1, orderId);
                    itemPs.setInt(2, productId);
                    itemPs.setInt(3, qty);
                    itemPs.setDouble(4, price);
                    itemPs.executeUpdate();

                    // FIX #2: Decrease product quantity in inventory
                    String updateStock = "UPDATE products SET quantity = quantity - ? WHERE product_id = ?";
                    PreparedStatement updatePs = conn.prepareStatement(updateStock);
                    updatePs.setInt(1, qty);
                    updatePs.setInt(2, productId);
                    updatePs.executeUpdate();
                    
                    // FIX #5: Insert into inventory tracking table
                    String inventoryQuery = "INSERT INTO inventory (product_id, change_type, quantity) VALUES (?, 'remove', ?)";
                    PreparedStatement invPs = conn.prepareStatement(inventoryQuery);
                    invPs.setInt(1, productId);
                    invPs.setInt(2, qty);
                    invPs.executeUpdate();
                    
                    // FIX #1: Get seller_id and insert into sales table
                    String getSellerQuery = "SELECT seller_id FROM products WHERE product_id = ?";
                    PreparedStatement sellerPs = conn.prepareStatement(getSellerQuery);
                    sellerPs.setInt(1, productId);
                    ResultSet sellerRs = sellerPs.executeQuery();
                    
                    if (sellerRs.next()) {
                        int sellerId = sellerRs.getInt("seller_id");
                        
                        String salesQuery = "INSERT INTO sales (product_id, seller_id, customer_id, quantity, total_price) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement salesPs = conn.prepareStatement(salesQuery, Statement.RETURN_GENERATED_KEYS);
                        salesPs.setInt(1, productId);
                        salesPs.setInt(2, sellerId);
                        salesPs.setInt(3, customerId);
                        salesPs.setInt(4, qty);
                        salesPs.setDouble(5, totalPrice);
                        salesPs.executeUpdate();
                        
                        // FIX #4: Insert into payments table
                        ResultSet salesKeyRs = salesPs.getGeneratedKeys();
                        if (salesKeyRs.next()) {
                            int saleId = salesKeyRs.getInt(1);
                            String paymentQuery = "INSERT INTO payments (sale_id, payment_method, card_number) VALUES (?, ?, ?)";
                            PreparedStatement paymentPs = conn.prepareStatement(paymentQuery);
                            paymentPs.setInt(1, saleId);
                            paymentPs.setString(2, paymentType.equals("Online Payment") ? "Card" : "CashOnDelivery");
                            paymentPs.setString(3, paymentType.equals("Online Payment") ? cardInfo : null);
                            paymentPs.executeUpdate();
                        }
                    }

                    // Remove from cart
                    String delCart = "DELETE FROM cart WHERE customer_id=? AND product_id=?";
                    PreparedStatement delPs = conn.prepareStatement(delCart);
                    delPs.setInt(1, customerId);
                    delPs.setInt(2, productId);
                    delPs.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Order placed successfully!");
                dispose();
                new BillDialog(parent, orderId);
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        mainPanel.add(title);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        mainPanel.add(totalLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        mainPanel.add(new JLabel("Delivery Address:"));
        mainPanel.add(addressBox);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        mainPanel.add(new JLabel("Payment Method:"));
        mainPanel.add(paymentBox);
        mainPanel.add(cardField);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        mainPanel.add(payBtn);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(statusLabel);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}