#!/bin/bash
# Business Module Package Migration - Part 3 (final corrections)

cd smartadmin-modules/smartadmin-business

echo "Business 模塊最終修正（6 個模式）"
echo ""

# 1. Fix SmartExcelUtil path (actually in common.core.util)
echo "[1/6] SmartExcelUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.util\.SmartExcelUtil;/import net.lab1024.sa.common.core.util.SmartExcelUtil;/g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.excel\.util\.SmartExcelUtil;/import net.lab1024.sa.common.core.util.SmartExcelUtil;/g' {} \;

# 2. Fix JsonUtil path (common.core.json → common.json)
echo "[2/6] JsonUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.json\.util\./import net.lab1024.sa.common.json.util./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.json\.deserializer\./import net.lab1024.sa.common.json.deserializer./g' {} \;

# 3. Fix SmartEnumUtil path (common.core.validation → common.validation)
echo "[3/6] SmartEnumUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.validation\.util\./import net.lab1024.sa.common.validation.util./g' {} \;

# 4. Fix Kafka MQ paths (common.core.mq → common.mq)
echo "[4/6] Kafka MQ 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.mq\.kafka\./import net.lab1024.sa.common.mq.kafka./g' {} \;

# 5. Fix DataTracer paths (already correct, but verify)
echo "[5/6] DataTracer 路徑驗證..."
# No changes needed - net.lab1024.sa.support.datatracer.* is correct

# 6. Fix any remaining common.core.* sub-modules
echo "[6/6] 剩餘 common.core 子模塊修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.excel\./import net.lab1024.sa.common.excel./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.mq\./import net.lab1024.sa.common.mq./g' {} \;

echo ""
echo "最終修正完成！"
