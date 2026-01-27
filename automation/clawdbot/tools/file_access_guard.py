#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - File Access Guard
文件訪問控制系統：白名單/黑名單/審計模式

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import re
import logging
from enum import Enum
from dataclasses import dataclass
from typing import Tuple, Optional
import psycopg2

# ============================================================================
# 日誌配置
# ============================================================================

logger = logging.getLogger(__name__)

# ============================================================================
# 常量定義
# ============================================================================

# 白名單規則 (允許讀取)
ALLOWED_READ_PATTERNS = [
    r".*/(controller|service|manager|dao)/.*\.java$",
    r".*/(domain|entity|vo|form|query)/.*\.java$",
    r".*/vue/.*\.(vue|ts|js)$",
    r".*/application.*\.yml$",
    r".*/src/test/java/.*\.java$",
    r".*/mapper/.*\.xml$",
]

# 白名單規則 (允許寫入 - 更嚴格)
ALLOWED_WRITE_PATTERNS = [
    r".*/(controller|service|manager|dao)/.*\.java$",
    r".*/(domain|entity|vo|form|query)/.*\.java$",
    r".*/vue/.*\.(vue|ts)$",
    r".*/src/test/java/.*\.java$",
    r".*/mapper/.*\.xml$",
]

# 黑名單規則 (禁止訪問)
FORBIDDEN_PATTERNS = [
    r".*application-prod\.yml$",
    r".*\.env$",
    r".*/secret.*",
    r".*/SecurityConfig\.java$",
    r".*/db/migration/V.*\.sql$",
    r".*/foundation/core/.*\.java$",
    r".*\.git/.*",
]

# 審計模式 (需人工審批)
AUDIT_REQUIRED_PATTERNS = [
    r".*/foundation/.*\.java$",
    r".*/GlobalExceptionHandler\.java$",
    r".*/DataSourceConfig\.java$",
]

# ============================================================================
# 枚舉和數據類
# ============================================================================

class AccessDecision(Enum):
    """訪問決策枚舉"""
    ALLOW = "ALLOW"      # 允許訪問
    DENY = "DENY"        # 拒絕訪問
    AUDIT = "AUDIT"      # 需要審計審批

class Operation(Enum):
    """操作類型枚舉"""
    READ = "READ"
    WRITE = "WRITE"
    DELETE = "DELETE"

@dataclass
class AccessCheckResult:
    """訪問檢查結果"""
    decision: AccessDecision
    reason: str
    matched_pattern: Optional[str] = None

# ============================================================================
# 核心類
# ============================================================================

