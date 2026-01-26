"""
SmartAdmin Telegram Webhook 服務
接收 Prometheus AlertManager 告警並轉發到 Telegram

功能：
1. 接收 AlertManager Webhook
2. 格式化告警消息
3. 發送到 Telegram Bot
4. 接收 Argo Workflow 狀態通知
"""

from flask import Flask, request, jsonify
import requests
import os
from datetime import datetime
from typing import Dict, Any

app = Flask(__name__)

# 從環境變量讀取配置
TELEGRAM_BOT_TOKEN = os.environ.get('TELEGRAM_BOT_TOKEN', '')
TELEGRAM_CHAT_ID = os.environ.get('TELEGRAM_CHAT_ID', '')
TELEGRAM_API_URL = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}"


def send_telegram_message(text: str, parse_mode: str = "HTML") -> bool:
    """
    發送 Telegram 消息

    Args:
        text: 消息內容
        parse_mode: 解析模式（HTML 或 Markdown）

    Returns:
        bool: 發送是否成功
    """
    try:
        response = requests.post(
            f"{TELEGRAM_API_URL}/sendMessage",
            json={
                "chat_id": TELEGRAM_CHAT_ID,
                "text": text,
                "parse_mode": parse_mode,
                "disable_web_page_preview": True
            },
            timeout=10
        )
        return response.status_code == 200
    except Exception as e:
        print(f"Failed to send Telegram message: {e}")
        return False


def format_alert_message(alert: Dict[str, Any]) -> str:
    """
    格式化 Prometheus 告警消息

    Args:
        alert: AlertManager alert 對象

    Returns:
        str: 格式化後的消息
    """
    status = alert.get('status', 'unknown')
    labels = alert.get('labels', {})
    annotations = alert.get('annotations', {})

    # 根據嚴重性選擇 Emoji
    severity = labels.get('severity', 'info')
    emoji_map = {
        'critical': '🔴',
        'warning': '⚠️',
        'info': 'ℹ️'
    }
    emoji = emoji_map.get(severity, '📢')

    # 構建消息
    alert_name = labels.get('alertname', 'Unknown Alert')
    summary = annotations.get('summary', 'No summary available')
    description = annotations.get('description', '')

    message = f"{emoji} <b>{alert_name}</b>\n\n"
    message += f"<b>狀態</b>: {status}\n"
    message += f"<b>嚴重性</b>: {severity}\n"
    message += f"<b>摘要</b>: {summary}\n"

    if description:
        message += f"<b>描述</b>: {description}\n"

    # 添加時間戳
    starts_at = alert.get('startsAt', '')
    if starts_at:
        message += f"\n<b>開始時間</b>: {starts_at}\n"

    return message


@app.route('/health', methods=['GET'])
def health_check():
    """健康檢查端點"""
    return jsonify({"status": "healthy", "service": "telegram-webhook"}), 200


