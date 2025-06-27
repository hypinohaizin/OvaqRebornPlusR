package net.shoreline.client.impl.manager.client;

import net.shoreline.client.OvaqRebornPlusMod;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    public static void checkUpdate() {
        try {
            //URL url = new URL("https://gist.githubusercontent.com/dada994a/be4892ca0353b822b753cb09d318e7fb/raw/bd696b63552891b581fc1e4424c86848ce9f9e08/Ver.txt");
            URL url = new URL("https://ovaqclient.web.fc2.com/ver.txt");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String latestVersion = reader.readLine().trim();
            reader.close();

            if (!OvaqRebornPlusMod.MOD_VER.equals(latestVersion)) {
                JFrame frame = new JFrame();
                frame.setAlwaysOnTop(true);
                JOptionPane.showMessageDialog(frame, "新しいバージョンが利用可能です！\n最新バージョン: " + latestVersion +
                                "\n現在のバージョン: " + OvaqRebornPlusMod.MOD_VER + "\nクライアントを更新してください。",
                        "バージョン更新", JOptionPane.WARNING_MESSAGE);
                System.exit(1);
            }
        } catch (IOException e) {
        }
    }
}
