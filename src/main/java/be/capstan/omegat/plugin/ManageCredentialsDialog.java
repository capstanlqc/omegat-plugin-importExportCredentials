package be.capstan.omegat.plugin;

import org.omegat.core.team2.TeamSettings;
import org.omegat.core.Core;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.Properties;

public class ManageCredentialsDialog extends JDialog {
    private static final ResourceBundle res = ResourceBundle.getBundle("ImportExportCredentials", Locale.getDefault());

    private final JTable table;
    private final CredentialsTableModel tableModel;
    private boolean ascendingSort = true; // Default ascending

    public ManageCredentialsDialog(Frame owner) {
        super(owner, res.getString("icp.manage.dialog.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        List<String> urls = loadUniqueUrls();

        tableModel = new CredentialsTableModel(urls);
        table = new JTable(tableModel) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class;
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        TableColumn checkboxColumn = table.getColumnModel().getColumn(0);
        checkboxColumn.setMaxWidth(50);
        checkboxColumn.setMinWidth(50);
        checkboxColumn.setResizable(false);

        JTableHeader th = table.getTableHeader();
        th.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col == 1) { // Sort by URL column
                    ascendingSort = !ascendingSort;
                    tableModel.sortByUrl(ascendingSort);
                } else if (col == 0) {
                    // Toggle select/deselect all when clicking checkbox header
                    boolean anyUnselected = tableModel.isAnyRowUnselected();
                    tableModel.selectAll(anyUnselected);
                }
            }
        });

        th.setToolTipText(res.getString("icp.manage.table.headerTip"));

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with buttons centered
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton exportButton = new JButton(res.getString("icp.manage.exportSelected"));
        JButton deleteButton = new JButton(res.getString("icp.manage.deleteSelected"));
        JButton exportDeleteButton = new JButton(res.getString("icp.manage.exportDeleteSelected"));
        JButton cancelButton = new JButton(res.getString("icp.manage.cancel"));

        exportButton.addActionListener(e -> exportSelected());
        deleteButton.addActionListener(e -> deleteSelected(false));
        exportDeleteButton.addActionListener(e -> deleteSelected(true));
        cancelButton.addActionListener(e -> dispose());

        bottomPanel.add(exportButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(exportDeleteButton);
        bottomPanel.add(cancelButton);

        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);

        // Prevent resizing smaller than preferred size (to keep buttons and list visible)
        setMinimumSize(getPreferredSize());
        setResizable(false);
    }

    private List<String> loadUniqueUrls() {
        Set<String> urlSet = new HashSet<>();
        try {
            for (Object keyObj : TeamSettings.listKeys()) {
                String key = keyObj.toString();
                if (key.contains("!username") || key.contains("!password")) {
                    int idx = key.lastIndexOf('!');
                    if (idx > 0) {
                        String url = key.substring(0, idx);
                        urlSet.add(url);
                    }
                }
            }
        } catch (Exception ex) {
            // Possibly log or ignore
        }

        List<String> urls = new ArrayList<>(urlSet);
        Collections.sort(urls);
        return urls;
    }

    private List<String> getSelectedUrls() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((Boolean) tableModel.getValueAt(i, 0)) {
                selected.add((String) tableModel.getValueAt(i, 1));
            }
        }
        return selected;
    }

    private void exportSelected() {
        List<String> selectedUrls = getSelectedUrls();
        if (selectedUrls.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    res.getString("icp.manage.noSelection"),
                    res.getString("icp.manage.warningTitle"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser saveChooser = new JFileChooser(System.getProperty("user.home"));
        saveChooser.setDialogTitle(res.getString("icp.manage.exportDialogTitle"));
        saveChooser.setSelectedFile(new File("export_credentials.properties"));
        saveChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                res.getString("icp.importdialog.fc.fileFilter"), "properties"));

        int resChoice = saveChooser.showSaveDialog(this);
        if (resChoice != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = saveChooser.getSelectedFile();

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            // Use a Properties object to properly escape keys and values
            Properties exportProps = new Properties();

            for (String url : selectedUrls) {
                String username = TeamSettings.get(url + "!username");
                String password = TeamSettings.get(url + "!password");
                if (username != null) {
                    exportProps.setProperty(url + "!username", username);
                }
                if (password != null) {
                    exportProps.setProperty(url + "!password", password);
                }
            }

            // Store properties to writer; null comment disables header comment line
            exportProps.store(writer, null);
            writer.flush();

            // Show info dialog after successful export
            JOptionPane.showMessageDialog(this,
                    res.getString("icp.manage.exportSuccess").replace("{count}", Integer.toString(selectedUrls.size())),
                    res.getString("icp.manage.informationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);

            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    res.getString("icp.manage.exportError") + e.getMessage(),
                    res.getString("icp.manage.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected(boolean exportThenDelete) {
        List<String> selectedUrls = getSelectedUrls();
        if (selectedUrls.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    res.getString("icp.manage.noSelection"),
                    res.getString("icp.manage.warningTitle"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (exportThenDelete) {
            exportSelected();
        }
        int choice = JOptionPane.showConfirmDialog(this,
                res.getString("icp.manage.deleteConfirm"),
                res.getString("icp.manage.deleteTitle"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            for (String url : selectedUrls) {
                TeamSettings.set(url + "!username", null);
                TeamSettings.set(url + "!password", null);
            }
            // Refresh table after deletion
            tableModel.setUrls(loadUniqueUrls());

            // Show info dialog after successful deletion or export+deletion
            String msg = exportThenDelete ?
                    res.getString("icp.manage.exportDeleteSuccess").replace("{count}", Integer.toString(selectedUrls.size())) :
                    res.getString("icp.manage.deleteSuccess").replace("{count}", Integer.toString(selectedUrls.size()));

            JOptionPane.showMessageDialog(this,
                    msg,
                    res.getString("icp.manage.informationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);

            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    res.getString("icp.manage.deleteError") + e.getMessage(),
                    res.getString("icp.manage.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class CredentialsTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES;
        private final Class<?>[] COLUMN_CLASSES = { Boolean.class, String.class };

        private List<String> urls;
        private List<Boolean> checked;

        CredentialsTableModel(List<String> urls) {
            // Localize column names here
//            ResourceBundle res = ResourceBundle.getBundle("ImportExportCredentials", Locale.getDefault());
            COLUMN_NAMES = new String[] {
                res.getString("icp.manage.table.checkLabel"), 
                res.getString("icp.manage.table.urlLabel")
            };
            setUrls(urls);
        }

        public void setUrls(List<String> urls) {
            this.urls = new ArrayList<>(urls);
            this.checked = new ArrayList<>();
            for (int i = 0; i < urls.size(); i++) {
                checked.add(Boolean.FALSE);
            }
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return urls.size(); }
        @Override public int getColumnCount() { return COLUMN_NAMES.length; }
        @Override public String getColumnName(int col) { return COLUMN_NAMES[col]; }
        @Override public Class<?> getColumnClass(int col) { return COLUMN_CLASSES[col]; }
        @Override public Object getValueAt(int row, int col) {
            if (col == 0) return checked.get(row);
            return urls.get(row);
        }
        @Override public boolean isCellEditable(int row, int col) {
            return col == 0;
        }
        @Override public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                checked.set(row, (Boolean) value);
                fireTableCellUpdated(row, col);
            }
        }

        void selectAll(boolean select) {
            for (int i = 0; i < checked.size(); i++) {
                checked.set(i, select);
            }
            fireTableRowsUpdated(0, checked.size() - 1);
        }

        boolean isAnyRowUnselected() {
            for (Boolean b : checked) {
                if (!b) return true;
            }
            return false;
        }

        void sortByUrl(boolean ascending) {
            Comparator<String> comp = ascending ? Comparator.naturalOrder() : Comparator.reverseOrder();

            List<String> sortedUrls = new ArrayList<>(urls);
            sortedUrls.sort(comp);

            Map<String, Integer> sortedIndex = new HashMap<>();
            for (int i = 0; i < sortedUrls.size(); i++) {
                sortedIndex.put(sortedUrls.get(i), i);
            }

            List<String> newUrls = new ArrayList<>(urls.size());
            List<Boolean> newChecked = new ArrayList<>(urls.size());
            for (int i = 0; i < urls.size(); i++) {
                newUrls.add(null);
                newChecked.add(false);
            }

            for (int i = 0; i < urls.size(); i++) {
                int pos = sortedIndex.get(urls.get(i));
                newUrls.set(pos, urls.get(i));
                newChecked.set(pos, checked.get(i));
            }
            this.urls = newUrls;
            this.checked = newChecked;

            fireTableDataChanged();
        }
    }
}
