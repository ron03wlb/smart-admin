# Security Gaps Analysis - Automation System

**版本**: v1.0.0
**日期**: 2026-01-27
**嚴重性**: P1 - Important（3 個漏洞）
**影響**: High to Medium

---

## 📋 執行摘要

經過對 automation/clawdbot 系統的深度安全審查（3,017 行 Python 代碼），識別出 **3 個 P1 級別安全漏洞**。這些漏洞可能導致任意文件訪問、資源耗盡和功能靜默失效。

### 漏洞概覽

| 漏洞 ID | 名稱 | 嚴重性 | CVSS 分數 | 利用難度 | 影響範圍 | 修復難度 |
|---------|------|--------|----------|---------|---------|---------|
| **#1** | 路徑遍歷攻擊 | High | 7.5 | Medium | 任意文件訪問 | Low（1-2 天）|
| **#2** | 數據庫連接泄漏 | Medium | 5.3 | Low | 資源耗盡 | Low（1 天）|
| **#3** | pg_stat_statements 依賴未驗證 | Medium | 4.0 | Low | 功能靜默失效 | Low（0.5 天）|

**總體風險評估**: 🟡 **MEDIUM-HIGH**
- 建議在生產環境部署前修復所有 3 個漏洞
- 路徑遍歷攻擊（#1）為最高優先級（High severity）

---

## 🔴 漏洞 #1: 路徑遍歷攻擊

### 基本信息

| 屬性 | 值 |
|------|-----|
| **嚴重性** | High |
| **CVSS v3.1 分數** | 7.5 (High) |
| **CVSS 向量** | CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N |
| **CWE 編號** | CWE-22: Improper Limitation of a Pathname to a Restricted Directory ('Path Traversal') |
| **位置** | `automation/clawdbot/tools/file_access_guard.py:297-318` |
| **受影響組件** | FileAccessGuard.normalize_path() |
| **利用難度** | Medium（需要知道項目結構）|
| **修復難度** | Low（1-2 天）|

---

### 漏洞位置

**文件**: `automation/clawdbot/tools/file_access_guard.py`
**行號**: 297-318
**方法**: `normalize_path(file_path: str) -> str`

**當前實現（脆弱）**:
```python
def normalize_path(file_path: str) -> str:
    """
    規範化文件路徑

    將路徑轉換為統一格式：
    - 使用正斜杠 /
    - 移除雙斜杠 //
    - 解析相對路徑 . 和 ..

    Args:
        file_path: 原始文件路徑

    Returns:
        規範化後的路徑
    """
    # ❌ 問題：os.path.normpath() 會處理 .. 但不檢查最終路徑是否在允許範圍內
    normalized = os.path.normpath(file_path)

    # 統一使用正斜杠
    normalized = normalized.replace('\\', '/')

    # 移除雙斜杠
    while '//' in normalized:
        normalized = normalized.replace('//', '/')

    return normalized
```

---

### 技術說明

**問題根源**:
1. `os.path.normpath()` **只處理路徑語法**（如 `..`, `.`），不檢查路徑是否越界
2. 規範化後的路徑**不驗證是否仍在 PROJECT_ROOT 內**
3. 黑名單檢查**可能在規範化前**完成，導致繞過

**為什麼不安全**:
```python
# 示例 1: 基本路徑遍歷
original_path = "/allowed/module/../../../etc/passwd"
normalized = os.path.normpath(original_path)
print(normalized)  # 輸出: "/etc/passwd"
# 即使 /etc/passwd 在黑名單中，但黑名單檢查可能在規範化前進行

# 示例 2: Windows 風格路徑
original_path = "C:\\allowed\\module\\..\\..\\..\\Windows\\System32\\config\\SAM"
normalized = os.path.normpath(original_path).replace('\\', '/')
print(normalized)  # 輸出: "C:/Windows/System32/config/SAM"

# 示例 3: 相對路徑混合
original_path = "../../../../../../root/.ssh/id_rsa"
normalized = os.path.normpath(os.path.join(PROJECT_ROOT, original_path))
print(normalized)  # 可能輸出: "/root/.ssh/id_rsa"（取決於 PROJECT_ROOT 深度）
```

---

### 攻擊場景

#### 場景 A: 訪問系統敏感文件

**目標**: 讀取 `/etc/passwd` 獲取用戶列表

**步驟**:
1. 攻擊者知道項目位於 `/home/smartadmin/project`
2. 構造惡意路徑: `/home/smartadmin/project/sa-admin/../../../etc/passwd`
3. `normalize_path()` 規範化為: `/etc/passwd`
4. 如果黑名單檢查在規範化前，可能被繞過
5. 成功訪問 `/etc/passwd`

**Payload**:
```python
malicious_paths = [
    "/allowed/sa-admin/../../../etc/passwd",
    "/allowed/sa-admin/../../../etc/shadow",
    "/allowed/sa-admin/../../../root/.bashrc",
]
```

---

#### 場景 B: 訪問 SSH 私鑰

**目標**: 竊取用戶的 SSH 私鑰進行橫向移動

**步驟**:
1. 攻擊者推測用戶主目錄: `/home/developer`
2. 構造路徑: `../../../../../../home/developer/.ssh/id_rsa`
3. 規範化後可能訪問到私鑰文件
4. 獲取私鑰後可以 SSH 到其他服務器

