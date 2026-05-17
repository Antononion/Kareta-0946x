package ru.gr0946x.ui;
import ru.gr0946x.net.Client;

import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class GraphicalUi extends JFrame {

    private final Client client;
    private DefaultListModel<String> onlineUsersModel;
    private JList<String> usersList;
    private JTextArea chatArea;
    private JTextField inputField;
    private JTextField searchField;
    private JLabel statusLabel;
    private String currentChatTarget = null;

    public GraphicalUi(Client client) {
        this.client = client;
        initUI();
        setupNetworkListeners();
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            client.sendData("LIST_USERS");
        });
    }

    private void initUI() {
        setTitle("Мессенджер Карета");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(160, 0));

        onlineUsersModel = new DefaultListModel<>();
        usersList = new JList<>(onlineUsersModel);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane usersScrollPane = new JScrollPane(usersList);
        usersScrollPane.setBorder(BorderFactory.createTitledBorder("Онлайн"));
        leftPanel.add(usersScrollPane, BorderLayout.CENTER);

        usersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = usersList.locationToIndex(e.getPoint());

                if (index < 0 || onlineUsersModel.isEmpty()) {
                    usersList.clearSelection();
                    currentChatTarget = null;
                    statusLabel.setText("Статус: Общий чат (BROADCAST)");
                    chatArea.append("📢 Переключено на общий чат\n");
                    return;
                }

                Rectangle cellBounds = usersList.getCellBounds(index, index);

                if (cellBounds != null && cellBounds.contains(e.getPoint())) {
                    String selected = onlineUsersModel.getElementAt(index);
                    usersList.setSelectedIndex(index);
                    currentChatTarget = selected;
                    statusLabel.setText("Статус: Личный чат с " + selected);

                    chatArea.append("🔒 Переключено на чат с " + selected + "\n");
                    chatArea.append("⏳ Загрузка истории...\n");
                    client.sendData("HISTORY:" + selected);

                } else {
                    usersList.clearSelection();
                    currentChatTarget = null;
                    statusLabel.setText("Статус: Общий чат (BROADCAST)");
                    chatArea.append("📢 Переключено на общий чат\n");
                }
            }
        });

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new BorderLayout(5, 5));

        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchField = new JTextField();
        JButton searchBtn = new JButton("🔍");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        searchPanel.setBorder(BorderFactory.createTitledBorder("Поиск в чате"));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputField = new JTextField();
        JButton sendBtn = new JButton("Отправить");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        controlsPanel.add(searchPanel, BorderLayout.NORTH);
        controlsPanel.add(inputPanel, BorderLayout.SOUTH);
        controlsPanel.add(new JSeparator(), BorderLayout.CENTER);

        rightPanel.add(controlsPanel, BorderLayout.SOUTH);

        statusLabel = new JLabel("Статус: Общий чат (BROADCAST)", SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        rightPanel.add(statusLabel, BorderLayout.NORTH);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        searchBtn.addActionListener(e -> searchMessages());
        searchField.addActionListener(e -> searchMessages());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void setupNetworkListeners() {
        client.addDataListener((data, type) -> {
            SwingUtilities.invokeLater(() -> handleServerResponse(data, type));
        });
    }

    private void handleServerResponse(String data, MessageType type) {

        if (type == MessageType.ERROR) {
            chatArea.append("❌ Ошибка: " + data + "\n");
        }
        else if (type == MessageType.INFO) {
            if (data.contains("Пользователи онлайн:")) {
                updateUsersList(data);
            } else if (data.startsWith("История") || data.startsWith("Результаты поиска")) {
                chatArea.append("\n📜 " + data + "\n");
            } else {
                chatArea.append("ℹ️ " + data + "\n");
            }
        }
        else if (type == MessageType.MESSAGE) {
            String[] parts = data.split(ProtocolConstants.AUTHOR_SEPARATOR, 2);
            if (parts.length == 2) {
                chatArea.append(parts[0] + ": " + parts[1] + "\n");
            } else {
                chatArea.append(data + "\n");
            }
        }
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void updateUsersList(String data) {
        String content = data.replace("Пользователи онлайн: ", "").trim();

        onlineUsersModel.clear();

        if (content.isEmpty() || content.equals("Нет пользователей онлайн")) {
            return;
        }

        String[] users = content.split(",\\s*");
        for (String user : users) {
            if (!user.isEmpty()) {
                onlineUsersModel.addElement(user.trim());
            }
        }
        usersList.revalidate();
        usersList.repaint();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        chatArea.append("Вы: " + text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());

        if (currentChatTarget != null) {
            client.sendData("PM:" + currentChatTarget + ":" + text);
        } else {
            client.sendData("BROADCAST:" + text);
        }
        inputField.setText("");
    }

    private void searchMessages() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        if (currentChatTarget != null) {
            client.sendData("SEARCH:" + currentChatTarget + ":" + query);
        } else {
            chatArea.append("️ Выберите собеседника в списке слева для поиска.\n");
        }
    }

    public void start() {
        setVisible(true);
    }
}