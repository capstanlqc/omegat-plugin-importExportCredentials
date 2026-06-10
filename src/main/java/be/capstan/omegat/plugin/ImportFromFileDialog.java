package be.capstan.omegat.plugin;

import org.omegat.core.team2.TeamSettings;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ImportFromFileDialog {
    private static final ResourceBundle res = ResourceBundle.getBundle(
            "ImportExportCredentials", Locale.getDefault());

    public static void showDialog(Component parent, Runnable onSuccess) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory()
                        || f.getName().endsWith(".properties")
                        || f.getName().endsWith(".properties.encrypted");
            }
            @Override
            public String getDescription() {
                return res.getString("icp.import.fileFilter");
            }
        });
        fileChooser.setDialogTitle(res.getString("icp.import.dialogTitle"));

        int result = fileChooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String content;
            if (selectedFile.getName().endsWith(".properties.encrypted")) {
                content = decryptFile(parent, selectedFile);
            } else {
                content = readPlainFile(parent, selectedFile);
            }
            if (content != null) {
                processContent(parent, content, onSuccess);
            }
        }
    }

    //private static List<String> getHardcodedPasswords() {
        //try {
            //Class<?> cls = Class.forName("be.capstan.omegat.plugin.CredentialKeys");
            //@SuppressWarnings("unchecked")
            //List<String> passwords =
                    //(List<String>) cls.getMethod("getPasswords").invoke(null);
            //return passwords;
        //} catch (Exception ignored) {
            //return Collections.emptyList();
        //}
    //}

    private static String readPlainFile(Component parent, File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    res.getString("icp.import.readError") + ex.getMessage(),
                    res.getString("icp.import.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static String decryptFile(Component parent, File file) {
        byte[] raw;
        try {
            String fileText = readPlainFile(parent, file);
            if (fileText == null) return null;
            String b64 = fileText.lines()
                    .filter(l -> !l.startsWith("#"))
                    .collect(Collectors.joining());
            raw = Base64.getDecoder().decode(b64);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    res.getString("icp.import.readError") + ex.getMessage(),
                    res.getString("icp.import.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // Try each hardcoded password in turn
        for (String pwd : CredentialKeys.getPasswords()) {
            try {
                String result = FileDecryptor.decrypt(raw, pwd);
                if (isValidCredentialContent(result)) return result;
            } catch (Exception ignored) {}
        }

        // All hardcoded passwords failed or none present — ask the user
        JPasswordField pwField = new JPasswordField(30);
        int opt = JOptionPane.showConfirmDialog(
                parent,
                new Object[]{ res.getString("icp.import.passwordPrompt"), pwField },
                res.getString("icp.import.passwordTitle"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return null;

        try {
            String result = FileDecryptor.decrypt(raw, new String(pwField.getPassword()));
            if (isValidCredentialContent(result)) return result;
        } catch (Exception ignored) {}

        JOptionPane.showMessageDialog(parent,
                res.getString("icp.import.decryptError"),
                res.getString("icp.import.decryptErrorTitle"),
                JOptionPane.ERROR_MESSAGE);
        return null;
    }

    private static boolean isValidCredentialContent(String text) {
        return text != null
                && (text.contains("!username=") || text.contains("!password="));
    }

    private static void processContent(Component parent, String content, Runnable onSuccess) {
        Properties props = new Properties();
        try {
            props.load(new StringReader(content));
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
