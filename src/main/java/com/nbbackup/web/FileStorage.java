package com.nbbackup.web;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileStorage {

    private static final String DATA_DIR = "data";
    private static final String DEVICES_FILE = DATA_DIR + "/devices.json";
    private static final String COMMANDS_FILE = DATA_DIR + "/commands.json";
    private static final String TASKS_FILE = DATA_DIR + "/tasks.json";
    private static final String LOGS_FILE = DATA_DIR + "/logs.json";

    private static FileStorage instance;

    public static synchronized FileStorage getInstance() {
        if (instance == null) {
            instance = new FileStorage();
        }
        return instance;
    }

    private FileStorage() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            initDefaults();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initDefaults() {
        if (!Files.exists(Paths.get(COMMANDS_FILE))) {
            List<Map<String, Object>> commands = new ArrayList<>();
            String[] brands = { "华为", "H3C", "思科", "锐捷", "中兴", "迪普" };
            String[][] cmdTypes = {
                { "测试连接", "display version", "测试设备连接" },
                { "备份配置", "display current-configuration", "获取当前配置" },
                { "保存配置", "save", "保存配置" }
            };

            int id = 1;
            for (int i = 0; i < brands.length; i++) {
                for (int j = 0; j < cmdTypes.length; j++) {
                    Map<String, Object> cmd = new LinkedHashMap<>();
                    cmd.put("id", id++);
                    cmd.put("brandName", brands[i]);
                    cmd.put("commandType", cmdTypes[j][0]);
                    cmd.put("command", cmdTypes[j][1]);
                    cmd.put("description", cmdTypes[j][2]);
                    commands.add(cmd);
                }
            }
            writeJson(COMMANDS_FILE, commands);
        }
        if (!Files.exists(Paths.get(DEVICES_FILE))) {
            writeJson(DEVICES_FILE, new ArrayList<>());
        }
        if (!Files.exists(Paths.get(TASKS_FILE))) {
            writeJson(TASKS_FILE, new ArrayList<>());
        }
        if (!Files.exists(Paths.get(LOGS_FILE))) {
            writeJson(LOGS_FILE, new ArrayList<>());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadDevices() {
        Object obj = readJson(DEVICES_FILE);
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return new ArrayList<>();
    }

    public void saveDevices(List<Map<String, Object>> devices) {
        writeJson(DEVICES_FILE, devices);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadCommands() {
        Object obj = readJson(COMMANDS_FILE);
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return new ArrayList<>();
    }

    public void saveCommands(List<Map<String, Object>> commands) {
        writeJson(COMMANDS_FILE, commands);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadTasks() {
        Object obj = readJson(TASKS_FILE);
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return new ArrayList<>();
    }

    public void saveTasks(List<Map<String, Object>> tasks) {
        writeJson(TASKS_FILE, tasks);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadLogs() {
        Object obj = readJson(LOGS_FILE);
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return new ArrayList<>();
    }

    public void saveLogs(List<Map<String, Object>> logs) {
        writeJson(LOGS_FILE, logs);
    }

    public synchronized int nextDeviceId() {
        return findMaxId(loadDevices()) + 1;
    }

    public synchronized int nextTaskId() {
        return findMaxId(loadTasks()) + 1;
    }

    public synchronized int nextLogId() {
        return findMaxId(loadLogs()) + 1;
    }

    private int findMaxId(List<Map<String, Object>> list) {
        int max = 0;
        for (Map<String, Object> m : list) {
            Object id = m.get("id");
            if (id instanceof Number) {
                max = Math.max(max, ((Number) id).intValue());
            }
        }
        return max;
    }

    private Object readJson(String path) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
            if (content.trim().isEmpty()) return new ArrayList<>();
            JsonUtil.JsonParser parser = new JsonUtil.JsonParser(content);
            Object result = parser.parse();
            return result == null ? new ArrayList<>() : result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void writeJson(String path, Object data) {
        try {
            Files.write(Paths.get(path), JsonUtil.toJson(data).getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> getBrands() {
        List<Map<String, Object>> brands = new ArrayList<>();
        String[] names = { "华为", "H3C", "思科", "锐捷", "中兴", "迪普" };
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i + 1);
            m.put("displayName", names[i]);
            m.put("description", names[i] + "网络设备");
            brands.add(m);
        }
        return brands;
    }
}
