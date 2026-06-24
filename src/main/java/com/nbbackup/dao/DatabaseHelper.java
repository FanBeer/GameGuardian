package com.nbbackup.dao;

import com.nbbackup.model.*;
import com.nbbackup.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:./nbbackup.db";
    private static DatabaseHelper instance;

    private DatabaseHelper() {
        initDatabase();
    }

    public static synchronized DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initDatabase() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            initDefaultBrands(conn);
            initDefaultCommands(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String createDevicesTable = "CREATE TABLE IF NOT EXISTS devices (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "brand TEXT NOT NULL, " +
                "model TEXT, " +
                "ip_address TEXT NOT NULL UNIQUE, " +
                "port INTEGER DEFAULT 22, " +
                "username TEXT, " +
                "password TEXT, " +
                "status TEXT DEFAULT '未知', " +
                "remark TEXT, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "last_backup_time TIMESTAMP)";

        String createBrandsTable = "CREATE TABLE IF NOT EXISTS brands (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, " +
                "display_name TEXT NOT NULL, " +
                "description TEXT)";

        String createCommandsTable = "CREATE TABLE IF NOT EXISTS commands (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "brand_id INTEGER NOT NULL, " +
                "command_type TEXT NOT NULL, " +
                "command TEXT NOT NULL, " +
                "description TEXT, " +
                "FOREIGN KEY (brand_id) REFERENCES brands(id))";

        String createTasksTable = "CREATE TABLE IF NOT EXISTS tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "device_id INTEGER NOT NULL, " +
                "device_ip TEXT NOT NULL, " +
                "device_brand TEXT, " +
                "status TEXT DEFAULT '等待中', " +
                "start_time TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "output TEXT, " +
                "error_info TEXT)";

        String createLogsTable = "CREATE TABLE IF NOT EXISTS logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id INTEGER NOT NULL, " +
                "level TEXT DEFAULT 'INFO', " +
                "message TEXT, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (task_id) REFERENCES tasks(id))";

        conn.createStatement().execute(createDevicesTable);
        conn.createStatement().execute(createBrandsTable);
        conn.createStatement().execute(createCommandsTable);
        conn.createStatement().execute(createTasksTable);
        conn.createStatement().execute(createLogsTable);
    }

    private void initDefaultBrands(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM brands";
        ResultSet rs = conn.createStatement().executeQuery(checkSql);
        if (rs.next() && rs.getInt(1) > 0) {
            return;
        }

        String[][] brands = {
                {"huawei", "华为", "华为网络设备"},
                {"h3c", "H3C", "华三网络设备"},
                {"cisco", "思科", "思科网络设备"},
                {"ruijie", "锐捷", "锐捷网络设备"},
                {"zte", "中兴", "中兴网络设备"},
                {"dptech", "迪普", "迪普网络设备"}
        };

        String insertSql = "INSERT INTO brands (name, display_name, description) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(insertSql);
        for (String[] brand : brands) {
            pstmt.setString(1, brand[0]);
            pstmt.setString(2, brand[1]);
            pstmt.setString(3, brand[2]);
            pstmt.executeUpdate();
        }
    }

    private void initDefaultCommands(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM commands";
        ResultSet rs = conn.createStatement().executeQuery(checkSql);
        if (rs.next() && rs.getInt(1) > 0) {
            return;
        }

        String[][][] commands = {
                {{"huawei", "1"}, {"测试连接", "display version", "测试设备连接"}, {"备份配置", "display current-configuration", "获取当前配置"}, {"保存配置", "save", "保存配置"}},
                {{"h3c", "2"}, {"测试连接", "display version", "测试设备连接"}, {"备份配置", "display current-configuration", "获取当前配置"}, {"保存配置", "save", "保存配置"}},
                {{"cisco", "3"}, {"测试连接", "show version", "测试设备连接"}, {"备份配置", "show running-config", "获取当前配置"}, {"保存配置", "copy running-config startup-config", "保存配置"}},
                {{"ruijie", "4"}, {"测试连接", "show version", "测试设备连接"}, {"备份配置", "show running-config", "获取当前配置"}, {"保存配置", "write", "保存配置"}},
                {{"zte", "5"}, {"测试连接", "show version", "测试设备连接"}, {"备份配置", "show running-config", "获取当前配置"}, {"保存配置", "write", "保存配置"}},
                {{"dptech", "6"}, {"测试连接", "display version", "测试设备连接"}, {"备份配置", "display current-configuration", "获取当前配置"}, {"保存配置", "save", "保存配置"}}
        };

        String insertSql = "INSERT INTO commands (brand_id, command_type, command, description) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(insertSql);
        for (String[][] brandCmds : commands) {
            int brandId = Integer.parseInt(brandCmds[0][1]);
            for (int i = 1; i < brandCmds.length; i++) {
                pstmt.setInt(1, brandId);
                pstmt.setString(2, brandCmds[i][0]);
                pstmt.setString(3, brandCmds[i][1]);
                pstmt.setString(4, brandCmds[i][2]);
                pstmt.executeUpdate();
            }
        }
    }

    // Device operations
    public List<Device> getAllDevices() {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT * FROM devices ORDER BY id DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                devices.add(extractDevice(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return devices;
    }

    public Device getDeviceById(int id) {
        String sql = "SELECT * FROM devices WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractDevice(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Device extractDevice(ResultSet rs) throws SQLException {
        Device device = new Device();
        device.setId(rs.getInt("id"));
        device.setBrand(rs.getString("brand"));
        device.setModel(rs.getString("model"));
        device.setIpAddress(rs.getString("ip_address"));
        device.setPort(rs.getInt("port"));
        device.setUsername(rs.getString("username"));
        device.setPassword(rs.getString("password"));
        device.setStatus(rs.getString("status"));
        device.setRemark(rs.getString("remark"));
        device.setCreateTime(rs.getTimestamp("create_time"));
        device.setLastBackupTime(rs.getTimestamp("last_backup_time"));
        return device;
    }

    public int addDevice(Device device) {
        String sql = "INSERT INTO devices (brand, model, ip_address, port, username, password, status, remark, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, device.getBrand());
            pstmt.setString(2, device.getModel());
            pstmt.setString(3, device.getIpAddress());
            pstmt.setInt(4, device.getPort());
            pstmt.setString(5, device.getUsername());
            pstmt.setString(6, PasswordUtil.encrypt(device.getPassword()));
            pstmt.setString(7, device.getStatus());
            pstmt.setString(8, device.getRemark());
            pstmt.setTimestamp(9, new Timestamp(device.getCreateTime().getTime()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateDevice(Device device) {
        String sql = "UPDATE devices SET brand=?, model=?, ip_address=?, port=?, username=?, password=?, status=?, remark=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, device.getBrand());
            pstmt.setString(2, device.getModel());
            pstmt.setString(3, device.getIpAddress());
            pstmt.setInt(4, device.getPort());
            pstmt.setString(5, device.getUsername());
            pstmt.setString(6, PasswordUtil.encrypt(device.getPassword()));
            pstmt.setString(7, device.getStatus());
            pstmt.setString(8, device.getRemark());
            pstmt.setInt(9, device.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteDevice(int id) {
        String sql = "DELETE FROM devices WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateDeviceStatus(int id, String status) {
        String sql = "UPDATE devices SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateDeviceLastBackupTime(int id) {
        String sql = "UPDATE devices SET last_backup_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Brand operations
    public List<Brand> getAllBrands() {
        List<Brand> brands = new ArrayList<>();
        String sql = "SELECT * FROM brands ORDER BY id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Brand brand = new Brand();
                brand.setId(rs.getInt("id"));
                brand.setName(rs.getString("name"));
                brand.setDisplayName(rs.getString("display_name"));
                brand.setDescription(rs.getString("description"));
                brands.add(brand);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return brands;
    }

    // Command operations
    public List<CommandTemplate> getCommandsByBrandId(int brandId) {
        List<CommandTemplate> commands = new ArrayList<>();
        String sql = "SELECT c.*, b.display_name as brand_name FROM commands c " +
                "JOIN brands b ON c.brand_id = b.id WHERE c.brand_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, brandId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                CommandTemplate cmd = new CommandTemplate();
                cmd.setId(rs.getInt("id"));
                cmd.setBrandId(rs.getInt("brand_id"));
                cmd.setBrandName(rs.getString("brand_name"));
                cmd.setCommandType(rs.getString("command_type"));
                cmd.setCommand(rs.getString("command"));
                cmd.setDescription(rs.getString("description"));
                commands.add(cmd);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commands;
    }

    public List<CommandTemplate> getCommandsByBrandName(String brandName) {
        List<CommandTemplate> commands = new ArrayList<>();
        String sql = "SELECT c.*, b.display_name as brand_name FROM commands c " +
                "JOIN brands b ON c.brand_id = b.id WHERE b.name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, brandName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                CommandTemplate cmd = new CommandTemplate();
                cmd.setId(rs.getInt("id"));
                cmd.setBrandId(rs.getInt("brand_id"));
                cmd.setBrandName(rs.getString("brand_name"));
                cmd.setCommandType(rs.getString("command_type"));
                cmd.setCommand(rs.getString("command"));
                cmd.setDescription(rs.getString("description"));
                commands.add(cmd);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commands;
    }

    public boolean updateCommand(int id, String command) {
        String sql = "UPDATE commands SET command = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, command);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Task operations
    public int addTask(BackupTask task) {
        String sql = "INSERT INTO tasks (device_id, device_ip, device_brand, status, start_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, task.getDeviceId());
            pstmt.setString(2, task.getDeviceIp());
            pstmt.setString(3, task.getDeviceBrand());
            pstmt.setString(4, task.getStatus());
            pstmt.setTimestamp(5, new Timestamp(task.getStartTime().getTime()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateTask(BackupTask task) {
        String sql = "UPDATE tasks SET status=?, end_time=?, output=?, error_info=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, task.getStatus());
            pstmt.setTimestamp(2, task.getEndTime() != null ? new Timestamp(task.getEndTime().getTime()) : null);
            pstmt.setString(3, task.getOutput());
            pstmt.setString(4, task.getErrorInfo());
            pstmt.setInt(5, task.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<BackupTask> getAllTasks() {
        List<BackupTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY start_time DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(extractTask(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public List<BackupTask> getTasksByDeviceId(int deviceId) {
        List<BackupTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE device_id = ? ORDER BY start_time DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deviceId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tasks.add(extractTask(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    private BackupTask extractTask(ResultSet rs) throws SQLException {
        BackupTask task = new BackupTask();
        task.setId(rs.getInt("id"));
        task.setDeviceId(rs.getInt("device_id"));
        task.setDeviceIp(rs.getString("device_ip"));
        task.setDeviceBrand(rs.getString("device_brand"));
        task.setStatus(rs.getString("status"));
        task.setStartTime(rs.getTimestamp("start_time"));
        task.setEndTime(rs.getTimestamp("end_time"));
        task.setOutput(rs.getString("output"));
        task.setErrorInfo(rs.getString("error_info"));
        return task;
    }

    // Log operations
    public int addLog(TaskLog log) {
        String sql = "INSERT INTO logs (task_id, level, message, create_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, log.getTaskId());
            pstmt.setString(2, log.getLevel());
            pstmt.setString(3, log.getMessage());
            pstmt.setTimestamp(4, new Timestamp(log.getCreateTime().getTime()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<TaskLog> getLogsByTaskId(int taskId) {
        List<TaskLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM logs WHERE task_id = ? ORDER BY create_time";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                TaskLog log = new TaskLog();
                log.setId(rs.getInt("id"));
                log.setTaskId(rs.getInt("task_id"));
                log.setLevel(rs.getString("level"));
                log.setMessage(rs.getString("message"));
                log.setCreateTime(rs.getTimestamp("create_time"));
                logs.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public List<TaskLog> getAllLogs() {
        List<TaskLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM logs ORDER BY create_time DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TaskLog log = new TaskLog();
                log.setId(rs.getInt("id"));
                log.setTaskId(rs.getInt("task_id"));
                log.setLevel(rs.getString("level"));
                log.setMessage(rs.getString("message"));
                log.setCreateTime(rs.getTimestamp("create_time"));
                logs.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
}
