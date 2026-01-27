# Quick Fix Guide - Security Issues

**用途**: 快速修復 3 個 P1 安全漏洞（1-2 小時）
**適用場景**: 需要立即修復安全問題，不等待全面重構
**版本**: v1.0.0
**日期**: 2026-01-27

---

## 🚨 為什麼要立即修復？

automation/clawdbot 系統存在 **3 個 P1 級別安全漏洞**：

| 漏洞 | 嚴重性 | 風險 | 修復時間 |
|------|--------|------|---------|
| **路徑遍歷攻擊** | High | 任意文件訪問（/etc/passwd, SSH 私鑰）| 30-45 分鐘 |
| **數據庫連接泄漏** | Medium | 連接池耗盡，系統不可用 | 20-30 分鐘 |
| **擴展依賴未驗證** | Medium | 功能靜默失效，用戶不知道 | 10-15 分鐘 |

**總計**: 60-90 分鐘可完成所有修復

**立即修復的理由**:
- 🔴 路徑遍歷攻擊可能導致完整系統妥協
- 🔴 數據庫連接泄漏影響系統穩定性（生產環境風險高）
- 🔴 功能靜默失效導致誤導用戶（以為沒有性能問題）

---

## 📋 修復前準備

### 1. 備份當前文件

```bash
cd automation/clawdbot

# 創建備份目錄
mkdir -p backups/$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"

# 備份需要修改的文件
cp tools/file_access_guard.py $BACKUP_DIR/
cp crews/common/tools.py $BACKUP_DIR/
cp crews/common/base_crew.py $BACKUP_DIR/

echo "✅ Backup completed: $BACKUP_DIR"
```

---

### 2. 確認環境

```bash
# 檢查 Python 版本
python3 --version  # 需要 Python 3.8+

# 檢查 PostgreSQL 連接
psql -U postgres -d smart_admin -c "SELECT version();"

# 檢查項目根目錄
echo $PROJECT_ROOT  # 應該指向項目根目錄
# 如果未設置:
export PROJECT_ROOT="/path/to/smart-admin"
```

---

## 🔧 Step 1: 修復路徑遍歷漏洞（30-45 分鐘）

### 1.1 打開文件

```bash
cd automation/clawdbot/tools
vim file_access_guard.py
# 或使用你喜歡的編輯器
```

---

### 1.2 找到 normalize_path() 函數（第 297-318 行）

**當前代碼（脆弱）**:
```python
def normalize_path(file_path: str) -> str:
    """規範化文件路徑"""
    normalized = os.path.normpath(file_path)
    normalized = normalized.replace('\\', '/')
    while '//' in normalized:
        normalized = normalized.replace('//', '/')
    return normalized
```

---

### 1.3 替換為安全版本

**完整替換代碼**（直接複製）:
```python
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
    from pathlib import Path

    try:
        # 1. 轉換為絕對路徑
        if not os.path.isabs(file_path):
            file_path = os.path.join(PROJECT_ROOT, file_path)

        # 2. 解析符號鏈接和相對路徑
        resolved_path = Path(file_path).resolve()
        project_root_path = Path(PROJECT_ROOT).resolve()

        # 3. 關鍵檢查：確保路徑在項目根目錄內
        try:
            resolved_path.relative_to(project_root_path)
        except ValueError as e:
            logger.error(
                f"Path traversal attack detected! "
                f"Requested: {file_path}, "
                f"Resolved: {resolved_path}, "
                f"Root: {project_root_path}"
            )
            raise SecurityError(
                f"Path traversal detected: {file_path} "
                f"resolves to {resolved_path} outside project root"
            ) from e

        # 4. 統一使用正斜杠
        normalized = str(resolved_path).replace('\\', '/')

        logger.debug(f"Path normalized: {file_path} -> {normalized}")
        return normalized

    except (OSError, RuntimeError) as e:
        logger.error(f"Path normalization error: {e}")
        raise SecurityError(f"Invalid path: {file_path}") from e
```

**注意**: 保持縮進與原函數一致（通常 4 個空格）

---

### 1.4 保存文件

```bash
# 保存並退出編輯器
:wq  # Vim
# 或 Ctrl+X, Y, Enter (nano)
```

---

### 1.5 測試修復

