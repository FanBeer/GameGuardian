# 网络设备配置备份应用 - 规格说明

## 1. 项目概述

- **项目名称**: NetDeviceBackup
- **项目类型**: Python Flask Web应用
- **核心功能**: 支持多品牌型号网络设备的配置备份，通过SSH/Telnet远程连接设备，执行备份命令，记录执行结果
- **目标用户**: 个人网络管理员

## 2. 技术栈

- **后端**: Python 3 + Flask + SQLite
- **前端**: HTML5 + Bootstrap5 + JavaScript (原生)
- **SSH库**: Paramiko
- **数据库**: SQLite3

## 3. 功能列表

### 3.1 设备资产管理
- 添加设备（IP、端口、协议、用户名、密码、品牌型号）
- 编辑设备信息
- 删除设备
- 设备列表展示（支持按品牌筛选）

### 3.2 品牌命令模板管理
- 预设品牌：华为、华三、锐捷、思科、Juniper
- 每个品牌包含配置备份命令
- 查看/编辑各品牌的备份命令模板

### 3.3 备份任务执行
- 选择单个或多个设备执行备份
- 显示任务执行进度（待执行、执行中、已完成、失败）
- 实时显示命令回显结果
- 记录错误信息
- 备份结果存储到本地文件

### 3.4 历史记录查看
- 查看历史备份任务列表
- 查看具体任务的执行详情（回显结果、错误信息）

## 4. 数据模型

### 4.1 Device（设备表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| name | VARCHAR(100) | 设备名称 |
| ip | VARCHAR(50) | IP地址 |
| port | INTEGER | 端口号 |
| protocol | VARCHAR(10) | SSH/Telnet |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(200) | 密码(加密存储) |
| brand | VARCHAR(50) | 品牌 |
| model | VARCHAR(50) | 型号 |
| created_at | DATETIME | 创建时间 |

### 4.2 BrandCommand（品牌命令表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| brand | VARCHAR(50) | 品牌名称 |
| command | TEXT | 备份命令 |
| description | VARCHAR(200) | 命令描述 |

### 4.3 BackupTask（备份任务表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| device_id | INTEGER | 设备ID |
| status | VARCHAR(20) | pending/running/completed/failed |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| result | TEXT | 执行结果 |
| error | TEXT | 错误信息 |

## 5. 页面结构

### 5.1 设备管理页面 (/)
- 设备列表表格
- 添加设备按钮 → 模态框
- 编辑/删除设备操作
- 品牌筛选下拉框
- 执行备份按钮

### 5.2 品牌命令管理页面 (/brands)
- 品牌列表
- 点击品牌显示对应的命令模板
- 编辑命令模板

### 5.3 备份历史页面 (/history)
- 历史任务列表
- 状态筛选
- 查看详情按钮

### 5.4 备份详情页面 (/task/<id>)
- 任务详细信息
- 命令回显结果
- 错误信息显示

## 6. API接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/devices | 获取设备列表 |
| POST | /api/devices | 添加设备 |
| PUT | /api/devices/<id> | 更新设备 |
| DELETE | /api/devices/<id> | 删除设备 |
| GET | /api/brands | 获取品牌列表 |
| GET | /api/brands/<brand>/commands | 获取品牌命令 |
| PUT | /api/brands/<brand>/commands | 更新品牌命令 |
| POST | /api/backup | 执行备份任务 |
| GET | /api/tasks | 获取任务列表 |
| GET | /api/tasks/<id> | 获取任务详情 |

## 7. 安全说明

- 无需登录认证（个人使用场景）
- 密码使用base64简单编码存储（仅防旁观者）
- 不暴露在公网环境下使用
