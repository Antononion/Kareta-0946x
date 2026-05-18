package ru.gr0946x.ui;

import ru.gr0946x.net.ChatCommand;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class GraphicalUi extends JFrame implements Ui {

    private Consumer<String> userDataListener;
    private DefaultListModel<String> onlineUsersModel;
    private JList<String> usersList;
    private JTextArea chatArea;
    private JTextField inputField;
    private JTextField searchField;
    private JLabel statusLabel;
    private String currentChatTarget = null;
    private final String myNick;

    public GraphicalUi(String myNick) {
        this.myNick = myNick;
        initUI();
    }

    private void initUI() {
        setTitle("Мессенджер Карета");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(160, 0));
        onlineUsersModel = new DefaultListModel<>();
        usersList = new JList<>(onlineUsersModel);
        usersList.setCellRenderer(new UserCellRenderer());
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
                    statusLabel.setText("Общий чат");
                    chatArea.append("📢 Переключено на общий чат\n");
                    return;
                }
                Rectangle cellBounds = usersList.getCellBounds(index, index);
                if (cellBounds != null && cellBounds.contains(e.getPoint())) {
                    String selected = onlineUsersModel.getElementAt(index);
                    usersList.setSelectedIndex(index);
                    currentChatTarget = selected;
                    statusLabel.setText("Личный чат с " + selected);
                    chatArea.append(" Переключено на чат с " + selected + "\n");
                    chatArea.append(" Загрузка истории...\n");
                    sendToNetwork(ChatCommand.HISTORY.name() + ":" + selected);
                } else {
                    usersList.clearSelection();
                    currentChatTarget = null;
                    statusLabel.setText("Общий чат");
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
        rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new BorderLayout(5, 5));

        // Поиск
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchField = new JTextField();
        JButton searchBtn = new JButton("🔍");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        searchPanel.setBorder(BorderFactory.createTitledBorder("Поиск в чате"));

        // Ввод
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputField = new JTextField();
        JButton sendBtn = new JButton("Отправить");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        controlsPanel.add(searchPanel, BorderLayout.NORTH);
        controlsPanel.add(inputPanel, BorderLayout.SOUTH);
        controlsPanel.add(new JSeparator(), BorderLayout.CENTER);
        rightPanel.add(controlsPanel, BorderLayout.SOUTH);

        statusLabel = new JLabel("Общий чат", SwingConstants.LEFT);
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
                dispose();
            }
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        if (currentChatTarget != null && currentChatTarget.equalsIgnoreCase(myNick)) {
            chatArea.append("⚠️ Нельзя отправить сообщение самому себе!\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            return;
        }

        chatArea.append("Вы: " + text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());

        if (currentChatTarget != null) {
            sendToNetwork(ChatCommand.PM.name() + ":" + currentChatTarget + ":" + text);
        } else {
            sendToNetwork(ChatCommand.BROADCAST.name() + ":" + text);
        }
        inputField.setText("");
    }

    private void searchMessages() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;
        if (currentChatTarget != null) {
            sendToNetwork(ChatCommand.SEARCH.name() + ":" + currentChatTarget + ":" + query);
        } else {
            chatArea.append("⚠️ Выберите собеседника для поиска.\n");
        }
    }

    private void sendToNetwork(String data) {
        if (userDataListener != null) userDataListener.accept(data);
    }

    @Override
    public void showInfo(String data, MessageType type) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.ERROR) {
                chatArea.append("❌ Ошибка: " + data + "\n");
            }
            else if (type == MessageType.INFO) {
                if (data.contains("Пользователи онлайн:")) {
                    updateUsersList(data);
                }
                else if (data.startsWith("История") || data.startsWith("Результаты поиска")) {
                    statusLabel.setText("📜 " + data);
                }
                else {
                    statusLabel.setText(data);
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
        });
    }

    private void updateUsersList(String data) {
        String content = data.replace("Пользователи онлайн: ", "").trim();
        onlineUsersModel.clear();

        if (!content.isEmpty() && !content.equals("Нет пользователей онлайн")) {
            for (String user : content.split(",\\s*")) {
                String trimmed = user.trim();
                if (!trimmed.isEmpty()) {
                    onlineUsersModel.addElement(trimmed);
                }
            }
        }
    }

    @Override
    public void addUserDataListener(Consumer<String> listener) {
        this.userDataListener = listener;
    }

    @Override
    public void removeUserDataListener(Consumer<String> listener) {
        if (this.userDataListener == listener) this.userDataListener = null;
    }

    public void start() {
        sendToNetwork(ChatCommand.LIST_USERS.name());
        setVisible(true);
        setState(JFrame.NORMAL);
        toFront();
        requestFocusInWindow();
    }

    private class UserCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String nick = value.toString();

            if (nick.equalsIgnoreCase(myNick)) {
                label.setText(nick + " (вы)");
                if (!isSelected) {
                    label.setForeground(Color.BLUE);
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                }
            } else {
                label.setText(nick);
            }
            return label;
        }
    }
}