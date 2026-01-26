"""
SmartAdmin AI 文件訪問控制系統
實施 P0-3 安全修復：防止 Agent 訪問敏感文件

使用方式：
    from tools.file_access_guard import FileAccessGuard

    guard = FileAccessGuard()
    if guard.is_allowed("src/main/java/SomeController.java", "read"):
        # 執行操作
    else:
        # 拒絕並記錄
"""

import re
from pathlib import Path
from typing import Literal, Tuple
from enum import Enum


class AccessDecision(Enum):
    """訪問決策結果"""
    ALLOW = "allow"
    DENY = "deny"
    AUDIT = "audit"  # 允許但需人工審批


class FileAccessGuard:
    """文件訪問守衛：實施白名單和黑名單控制"""

    # 允許讀取的路徑模式（白名單）
    ALLOWED_READ_PATTERNS = [
        # 業務代碼
        r".*/(controller|service|manager|dao)/.*\.java$",
        r".*/(domain|entity|vo|form|query)/.*\.java$",

        # 前端代碼
        r".*/vue/.*\.(vue|ts|js)$",

        # 配置文件（僅讀取）
        r".*/application.*\.yml$",
        r".*/logback.*\.xml$",

        # 測試代碼
        r".*/src/test/java/.*\.java$",

        # 文檔
        r".*\.(md|txt)$",

        # MyBatis Mapper
        r".*/mapper/.*\.xml$",
    ]

    # 允許寫入的路徑模式（更嚴格）
    ALLOWED_WRITE_PATTERNS = [
        # 僅允許業務代碼層
        r".*/(controller|service|manager|dao)/.*\.java$",
        r".*/(domain|entity|vo|form|query)/.*\.java$",

        # 前端組件
        r".*/vue/.*\.(vue|ts)$",

        # 測試文件
        r".*/src/test/java/.*\.java$",

        # MyBatis Mapper
        r".*/mapper/.*\.xml$",
    ]

    # 禁止訪問的路徑模式（黑名單）
    FORBIDDEN_PATTERNS = [
        # 配置文件（包含敏感信息）
        r".*application-prod\.yml$",
        r".*application-secret\.yml$",
        r".*\.env$",
        r".*/secret.*",
        r".*/credential.*",

        # 安全相關代碼
        r".*/SecurityConfig\.java$",
        r".*/JwtFilter\.java$",
        r".*/AuthenticationFilter\.java$",

        # 資料庫遷移腳本（防止誤修改）
        r".*/db/migration/V.*\.sql$",
        r".*/flyway/.*\.sql$",

        # 核心基礎設施
        r".*/foundation/core/.*\.java$",
        r".*/base/common/.*\.java$",

        # Git 和 IDE 配置
        r".*\.git/.*",
        r".*\.idea/.*",
        r".*\.vscode/.*",

        # 構建產物
        r".*/build/.*",
        r".*/target/.*",
        r".*\.jar$",
        r".*\.class$",
    ]

    # 需要人工審批的路徑模式（審計模式）
    AUDIT_REQUIRED_PATTERNS = [
        # 架構層級代碼
        r".*/foundation/.*\.java$",
        r".*/base/.*\.java$",

        # 全局配置
        r".*/GlobalExceptionHandler\.java$",
        r".*/WebMvcConfig\.java$",

        # 資料源配置
        r".*/DataSourceConfig\.java$",
        r".*/RedisConfig\.java$",
    ]

    def __init__(self, log_callback=None):
        """
        初始化文件訪問守衛

        Args:
            log_callback: 日誌回調函數，用於記錄訪問決策
        """
        self.log_callback = log_callback
        self._compile_patterns()

    def _compile_patterns(self):
        """編譯正則表達式模式"""
        self.allowed_read_compiled = [re.compile(p) for p in self.ALLOWED_READ_PATTERNS]
        self.allowed_write_compiled = [re.compile(p) for p in self.ALLOWED_WRITE_PATTERNS]
        self.forbidden_compiled = [re.compile(p) for p in self.FORBIDDEN_PATTERNS]
        self.audit_compiled = [re.compile(p) for p in self.AUDIT_REQUIRED_PATTERNS]

    def check_access(
        self,
        file_path: str,
        operation: Literal["read", "write", "delete"],
        agent_name: str = "unknown"
    ) -> Tuple[AccessDecision, str]:
        """
        檢查文件訪問權限

        Args:
            file_path: 文件路徑
            operation: 操作類型（read/write/delete）
            agent_name: Agent 名稱

        Returns:
            Tuple[AccessDecision, str]: (決策, 原因說明)
        """
        # 規範化路徑
        normalized_path = str(Path(file_path)).replace("\\", "/")

        # 1. 檢查黑名單（最高優先級）
        for pattern in self.forbidden_compiled:
            if pattern.match(normalized_path):
                reason = f"路徑匹配禁止模式: {pattern.pattern}"
                self._log_access(agent_name, normalized_path, operation, AccessDecision.DENY, reason)
                return AccessDecision.DENY, reason

        # 2. 檢查審計模式（次優先級）
        for pattern in self.audit_compiled:
            if pattern.match(normalized_path):
                reason = f"路徑匹配審計模式: {pattern.pattern}，需要人工審批"
                self._log_access(agent_name, normalized_path, operation, AccessDecision.AUDIT, reason)
                return AccessDecision.AUDIT, reason

        # 3. 根據操作類型檢查白名單
        if operation == "read":
            allowed_patterns = self.allowed_read_compiled
        elif operation == "write":
            allowed_patterns = self.allowed_write_compiled
        elif operation == "delete":
            # 刪除操作非常危險，默認拒絕
            reason = "刪除操作默認禁止"
            self._log_access(agent_name, normalized_path, operation, AccessDecision.DENY, reason)
            return AccessDecision.DENY, reason
        else:
            reason = f"未知操作類型: {operation}"
            self._log_access(agent_name, normalized_path, operation, AccessDecision.DENY, reason)
            return AccessDecision.DENY, reason

        # 4. 檢查白名單
        for pattern in allowed_patterns:
            if pattern.match(normalized_path):
                reason = f"路徑匹配允許模式: {pattern.pattern}"
                self._log_access(agent_name, normalized_path, operation, AccessDecision.ALLOW, reason)
                return AccessDecision.ALLOW, reason

        # 5. 默認拒絕（白名單機制）
        reason = "路徑不在白名單中"
        self._log_access(agent_name, normalized_path, operation, AccessDecision.DENY, reason)
        return AccessDecision.DENY, reason

    def _log_access(
        self,
        agent_name: str,
        file_path: str,
        operation: str,
        decision: AccessDecision,
        reason: str
    ):
        """記錄訪問決策"""
        if self.log_callback:
            self.log_callback({
                "agent_name": agent_name,
                "file_path": file_path,
                "operation": operation,
                "decision": decision.value,
                "reason": reason
            })

    def is_allowed(
        self,
        file_path: str,
        operation: Literal["read", "write", "delete"],
        agent_name: str = "unknown"
    ) -> bool:
        """
        簡化的訪問檢查（僅返回 True/False）

        注意：AUDIT 模式會返回 False，需要通過 check_access 獲取詳細信息
        """
        decision, _ = self.check_access(file_path, operation, agent_name)
        return decision == AccessDecision.ALLOW


# 使用示例
if __name__ == "__main__":
    guard = FileAccessGuard()

    # 測試案例
    test_cases = [
        # (路徑, 操作, 預期結果)
        ("src/main/java/module/employee/controller/EmployeeController.java", "read", AccessDecision.ALLOW),
        ("src/main/java/module/employee/controller/EmployeeController.java", "write", AccessDecision.ALLOW),
        ("src/main/resources/application-prod.yml", "read", AccessDecision.DENY),
        ("src/main/java/config/SecurityConfig.java", "write", AccessDecision.DENY),
        ("src/main/java/foundation/domain/ResponseDTO.java", "write", AccessDecision.AUDIT),
        ("db/migration/V001__init.sql", "write", AccessDecision.DENY),
        ("some/random/file.txt", "read", AccessDecision.DENY),
    ]

    print("文件訪問控制測試:\n")
    for file_path, operation, expected in test_cases:
        decision, reason = guard.check_access(file_path, operation, agent_name="test-agent")
        status = "✅" if decision == expected else "❌"
        print(f"{status} {operation:6} {file_path:60} => {decision.value:6} ({reason})")
