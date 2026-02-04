#!/bin/bash
# Business Module Package Migration Script
# Migrates: sa-admin/module/business → smartadmin-modules/smartadmin-business

cd smartadmin-modules/smartadmin-business

echo "=================================================="
echo "Business 模塊包名遷移（11 階段）"
echo "=================================================="
echo ""

# ========================================
# Phase 1: Update main package declaration
# ========================================
echo "[1/11] 更新主包名聲明..."
find src -name "*.java" \
    -exec sed -i 's/^package net\.lab1024\.sa\.admin\.module\.business/package net.lab1024.sa.business/g' {} \;

# ========================================
# Phase 2: Update Business internal imports
# ========================================
echo "[2/11] 更新 Business 內部引用..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.admin\.module\.business\./import net.lab1024.sa.business./g' {} \;

# ========================================
# Phase 3: Remove OA references (if any)
# ========================================
echo "[3/11] 移除 OA 模塊引用..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.admin\.module\.business\.oa\..*//g' {} \;

# ========================================
# Phase 4: Update Support module references
# ========================================
echo "[4/11] 更新 Support 模塊引用..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.module\.support\./import net.lab1024.sa.support./g' {} \;

# ========================================
# Phase 5: Update Foundation → Common.Core
# ========================================
echo "[5/11] Foundation → Common.Core..."

# Exception modules that have their own packages
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.captcha\./import net.lab1024.sa.common.captcha./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.cache\./import net.lab1024.sa.common.cache./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.apiencrypt\./import net.lab1024.sa.common.apiencrypt./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.repeatsubmit\./import net.lab1024.sa.common.repeatsubmit./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.datamasking\./import net.lab1024.sa.common.datamasking./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\.ipgeolocation\./import net.lab1024.sa.common.ipgeo./g' {} \;

# General foundation → common.core (for core modules)
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.foundation\./import net.lab1024.sa.common.core./g' {} \;

# ========================================
# Phase 6: Update Infrastructure → Common
# ========================================
echo "[6/11] Infrastructure → Common..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.web\./import net.lab1024.sa.common.web./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.mybatis\./import net.lab1024.sa.common.mybatis./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.redis\./import net.lab1024.sa.common.redis./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.swagger\./import net.lab1024.sa.common.swagger./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.token\./import net.lab1024.sa.common.token./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.base\.infrastructure\.datasource\./import net.lab1024.sa.common.datasource./g' {} \;

# ========================================
# Phase 7: Fix common.core sub-module corrections
# ========================================
echo "[7/11] Common.Core 子模塊修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.captcha\./import net.lab1024.sa.common.captcha./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.cache\./import net.lab1024.sa.common.cache./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.apiencrypt\./import net.lab1024.sa.common.apiencrypt./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.repeatsubmit\./import net.lab1024.sa.common.repeatsubmit./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.datamasking\./import net.lab1024.sa.common.datamasking./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.ipgeolocation\./import net.lab1024.sa.common.ipgeo./g' {} \;
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.ipgeo\./import net.lab1024.sa.common.ipgeo./g' {} \;

# ========================================
# Phase 8: Fix web util paths
# ========================================
echo "[8/11] Web Util 路徑修正..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.common\.web\.web\./import net.lab1024.sa.common.web./g' {} \;

# ========================================
# Phase 9: Fix AdminRequestUtil → SmartRequestUtil
# ========================================
echo "[9/11] AdminRequestUtil → SmartRequestUtil..."
find src -name "*.java" \
    -exec sed -i 's/import net\.lab1024\.sa\.admin\.util\.AdminRequestUtil;/import net.lab1024.sa.common.web.util.SmartRequestUtil;/g' {} \;
find src -name "*.java" \
    -exec sed -i 's/AdminRequestUtil\./SmartRequestUtil./g' {} \;

# ========================================
# Phase 10: Update admin.constant references
# ========================================
echo "[10/11] Admin Constant 引用..."
find src -name "*.java" \
    -exec sed -i 's/net\.lab1024\.sa\.admin\.constant\./net.lab1024.sa.business.constant./g' {} \;

# ========================================
# Phase 11: Update Mapper XML namespaces
# ========================================
echo "[11/11] Mapper XML Namespace 更新..."
find src/main/resources/mapper -name "*.xml" \
    -exec sed -i 's/net\.lab1024\.sa\.admin\.module\.business\./net.lab1024.sa.business./g' {} \;

echo ""
echo "=================================================="
echo "遷移完成！"
echo "=================================================="
echo ""
echo "驗證建議："
echo "  cd ../.. && ./gradlew :smartadmin-modules:smartadmin-business:compileJava"