class FileAccessGuard:
    """文件訪問控制守衛"""

    def __init__(self, db_connection_string: str):
        """
        初始化文件訪問守衛

        Args:
            db_connection_string: PostgreSQL 連接字符串
                格式: postgresql://user:password@host:port/database
        """
        from psycopg2 import pool

        self.logger = logging.getLogger(__name__)

        try:
            # 創建連接池（最小 1 個連接，最大 10 個連接）
            self.connection_pool = pool.SimpleConnectionPool(
                minconn=1,
                maxconn=10,
                dsn=db_connection_string
            )

            if self.connection_pool:
                self.logger.info("PostgreSQL connection pool created successfully")
            else:
                self.logger.error("Failed to create PostgreSQL connection pool")
                raise Exception("Connection pool creation failed")

        except psycopg2.Error as e:
            self.logger.error(f"PostgreSQL connection error: {e}")
            raise

    def check_access(
        self,
        file_path: str,
        operation: Operation,
        agent_name: str
    ) -> AccessCheckResult:
        """
        檢查文件訪問權限

        Args:
            file_path: 文件路徑
            operation: 操作類型 (READ/WRITE/DELETE)
            agent_name: Agent 名稱

        Returns:
            AccessCheckResult: 訪問檢查結果
        """
        # 1. 規範化路徑
        normalized_path = normalize_path(file_path)

        # 2. 檢查黑名單（最高優先級）
        is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
        if is_forbidden:
            reason = f"Forbidden by pattern: {pattern}"
            logger.warning(f"[DENY] {agent_name} tried to {operation.value} {normalized_path}: {reason}")
            return AccessCheckResult(
                decision=AccessDecision.DENY,
                reason=reason,
                matched_pattern=pattern
            )

        # 3. 檢查審計規則（需人工審批）
        is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
        if is_audit:
            reason = f"Audit required by pattern: {pattern}"
            logger.info(f"[AUDIT] {agent_name} requested {operation.value} on {normalized_path}: {reason}")
            return AccessCheckResult(
                decision=AccessDecision.AUDIT,
                reason=reason,
                matched_pattern=pattern
            )

        # 4. 檢查白名單（根據操作類型）
        if operation == Operation.READ:
            patterns = ALLOWED_READ_PATTERNS
        elif operation == Operation.WRITE:
            patterns = ALLOWED_WRITE_PATTERNS
        else:  # DELETE
            # DELETE 操作默認拒絕（除非明確在白名單）
            patterns = []

        is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
        if is_allowed:
            reason = f"Allowed by pattern: {pattern}"
            logger.info(f"[ALLOW] {agent_name} {operation.value} {normalized_path}: {reason}")
            return AccessCheckResult(
                decision=AccessDecision.ALLOW,
                reason=reason,
                matched_pattern=pattern
            )

        # 5. 默認拒絕（無匹配模式）
        reason = "No matching pattern (default deny)"
        logger.warning(f"[DENY] {agent_name} tried to {operation.value} {normalized_path}: {reason}")
        return AccessCheckResult(
            decision=AccessDecision.DENY,
            reason=reason,
            matched_pattern=None
        )

    def log_access_decision(
        self,
        file_path: str,
        operation: Operation,
        decision: AccessDecision,
        agent_name: str,
        reason: str
    ) -> None:
        """
        記錄訪問決策到 PostgreSQL 審計表

        Args:
            file_path: 文件路徑
            operation: 操作類型
            decision: 訪問決策
            agent_name: Agent 名稱
            reason: 決策原因
        """
        from datetime import datetime

        connection = None
        cursor = None

        try:
            # 從連接池獲取連接
            connection = self.connection_pool.getconn()
            cursor = connection.cursor()

            # 插入審計記錄
            insert_query = """
            INSERT INTO t_ai_operation_audit (
                operation_type,
                file_path,
                decision,
                agent_name,
                reason,
                timestamp,
                created_at
            ) VALUES (%s, %s, %s, %s, %s, %s, %s)
            """

            now = datetime.now()
            cursor.execute(insert_query, (
                operation.value,      # 'READ', 'WRITE', 'DELETE'
                file_path,
                decision.value,       # 'ALLOW', 'DENY', 'AUDIT'
                agent_name,
                reason,
                now,
                now
            ))

            # 提交事務
            connection.commit()

            self.logger.debug(f"Logged access decision: {decision.value} for {file_path}")

        except psycopg2.Error as e:
            self.logger.error(f"Failed to log access decision: {e}")
            if connection:
                connection.rollback()
            # 不拋出異常，避免影響主流程

        finally:
            # 關閉游標和歸還連接到池
            if cursor:
                cursor.close()
            if connection:
                self.connection_pool.putconn(connection)

    def _check_pattern_match(
        self,
        file_path: str,
        patterns: list[str]
    ) -> Tuple[bool, Optional[str]]:
        """
        檢查文件路徑是否匹配給定的正則表達式模式列表

        Args:
            file_path: 文件路徑（已規範化）
            patterns: 正則表達式模式列表

        Returns:
            (是否匹配, 匹配的模式)
        """
        for pattern in patterns:
            try:
                # 使用 re.match() 從字符串開頭匹配
                if re.match(pattern, file_path):
                    return (True, pattern)
            except re.error as e:
                # 記錄正則表達式錯誤
                logger.error(f"Invalid regex pattern '{pattern}': {e}")
                continue

        # 無匹配模式
        return (False, None)

# ============================================================================
# 工具函數
# ============================================================================

