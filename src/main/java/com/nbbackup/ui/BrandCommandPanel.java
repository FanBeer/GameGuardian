package com.nbbackup.ui;

import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.model.Brand;
import com.nbbackup.model.CommandTemplate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class BrandCommandPanel extends JPanel {
    private JList<Brand> brandList;
    private JTable commandTable;
    private DefaultTableModel tableModel;
    private DatabaseHelper db;
    private JTextArea commandEditor;
    private Brand selectedBrand;

    private final String[] COLUMNS = {"ID", "命令类型", "命令", "描述"};

    public BrandCommandPanel() {
        this.db = DatabaseHelper.getInstance();
        setLayout(new BorderLayout());
        setBackground(new Color(236, 240, 241));
        initComponents();
    }

    private void initComponents() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);

        JPanel leftPanel = createBrandListPanel();
        JPanel rightPanel = createCommandPanel();

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createBrandListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 152, 219)),
                "品牌列表",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Microsoft YaHei", Font.BOLD, 13),
                new Color(44, 62, 80)
        ));

        ListModel<Brand> listModel = new DefaultListModel<>();
        List<Brand> brands = db.getAllBrands();
        for (Brand brand : brands) {
            ((DefaultListModel<Brand>) listModel).addElement(brand);
        }

        brandList = new JList<>(listModel);
        brandList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        brandList.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        brandList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                return label;
            }
        });

        brandList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedBrand = brandList.getSelectedValue();
                if (selectedBrand != null) {
                    loadCommands();
                }
            }
        });

        if (brands.size() > 0) {
            brandList.setSelectedIndex(0);
        }

        JScrollPane scrollPane = new JScrollPane(brandList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCommandPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 152, 219)),
                "命令模板",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Microsoft YaHei", Font.BOLD, 13),
                new Color(44, 62, 80)
        ));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }
        };

        commandTable = new JTable(tableModel);
        commandTable.setRowHeight(30);
        commandTable.getTableHeader().setBackground(new Color(44, 62, 80));
        commandTable.getTableHeader().setForeground(Color.WHITE);
        commandTable.getColumnModel().getColumn(2).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(commandTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        JButton saveBtn = new JButton("保存修改");
        saveBtn.setBackground(new Color(39, 174, 96));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setPreferredSize(new Dimension(100, 30));
        saveBtn.addActionListener(e -> saveCommands());

        JButton refreshBtn = new JButton("刷新");
        refreshBtn.setBackground(new Color(149, 165, 166));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setPreferredSize(new Dimension(80, 30));
        refreshBtn.addActionListener(e -> {
            if (selectedBrand != null) {
                loadCommands();
            }
        });

        buttonPanel.add(refreshBtn);
        buttonPanel.add(saveBtn);

        topPanel.add(scrollPane, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBackground(Color.WHITE);
        editorPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(243, 156, 18)),
                "命令编辑",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Microsoft YaHei", Font.BOLD, 13),
                new Color(44, 62, 80)
        ));

        commandEditor = new JTextArea();
        commandEditor.setFont(new Font("Consolas", Font.PLAIN, 13));
        commandEditor.setLineWrap(true);
        commandEditor.setWrapStyleWord(true);
        JScrollPane editorScroll = new JScrollPane(commandEditor);
        editorScroll.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));

        JPanel editorButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        editorButtonPanel.setBackground(Color.WHITE);

        JButton applyBtn = new JButton("应用到选中行");
        applyBtn.setBackground(new Color(52, 152, 219));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setFocusPainted(false);
        applyBtn.addActionListener(e -> applyToSelectedRow());

        editorButtonPanel.add(applyBtn);

        editorPanel.add(editorScroll, BorderLayout.CENTER);
        editorPanel.add(editorButtonPanel, BorderLayout.SOUTH);

        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setTopComponent(topPanel);
        rightSplitPane.setBottomComponent(editorPanel);
        rightSplitPane.setDividerLocation(300);
        rightSplitPane.setOneTouchExpandable(true);

        commandTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = commandTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String command = (String) tableModel.getValueAt(selectedRow, 2);
                    commandEditor.setText(command);
                }
            }
        });

        panel.add(rightSplitPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadCommands() {
        tableModel.setRowCount(0);
        if (selectedBrand == null) return;

        List<CommandTemplate> commands = db.getCommandsByBrandId(selectedBrand.getId());
        for (CommandTemplate cmd : commands) {
            Vector<Object> row = new Vector<>();
            row.add(cmd.getId());
            row.add(cmd.getCommandType());
            row.add(cmd.getCommand());
            row.add(cmd.getDescription());
            tableModel.addRow(row);
        }
    }

    private void applyToSelectedRow() {
        int selectedRow = commandTable.getSelectedRow();
        if (selectedRow >= 0) {
            String newCommand = commandEditor.getText();
            tableModel.setValueAt(newCommand, selectedRow, 2);
        } else {
            JOptionPane.showMessageDialog(this, "请先选择要编辑的行！");
        }
    }

    private void saveCommands() {
        if (selectedBrand == null) return;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int id = (int) tableModel.getValueAt(i, 0);
            String command = (String) tableModel.getValueAt(i, 2);
            db.updateCommand(id, command);
        }

        JOptionPane.showMessageDialog(this, "保存成功！");
        loadCommands();
    }
}
