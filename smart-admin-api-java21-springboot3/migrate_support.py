#!/usr/bin/env python3
"""
Support 模塊批量遷移腳本
節省 5 人天工作量

遷移內容：18 個 support 模塊，309 個 Java 檔案
包名映射：net.lab1024.sa.base.module.support.{module} → net.lab1024.sa.support.{module}
"""
import os
import shutil
import re
from pathlib import Path

# 模塊映射（不包括 build 目錄）
SUPPORT_MODULES = [
    'changelog', 'codegenerator', 'config', 'datatracer', 'dict',
    'feedback', 'file', 'heartbeat', 'helpdoc', 'job',
    'liteflow', 'loginlog', 'mail', 'message', 'operatelog',
    'reload', 'serialnumber', 'table'
]

# 包名映射規則
PACKAGE_MAPPINGS = {
    'net.lab1024.sa.base.module.support': 'net.lab1024.sa.support',
    'net.lab1024.sa.foundation.domain': 'net.lab1024.sa.common.core.domain',
    'net.lab1024.sa.foundation.core': 'net.lab1024.sa.common.core',
    'net.lab1024.sa.foundation.validation': 'net.lab1024.sa.common.validation',
    'net.lab1024.sa.foundation.json': 'net.lab1024.sa.common.json',
    'net.lab1024.sa.foundation.excel': 'net.lab1024.sa.common.excel',
    'net.lab1024.sa.foundation.cache': 'net.lab1024.sa.common.cache',
    'net.lab1024.sa.foundation.captcha': 'net.lab1024.sa.common.captcha',
    'net.lab1024.sa.foundation.api': 'net.lab1024.sa.common.apiencrypt',
    'net.lab1024.sa.foundation.datamasking': 'net.lab1024.sa.common.datamasking',
    'net.lab1024.sa.foundation.securityprotect': 'net.lab1024.sa.common.security',
    'net.lab1024.sa.foundation.repeatsubmit': 'net.lab1024.sa.common.repeatsubmit',
    'net.lab1024.sa.foundation.mq': 'net.lab1024.sa.common.mq',
    'net.lab1024.sa.foundation.ip': 'net.lab1024.sa.common.ipgeolocation',
    'net.lab1024.sa.base.infrastructure.web': 'net.lab1024.sa.common.web',
    'net.lab1024.sa.base.infrastructure.redis': 'net.lab1024.sa.common.redis',
    'net.lab1024.sa.base.infrastructure.token': 'net.lab1024.sa.common.token',
    'net.lab1024.sa.base.infrastructure.mybatis': 'net.lab1024.sa.common.mybatis',
    'net.lab1024.sa.base.infrastructure.datasource': 'net.lab1024.sa.common.datasource',
}

# Import 特殊修正
IMPORT_FIXES = [
    (r'import net\.lab1024\.sa\.common\.domain\.', 'import net.lab1024.sa.common.core.domain.'),
    (r'import net\.lab1024\.sa\.util\.', 'import net.lab1024.sa.common.core.util.'),
    (r'import net\.lab1024\.sa\.common\.core\.domain\.enumeration\.', 'import net.lab1024.sa.common.core.enumeration.'),
    (r'import net\.lab1024\.sa\.annotation\.', 'import net.lab1024.sa.common.core.annotation.'),
    (r'import net\.lab1024\.sa\.code\.', 'import net.lab1024.sa.common.core.code.'),
]

def update_file_content(file_path: Path) -> int:
    """更新單個檔案的包名和 import 語句"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        original = content

        # 1. 更新包名映射
        for old_pkg, new_pkg in PACKAGE_MAPPINGS.items():
            content = content.replace(old_pkg, new_pkg)

        # 2. 特殊 import 修正
        for pattern, replacement in IMPORT_FIXES:
            content = re.sub(pattern, replacement, content)

        # 3. 寫回檔案（僅當有變更時）
        if content != original:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return 1
        return 0
    except Exception as e:
        print(f'  ❌ Error updating {file_path}: {e}')
        return 0

def migrate_module(module_name: str) -> tuple[int, int]:
    """遷移單個 support 模塊"""
    old_path = Path(f'sa-base/support/{module_name}')
    new_path = Path(f'smartadmin-support/smartadmin-support-{module_name}')

    if not old_path.exists():
        print(f'  ⚠️  Source not found: {old_path}')
        return 0, 0

    # 1. 複製源碼目錄（保留目錄結構）
    if (old_path / 'src').exists():
        if (new_path / 'src').exists():
            shutil.rmtree(new_path / 'src')
        shutil.copytree(old_path / 'src', new_path / 'src')

    # 2. 複製 build.gradle.kts（如果存在）
    if (old_path / 'build.gradle.kts').exists():
        shutil.copy2(old_path / 'build.gradle.kts', new_path / 'build.gradle.kts')

    # 3. 更新所有 Java 檔案的包名和 import
    files_updated = 0
    files_total = 0
    for java_file in (new_path / 'src').rglob('*.java'):
        files_total += 1
        files_updated += update_file_content(java_file)

    # 4. 更新 build.gradle.kts 中的依賴
    gradle_file = new_path / 'build.gradle.kts'
    if gradle_file.exists():
        update_file_content(gradle_file)

    return files_total, files_updated

def main():
    """主函數：批量遷移所有 support 模塊"""
    print('=' * 60)
    print('Support 模塊批量遷移腳本')
    print('=' * 60)
    print(f'模塊數量: {len(SUPPORT_MODULES)}')
    print(f'目標目錄: smartadmin-support/')
    print('=' * 60)

    total_files = 0
    total_updated = 0
    success_count = 0

    for i, module in enumerate(SUPPORT_MODULES, 1):
        print(f'\n[{i}/{len(SUPPORT_MODULES)}] 遷移 {module}...')
        files_count, updated_count = migrate_module(module)

        if files_count > 0:
            total_files += files_count
            total_updated += updated_count
            success_count += 1
            print(f'  ✅ {module}: {files_count} files, {updated_count} updated')
        else:
            print(f'  ⚠️  {module}: No files migrated')

    print('\n' + '=' * 60)
    print('遷移完成！')
    print('=' * 60)
    print(f'成功模塊: {success_count}/{len(SUPPORT_MODULES)}')
    print(f'總檔案數: {total_files}')
    print(f'已更新: {total_updated}')
    print('=' * 60)

if __name__ == '__main__':
    main()