def normalize_path(file_path: str) -> str:
    """
    規範化文件路徑（處理 Windows/Linux 路徑分隔符差異）

    Args:
        file_path: 原始文件路徑

    Returns:
        規範化後的路徑（統一使用正斜杠）
    """
    import os
    # 1. 使用 os.path.normpath 規範化路徑
    normalized = os.path.normpath(file_path)

    # 2. 統一使用正斜杠（便於正則表達式匹配）
    normalized = normalized.replace('\\', '/')

    # 3. 移除多餘的斜杠
    while '//' in normalized:
        normalized = normalized.replace('//', '/')

    return normalized

# ============================================================================
# 測試用例 (Inline)
# ============================================================================

def test_allow_read_controller():
    """測試：允許讀取 Controller"""
    # 創建簡單的測試類（不需要數據庫連接）
    class TestGuard:
        def _check_pattern_match(self, file_path: str, patterns: list[str]) -> Tuple[bool, Optional[str]]:
            for pattern in patterns:
                if re.match(pattern, file_path):
                    return (True, pattern)
            return (False, None)

        def check_access(self, file_path: str, operation: Operation, agent_name: str) -> AccessCheckResult:
            normalized_path = normalize_path(file_path)
            is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
            if is_forbidden:
                return AccessCheckResult(AccessDecision.DENY, f"Forbidden: {pattern}", pattern)
            is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
            if is_audit:
                return AccessCheckResult(AccessDecision.AUDIT, f"Audit: {pattern}", pattern)
            patterns = ALLOWED_READ_PATTERNS if operation == Operation.READ else (ALLOWED_WRITE_PATTERNS if operation == Operation.WRITE else [])
            is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
            if is_allowed:
                return AccessCheckResult(AccessDecision.ALLOW, f"Allowed: {pattern}", pattern)
            return AccessCheckResult(AccessDecision.DENY, "No matching pattern", None)

    guard = TestGuard()
    result = guard.check_access("/project/controller/UserController.java", Operation.READ, "java-architect")
    assert result.decision == AccessDecision.ALLOW, f"Expected ALLOW, got {result.decision}"
    assert "controller" in result.matched_pattern, f"Pattern should contain 'controller': {result.matched_pattern}"
    print("✓ test_allow_read_controller passed")

def test_allow_write_controller():
    """測試：允許寫入 Controller"""
    class TestGuard:
        def _check_pattern_match(self, file_path: str, patterns: list[str]) -> Tuple[bool, Optional[str]]:
            for pattern in patterns:
                if re.match(pattern, file_path):
                    return (True, pattern)
            return (False, None)

        def check_access(self, file_path: str, operation: Operation, agent_name: str) -> AccessCheckResult:
            normalized_path = normalize_path(file_path)
            is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
            if is_forbidden:
                return AccessCheckResult(AccessDecision.DENY, f"Forbidden: {pattern}", pattern)
            is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
            if is_audit:
                return AccessCheckResult(AccessDecision.AUDIT, f"Audit: {pattern}", pattern)
            patterns = ALLOWED_READ_PATTERNS if operation == Operation.READ else (ALLOWED_WRITE_PATTERNS if operation == Operation.WRITE else [])
            is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
            if is_allowed:
                return AccessCheckResult(AccessDecision.ALLOW, f"Allowed: {pattern}", pattern)
            return AccessCheckResult(AccessDecision.DENY, "No matching pattern", None)

    guard = TestGuard()
    result = guard.check_access("/project/controller/UserController.java", Operation.WRITE, "java-architect")
    assert result.decision == AccessDecision.ALLOW, f"Expected ALLOW, got {result.decision}"
    assert "controller" in result.matched_pattern, f"Pattern should contain 'controller': {result.matched_pattern}"
    print("✓ test_allow_write_controller passed")

