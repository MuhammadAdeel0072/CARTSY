import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ChatDialog extends JDialog {

    private int currentUserId;
    private String currentUserRole;

    private JList<UserItem> contactList;
    private DefaultListModel<UserItem> listModel;

    private JPanel chatPanel;
    private JScrollPane chatScroll;

    private JTextField inputField;
    private JButton sendBtn;

    private Timer refreshTimer;

    private int selectedUserId = -1;
    private boolean userScrolledUp = false;

    public ChatDialog(JFrame parent, int userId, String role, int initialContactId) {
        super(parent, "Chat", true);
        this.currentUserId = userId;
        this.currentUserRole = role;
        this.selectedUserId = initialContactId;

        initializeUI();
        loadContactList();

        if (selectedUserId != -1) {
            loadChat(selectedUserId);
            contactList.setSelectedValue(findUserItem(selectedUserId), true);
        }

        refreshTimer = new Timer(2000, e -> {
            loadContactList();
            if (selectedUserId != -1) loadChat(selectedUserId);
        });
        refreshTimer.start();

        setVisible(true);
    }

    public ChatDialog(JFrame parent, int userId, String role) {
        this(parent, userId, role, -1);
    }

    private void initializeUI() {
        setUndecorated(true);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Color primary = new Color(0, 153, 204);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(primary, 2, true));
        add(mainPanel);

        // Top bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        JLabel title = new JLabel("Chat");
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
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.RED); }
            public void mouseExited(MouseEvent e) { closeBtn.setForeground(primary); }
        });

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Left panel - contact list
        JPanel leftPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel(currentUserRole.equals("Seller") ? "Customers" : "Sellers");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerLabel.setBorder(new EmptyBorder(10,10,10,10));
        leftPanel.add(headerLabel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        contactList = new JList<>(listModel);
        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactList.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        JScrollPane listScroll = new JScrollPane(contactList);
        listScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        listScroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(180, 180, 180);
            }
        });

        leftPanel.add(listScroll, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(300, 0));
        mainPanel.add(leftPanel, BorderLayout.WEST);

        // Custom cell renderer
        contactList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            UserItem item = (UserItem) value;
            JPanel panel = new JPanel(new BorderLayout(10,5));
            panel.setBackground(isSelected ? new Color(200, 230, 255) : Color.WHITE);
            panel.setBorder(new EmptyBorder(5,5,5,5));

            JLabel nameLabel = new JLabel(item.name + (item.unread > 0 ? " (" + item.unread + ")" : ""));
            nameLabel.setFont(new Font("Segoe UI", item.unread > 0 ? Font.BOLD : Font.PLAIN, 20));
            nameLabel.setForeground(new Color(30,30,30));

            JLabel avatarLabel = new JLabel();
            if (item.profilePath != null && !item.profilePath.isEmpty()) {
                ImageIcon icon = new ImageIcon(new ImageIcon(item.profilePath)
                        .getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH));
                avatarLabel.setIcon(icon);
            }
            avatarLabel.setBorder(BorderFactory.createLineBorder(new Color(200,200,200), 1, true));

            panel.add(avatarLabel, BorderLayout.WEST);
            panel.add(nameLabel, BorderLayout.CENTER);

            // Hover effect
            panel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { panel.setBackground(new Color(220, 240, 255)); }
                public void mouseExited(java.awt.event.MouseEvent e) { panel.setBackground(isSelected ? new Color(200, 230, 255) : Color.WHITE); }
            });

            return panel;
        });

        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                UserItem selected = contactList.getSelectedValue();
                if (selected != null) {
                    selectedUserId = selected.userId;
                    loadChat(selectedUserId);
                }
            }
        });

        // Right panel - chat
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.WHITE);
        chatPanel.setBorder(new EmptyBorder(10,10,10,10));

        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = new Color(180,180,180); }
        });
        chatScroll.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int max = chatScroll.getVerticalScrollBar().getMaximum();
            int extent = chatScroll.getVerticalScrollBar().getModel().getExtent();
            userScrolledUp = e.getValue() + extent < max;
        });
        mainPanel.add(chatScroll, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5,5));
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        sendBtn = new JButton("Send");
        sendBtn.setBackground(primary);
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sendBtn.setFocusPainted(false);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        inputPanel.setBorder(new EmptyBorder(10,10,10,10));
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    private void loadContactList() {
        ArrayList<UserItem> tempList = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT u.id, u.name, u.profile_picture, " +
                    "SUM(CASE WHEN c.is_read = 0 AND c.receiver_id = ? THEN 1 ELSE 0 END) AS unread " +
                    "FROM chat c JOIN users u ON (CASE WHEN ? = c.receiver_id THEN c.sender_id ELSE c.receiver_id END) = u.id " +
                    "WHERE (c.sender_id = ? OR c.receiver_id = ?) AND u.role = ? " +
                    "GROUP BY u.id, u.name, u.profile_picture";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUserId);
            ps.setInt(2, currentUserId);
            ps.setInt(3, currentUserId);
            ps.setInt(4, currentUserId);
            ps.setString(5, currentUserRole.equals("Seller") ? "Customer" : "Seller");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id == currentUserId) continue;
                tempList.add(new UserItem(
                        id,
                        rs.getString("name"),
                        rs.getInt("unread"),
                        rs.getString("profile_picture")
                ));
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        listModel.clear();
        for (UserItem u : tempList) listModel.addElement(u);
    }

    private void loadChat(int otherUserId) {
        chatPanel.removeAll();
        ArrayList<MessageItem> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT sender_id, receiver_id, message, sent_at, is_read FROM chat " +
                         "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
                         "ORDER BY sent_at ASC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUserId);
            ps.setInt(2, otherUserId);
            ps.setInt(3, otherUserId);
            ps.setInt(4, currentUserId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(new MessageItem(
                        rs.getInt("sender_id"),
                        rs.getString("message"),
                        rs.getTimestamp("sent_at"),
                        rs.getBoolean("is_read")
                ));
            }

            String updateSql = "UPDATE chat SET is_read = 1 WHERE receiver_id = ? AND sender_id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(updateSql);
            psUpdate.setInt(1, currentUserId);
            psUpdate.setInt(2, otherUserId);
            psUpdate.executeUpdate();

        } catch (SQLException ex) { ex.printStackTrace(); }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");

        for (MessageItem m : messages) {
            JPanel bubble = new JPanel(new BorderLayout());
            bubble.setBackground(m.senderId == currentUserId ? new Color(0, 153, 204) : new Color(230,230,230));
            bubble.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200,200,200), 1, true),
                    new EmptyBorder(12,16,12,16)
            ));
            bubble.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
            bubble.setAlignmentX(m.senderId == currentUserId ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

            JLabel msgLabel = new JLabel("<html><body style='width: 400px; font-size:16px;'>" + m.message + "</body></html>");
            msgLabel.setForeground(m.senderId == currentUserId ? Color.WHITE : Color.BLACK);

            JLabel timeLabel = new JLabel(sdf.format(m.sentAt) + (m.senderId == currentUserId ? (m.isRead ? " ✓✓" : " ✓") : ""));
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            timeLabel.setForeground(Color.DARK_GRAY);

            bubble.add(msgLabel, BorderLayout.CENTER);
            bubble.add(timeLabel, BorderLayout.SOUTH);

            JPanel container = new JPanel(new FlowLayout(m.senderId == currentUserId ? FlowLayout.RIGHT : FlowLayout.LEFT));
            container.setOpaque(false);
            container.add(bubble);
            chatPanel.add(container);
        }

        chatPanel.revalidate();
        chatPanel.repaint();

        if (!userScrolledUp) {
            SwingUtilities.invokeLater(() -> chatScroll.getVerticalScrollBar().setValue(chatScroll.getVerticalScrollBar().getMaximum()));
        }
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty() || selectedUserId == -1) return;

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO chat(sender_id, receiver_id, message) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUserId);
            ps.setInt(2, selectedUserId);
            ps.setString(3, msg);
            ps.executeUpdate();
        } catch (SQLException ex) { ex.printStackTrace(); }

        inputField.setText("");
        loadChat(selectedUserId);
        loadContactList();
    }

    private UserItem findUserItem(int userId) {
        for (int i = 0; i < listModel.size(); i++) {
            UserItem u = listModel.get(i);
            if (u.userId == userId) return u;
        }
        return null;
    }

    private static class UserItem {
        int userId; String name; int unread; String profilePath;
        public UserItem(int id, String name, int unread, String profilePath) {
            this.userId = id; this.name = name; this.unread = unread; this.profilePath = profilePath;
        }
        @Override public String toString() { return name; }
    }

    private static class MessageItem {
        int senderId; String message; Timestamp sentAt; boolean isRead;
        public MessageItem(int senderId, String message, Timestamp sentAt, boolean isRead) {
            this.senderId = senderId; this.message = message; this.sentAt = sentAt; this.isRead = isRead;
        }
    }
}