**Payload**:
```python
ssh_key_paths = [
    "../../../../../../home/developer/.ssh/id_rsa",
    "../../../../../../home/admin/.ssh/id_rsa",
    "../../../../../../root/.ssh/id_rsa",
]
```

---

#### 場景 C: 訪問應用程序配置文件

**目標**: 讀取 `application.yml` 獲取數據庫密碼

**步驟**:
1. 攻擊者知道 Spring Boot 配置位於項目內
2. 但通過路徑遍歷訪問其他模塊的配置
3. 構造路徑: `/allowed/sa-admin/../sa-base/src/main/resources/application.yml`
4. 獲取數據庫連接字符串和密碼

**Payload**:
```python
config_paths = [
    "/allowed/sa-admin/../sa-base/src/main/resources/application.yml",
    "/allowed/sa-admin/../sa-base/src/main/resources/application-prod.yml",
    "/allowed/sa-admin/../../.env",
]
```

---

### 影響評估

**機密性影響**: **HIGH**
- 可能訪問: `/etc/passwd`, `/etc/shadow`, `~/.ssh/id_rsa`
- 可能讀取: 數據庫密碼、API 密鑰、OAuth 憑證

**完整性影響**: **NONE**
- 當前僅為讀取操作，不涉及寫入
- 但如果 `write_file()` 也存在類似問題，完整性影響為 HIGH

**可用性影響**: **NONE**
- 不直接影響系統可用性

**攻擊複雜度**: **MEDIUM**
- 需要知道項目目錄結構
- 需要了解目標文件位置
- 不需要身份驗證（如果 Agent 已通過其他驗證）

**潛在後果**:
1. **數據洩露**: 敏感文件被讀取（配置、密鑰、密碼）
2. **橫向移動**: 獲取 SSH 私鑰後訪問其他服務器
3. **權限提升**: 讀取 `/etc/shadow` 後破解密碼
4. **完整系統妥協**: 結合其他漏洞可能獲得完整系統控制

---

### 推薦修復方案

#### 方案 A: 安全的路徑規範化（推薦）

**實現**:
```python
import os
from pathlib import Path
import logging

logger = logging.getLogger(__name__)

# 項目根目錄（從環境變量讀取）
PROJECT_ROOT = os.getenv('PROJECT_ROOT', os.getcwd())

class SecurityError(Exception):
    """安全錯誤異常"""
    pass

def normalize_path(file_path: str) -> str:
    """
    安全的路徑規範化，防止路徑遍歷攻擊

    關鍵安全措施:
    1. 轉換為絕對路徑
    2. 解析符號鏈接和相對路徑
    3. 驗證路徑在項目根目錄內（關鍵！）

    Args:
        file_path: 原始文件路徑

    Returns:
        規範化後的安全路徑

    Raises:
        SecurityError: 如果檢測到路徑遍歷攻擊
    """
    try:
        # 1. 轉換為絕對路徑
        if not os.path.isabs(file_path):
            file_path = os.path.join(PROJECT_ROOT, file_path)

        # 2. 解析符號鏈接和相對路徑（處理 .., ., symlink）
        resolved_path = Path(file_path).resolve()
        project_root_path = Path(PROJECT_ROOT).resolve()

        # 3. 關鍵檢查：確保路徑在項目根目錄內
        try:
            # 如果 resolved_path 不在 project_root_path 下，會拋出 ValueError
            resolved_path.relative_to(project_root_path)
        except ValueError as e:
            # 檢測到路徑遍歷攻擊！
            logger.error(
                f"Path traversal attack detected! "
                f"Requested path: {file_path}, "
                f"Resolved to: {resolved_path}, "
                f"Project root: {project_root_path}"
            )
            raise SecurityError(
                f"Path traversal detected: {file_path} "
                f"resolves to {resolved_path} outside project root {project_root_path}"
            ) from e

        # 4. 統一使用正斜杠（跨平台兼容）
        normalized = str(resolved_path).replace('\\', '/')

        logger.debug(f"Path normalized: {file_path} -> {normalized}")
        return normalized

    except (OSError, RuntimeError) as e:
        # 文件不存在、權限問題等
        logger.error(f"Path normalization error: {e}")
        raise SecurityError(f"Invalid path: {file_path}") from e
```

**關鍵改進點**:
1. ✅ 使用 `Path.resolve()` 解析符號鏈接和相對路徑
2. ✅ 使用 `relative_to()` 驗證路徑在項目根目錄內
3. ✅ 路徑遍歷攻擊會拋出 `SecurityError` 異常（不是靜默失敗）
4. ✅ 詳細的安全日誌記錄

---

#### 方案 B: 白名單前綴驗證（額外防護）

如果路徑規範化還不夠，可以添加白名單前綴驗證：

```python
# 允許的路徑前綴
ALLOWED_PREFIXES = [
    f"{PROJECT_ROOT}/smart-admin-api-java21-springboot3",
    f"{PROJECT_ROOT}/smart-admin-web-vue3",
    f"{PROJECT_ROOT}/docs",
]

def normalize_path(file_path: str) -> str:
    """安全的路徑規範化 + 白名單驗證"""
    # ... 方案 A 的代碼 ...

    # 額外檢查：路徑必須以允許的前綴開頭
    normalized_path_str = str(resolved_path)
    if not any(normalized_path_str.startswith(prefix) for prefix in ALLOWED_PREFIXES):
        logger.error(f"Path not in whitelist: {normalized_path_str}")
        raise SecurityError(
            f"Path {file_path} not in allowed directories. "
            f"Allowed: {ALLOWED_PREFIXES}"
        )

    return normalized
```

