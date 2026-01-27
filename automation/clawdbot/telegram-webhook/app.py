#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Telegram Webhook Service
Flask-based Telegram 通知服務 + Workflow Command Handler

作者: SmartAdmin Auto-Coding System
版本: 2.0.0 (新增 /command 端點，支持工作流觸發)
日期: 2026-01-28
"""

import os
import logging
from flask import Flask, request, jsonify
from typing import Dict, Any
import requests

# ============================================================================
# 配置
# ============================================================================

app = Flask(__name__)
app.config['JSON_AS_ASCII'] = False

# Telegram Bot 配置
TELEGRAM_BOT_TOKEN = os.getenv('TELEGRAM_BOT_TOKEN')
TELEGRAM_CHAT_ID = os.getenv('TELEGRAM_CHAT_ID')
TELEGRAM_API_URL = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendMessage"

# 日誌配置
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ============================================================================
# 端點 1: 健康檢查
# ============================================================================

@app.route('/health', methods=['GET'])
def health_check():
    """
    健康檢查端點

    Returns:
        JSON: {"status": "healthy", "version": "1.0.0", "telegram_bot": "connected"}
    """
    try:
        # 測試 Telegram Bot 連接
        if TELEGRAM_BOT_TOKEN:
            test_url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/getMe"
            response = requests.get(test_url, timeout=5)

            if response.status_code == 200:
                return jsonify({
                    "status": "healthy",
                    "version": "2.0.0",
                    "telegram_bot": "connected",
                    "command_handler": "enabled" if command_handler else "disabled"
                }), 200
            else:
                return jsonify({
                    "status": "degraded",
                    "version": "2.0.0",
                    "telegram_bot": "disconnected",
                    "command_handler": "enabled" if command_handler else "disabled",
                    "error": f"HTTP {response.status_code}"
                }), 200
        else:
            return jsonify({
                "status": "degraded",
                "version": "2.0.0",
                "telegram_bot": "not_configured",
                "command_handler": "enabled" if command_handler else "disabled"
            }), 200

    except Exception as e:
        logger.error(f"Health check error: {e}")
        return jsonify({
            "status": "unhealthy",
            "version": "1.0.0",
            "error": str(e)
        }), 500

# ============================================================================
# 端點 2: Prometheus 告警
# ============================================================================

@app.route('/alert', methods=['POST'])
def handle_prometheus_alert():
    """
    處理 Prometheus 告警

    Request Body:
        {
            "status": "firing",
            "labels": {"alertname": "...", "severity": "..."},
            "annotations": {"summary": "...", "description": "..."}
        }
    """
    try:
        # 1. 獲取請求數據
        data = request.get_json()

        if not data:
            return jsonify({"error": "No data provided"}), 400

        # 2. 構建告警消息
        status = data.get("status", "unknown")
        labels = data.get("labels", {})
        annotations = data.get("annotations", {})

        # 狀態 Emoji
        status_emoji = {"firing": "🔥", "resolved": "✅"}
        emoji = status_emoji.get(status, "❓")

        message = f"""
{emoji} *Prometheus Alert*

*Status*: {status.upper()}
*Alert*: {labels.get("alertname", "N/A")}
*Severity*: {labels.get("severity", "N/A")}

