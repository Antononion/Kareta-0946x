package ru.gr0946x.net;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.gr0946x.bd.config.DatabaseConfig;
import ru.gr0946x.bd.service.AuthService;
import ru.gr0946x.bd.service.ChatService;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {
    private final AuthService authService;
    private final ChatService chatService;
    private boolean isActive;

    public Server(int port) {
        var context = new AnnotationConfigApplicationContext(DatabaseConfig.class);
        this.authService = context.getBean(AuthService.class);
        this.chatService = context.getBean(ChatService.class);

        this.isActive = true;
        new Thread(() -> {
            try (var serverSocket = new ServerSocket(port)) {
                System.out.println("Сервер запущен на порту " + port);
                while (isActive) {
                    try {
                        var socket = serverSocket.accept();
                        System.out.println("Клиент подключён");
                        var connClient = new ConnectedClient(socket, authService, chatService);
                        connClient.start();
                    } catch (Exception e) {
                        System.err.println("Ошибка при подключении клиента: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка запуска сервера: " + e.getMessage());
                isActive = false;
            }
        }).start();
    }
}