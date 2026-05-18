package ru.gr0946x.ui;

import ru.gr0946x.net.AuthCommand;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class AuthUi extends JFrame implements Ui {

    private Consumer<String> userDataListener;
    private Consumer<String> onAuthSuccess;

    private JTextField nickField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    public AuthUi() {
        initUI();
    }

    public void setOnAuthSuccess(Consumer<String> callback) {
        this.onAuthSuccess = callback;
    }

    private void initUI() {
        setTitle("Вход в Карету");
        setSize(320, 220);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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

        btnLogin.addActionListener(e -> attemptAuth(AuthCommand.LOGIN));
        btnRegister.addActionListener(e -> attemptAuth(AuthCommand.REG));
        passwordField.addActionListener(e -> attemptAuth(AuthCommand.LOGIN));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private void attemptAuth(AuthCommand command) {
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

        String payload = command.name() + ProtocolConstants.COMMAND_SEPARATOR +
                nick + ProtocolConstants.COMMAND_SEPARATOR + password;
        if (userDataListener != null) {
            userDataListener.accept(payload);
        }
    }

    @Override
    public void showInfo(String data, MessageType type) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.ERROR) {
                statusLabel.setText("Ошибка: " + data);
                statusLabel.setForeground(Color.RED);
                nickField.setEnabled(true);
                passwordField.setEnabled(true);
            }
            else if (type == MessageType.INFO) {
                statusLabel.setText(data);

                if (data.contains("Добро пожаловать") || data.contains("успешна")) {
                    statusLabel.setForeground(Color.GREEN);
                    if (onAuthSuccess != null) {
                        onAuthSuccess.accept(nickField.getText().trim());
                    }
                }
            }
        });
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
        setVisible(true);
    }
}