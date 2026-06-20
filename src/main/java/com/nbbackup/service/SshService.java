package com.nbbackup.service;

import com.jcraft.jsch.*;
import com.nbbackup.model.CommandTemplate;
import com.nbbackup.model.Device;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SshService {
    private static final int TIMEOUT = 30000;
    private static final Pattern PROMPT_PATTERN = Pattern.compile("[\\[<][^\\]>]+[\\]>][\\s]*#");

    public interface CommandCallback {
        void onOutput(String line);
        void onComplete(String output, String error, boolean success);
    }

    public static class CommandResult {
        private String output;
        private String error;
        private boolean success;

        public CommandResult(String output, String error, boolean success) {
            this.output = output;
            this.error = error;
            this.success = success;
        }

        public String getOutput() { return output; }
        public String getError() { return error; }
        public boolean isSuccess() { return success; }
    }

    public static String[] detectDeviceBrand(Device device) {
        String brandKey = device.getBrand().toLowerCase();
        switch (brandKey) {
            case "华为":
                return new String[]{"huawei", "screen-length 0 temporary"};
            case "h3c":
                return new String[]{"h3c", "screen-length 0 temporary"};
            case "思科":
                return new String[]{"cisco", "terminal length 0"};
            case "锐捷":
                return new String[]{"ruijie", "terminal length 0"};
            case "中兴":
                return new String[]{"zte", "terminal length 0"};
            case "迪普":
                return new String[]{"dptech", "screen-length 0 temporary"};
            default:
                return new String[]{"generic", "terminal length 0"};
        }
    }

    public void executeCommands(Device device, List<CommandTemplate> commands, CommandCallback callback) {
        new Thread(() -> {
            Session session = null;
            try {
                JSch jsch = new JSch();
                session = jsch.getSession(device.getUsername(), device.getIpAddress(), device.getPort());
                session.setPassword(device.getPassword());
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(TIMEOUT);
                session.connect(TIMEOUT);

                ChannelShell channel = (ChannelShell) session.openChannel("shell");
                channel.setInputStream(null);
                channel.setOutputStream(null);

                BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                java.io.OutputStream os = channel.getOutputStream();

                channel.connect(TIMEOUT);

                StringBuilder output = new StringBuilder();
                String[] brandInfo = detectDeviceBrand(device);
                String screenCmd = brandInfo[1];

                os.write((screenCmd + "\r\n").getBytes());
                os.flush();
                Thread.sleep(500);
                drainReader(reader, output);

                for (CommandTemplate cmd : commands) {
                    String command = cmd.getCommand();
                    if (callback != null) {
                        callback.onOutput("> " + command);
                    }
                    output.append("> ").append(command).append("\n");

                    os.write((command + "\r\n").getBytes());
                    os.flush();
                    Thread.sleep(1000);

                    StringBuilder cmdOutput = new StringBuilder();
                    drainReader(reader, cmdOutput);
                    output.append(cmdOutput);

                    if (callback != null) {
                        callback.onOutput(cmdOutput.toString());
                    }
                }

                os.write("quit\r\n".getBytes());
                os.flush();
                Thread.sleep(500);

                channel.disconnect();
                session.disconnect();

                if (callback != null) {
                    callback.onComplete(output.toString(), null, true);
                }

            } catch (Exception e) {
                String errorMsg = "连接失败: " + e.getMessage();
                if (callback != null) {
                    callback.onComplete(null, errorMsg, false);
                }
            } finally {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }).start();
    }

    public CommandResult executeCommandsSync(Device device, List<CommandTemplate> commands) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(device.getUsername(), device.getIpAddress(), device.getPort());
            session.setPassword(device.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(TIMEOUT);
            session.connect(TIMEOUT);

            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.setInputStream(null);
            channel.setOutputStream(null);

            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            java.io.OutputStream os = channel.getOutputStream();

            channel.connect(TIMEOUT);

            StringBuilder output = new StringBuilder();
            String[] brandInfo = detectDeviceBrand(device);
            String screenCmd = brandInfo[1];

            os.write((screenCmd + "\r\n").getBytes());
            os.flush();
            Thread.sleep(500);
            drainReader(reader, output);

            for (CommandTemplate cmd : commands) {
                String command = cmd.getCommand();
                output.append("> ").append(command).append("\n");

                os.write((command + "\r\n").getBytes());
                os.flush();
                Thread.sleep(1000);

                StringBuilder cmdOutput = new StringBuilder();
                drainReader(reader, cmdOutput);
                output.append(cmdOutput);
            }

            os.write("quit\r\n".getBytes());
            os.flush();
            Thread.sleep(500);

            channel.disconnect();
            session.disconnect();

            return new CommandResult(output.toString(), null, true);

        } catch (Exception e) {
            return new CommandResult(null, "连接失败: " + e.getMessage(), false);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void drainReader(BufferedReader reader, StringBuilder output) throws Exception {
        while (reader.ready()) {
            String line = reader.readLine();
            if (line != null) {
                output.append(line).append("\n");
            }
        }
    }

    public boolean testConnection(Device device) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(device.getUsername(), device.getIpAddress(), device.getPort());
            session.setPassword(device.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(TIMEOUT);
            session.connect(TIMEOUT);

            boolean connected = session.isConnected();
            session.disconnect();
            return connected;
        } catch (Exception e) {
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
