package be.capstan.omegat.plugin;

import java.awt.Frame;
import java.util.*;
import javax.swing.*;

import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.openide.awt.Mnemonics;

public class ImportExportCredentials {
    protected static final ResourceBundle res = ResourceBundle.getBundle(
            "ImportExportCredentials", Locale.getDefault());
    private static JMenuItem manageCredentialsMenuItem;

    public static void loadPlugins() {
        CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
            @Override
            public void onApplicationStartup() {
                addMenuItems();
            }

            @Override
            public void onApplicationShutdown() {
                // Cleanup if needed
            }
        });
    }

    public static void unloadPlugins() {
        // Cleanup if needed
    }

    private static void addMenuItems() {
        try {
            JMenu toolsMenu = Core.getMainWindow().getMainMenu().getToolsMenu();
            int menuPosition = toolsMenu.getItemCount();
            manageCredentialsMenuItem = new JMenuItem();
            Mnemonics.setLocalizedText(manageCredentialsMenuItem,
                    res.getString("icp.mainmenu.name"));
            manageCredentialsMenuItem.addActionListener(e -> showMainDialog());
            toolsMenu.add(new JPopupMenu.Separator(), menuPosition);
            toolsMenu.add(manageCredentialsMenuItem, menuPosition + 1);
        } catch (Exception e) {
            System.err.println("Import/Export Credentials plugin: Error adding menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show the main credentials management dialog
     */
    public static void showMainDialog() {
        SwingUtilities.invokeLater(() -> {
            try {
                Frame owner = Core.getMainWindow().getApplicationFrame();
                MainCredentialsDialog dialog = new MainCredentialsDialog(owner);
                dialog.setVisible(true);
            } catch (Exception e) {
                System.err.println("Error showing main dialog: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
