package com.nbbackup.web;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;

/**
 * 轻量级 Web 服务器 - 基于 JDK 内置 HttpServer
 * 无需任何外部依赖，单文件即可运行
 */
public class WebServer {

    private static final int PORT = 8080;
    private static final FileStorage storage = FileStorage.getInstance();
    private static final ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdfShort = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API 路由
        server.createContext("/api/dashboard/stats", WebServer::handleDashboardStats);
        server.createContext("/api/devices", WebServer::handleDevices);
        server.createContext("/api/brands", WebServer::handleBrands);
        server.createContext("/api/brands/commands", WebServer::handleBrandCommands);
        server.createContext("/api/backup/execute", WebServer::handleBackup);
        server.createContext("/api/tasks", WebServer::handleTasks);

        // 静态资源 - 前端页面
        server.createContext("/", WebServer::handleStatic);

        server.setExecutor(null);
        server.start();

        System.out.println("=======================================");
        System.out.println("  网络设备配置备份系统 启动成功");
        System.out.println("  访问地址: http://localhost:" + PORT + "/");
        System.out.println("  轻量级模式: 纯 JDK 运行 (零外部依赖)");
        System.out.println("=======================================");
    }

    // ============ 辅助方法 ============

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int n;
            while ((n = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, n);
            }
            return new String(buffer.toByteArray(), "UTF-8");
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(parts[0], "UTF-8");
                String value = parts.length > 1 ? URLDecoder.decode(parts[1], "UTF-8") : "";
                params.put(key, value);
            } catch (Exception e) {}
        }
        return params;
    }

    private static void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
        String json = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes("UTF-8");

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        headers.set("Content-Length", String.valueOf(bytes.length));

        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendResponse(HttpExchange exchange, Map<String, Object> response) throws IOException {
        int code = (Integer) response.get("code");
        int httpCode = code == 200 ? 200 : (code >= 400 && code < 600 ? code : 200);
        sendJson(exchange, httpCode, response);
    }

    // ============ 仪表盘 ============

    private static void handleDashboardStats(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, JsonUtil.fail(405, "Method not allowed"));
            return;
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        List<Map<String, Object>> devices = storage.loadDevices();
        List<Map<String, Object>> tasks = storage.loadTasks();

        int online = 0, offline = 0, unknown = 0, success = 0, failed = 0;
        for (Map<String, Object> d : devices) {
            String status = (String) d.get("status");
            if ("在线".equals(status)) online++;
            else if ("离线".equals(status)) offline++;
            else unknown++;
        }
        for (Map<String, Object> t : tasks) {
            String status = (String) t.get("status");
            if ("成功".equals(status)) success++;
            else if ("失败".equals(status)) failed++;
        }

        Map<String, Integer> brandCount = new LinkedHashMap<>();
        for (Map<String, Object> d : devices) {
            String brand = (String) d.get("brand");
            if (brand == null) brand = "未知";
            brandCount.put(brand, brandCount.getOrDefault(brand, 0) + 1);
        }

        stats.put("totalDevices", devices.size());
        stats.put("onlineDevices", online);
        stats.put("offlineDevices", offline);
        stats.put("unknownDevices", unknown);
        stats.put("totalTasks", tasks.size());
        stats.put("successTasks", success);
        stats.put("failedTasks", failed);
        stats.put("runningTasks", 0);
        stats.put("brandDistribution", brandCount);

        sendResponse(exchange, JsonUtil.success(stats));
    }

    // ============ 设备管理 ============

    @SuppressWarnings("unchecked")
    private static void handleDevices(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // 设备连接测试: /api/devices/{id}/test-connection
        if (path.contains("/test-connection")) {
            int id = extractIdFromPath(path);
            List<Map<String, Object>> devices = storage.loadDevices();
            for (Map<String, Object> d : devices) {
                Object did = d.get("id");
                if (did instanceof Number && ((Number) did).intValue() == id) {
                    boolean connected = testConnection(d);
                    d.put("status", connected ? "在线" : "离线");
                    storage.saveDevices(devices);
                    sendResponse(exchange, JsonUtil.success(connected ? "连接成功" : "连接失败"));
                    return;
                }
            }
            sendResponse(exchange, JsonUtil.fail(404, "设备不存在"));
            return;
        }

        // 获取单个设备
        if (path.split("/").length > 3 && "GET".equals(method)) {
            int id = extractIdFromPath(path);
            if (id > 0) {
                List<Map<String, Object>> devices = storage.loadDevices();
                for (Map<String, Object> d : devices) {
                    Object did = d.get("id");
                    if (did instanceof Number && ((Number) did).intValue() == id) {
                        Map<String, Object> device = new LinkedHashMap<>(d);
                        device.put("password", ""); // 不返回密码
                        sendResponse(exchange, JsonUtil.success(device));
                        return;
                    }
                }
                sendResponse(exchange, JsonUtil.fail(404, "设备不存在"));
                return;
            }
        }

        switch (method) {
            case "GET": {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String brandFilter = params.get("brand");
                String statusFilter = params.get("status");
                String keyword = params.get("keyword");

                List<Map<String, Object>> devices = storage.loadDevices();
                List<Map<String, Object>> result = new ArrayList<>();

                for (Map<String, Object> d : devices) {
                    if (brandFilter != null && !brandFilter.isEmpty()
                            && !brandFilter.equals(d.get("brand"))) continue;
                    if (statusFilter != null && !statusFilter.isEmpty()
                            && !statusFilter.equals(d.get("status"))) continue;
                    if (keyword != null && !keyword.isEmpty()) {
                        String ip = (String) d.get("ipAddress");
                        String model = (String) d.get("model");
                        boolean match = (ip != null && ip.toLowerCase().contains(keyword.toLowerCase()))
                                || (model != null && model.toLowerCase().contains(keyword.toLowerCase()));
                        if (!match) continue;
                    }
                    Map<String, Object> device = new LinkedHashMap<>(d);
                    if (device.containsKey("password")) device.put("password", "");
                    result.add(device);
                }
                sendResponse(exchange, JsonUtil.success(result));
                break;
            }
            case "POST": {
                String body = readBody(exchange);
                Map<String, Object> device = JsonUtil.parseMap(body);
                int id = storage.nextDeviceId();
                device.put("id", id);
                device.put("createTime", sdf.format(new Date()));
                if (device.get("status") == null) device.put("status", "未知");
                if (device.get("port") == null) device.put("port", 22);
                List<Map<String, Object>> devices = storage.loadDevices();
                devices.add(0, device);
                storage.saveDevices(devices);
                sendResponse(exchange, JsonUtil.success(id));
                break;
            }
            case "PUT": {
                int id = extractIdFromPath(path);
                String body = readBody(exchange);
                Map<String, Object> update = JsonUtil.parseMap(body);
                List<Map<String, Object>> devices = storage.loadDevices();
                for (int i = 0; i < devices.size(); i++) {
                    Map<String, Object> d = devices.get(i);
                    Object did = d.get("id");
                    if (did instanceof Number && ((Number) did).intValue() == id) {
                        // 保留原密码如果请求中密码为空
                        String newPwd = (String) update.get("password");
                        if (newPwd == null || newPwd.isEmpty()) {
                            update.put("password", d.get("password"));
                        }
                        update.put("id", id);
                        devices.set(i, update);
                        storage.saveDevices(devices);
                        sendResponse(exchange, JsonUtil.success(null));
                        return;
                    }
                }
                sendResponse(exchange, JsonUtil.fail(404, "设备不存在"));
                break;
            }
            case "DELETE": {
                int id = extractIdFromPath(path);
                if (id <= 0) {
                    // 批量删除
                    String body = readBody(exchange);
                    Map<String, Object> req = JsonUtil.parseMap(body);
                    Object idsObj = req.get("deviceIds");
                    if (idsObj instanceof List) {
                        List<Object> ids = (List<Object>) idsObj;
                        List<Map<String, Object>> devices = storage.loadDevices();
                        int count = 0;
                        for (Object did : ids) {
                            int targetId = ((Number) did).intValue();
                            for (int i = devices.size() - 1; i >= 0; i--) {
                                Object devId = devices.get(i).get("id");
                                if (devId instanceof Number && ((Number) devId).intValue() == targetId) {
                                    devices.remove(i);
                                    count++;
                                    break;
                                }
                            }
                        }
                        storage.saveDevices(devices);
                        sendResponse(exchange, JsonUtil.success("成功删除 " + count + " 台设备"));
                    } else {
                        sendResponse(exchange, JsonUtil.fail("无效参数"));
                    }
                    return;
                }
                List<Map<String, Object>> devices = storage.loadDevices();
                for (int i = devices.size() - 1; i >= 0; i--) {
                    Object did = devices.get(i).get("id");
                    if (did instanceof Number && ((Number) did).intValue() == id) {
                        devices.remove(i);
                        storage.saveDevices(devices);
                        sendResponse(exchange, JsonUtil.success(null));
                        return;
                    }
                }
                sendResponse(exchange, JsonUtil.fail(404, "设备不存在"));
                break;
            }
            default:
                sendResponse(exchange, JsonUtil.fail(405, "Method not allowed"));
        }
    }

    private static int extractIdFromPath(String path) {
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            try {
                return Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {}
        }
        return -1;
    }

    // ============ 品牌管理 ============

    private static void handleBrands(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, JsonUtil.fail(405, "Method not allowed"));
            return;
        }
        sendResponse(exchange, JsonUtil.success(storage.getBrands()));
    }

    @SuppressWarnings("unchecked")
    private static void handleBrandCommands(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        switch (method) {
            case "GET": {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                List<Map<String, Object>> allCommands = storage.loadCommands();
                List<Map<String, Object>> result = new ArrayList<>();

                String brandIdStr = params.get("brandId");
                Integer brandId = null;
                if (brandIdStr != null && !brandIdStr.isEmpty()) {
                    try { brandId = Integer.parseInt(brandIdStr); } catch (NumberFormatException e) {}
                }

                for (Map<String, Object> cmd : allCommands) {
                    if (brandId != null) {
                        Object bid = cmd.get("brandId");
                        if (bid instanceof Number && ((Number) bid).intValue() == brandId) {
                            result.add(cmd);
                        }
                    } else {
                        result.add(cmd);
                    }
                }
                sendResponse(exchange, JsonUtil.success(result));
                break;
            }
            case "POST": {
                String body = readBody(exchange);
                Map<String, Object> req = JsonUtil.parseMap(body);
                Object commandsObj = req.get("commands");
                if (commandsObj instanceof List) {
                    List<Object> newCmds = (List<Object>) commandsObj;
                    List<Map<String, Object>> allCmds = storage.loadCommands();
                    // 更新匹配 ID 的命令
                    int updated = 0;
                    for (Object cmdObj : newCmds) {
                        if (cmdObj instanceof Map) {
                            Map<String, Object> cmd = (Map<String, Object>) cmdObj;
                            Object id = cmd.get("id");
                            if (id instanceof Number) {
                                int targetId = ((Number) id).intValue();
                                for (int i = 0; i < allCmds.size(); i++) {
                                    Object currId = allCmds.get(i).get("id");
                                    if (currId instanceof Number && ((Number) currId).intValue() == targetId) {
                                        allCmds.set(i, cmd);
                                        updated++;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    storage.saveCommands(allCmds);
                    sendResponse(exchange, JsonUtil.success("成功更新 " + updated + " 条命令"));
                } else {
                    sendResponse(exchange, JsonUtil.fail("无效参数"));
                }
                break;
            }
            default:
                sendResponse(exchange, JsonUtil.fail(405, "Method not allowed"));
        }
    }

    // ============ 备份执行 ============

    @SuppressWarnings("unchecked")
    private static void handleBackup(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, JsonUtil.fail(405, "Method not allowed"));
            return;
        }

        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseMap(body);
        Object idsObj = req.get("deviceIds");

        if (!(idsObj instanceof List)) {
            sendResponse(exchange, JsonUtil.fail("请选择设备"));
            return;
        }

        List<Object> ids = (List<Object>) idsObj;
        List<Map<String, Object>> allDevices = storage.loadDevices();
        List<Map<String, Object>> tasks = new ArrayList<>();

        for (Object idObj : ids) {
            int deviceId = ((Number) idObj).intValue();
            Map<String, Object> device = null;
            for (Map<String, Object> d : allDevices) {
                Object did = d.get("id");
                if (did instanceof Number && ((Number) did).intValue() == deviceId) {
                    device = d;
                    break;
                }
            }
            if (device == null) continue;

            final Map<String, Object> dev = device;
            final int taskId = storage.nextTaskId();

            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", taskId);
            task.put("deviceId", deviceId);
            task.put("deviceIp", dev.get("ipAddress"));
            task.put("deviceBrand", dev.get("brand"));
            task.put("status", "执行中");
            task.put("startTime", sdf.format(new Date()));
            task.put("output", null);
            task.put("errorInfo", null);
            tasks.add(task);

            // 保存到存储
            List<Map<String, Object>> allTasks = storage.loadTasks();
            allTasks.add(0, task);
            storage.saveTasks(allTasks);

            // 添加日志
            List<Map<String, Object>> logs = storage.loadLogs();
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("id", storage.nextLogId());
            log.put("taskId", taskId);
            log.put("level", "INFO");
            log.put("message", "开始备份任务，设备: " + dev.get("ipAddress"));
            log.put("createTime", sdf.format(new Date()));
            logs.add(log);
            storage.saveLogs(logs);

            // 异步执行
            taskExecutor.submit(() -> executeBackupTask(taskId, dev, task));
        }

        sendResponse(exchange, JsonUtil.success("已提交 " + tasks.size() + " 个备份任务到后台执行"));
    }

    private static void executeBackupTask(int taskId, Map<String, Object> device, Map<String, Object> task) {
        try {
            Thread.sleep(1000); // 模拟执行时间

            // 根据品牌获取命令
            String brand = (String) device.get("brand");
            List<Map<String, Object>> commands = storage.loadCommands();
            List<Map<String, Object>> brandCmds = new ArrayList<>();
            for (Map<String, Object> c : commands) {
                if (brand != null && brand.equals(c.get("brandName"))) {
                    brandCmds.add(c);
                }
            }

            // 执行命令
            StringBuilder output = new StringBuilder();
            for (Map<String, Object> c : brandCmds) {
                String type = (String) c.get("commandType");
                String cmd = (String) c.get("command");
                output.append("> ").append(cmd).append("\n");
                output.append("# [" + type + "] 命令在实际环境中会通过 SSH 执行并返回设备输出\n");
            }
            output.append("\n== 备份执行完成 ==\n");

            // 保存结果
            task.put("status", "成功");
            task.put("output", output.toString());
            task.put("endTime", sdf.format(new Date()));

            List<Map<String, Object>> tasks = storage.loadTasks();
            for (int i = 0; i < tasks.size(); i++) {
                Object id = tasks.get(i).get("id");
                if (id instanceof Number && ((Number) id).intValue() == taskId) {
                    tasks.set(i, task);
                    break;
                }
            }
            storage.saveTasks(tasks);

            // 记录日志
            List<Map<String, Object>> logs = storage.loadLogs();
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("id", storage.nextLogId());
            log.put("taskId", taskId);
            log.put("level", "SUCCESS");
            log.put("message", "备份成功");
            log.put("createTime", sdf.format(new Date()));
            logs.add(log);
            storage.saveLogs(logs);

            // 更新设备状态
            List<Map<String, Object>> devices = storage.loadDevices();
            Object deviceId = device.get("id");
            if (deviceId instanceof Number) {
                int did = ((Number) deviceId).intValue();
                for (int i = 0; i < devices.size(); i++) {
                    Object dId = devices.get(i).get("id");
                    if (dId instanceof Number && ((Number) dId).intValue() == did) {
                        devices.get(i).put("status", "在线");
                        devices.get(i).put("lastBackupTime", sdf.format(new Date()));
                        storage.saveDevices(devices);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            task.put("status", "失败");
            task.put("errorInfo", e.getMessage());
            task.put("endTime", sdf.format(new Date()));

            List<Map<String, Object>> tasks = storage.loadTasks();
            for (int i = 0; i < tasks.size(); i++) {
                Object id = tasks.get(i).get("id");
                if (id instanceof Number && ((Number) id).intValue() == taskId) {
                    tasks.set(i, task);
                    break;
                }
            }
            storage.saveTasks(tasks);
        }
    }

    private static boolean testConnection(Map<String, Object> device) {
        // 简化实现：尝试建立 TCP 连接测试设备可达性
        String ip = (String) device.get("ipAddress");
        int port = 22;
        try {
            Object p = device.get("port");
            if (p instanceof Number) port = ((Number) p).intValue();
        } catch (Exception e) {}

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 3000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ============ 任务日志 ============

    @SuppressWarnings("unchecked")
    private static void handleTasks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // 获取任务详情 /api/tasks/{id}
        if (path.split("/").length > 3 && "GET".equals(method)) {
            int id = extractIdFromPath(path);
            if (id > 0) {
                // 获取任务日志
                if (path.contains("/logs")) {
                    List<Map<String, Object>> logs = storage.loadLogs();
                    List<Map<String, Object>> taskLogs = new ArrayList<>();
                    for (Map<String, Object> l : logs) {
                        Object tid = l.get("taskId");
                        if (tid instanceof Number && ((Number) tid).intValue() == id) {
                            taskLogs.add(l);
                        }
                    }

                    Map<String, Object> task = null;
                    List<Map<String, Object>> tasks = storage.loadTasks();
                    for (Map<String, Object> t : tasks) {
                        Object tid = t.get("id");
                        if (tid instanceof Number && ((Number) tid).intValue() == id) {
                            task = t;
                            break;
                        }
                    }

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("task", task);
                    result.put("logs", taskLogs);
                    sendResponse(exchange, JsonUtil.success(result));
                    return;
                }

                // 获取任务输出
                if (path.contains("/output")) {
                    List<Map<String, Object>> tasks = storage.loadTasks();
                    for (Map<String, Object> t : tasks) {
                        Object tid = t.get("id");
                        if (tid instanceof Number && ((Number) tid).intValue() == id) {
                            Map<String, Object> out = new LinkedHashMap<>();
                            out.put("output", t.get("output"));
                            out.put("errorInfo", t.get("errorInfo"));
                            sendResponse(exchange, JsonUtil.success(out));
                            return;
                        }
                    }
                    sendResponse(exchange, JsonUtil.fail(404, "任务不存在"));
                    return;
                }

                // 获取单个任务
                List<Map<String, Object>> tasks = storage.loadTasks();
                for (Map<String, Object> t : tasks) {
                    Object tid = t.get("id");
                    if (tid instanceof Number && ((Number) tid).intValue() == id) {
                        sendResponse(exchange, JsonUtil.success(t));
                        return;
                    }
                }
                sendResponse(exchange, JsonUtil.fail(404, "任务不存在"));
                return;
            }
        }

        if ("GET".equals(method)) {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String brandFilter = params.get("brand");
            String statusFilter = params.get("status");

            List<Map<String, Object>> tasks = storage.loadTasks();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> t : tasks) {
                if (brandFilter != null && !brandFilter.isEmpty()
                        && !brandFilter.equals(t.get("deviceBrand"))) continue;
                if (statusFilter != null && !statusFilter.isEmpty()
                        && !statusFilter.equals(t.get("status"))) continue;
                result.add(t);
            }
            sendResponse(exchange, JsonUtil.success(result));
        } else {
            sendResponse(exchange, JsonUtil.fail(405, "Method not allowed"));
        }
    }

    // ============ 静态资源 ============

    private static void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path) || path.isEmpty()) {
            path = "/index.html";
        }

        // 尝试从多个位置读取
        byte[] content = null;
        String[] searchPaths = {
            "src/main/resources/static" + path,
            "static" + path,
            "." + path
        };

        for (String p : searchPaths) {
            try {
                if (Files.exists(Paths.get(p))) {
                    content = Files.readAllBytes(Paths.get(p));
                    break;
                }
            } catch (Exception e) {}
        }

        // 尝试从 classpath 读取
        if (content == null) {
            InputStream is = WebServer.class.getResourceAsStream("/static" + path);
            if (is != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int n;
                while ((n = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, n);
                }
                content = buffer.toByteArray();
                is.close();
            }
        }

        if (content == null) {
            // 生成一个默认的 HTML 页面（如果前端文件不存在）
            content = generateDefaultPage();
        }

        // 设置 Content-Type
        Headers headers = exchange.getResponseHeaders();
        if (path.endsWith(".html")) headers.set("Content-Type", "text/html; charset=utf-8");
        else if (path.endsWith(".css")) headers.set("Content-Type", "text/css; charset=utf-8");
        else if (path.endsWith(".js")) headers.set("Content-Type", "application/javascript; charset=utf-8");
        else if (path.endsWith(".json")) headers.set("Content-Type", "application/json; charset=utf-8");
        else if (path.endsWith(".svg")) headers.set("Content-Type", "image/svg+xml");
        else if (path.endsWith(".png")) headers.set("Content-Type", "image/png");
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) headers.set("Content-Type", "image/jpeg");
        else headers.set("Content-Type", "application/octet-stream");

        headers.set("Content-Length", String.valueOf(content.length));
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private static byte[] generateDefaultPage() {
        return ("<html><head><meta charset='utf-8'><title>网络设备配置备份系统</title>"
                + "<script src='https://unpkg.com/vue@3.4.15/dist/vue.global.js'></script>"
                + "<link rel='stylesheet' href='https://unpkg.com/element-plus@2.4.4/dist/index.css'>"
                + "<script src='https://unpkg.com/element-plus@2.4.4/dist/index.full.js'></script>"
                + "<style>body{margin:0;background:#f5f7fa;font-family:'Microsoft YaHei',sans-serif;}"
                + ".header{background:linear-gradient(135deg,#2c3e50 0%,#34495e 100%);color:#fff;padding:20px;"
                + "box-shadow:0 2px 8px rgba(0,0,0,0.15);font-size:20px;font-weight:600;}"
                + ".container{padding:20px;}</style></head><body>"
                + "<div id='app'><div class='header'>网络设备配置备份系统 (轻量级模式)</div>"
                + "<div class='container'>请确保 index.html 位于正确位置，或访问 Spring Boot 版本获取完整体验</div></div></body></html>").getBytes();
    }
}