def test_deny_read_prod_config():
    """測試：拒絕讀取生產配置"""
    class TestGuard:
        def _check_pattern_match(self, file_path: str, patterns: list[str]) -> Tuple[bool, Optional[str]]:
            for pattern in patterns:
                if re.match(pattern, file_path):
                    return (True, pattern)
            return (False, None)

        def check_access(self, file_path: str, operation: Operation, agent_name: str) -> AccessCheckResult:
            normalized_path = normalize_path(file_path)
            is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
            if is_forbidden:
                return AccessCheckResult(AccessDecision.DENY, f"Forbidden: {pattern}", pattern)
            is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
            if is_audit:
                return AccessCheckResult(AccessDecision.AUDIT, f"Audit: {pattern}", pattern)
            patterns = ALLOWED_READ_PATTERNS if operation == Operation.READ else (ALLOWED_WRITE_PATTERNS if operation == Operation.WRITE else [])
            is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
            if is_allowed:
                return AccessCheckResult(AccessDecision.ALLOW, f"Allowed: {pattern}", pattern)
            return AccessCheckResult(AccessDecision.DENY, "No matching pattern", None)

    guard = TestGuard()
    result = guard.check_access("/project/application-prod.yml", Operation.READ, "java-architect")
    assert result.decision == AccessDecision.DENY, f"Expected DENY, got {result.decision}"
    assert "application-prod" in result.matched_pattern, f"Pattern should contain 'application-prod': {result.matched_pattern}"
    print("✓ test_deny_read_prod_config passed")

def test_deny_write_security_config():
    """測試：拒絕寫入安全配置"""
    class TestGuard:
        def _check_pattern_match(self, file_path: str, patterns: list[str]) -> Tuple[bool, Optional[str]]:
            for pattern in patterns:
                if re.match(pattern, file_path):
                    return (True, pattern)
            return (False, None)

        def check_access(self, file_path: str, operation: Operation, agent_name: str) -> AccessCheckResult:
            normalized_path = normalize_path(file_path)
            is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
            if is_forbidden:
                return AccessCheckResult(AccessDecision.DENY, f"Forbidden: {pattern}", pattern)
            is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
            if is_audit:
                return AccessCheckResult(AccessDecision.AUDIT, f"Audit: {pattern}", pattern)
            patterns = ALLOWED_READ_PATTERNS if operation == Operation.READ else (ALLOWED_WRITE_PATTERNS if operation == Operation.WRITE else [])
            is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
            if is_allowed:
                return AccessCheckResult(AccessDecision.ALLOW, f"Allowed: {pattern}", pattern)
            return AccessCheckResult(AccessDecision.DENY, "No matching pattern", None)

    guard = TestGuard()
    result = guard.check_access("/project/SecurityConfig.java", Operation.WRITE, "java-architect")
    assert result.decision == AccessDecision.DENY, f"Expected DENY, got {result.decision}"
    assert "SecurityConfig" in result.matched_pattern, f"Pattern should contain 'SecurityConfig': {result.matched_pattern}"
    print("✓ test_deny_write_security_config passed")

def test_audit_write_foundation():
    """測試：審計寫入 foundation 層"""
    class TestGuard:
        def _check_pattern_match(self, file_path: str, patterns: list[str]) -> Tuple[bool, Optional[str]]:
            for pattern in patterns:
                if re.match(pattern, file_path):
                    return (True, pattern)
            return (False, None)

        def check_access(self, file_path: str, operation: Operation, agent_name: str) -> AccessCheckResult:
            normalized_path = normalize_path(file_path)
            is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
            if is_forbidden:
                return AccessCheckResult(AccessDecision.DENY, f"Forbidden: {pattern}", pattern)
            is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
            if is_audit:
                return AccessCheckResult(AccessDecision.AUDIT, f"Audit: {pattern}", pattern)
            patterns = ALLOWED_READ_PATTERNS if operation == Operation.READ else (ALLOWED_WRITE_PATTERNS if operation == Operation.WRITE else [])
            is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
            if is_allowed:
                return AccessCheckResult(AccessDecision.ALLOW, f"Allowed: {pattern}", pattern)
            return AccessCheckResult(AccessDecision.DENY, "No matching pattern", None)

    guard = TestGuard()
    result = guard.check_access("/project/foundation/SomethingImportant.java", Operation.WRITE, "java-architect")
    assert result.decision == AccessDecision.AUDIT, f"Expected AUDIT, got {result.decision}"
    assert "foundation" in result.matched_pattern, f"Pattern should contain 'foundation': {result.matched_pattern}"
    print("✓ test_audit_write_foundation passed")