---

### 測試用例

#### 單元測試（pytest）

```python
import pytest
from automation.clawdbot.tools.file_access_guard import normalize_path, SecurityError

# 測試數據
PROJECT_ROOT = "/home/smartadmin/project"

def test_path_traversal_prevention_basic():
    """測試基本路徑遍歷防護"""
    attack_vectors = [
        "/allowed/path/../../../etc/passwd",
        "../../secret/config.yml",
        "/project/../forbidden/file.txt",
        "allowed/path/../../../etc/shadow",
        "/tmp/../../../root/.ssh/id_rsa",
    ]

    for malicious_path in attack_vectors:
        with pytest.raises(SecurityError, match="Path traversal detected"):
            normalize_path(malicious_path)

def test_path_traversal_prevention_windows():
    """測試 Windows 路徑遍歷"""
    attack_vectors = [
        "C:\\allowed\\..\\..\\..\\Windows\\System32\\config\\SAM",
        "..\\..\\..\\..\\..\\Users\\Admin\\Desktop",
    ]

    for malicious_path in attack_vectors:
        with pytest.raises(SecurityError):
            normalize_path(malicious_path)

def test_path_traversal_prevention_symlink():
    """測試符號鏈接繞過"""
    # 假設攻擊者創建了 symlink: /allowed/link -> /etc
    symlink_attacks = [
        "/allowed/link/passwd",  # 實際指向 /etc/passwd
        "/allowed/link/shadow",
    ]

    # 如果 symlink 指向項目外，應該被阻止
    for symlink_path in symlink_attacks:
        with pytest.raises(SecurityError):
            normalize_path(symlink_path)

def test_allowed_paths():
    """測試允許的路徑不會被錯誤阻止"""
    allowed_paths = [
        f"{PROJECT_ROOT}/sa-admin/src/main/java/net/lab1024/sa/admin",
        f"{PROJECT_ROOT}/sa-base/src/main/resources/application.yml",
        "sa-admin/pom.xml",  # 相對路徑
    ]

    for allowed_path in allowed_paths:
        try:
            result = normalize_path(allowed_path)
            assert result.startswith(PROJECT_ROOT)
        except SecurityError:
            pytest.fail(f"Allowed path was blocked: {allowed_path}")

def test_edge_cases():
    """測試邊緣情況"""
    edge_cases = [
        (".", f"{PROJECT_ROOT}"),  # 當前目錄
        ("..", "Should raise SecurityError"),  # 上級目錄
        ("", "Should raise SecurityError"),  # 空路徑
        ("/etc/passwd", "Should raise SecurityError"),  # 絕對路徑（項目外）
    ]

    # 當前目錄允許
    assert normalize_path(".") == PROJECT_ROOT

    # 其他應該被阻止
    with pytest.raises(SecurityError):
        normalize_path("..")

    with pytest.raises(SecurityError):
        normalize_path("/etc/passwd")
```

---

#### 集成測試

```python
import os
import tempfile
import pytest
from automation.clawdbot.tools.file_access_guard import FileAccessGuard, Operation

def test_file_access_guard_with_path_traversal():
    """測試 FileAccessGuard 阻止路徑遍歷攻擊"""
    # 創建臨時項目目錄
    with tempfile.TemporaryDirectory() as tmpdir:
        os.environ['PROJECT_ROOT'] = tmpdir

        # 創建允許的目錄
        allowed_dir = os.path.join(tmpdir, "allowed")
        os.makedirs(allowed_dir)

        # 創建 FileAccessGuard 實例
        guard = FileAccessGuard(db_connection_string="...")

        # 嘗試路徑遍歷攻擊
        malicious_path = f"{allowed_dir}/../../../etc/passwd"

        result = guard.check_access(
            file_path=malicious_path,
            operation=Operation.READ,
            agent_name="TestAgent"
        )

        # 應該被拒絕
        assert result.decision == AccessDecision.DENY
        assert "Path traversal" in result.reason
```

---

### 驗證修復

**步驟 1: 應用修復**
```bash
cd automation/clawdbot/tools
# 備份當前文件
cp file_access_guard.py file_access_guard.py.bak

# 應用安全版本的 normalize_path()
# （手動編輯或使用 patch）
```

**步驟 2: 運行測試**
```bash
# 運行單元測試
python3 -m pytest tests/test_path_traversal.py -v

# 預期: 所有測試通過
# - test_path_traversal_prevention_basic PASSED
# - test_path_traversal_prevention_windows PASSED
# - test_path_traversal_prevention_symlink PASSED
# - test_allowed_paths PASSED
# - test_edge_cases PASSED
```

**步驟 3: 手動驗證**
```python
# 在 Python REPL 中測試
>>> from automation.clawdbot.tools.file_access_guard import normalize_path, SecurityError
>>> normalize_path("/allowed/../../../etc/passwd")
SecurityError: Path traversal detected: /allowed/../../../etc/passwd resolves to /etc/passwd outside project root /home/smartadmin/project
```

**步驟 4: 審計日誌檢查**
```sql
-- 檢查是否有路徑遍歷攻擊被記錄
SELECT * FROM t_ai_operation_audit
WHERE operation_type = 'file_access'
  AND status = 'FAILED'
  AND error_message LIKE '%Path traversal%'
ORDER BY created_at DESC
LIMIT 10;
```

