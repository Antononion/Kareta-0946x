package ru.gr0946x.net;
import ru.gr0946x.net.AuthCommand;
import ru.gr0946x.net.ChatCommand;
import ru.gr0946x.bd.entity.User;
import ru.gr0946x.bd.service.AuthService;
import ru.gr0946x.bd.service.ChatService;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectedClient {
    private final Communicator communicator;
    private final AuthService authService;
    private final ChatService chatService;

    private static final CopyOnWriteArrayList<ConnectedClient> onlineClients = new CopyOnWriteArrayList<>();

    private User authenticatedUser;
    private volatile boolean isActive = true;

    public ConnectedClient(Socket socket, AuthService authService, ChatService chatService) throws IOException {
        this.communicator = new Communicator(socket);
        this.communicator.setOnDisconnect(this::stop);
        this.authService = authService;
        this.chatService = chatService;
        this.communicator.addDataListener(this::parseData);
        onlineClients.add(this);
    }

    public void start() {
        communicator.start();
        sendData(MessageType.REQUEST + ProtocolConstants.COMMAND_SEPARATOR + "Введите 'REG ник пароль' или 'LOGIN ник пароль'");
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    private void parseData(String data) {
        if (authenticatedUser == null) {
            handleAuth(data);
        } else {
            handleMessage(data);
        }
    }

    private void handleAuth(String data) {
        String[] parts = data.split(ProtocolConstants.COMMAND_SEPARATOR, 3);
        if (parts.length < 3) {
            sendError("Формат: REG|LOGIN:Ник:Пароль");
            return;
        }

        String commandStr = parts[0].trim().toUpperCase();
        String nick = parts[1].trim();
        String password = parts[2];

        try {
            AuthCommand command = AuthCommand.valueOf(commandStr);
            switch (command) {
                case REG -> {
                    authenticatedUser = authService.register(nick, password);
                    sendInfo("Регистрация успешна. Добро пожаловать, " + authenticatedUser.getNick() + "!");
                    notifyAllOnlineList();
                }
                case LOGIN -> {
                    authenticatedUser = authService.login(nick, password);
                    sendInfo("Вход выполнен. Добро пожаловать, " + authenticatedUser.getNick() + "!");
                    notifyAllOnlineList();
                }
            }
        } catch (IllegalArgumentException e) {
            sendError("Неизвестная команда. Используйте " + AuthCommand.REG.name() + " или " + AuthCommand.LOGIN.name());
        }
    }

    private void handleMessage(String data) {
        String[] parts = data.split(ProtocolConstants.COMMAND_SEPARATOR, 3);
        String commandStr = parts[0].trim().toUpperCase();
        try {
            ChatCommand command = ChatCommand.valueOf(commandStr);

            if (command == ChatCommand.LIST_USERS) {
                notifyAllOnlineList();
                return;
            }

            if (parts.length < 2) {
                sendError("Формат: PM:Ник:Текст | BROADCAST:Текст | SEARCH:Ник:Запрос | HISTORY:Ник");
                return;
            }

            switch (command) {
                case PM -> handlePrivateMessage(parts);
                case BROADCAST -> handleBroadcast(parts[1]);
                case HISTORY -> handleHistory(parts[1]);
                case SEARCH -> handleSearch(parts);
                case LIST_USERS -> notifyAllOnlineList();
            }
        } catch (IllegalArgumentException e) {
            sendError("Неизвестная команда: " + commandStr);
        }
    }

    private void handlePrivateMessage(String[] parts) {
        String targetNick = parts[1];
        String content = parts[2];
        var targetUser = authService.findByNick(targetNick)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + targetNick));

        chatService.saveMessage(authenticatedUser, targetUser, content);
        onlineClients.stream()
                .filter(c -> c.authenticatedUser != null && c.authenticatedUser.getNick().equalsIgnoreCase(targetNick))
                .findFirst()
                .ifPresent(c -> c.sendData(MessageType.MESSAGE + ":" + authenticatedUser.getNick() + ":" + content));

        sendData(MessageType.INFO + ":Сообщение отправлено пользователю " + targetNick);
    }

    private void handleBroadcast(String content) {
        chatService.saveMessage(authenticatedUser, null, content);
        String payload = MessageType.MESSAGE + ":" + authenticatedUser.getNick() + ":" + content;
        onlineClients.stream()
                .filter(c -> c != this && c.authenticatedUser != null)
                .forEach(c -> c.sendData(payload));
    }

    private void handleHistory(String targetNick) {
        var targetUser = authService.findByNick(targetNick)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        var history = chatService.getHistory(authenticatedUser, targetUser);
        String response = history.stream()
                .map(m -> MessageType.MESSAGE + ":" + m.fromNick() + ":" + m.content())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("История пуста");
        sendData(MessageType.INFO + ":История переписки с " + targetNick + ":\n" + response);
    }

    private void handleSearch(String[] parts) {
        String targetNick = parts[1];
        String query = parts[2];
        var targetUser = authService.findByNick(targetNick)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        var results = chatService.searchMessages(authenticatedUser, targetUser, query);
        String response = results.stream()
                .map(m -> MessageType.MESSAGE + ":" + m.fromNick() + ":" + m.content())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("Ничего не найдено");
        sendData(MessageType.INFO + ":Результаты поиска в чате с " + targetNick + ":\n" + response);
    }

    private void notifyAllOnlineList() {
        String online = onlineClients.stream()
                .filter(c -> c.authenticatedUser != null)
                .map(c -> c.authenticatedUser.getNick())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Нет пользователей онлайн");

        String payload = MessageType.INFO + ":Пользователи онлайн: " + online;

        for (ConnectedClient client : onlineClients) {
            if (client.authenticatedUser != null) {
                client.sendData(payload);
            }
        }
    }

    private void sendError(String msg) {
        sendData(MessageType.ERROR + ":" + msg);
    }

    private void sendInfo(String msg) {
        sendData(MessageType.INFO + ":" + msg);
    }

    public void stop() {
        isActive = false;
        communicator.stop();
        onlineClients.remove(this);
        if (authenticatedUser != null) {
            System.out.println(authenticatedUser.getNick() + " отключился");
        }
        notifyAllOnlineList();
    }
}