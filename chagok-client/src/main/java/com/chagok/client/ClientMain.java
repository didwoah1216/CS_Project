package com.chagok.client;

import com.chagok.client.net.ClientConfig;
import com.chagok.client.ui.StartFrame;
import com.chagok.client.util.UiFonts;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

public final class ClientMain {
    private ClientMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                // 기본 LookAndFeel 유지
            }
            UiFonts.install();
            AppContext app = new AppContext(ClientConfig.load());
            Timer ping = new Timer(60_000, event -> {
                if (app.connection.isConnected()) {
                    try {
                        app.api.ping();
                    } catch (RuntimeException ignored) {
                        // 다음 요청에서 재연결한다.
                    }
                }
            });
            ping.setRepeats(true);
            ping.start();
            new StartFrame(app).setVisible(true);
        });
    }
}