---

## 🟡 漏洞 #2: 數據庫連接泄漏

### 基本信息

| 屬性 | 值 |
|------|-----|
| **嚴重性** | Medium |
| **CVSS v3.1 分數** | 5.3 (Medium) |
| **CVSS 向量** | CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L |
| **CWE 編號** | CWE-404: Improper Resource Shutdown or Release |
| **位置** | `automation/clawdbot/crews/common/tools.py:323-324` |
| **受影響組件** | DatabaseQueryTool.__init__() |
| **利用難度** | Low（長時間運行自然觸發）|
| **修復難度** | Low（1 天）|

---

### 漏洞位置

**文件**: `automation/clawdbot/crews/common/tools.py`
**行號**: 323-324, 396
**類**: `DatabaseQueryTool`

**當前實現（脆弱）**:
```python
class DatabaseQueryTool:
    """數據庫查詢工具"""

    def __init__(self, db_connection_string: str):
        """
        初始化數據庫查詢工具

        Args:
            db_connection_string: PostgreSQL 連接字符串
        """
        import psycopg2

        # ❌ 問題 1: 每個實例創建獨立連接（不使用連接池）
        self.conn = psycopg2.connect(db_connection_string)

    def close(self):
        """關閉數據庫連接"""
        # ❌ 問題 2: 需要手動調用 close()，異常情況下不會自動清理
        if self.conn:
            self.conn.close()

    # ❌ 問題 3: 沒有 __enter__ 和 __exit__，無法作為 context manager
```

**對比：FileAccessGuard 和 BaseCrew 已經使用連接池**
```python
# file_access_guard.py:96-106 - 正確使用連接池 ✅
self.connection_pool = pool.SimpleConnectionPool(
    minconn=1,
    maxconn=10,
    dsn=db_connection_string
)

# base_crew.py:75-86 - 正確使用連接池 ✅
self.connection_pool = pool.ThreadedConnectionPool(
    minconn=1,
    maxconn=5,
    dsn=DB_CONNECTION_STRING
)
```

---

### 技術說明

**為什麼會泄漏**:
1. **每個實例獨立連接**: 每次創建 `DatabaseQueryTool` 都會打開新連接
2. **手動關閉依賴**: `close()` 需要手動調用，異常時可能被跳過
3. **無 context manager**: 不支持 `with` 語句自動清理
4. **長時間運行**: QA Crew 掃描大型代碼庫時會長時間持有連接

**示例問題代碼**:
```python
# 問題場景 1: 異常導致連接未關閉
def analyze_database():
    tool = DatabaseQueryTool(DB_URL)
    try:
        results = tool.check_slow_queries()
        # ... 處理結果
    except Exception as e:
        logger.error(f"Error: {e}")
        # ❌ 異常時 tool.close() 不會被調用
        return None
    # ❌ 正常路徑也可能忘記調用 close()

# 問題場景 2: 多次創建實例
for module in modules:
    # ❌ 每次迭代創建新連接，可能導致連接池耗盡
    tool = DatabaseQueryTool(DB_URL)
    tool.analyze_query(module.query)
    # 忘記調用 close()

# 問題場景 3: 長時間持有連接
class QACrew:
    def __init__(self):
        # ❌ 在構造函數中創建連接，整個 Crew 生命週期持有
        self.db_tool = DatabaseQueryTool(DB_URL)

    def run(self, pr_number: int):
        # 長時間運行（可能數小時）
        # 連接一直被占用
        pass
```

---

### 影響評估

**可用性影響**: **LOW**
- PostgreSQL 連接池耗盡（默認 100 連接）
- 影響其他服務的數據庫訪問
- 系統響應變慢或拒絕新連接

**攻擊場景**: **被動觸發**
- 不需要主動攻擊，長時間運行自然觸發
- QA Crew 掃描大型代碼庫（數小時）
- 多個 Crew 並發運行

**實際影響案例**:
```sql
-- 查看當前連接數
SELECT count(*) FROM pg_stat_activity WHERE datname = 'smart_admin';
-- 正常: 10-20 連接
-- 泄漏後: 80-100 連接（接近上限）

-- 查看長時間空閒連接
SELECT pid, usename, application_name, state, state_change
FROM pg_stat_activity
WHERE state = 'idle'
  AND state_change < NOW() - INTERVAL '1 hour'
ORDER BY state_change;
-- 可能看到數小時前的 idle 連接
```

---

### 推薦修復方案

#### 方案 A: 使用共享連接池（推薦）

