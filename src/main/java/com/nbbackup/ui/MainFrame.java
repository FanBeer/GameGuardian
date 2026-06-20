package com.nbbackup.ui;

import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.model.BackupTask;
import com.nbbackup.model.CommandTemplate;
import com.nbbackup.model.Device;
import com.nbbackup.model.TaskLog;
import com.nbbackup.service.SshService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MainFrame extends JFrame {
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private DevicePanel devicePanel;
    private BrandCommandPanel brandCommandPanel;
    private TaskLogPanel taskLogPanel;
    private DatabaseHelper db;
    private SshService sshService;
    private JLabel statusLabel;
    private AtomicInteger runningTasks;

    public MainFrame() {
        this.db = DatabaseHelper.getInstance();
        this.sshService = new SshService();
        this.runningTasks = new AtomicInteger(0);

        setTitle("网络设备配置备份系统");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(44, 62, 80));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titleLabel = new JLabel("网络设备配置备份系统");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel, BorderLayout.WEST);

        statusLabel = new JLabel("就绪");
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(149, 165, 166));
        titlePanel.add(statusLabel, BorderLayout.EAST);

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(new Color(44, 62, 80));
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JButton deviceBtn = createNavButton("设备资产列表", 0);
        JButton brandCmdBtn = createNavButton("品牌命令配置", 1);
        JButton taskLogBtn = createNavButton("任务日志", 2);

        navPanel.add(Box.createVerticalGlue());
        navPanel.add(deviceBtn);
        navPanel.add(Box.createVerticalStrut(5));
        navPanel.add(brandCmdBtn);
        navPanel.add(Box.createVerticalStrut(5));
        navPanel.add(taskLogBtn);
        navPanel.add(Box.createVerticalGlue());

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(236, 240, 241));

        devicePanel = new DevicePanel(this);
        brandCommandPanel = new BrandCommandPanel();
        taskLogPanel = new TaskLogPanel(this);

        contentPanel.add(devicePanel, "DEVICE");
        contentPanel.add(brandCommandPanel, "BRAND_CMD");
        contentPanel.add(taskLogPanel, "TASK_LOG");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(navPanel);
        splitPane.setRightComponent(contentPanel);
        splitPane.setDividerLocation(180);
        splitPane.setDividerSize(5);
        splitPane.setOneTouchExpandable(true);

        add(titlePanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        deviceBtn.addActionListener(e -> {
            cardLayout.show(contentPanel, "DEVICE");
            updateNavButton(deviceBtn, brandCmdBtn, taskLogBtn);
        });
        brandCmdBtn.addActionListener(e -> {
            cardLayout.show(contentPanel, "BRAND_CMD");
            updateNavButton(brandCmdBtn, deviceBtn, taskLogBtn);
        });
        taskLogBtn.addActionListener(e -> {
            cardLayout.show(contentPanel, "TASK_LOG");
            taskLogPanel.refreshTasks();
            updateNavButton(taskLogBtn, deviceBtn, brandCmdBtn);
        });

        updateNavButton(deviceBtn, brandCmdBtn, taskLogBtn);
    }

    private JButton createNavButton(String text, int index) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(180, 45));
        btn.setMinimumSize(new Dimension(180, 45));
        btn.setPreferredSize(new Dimension(180, 45));
        btn.setFocusPainted(false);
        btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.WHITE);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        btn.setName(String.valueOf(index));
        return btn;
    }

    private void updateNavButton(JButton active, JButton... inactive) {
        active.setBackground(new Color(52, 152, 219));
        active.setOpaque(true);
        for (JButton btn : inactive) {
            btn.setOpaque(false);
            btn.setBackground(null);
        }
    }

    public void startBackup(int[] selectedRows, DefaultTableModel tableModel) {
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "请选择要备份的设备！");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("即将备份 ").append(selectedRows.length).append(" 个设备:\n\n");
        for (int row : selectedRows) {
            sb.append("- ").append(tableModel.getValueAt(row, 2))
              .append(" (").append(tableModel.getValueAt(row, 3)).append(")\n");
        }
        sb.append("\n是否继续？");

        int confirm = JOptionPane.showConfirmDialog(this, sb.toString(), "确认备份", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        runningTasks.addAndGet(selectedRows.length);
        updateStatus("正在备份 " + selectedRows.length + " 个设备...");

        for (int row : selectedRows) {
            int deviceId = (int) tableModel.getValueAt(row, 0);
            Device device = db.getDeviceById(deviceId);
            if (device != null) {
                final int taskRow = row;
                new Thread(() -> executeBackup(device, tableModel, taskRow)).start();
            }
        }
    }

    private void executeBackup(Device device, DefaultTableModel tableModel, int taskRow) {
        BackupTask task = new BackupTask(device.getId(), device.getIpAddress(), device.getBrand());
        task.setStatus(BackupTask.STATUS_RUNNING);
        int taskId = db.addTask(task);
        task.setId(taskId);

        db.addLog(new TaskLog(taskId, TaskLog.LEVEL_INFO, "开始备份任务，设备: " + device.getIpAddress()));

        SwingUtilities.invokeLater(() -> {
            tableModel.setValueAt("执行中", taskRow, 6);
            statusLabel.setText("正在备份: " + device.getIpAddress());
        });

        String brandKey = device.getBrand().toLowerCase();
        switch (brandKey) {
            case "华为": brandKey = "huawei"; break;
            case "h3c": brandKey = "h3c"; break;
            case "思科": brandKey = "cisco"; break;
            case "锐捷": brandKey = "ruijie"; break;
            case "中兴": brandKey = "zte"; break;
            case "迪普": brandKey = "dptech"; break;
        }

        List<CommandTemplate> commands = db.getCommandsByBrandName(brandKey);
        if (commands.isEmpty()) {
            commands = db.getCommandsByBrandName("huawei");
        }

        SshService.CommandResult result = sshService.executeCommandsSync(device, commands);

        task.setEndTime(new Date());
        if (result.isSuccess()) {
            task.setStatus(BackupTask.STATUS_SUCCESS);
            task.setOutput(result.getOutput());
            db.addLog(new TaskLog(taskId, TaskLog.LEVEL_SUCCESS, "备份成功"));
            db.updateDeviceLastBackupTime(device.getId());
            SwingUtilities.invokeLater(() -> {
                tableModel.setValueAt("在线", taskRow, 6);
                tableModel.setValueAt(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), taskRow, 7);
            });
        } else {
            task.setStatus(BackupTask.STATUS_FAILED);
            task.setErrorInfo(result.getError());
            db.addLog(new TaskLog(taskId, TaskLog.LEVEL_ERROR, "备份失败: " + result.getError()));
            db.updateDeviceStatus(device.getId(), "离线");
            SwingUtilities.invokeLater(() -> {
                tableModel.setValueAt("离线", taskRow, 6);
            });
        }

        db.updateTask(task);

        int remaining = runningTasks.decrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (remaining <= 0) {
                updateStatus("就绪");
                devicePanel.loadDevices();
                taskLogPanel.refreshTasks();
                JOptionPane.showMessageDialog(this, "备份任务全部完成！");
            } else {
                updateStatus("还有 " + remaining + " 个设备在备份...");
            }
        });
    }

    private void updateStatus(String text) {
        statusLabel.setText(text);
    }
}
