package com.nbbackup.ui.dialogs;

import com.nbbackup.model.Brand;
import com.nbbackup.model.Device;
import com.nbbackup.util.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EditDeviceDialog extends JDialog {
    private JComboBox<Brand> brandCombo;
    private JTextField modelField;
    private JTextField ipField;
    private JSpinner portSpinner;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField remarkField;
    private JComboBox<String> statusCombo;
    private boolean confirmed = false;
    private Device device;

    public EditDeviceDialog(Frame parent, Device device, List<Brand> brands) {
        super(parent, "编辑设备", true);
        this.device = device;
        setSize(450, 450);
        setLocationRelativeTo(parent);
        setResizable(false);
        initComponents(brands);
        populateFields();
    }

    private void initComponents(List<Brand> brands) {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("品牌:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        brandCombo = new JComboBox<>(brands.toArray(new Brand[0]));
        brandCombo.setPreferredSize(new Dimension(250, 30));
        mainPanel.add(brandCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("型号:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        modelField = new JTextField(20);
        modelField.setPreferredSize(new Dimension(250, 30));
        mainPanel.add(modelField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("IP地址:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        ipField = new JTextField(20);
        ipField.setPreferredSize(new Dimension(250, 30));
        mainPanel.add(ipField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("端口:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        portSpinner = new JSpinner(new SpinnerNumberModel(22, 1, 65535, 1));
        portSpinner.setPreferredSize(new Dimension(250, 30));
        mainPanel.add(portSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        usernameField = new JTextField(20);
        usernameField.setPreferredSize(new Dimension(250, 30));
        mainPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("密码:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(250, 30));
        passwordField.setEchoChar('*');
        mainPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("状态:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        statusCombo = new JComboBox<>(new String[]{"在线", "离线", "未知"});
        statusCombo.setPreferredSize(new Dimension(250, 30));
        mainPanel.add(statusCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("备注:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        remarkField = new JTextField(20);
        remarkField.setPreferredSize(new Dimension(250, 30));
        mainPanel.add(remarkField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        JButton cancelBtn = new JButton("取消");
        cancelBtn.setPreferredSize(new Dimension(90, 32));
        cancelBtn.addActionListener(e -> dispose());

        JButton okBtn = new JButton("保存");
        okBtn.setBackground(new Color(39, 174, 96));
        okBtn.setForeground(Color.WHITE);
        okBtn.setFocusPainted(false);
        okBtn.setPreferredSize(new Dimension(90, 32));
        okBtn.addActionListener(e -> validateAndSave());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.weighty = 1;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);
    }

    private void populateFields() {
        for (int i = 0; i < brandCombo.getItemCount(); i++) {
            Brand brand = brandCombo.getItemAt(i);
            if (brand.getDisplayName().equals(device.getBrand())) {
                brandCombo.setSelectedIndex(i);
                break;
            }
        }

        modelField.setText(device.getModel());
        ipField.setText(device.getIpAddress());
        portSpinner.setValue(device.getPort());
        usernameField.setText(device.getUsername());
        passwordField.setText(device.getPassword());
        statusCombo.setSelectedItem(device.getStatus());
        remarkField.setText(device.getRemark());
    }

    private void validateAndSave() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "IP地址不能为空！", "验证错误", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!isValidIpAddress(ip)) {
            JOptionPane.showMessageDialog(this, "请输入有效的IP地址！", "验证错误", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Brand selectedBrand = (Brand) brandCombo.getSelectedItem();
        device.setBrand(selectedBrand.getDisplayName());
        device.setModel(modelField.getText().trim());
        device.setIpAddress(ip);
        device.setPort((int) portSpinner.getValue());
        device.setUsername(usernameField.getText().trim());
        String newPassword = new String(passwordField.getPassword());
        if (!newPassword.isEmpty()) {
            device.setPassword(newPassword);
        }
        device.setStatus((String) statusCombo.getSelectedItem());
        device.setRemark(remarkField.getText().trim());

        confirmed = true;
        dispose();
    }

    private boolean isValidIpAddress(String ip) {
        String pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(pattern);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