**實現**:
```python
# automation/clawdbot/crews/common/base_crew.py

from psycopg2 import pool
from contextlib import contextmanager

class BaseCrew(ABC):
    def __init__(self, crew_name: str, crew_type: str):
        self.crew_name = crew_name
        self.crew_type = crew_type

        # ✅ 創建共享連接池
        self.connection_pool = self._create_connection_pool()

    def _create_connection_pool(self):
        """創建數據庫連接池"""
        try:
            return pool.ThreadedConnectionPool(
                minconn=1,
                maxconn=10,
                dsn=DB_CONNECTION_STRING,
                # 連接池配置
                connect_timeout=10,
                options="-c statement_timeout=60000",  # 60 秒超時
            )
        except psycopg2.Error as e:
            logger.error(f"Failed to create connection pool: {e}")
            return None

    @contextmanager
    def get_db_connection(self):
        """
        獲取數據庫連接（Context Manager）

        使用方式:
        >>> with self.get_db_connection() as conn:
        >>>     cursor = conn.cursor()
        >>>     cursor.execute("SELECT ...")
        """
        conn = None
        try:
            # 從連接池獲取連接
            conn = self.connection_pool.getconn()
            yield conn
            # 成功時提交事務
            conn.commit()
        except Exception as e:
            # 異常時回滾
            if conn:
                conn.rollback()
            raise
        finally:
            # ✅ 關鍵：無論如何都歸還連接到池
            if conn:
                self.connection_pool.putconn(conn)


# automation/clawdbot/crews/common/tools.py（修改）

class DatabaseQueryTool:
    """數據庫查詢工具（使用共享連接池）"""

    def __init__(self, connection_pool):
        """
        初始化數據庫查詢工具

        Args:
            connection_pool: 共享連接池（來自 BaseCrew）
        """
        # ✅ 使用共享連接池，而非創建新連接
        self.connection_pool = connection_pool
        self.pg_stat_statements_enabled = False
        self._check_extensions()

    def _check_extensions(self):
        """檢查必需的 PostgreSQL 擴展"""
        try:
            # ✅ 使用 context manager，自動歸還連接
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT name, installed_version
                    FROM pg_available_extensions
                    WHERE name = 'pg_stat_statements'
                """)
                result = cursor.fetchone()
                self.pg_stat_statements_enabled = bool(result and result[1])
                cursor.close()
        except Exception as e:
            logger.error(f"Failed to check extensions: {e}")
            self.pg_stat_statements_enabled = False

    @contextmanager
    def _get_connection(self):
        """內部使用的連接獲取（Context Manager）"""
        conn = None
        try:
            conn = self.connection_pool.getconn()
            yield conn
            conn.commit()
        except Exception as e:
            if conn:
                conn.rollback()
            raise
        finally:
            if conn:
                self.connection_pool.putconn(conn)

    def check_slow_queries(self, min_duration_ms: int = 1000) -> List[Dict[str, Any]]:
        """檢查慢查詢"""
        if not self.pg_stat_statements_enabled:
            logger.warning("pg_stat_statements not enabled")
            return []

        # ✅ 使用 context manager，自動清理
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT query, calls, mean_exec_time, max_exec_time
                FROM pg_stat_statements
                WHERE mean_exec_time > %s
                ORDER BY mean_exec_time DESC
                LIMIT 10
            """, (min_duration_ms,))

            results = cursor.fetchall()
            cursor.close()

            return [
                {
                    "query": row[0],
                    "calls": row[1],
                    "mean_exec_time": row[2],
                    "max_exec_time": row[3],
                }
                for row in results
            ]

    # ✅ 不再需要 close() 方法


# 使用方式（在 Crew 中）
class AnalyzerCrew(BaseCrew):
    def run(self, target_module: str):
        # ✅ 傳入共享連接池
        db_tool = DatabaseQueryTool(self.connection_pool)
        slow_queries = db_tool.check_slow_queries()
        # 不需要手動 close()，連接自動歸還
```

---

### 測試用例

#### 連接池測試

```python
import pytest
from psycopg2 import pool
from automation.clawdbot.crews.common.tools import DatabaseQueryTool

def test_connection_pool_no_leak():
    """測試連接池不會泄漏"""
    # 創建小型連接池
    conn_pool = pool.SimpleConnectionPool(
        minconn=1,
        maxconn=5,
        dsn="postgresql://localhost/test"
    )

    # 模擬 100 次操作
    for i in range(100):
        tool = DatabaseQueryTool(conn_pool)
        # 執行查詢
        slow_queries = tool.check_slow_queries()
        # ✅ 不需要手動 close()，連接自動歸還

    # 驗證：所有連接已歸還
    # pool._used 應該為 0（所有連接都在 pool._pool 中）
    assert conn_pool._used == 0, f"Connection leak detected: {conn_pool._used} connections not returned"

    # 清理
    conn_pool.closeall()

def test_exception_returns_connection():
    """測試異常情況下連接仍被歸還"""
    conn_pool = pool.SimpleConnectionPool(minconn=1, maxconn=5, dsn="...")

    tool = DatabaseQueryTool(conn_pool)

    try:
        # 執行會失敗的查詢
        tool.check_slow_queries()
    except Exception:
        pass  # 忽略異常

    # 驗證：連接已歸還
    assert conn_pool._used == 0

    conn_pool.closeall()

def test_concurrent_access():
    """測試並發訪問不會超過最大連接數"""
    import threading

    conn_pool = pool.ThreadedConnectionPool(minconn=1, maxconn=5, dsn="...")

    def worker():
        tool = DatabaseQueryTool(conn_pool)
        tool.check_slow_queries()

    # 啟動 10 個並發線程（超過連接池大小）
    threads = [threading.Thread(target=worker) for _ in range(10)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    # 驗證：連接數不超過最大值
    assert conn_pool._used == 0
    assert len(conn_pool._pool) <= 5

    conn_pool.closeall()
```

---

### 驗證修復

