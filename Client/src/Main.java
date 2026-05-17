import ru.gr0946x.net.Client;
import ru.gr0946x.ui.AuthUi;
import ru.gr0946x.net.MessageType;

import javax.swing.*;
import java.io.IOException;

void main() {
    SwingUtilities.invokeLater(() -> {
        try {
            Client client = new Client("localhost", 9460);
            client.start();
            new AuthUi(client).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Не удалось подключиться к серверу!\nУбедитесь, что Server запущен.",
                    "Ошибка подключения",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    });
}