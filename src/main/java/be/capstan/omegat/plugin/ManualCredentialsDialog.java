package be.capstan.omegat.plugin;

import org.omegat.core.team2.TeamSettings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.ResourceBundle;

public class ManualCredentialsDialog extends JDialog {
    protected static final ResourceBundle res = ResourceBundle.getBundle(
            "ImportExportCredentials", Locale.getDefault());

    private final JTextField urlField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JCheckBox stripSpacesCheck;
    private final JCheckBox rememberInputCheck;
    private final JLabel statusLabel;

    // Temporarily remembered draft values
    private String tempUrl = "";
    private String tempUsername = "";
    private String tempPassword = "";

    // Tracks if user edited fields after toggling "Remember input" ON
    private boolean hasEditedSinceRemember = false;

    public ManualCredentialsDialog(Frame owner) {
        super(owner, res.getString("icp.manualdialog.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Fields panel
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // URL label and field
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel(res.getString("icp.manualdialog.urlLabel")), gbc);

        urlField = new JTextField(30);
        gbc.gridx = 1; gbc.weightx = 1.0;
        fieldsPanel.add(urlField, gbc);

        // Username label and field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel(res.getString("icp.manualdialog.usernameLabel")), gbc);

        usernameField = new JTextField(30);
        gbc.gridx = 1; gbc.weightx = 1.0;
        fieldsPanel.add(usernameField, gbc);

        // Password label and field with toggle button
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel(res.getString("icp.manualdialog.passwordLabel")), gbc);

        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordField = new JPasswordField(30);
        passwordPanel.add(passwordField, BorderLayout.CENTER);

        JButton toggleButton = new JButton(res.getString("icp.manualdialog.toggleButton.show"));
        toggleButton.setMargin(new Insets(2, 5, 2, 5));
        toggleButton.addActionListener(ev -> {
            if (passwordField.getEchoChar() == 0) {
                char echoChar = (Character) UIManager.getDefaults().get("PasswordField.echoChar");
                passwordField.setEchoChar(echoChar);
                toggleButton.setText(res.getString("icp.manualdialog.toggleButton.show"));
            } else {
                passwordField.setEchoChar((char) 0);
                toggleButton.setText(res.getString("icp.manualdialog.toggleButton.hide"));
            }
        });
        passwordPanel.add(toggleButton, BorderLayout.EAST);

        gbc.gridx = 1; gbc.weightx = 1.0;
        fieldsPanel.add(passwordPanel, gbc);

        // Two checkboxes on one line
        JPanel checksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        stripSpacesCheck = new JCheckBox(res.getString("icp.manualdialog.stripSpacesCheck"), true);
        rememberInputCheck = new JCheckBox(res.getString("icp.manualdialog.rememberInputCheck"), false);
        checksPanel.add(stripSpacesCheck);
        checksPanel.add(rememberInputCheck);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        fieldsPanel.add(checksPanel, gbc);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(fieldsPanel, BorderLayout.NORTH);
        add(topPanel, BorderLayout.NORTH);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton(res.getString("icp.manualdialog.okButton"));
        JButton cancelButton = new JButton(res.getString("icp.manualdialog.cancelButton"));

        okButton.addActionListener(this::onOk);
        cancelButton.addActionListener(e -> {
            dispose();
            ImportExportCredentials.showImportChoiceDialog(getOwner());
        });
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.PAGE_END);

        pack();
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(owner);