**步驟 1: 應用修復**
```bash
cd automation/clawdbot/crews/common
# 備份
cp tools.py tools.py.bak
cp base_crew.py base_crew.py.bak

# 應用修復（編輯文件）
# 1. 在 base_crew.py 中添加 get_db_connection() 方法
# 2. 修改 tools.py 中的 DatabaseQueryTool
```

**步驟 2: 運行測試**
```bash
python3 -m pytest tests/test_connection_pool.py -v

# 預期輸出:
# test_connection_pool_no_leak PASSED
# test_exception_returns_connection PASSED
# test_concurrent_access PASSED
```

**步驟 3: 監控連接池**
```sql
-- 部署後監控連接數
SELECT count(*) as total_connections,
       count(*) FILTER (WHERE state = 'active') as active,
       count(*) FILTER (WHERE state = 'idle') as idle
FROM pg_stat_activity
WHERE datname = 'smart_admin';

-- 預期：連接數保持穩定（不會無限增長）
```

---

## 🟡 漏洞 #3: pg_stat_statements 擴展依賴未驗證

### 基本信息

| 屬性 | 值 |
|------|-----|
| **嚴重性** | Medium |
| **CVSS v3.1 分數** | 4.0 (Medium) |
| **CVSS 向量** | CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L |
| **CWE 編號** | CWE-390: Detection of Error Condition Without Action |
| **位置** | `automation/clawdbot/crews/common/tools.py:368-377` |
| **受影響組件** | DatabaseQueryTool.check_slow_queries() |
| **利用難度** | Low（自動觸發）|
| **修復難度** | Low（0.5 天）|

---

### 漏洞位置

**文件**: `automation/clawdbot/crews/common/tools.py`
**行號**: 368-377, 392
**方法**: `check_slow_queries()`

**當前實現（脆弱）**:
```python
def check_slow_queries(self, min_duration_ms: int = 1000) -> List[Dict[str, Any]]:
    """
    檢查慢查詢

    Args:
        min_duration_ms: 最小執行時間（毫秒）

    Returns:
        慢查詢列表
    """
    try:
        cursor = self.conn.cursor()

        # ❌ 問題：假設 pg_stat_statements 擴展已啟用
        query = """
        SELECT query, calls, mean_exec_time, max_exec_time
        FROM pg_stat_statements
        WHERE mean_exec_time > %s
        ORDER BY mean_exec_time DESC
        LIMIT 10
        """

        cursor.execute(query, (min_duration_ms,))
        results = cursor.fetchall()
        cursor.close()

        return [...]

    except Exception as e:
        # ❌ 問題：異常被靜默處理，用戶不知道功能失效
        logger.error(f"Error checking slow queries: {e}")
        return []  # 返回空列表，假裝沒有慢查詢
```

---

### 技術說明

**pg_stat_statements 擴展說明**:
- PostgreSQL 官方擴展，用於追蹤 SQL 執行統計
- **不是默認啟用**，需要手動配置:
  ```sql
  -- 1. 安裝擴展
  CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

  -- 2. 修改 postgresql.conf
  shared_preload_libraries = 'pg_stat_statements'
  pg_stat_statements.track = all

  -- 3. 重啟 PostgreSQL
  sudo systemctl restart postgresql
  ```

**問題**:
1. **靜默失敗**: 如果擴展未啟用，查詢失敗但返回空列表 `[]`
2. **誤導用戶**: 用戶以為沒有慢查詢，實際是檢測功能失效
3. **審計日誌誤導**: 日誌顯示 SUCCESS，但實際無數據

---

### 影響評估

**功能影響**: **MEDIUM**
- 慢查詢檢測功能靜默失效
- 性能問題無法被及時發現
- 影響 Analyzer Crew 和 QA Crew 的質量檢查

**用戶體驗影響**:
```python
# 場景: 用戶運行 Analyzer Crew
>>> crew.run(target_module="sa-admin")
{
    "status": "SUCCESS",
    "results": {
        "slow_queries": [],  # ❌ 空列表，但實際有慢查詢
        ...
    }
}

# 用戶誤以為沒有性能問題，實際上檢測功能失效
```

**實際案例**:
```sql
-- 假設數據庫有很多慢查詢
SELECT query, mean_exec_time
FROM pg_stat_statements
WHERE mean_exec_time > 1000
ORDER BY mean_exec_time DESC
LIMIT 10;

-- 如果擴展未啟用，會報錯：
-- ERROR:  relation "pg_stat_statements" does not exist

-- 但 Python 代碼會捕獲異常並返回 []，用戶不知道
```

---

### 推薦修復方案

#### 方案 A: 啟動時檢查擴展（推薦）

