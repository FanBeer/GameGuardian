package com.nbbackup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NetworkDeviceBackupApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetworkDeviceBackupApplication.class, args);
        System.out.println("\n" +
                "=======================================\n" +
                "  网络设备配置备份系统 启动成功\n" +
                "  访问地址: http://localhost:8080/\n" +
                "=======================================\n");
    }
}
