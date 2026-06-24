package com.nbbackup.ui;

import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.model.BackupTask;
import com.nbbackup.model.TaskLog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class TaskLogPanel extends JPanel {
    private JTable taskTable;
    private DefaultTableModel taskTableModel;
    private JTextArea logArea;
    private JComboBox<String> statusFilter;
    private JComboBox<String> brandFilter;
    private DatabaseHelper db;
    private MainFrame mainFrame;

    private final String[] TASK_COLUMNS = {"任务ID", "设备IP", "品牌", "状态", "开始时间", "结束时间", "耗时"};

    public TaskLogPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.db = DatabaseHelper.getInstance();
        setLayout(new BorderLayout());
        setBackground(new Color(236, 240, 241));
        initComponents();
        loadTasks();
    }

    private void initComponents() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(250);

        JPanel topPanel = createTaskListPanel();
        JPanel bottomPanel = createLogDetailPanel();

        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createTaskListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(Color.WHITE);

        JLabel brandLabel = new JLabel("品牌:");
        brandFilter = new JComboBox<>();
        brandFilter.addItem("全部");
        brandFilter.addItem("华为");
        brandFilter.addItem("H3C");
        brandFilter.addItem("思科");
        brandFilter.addItem("锐捷");
        brandFilter.addItem("中兴");
        brandFilter.addItem("迪普");
        brandFilter.addActionListener(e -> filterTasks());

        JLabel statusLabel = new JLabel("状态:");
        statusFilter = new JComboBox<>(new String[]{"全部", "等待中", "执行中", "成功", "失败", "部分成功"});
        statusFilter.addActionListener(e -> filterTasks());

        JButton refreshBtn = new JButton("刷新");
        refreshBtn.setBackground(new Color(149, 165, 166));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadTasks());

        JButton clearBtn = new JButton("清空日志");
        clearBtn.setBackground(new Color(231, 76, 60));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> clearLogs());

        filterPanel.add(brandLabel);
        filterPanel.add(brandFilter);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(statusLabel);
        filterPanel.add(statusFilter);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(refreshBtn);
        filterPanel.add(clearBtn);

        taskTableModel = new DefaultTableModel(TASK_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        taskTable = new JTable(taskTableModel);
        taskTable.setRowHeight(28);
        taskTable.getTableHeader().setBackground(new Color(44, 62, 80));
        taskTable.getTableHeader().setForeground(Color.WHITE);
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadTaskLogs();
            }
        });

        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLogDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 152, 219)),
                "日志详情",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Microsoft YaHei", Font.BOLD, 13),
                new Color(44, 62, 80)
        ));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(248, 248, 248));
        logArea.setForeground(new Color(44, 62, 80));
        logArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        JButton exportBtn = new JButton("导出日志");
        exportBtn.setBackground(new Color(52, 73, 94));
        exportBtn.setForeground(Color.WHITE);
        exportBtn.setFocusPainted(false);
        exportBtn.addActionListener(e -> exportLogs());

        buttonPanel.add(exportBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    public void loadTasks() {
        taskTableModel.setRowCount(0);
        List<BackupTask> tasks = db.getAllTasks();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        for (BackupTask task : tasks) {
            Vector<Object> row = new Vector<>();
            row.add(task.getId());
            row.add(task.getDeviceIp());
            row.add(task.getDeviceBrand());
            row.add(task.getStatus());
            row.add(task.getStartTime() != null ? sdf.format(task.getStartTime()) : "");
            row.add(task.getEndTime() != null ? sdf.format(task.getEndTime()) : "");
            row.add(formatDuration(task.getDuration()));
            taskTableModel.addRow(row);
        }
    }

    private void filterTasks() {
        taskTableModel.setRowCount(0);
        List<BackupTask> tasks = db.getAllTasks();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        String selectedBrand = (String) brandFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();

        tasks = tasks.stream().filter(t -> {
            if (!"全部".equals(selectedBrand) && !t.getDeviceBrand().equals(selectedBrand)) {
                return false;
            }
            if (!"全部".equals(selectedStatus) && !t.getStatus().equals(selectedStatus)) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        for (BackupTask task : tasks) {
            Vector<Object> row = new Vector<>();
            row.add(task.getId());
            row.add(task.getDeviceIp());
            row.add(task.getDeviceBrand());
            row.add(task.getStatus());
            row.add(task.getStartTime() != null ? sdf.format(task.getStartTime()) : "");
            row.add(task.getEndTime() != null ? sdf.format(task.getEndTime()) : "");
            row.add(formatDuration(task.getDuration()));
            taskTableModel.addRow(row);
        }
    }

    private void loadTaskLogs() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow < 0) {
            logArea.setText("");
            return;
        }

        int taskId = (int) taskTableModel.getValueAt(selectedRow, 0);
        BackupTask task = db.getAllTasks().stream()
                .filter(t -> t.getId() == taskId)
                .findFirst()
                .orElse(null);

        if (task == null) {
            logArea.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 任务 #").append(taskId).append(" ===\n");
        sb.append("设备IP: ").append(task.getDeviceIp()).append("\n");
        sb.append("设备品牌: ").append(task.getDeviceBrand()).append("\n");
        sb.append("状态: ").append(task.getStatus()).append("\n");
        sb.append("开始时间: ").append(task.getStartTime()).append("\n");
        if (task.getEndTime() != null) {
            sb.append("结束时间: ").append(task.getEndTime()).append("\n");
        }
        sb.append("\n--- 命令回显 ---\n");

        if (task.getOutput() != null && !task.getOutput().isEmpty()) {
            sb.append(task.getOutput());
        } else {
            sb.append("(无回显数据)");
        }

        sb.append("\n\n--- 错误信息 ---\n");
        if (task.getErrorInfo() != null && !task.getErrorInfo().isEmpty()) {
            sb.append(task.getErrorInfo());
        } else {
            sb.append("(无错误)");
        }

        List<TaskLog> logs = db.getLogsByTaskId(taskId);
        if (!logs.isEmpty()) {
            sb.append("\n\n--- 日志记录 ---\n");
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            for (TaskLog log : logs) {
                sb.append("[").append(sdf.format(log.getCreateTime())).append("]");
                sb.append("[").append(log.getLevel()).append("] ");
                sb.append(log.getMessage()).append("\n");
            }
        }

        logArea.setText(sb.toString());
        logArea.setCaretPosition(0);
    }

    private String formatDuration(long millis) {
        if (millis < 0) return "-";
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "秒";
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + "分" + seconds + "秒";
    }

    private void clearLogs() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要清空所有日志吗？",
                "确认清空", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            logArea.setText("");
        }
    }

    private void exportLogs() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择要导出的任务！");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("文本文件", "txt", "log"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.FileWriter writer = new java.io.FileWriter(fileChooser.getSelectedFile());
                writer.write(logArea.getText());
                writer.close();
                JOptionPane.showMessageDialog(this, "导出成功！");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "导出失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshTasks() {
        loadTasks();
    }
}