**實現**:
```python
class DatabaseQueryTool:
    """數據庫查詢工具"""

    def __init__(self, connection_pool):
        self.connection_pool = connection_pool

        # ✅ 啟動時檢查擴展
        self.pg_stat_statements_enabled = False
        self._check_extensions()

    def _check_extensions(self):
        """
        檢查必需的 PostgreSQL 擴展

        在初始化時檢查，如果擴展未啟用，記錄警告並提供啟用指南
        """
        try:
            with self._get_connection() as conn:
                cursor = conn.cursor()

                # 檢查擴展是否可用
                cursor.execute("""
                    SELECT name, installed_version, comment
                    FROM pg_available_extensions
                    WHERE name = 'pg_stat_statements'
                """)
                result = cursor.fetchone()

                if result and result[1]:
                    # 擴展已安裝並啟用
                    logger.info(f"✅ pg_stat_statements enabled (version {result[1]})")
                    self.pg_stat_statements_enabled = True
                else:
                    # 擴展未啟用
                    logger.warning(
                        "⚠️ pg_stat_statements NOT enabled. "
                        "Slow query detection will be unavailable."
                    )
                    logger.info(
                        "To enable pg_stat_statements:\n"
                        "1. Run: CREATE EXTENSION IF NOT EXISTS pg_stat_statements;\n"
                        "2. Add to postgresql.conf: shared_preload_libraries = 'pg_stat_statements'\n"
                        "3. Restart PostgreSQL: sudo systemctl restart postgresql\n"
                        "4. Verify: SELECT * FROM pg_stat_statements LIMIT 1;"
                    )
                    self.pg_stat_statements_enabled = False

                cursor.close()

        except Exception as e:
            logger.error(f"❌ Failed to check extensions: {e}")
            self.pg_stat_statements_enabled = False

    def check_slow_queries(self, min_duration_ms: int = 1000) -> List[Dict[str, Any]]:
        """
        檢查慢查詢（帶擴展檢查）

        Returns:
            慢查詢列表。如果擴展未啟用，返回空列表並記錄警告
        """
        # ✅ 檢查擴展狀態
        if not self.pg_stat_statements_enabled:
            logger.warning(
                "Slow query detection unavailable: pg_stat_statements not enabled. "
                "Returning empty result. See initialization logs for setup instructions."
            )
            return []

        # 原有的查詢邏輯...
        try:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT query, calls, mean_exec_time, max_exec_time
                    FROM pg_stat_statements
                    WHERE mean_exec_time > %s
                    ORDER BY mean_exec_time DESC
                    LIMIT 10
                """, (min_duration_ms,))

                results = cursor.fetchall()
                cursor.close()

                logger.info(f"Found {len(results)} slow queries (>{min_duration_ms}ms)")

                return [
                    {
                        "query": row[0],
                        "calls": row[1],
                        "mean_exec_time": row[2],
                        "max_exec_time": row[3],
                    }
                    for row in results
                ]

        except Exception as e:
            logger.error(f"Error checking slow queries: {e}")
            return []
```

---

#### 方案 B: 提供備用檢測方案

如果擴展未啟用，可以解析 PostgreSQL 日誌文件作為備用：

```python
def check_slow_queries(self, min_duration_ms: int = 1000) -> List[Dict[str, Any]]:
    """檢查慢查詢（帶備用方案）"""

    # 主要方案：pg_stat_statements
    if self.pg_stat_statements_enabled:
        return self._check_slow_queries_from_extension(min_duration_ms)

    # 備用方案：解析 PostgreSQL 日誌
    logger.info("Using fallback: parsing PostgreSQL log files")
    return self._check_slow_queries_from_logs(min_duration_ms)

def _check_slow_queries_from_logs(self, min_duration_ms: int) -> List[Dict[str, Any]]:
    """
    從 PostgreSQL 日誌文件解析慢查詢（備用方案）

    前提：postgresql.conf 需要配置:
    log_min_duration_statement = 1000  # 記錄超過 1 秒的查詢
    """
    log_file = os.getenv('POSTGRESQL_LOG_FILE', '/var/log/postgresql/postgresql.log')

    try:
        slow_queries = []
        with open(log_file, 'r') as f:
            for line in f:
                if 'duration:' in line:
                    # 解析日誌行
                    # 示例: 2026-01-27 10:30:45 UTC LOG:  duration: 1234.567 ms  statement: SELECT ...
                    match = re.search(r'duration: ([\d.]+) ms  statement: (.+)', line)
                    if match:
                        duration = float(match.group(1))
                        query = match.group(2).strip()

                        if duration >= min_duration_ms:
                            slow_queries.append({
                                "query": query,
                                "duration_ms": duration,
                                "source": "log_file"
                            })

        logger.info(f"Found {len(slow_queries)} slow queries from log file")
        return slow_queries

    except FileNotFoundError:
        logger.error(f"PostgreSQL log file not found: {log_file}")
        return []
    except Exception as e:
        logger.error(f"Error parsing log file: {e}")
        return []
```

---

### 測試用例

```python
import pytest
from automation.clawdbot.crews.common.tools import DatabaseQueryTool

def test_extension_enabled(postgresql_with_extension):
    """測試擴展已啟用時的正常功能"""
    tool = DatabaseQueryTool(postgresql_with_extension.get_pool())

    # 應該成功初始化
    assert tool.pg_stat_statements_enabled is True

    # 應該能查詢慢查詢
    slow_queries = tool.check_slow_queries()
    assert isinstance(slow_queries, list)

def test_extension_not_enabled(postgresql_without_extension):
    """測試擴展未啟用時的行為"""
    tool = DatabaseQueryTool(postgresql_without_extension.get_pool())

    # 應該檢測到擴展未啟用
    assert tool.pg_stat_statements_enabled is False

    # 應該返回空列表並記錄警告
    with pytest.warns(UserWarning, match="pg_stat_statements not enabled"):
        slow_queries = tool.check_slow_queries()

    assert slow_queries == []

def test_extension_check_logs_warning(caplog, postgresql_without_extension):
    """測試警告日誌正確記錄"""
    import logging
    caplog.set_level(logging.WARNING)

    tool = DatabaseQueryTool(postgresql_without_extension.get_pool())

    # 檢查警告日誌
    assert "pg_stat_statements NOT enabled" in caplog.text
    assert "To enable pg_stat_statements:" in caplog.text
    assert "CREATE EXTENSION" in caplog.text
```

