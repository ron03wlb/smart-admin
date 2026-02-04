#!/bin/bash
# Business Module - Temporary fix for SmartExcelUtil path

cd smartadmin-modules/smartadmin-business

echo "修正 SmartExcelUtil 路徑（臨時方案：使用 sa-base:foundation:core）"

# Use old path from foundation:core
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.util\.SmartExcelUtil;/import net.lab1024.sa.util.SmartExcelUtil;/g' {} \;

echo "完成！"