@app.route('/alert', methods=['POST'])
def handle_alert():
    """
    處理 Prometheus AlertManager Webhook

    Payload 示例：
    {
        "alerts": [
            {
                "status": "firing",
                "labels": {
                    "alertname": "HighErrorRate",
                    "severity": "critical"
                },
                "annotations": {
                    "summary": "錯誤率超過 5%"
                }
            }
        ]
    }
    """
    try:
        data = request.json
        alerts = data.get('alerts', [])

        for alert in alerts:
            message = format_alert_message(alert)
            send_telegram_message(message)

        return jsonify({"status": "ok", "alerts_processed": len(alerts)}), 200

    except Exception as e:
        print(f"Error handling alert: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


@app.route('/workflow', methods=['POST'])
def handle_workflow_notification():
    """
    處理 Argo Workflow 狀態通知

    Payload 示例：
    {
        "workflow_name": "analyzer-workflow",
        "status": "success",
        "duration": "5m30s",
        "message": "Analysis completed successfully"
    }
    """
    try:
        data = request.json
        workflow_name = data.get('workflow_name', 'Unknown Workflow')
        status = data.get('status', 'unknown')
        duration = data.get('duration', 'N/A')
        message_text = data.get('message', '')

        # 根據狀態選擇 Emoji
        status_emoji = {
            'success': '✅',
            'failed': '❌',
            'running': '🔄',
            'pending': '⏳'
        }.get(status, '📋')

        message = f"{status_emoji} <b>Workflow 狀態更新</b>\n\n"
        message += f"<b>名稱</b>: {workflow_name}\n"
        message += f"<b>狀態</b>: {status}\n"
        message += f"<b>耗時</b>: {duration}\n"

        if message_text:
            message += f"\n{message_text}\n"

        send_telegram_message(message)

        return jsonify({"status": "ok"}), 200

    except Exception as e:
        print(f"Error handling workflow notification: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


@app.route('/approval', methods=['POST'])
def handle_approval_request():
    """
    處理人工審批請求（P1/P2 優化建議）

    Payload 示例：
    {
        "type": "optimization",
        "priority": "P1",
        "title": "優化 SQL N+1 查詢",
        "description": "發現 EmployeeService 存在 N+1 查詢問題",
        "approval_url": "http://argo-ui:2746/workflows/smartadmin/analyzer-abc123"
    }
    """
    try:
        data = request.json
        approval_type = data.get('type', 'unknown')
        priority = data.get('priority', 'P2')
        title = data.get('title', 'Approval Required')
        description = data.get('description', '')
        approval_url = data.get('approval_url', '')

        # 根據優先級選擇 Emoji
        priority_emoji = {
            'P0': '🚨',
            'P1': '⚠️',
            'P2': 'ℹ️'
        }.get(priority, '📋')

        message = f"{priority_emoji} <b>需要人工審批</b>\n\n"
        message += f"<b>類型</b>: {approval_type}\n"
        message += f"<b>優先級</b>: {priority}\n"
        message += f"<b>標題</b>: {title}\n\n"
        message += f"{description}\n\n"

        if approval_url:
            message += f"<a href='{approval_url}'>點擊查看詳情</a>\n"

        send_telegram_message(message)

        return jsonify({"status": "ok"}), 200

    except Exception as e:
        print(f"Error handling approval request: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


@app.route('/cost', methods=['POST'])
def handle_cost_alert():
    """
    處理成本告警

    Payload 示例：
    {
        "period": "daily",
        "current_cost": 75.50,
        "budget_limit": 50.00,
        "agent_breakdown": {
            "java-architect": 30.00,
            "postgres-pro": 20.00,
            "code-reviewer": 25.50
        }
    }
    """
    try:
        data = request.json
        period = data.get('period', 'daily')
        current_cost = data.get('current_cost', 0)
        budget_limit = data.get('budget_limit', 0)
        agent_breakdown = data.get('agent_breakdown', {})

        message = f"💰 <b>成本告警</b>\n\n"
        message += f"<b>週期</b>: {period}\n"
        message += f"<b>當前成本</b>: ${current_cost:.2f}\n"
        message += f"<b>預算限制</b>: ${budget_limit:.2f}\n"
        message += f"<b>超支</b>: ${current_cost - budget_limit:.2f}\n\n"

        if agent_breakdown:
            message += "<b>Agent 成本分解</b>:\n"
            for agent, cost in sorted(agent_breakdown.items(), key=lambda x: x[1], reverse=True):
                message += f"  • {agent}: ${cost:.2f}\n"

        message += "\n⚠️ 已自動切換到節省模式（Haiku）"

        send_telegram_message(message)

        return jsonify({"status": "ok"}), 200

    except Exception as e:
        print(f"Error handling cost alert: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


if __name__ == '__main__':
    # 驗證環境變量
    if not TELEGRAM_BOT_TOKEN or not TELEGRAM_CHAT_ID:
        print("ERROR: TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID must be set")
        exit(1)

    print(f"Starting Telegram Webhook service...")
    print(f"Bot Token: {TELEGRAM_BOT_TOKEN[:10]}...")
    print(f"Chat ID: {TELEGRAM_CHAT_ID}")

    # 發送啟動消息
    send_telegram_message("🚀 <b>Telegram Webhook 服務已啟動</b>\n\nSmartAdmin AI 通知系統已就緒")

    # 啟動 Flask 服務
    app.run(host='0.0.0.0', port=8080)
