import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;

public class ImprovedChatDialog extends JDialog {
    private int currentUserId;
    private int chatWithUserId;
    private String chatWithName;
    private JTextArea chatArea;
    private JTextField messageField;
    private Timer refreshTimer;

    public ImprovedChatDialog(JFrame parent, int currentUserId, int chatWithUserId, String chatWithName) {
        super(parent, "Chat with " + chatWithName, false);
        this.currentUserId = currentUserId;
        this.chatWithUserId = chatWithUserId;
        this.chatWithName = chatWithName;
        
        setUndecorated(true);
        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary, 2, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        mainPanel.setPreferredSize(new Dimension(500, 600));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel title = new JLabel("Chat with " + chatWithName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(primary);

        JButton closeBtn = new JButton("X");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setForeground(primary);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> {
            stopRefresh();
            dispose();
        });

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(245, 245, 245));
        JScrollPane chatScroll = new JScrollPane(chatArea);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.setPreferredSize(new Dimension(0, 40));

        JButton sendBtn = new JButton("Send");
        sendBtn.setBackground(primary);
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendBtn.setFocusPainted(false);
        sendBtn.setPreferredSize(new Dimension(80, 40));

        sendBtn.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(chatScroll, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);

        loadChatHistory();
        markMessagesAsRead();
        startAutoRefresh();

        setVisible(true);
    }

    private void loadChatHistory() {
        chatArea.setText("");
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT c.message, c.sender_id, c.sent_at, u.name FROM chat c " +
                          "JOIN users u ON c.sender_id = u.id " +
                          "WHERE (c.sender_id=? AND c.receiver_id=?) OR (c.sender_id=? AND c.receiver_id=?) " +
                          "ORDER BY c.sent_at ASC";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, currentUserId);
            ps.setInt(2, chatWithUserId);
            ps.setInt(3, chatWithUserId);
            ps.setInt(4, currentUserId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String message = rs.getString("message");
                int senderId = rs.getInt("sender_id");
                String senderName = rs.getString("name");
                Timestamp sentAt = rs.getTimestamp("sent_at");

                String prefix = (senderId == currentUserId) ? "You" : senderName;
                chatArea.append(String.format("[%s] %s:\n%s\n\n", 
                    sentAt.toString().substring(11, 16), prefix, message));
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        } catch (Exception ex) {
            chatArea.append("Error loading chat: " + ex.getMessage() + "\n");
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String query = "INSERT INTO chat (sender_id, receiver_id, message) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, currentUserId);
            ps.setInt(2, chatWithUserId);
            ps.setString(3, message);
            ps.executeUpdate();

            messageField.setText("");
            loadChatHistory();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error sending message: " + ex.getMessage());
        }
    }

    private void markMessagesAsRead() {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "UPDATE chat SET is_read=TRUE WHERE sender_id=? AND receiver_id=? AND is_read=FALSE";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, chatWithUserId);
            ps.setInt(2, currentUserId);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    String currentText = chatArea.getText();
                    loadChatHistory();
                    markMessagesAsRead();
                });
            }
        }, 3000, 3000);
    }

    private void stopRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
}
