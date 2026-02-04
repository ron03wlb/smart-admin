#!/bin/bash
# OA Module Package Migration - Part 2 (supplementary fixes)

cd smartadmin-modules/smartadmin-oa

echo "OA 模塊補充修復（5 個模式）"
echo ""

# 1. Fix SmartExcelUtil path (use old path from foundation:core)
echo "[1/5] SmartExcelUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.util\.SmartExcelUtil;/import net.lab1024.sa.util.SmartExcelUtil;/g' {} \;

# 2. Fix SmartPageUtil path
echo "[2/5] SmartPageUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.mybatis\.util\./import net.lab1024.sa.common.mybatis.util./g' {} \;

# 3. Fix SmartBeanUtil path (short form)
echo "[3/5] SmartBeanUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.util\.SmartBeanUtil;/import net.lab1024.sa.common.core.util.SmartBeanUtil;/g' {} \;

# 4. Fix SchemaEnum annotation path
echo "[4/5] SchemaEnum 註解路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.swagger\.annotation\./import net.lab1024.sa.common.swagger.annotation./g' {} \;

# 5. Fix CheckEnum annotation path
echo "[5/5] CheckEnum 註解路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.validation\.annotation\./import net.lab1024.sa.common.validation.annotation./g' {} \;

echo ""
echo "補充修復完成！"
