#!/bin/bash
# Support 模塊目錄結構修正腳本
# 將檔案從 net/lab1024/sa/base/module/support/{module}
# 移動到 net/lab1024/sa/support/{module}

set -e

MODULES=(
    "changelog" "codegenerator" "config" "datatracer" "dict"
    "feedback" "file" "heartbeat" "helpdoc" "job"
    "liteflow" "loginlog" "mail" "message" "operatelog"
    "reload" "serialnumber" "table"
)

echo "======================================"
echo "Support 模塊目錄結構修正腳本"
echo "======================================"

SUCCESS_COUNT=0

for i in "${!MODULES[@]}"; do
    MODULE="${MODULES[$i]}"
    MODULE_PATH="smartadmin-support/smartadmin-support-$MODULE"

    echo ""
    echo "[$((i+1))/18] 修正 $MODULE 目錄結構..."

    OLD_STRUCTURE="$MODULE_PATH/src/main/java/net/lab1024/sa/base/module/support/$MODULE"
    NEW_STRUCTURE="$MODULE_PATH/src/main/java/net/lab1024/sa/support/$MODULE"

    # 檢查是否需要重組
    if [ -d "$OLD_STRUCTURE" ]; then
        # 創建新目錄結構
        mkdir -p "$MODULE_PATH/src/main/java/net/lab1024/sa/support"

        # 移動檔案
        mv "$OLD_STRUCTURE" "$MODULE_PATH/src/main/java/net/lab1024/sa/support/"

        # 清理舊目錄
        rm -rf "$MODULE_PATH/src/main/java/net/lab1024/sa/base"

        # 更新 package 聲明
        find "$MODULE_PATH/src" -name "*.java" -exec sed -i 's/^package net\.lab1024\.sa\.base\.module\.support\./package net.lab1024.sa.support./g' {} \;

        FILE_COUNT=$(find "$NEW_STRUCTURE" -name "*.java" 2>/dev/null | wc -l)
        echo "  ✅ $MODULE: Restructured $FILE_COUNT files"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    elif [ -d "$NEW_STRUCTURE" ]; then
        echo "  ✓ $MODULE: Already restructured"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "  ⚠️  $MODULE: No source files found"
    fi
done

echo ""
echo "======================================"
echo "目錄結構修正完成！"
echo "======================================"
echo "成功模塊: $SUCCESS_COUNT/18"
echo "======================================"
