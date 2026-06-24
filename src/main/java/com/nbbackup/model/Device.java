package com.nbbackup.model;

import java.util.Date;

public class Device {
    private int id;
    private String brand;
    private String model;
    private String ipAddress;
    private int port;
    private String username;
    private String password;
    private String status;
    private String remark;
    private Date createTime;
    private Date lastBackupTime;

    public Device() {
        this.status = "未知";
        this.port = 22;
        this.createTime = new Date();
    }

    public Device(String brand, String model, String ipAddress, int port, String username, String password) {
        this();
        this.brand = brand;
        this.model = model;
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getLastBackupTime() { return lastBackupTime; }
    public void setLastBackupTime(Date lastBackupTime) { this.lastBackupTime = lastBackupTime; }
}
