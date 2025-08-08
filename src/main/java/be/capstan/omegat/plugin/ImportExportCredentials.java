package be.capstan.omegat.plugin;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;

import org.omegat.core.team2.TeamSettings;
import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.openide.awt.Mnemonics;

public class ImportExportCredentials {
    protected static final ResourceBundle res = ResourceBundle.getBundle(
            "ImportExportCredentials", Locale.getDefault());
    private static JMenuItem importCredentialsMenuItem;
    private static JDialog importCredentialsDialog = null;

    public static void loadPlugins() {
        CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
            @Override
            public void onApplicationStartup() {
                addMenuItems();
            }

            @Override
            public void onApplicationShutdown() {
            }
        });
    }

    public static void unloadPlugins() {
    }

    private static void addMenuItems() {
        try {
            JMenu toolsMenu = Core.getMainWindow().getMainMenu().getToolsMenu();
            int menuPosition = toolsMenu.getItemCount();
            importCredentialsMenuItem = new JMenuItem();
            Mnemonics.setLocalizedText(importCredentialsMenuItem,
                    res.getString("icp.mainmenu.name"));
            importCredentialsMenuItem.addActionListener(e -> showImportChoiceDialog(Core.getMainWindow().getApplicationFrame()));
            toolsMenu.add(new JPopupMenu.Separator(), menuPosition);
            toolsMenu.add(importCredentialsMenuItem, menuPosition + 1);
        } catch (Exception e) {
            System.err.println("Import Credentials plugin: Error adding menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void showImportChoiceDialog(Component parent) {
        if (importCredentialsDialog != null && importCredentialsDialog.isVisible()) {
            importCredentialsDialog.toFront();
            return;
        }

        importCredentialsDialog = new JDialog(
                SwingUtilities.getWindowAncestor(parent),
                res.getString("icp.maindialog.title"),
                Dialog.ModalityType.APPLICATION_MODAL
        );
        importCredentialsDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Row 1: two buttons horizontally
        JPanel topButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton fileButton = new JButton(res.getString("icp.maindialog.fileButton"));
        fileButton.addActionListener(e -> {
            importCredentialsDialog.dispose();
            ImportFromFileDialog.showDialog(Core.getMainWindow().getApplicationFrame());
        });

        JButton manualButton = new JButton(res.getString("icp.maindialog.manualButton"));
        manualButton.addActionListener(e -> {
            importCredentialsDialog.dispose();
            new ManualCredentialsDialog(Core.getMainWindow().getApplicationFrame()).setVisible(true);
        });

        topButtonsPanel.add(fileButton);
        topButtonsPanel.add(manualButton);

        // Row 2: new export/delete button centered
        JPanel middleButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton manageCredentialsButton = new JButton(res.getString("icp.maindialog.manageButton"));
        manageCredentialsButton.addActionListener(e -> {
            importCredentialsDialog.dispose();
            ManageCredentialsDialog dialog = new ManageCredentialsDialog(Core.getMainWindow().getApplicationFrame());
            dialog.setVisible(true);
        });
        middleButtonPanel.add(manageCredentialsButton);

        // Row 3: Cancel button centered
        JPanel cancelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelButton = new JButton(res.getString("icp.maindialog.cancelButton"));
        cancelButton.addActionListener(e -> importCredentialsDialog.dispose());
        cancelPanel.add(cancelButton);

        // Add panels to dialog
        JPanel buttonsVerticalPanel = new JPanel();
        buttonsVerticalPanel.setLayout(new BoxLayout(buttonsVerticalPanel, BoxLayout.Y_AXIS));
        buttonsVerticalPanel.add(topButtonsPanel);
        buttonsVerticalPanel.add(Box.createVerticalStrut(10));
        buttonsVerticalPanel.add(middleButtonPanel);
        buttonsVerticalPanel.add(Box.createVerticalStrut(10));
        buttonsVerticalPanel.add(cancelPanel);

        contentPanel.add(buttonsVerticalPanel, BorderLayout.NORTH);

        importCredentialsDialog.getContentPane().add(contentPanel);
        importCredentialsDialog.pack();
        importCredentialsDialog.setLocationRelativeTo(parent);
        importCredentialsDialog.setResizable(false);
        importCredentialsDialog.setVisible(true);
    }
}
