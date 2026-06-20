# 网络设备配置备份系统 - 规格文档 (Web版)

## 1. 项目概述

**项目名称**: NetworkDeviceBackup (网络设备配置备份系统)
**技术栈**: Spring Boot 2.7 + Vue 3 + Element Plus + SQLite + JSch (SSH)
**核心功能**: 支持多品牌网络设备的 SSH 远程配置备份管理
**目标用户**: 网络运维工程师、系统管理员
**部署方式**: 单 jar 包部署，无需额外的前端构建工具和 Node.js

---

## 2. UI/UX 规格

### 2.1 布局结构

- **顶部标题栏**: 显示系统名称和管理控制台标识
- **左侧导航栏**: 220px 宽，包含 4 个菜单
- **右侧内容区**: 根据导航选择显示对应页面

### 2.2 组件设计

**主色调**:
- 主色: #2c3e50 (深蓝灰)
- 次色: #3498db (亮蓝)
- 成功: #27ae60 (绿色)
- 错误: #e74c3c (红色)
- 警告: #f39c12 (橙色)
- 背景: #f5f7fa (浅灰)

**状态标签**:
- 在线设备: 绿色标签 (success)
- 离线设备: 红色标签 (danger)
- 未知状态: 黄色标签 (warning)
- 成功任务: 绿色
- 失败任务: 红色
- 执行中任务: 蓝色

### 2.3 页面结构

#### 2.3.1 仪表盘 (Dashboard)
- 4 个统计卡片：设备总数、在线设备、离线设备、任务总数
- 各品牌设备分布图
- 任务执行情况（成功/失败/执行中）
- 快捷操作按钮

#### 2.3.2 设备资产列表页
- 顶部工具栏：添加/批量备份/批量删除/刷新
- 搜索框：按 IP 或型号搜索
- 设备表格：复选框、ID、品牌、型号、IP、端口、用户名、状态标签、最后备份时间、操作列
- 操作列：编辑/单台备份/删除

#### 2.3.3 添加/编辑设备对话框
- 品牌下拉选择
- 型号输入
- IP 地址输入
- SSH 端口号
- 用户名
- 密码（编辑时留空保持原密码）
- 状态选择（编辑时可用）
- 备注

#### 2.3.4 品牌命令配置页
- 左侧：品牌列表卡片（点击切换）
- 右侧：选中品牌的命令模板表格
- 每个命令类型：测试连接、备份配置、保存配置
- 命令内容可直接在表格中编辑
- 保存修改按钮

#### 2.3.5 任务日志页
- 任务列表表格：任务ID、设备IP、品牌、状态、开始时间、结束时间、耗时、操作
- 点击任务行或点击"查看日志"按钮，弹出任务详情对话框
- 任务详情对话框包含三个 Tab：
  - 命令输出（所有命令执行后的回显）
  - 错误信息（任务失败时的错误描述）
  - 日志记录（INFO/SUCCESS/ERROR 级别的日志时间线）

---

## 3. 功能规格

### 3.1 设备资产管理

**操作功能**:
- 添加设备（表单对话框）
- 编辑设备
- 删除设备（确认对话框）
- 批量选择和批量备份
- 批量删除
- 搜索过滤（按 IP/型号）
- 状态自动更新（备份成功后更新为"在线"，失败后更新为"离线"）

### 3.2 品牌命令模板

**支持的品牌**: 华为、H3C、思科、锐捷、中兴、迪普

**每品牌的命令类型**:
- 测试连接 (display version / show version 等)
- 备份配置 (display current-configuration / show running-config)
- 保存配置 (save / write / copy running-config startup-config)

**功能**:
- 可自定义每个品牌的命令内容
- 在线编辑并保存
- 不限制命令条数

### 3.3 任务执行与日志

**任务状态**: 等待中 / 执行中 / 成功 / 失败

**执行方式**:
- 单台设备备份
- 多台设备并发备份（每台设备独立线程执行）
- 任务在后台执行，不阻塞前端操作

**日志功能**:
- 实时记录命令执行过程
- 保存完整的 SSH 命令输出
- 按任务、按品牌、按状态过滤
- 支持 Tab 切换查看不同类型的日志

### 3.4 数据存储

**SQLite数据库**:
- `devices` 表：设备资产
- `brands` 表：品牌配置
- `commands` 表：命令模板
- `tasks` 表：任务记录（包含完整输出和错误信息）
- `logs` 表：日志记录（INFO/SUCCESS/ERROR 级别）

**密码安全**:
- 使用 SHA-256 + 盐值 加密存储设备密码
- 前端不回显密码明文
- 可在编辑设备时更新密码（留空保持原密码）

---

## 4. 技术架构

### 4.1 后端架构