def test_deny_write_migration():
    """測試：拒絕寫入數據庫遷移文件"""
    class TestGuard:
        def _check_pattern_match(self, file_path: str, patterns: list[str]) -> Tuple[bool, Optional[str]]:
            for pattern in patterns:
                if re.match(pattern, file_path):
                    return (True, pattern)
            return (False, None)

        def check_access(self, file_path: str, operation: Operation, agent_name: str) -> AccessCheckResult:
            normalized_path = normalize_path(file_path)
            is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
            if is_forbidden:
                return AccessCheckResult(AccessDecision.DENY, f"Forbidden: {pattern}", pattern)
            is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
            if is_audit:
                return AccessCheckResult(AccessDecision.AUDIT, f"Audit: {pattern}", pattern)
            patterns = ALLOWED_READ_PATTERNS if operation == Operation.READ else (ALLOWED_WRITE_PATTERNS if operation == Operation.WRITE else [])
            is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
            if is_allowed:
                return AccessCheckResult(AccessDecision.ALLOW, f"Allowed: {pattern}", pattern)
            return AccessCheckResult(AccessDecision.DENY, "No matching pattern", None)

    guard = TestGuard()
    result = guard.check_access("/project/db/migration/V001__init.sql", Operation.WRITE, "java-architect")
    assert result.decision == AccessDecision.DENY, f"Expected DENY, got {result.decision}"
    assert "migration" in result.matched_pattern, f"Pattern should contain 'migration': {result.matched_pattern}"
    print("✓ test_deny_write_migration passed")

def test_deny_unknown_file():
    """測試：拒絕未知文件訪問"""
    class TestGuard:
        def _check_pattern_match(self, file_path: str, patterns: list[str]) -> Tuple[bool, Optional[str]]:
            for pattern in patterns:
                if re.match(pattern, file_path):
                    return (True, pattern)
            return (False, None)

        def check_access(self, file_path: str, operation: Operation, agent_name: str) -> AccessCheckResult:
            normalized_path = normalize_path(file_path)
            is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
            if is_forbidden:
                return AccessCheckResult(AccessDecision.DENY, f"Forbidden: {pattern}", pattern)
            is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
            if is_audit:
                return AccessCheckResult(AccessDecision.AUDIT, f"Audit: {pattern}", pattern)
            patterns = ALLOWED_READ_PATTERNS if operation == Operation.READ else (ALLOWED_WRITE_PATTERNS if operation == Operation.WRITE else [])
            is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
            if is_allowed:
                return AccessCheckResult(AccessDecision.ALLOW, f"Allowed: {pattern}", pattern)
            return AccessCheckResult(AccessDecision.DENY, "No matching pattern", None)

    guard = TestGuard()
    result = guard.check_access("/random/unknown.txt", Operation.READ, "java-architect")
    assert result.decision == AccessDecision.DENY, f"Expected DENY, got {result.decision}"
    assert result.matched_pattern is None, f"Should have no matched pattern: {result.matched_pattern}"
    print("✓ test_deny_unknown_file passed")

# ============================================================================
# 主函數 (CLI 測試入口)
# ============================================================================

if __name__ == '__main__':
    print("FileAccessGuard - Test Runner")
    print("=" * 60)
    print()

    # 運行所有測試
    tests = [
        test_allow_read_controller,
        test_allow_write_controller,
        test_deny_read_prod_config,
        test_deny_write_security_config,
        test_audit_write_foundation,
        test_deny_write_migration,
        test_deny_unknown_file
    ]

    print(f"Running {len(tests)} tests...")
    print()

    passed = 0
    failed = 0

    for test_func in tests:
        try:
            test_func()
            passed += 1
        except AssertionError as e:
            print(f"✗ {test_func.__name__} FAILED: {e}")
            failed += 1
        except Exception as e:
            print(f"✗ {test_func.__name__} ERROR: {e}")
            failed += 1

    print()
    print("=" * 60)
    print(f"Test Results: {passed} passed, {failed} failed")
    print("=" * 60)

    if failed > 0:
        exit(1)
