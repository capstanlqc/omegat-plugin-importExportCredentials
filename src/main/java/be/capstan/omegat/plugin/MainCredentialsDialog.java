package be.capstan.omegat.plugin;

import org.omegat.core.team2.TeamSettings;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class MainCredentialsDialog extends JDialog {
    private static final ResourceBundle res = ResourceBundle.getBundle("ImportExportCredentials", Locale.getDefault());

    private final JTable table;
    private final CredentialsTableModel tableModel;
    private boolean ascendingSort = true;

    public MainCredentialsDialog(Frame owner) {
        super(owner, res.getString("icp.main.dialog.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Main panel with table and buttons
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table
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

        // Configure table columns
        TableColumn checkboxColumn = table.getColumnModel().getColumn(0);
        checkboxColumn.setMaxWidth(50);
        checkboxColumn.setMinWidth(50);
        checkboxColumn.setResizable(false);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Table header with click handlers
        JTableHeader th = table.getTableHeader();
        th.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col == 1) { // Sort by URL column
                    ascendingSort = !ascendingSort;
                    tableModel.sortByUrl(ascendingSort);
                } else if (col == 0) {
                    // Toggle select/deselect all
                    boolean anyUnselected = tableModel.isAnyRowUnselected();
                    tableModel.selectAll(anyUnselected);
                }
            }
        });

        th.setToolTipText(res.getString("icp.main.table.headerTip"));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Right panel with vertically stacked buttons
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // Create buttons in the new order: Import, Add, Edit, Export, Delete
        JButton importButton = new JButton(res.getString("icp.main.import"));
        JButton addButton = new JButton(res.getString("icp.main.addManually"));
        JButton editButton = new JButton(res.getString("icp.main.edit"));
        JButton exportButton = new JButton(res.getString("icp.main.exportSelected"));
        JButton deleteButton = new JButton(res.getString("icp.main.deleteSelected"));

        // Set smaller margins (internal padding) for all buttons
        Insets smallMargin = new Insets(2, 8, 2, 8); // top, left, bottom, right
        JButton[] buttons = {importButton, addButton, editButton, exportButton, deleteButton};
        for (JButton btn : buttons) {
            btn.setMargin(smallMargin);
        }

        // Calculate the maximum button width needed
        int maxWidth = 0;
        FontMetrics fm = importButton.getFontMetrics(importButton.getFont());
        for (JButton btn : buttons) {
            int textWidth = fm.stringWidth(btn.getText());
            // Get the button's border insets
            Insets insets = btn.getInsets();
            int totalInsets = insets.left + insets.right;
            // Calculate required width: text + insets + small extra padding
            int requiredWidth = textWidth + totalInsets + 16; // 16px extra for safety
            maxWidth = Math.max(maxWidth, requiredWidth);
        }

        int buttonHeight = 30;

        // Set uniform size for all buttons
        Dimension buttonSize = new Dimension(maxWidth, buttonHeight);
        for (JButton btn : buttons) {
            btn.setMaximumSize(buttonSize);
            btn.setPreferredSize(buttonSize);
            btn.setMinimumSize(buttonSize);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        // Add action listeners
        importButton.addActionListener(e -> handleImport());
        addButton.addActionListener(e -> handleAddManually());
        editButton.addActionListener(e -> handleEdit());
        exportButton.addActionListener(e -> handleExportSelected());
        deleteButton.addActionListener(e -> handleDeleteSelected());

        // Add buttons with spacing in order: Import, Add, Edit, Export, Delete
        rightPanel.add(importButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(addButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(editButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(exportButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(deleteButton);
        rightPanel.add(Box.createVerticalGlue()); // Push buttons to top

        mainPanel.add(rightPanel, BorderLayout.EAST);
        add(mainPanel, BorderLayout.CENTER);

        // Bottom panel with Close button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton closeButton = new JButton(res.getString("icp.main.close"));
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        setMinimumSize(new Dimension(650, 400));
    }

    private void handleImport() {
        ImportFromFileDialog.showDialog(this, this::refreshTable);
    }

    private void handleAddManually() {
        AddCredentialsDialog dialog = new AddCredentialsDialog(
                (Frame) getOwner(), this::refreshTable);
        dialog.setVisible(true);
    }

    private void handleEdit() {
        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            // No highlighted row, show message
            JOptionPane.showMessageDialog(this,
                    res.getString("icp.main.noRowSelected"),
                    res.getString("icp.main.warningTitle"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String url = (String) tableModel.getValueAt(selectedRow, 1);
        EditCredentialsDialog dialog = new EditCredentialsDialog(
                (Frame) getOwner(), url, this::refreshTable);
        dialog.setVisible(true);
    }

    private void handleExportSelected() {
        List<String> selectedUrls = getSelectedUrls();

        // If no checkboxes selected, use highlighted row
        if (selectedUrls.isEmpty()) {
            int highlightedRow = table.getSelectedRow();

            if (highlightedRow < 0) {
                // No highlighted row either, show message
                JOptionPane.showMessageDialog(this,
                        res.getString("icp.main.noSelection"),
                        res.getString("icp.main.warningTitle"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Use the highlighted row
            String url = (String) tableModel.getValueAt(highlightedRow, 1);
            selectedUrls = new ArrayList<>();
            selectedUrls.add(url);
        }

        JFileChooser saveChooser = new JFileChooser(System.getProperty("user.home"));
        saveChooser.setDialogTitle(res.getString("icp.main.exportDialogTitle"));
        saveChooser.setSelectedFile(new File("export_credentials.properties"));
        saveChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                res.getString("icp.main.fileFilter"), "properties"));

        int result = saveChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = saveChooser.getSelectedFile();
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {

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

            exportProps.store(writer, null);
            writer.flush();

            JOptionPane.showMessageDialog(this,
                    res.getString("icp.main.exportSuccess")
                            .replace("{count}", Integer.toString(selectedUrls.size())),
                    res.getString("icp.main.informationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    res.getString("icp.main.exportError") + e.getMessage(),
                    res.getString("icp.main.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteSelected() {
        List<String> selectedUrls = getSelectedUrls();

        // If no checkboxes selected, use highlighted row
        if (selectedUrls.isEmpty()) {
            int highlightedRow = table.getSelectedRow();

            if (highlightedRow < 0) {
                // No highlighted row either, show message
                JOptionPane.showMessageDialog(this,
                        res.getString("icp.main.noSelection"),
                        res.getString("icp.main.warningTitle"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Use the highlighted row
            String url = (String) tableModel.getValueAt(highlightedRow, 1);
            selectedUrls = new ArrayList<>();
            selectedUrls.add(url);
        }

        int choice = JOptionPane.showConfirmDialog(this,
                res.getString("icp.main.deleteConfirm")
                        .replace("{count}", Integer.toString(selectedUrls.size())),
                res.getString("icp.main.deleteTitle"),
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

            JOptionPane.showMessageDialog(this,
                    res.getString("icp.main.deleteSuccess")
                            .replace("{count}", Integer.toString(selectedUrls.size())),
                    res.getString("icp.main.informationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);

            refreshTable();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    res.getString("icp.main.deleteError") + e.getMessage(),
                    res.getString("icp.main.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshTable() {
        List<String> urls = loadUniqueUrls();
        tableModel.setUrls(urls);
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
            // Log or ignore
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

    // Inner class: CredentialsTableModel
    private static class CredentialsTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES;
        private final Class<?>[] COLUMN_CLASSES = { Boolean.class, String.class };

        private List<String> urls;
        private List<Boolean> checked;

        CredentialsTableModel(List<String> urls) {
            COLUMN_NAMES = new String[] {
                res.getString("icp.main.table.checkLabel"),
                res.getString("icp.main.table.urlLabel")
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
            Comparator<String> comp = ascending ? 
                    Comparator.naturalOrder() : Comparator.reverseOrder();

            List<String> sortedUrls = new ArrayList<>(urls);
            sortedUrls.sort(comp);

            Map<String, Integer> sortedIndex = new HashMap<>();
            for (int i = 0; i < sortedUrls.size(); i++) {
                sortedIndex.put(sortedUrls.get(i), i);
            }

            List<String> newUrls = new ArrayList<>(Collections.nCopies(urls.size(), null));
            List<Boolean> newChecked = new ArrayList<>(Collections.nCopies(urls.size(), false));

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
