package com.nbbackup.controller;

import com.nbbackup.common.R;
import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.model.Device;
import com.nbbackup.util.PasswordUtil;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DatabaseHelper db = DatabaseHelper.getInstance();

    @GetMapping
    public R<List<Device>> getAllDevices(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            List<Device> devices = db.getAllDevices();

            if (brand != null && !brand.isEmpty()) {
                devices.removeIf(d -> !brand.equals(d.getBrand()));
            }

            if (status != null && !status.isEmpty()) {
                devices.removeIf(d -> !status.equals(d.getStatus()));
            }

            if (keyword != null && !keyword.isEmpty()) {
                String kw = keyword.toLowerCase();
                devices.removeIf(d -> {
                    String ip = d.getIpAddress() == null ? "" : d.getIpAddress().toLowerCase();
                    String model = d.getModel() == null ? "" : d.getModel().toLowerCase();
                    return !ip.contains(kw) && !model.contains(kw);
                });
            }

            return R.ok(devices);
        } catch (Exception e) {
            return R.fail("获取设备列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public R<Device> getDeviceById(@PathVariable int id) {
        try {
            Device device = db.getDeviceById(id);
            if (device == null) {
                return R.fail(404, "设备不存在");
            }
            device.setPassword(""); // 不返回密码
            return R.ok(device);
        } catch (Exception e) {
            return R.fail("获取设备信息失败: " + e.getMessage());
        }
    }

    @PostMapping
    public R<Integer> addDevice(@RequestBody Device device) {
        try {
            if (device.getIpAddress() == null || device.getIpAddress().isEmpty()) {
                return R.fail("IP地址不能为空");
            }
            if (device.getBrand() == null || device.getBrand().isEmpty()) {
                device.setBrand("华为");
            }
            if (device.getStatus() == null || device.getStatus().isEmpty()) {
                device.setStatus("未知");
            }
            if (device.getPort() == 0) {
                device.setPort(22);
            }

            int id = db.addDevice(device);
            if (id > 0) {
                return R.ok("设备添加成功", id);
            }
            return R.fail("设备添加失败");
        } catch (Exception e) {
            return R.fail("添加设备失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public R<String> updateDevice(@PathVariable int id, @RequestBody Device device) {
        try {
            Device existing = db.getDeviceById(id);
            if (existing == null) {
                return R.fail(404, "设备不存在");
            }

            device.setId(id);
            // 如果密码为空则保持原密码
            if (device.getPassword() == null || device.getPassword().isEmpty()) {
                device.setPassword(existing.getPassword());
            } else {
                device.setPassword(PasswordUtil.encrypt(device.getPassword()));
            }

            boolean success = db.updateDevice(device);
            if (success) {
                return R.ok("设备更新成功", null);
            }
            return R.fail("设备更新失败");
        } catch (Exception e) {
            return R.fail("更新设备失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public R<String> deleteDevice(@PathVariable int id) {
        try {
            boolean success = db.deleteDevice(id);
            if (success) {
                return R.ok("设备删除成功", null);
            }
            return R.fail("设备删除失败");
        } catch (Exception e) {
            return R.fail("删除设备失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    public R<String> deleteDevices(@RequestBody List<Integer> ids) {
        try {
            int count = 0;
            for (Integer id : ids) {
                if (db.deleteDevice(id)) {
                    count++;
                }
            }
            return R.ok("成功删除 " + count + " 台设备", null);
        } catch (Exception e) {
            return R.fail("批量删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/test-connection")
    public R<String> testConnection(@PathVariable int id) {
        try {
            Device device = db.getDeviceById(id);
            if (device == null) {
                return R.fail(404, "设备不存在");
            }
            com.nbbackup.service.SshService sshService = new com.nbbackup.service.SshService();
            boolean connected = sshService.testConnection(device);

            if (connected) {
                db.updateDeviceStatus(id, "在线");
                return R.ok("连接成功", null);
            } else {
                db.updateDeviceStatus(id, "离线");
                return R.fail("连接失败，请检查网络、账号密码或SSH配置");
            }
        } catch (Exception e) {
            return R.fail("连接测试失败: " + e.getMessage());
        }
    }
}
