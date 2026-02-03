#!/usr/bin/env python3
"""
SmartAdmin Foundation 模塊遷移腳本
Week 1 Day 3-4: 批量遷移 Foundation 模塊到新結構

遷移映射:
- sa-base/foundation/domain     → smartadmin-common/smartadmin-common-core
- sa-base/foundation/core       → smartadmin-common/smartadmin-common-core
- sa-base/foundation/validation → smartadmin-common/smartadmin-common-validation
- ... (其他 11 個模塊)

操作:
1. 複製源代碼目錄
2. 更新包名 (net.lab1024.sa.foundation → net.lab1024.sa.common.core)
3. 創建 build.gradle.kts
4. 複製資源文件
"""

import os
import shutil
import re
from pathlib import Path
from typing import Dict, List

# ========================================================================
# 配置: Foundation 模塊映射表
# ========================================================================
FOUNDATION_MODULE_MAPPING: Dict[str, str] = {
    # Foundation 模塊 → Common 模塊
    "domain": "smartadmin-common-core",  # 合併到 core
    "core": "smartadmin-common-core",
    "validation": "smartadmin-common-validation",
    "json": "smartadmin-common-json",
    "excel": "smartadmin-common-excel",
    "ip-geolocation": "smartadmin-common-ip-geolocation",
    "cache": "smartadmin-common-cache",
    "mq": "smartadmin-common-mq",
    "redis-lock": "smartadmin-common-redis-lock",
    "api-encrypt": "smartadmin-common-api-encrypt",
    "captcha": "smartadmin-common-captcha",
    "repeat-submit": "smartadmin-common-repeat-submit",
    "data-masking": "smartadmin-common-data-masking",
    "security-protect": "smartadmin-common-security",
}

# 包名映射
PACKAGE_MAPPING = {
    "net.lab1024.sa.foundation": "net.lab1024.sa.common.core",
}


# ========================================================================
# 工具函數
# ========================================================================
def copy_source_files(old_module_path: str, new_module_path: str):
    """複製源代碼文件"""
    old_src = os.path.join(old_module_path, "src")
    new_src = os.path.join(new_module_path, "src")

    if not os.path.exists(old_src):
        print(f"⚠️  源目錄不存在: {old_src}")
        return

    # 複製整個 src 目錄
    if os.path.exists(new_src):
        shutil.rmtree(new_src)

    shutil.copytree(old_src, new_src)
    print(f"  📁 複製源代碼: {old_src} → {new_src}")


def update_package_names(module_path: str, old_pkg: str, new_pkg: str):
    """批量更新包名"""
    src_dir = os.path.join(module_path, "src")

    if not os.path.exists(src_dir):
        return

    java_files = []
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root, file))

    print(f"  🔄 更新包名: {len(java_files)} 個 Java 文件")

    for file_path in java_files:
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # 替換包名
            updated_content = content.replace(old_pkg, new_pkg)

            # 只有內容改變時才寫入
            if updated_content != content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(updated_content)
                print(f"    ✅ {os.path.basename(file_path)}")

        except Exception as e:
            print(f"    ❌ 錯誤處理 {file_path}: {e}")


def create_build_gradle(module_path: str, module_name: str):
    """創建 build.gradle.kts"""
    build_file = os.path.join(module_path, "build.gradle.kts")

    # 基本的 build.gradle.kts 模板
    build_content = f'''plugins {{
    java
}}

description = "SmartAdmin Common - {module_name}"

dependencies {{
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Vavr - Functional programming
    api("io.vavr:vavr")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Spring Context (for @Component, @Service, etc.)
    compileOnly("org.springframework:spring-context")

    // Jakarta Validation
    compileOnly("jakarta.validation:jakarta.validation-api")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")
}}
'''

    with open(build_file, 'w', encoding='utf-8') as f:
        f.write(build_content)

    print(f"  📝 創建 build.gradle.kts")


# ========================================================================
# 主遷移邏輯
# ========================================================================
def migrate_foundation_module(module_name: str, target_module: str):
    """遷移單個 Foundation 模塊"""
    print(f"\n{'='*70}")
    print(f"遷移: {module_name} → {target_module}")
    print(f"{'='*70}")

    old_module_path = f"sa-base/foundation/{module_name}"
    new_module_path = f"smartadmin-common/{target_module}"

    if not os.path.exists(old_module_path):
        print(f"❌ 舊模塊不存在: {old_module_path}")
        return False

    if not os.path.exists(new_module_path):
        print(f"❌ 新模塊目錄不存在: {new_module_path}")
        return False

    # Step 1: 複製源代碼
    copy_source_files(old_module_path, new_module_path)

    # Step 2: 更新包名
    old_pkg = f"net.lab1024.sa.foundation.{module_name.replace('-', '')}"
    new_pkg = f"net.lab1024.sa.common.core"

    # 特殊處理: domain 和 core 都合併到 common.core
    if module_name == "domain":
        old_pkg = "net.lab1024.sa.foundation.domain"
        new_pkg = "net.lab1024.sa.common.core.domain"
    elif module_name == "core":
        old_pkg = "net.lab1024.sa.foundation.core"
        new_pkg = "net.lab1024.sa.common.core.util"

    update_package_names(new_module_path, old_pkg, new_pkg)

    # Step 3: 創建 build.gradle.kts
    if not os.path.exists(os.path.join(new_module_path, "build.gradle.kts")):
        create_build_gradle(new_module_path, target_module)

    print(f"✅ {module_name} 遷移完成！")
    return True


def main():
    """主函數"""
    print("=" * 70)
    print("SmartAdmin Foundation 模塊批量遷移工具")
    print("Week 1 Day 3-4: 遷移 14 個 Foundation 模塊")
    print("=" * 70)

    # 切換到項目根目錄
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    success_count = 0
    fail_count = 0

    for old_module, new_module in FOUNDATION_MODULE_MAPPING.items():
        try:
            if migrate_foundation_module(old_module, new_module):
                success_count += 1
            else:
                fail_count += 1
        except Exception as e:
            print(f"❌ 遷移失敗: {old_module} - {e}")
            fail_count += 1

    # 總結報告
    print("\n" + "=" * 70)
    print("遷移總結")
    print("=" * 70)
    print(f"✅ 成功: {success_count} 個模塊")
    print(f"❌ 失敗: {fail_count} 個模塊")
    print(f"📦 總計: {len(FOUNDATION_MODULE_MAPPING)} 個模塊")
    print("=" * 70)

    print("\n下一步:")
    print("1. 運行: ./gradlew :smartadmin-common:smartadmin-common-core:build")
    print("2. 運行: ./gradlew :smartadmin-common:smartadmin-common-validation:build")
    print("3. 驗證所有 common 模塊編譯通過")
    print("4. 創建 Git commit: git add . && git commit -m 'Week 1: Foundation modules migration'")


if __name__ == "__main__":
    main()