---

### 驗證修復

**步驟 1: 應用修復**
```bash
cd automation/clawdbot/crews/common
cp tools.py tools.py.bak

# 編輯 tools.py，添加 _check_extensions() 方法
```

**步驟 2: 測試擴展檢查**
```bash
# 啟動 Python REPL
>>> from automation.clawdbot.crews.common.tools import DatabaseQueryTool
>>> tool = DatabaseQueryTool(connection_pool)

# 查看日誌輸出
# 如果擴展已啟用：
# ✅ pg_stat_statements enabled (version 1.10)

# 如果擴展未啟用：
# ⚠️ pg_stat_statements NOT enabled.
# To enable pg_stat_statements:
# 1. Run: CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
# ...
```

**步驟 3: 驗證功能**
```python
# 測試慢查詢檢測
>>> slow_queries = tool.check_slow_queries()
>>> if tool.pg_stat_statements_enabled:
...     print(f"Found {len(slow_queries)} slow queries")
... else:
...     print("Slow query detection unavailable (extension not enabled)")
```

**步驟 4: 啟用擴展（如果需要）**
```sql
-- 連接到 PostgreSQL
psql -U postgres -d smart_admin

-- 創建擴展
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- 修改配置（需要 superuser 權限）
ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';

-- 重啟 PostgreSQL
\q
sudo systemctl restart postgresql

-- 驗證
psql -U postgres -d smart_admin -c "SELECT * FROM pg_stat_statements LIMIT 1;"
```

---

## 📊 綜合評估與建議

### 優先級排序

| 優先級 | 漏洞 | 建議修復時間 | 理由 |
|--------|------|-------------|------|
| **P0** | #1 路徑遍歷攻擊 | 立即（1-2 天）| High severity，可能導致任意文件訪問 |
| **P1** | #2 數據庫連接泄漏 | 近期（1 天）| Medium severity，影響系統穩定性 |
| **P2** | #3 擴展依賴未驗證 | 短期（0.5 天）| Medium severity，功能靜默失效 |

---

### 修復路徑

#### 選項 A: 快速修復（推薦）
**時間**: 2-3 天
**內容**:
1. Day 1: 修復路徑遍歷漏洞 + 測試
2. Day 2: 修復數據庫連接泄漏 + 測試
3. Day 2 下午: 添加擴展檢查 + 完整回歸測試

**優勢**: 快速消除安全隱患，系統可安全部署

---

#### 選項 B: 完整安全強化（長期）
**時間**: 1 週
**內容**:
- 快速修復（2-3 天）
- 添加安全監控和告警（1 天）
- 完善審計日誌（1 天）
- 滲透測試和安全掃描（1-2 天）

**優勢**: 全面提升安全態勢

---

### 監控和告警

**部署後監控指標**:
```python
# 1. 路徑遍歷攻擊監控
SELECT count(*) as traversal_attempts
FROM t_ai_operation_audit
WHERE operation_type = 'file_access'
  AND status = 'FAILED'
  AND error_message LIKE '%Path traversal%'
  AND created_at > NOW() - INTERVAL '1 hour';

# 告警: traversal_attempts > 10 / 小時

# 2. 數據庫連接數監控
SELECT count(*) as active_connections
FROM pg_stat_activity
WHERE datname = 'smart_admin'
  AND state = 'active';

# 告警: active_connections > 80（接近上限 100）

# 3. 慢查詢檢測失效監控
SELECT count(*) as analysis_runs
FROM t_ai_operation_audit
WHERE crew_type = 'analyzer'
  AND results->>'slow_queries_count' = '0'
  AND created_at > NOW() - INTERVAL '1 day';

# 告警: 如果所有分析都返回 0 慢查詢，可能檢測失效
```

---

### 驗收標準

**安全修復完成標準**:
- ✅ 所有 3 個漏洞修復完成
- ✅ 單元測試覆蓋所有修復（pytest 通過率 100%）
- ✅ 路徑遍歷攻擊被成功阻止（10+ 測試用例）
- ✅ 數據庫連接池穩定（100 次操作後無泄漏）
- ✅ 擴展檢查在初始化時運行並記錄結果
- ✅ 審計日誌正確記錄所有安全事件
- ✅ 部署到測試環境並驗證（運行 24 小時無問題）

---

## 🔗 相關文檔

**修復指南**:
- [QUICK-FIX-SECURITY-GUIDE.md](QUICK-FIX-SECURITY-GUIDE.md) - 快速修復步驟（1-2 小時）
- [IMPLEMENTATION-STATUS.md](IMPLEMENTATION-STATUS.md) - 當前實施狀態

**代碼位置**:
- [file_access_guard.py](../../../automation/clawdbot/tools/file_access_guard.py:297-318) - 路徑遍歷漏洞
- [tools.py](../../../automation/clawdbot/crews/common/tools.py:323-324) - 連接泄漏
- [tools.py](../../../automation/clawdbot/crews/common/tools.py:368-377) - 擴展依賴

---

**文檔版本**: v1.0.0
**創建日期**: 2026-01-27
**最後更新**: 2026-01-27
**安全審計者**: SmartAdmin Security Team
