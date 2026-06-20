from flask import Flask, render_template, request, jsonify
import os
import threading
from datetime import datetime

app = Flask(__name__)
app.config['BACKUP_DIR'] = os.path.join(os.path.dirname(__file__), 'backup_results')

import models

# 确保备份目录存在
os.makedirs(app.config['BACKUP_DIR'], exist_ok=True)

# 初始化数据库
models.init_db()

@app.route('/')
def index():
    brands = models.get_all_brands()
    return render_template('index.html', brands=brands)

@app.route('/brands')
def brands_page():
    brands = models.get_all_brands()
    return render_template('brands.html', brands=brands)

@app.route('/history')
def history():
    return render_template('history.html')

@app.route('/task/<int:task_id>')
def task_detail(task_id):
    task = models.get_task(task_id)
    if not task:
        return "Task not found", 404
    return render_template('task_detail.html', task=task)

# API接口
@app.route('/api/devices', methods=['GET'])
def api_get_devices():
    devices = models.get_all_devices()
    for d in devices:
        d['password'] = '******'
    return jsonify(devices)

@app.route('/api/devices', methods=['POST'])
def api_add_device():
    data = request.json
    device_id = models.add_device(
        data['name'], data['ip'], data['port'],
        data['protocol'], data['username'], data['password'],
        data['brand'], data.get('model', '')
    )
    return jsonify({'id': device_id, 'message': 'Device added successfully'})

@app.route('/api/devices/<int:id>', methods=['PUT'])
def api_update_device(id):
    data = request.json
    models.update_device(
        id, data['name'], data['ip'], data['port'],
        data['protocol'], data['username'], data['password'],
        data['brand'], data.get('model', '')
    )
    return jsonify({'message': 'Device updated successfully'})

@app.route('/api/devices/<int:id>', methods=['DELETE'])
def api_delete_device(id):
    models.delete_device(id)
    return jsonify({'message': 'Device deleted successfully'})

@app.route('/api/brands', methods=['GET'])
def api_get_brands():
    brands = models.get_all_brands()
    return jsonify(brands)

@app.route('/api/brands/<brand>/commands', methods=['GET'])
def api_get_brand_commands(brand):
    brand_cmd = models.get_brand_command(brand)
    if brand_cmd:
        return jsonify(brand_cmd)
    return jsonify({'error': 'Brand not found'}), 404

@app.route('/api/brands/<brand>/commands', methods=['PUT'])
def api_update_brand_commands(brand):
    data = request.json
    models.update_brand_command(brand, data['command'])
    return jsonify({'message': 'Command updated successfully'})

@app.route('/api/tasks', methods=['GET'])
def api_get_tasks():
    tasks = models.get_all_tasks()
    return jsonify(tasks)

@app.route('/api/tasks/<int:task_id>', methods=['GET'])
def api_get_task_detail(task_id):
    task = models.get_task(task_id)
    if task:
        return jsonify(task)
    return jsonify({'error': 'Task not found'}), 404

@app.route('/api/backup', methods=['POST'])
def api_backup():
    data = request.json
    device_ids = data.get('device_ids', [])

    if not device_ids:
        return jsonify({'error': 'No devices selected'}), 400

    task_ids = []
    for device_id in device_ids:
        device = models.get_device(device_id)
        if device:
            task_id = models.create_task(
                device_id, device['name'], device['ip'], device['brand']
            )
            task_ids.append(task_id)
            # 启动后台任务执行
            thread = threading.Thread(target=execute_backup, args=(task_id, device))
            thread.daemon = True
            thread.start()

    return jsonify({'task_ids': task_ids, 'message': 'Backup tasks created'})

def execute_backup(task_id, device):
    """执行备份任务"""
    try:
        models.update_task_status(task_id, 'running')

        # 根据协议选择连接方式
        if device['protocol'] == 'SSH':
            result = ssh_backup(device)
        else:
            result = telnet_backup(device)

        # 保存备份结果到文件
        filename = f"{device['brand']}_{device['ip']}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.cfg"
        filepath = os.path.join(app.config['BACKUP_DIR'], filename)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(result)

        models.update_task_status(task_id, 'completed', result=result)

    except Exception as e:
        models.update_task_status(task_id, 'failed', error=str(e))

def ssh_backup(device):
    """通过SSH执行备份"""
    import paramiko

    password = models.decode_password(device['password'])
    brand_cmd = models.get_brand_command(device['brand'])

    if not brand_cmd:
        raise Exception(f"未找到品牌 {device['brand']} 的备份命令")

    commands = brand_cmd['backup_command'].strip().split('\n')

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        ssh.connect(
            device['ip'],
            port=device['port'],
            username=device['username'],
            password=password,
            timeout=30,
            banner_timeout=30
        )

        results = []
        for cmd in commands:
            cmd = cmd.strip()
            if not cmd:
                continue
            stdin, stdout, stderr = ssh.exec_command(cmd, get_pty=True)
            output = stdout.read().decode('utf-8', errors='ignore')
            error = stderr.read().decode('utf-8', errors='ignore')
            results.append(f"[{cmd}]\n{output}")
            if error:
                results.append(f"[ERROR]\n{error}")

        ssh.close()
        return '\n'.join(results)

    except Exception as e:
        raise Exception(f"SSH连接失败: {str(e)}")

def telnet_backup(device):
    """通过Telnet执行备份"""
    import telnetlib

    password = models.decode_password(device['password'])
    brand_cmd = models.get_brand_command(device['brand'])

    if not brand_cmd:
        raise Exception(f"未找到品牌 {device['brand']} 的备份命令")

    commands = brand_cmd['backup_command'].strip().split('\n')

    try:
        tn = telnetlib.Telnet(device['ip'], port=device['port'], timeout=30)

        # 等待登录提示
        tn.read_until(b'login:', timeout=10)
        tn.write(device['username'].encode('utf-8') + b'\n')

        # 等待密码提示
        tn.read_until(b'Password:', timeout=10)
        tn.write(password.encode('utf-8') + b'\n')

        results = []
        for cmd in commands:
            cmd = cmd.strip()
            if not cmd:
                continue
            tn.write(cmd.encode('utf-8') + b'\n')
            output = tn.read_until(b'\n', timeout=10).decode('utf-8', errors='ignore')
            results.append(f"[{cmd}]\n{output}")

        tn.write(b'exit\n')
        tn.close()

        return '\n'.join(results)

    except Exception as e:
        raise Exception(f"Telnet连接失败: {str(e)}")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
