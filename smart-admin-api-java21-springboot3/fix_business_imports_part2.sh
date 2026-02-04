#!/bin/bash
# Business Module Package Migration - Part 2 (补充修复)

cd smartadmin-modules/smartadmin-business

echo "Business 模塊補充修復（5 個模式）"
echo ""

# 1. Fix SmartPageUtil path
echo "[1/5] SmartPageUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.mybatis\.util\./import net.lab1024.sa.common.mybatis.util./g' {} \;

# 2. Fix SmartBeanUtil path (short form)
echo "[2/5] SmartBeanUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.util\.SmartBeanUtil;/import net.lab1024.sa.common.core.util.SmartBeanUtil;/g' {} \;

# 3. Fix SchemaEnum annotation path
echo "[3/5] SchemaEnum 註解路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.swagger\.annotation\./import net.lab1024.sa.common.swagger.annotation./g' {} \;

# 4. Fix CheckEnum annotation path
echo "[4/5] CheckEnum 註解路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.validation\.annotation\./import net.lab1024.sa.common.validation.annotation./g' {} \;

# 5. Fix AdminSwaggerTagConst (use Business module's own SwaggerTagConst)
echo "[5/5] AdminSwaggerTagConst → SwaggerTagConst..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.business\.constant\.AdminSwaggerTagConst;/import net.lab1024.sa.business.constant.SwaggerTagConst;/g' {} \;
find src -name "*.java" \
    -exec sed -i 's/AdminSwaggerTagConst\.Business\./SwaggerTagConst.Business./g' {} \;

echo ""
echo "補充修復完成！"
