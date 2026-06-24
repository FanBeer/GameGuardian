package com.nbbackup.controller;

import com.nbbackup.common.R;
import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.model.BackupTask;
import com.nbbackup.model.TaskLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final DatabaseHelper db = DatabaseHelper.getInstance();

    @GetMapping
    public R<List<BackupTask>> getAllTasks(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String status) {
        try {
            List<BackupTask> tasks = db.getAllTasks();

            if (brand != null && !brand.isEmpty()) {
                tasks.removeIf(t -> !brand.equals(t.getDeviceBrand()));
            }

            if (status != null && !status.isEmpty()) {
                tasks.removeIf(t -> !status.equals(t.getStatus()));
            }

            return R.ok(tasks);
        } catch (Exception e) {
            return R.fail("获取任务列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{taskId}")
    public R<BackupTask> getTaskById(@PathVariable int taskId) {
        try {
            List<BackupTask> tasks = db.getAllTasks();
            for (BackupTask task : tasks) {
                if (task.getId() == taskId) {
                    return R.ok(task);
                }
            }
            return R.fail(404, "任务不存在");
        } catch (Exception e) {
            return R.fail("获取任务详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/{taskId}/logs")
    public R<Map<String, Object>> getTaskLogs(@PathVariable int taskId) {
        try {
            BackupTask task = null;
            List<BackupTask> tasks = db.getAllTasks();
            for (BackupTask t : tasks) {
                if (t.getId() == taskId) {
                    task = t;
                    break;
                }
            }

            if (task == null) {
                return R.fail(404, "任务不存在");
            }

            List<TaskLog> logs = db.getLogsByTaskId(taskId);

            Map<String, Object> result = new HashMap<>();
            result.put("task", task);
            result.put("logs", logs);

            return R.ok(result);
        } catch (Exception e) {
            return R.fail("获取任务日志失败: " + e.getMessage());
        }
    }

    @GetMapping("/{taskId}/output")
    public R<Map<String, String>> getTaskOutput(@PathVariable int taskId) {
        try {
            List<BackupTask> tasks = db.getAllTasks();
            for (BackupTask task : tasks) {
                if (task.getId() == taskId) {
                    Map<String, String> result = new HashMap<>();
                    result.put("output", task.getOutput());
                    result.put("errorInfo", task.getErrorInfo());
                    return R.ok(result);
                }
            }
            return R.fail(404, "任务不存在");
        } catch (Exception e) {
            return R.fail("获取输出失败: " + e.getMessage());
        }
    }
}
