import ru.gr0946x.net.Client;
import ru.gr0946x.ui.AuthUi;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.ui.GraphicalUi;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

private static GraphicalUi chatUi;

void main() {
    SwingUtilities.invokeLater(() -> {
        try {
            Client client = new Client("localhost", 9460);
            client.start();

            Runnable shutdown = () -> {
                client.stop();
                System.exit(0);
            };

            AuthUi authUi = new AuthUi();
            BiConsumer<String, MessageType> authListener = authUi::showInfo;
            authUi.addUserDataListener(client::sendData);
            client.addDataListener(authListener);

            authUi.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    shutdown.run();
                }
            });

            authUi.setOnAuthSuccess(nick -> {
                client.removeDataListener(authListener);

                chatUi = new GraphicalUi(nick);
                BiConsumer<String, MessageType> chatListener = chatUi::showInfo;
                chatUi.addUserDataListener(client::sendData);
                client.addDataListener(chatListener);

                chatUi.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        shutdown.run();
                    }
                });

                chatUi.start();
                authUi.dispose();
            });

            authUi.start();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Не удалось подключиться к серверу!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    });
}