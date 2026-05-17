package ru.gr0946x.ui;
import ru.gr0946x.net.Client;
import ru.gr0946x.net.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BiConsumer;

public class AuthUi extends JFrame {

    private final Client client;
    private JTextField nickField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private BiConsumer<String, MessageType> authListener;

    public AuthUi(Client client) {
        this.client = client;
        initUI();
        setupNetworkListener();
    }

    private void initUI() {
        setTitle("Вход в Карету");
        setSize(320, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        inputPanel.add(new JLabel("Никнейм:"));
        nickField = new JTextField();
        inputPanel.add(nickField);

        inputPanel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        inputPanel.add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton btnLogin = new JButton("Войти");
        JButton btnRegister = new JButton("Регистрация");
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        btnLogin.addActionListener(e -> attemptAuth("LOGIN"));
        btnRegister.addActionListener(e -> attemptAuth("REG"));
        passwordField.addActionListener(e -> attemptAuth("LOGIN"));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void attemptAuth(String command) {
        String nick = nickField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (nick.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Заполните все поля!");
            return;
        }

        statusLabel.setText("Подключение...");
        statusLabel.setForeground(Color.BLUE);

        nickField.setEnabled(false);
        passwordField.setEnabled(false);

        client.sendData(command + ":" + nick + ":" + password);
    }

    private void setupNetworkListener() {
        authListener = (data, type) -> {
            SwingUtilities.invokeLater(() -> {
                if (type == MessageType.ERROR) {
                    statusLabel.setText("Ошибка: " + data);
                    statusLabel.setForeground(Color.RED);
                    nickField.setEnabled(true);
                    passwordField.setEnabled(true);
                }
                else if (type == MessageType.INFO) {
                    if (data.contains("Добро пожаловать") ||
                            data.contains("выполнен") ||
                            data.contains("успешна") ||
                            data.contains("вошел")) {

                        statusLabel.setText("Успешно! Загрузка...");
                        statusLabel.setForeground(Color.GREEN);

                        client.removeDataListener(authListener);

                        dispose();
                        new GraphicalUi(client).start();
                    }
                }
            });
        };
        client.addDataListener(authListener);
    }

    public void start() {
        setVisible(true);
    }
}