```
src/main/java/com/nbbackup/
├── NetworkDeviceBackupApplication.java  # Spring Boot 启动类
├── Main.java                            # 原桌面应用入口（兼容保留）
├── config/
│   └── CorsConfig.java                  # 跨域配置
├── common/
│   └── R.java                           # 统一响应格式
├── dto/
│   └── BackupRequest.java               # 备份请求 DTO
├── model/
│   ├── Device.java                      # 设备模型
│   ├── Brand.java                       # 品牌模型
│   ├── CommandTemplate.java             # 命令模板模型
│   ├── BackupTask.java                  # 备份任务模型
│   └── TaskLog.java                     # 任务日志模型
├── service/
│   └── SshService.java                  # SSH 连接与命令执行服务
├── dao/
│   └── DatabaseHelper.java              # 数据库操作单例
├── util/
│   └── PasswordUtil.java                # 密码加密工具
└── controller/
    ├── DashboardController.java         # 仪表盘统计 API
    ├── DeviceController.java            # 设备管理 API
    ├── BrandController.java             # 品牌命令 API
    ├── BackupController.java            # 备份任务执行 API
    └── TaskController.java              # 任务日志 API
```

### 4.2 REST API 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/stats` | 获取仪表盘统计数据 |
| GET | `/api/devices` | 获取设备列表（支持 brand/status/keyword 参数过滤） |
| GET | `/api/devices/{id}` | 获取单个设备详情 |
| POST | `/api/devices` | 新增设备 |
| PUT | `/api/devices/{id}` | 更新设备 |
| DELETE | `/api/devices/{id}` | 删除设备 |
| DELETE | `/api/devices/batch` | 批量删除设备 |
| GET | `/api/devices/{id}/test-connection` | 测试设备 SSH 连接 |
| GET | `/api/brands` | 获取所有品牌列表 |
| GET | `/api/brands/{brandId}/commands` | 获取指定品牌的命令模板 |
| PUT | `/api/brands/commands/{commandId}` | 更新单条命令 |
| POST | `/api/brands/commands/batch-update` | 批量更新命令 |
| POST | `/api/backup/execute` | 执行备份任务（提交设备ID列表） |
| GET | `/api/tasks` | 获取任务列表（支持 brand/status 过滤） |
| GET | `/api/tasks/{taskId}` | 获取单个任务详情 |
| GET | `/api/tasks/{taskId}/logs` | 获取任务日志记录 |
| GET | `/api/tasks/{taskId}/output` | 获取任务命令输出 |

### 4.3 前端架构

**技术选型**:
- Vue 3 (通过 CDN 引入，无需 npm 构建)
- Element Plus 2.4.4 (UI 组件库)
- Element Plus Icons (图标库)
- 原生 Fetch API (HTTP 请求)

**文件结构**:
```
src/main/resources/static/
└── index.html          # 单页面应用入口（包含全部前端逻辑）
```

**页面与路由**:
- 仪表盘 (dashboard)
- 设备资产列表 (devices)
- 品牌命令配置 (brands)
- 任务日志 (tasks)

---

## 5. 部署与使用说明

### 5.1 环境要求

- JDK 8 或更高版本
- Maven 3.6 或更高版本
- 网络可达性（能够连接到目标网络设备的 SSH 端口）

### 5.2 编译打包

```bash
cd /workspace
mvn clean package -DskipTests
```

### 5.3 启动应用

```bash
java -jar target/network-device-backup.jar
```

或者使用 Maven 直接运行：

```bash
mvn spring-boot:run
```

### 5.4 访问地址

启动成功后，在浏览器中访问：

```
http://localhost:8080/
```

### 5.5 首次使用

1. 打开网页后，点击左侧"设备资产列表"
2. 点击"添加设备"，填入设备信息（IP地址、端口、用户名、密码为必填）
3. 点击"品牌命令配置"，为不同品牌的设备检查/修改备份命令（默认已内置常用命令）
4. 回到设备列表，勾选要备份的设备，点击"备份选中"，或点击单台设备的"备份"按钮
5. 系统在后台并发执行备份任务，可在"任务日志"页面查看执行情况和命令输出详情

### 5.6 数据库文件

系统首次启动时会自动创建 SQLite 数据库文件：

```
./nbbackup.db
```

如需迁移或备份数据，直接复制此文件即可。如需重置，删除后重启应用即可重新初始化（会重新创建默认品牌和命令数据）。

---

## 6. 验收标准

- ✅ Web 管理控制台可正常访问（http://localhost:8080/）
- ✅ 可以添加、编辑、删除网络设备
- ✅ 设备列表支持搜索、过滤
- ✅ 支持 6 个品牌的命令模板自定义
- ✅ 可以执行单台或多台设备的配置备份
- ✅ 备份任务在后台并发执行
- ✅ 任务执行进度和结果可在任务日志页面查看
- ✅ 完整的命令输出、错误信息、日志记录三个 Tab 分别展示
- ✅ 数据持久化到 SQLite 数据库
- ✅ 密码加密存储，不在前端回显
- ✅ 仪表盘展示设备总数、在线/离线统计、任务执行情况
- ✅ 无需 Node.js 和 npm，编译后单 jar 包即可部署使用

---

## 7. 后续可扩展功能

- 定时自动备份任务（通过 Spring Scheduler 实现）
- 配置文件导出（下载命令输出为 .cfg 文件）
- 配置文件版本管理和差异对比
- 用户登录和权限管理
- 邮件/短信通知（备份成功/失败时发送通知）
- 设备分组管理（按机房/楼层/业务系统分组）
- 批量导入设备（支持 Excel/JSON 格式）
- 更多设备品牌和型号的默认命令模板
- 执行历史趋势图表分析
