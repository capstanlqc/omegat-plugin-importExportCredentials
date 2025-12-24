package be.capstan.omegat.plugin;

import org.omegat.core.team2.TeamSettings;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Locale;
import java.util.ResourceBundle;

public class ImportFromFileDialog {
    private static final ResourceBundle res = ResourceBundle.getBundle(
            "ImportExportCredentials", Locale.getDefault());

    // Updated to accept callback for refreshing parent dialog
    public static void showDialog(Component parent, Runnable onSuccess) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                res.getString("icp.import.fileFilter"), "properties"));
        fileChooser.setDialogTitle(res.getString("icp.import.dialogTitle"));

        int result = fileChooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            processFile(parent, selectedFile, onSuccess);
        }
        // No else needed - just returns to main dialog
    }

    private static void processFile(Component parent, File file, Runnable onSuccess) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, "UTF-8")) {
            props.load(reader);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    res.getString("icp.import.readError") + ex.getMessage(),
                    res.getString("icp.import.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Map<String, String>> urlCredentials = new HashMap<>();

        for (String key : props.stringPropertyNames()) {
            if (key.endsWith("!username") || key.endsWith("!password")) {
                int bangIndex = key.lastIndexOf('!');
                String url = key.substring(0, bangIndex);
                String subKey = key.substring(bangIndex + 1);
                String value = props.getProperty(key);

                urlCredentials.computeIfAbsent(url, k -> new HashMap<>()).put(subKey, value);
            }
        }

        int urlCount = 0;
        try {
            for (Map.Entry<String, Map<String, String>> entry : urlCredentials.entrySet()) {
                String url = entry.getKey();
                Map<String, String> creds = entry.getValue();

                if (creds.containsKey("username")) {
                    TeamSettings.set(url + "!username", creds.get("username"));
                }
                if (creds.containsKey("password")) {
                    TeamSettings.set(url + "!password", creds.get("password"));
                }
                urlCount++;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    res.getString("icp.import.saveError") + ex.getMessage(),
                    res.getString("icp.import.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(parent,
                res.getString("icp.import.successMessage")
                        .replace("{count}", Integer.toString(urlCount)),
                res.getString("icp.import.successTitle"),
                JOptionPane.INFORMATION_MESSAGE);

        if (onSuccess != null && urlCount > 0) {
            onSuccess.run();
        }
    }
}
