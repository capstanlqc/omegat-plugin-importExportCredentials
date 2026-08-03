package be.capstan.omegat.plugin;

import org.omegat.core.team2.TeamSettings;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Locale;
import java.util.ResourceBundle;

public class EditCredentialsDialog extends JDialog {
    private static final ResourceBundle res = ResourceBundle.getBundle(
            "ImportExportCredentials", Locale.getDefault());

    private final JTextField urlField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JCheckBox stripSpacesCheck;
    private final JLabel statusLabel;
    private final JButton toggleButton;
    private final String originalUrl;
    private final Runnable onSuccess;

    public EditCredentialsDialog(Frame owner, String url, Runnable onSuccess) {
        super(owner, res.getString("icp.edit.dialog.title"), true);
        this.originalUrl = url;
        this.onSuccess = onSuccess;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel(res.getString("icp.edit.urlLabel")), gbc);

        urlField = new JTextField(url, 30);
        gbc.gridx = 1; gbc.weightx = 1.0;
        fieldsPanel.add(urlField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel(res.getString("icp.edit.usernameLabel")), gbc);

        usernameField = new JTextField(30);
        gbc.gridx = 1; gbc.weightx = 1.0;
        fieldsPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel(res.getString("icp.edit.passwordLabel")), gbc);

        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordField = new JPasswordField(30);
        passwordPanel.add(passwordField, BorderLayout.CENTER);

        toggleButton = new JButton(res.getString("icp.edit.toggleButton.hide"));
        toggleButton.setMargin(new Insets(2, 5, 2, 5));
        toggleButton.addActionListener(ev -> togglePasswordVisibility());
        passwordPanel.add(toggleButton, BorderLayout.EAST);

        gbc.gridx = 1; gbc.weightx = 1.0;
        fieldsPanel.add(passwordPanel, gbc);

        stripSpacesCheck = new JCheckBox(res.getString("icp.edit.stripSpacesCheck"), true);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        fieldsPanel.add(stripSpacesCheck, gbc);

        add(fieldsPanel, BorderLayout.NORTH);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton(res.getString("icp.edit.okButton"));
        JButton cancelButton = new JButton(res.getString("icp.edit.cancelButton"));

        okButton.addActionListener(e -> onOk());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.PAGE_END);

        DocumentListener docListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { clearStatus(); }
            @Override public void removeUpdate(DocumentEvent e) { clearStatus(); }
            @Override public void changedUpdate(DocumentEvent e) { clearStatus(); }
        };

        urlField.getDocument().addDocumentListener(docListener);
        usernameField.getDocument().addDocumentListener(docListener);
        passwordField.getDocument().addDocumentListener(docListener);
        stripSpacesCheck.addActionListener(e -> clearStatus());

        loadCredentials(url);

        pack();
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void loadCredentials(String url) {
        try {
            String username = TeamSettings.get(url + "!username");
            
            // FIX: Ensure we are passing the full key with "!password"
            String password = CredentialStorage.getPlainPassword(url + "!password");

            if (username != null) {
                usernameField.setText(username);
            }

            if (password != null && !password.isEmpty()) {
                passwordField.setText(password);
                passwordField.setEchoChar((char) 0);
                toggleButton.setText(res.getString("icp.edit.toggleButton.hide"));
            }
        } catch (Exception e) {
            // Ignore loading failures to allow manual overriding
        }
    }

    private void togglePasswordVisibility() {
        if (passwordField.getEchoChar() == 0) {
            char echoChar = (Character) UIManager.getDefaults().get("PasswordField.echoChar");
            if (echoChar == 0) echoChar = '*';
            passwordField.setEchoChar(echoChar);
            toggleButton.setText(res.getString("icp.edit.toggleButton.show"));
        } else {
            passwordField.setEchoChar((char) 0);
            toggleButton.setText(res.getString("icp.edit.toggleButton.hide"));
        }
    }

    private void clearStatus() {
        statusLabel.setText(" ");
    }

    private void onOk() {
        clearStatus();

        String urlText = urlField.getText();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        boolean strip = stripSpacesCheck.isSelected();
        if (strip) {
            urlText = trimWhitespace(urlText);
            username = trimWhitespace(username);
            password = trimWhitespace(password);
        }

        if (isEmpty(urlText) || isEmpty(username) || isEmpty(password)) {
            statusLabel.setText(res.getString("icp.edit.statusEmpty"));
            return;
        }

        if (!isValidURL(urlText)) {
            statusLabel.setText(res.getString("icp.edit.statusInvalid"));
            return;
        }

        if (!strip && (isAllWhitespace(username) || isAllWhitespace(password))) {
            int choice = JOptionPane.showOptionDialog(this,
                    res.getString("icp.edit.emptyQuestion"), res.getString("icp.edit.emptyTitle"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new Object[]{ res.getString("icp.edit.emptyContinue"), res.getString("icp.edit.emptyGoBack") },
                    res.getString("icp.edit.emptyGoBack")
            );
            if (choice != JOptionPane.YES_OPTION) return;
        } else if (strip && (username.isEmpty() || password.isEmpty())) {
            statusLabel.setText(res.getString("icp.edit.statusEmptyWarning"));
            return;
        }

        try {
            if (!originalUrl.equals(urlText)) {
                TeamSettings.set(originalUrl + "!username", null);
                TeamSettings.set(originalUrl + "!password", null);
            }

            TeamSettings.set(urlText + "!username", username);
            
            // FIX: Using standardized full key
            CredentialStorage.setPassword(urlText + "!password", password);

            JOptionPane.showMessageDialog(this,
                    res.getString("icp.edit.confirmationMessage").replace("{URL}", urlText),
                    res.getString("icp.edit.confirmationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);

            if (onSuccess != null) onSuccess.run();
            dispose();

        } catch (Exception ex) {
            statusLabel.setText(res.getString("icp.edit.statusError") + ex.getMessage());
        }
    }

    private boolean isValidURL(String urlText) {
        try { new java.net.URL(urlText); return true; } 
        catch (java.net.MalformedURLException e) { return false; }
    }

    private boolean isEmpty(String s) { return s == null || s.isEmpty(); }
    private boolean isAllWhitespace(String s) { return s != null && s.trim().isEmpty(); }
    private String trimWhitespace(String s) { return s == null ? null : s.strip(); }
}
