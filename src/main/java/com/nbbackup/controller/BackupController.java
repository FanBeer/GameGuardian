package com.nbbackup.controller;

import com.nbbackup.common.R;
import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.dto.BackupRequest;
import com.nbbackup.model.BackupTask;
import com.nbbackup.model.CommandTemplate;
import com.nbbackup.model.Device;
import com.nbbackup.model.TaskLog;
import com.nbbackup.service.SshService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final DatabaseHelper db = DatabaseHelper.getInstance();
    private final SshService sshService = new SshService();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // 记录当前正在执行的任务
    private final Map<Integer, BackupTask> runningTasks = new ConcurrentHashMap<>();

    @PostMapping("/execute")
    public R<List<BackupTask>> executeBackup(@RequestBody BackupRequest request) {
        try {
            List<Integer> deviceIds = request.getDeviceIds();
            if (deviceIds == null || deviceIds.isEmpty()) {
                return R.fail("请选择要备份的设备");
            }

            List<BackupTask> tasks = new ArrayList<>();
            AtomicInteger counter = new AtomicInteger(0);

            for (Integer deviceId : deviceIds) {
                Device device = db.getDeviceById(deviceId);
                if (device == null) {
                    continue;
                }

                BackupTask task = new BackupTask(device.getId(), device.getIpAddress(), device.getBrand());
                task.setStatus("执行中");
                int taskId = db.addTask(task);
                task.setId(taskId);
                tasks.add(task);

                // 异步执行备份
                executorService.submit(() -> {
                    try {
                        db.addLog(new TaskLog(taskId, "INFO",
                                "开始备份任务，设备: " + device.getIpAddress()));

                        // 获取命令模板
                        List<CommandTemplate> commands = db.getCommandsByBrandName(
                                device.getBrand() == null ? "华为" : device.getBrand());

                        // 执行SSH命令
                        SshService.CommandResult result = sshService.executeCommandsSync(device, commands);

                        task.setEndTime(new java.util.Date());
                        if (result.isSuccess()) {
                            task.setStatus("成功");
                            task.setOutput(result.getOutput());
                            db.addLog(new TaskLog(taskId, "SUCCESS", "备份成功"));
                            db.updateDeviceStatus(deviceId, "在线");
                            db.updateDeviceLastBackupTime(deviceId);
                        } else {
                            task.setStatus("失败");
                            task.setErrorInfo(result.getError());
                            db.addLog(new TaskLog(taskId, "ERROR", "备份失败: " + result.getError()));
                            db.updateDeviceStatus(deviceId, "离线");
                        }
                        db.updateTask(task);
                    } catch (Exception e) {
                        task.setStatus("失败");
                        task.setEndTime(new java.util.Date());
                        task.setErrorInfo(e.getMessage());
                        db.updateTask(task);
                        db.addLog(new TaskLog(taskId, "ERROR", "任务执行异常: " + e.getMessage()));
                    } finally {
                        counter.incrementAndGet();
                    }
                });
            }

            return R.ok("已提交 " + tasks.size() + " 个备份任务到后台执行", tasks);
        } catch (Exception e) {
            return R.fail("备份任务启动失败: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public R<Map<Integer, BackupTask>> getRunningTasks() {
        return R.ok(runningTasks);
    }
}
