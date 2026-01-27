#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - PostgreSQL 審計表部署腳本
執行 V999__ai_system_tables.sql 以創建 5 個審計表
"""

import psycopg2
import sys
from pathlib import Path

# 數據庫連接配置（從 sa-base.yaml 讀取）
DB_CONFIG = {
    'host': 'db.vrerqanwgphvfyenjbsd.supabase.co',
    'port': 5432,
    'database': 'postgres',
    'user': 'postgres',
    'password': 'smart_admin_pwd',
    'sslmode': 'require'
}

# SQL 文件路徑
SQL_FILE = Path(__file__).parent / 'smart-admin-api-java21-springboot3' / 'sa-admin' / 'src' / 'main' / 'resources' / 'db' / 'migration' / 'V999__ai_system_tables.sql'

def main():
    print("=" * 80)
    print("SmartAdmin Auto-Coding - PostgreSQL 審計表部署")
    print("=" * 80)
    print()

    # 檢查 SQL 文件是否存在
    if not SQL_FILE.exists():
        print(f"[ERROR] SQL file not found: {SQL_FILE}")
        return 1

    print(f"[FILE] SQL: {SQL_FILE}")
    print(f"[DB] Host: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
    print()

    # 讀取 SQL 文件
    try:
        with open(SQL_FILE, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        print(f"[OK] Successfully read SQL file ({len(sql_content)} characters)")
    except Exception as e:
        print(f"[ERROR] Failed to read SQL file: {e}")
        return 1

    # 連接數據庫
    try:
        print()
        print("[CONNECTING] Connecting to database...")
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False
        cursor = conn.cursor()
        print("[OK] Database connection successful")
    except Exception as e:
        print(f"[ERROR] Database connection failed: {e}")
        print()
        print("建議：")
        print("  1. 檢查網路連接")
        print("  2. 檢查 Supabase 數據庫是否可訪問")
        print("  3. 驗證用戶名和密碼是否正確")
        return 1

    # 執行 SQL
    try:
        print()
        print("[EXECUTING] Executing SQL script...")
        cursor.execute(sql_content)
        conn.commit()
        print("[OK] SQL script executed successfully")
    except Exception as e:
        print(f"[ERROR] SQL execution failed: {e}")
        conn.rollback()
        cursor.close()
        conn.close()
        return 1

    # 驗證表創建
    try:
        print()
        print("[VERIFY] Verifying audit tables creation...")
        cursor.execute("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name LIKE 't_ai_%'
            ORDER BY table_name;
        """)
        tables = cursor.fetchall()

        print(f"[OK] Found {len(tables)} audit tables:")
        for table in tables:
            print(f"   - {table[0]}")

        expected_tables = {
            't_ai_operation_audit',
            't_ai_execution_log',
            't_agent_performance',
            't_skill_usage_stats',
            't_cost_attribution'
        }

        found_tables = {table[0] for table in tables}
        missing = expected_tables - found_tables

        if missing:
            print(f"[WARN] Missing {len(missing)} tables: {', '.join(missing)}")
        else:
            print("[OK] All 5 audit tables created successfully!")

    except Exception as e:
        print(f"[WARN] Verification failed: {e}")

    # 驗證索引創建
    try:
        print()
        print("[VERIFY] Verifying indexes creation...")
        cursor.execute("""
            SELECT indexname
            FROM pg_indexes
            WHERE schemaname = 'public'
              AND indexname LIKE 'idx_ai_%'
            ORDER BY indexname;
        """)
        indexes = cursor.fetchall()

        print(f"[OK] Found {len(indexes)} indexes:")
        for index in indexes:
            print(f"   - {index[0]}")

    except Exception as e:
        print(f"[WARN] Index verification failed: {e}")

    # 驗證清理函數
    try:
        print()
        print("[VERIFY] Verifying cleanup function...")
        cursor.execute("""
            SELECT proname
            FROM pg_proc
            WHERE proname = 'cleanup_old_audit_logs';
        """)
        functions = cursor.fetchall()

        if functions:
            print("[OK] Cleanup function 'cleanup_old_audit_logs()' created successfully")
        else:
            print("[WARN] Cleanup function not found")

    except Exception as e:
        print(f"[WARN] Function verification failed: {e}")

    # 關閉連接
    cursor.close()
    conn.close()

    print()
    print("=" * 80)
    print("[SUCCESS] PostgreSQL Audit Tables Deployment Complete!")
    print("=" * 80)
    print()
    print("Next Steps:")
    print("  1. 驗證表結構：SELECT * FROM t_ai_operation_audit LIMIT 1;")
    print("  2. 測試寫入：INSERT INTO t_ai_operation_audit ...")
    print("  3. 開始 P0-3 代碼實施（文件訪問白名單、Telegram Webhook）")
    print()

    return 0

if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\n\n[INTERRUPTED] User interrupted")
        sys.exit(130)
    except Exception as e:
        print(f"\n\n[ERROR] Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
