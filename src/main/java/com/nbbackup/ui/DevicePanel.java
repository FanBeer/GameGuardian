package com.nbbackup.ui;

import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.model.Brand;
import com.nbbackup.ui.dialogs.AddDeviceDialog;
import com.nbbackup.ui.dialogs.EditDeviceDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class DevicePanel extends JPanel {
    private JTable deviceTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> brandFilter;
    private JComboBox<String> statusFilter;
    private DatabaseHelper db;
    private MainFrame mainFrame;

    private final String[] COLUMNS = {"ID", "品牌", "型号", "IP地址", "端口", "用户名", "状态", "最后备份时间"};

    public DevicePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.db = DatabaseHelper.getInstance();
        setLayout(new BorderLayout());
        setBackground(new Color(236, 240, 241));
        initComponents();
        loadDevices();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbar.setBackground(Color.WHITE);

        JButton addBtn = createButton("添加设备", new Color(39, 174, 96));
        addBtn.addActionListener(e -> showAddDeviceDialog());

        JButton editBtn = createButton("编辑", new Color(52, 152, 219));
        editBtn.addActionListener(e -> editDevice());

        JButton deleteBtn = createButton("删除", new Color(231, 76, 60));
        deleteBtn.addActionListener(e -> deleteDevice());

        JButton backupBtn = createButton("备份", new Color(243, 156, 18));
        backupBtn.addActionListener(e -> backupSelectedDevices());

        JButton importBtn = createButton("导入", new Color(155, 89, 182));
        importBtn.addActionListener(e -> importDevices());

        JButton exportBtn = createButton("导出", new Color(52, 73, 94));
        exportBtn.addActionListener(e -> exportDevices());

        JButton refreshBtn = createButton("刷新", new Color(149, 165, 166));
        refreshBtn.addActionListener(e -> loadDevices());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(deleteBtn);
        toolbar.add(backupBtn);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(importBtn);
        toolbar.add(exportBtn);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(refreshBtn);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filterPanel.setBackground(Color.WHITE);

        JLabel searchLabel = new JLabel("搜索:");
        searchField = new JTextField(15);
        searchField.addActionListener(e -> filterDevices());

        JLabel brandLabel = new JLabel("品牌:");
        brandFilter = new JComboBox<>();
        brandFilter.addItem("全部");
        List<Brand> brands = db.getAllBrands();
        for (Brand brand : brands) {
            brandFilter.addItem(brand.getDisplayName());
        }
        brandFilter.addActionListener(e -> filterDevices());

        JLabel statusLabel = new JLabel("状态:");
        statusFilter = new JComboBox<>(new String[]{"全部", "在线", "离线", "未知"});
        statusFilter.addActionListener(e -> filterDevices());

        filterPanel.add(searchLabel);
        filterPanel.add(searchField);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(brandLabel);
        filterPanel.add(brandFilter);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(statusLabel);
        filterPanel.add(statusFilter);

        topPanel.add(toolbar, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        deviceTable = new JTable(tableModel);
        deviceTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        deviceTable.setRowHeight(30);
        deviceTable.getTableHeader().setBackground(new Color(44, 62, 80));
        deviceTable.getTableHeader().setForeground(Color.WHITE);
        deviceTable.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(deviceTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(80, 30));
        btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        return btn;
    }

    public void loadDevices() {
        tableModel.setRowCount(0);
        List<com.nbbackup.model.Device> devices = db.getAllDevices();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (com.nbbackup.model.Device device : devices) {
            Vector<Object> row = new Vector<>();
            row.add(device.getId());
            row.add(device.getBrand());
            row.add(device.getModel());
            row.add(device.getIpAddress());
            row.add(device.getPort());
            row.add(device.getUsername());
            row.add(device.getStatus());
            row.add(device.getLastBackupTime() != null ? sdf.format(device.getLastBackupTime()) : "");
            tableModel.addRow(row);
        }
    }

    private void filterDevices() {
        tableModel.setRowCount(0);
        List<com.nbbackup.model.Device> devices = db.getAllDevices();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        String search = searchField.getText().toLowerCase();
        String selectedBrand = (String) brandFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();

        devices = devices.stream().filter(d -> {
            if (!"全部".equals(selectedBrand) && !d.getBrand().equals(selectedBrand)) {
                return false;
            }
            if (!"全部".equals(selectedStatus) && !d.getStatus().equals(selectedStatus)) {
                return false;
            }
            if (!search.isEmpty()) {
                return d.getIpAddress().toLowerCase().contains(search) ||
                        (d.getModel() != null && d.getModel().toLowerCase().contains(search));
            }
            return true;
        }).collect(Collectors.toList());

        for (com.nbbackup.model.Device device : devices) {
            Vector<Object> row = new Vector<>();
            row.add(device.getId());
            row.add(device.getBrand());
            row.add(device.getModel());
            row.add(device.getIpAddress());
            row.add(device.getPort());
            row.add(device.getUsername());
            row.add(device.getStatus());
            row.add(device.getLastBackupTime() != null ? sdf.format(device.getLastBackupTime()) : "");
            tableModel.addRow(row);
        }
    }

    private void showAddDeviceDialog() {
        AddDeviceDialog dialog = new AddDeviceDialog(mainFrame, db.getAllBrands());
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            com.nbbackup.model.Device device = dialog.getDevice();
            int id = db.addDevice(device);
            if (id > 0) {
                loadDevices();
                JOptionPane.showMessageDialog(this, "设备添加成功！");
            } else {
                JOptionPane.showMessageDialog(this, "设备添加失败！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editDevice() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择要编辑的设备！");
            return;
        }
        int deviceId = (int) tableModel.getValueAt(selectedRow, 0);
        com.nbbackup.model.Device device = db.getDeviceById(deviceId);
        if (device != null) {
            EditDeviceDialog dialog = new EditDeviceDialog(mainFrame, device, db.getAllBrands());
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                db.updateDevice(device);
                loadDevices();
                JOptionPane.showMessageDialog(this, "设备更新成功！");
            }
        }
    }

    private void deleteDevice() {
        int[] selectedRows = deviceTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的设备！");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除选中的 " + selectedRows.length + " 个设备吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            for (int row : selectedRows) {
                int deviceId = (int) tableModel.getValueAt(row, 0);
                db.deleteDevice(deviceId);
            }
            loadDevices();
            JOptionPane.showMessageDialog(this, "删除成功！");
        }
    }

    private void backupSelectedDevices() {
        int[] selectedRows = deviceTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "请选择要备份的设备！");
            return;
        }
        mainFrame.startBackup(selectedRows, tableModel);
    }

    private void importDevices() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON文件", "json"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this, "导入功能开发中...");
        }
    }

    private void exportDevices() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON文件", "json"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                List<com.nbbackup.model.Device> devices = db.getAllDevices();
                StringBuilder json = new StringBuilder("[\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < devices.size(); i++) {
                    com.nbbackup.model.Device d = devices.get(i);
                    json.append("  {\n");
                    json.append("    \"brand\": \"").append(d.getBrand()).append("\",\n");
                    json.append("    \"model\": \"").append(d.getModel()).append("\",\n");
                    json.append("    \"ipAddress\": \"").append(d.getIpAddress()).append("\",\n");
                    json.append("    \"port\": ").append(d.getPort()).append(",\n");
                    json.append("    \"username\": \"").append(d.getUsername()).append("\",\n");
                    json.append("    \"remark\": \"").append(d.getRemark()).append("\"\n");
                    json.append("  }");
                    if (i < devices.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("]");
                FileWriter writer = new FileWriter(file);
                writer.write(json.toString());
                writer.close();
                JOptionPane.showMessageDialog(this, "导出成功！");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "导出失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
