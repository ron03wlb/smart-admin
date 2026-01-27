#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Base Crew Class
所有 CrewAI Crew 實現的基礎類

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import logging
import os
import json
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional, List
from datetime import datetime
from contextlib import contextmanager
import psycopg2
from psycopg2 import pool
import requests
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

# ============================================================================
# 配置
# ============================================================================

# PostgreSQL 配置
DB_HOST = os.getenv('POSTGRES_HOST', 'localhost')
DB_PORT = os.getenv('POSTGRES_PORT', '5432')
DB_NAME = os.getenv('POSTGRES_DB', 'smart_admin')
DB_USER = os.getenv('POSTGRES_USER', 'postgres')
DB_PASSWORD = os.getenv('POSTGRES_PASSWORD', '')
DB_CONNECTION_STRING = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# Telegram 配置
TELEGRAM_BOT_TOKEN = os.getenv('TELEGRAM_BOT_TOKEN')
TELEGRAM_CHAT_ID = os.getenv('TELEGRAM_CHAT_ID')
TELEGRAM_API_URL = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendMessage"

# Claude API 配置
CLAUDE_API_KEY = os.getenv('CLAUDE_API_KEY')
CLAUDE_BASE_URL = os.getenv('CLAUDE_BASE_URL', 'https://api.anthropic.com')
CLAUDE_DEFAULT_MODEL = os.getenv('CLAUDE_DEFAULT_MODEL', 'claude-sonnet-4.5')

# 日誌配置
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

# ============================================================================
# 異常類型
# ============================================================================

class RetryableError(Exception):
    """可重試的錯誤（網絡暫時故障等）"""
    pass

class FatalError(Exception):
    """不可重試的錯誤（配置錯誤等）"""
    pass

class SecurityError(Exception):
    """安全相關錯誤（路徑遍歷等）"""
    pass

# ============================================================================
# 基礎類
# ============================================================================

