#!/bin/bash
# OA Module Package Migration - Part 3 (final corrections)

cd smartadmin-modules/smartadmin-oa

echo "OA 模塊最終修正（6 個模式）"
echo ""

# 1. Fix remaining base.web.util references
echo "[1/6] 剩餘 base.web.util 引用修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.web\.util\./import net.lab1024.sa.common.web.util./g' {} \;

# 2. Fix RepeatSubmit annotation path (common.core.repeatsubmit → common.repeatsubmit)
echo "[2/6] RepeatSubmit 註解路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.repeatsubmit\./import net.lab1024.sa.common.repeatsubmit./g' {} \;

# 3. Fix CollectionUtils (apache.commons.collections → commons.collections4)
echo "[3/6] CollectionUtils 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import org\.apache\.commons\.collections\.CollectionUtils;/import org.apache.commons.collections4.CollectionUtils;/g' {} \;

# 4. Fix remaining admin.module.business.oa references
echo "[4/6] 剩餘 admin.module.business.oa 引用修正..."
find src -name "*.java" \
    -exec sed -i 's/net\.lab1024\.sa\.admin\.module\.business\.oa\./net.lab1024.sa.oa./g' {} \;

# 5. Fix JsonUtil path (common.core.json → common.json)
echo "[5/6] JsonUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.json\./import net.lab1024.sa.common.json./g' {} \;

# 6. Fix SmartEnumUtil path (common.core.validation → common.validation)
echo "[6/6] SmartEnumUtil 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.validation\.util\./import net.lab1024.sa.common.validation.util./g' {} \;

echo ""
echo "最終修正完成！"