**創建測試腳本** (`test_path_traversal_fix.py`):
```python
#!/usr/bin/env python3
import sys
sys.path.insert(0, '/path/to/automation/clawdbot')

from tools.file_access_guard import normalize_path, SecurityError
import os

# 設置項目根目錄
os.environ['PROJECT_ROOT'] = '/path/to/smart-admin'

# 測試用例
test_cases = [
    # (路徑, 是否應該被阻止)
    ("/allowed/sa-admin/../../../etc/passwd", True),
    ("../../secret/config.yml", True),
    ("/project/../forbidden/file.txt", True),
    ("sa-admin/pom.xml", False),  # 允許的相對路徑
    ("/path/to/smart-admin/sa-admin/src/main/java/App.java", False),  # 允許的絕對路徑
]

passed = 0
failed = 0

for path, should_block in test_cases:
    try:
        result = normalize_path(path)
        if should_block:
            print(f"❌ FAILED: {path} should be blocked but was allowed")
            failed += 1
        else:
            print(f"✅ PASSED: {path} allowed correctly")
            passed += 1
    except SecurityError as e:
        if should_block:
            print(f"✅ PASSED: {path} blocked correctly")
            passed += 1
        else:
            print(f"❌ FAILED: {path} should be allowed but was blocked")
            print(f"   Error: {e}")
            failed += 1

print(f"\n{'='*50}")
print(f"Total: {passed + failed} tests")
print(f"✅ Passed: {passed}")
print(f"❌ Failed: {failed}")
print(f"{'='*50}")

if failed == 0:
    print("\n🎉 All tests passed! Path traversal fix is working.")
    sys.exit(0)
else:
    print(f"\n⚠️ {failed} test(s) failed. Please review the fix.")
    sys.exit(1)
```

**運行測試**:
```bash
cd automation/clawdbot
chmod +x test_path_traversal_fix.py
python3 test_path_traversal_fix.py

# 預期輸出:
# ✅ PASSED: /allowed/sa-admin/../../../etc/passwd blocked correctly
# ✅ PASSED: ../../secret/config.yml blocked correctly
# ...
# 🎉 All tests passed! Path traversal fix is working.
```

---

## 🔧 Step 2: 修復數據庫連接泄漏（20-30 分鐘）

### 2.1 修改 BaseCrew（添加 Context Manager）

**文件**: `automation/clawdbot/crews/common/base_crew.py`

**在 BaseCrew 類中添加方法**（找到 `__init__` 方法後添加）:

```python
class BaseCrew(ABC):
    # ... 現有代碼 ...

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
```

**添加 import**（在文件頂部）:
```python
from contextlib import contextmanager
```

---

### 2.2 修改 DatabaseQueryTool

**文件**: `automation/clawdbot/crews/common/tools.py`

**找到 DatabaseQueryTool 類（第 321 行左右）**

**替換整個 __init__ 方法**:
```python
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

    # ✅ 刪除 close() 方法（不再需要）
```

**更新所有使用連接的方法**（示例 - check_slow_queries）:
```python
def check_slow_queries(self, min_duration_ms: int = 1000) -> List[Dict[str, Any]]:
    """檢查慢查詢"""
    if not self.pg_stat_statements_enabled:
        logger.warning("pg_stat_statements not enabled")
        return []

    # ✅ 使用 context manager
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

        return [{"query": row[0], ...} for row in results]
```

**添加 import**（在文件頂部）:
```python
from contextlib import contextmanager
```

---

### 2.3 更新 Crew 使用方式

**文件**: `automation/clawdbot/crews/analyzer_crew.py`（以及 developer_crew.py, qa_crew.py）

**找到創建 DatabaseQueryTool 的地方**（示例 - 第 350 行左右）:

**修改前**:
```python
# ❌ 舊方式：傳入連接字符串
db_tool = DatabaseQueryTool(DB_CONNECTION_STRING)
```

**修改後**:
```python
# ✅ 新方式：傳入共享連接池
db_tool = DatabaseQueryTool(self.connection_pool)
```

**重要**: 不再需要手動調用 `db_tool.close()`，刪除所有 close() 調用

---

### 2.4 測試修復

**創建測試腳本** (`test_connection_pool.py`):
```python
#!/usr/bin/env python3
import sys
sys.path.insert(0, '/path/to/automation/clawdbot')

from crews.common.base_crew import BaseCrew
from crews.common.tools import DatabaseQueryTool
from psycopg2 import pool

# 創建測試連接池
conn_pool = pool.SimpleConnectionPool(
    minconn=1,
    maxconn=5,
    dsn="postgresql://postgres:password@localhost/smart_admin"
)

print("Testing connection pool...")

# 模擬 100 次操作
for i in range(100):
    tool = DatabaseQueryTool(conn_pool)
    try:
        # 執行查詢
        slow_queries = tool.check_slow_queries()
        if i % 20 == 0:
            print(f"  Iteration {i}: {len(slow_queries)} slow queries")
    except Exception as e:
        print(f"  ❌ Error at iteration {i}: {e}")

# 驗證連接池狀態
print(f"\n{'='*50}")
print(f"Connection pool status:")
print(f"  Used connections: {conn_pool._used}")
print(f"  Available connections: {len(conn_pool._pool)}")

if conn_pool._used == 0:
    print(f"\n✅ SUCCESS: No connection leaks detected")
    print(f"   All connections returned to pool")
else:
    print(f"\n❌ FAILED: Connection leak detected")
    print(f"   {conn_pool._used} connection(s) not returned")

conn_pool.closeall()
```