        // Add listeners to reset status and track changes in fields
        DocumentListener docListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onFieldEdited(); }
            @Override public void removeUpdate(DocumentEvent e) { onFieldEdited(); }
            @Override public void changedUpdate(DocumentEvent e) { onFieldEdited(); }
        };

        urlField.getDocument().addDocumentListener(docListener);
        usernameField.getDocument().addDocumentListener(docListener);
        passwordField.getDocument().addDocumentListener(docListener);
        stripSpacesCheck.addActionListener(e -> clearStatus());

        rememberInputCheck.addActionListener(e -> onRememberInputToggled());

        // Initialize fields according to checkbox (unchecked = empty)
        if (rememberInputCheck.isSelected()) {
            loadPersistentValuesToFields();
            hasEditedSinceRemember = false;
        } else {
            clearAllFields();
            hasEditedSinceRemember = false;
        }
    }

    /** Called on any text field edit */
    private void onFieldEdited() {
        clearStatus();

        if (!rememberInputCheck.isSelected()) {
            // When unchecked, update temp remembered always
            updateTempFromFields();
        } else {
            // Remember input is checked
            if (!hasEditedSinceRemember) {
                // First edit after toggling ON: discard old temp, update with current
                updateTempFromFields();
                hasEditedSinceRemember = true;
            } else {
                // Subsequent edits: update temp remembered
                updateTempFromFields();
            }
        }
    }

    /** Called when the "Remember input" checkbox is toggled */
    private void onRememberInputToggled() {
        clearStatus();

        if (rememberInputCheck.isSelected()) {
            // Checked → show persistent values if any
            loadPersistentValuesToFields();
            hasEditedSinceRemember = false;
        } else {
            // Unchecked → revert to temp remembered values
            loadTempValuesToFields();
            hasEditedSinceRemember = false;
        }
    }

    private void updateTempFromFields() {
        tempUrl = urlField.getText();
        tempUsername = usernameField.getText();
        tempPassword = new String(passwordField.getPassword());
    }

    private void loadTempValuesToFields() {
        urlField.setText(tempUrl);
        usernameField.setText(tempUsername);
        passwordField.setText(tempPassword);
    }

    private void loadPersistentValuesToFields() {
        try {
            String lastUrl = TeamSettings.get("manualCreds.lastUrl");
            if (lastUrl != null && !lastUrl.isEmpty()) {
                urlField.setText(lastUrl);
                String storedUsername = TeamSettings.get(lastUrl + "!username");
                String storedPasswordB64 = TeamSettings.get(lastUrl + "!password");

                usernameField.setText(storedUsername != null ? storedUsername
                        : (tempUsername != null ? tempUsername : ""));

                if (storedPasswordB64 != null) {
                    try {
                        String decoded = new String(
                                Base64.getDecoder().decode(storedPasswordB64),
                                StandardCharsets.UTF_8
                        );
                        passwordField.setText(decoded);
                    } catch (IllegalArgumentException ignore) {
                        passwordField.setText(tempPassword != null ? tempPassword : "");
                    }
                } else {
                    passwordField.setText(tempPassword != null ? tempPassword : "");
                }
            } else {
                // No persistent stored values, fallback to temp or empty
                urlField.setText(tempUrl != null ? tempUrl : "");
                usernameField.setText(tempUsername != null ? tempUsername : "");
                passwordField.setText(tempPassword != null ? tempPassword : "");
            }
        } catch (Exception e) {
            // On error fallback to temp or empty
            urlField.setText(tempUrl != null ? tempUrl : "");
            usernameField.setText(tempUsername != null ? tempUsername : "");
            passwordField.setText(tempPassword != null ? tempPassword : "");
        }
    }

    private void clearAllFields() {
        urlField.setText("");
        usernameField.setText("");
        passwordField.setText("");
    }

    private void clearStatus() {
        statusLabel.setText(" ");
    }

    private void onOk(ActionEvent e) {
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
            statusLabel.setText(res.getString("icp.manualdialog.statusEmpty"));
            return;
        }
    
        if (!isValidURL(urlText)) {
            statusLabel.setText(res.getString("icp.manualdialog.statusInvalid"));
            return;
        }
    
        if (!strip && (isAllWhitespace(username) || isAllWhitespace(password))) {
            int choice = JOptionPane.showOptionDialog(
                    this,
                    res.getString("icp.manualdialog.emptyQuestion"),
                    res.getString("icp.manualdialog.emptyTitle"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[]{
                            res.getString("icp.manualdialog.emptyContinue"),
                            res.getString("icp.manualdialog.emptyGoBack")},
                    res.getString("icp.manualdialog.emptyGoBack")
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        } else if (strip && (username.isEmpty() || password.isEmpty())) {
            statusLabel.setText(res.getString("icp.manualdialog.statusEmptyWarning"));
            return;
        }
    
        String base64Password = Base64.getEncoder()
                .encodeToString(password.getBytes(StandardCharsets.UTF_8));
    
        try {
            TeamSettings.set(urlText + "!username", username);
            TeamSettings.set(urlText + "!password", base64Password);
    
            if (rememberInputCheck.isSelected()) {
                TeamSettings.set("manualCreds.lastUrl", urlText);
            } else {
                TeamSettings.set("manualCreds.lastUrl", null);
            }
        } catch (Exception ex) {
            statusLabel.setText(res.getString("icp.manualdialog.statusError") + ex.getMessage());
            return;
        }
    
        // *** New confirmation dialog ***
        JOptionPane.showMessageDialog(
                this,
                res.getString("icp.manualdialog.confirmationMessage").replace("{URL}", urlText),
                res.getString("icp.manualdialog.confirmationTitle"),
                JOptionPane.INFORMATION_MESSAGE
        );
    
        dispose();
    }

    private boolean isValidURL(String urlText) {
        try {
            new URL(urlText);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private boolean isAllWhitespace(String s) {
        return s != null && s.trim().isEmpty();
    }

    private String trimWhitespace(String s) {
        return s == null ? null : s.strip();
    }
}
