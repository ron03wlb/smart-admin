#!/bin/bash
# OA Module Package Migration Script
# Migrates: sa-admin/module/business/oa → smartadmin-modules/smartadmin-oa

cd smartadmin-modules/smartadmin-oa

echo "=================================================="
echo "OA 模塊包名遷移（11 階段）"
echo "=================================================="
echo ""

# ========================================
# Phase 1: Update main package declaration (business.oa → oa)
# ========================================
echo "[1/11] 更新主包名聲明（business.oa → oa）..."
find src -name "*.java" \
    -exec sed -i 's/^package net\.lab1024\.sa\.admin\.module\.business\.oa/package net.lab1024.sa.oa/g' {} \;

# ========================================
# Phase 2: Update OA internal imports
# ========================================
echo "[2/11] 更新 OA 內部引用..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.admin\.module\.business\.oa\./import net.lab1024.sa.oa./g' {} \;

# ========================================
# Phase 3: Update System module references (OA → System dependency)
# ========================================
echo "[3/11] 更新 System 模塊引用（OA 依賴 System）..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.admin\.module\.system\./import net.lab1024.sa.system./g' {} \;

# ========================================
# Phase 4: Remove other Business module references (should be none)
# ========================================
echo "[4/11] 移除其他 Business 模塊引用（驗證無非法依賴）..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.admin\.module\.business\.(?!oa).*//g' {} \;

# ========================================
# Phase 5: Update Support module references
# ========================================
echo "[5/11] 更新 Support 模塊引用..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.module\.support\./import net.lab1024.sa.support./g' {} \;

# ========================================
# Phase 6: Update Foundation → Common.Core
# ========================================
echo "[6/11] Foundation → Common.Core..."

# Exception modules that have their own packages
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.captcha\./import net.lab1024.sa.common.captcha./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.cache\./import net.lab1024.sa.common.cache./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.datamasking\./import net.lab1024.sa.common.datamasking./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.ipgeolocation\./import net.lab1024.sa.common.ipgeo./g' {} \;

# General foundation → common.core (for core modules)
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\./import net.lab1024.sa.common.core./g' {} \;

# ========================================
# Phase 7: Update Infrastructure → Common
# ========================================
echo "[7/11] Infrastructure → Common..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.web\./import net.lab1024.sa.common.web./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.mybatis\./import net.lab1024.sa.common.mybatis./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.swagger\./import net.lab1024.sa.common.swagger./g' {} \;

# ========================================
# Phase 8: Fix common.core sub-module corrections
# ========================================
echo "[8/11] Common.Core 子模塊修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.captcha\./import net.lab1024.sa.common.captcha./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.cache\./import net.lab1024.sa.common.cache./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.datamasking\./import net.lab1024.sa.common.datamasking./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.ipgeolocation\./import net.lab1024.sa.common.ipgeo./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.ipgeo\./import net.lab1024.sa.common.ipgeo./g' {} \;

# ========================================
# Phase 9: Fix web util paths
# ========================================
echo "[9/11] Web Util 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.web\.web\./import net.lab1024.sa.common.web./g' {} \;

# ========================================
# Phase 10: Fix AdminRequestUtil → SmartRequestUtil
# ========================================
echo "[10/11] AdminRequestUtil → SmartRequestUtil..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.admin\.util\.AdminRequestUtil;/import net.lab1024.sa.common.web.util.SmartRequestUtil;/g' {} \;
find src -name "*.java" \
    -exec sed -i 's/AdminRequestUtil\./SmartRequestUtil./g' {} \;

# ========================================
# Phase 11: Update admin.constant references (if any)
# ========================================
echo "[11/11] Admin Constant 引用..."
find src -name "*.java" \
    -exec sed -i 's/net\.lab1024\.sa\.admin\.constant\./net.lab1024.sa.oa.constant./g' {} \;

echo ""
echo "=================================================="
echo "遷移完成！"
echo "=================================================="
echo ""
echo "驗證建議："
echo "  cd ../.. && ./gradlew :smartadmin-modules:smartadmin-oa:compileJava"
