package com.nbbackup.model;

import java.util.Date;

public class BackupTask {
    private int id;
    private int deviceId;
    private String deviceIp;
    private String deviceBrand;
    private String status;
    private Date startTime;
    private Date endTime;
    private String output;
    private String errorInfo;

    public static final String STATUS_WAITING = "等待中";
    public static final String STATUS_RUNNING = "执行中";
    public static final String STATUS_SUCCESS = "成功";
    public static final String STATUS_FAILED = "失败";
    public static final String STATUS_PARTIAL_SUCCESS = "部分成功";

    public BackupTask() {
        this.status = STATUS_WAITING;
        this.startTime = new Date();
    }

    public BackupTask(int deviceId, String deviceIp, String deviceBrand) {
        this();
        this.deviceId = deviceId;
        this.deviceIp = deviceIp;
        this.deviceBrand = deviceBrand;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeviceId() { return deviceId; }
    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }

    public String getDeviceIp() { return deviceIp; }
    public void setDeviceIp(String deviceIp) { this.deviceIp = deviceIp; }

    public String getDeviceBrand() { return deviceBrand; }
    public void setDeviceBrand(String deviceBrand) { this.deviceBrand = deviceBrand; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getErrorInfo() { return errorInfo; }
    public void setErrorInfo(String errorInfo) { this.errorInfo = errorInfo; }

    public long getDuration() {
        if (startTime == null) return 0;
        Date end = endTime != null ? endTime : new Date();
        return end.getTime() - startTime.getTime();
    }
}
