#!/bin/bash
# Support 模塊批量遷移腳本（Bash 版本）
# 遷移內容：18 個 support 模塊，309 個 Java 檔案

set -e  # 遇到錯誤立即退出

# 模塊列表（loginlog 已完成，剩餘 17 個）
MODULES=(
    "changelog" "codegenerator" "config" "datatracer" "dict"
    "feedback" "file" "heartbeat" "helpdoc" "job"
    "liteflow" "mail" "message" "operatelog"
    "reload" "serialnumber" "table"
)

echo "======================================"
echo "Support 模塊批量遷移腳本"
echo "======================================"
echo "模塊數量: ${#MODULES[@]} (已完成 loginlog)"
echo "======================================"

SUCCESS_COUNT=0

for i in "${!MODULES[@]}"; do
    MODULE="${MODULES[$i]}"
    echo ""
    echo "[$((i+2))/18] 遷移 $MODULE..."

    OLD_PATH="sa-base/support/$MODULE"
    NEW_PATH="smartadmin-support/smartadmin-support-$MODULE"

    # 1. 複製源碼
    if [ -d "$OLD_PATH/src" ]; then
        cp -r "$OLD_PATH/src" "$NEW_PATH/"
        FILE_COUNT=$(find "$NEW_PATH/src" -name "*.java" | wc -l)
        echo "  ✓ Copied $FILE_COUNT Java files"

        # 2. 更新包名
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.base\.module\.support/net.lab1024.sa.support/g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.foundation\.domain\./net.lab1024.sa.common.core.domain./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.foundation\.core\./net.lab1024.sa.common.core./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.foundation\.validation\./net.lab1024.sa.common.validation./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.foundation\.json\./net.lab1024.sa.common.json./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.foundation\.excel\./net.lab1024.sa.common.excel./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.foundation\.cache\./net.lab1024.sa.common.cache./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.foundation\.mq\./net.lab1024.sa.common.mq./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.util\./net.lab1024.sa.common.core.util./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.annotation\./net.lab1024.sa.common.core.annotation./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.common\.domain\./net.lab1024.sa.common.core.domain./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.common\.core\.domain\.enumeration\./net.lab1024.sa.common.core.enumeration./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.base\.infrastructure\.web\./net.lab1024.sa.common.web./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.base\.infrastructure\.mybatis\./net.lab1024.sa.common.mybatis./g' {} \;
        find "$NEW_PATH/src" -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.base\.infrastructure\.redis\./net.lab1024.sa.common.redis./g' {} \;
        echo "  ✓ Updated package names"

        # 3. 複製 build.gradle.kts
        if [ -f "$OLD_PATH/build.gradle.kts" ]; then
            cp "$OLD_PATH/build.gradle.kts" "$NEW_PATH/"
            echo "  ✓ Copied build.gradle.kts"
        fi

        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        echo "  ✅ $MODULE migration completed"
    else
        echo "  ⚠️  Source not found: $OLD_PATH/src"
    fi
done

echo ""
echo "======================================"
echo "遷移完成！"
echo "======================================"
echo "成功模塊: $((SUCCESS_COUNT + 1))/18 (含 loginlog)"
echo "======================================"