class BaseCrew(ABC):
    """
    CrewAI Crew 基礎類

    所有 Crew 實現必須繼承此類並實現抽象方法。
    提供通用功能：審計日誌、Telegram 通知、錯誤處理、配置管理。
    """

    def __init__(self, crew_name: str, crew_type: str):
        """
        初始化 Crew

        Args:
            crew_name: Crew 名稱（例如：analyzer-crew）
            crew_type: Crew 類型（例如：analyzer, developer, qa）
        """
        self.crew_name = crew_name
        self.crew_type = crew_type
        self.logger = logging.getLogger(f"{__name__}.{crew_name}")

        # 初始化 PostgreSQL 連接池（Thread-safe）
        try:
            self.connection_pool = pool.ThreadedConnectionPool(
                minconn=1,
                maxconn=10,  # 增加連接池大小
                dsn=DB_CONNECTION_STRING,
                connect_timeout=10,
                options="-c statement_timeout=60000"  # 60秒超時
            )
            if self.connection_pool:
                self.logger.info(f"PostgreSQL connection pool created for {crew_name} (maxconn=10)")
            else:
                self.logger.error(f"Failed to create PostgreSQL connection pool for {crew_name}")
        except psycopg2.Error as e:
            self.logger.error(f"PostgreSQL connection error: {e}")
            self.connection_pool = None

        # 驗證 Telegram 配置
        if not TELEGRAM_BOT_TOKEN or not TELEGRAM_CHAT_ID:
            self.logger.warning("Telegram credentials not configured")

        # 驗證 Claude API 配置
        if not CLAUDE_API_KEY:
            self.logger.warning("Claude API key not configured")

    # ========================================================================
    # 抽象方法 (子類必須實現)
    # ========================================================================

    @abstractmethod
    def create_crew(self):
        """
        創建 CrewAI Crew 實例

        子類必須實現此方法，定義 Agents 和 Tasks。

        Returns:
            Crew: CrewAI Crew 實例
        """
        pass

    @abstractmethod
    def run(self, **kwargs) -> Dict[str, Any]:
        """
        運行 Crew 並返回結果

        子類必須實現此方法，定義執行邏輯。

        Args:
            **kwargs: Crew 執行參數

        Returns:
            Dict[str, Any]: 執行結果
        """
        pass

    # ========================================================================
    # 連接池管理（Context Manager）
    # ========================================================================

    @contextmanager
    def get_db_connection(self):
        """
        獲取數據庫連接（Context Manager）

        使用方式:
        >>> with self.get_db_connection() as conn:
        >>>     cursor = conn.cursor()
        >>>     cursor.execute("SELECT ...")

        連接會自動歸還到連接池，即使發生異常。
        """
        conn = None
        try:
            if not self.connection_pool:
                raise FatalError("Connection pool not initialized")

            conn = self.connection_pool.getconn()
            self.logger.debug("Database connection acquired from pool")
            yield conn
            conn.commit()
            self.logger.debug("Database transaction committed")

        except Exception as e:
            if conn:
                conn.rollback()
                self.logger.warning(f"Database transaction rolled back due to error: {e}")
            raise

        finally:
            # ✅ 關鍵：無論如何都歸還連接
            if conn:
                self.connection_pool.putconn(conn)
                self.logger.debug("Database connection returned to pool")

    # ========================================================================
    # 錯誤重試機制
    # ========================================================================

    @retry(
        retry=retry_if_exception_type(RetryableError),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=4, max=10),
        before_sleep=lambda retry_state: logging.getLogger(__name__).warning(
            f"Retrying after error (attempt {retry_state.attempt_number}/3)..."
        )
    )
    def run_with_retry(self, **kwargs) -> Dict[str, Any]:
        """
        帶重試機制的運行方法

        自動重試 RetryableError（最多3次，指數退避）:
        - 網絡錯誤（requests.exceptions.RequestException）
        - 數據庫暫時故障（psycopg2.OperationalError）

        不重試 FatalError:
        - 配置錯誤（ValueError, KeyError）
        - 文件不存在（FileNotFoundError）
        - 安全錯誤（SecurityError）

        Args:
            **kwargs: Crew 執行參數

        Returns:
            Dict[str, Any]: 執行結果

        Raises:
            RetryableError: 可重試錯誤（會自動重試）
            FatalError: 不可重試錯誤（立即失敗）
        """
        try:
            return self.run(**kwargs)

        except (requests.exceptions.RequestException, psycopg2.OperationalError) as e:
            # 網絡錯誤、數據庫暫時故障 → 可重試
            self.logger.warning(f"Retryable error encountered: {e}")
            raise RetryableError(f"Retryable error: {e}") from e

        except (ValueError, KeyError, FileNotFoundError, SecurityError) as e:
            # 配置錯誤、文件不存在、安全錯誤 → 不可重試
            self.logger.error(f"Fatal error encountered: {e}")
            raise FatalError(f"Fatal error: {e}") from e

        except Exception as e:
            # 其他未知錯誤 → 視為致命錯誤
            self.logger.error(f"Unknown error encountered: {e}")
            raise FatalError(f"Unknown error: {e}") from e

    # ========================================================================
    # 審計日誌方法
    # ========================================================================

    def log_execution_start(self, input_params: Dict[str, Any]) -> Optional[int]:
        """
        記錄 Crew 執行開始

        Args:
            input_params: 輸入參數

        Returns:
            Optional[int]: 執行日誌 ID（用於後續更新）
        """
        if not self.connection_pool:
            self.logger.warning("Connection pool not available, skipping log")
            return None

        connection = None
        cursor = None

        try:
            connection = self.connection_pool.getconn()
            cursor = connection.cursor()

            insert_query = """
            INSERT INTO t_ai_execution_log (
                agent_name,
                operation_type,
                input_params,
                status,
                started_at,
                created_at
            ) VALUES (%s, %s, %s, %s, %s, %s)
            RETURNING id
            """

            now = datetime.now()
            cursor.execute(insert_query, (
                self.crew_name,
                'CREW_EXECUTION',
                json.dumps(input_params, ensure_ascii=False),
                'RUNNING',
                now,
                now
            ))

            execution_id = cursor.fetchone()[0]
            connection.commit()

            self.logger.info(f"Logged execution start: ID={execution_id}")
            return execution_id

        except psycopg2.Error as e:
            self.logger.error(f"Failed to log execution start: {e}")
            if connection:
                connection.rollback()
            return None

        finally:
            if cursor:
                cursor.close()
            if connection:
                self.connection_pool.putconn(connection)

    def log_execution_complete(
        self,
        execution_id: Optional[int],
        status: str,
        output_result: Optional[Dict[str, Any]] = None,
        error_message: Optional[str] = None,
        tokens_used: Optional[int] = None,
        cost: Optional[float] = None
    ) -> None:
        """
        記錄 Crew 執行完成

        Args:
            execution_id: 執行日誌 ID
            status: 執行狀態（SUCCESS, FAILED）
            output_result: 輸出結果
            error_message: 錯誤消息（如果失敗）
            tokens_used: Token 使用量
            cost: 成本（美元）
        """
        if not self.connection_pool or execution_id is None:
            self.logger.warning("Connection pool not available or execution_id is None, skipping log")
            return

        connection = None
        cursor = None

        try:
            connection = self.connection_pool.getconn()
            cursor = connection.cursor()

            update_query = """
            UPDATE t_ai_execution_log
            SET status = %s,
                output_result = %s,
                error_message = %s,
                tokens_used = %s,
                cost = %s,
                completed_at = %s,
                updated_at = %s
            WHERE id = %s
            """

            now = datetime.now()
            cursor.execute(update_query, (
                status,
                json.dumps(output_result, ensure_ascii=False) if output_result else None,
                error_message,
                tokens_used,
                cost,
                now,
                now,
                execution_id
            ))

            connection.commit()

            self.logger.info(f"Logged execution complete: ID={execution_id}, Status={status}")

        except psycopg2.Error as e:
            self.logger.error(f"Failed to log execution complete: {e}")
            if connection:
                connection.rollback()

        finally:
            if cursor:
                cursor.close()
            if connection:
                self.connection_pool.putconn(connection)

    # ========================================================================
    # Telegram 通知方法
    # ========================================================================

    def send_telegram_notification(self, message: str, parse_mode: str = 'Markdown') -> bool:
        """
        發送 Telegram 通知

        Args:
            message: 消息內容
            parse_mode: 解析模式（Markdown/HTML）

        Returns:
            bool: 是否發送成功
        """
        if not TELEGRAM_BOT_TOKEN or not TELEGRAM_CHAT_ID:
            self.logger.warning("Telegram credentials not configured")
            return False

        try:
            payload = {
                'chat_id': TELEGRAM_CHAT_ID,
                'text': message,
                'parse_mode': parse_mode
            }

            response = requests.post(TELEGRAM_API_URL, json=payload, timeout=10)

            if response.status_code == 200:
                self.logger.info("Telegram notification sent successfully")
                return True
            else:
                self.logger.error(f"Failed to send Telegram notification: HTTP {response.status_code}")
                return False

        except requests.exceptions.RequestException as e:
            self.logger.error(f"Network error while sending Telegram notification: {e}")
            return False

    def format_execution_notification(
        self,
        status: str,
        duration: str,
        summary: str,
        details: Optional[List[str]] = None
    ) -> str:
        """
        格式化 Crew 執行通知消息

        Args:
            status: 執行狀態（SUCCESS, FAILED）
            duration: 執行時長
            summary: 摘要
            details: 詳細信息列表

        Returns:
            str: Markdown 格式的消息
        """
        # 狀態 Emoji
        status_emoji = {
            "SUCCESS": "✅",
            "FAILED": "❌",
            "RUNNING": "🔄"
        }
        emoji = status_emoji.get(status, "❓")

        message = f"""
{emoji} *Crew Execution Update*

*Crew*: `{self.crew_name}`
*Type*: {self.crew_type}
*Status*: {status}
*Duration*: {duration}

*Summary*:
{summary}
"""

        if details:
            message += "\n*Details*:\n"
            for detail in details:
                message += f"• {detail}\n"

        message += f"\n*Timestamp*: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"

        return message.strip()

    # ========================================================================
    # 錯誤處理方法
    # ========================================================================

    def handle_error(
        self,
        execution_id: Optional[int],
        error: Exception,
        context: str = ""
    ) -> Dict[str, Any]:
        """
        處理 Crew 執行錯誤

        Args:
            execution_id: 執行日誌 ID
            error: 異常對象
            context: 錯誤上下文描述

        Returns:
            Dict[str, Any]: 錯誤結果
        """
        error_message = f"{context}: {str(error)}" if context else str(error)

        self.logger.error(f"Crew execution error: {error_message}", exc_info=True)

        # 記錄到數據庫
        self.log_execution_complete(
            execution_id=execution_id,
            status='FAILED',
            error_message=error_message
        )

        # 發送 Telegram 通知
        notification = self.format_execution_notification(
            status='FAILED',
            duration='N/A',
            summary=f"Crew execution failed: {error_message}",
            details=None
        )
        self.send_telegram_notification(notification)

        return {
            "status": "FAILED",
            "error": error_message,
            "crew_name": self.crew_name,
            "timestamp": datetime.now().isoformat()
        }

    # ========================================================================
    # 配置管理方法
    # ========================================================================

    def get_config(self, key: str, default: Any = None) -> Any:
        """
        獲取配置值（從環境變量）

        Args:
            key: 配置鍵
            default: 默認值

        Returns:
            Any: 配置值
        """
        return os.getenv(key, default)

    def validate_config(self, required_keys: List[str]) -> bool:
        """
        驗證必需的配置是否存在

        Args:
            required_keys: 必需的配置鍵列表

        Returns:
            bool: 所有必需配置是否都存在
        """
        missing_keys = []
        for key in required_keys:
            if not os.getenv(key):
                missing_keys.append(key)

        if missing_keys:
            self.logger.error(f"Missing required configuration: {', '.join(missing_keys)}")
            return False

        return True

    # ========================================================================
    # 清理方法
    # ========================================================================

    def cleanup(self) -> None:
        """
        清理資源（關閉數據庫連接池等）
        """
        if self.connection_pool:
            try:
                self.connection_pool.closeall()
                self.logger.info(f"Connection pool closed for {self.crew_name}")
            except Exception as e:
                self.logger.error(f"Error closing connection pool: {e}")

    def __del__(self):
        """析構函數，確保資源清理"""
        self.cleanup()