*Summary*: {annotations.get("summary", "N/A")}
*Description*: {annotations.get("description", "N/A")}
"""

        # 3. 發送到 Telegram
        success = send_telegram_message(message.strip())

        if success:
            return jsonify({"status": "sent"}), 200
        else:
            return jsonify({"error": "Failed to send message"}), 500

    except Exception as e:
        logger.error(f"Error handling alert: {e}")
        return jsonify({"error": str(e)}), 500

# ============================================================================
# 端點 3: Argo Workflow 狀態
# ============================================================================

@app.route('/workflow', methods=['POST'])
def handle_workflow_status():
    """
    處理 Argo Workflow 狀態更新

    Request Body:
        {
            "workflow_name": "analyzer-workflow",
            "status": "Succeeded",
            "message": "Analysis completed",
            "duration": "5m30s",
            "node_count": 3
        }
    """
    try:
        # 1. 獲取請求數據
        data = request.get_json()

        if not data:
            return jsonify({"error": "No data provided"}), 400

        # 2. 格式化消息
        message = format_workflow_message(data)

        # 3. 發送到 Telegram
        success = send_telegram_message(message)

        if success:
            return jsonify({"status": "sent"}), 200
        else:
            return jsonify({"error": "Failed to send message"}), 500

    except Exception as e:
        logger.error(f"Error handling workflow status: {e}")
        return jsonify({"error": str(e)}), 500

# ============================================================================
# 端點 4: 人工審批請求
# ============================================================================

@app.route('/approval', methods=['POST'])
def handle_approval_request():
    """
    處理人工審批請求

    Request Body:
        {
            "type": "file_access",
            "file_path": "/path/to/file",
            "agent": "java-architect",
            "reason": "Need to modify foundation layer",
            "operation": "WRITE"
        }
    """
    try:
        # 1. 獲取請求數據
        data = request.get_json()

        if not data:
            return jsonify({"error": "No data provided"}), 400

        # 2. 生成唯一請求 ID
        import uuid
        data["request_id"] = str(uuid.uuid4())[:8]

        # 3. 格式化消息
        message = format_approval_message(data)

        # 4. 發送到 Telegram
        success = send_telegram_message(message)

        if success:
            return jsonify({
                "status": "sent",
                "request_id": data["request_id"]
            }), 200
        else:
            return jsonify({"error": "Failed to send message"}), 500

    except Exception as e:
        logger.error(f"Error handling approval request: {e}")
        return jsonify({"error": str(e)}), 500

# ============================================================================
# 端點 5: 成本告警
# ============================================================================

@app.route('/cost', methods=['POST'])
def handle_cost_alert():
    """
    處理成本告警

    Request Body:
        {
            "period": "monthly",
            "cost": 45.67,
            "threshold": 50.00,
            "breakdown": {
                "claude_api": 35.00,
                "infrastructure": 10.67
            }
        }
    """
    try:
        # 1. 獲取請求數據
        data = request.get_json()

        if not data:
            return jsonify({"error": "No data provided"}), 400

        # 2. 格式化消息
        message = format_cost_alert(data)

        # 3. 發送到 Telegram
        success = send_telegram_message(message)

        if success:
            return jsonify({"status": "sent"}), 200
        else:
            return jsonify({"error": "Failed to send message"}), 500

    except Exception as e:
        logger.error(f"Error handling cost alert: {e}")
        return jsonify({"error": str(e)}), 500

# ============================================================================
# 工具函數: 消息格式化
# ============================================================================

def format_workflow_message(data: Dict[str, Any]) -> str:
    """
    格式化 Workflow 消息為 Markdown

    Args:
        data: Workflow 數據
            {
                "workflow_name": "analyzer-workflow",
                "status": "Succeeded",
                "message": "Analysis completed",
                "duration": "5m30s",
                "node_count": 3
            }

    Returns:
        Markdown 格式的消息
    """
    from datetime import datetime

    # 狀態 Emoji
    status_emoji = {
        "Succeeded": "✅",
        "Failed": "❌",
        "Running": "🔄",
        "Pending": "⏳"
    }

    status = data.get("status", "Unknown")
    emoji = status_emoji.get(status, "❓")

    # 構建 Markdown 消息
    message = f"""
{emoji} *Workflow Status Update*

*Workflow*: `{data.get("workflow_name", "N/A")}`
*Status*: {status}
*Message*: {data.get("message", "N/A")}

*Details*:
• Duration: {data.get("duration", "N/A")}
• Nodes: {data.get("node_count", 0)}
• Timestamp: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""

    return message.strip()

def format_approval_message(data: Dict[str, Any]) -> str:
    """
    格式化審批請求消息

    Args:
        data: 審批請求數據
            {
                "type": "file_access",
                "file_path": "/path/to/file",
                "agent": "java-architect",
                "reason": "Need to modify foundation layer",
                "operation": "WRITE",
                "request_id": "abc123"
            }

    Returns:
        Markdown 格式的消息
    """
    from datetime import datetime

    request_id = data.get("request_id", "N/A")

    message = f"""
⚠️ *Manual Approval Required*

*Type*: {data.get("type", "N/A")}
*Agent*: `{data.get("agent", "Unknown")}`
*Operation*: {data.get("operation", "N/A")}

*File Path*:
```
{data.get("file_path", "N/A")}
```

*Reason*:
{data.get("reason", "N/A")}

*Request ID*: `{request_id}`
*Timestamp*: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

*Actions*:
• ✅ Approve: Reply with `/approve {request_id}`
• ❌ Deny: Reply with `/deny {request_id}`
"""

    return message.strip()

def format_cost_alert(data: Dict[str, Any]) -> str:
    """
    格式化成本告警消息

    Args:
        data: 成本告警數據
            {
                "period": "monthly",
                "cost": 45.67,
                "threshold": 50.00,
                "breakdown": {
                    "claude_api": 35.00,
                    "infrastructure": 10.67
                }
            }

    Returns:
        Markdown 格式的消息
    """
    from datetime import datetime

    cost = data.get("cost", 0.0)
    threshold = data.get("threshold", 0.0)
    percentage = (cost / threshold * 100) if threshold > 0 else 0

    # 成本告警級別
    if percentage >= 90:
        emoji = "🚨"
        level = "CRITICAL"
    elif percentage >= 75:
        emoji = "⚠️"
        level = "WARNING"
    else:
        emoji = "ℹ️"
        level = "INFO"

    message = f"""
{emoji} *Cost Alert - {level}*

*Period*: {data.get("period", "N/A")}
*Current Cost*: ${cost:.2f}
*Threshold*: ${threshold:.2f}
*Usage*: {percentage:.1f}%

*Breakdown*:
"""

    # 添加成本明細
    breakdown = data.get("breakdown", {})
    for item, amount in breakdown.items():
        message += f"• {item}: ${amount:.2f}\n"

    message += f"\n*Timestamp*: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"

    return message.strip()

