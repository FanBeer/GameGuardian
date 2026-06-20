import sqlite3
import os
from datetime import datetime
import base64

DB_PATH = os.path.join(os.path.dirname(__file__), 'devices.db')

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_db()
    cursor = conn.cursor()

    # 设备表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS devices (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name VARCHAR(100) NOT NULL,
            ip VARCHAR(50) NOT NULL,
            port INTEGER DEFAULT 22,
            protocol VARCHAR(10) DEFAULT 'SSH',
            username VARCHAR(50) NOT NULL,
            password VARCHAR(200) NOT NULL,
            brand VARCHAR(50) NOT NULL,
            model VARCHAR(50),
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ''')

    # 品牌命令表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS brand_commands (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            brand VARCHAR(50) UNIQUE NOT NULL,
            backup_command TEXT NOT NULL,
            description VARCHAR(200)
        )
    ''')

    # 备份任务表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS backup_tasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id INTEGER NOT NULL,
            device_name VARCHAR(100),
            device_ip VARCHAR(50),
            brand VARCHAR(50),
            status VARCHAR(20) DEFAULT 'pending',
            start_time DATETIME,
            end_time DATETIME,
            result TEXT,
            error TEXT,
            FOREIGN KEY (device_id) REFERENCES devices(id)
        )
    ''')

    # 初始化预设品牌命令
    default_brands = [
        ('华为', 'display current-configuration\nsave\n', '华为设备配置备份命令'),
        ('华三', 'display current-configuration\nsave\n', '华三设备配置备份命令'),
        ('锐捷', 'show running-config\ncopy running-config startup-config\n', '锐捷设备配置备份命令'),
        ('思科', 'show running-config\ncopy running-config startup-config\n', '思科设备配置备份命令'),
        ('Juniper', 'show configuration\ncommit\n', 'Juniper设备配置备份命令'),
    ]

    for brand, command, desc in default_brands:
        cursor.execute('''
            INSERT OR IGNORE INTO brand_commands (brand, backup_command, description)
            VALUES (?, ?, ?)
        ''', (brand, command, desc))

    conn.commit()
    conn.close()

# 设备操作
def get_all_devices():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM devices ORDER BY created_at DESC')
    devices = cursor.fetchall()
    conn.close()
    return [dict(row) for row in devices]

def get_device(id):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM devices WHERE id = ?', (id,))
    device = cursor.fetchone()
    conn.close()
    return dict(device) if device else None

def add_device(name, ip, port, protocol, username, password, brand, model):
    conn = get_db()
    cursor = conn.cursor()
    encoded_password = base64.b64encode(password.encode()).decode()
    cursor.execute('''
        INSERT INTO devices (name, ip, port, protocol, username, password, brand, model)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    ''', (name, ip, port, protocol, username, encoded_password, brand, model))
    device_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return device_id

def update_device(id, name, ip, port, protocol, username, password, brand, model):
    conn = get_db()
    cursor = conn.cursor()
    encoded_password = base64.b64encode(password.encode()).decode()
    cursor.execute('''
        UPDATE devices SET name=?, ip=?, port=?, protocol=?, username=?, password=?, brand=?, model=?
        WHERE id=?
    ''', (name, ip, port, protocol, username, encoded_password, brand, model, id))
    conn.commit()
    conn.close()

def delete_device(id):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('DELETE FROM devices WHERE id=?', (id,))
    conn.commit()
    conn.close()

def decode_password(encoded):
    return base64.b64decode(encoded.encode()).decode()

# 品牌命令操作
def get_all_brands():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM brand_commands')
    brands = cursor.fetchall()
    conn.close()
    return [dict(row) for row in brands]

def get_brand_command(brand):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM brand_commands WHERE brand=?', (brand,))
    brand_cmd = cursor.fetchone()
    conn.close()
    return dict(brand_cmd) if brand_cmd else None

def update_brand_command(brand, command):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('''
        UPDATE brand_commands SET backup_command=? WHERE brand=?
    ''', (command, brand))
    conn.commit()
    conn.close()

# 备份任务操作
def create_task(device_id, device_name, device_ip, brand):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('''
        INSERT INTO backup_tasks (device_id, device_name, device_ip, brand, status, start_time)
        VALUES (?, ?, ?, ?, 'pending', ?)
    ''', (device_id, device_name, device_ip, brand, datetime.now()))
    task_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return task_id

def update_task_status(task_id, status, result=None, error=None):
    conn = get_db()
    cursor = conn.cursor()
    if status in ('completed', 'failed'):
        cursor.execute('''
            UPDATE backup_tasks SET status=?, end_time=?, result=?, error=? WHERE id=?
        ''', (status, datetime.now(), result, error, task_id))
    else:
        cursor.execute('''
            UPDATE backup_tasks SET status=? WHERE id=?
        ''', (status, task_id))
    conn.commit()
    conn.close()

def get_all_tasks():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM backup_tasks ORDER BY start_time DESC')
    tasks = cursor.fetchall()
    conn.close()
    return [dict(row) for row in tasks]

def get_task(task_id):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM backup_tasks WHERE id=?', (task_id,))
    task = cursor.fetchone()
    conn.close()
    return dict(task) if task else None