**運行測試**:
```bash
python3 test_connection_pool.py

# 預期輸出:
# Testing connection pool...
#   Iteration 0: 3 slow queries
#   Iteration 20: 5 slow queries
#   ...
# ✅ SUCCESS: No connection leaks detected
```

---

## 🔧 Step 3: 添加擴展檢查（10-15 分鐘）

### 3.1 修改 DatabaseQueryTool（添加擴展檢查）

**文件**: `automation/clawdbot/crews/common/tools.py`

**在 DatabaseQueryTool 類中添加方法**:

```python
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
```

---

### 3.2 更新 check_slow_queries() 方法

**在 check_slow_queries() 開頭添加檢查**:

```python
def check_slow_queries(self, min_duration_ms: int = 1000) -> List[Dict[str, Any]]:
    """檢查慢查詢（帶擴展檢查）"""

    # ✅ 檢查擴展狀態
    if not self.pg_stat_statements_enabled:
        logger.warning(
            "Slow query detection unavailable: pg_stat_statements not enabled. "
            "Returning empty result. See initialization logs for setup instructions."
        )
        return []

    # 原有的查詢邏輯...
    with self._get_connection() as conn:
        # ...
```

---

### 3.3 測試擴展檢查

**手動測試**:
```bash
# 啟動 Python REPL
cd automation/clawdbot
python3

>>> import os
>>> os.environ['DB_CONNECTION_STRING'] = "postgresql://localhost/smart_admin"
>>> os.environ['PROJECT_ROOT'] = "/path/to/smart-admin"

>>> from crews.common.tools import DatabaseQueryTool
>>> from psycopg2 import pool

>>> conn_pool = pool.SimpleConnectionPool(minconn=1, maxconn=5, dsn=os.environ['DB_CONNECTION_STRING'])
>>> tool = DatabaseQueryTool(conn_pool)

# 查看日誌輸出
# 如果擴展已啟用：
# ✅ pg_stat_statements enabled (version 1.10)

# 如果擴展未啟用：
# ⚠️ pg_stat_statements NOT enabled.
# To enable pg_stat_statements:
# ...

>>> tool.pg_stat_statements_enabled
# True 或 False

>>> slow_queries = tool.check_slow_queries()
>>> len(slow_queries)
# 如果擴展已啟用，返回實際數量
# 如果擴展未啟用，返回 0 並記錄警告
```

---

### 3.4 啟用擴展（如果需要）

**如果測試顯示擴展未啟用**:

```bash
# 連接到 PostgreSQL
psql -U postgres -d smart_admin

-- 創建擴展
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- 修改配置（需要 superuser 權限）
ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';

-- 退出
\q

# 重啟 PostgreSQL
sudo systemctl restart postgresql

# 驗證
psql -U postgres -d smart_admin -c "SELECT count(*) FROM pg_stat_statements;"
# 應該返回數字（不是錯誤）
```

---

## ✅ 驗證所有修復（10 分鐘）

### 1. 運行完整測試套件

```bash
cd automation/clawdbot

# 運行所有測試
python3 -m pytest tests/ -v

# 或手動測試
python3 test_path_traversal_fix.py
python3 test_connection_pool.py
```

---

### 2. 運行集成測試

**測試 Analyzer Crew**:
```bash
cd automation/clawdbot/crews
python3 analyzer_crew.py --target sa-admin

# 檢查日誌輸出：
# - 沒有路徑遍歷警告
# - 沒有連接泄漏
# - 擴展檢查結果正確記錄
```

---

### 3. 檢查審計日誌

```sql
-- 連接到 PostgreSQL
psql -U postgres -d smart_admin

-- 檢查路徑遍歷攻擊記錄（應該沒有新記錄）
SELECT * FROM t_ai_operation_audit
WHERE operation_type = 'file_access'
  AND status = 'FAILED'
  AND error_message LIKE '%Path traversal%'
ORDER BY created_at DESC
LIMIT 5;

-- 檢查數據庫連接數（應該穩定）
SELECT count(*) as active_connections
FROM pg_stat_activity
WHERE datname = 'smart_admin'
  AND state = 'active';
-- 應該 < 20（穩定範圍）

-- 檢查 pg_stat_statements 狀態
SELECT count(*) FROM pg_stat_statements;
-- 如果擴展已啟用，返回數字
-- 如果未啟用，返回錯誤（預期）
```

