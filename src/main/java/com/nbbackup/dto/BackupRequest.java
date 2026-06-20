package com.nbbackup.dto;

import java.util.List;

public class BackupRequest {
    private List<Integer> deviceIds;

    public List<Integer> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(List<Integer> deviceIds) {
        this.deviceIds = deviceIds;
    }
}