# ============================================================================
# 工具函數: Telegram API
# ============================================================================

def send_telegram_message(message: str, parse_mode: str = 'Markdown') -> bool:
    """
    發送消息到 Telegram

    Args:
        message: 消息內容
        parse_mode: 解析模式 (Markdown/HTML)

    Returns:
        是否發送成功
    """
    if not TELEGRAM_BOT_TOKEN or not TELEGRAM_CHAT_ID:
        logger.error("Missing TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID")
        return False

    try:
        # 構建請求數據
        payload = {
            'chat_id': TELEGRAM_CHAT_ID,
            'text': message,
            'parse_mode': parse_mode
        }

        # 發送 POST 請求到 Telegram Bot API
        response = requests.post(
            TELEGRAM_API_URL,
            json=payload,
            timeout=10  # 10 秒超時
        )

        # 檢查響應狀態
        if response.status_code == 200:
            logger.info("Message sent successfully to Telegram")
            return True
        else:
            logger.error(f"Failed to send message: HTTP {response.status_code}, {response.text}")
            return False

    except requests.exceptions.RequestException as e:
        logger.error(f"Network error while sending message: {e}")
        return False

    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return False

# ============================================================================
# 端點 6: Telegram 命令處理（新增 - v2.0.0）
# ============================================================================

# 導入 Command Handler
try:
    from command_handler import TelegramCommandHandler
    command_handler = TelegramCommandHandler()
    logger.info("TelegramCommandHandler initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize TelegramCommandHandler: {e}")
    command_handler = None

@app.route('/command', methods=['POST'])
def handle_telegram_command():
    """
    處理 Telegram Bot 命令（新增 - v2.0.0）

    Request Body:
        {
            "command": "/new_feature",
            "args": {
                "name": "Employee Management",
                "entity": "Employee",
                "target": "sa-admin"
            }
        }

    支持的命令:
        - /new_feature - 創建新功能（完整工作流：Analyzer → Developer → QA）
        - /code_review - 代碼審查
        - /analyze - 代碼分析
        - /help - 顯示幫助信息
    """
    try:
        # 1. 檢查 Command Handler 是否可用
        if not command_handler:
            return jsonify({
                "error": "Command handler not initialized"
            }), 500

        # 2. 獲取請求數據
        data = request.get_json()

        if not data:
            return jsonify({"error": "No data provided"}), 400

        command = data.get('command')
        args = data.get('args', {})

        if not command:
            return jsonify({"error": "Missing 'command' field"}), 400

        logger.info(f"Received command: {command}")
        logger.info(f"Arguments: {args}")

        # 3. 執行命令並觸發工作流
        result_message = command_handler.handle_command(command, args)

        # 4. 發送結果到 Telegram
        success = send_telegram_message(result_message)

        if success:
            return jsonify({
                "status": "executed",
                "command": command,
                "message": result_message
            }), 200
        else:
            return jsonify({
                "error": "Failed to send message to Telegram"
            }), 500

    except Exception as e:
        logger.error(f"Error handling command: {e}", exc_info=True)

        # 發送錯誤消息到 Telegram
        error_message = f"""
❌ *Command Execution Error*

*Command*: `{data.get('command', 'unknown')}`
*Error*: {str(e)}

Please check the logs for details.
"""
        send_telegram_message(error_message)

        return jsonify({"error": str(e)}), 500

# ============================================================================
# 主函數
# ============================================================================

if __name__ == '__main__':
    # TODO: 檢查環境變量
    if not TELEGRAM_BOT_TOKEN or not TELEGRAM_CHAT_ID:
        logger.error("Missing TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID")
        logger.info("Set environment variables:")
        logger.info("  export TELEGRAM_BOT_TOKEN=<your_bot_token>")
        logger.info("  export TELEGRAM_CHAT_ID=<your_chat_id>")
        exit(1)

    logger.info("Starting Telegram Webhook Service...")
    logger.info("Version: 1.0.0")
    logger.info("Listening on: 0.0.0.0:8080")

    # 啟動 Flask 應用
    app.run(host='0.0.0.0', port=8080, debug=False)