---

### 4. 部署到測試環境

```bash
# 提交修復
cd automation/clawdbot
git add tools/file_access_guard.py crews/common/tools.py crews/common/base_crew.py
git commit -m "fix(security): Fix 3 P1 security vulnerabilities

- Fix path traversal attack (CWE-22)
- Fix database connection leak (CWE-404)
- Add pg_stat_statements extension check

Fixes: SECURITY-001, SECURITY-002, SECURITY-003"

# 部署到測試環境
./deploy.sh

# 監控 24 小時
kubectl logs -f -l app=smartadmin-auto-coding -n smartadmin | grep -E "(Path traversal|connection|pg_stat_statements)"
```

---

## 🔄 回滾計劃（如果出問題）

### 快速回滾

```bash
cd automation/clawdbot

# 找到最新的備份
LATEST_BACKUP=$(ls -t backups/ | head -1)
echo "Rolling back to: $LATEST_BACKUP"

# 恢復文件
cp backups/$LATEST_BACKUP/file_access_guard.py tools/
cp backups/$LATEST_BACKUP/tools.py crews/common/
cp backups/$LATEST_BACKUP/base_crew.py crews/common/

# 驗證
python3 -c "from tools.file_access_guard import normalize_path; print('Rollback successful')"

# 重啟服務
kubectl rollout restart deployment/smartadmin-auto-coding -n smartadmin
```

---

### 常見問題排查

**問題 1: 路徑規範化失敗**
```python
SecurityError: Invalid path: /some/path

# 原因: PROJECT_ROOT 未設置或不正確
# 解決:
export PROJECT_ROOT="/correct/path/to/smart-admin"
```

**問題 2: 連接池耗盡**
```sql
ERROR: remaining connection slots are reserved for non-replication superuser connections

# 原因: 連接池大小不足
# 解決: 增加 PostgreSQL max_connections
ALTER SYSTEM SET max_connections = 200;
SELECT pg_reload_conf();
```

**問題 3: 擴展檢查失敗**
```
ERROR: relation "pg_available_extensions" does not exist

# 原因: 權限不足
# 解決: 使用 superuser 連接或授權
GRANT pg_read_all_settings TO your_user;
```

---

## 📊 修復完成檢查清單

**安全修復驗收標準**:
- [ ] 路徑遍歷測試全部通過（5+ 惡意路徑被阻止）
- [ ] 連接池測試無泄漏（100 次操作後 used=0）
- [ ] 擴展檢查在初始化時運行並記錄結果
- [ ] 所有 Crew 測試成功運行（analyzer, developer, qa）
- [ ] 審計日誌無新的安全警告
- [ ] PostgreSQL 連接數穩定（< 20 活動連接）
- [ ] 備份文件已創建（可回滾）
- [ ] Git commit 已提交（包含詳細說明）

---

## 🎯 下一步

**修復完成後**:
1. ✅ 通知安全團隊修復完成
2. ✅ 更新 [IMPLEMENTATION-STATUS.md](IMPLEMENTATION-STATUS.md) 安全狀態
3. ✅ 部署到生產環境（經過 24 小時測試後）
4. ✅ 監控安全告警（Prometheus + Grafana）

**長期改進**:
- 考慮完整的安全強化計劃（參見 [ARCHITECTURE-REFACTORING-PLAN.md](ARCHITECTURE-REFACTORING-PLAN.md)）
- 定期安全審計（每季度）
- 滲透測試（每半年）

---

## 🔗 相關文檔

**詳細分析**:
- [SECURITY-GAPS-ANALYSIS.md](SECURITY-GAPS-ANALYSIS.md) - 3 個漏洞的詳細技術分析
- [IMPLEMENTATION-STATUS.md](IMPLEMENTATION-STATUS.md) - 當前實施狀態

**代碼位置**:
- [file_access_guard.py](../../../automation/clawdbot/tools/file_access_guard.py:297-318) - 路徑遍歷修復位置
- [tools.py](../../../automation/clawdbot/crews/common/tools.py) - 連接池修復位置
- [base_crew.py](../../../automation/clawdbot/crews/common/base_crew.py) - Context Manager 添加位置

---

**文檔版本**: v1.0.0
**創建日期**: 2026-01-27
**預計修復時間**: 60-90 分鐘
**維護者**: SmartAdmin Security Team
