package be.capstan.omegat.plugin;

import org.omegat.core.team2.TeamSettings;
import org.omegat.core.Core;

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
    protected static final ResourceBundle res = ResourceBundle.getBundle(
            "ImportExportCredentials", Locale.getDefault());
            
    // This method is called to start the import process
    public static void showDialog(Component parent) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(res.getString("icp.importdialog.fc.fileFilter"), "properties"));
        fileChooser.setDialogTitle(res.getString("icp.importdialog.fc.title"));

        // Show file chooser centered relative to parent
        int result = fileChooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            processFile(parent, selectedFile);
        } else {
            // User canceled or closed - show main dialog again
            ImportExportCredentials.showImportChoiceDialog(parent);
        }
    }

    private static void processFile(Component parent, File file) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, "UTF-8")) {
            props.load(reader);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    res.getString("icp.importdialog.readError") + ex.getMessage(),
                    res.getString("icp.importdialog.error"),
                    JOptionPane.ERROR_MESSAGE);
            ImportExportCredentials.showImportChoiceDialog(parent);
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
                    res.getString("icp.importdialog.saveError") + ex.getMessage(),
                    res.getString("icp.importdialog.error"),
                    JOptionPane.ERROR_MESSAGE);
            ImportExportCredentials.showImportChoiceDialog(parent);
            return;
        }

        JOptionPane.showMessageDialog(parent,
                res.getString("icp.importdialog.info.message").replace("{count}", Integer.toString(urlCount)),
                res.getString("icp.importdialog.info.title"),
                JOptionPane.INFORMATION_MESSAGE);

        if (urlCount == 0) {
            ImportExportCredentials.showImportChoiceDialog(parent);
        }
    }
}
