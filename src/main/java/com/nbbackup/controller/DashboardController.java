package com.nbbackup.controller;

import com.nbbackup.common.R;
import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.model.BackupTask;
import com.nbbackup.model.Device;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DatabaseHelper db = DatabaseHelper.getInstance();

    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Device> devices = db.getAllDevices();
            List<BackupTask> tasks = db.getAllTasks();

            int totalDevices = devices.size();
            int onlineDevices = 0;
            int offlineDevices = 0;
            int unknownDevices = 0;
            int successTasks = 0;
            int failedTasks = 0;
            int runningTasks = 0;

            for (Device d : devices) {
                if ("在线".equals(d.getStatus())) {
                    onlineDevices++;
                } else if ("离线".equals(d.getStatus())) {
                    offlineDevices++;
                } else {
                    unknownDevices++;
                }
            }

            for (BackupTask t : tasks) {
                if ("成功".equals(t.getStatus())) {
                    successTasks++;
                } else if ("失败".equals(t.getStatus())) {
                    failedTasks++;
                } else if ("执行中".equals(t.getStatus())) {
                    runningTasks++;
                }
            }

            Map<String, Integer> brandCount = new HashMap<>();
            for (Device d : devices) {
                String brand = d.getBrand() == null ? "未知" : d.getBrand();
                brandCount.put(brand, brandCount.getOrDefault(brand, 0) + 1);
            }

            stats.put("totalDevices", totalDevices);
            stats.put("onlineDevices", onlineDevices);
            stats.put("offlineDevices", offlineDevices);
            stats.put("unknownDevices", unknownDevices);
            stats.put("totalTasks", tasks.size());
            stats.put("successTasks", successTasks);
            stats.put("failedTasks", failedTasks);
            stats.put("runningTasks", runningTasks);
            stats.put("brandDistribution", brandCount);

            return R.ok(stats);
        } catch (Exception e) {
            return R.fail("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/recent-tasks")
    public R<List<BackupTask>> getRecentTasks() {
        try {
            List<BackupTask> tasks = db.getAllTasks();
            int limit = Math.min(tasks.size(), 10);
            return R.ok(tasks.subList(0, limit));
        } catch (Exception e) {
            return R.fail("获取最近任务失败: " + e.getMessage());
        }
    }
}