# ============================================================================
# 工具函數
# ============================================================================

def get_crew_config() -> Dict[str, Any]:
    """
    獲取 Crew 通用配置

    Returns:
        Dict[str, Any]: 配置字典
    """
    return {
        "db_connection": DB_CONNECTION_STRING,
        "telegram_enabled": bool(TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID),
        "claude_api_enabled": bool(CLAUDE_API_KEY),
        "claude_model": CLAUDE_DEFAULT_MODEL,
    }

def test_database_connection() -> bool:
    """
    測試 PostgreSQL 數據庫連接

    Returns:
        bool: 連接是否成功
    """
    try:
        conn = psycopg2.connect(DB_CONNECTION_STRING)
        cursor = conn.cursor()
        cursor.execute("SELECT 1")
        cursor.close()
        conn.close()
        return True
    except psycopg2.Error as e:
        logging.error(f"Database connection test failed: {e}")
        return False

def test_telegram_connection() -> bool:
    """
    測試 Telegram Bot 連接

    Returns:
        bool: 連接是否成功
    """
    if not TELEGRAM_BOT_TOKEN:
        return False

    try:
        test_url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/getMe"
        response = requests.get(test_url, timeout=5)
        return response.status_code == 200
    except Exception as e:
        logging.error(f"Telegram connection test failed: {e}")
        return False

# ============================================================================
# 測試主函數
# ============================================================================

if __name__ == '__main__':
    print("=" * 70)
    print("BaseCrew Configuration Test")
    print("=" * 70)
    print()

    # 測試配置
    config = get_crew_config()
    print("Configuration:")
    print(f"  Database: {DB_HOST}:{DB_PORT}/{DB_NAME}")
    print(f"  Telegram Enabled: {config['telegram_enabled']}")
    print(f"  Claude API Enabled: {config['claude_api_enabled']}")
    print(f"  Claude Model: {config['claude_model']}")
    print()

    # 測試數據庫連接
    print("Testing database connection...")
    if test_database_connection():
        print("✓ Database connection successful")
    else:
        print("✗ Database connection failed")
    print()

    # 測試 Telegram 連接
    print("Testing Telegram connection...")
    if test_telegram_connection():
        print("✓ Telegram connection successful")
    else:
        print("✗ Telegram connection failed")
    print()

    print("=" * 70)
    print("Test completed")
    print("=" * 70)
