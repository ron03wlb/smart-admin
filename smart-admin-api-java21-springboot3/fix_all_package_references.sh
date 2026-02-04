#!/bin/bash
# Comprehensive Package Name Migration Script
# Week 3 - System Module Package Reference Fix
# This script systematically fixes all known package name patterns

set -e

TARGET_DIR="smartadmin-modules/smartadmin-system"

echo "=========================================="
echo "Package Name Migration Script"
echo "Target: $TARGET_DIR"
echo "=========================================="
echo ""

cd "$TARGET_DIR"

# Phase 1: Foundation → Common.Core mappings (with exceptions)
echo "[Phase 1] Foundation → Common.Core mappings..."

# Special cases first (more specific patterns)
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.repeatsubmit\.|net.lab1024.sa.common.repeatsubmit.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.cache\.|net.lab1024.sa.common.cache.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.json\.|net.lab1024.sa.common.json.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.validation\.|net.lab1024.sa.common.validation.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.captcha\.|net.lab1024.sa.common.captcha.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.excel\.|net.lab1024.sa.common.excel.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.security\.|net.lab1024.sa.common.security.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.redislock\.|net.lab1024.sa.common.redislock.|g' {} \;
# Note: foundation.securityprotect stays as-is (not yet migrated)

# General foundation → common.core (for remaining patterns)
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.foundation\.|net.lab1024.sa.common.core.|g' {} \;

echo "✓ Foundation mappings applied"

# Phase 2: Base.Infrastructure → Common mappings
echo "[Phase 2] Base.Infrastructure → Common mappings..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.infrastructure\.web\.|net.lab1024.sa.common.web.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.infrastructure\.mybatis\.|net.lab1024.sa.common.mybatis.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.infrastructure\.redis\.|net.lab1024.sa.common.redis.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.infrastructure\.token\.|net.lab1024.sa.common.token.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.infrastructure\.datasource\.|net.lab1024.sa.common.datasource.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.infrastructure\.|net.lab1024.sa.common.|g' {} \;

echo "✓ Infrastructure mappings applied"

# Phase 3: Common.Core sub-module corrections (after Phase 1)
echo "[Phase 3] Common.Core sub-module corrections..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.captcha\.|net.lab1024.sa.common.captcha.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.cache\.|net.lab1024.sa.common.cache.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.apiencrypt\.|net.lab1024.sa.common.apiencrypt.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.json\.|net.lab1024.sa.common.json.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.validation\.|net.lab1024.sa.common.validation.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.security\.|net.lab1024.sa.common.security.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.securityprotect\.|net.lab1024.sa.foundation.securityprotect.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.repeatsubmit\.|net.lab1024.sa.common.repeatsubmit.|g' {} \;

echo "✓ Common.Core corrections applied"

# Phase 4: Base.Module.Support → Support mappings
echo "[Phase 4] Base.Module.Support → Support mappings..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.module\.support\.|net.lab1024.sa.support.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.admin\.module\.support\.|net.lab1024.sa.support.|g' {} \;

echo "✓ Support mappings applied"

# Phase 5: Web util path correction
echo "[Phase 5] Web util path corrections..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.web\.util\.|net.lab1024.sa.common.web.web.util.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.web\.base\.|net.lab1024.sa.common.web.web.base.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.web\.util\.|net.lab1024.sa.common.web.web.util.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.web\.base\.|net.lab1024.sa.common.web.web.base.|g' {} \;

echo "✓ Web util corrections applied"

# Phase 6: MyBatis util path correction
echo "[Phase 6] MyBatis util corrections..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.mybatis\.util\.|net.lab1024.sa.common.mybatis.util.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.mybatis\.|net.lab1024.sa.common.mybatis.|g' {} \;

echo "✓ MyBatis corrections applied"

# Phase 7: Swagger path corrections
echo "[Phase 7] Swagger corrections..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.base\.swagger\.|net.lab1024.sa.common.swagger.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.common\.core\.swagger\.|net.lab1024.sa.common.swagger.|g' {} \;

echo "✓ Swagger corrections applied"

# Phase 8: Admin module references → System
echo "[Phase 8] Admin module → System mappings..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.admin\.module\.system\.|net.lab1024.sa.system.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.admin\.constant\.|net.lab1024.sa.system.constant.|g' {} \;

echo "✓ Admin module mappings applied"

# Phase 9: Remove invalid imports
echo "[Phase 9] Removing invalid imports..."

# Remove AdminApplication imports (shouldn't be needed in System module)
find src -name "*.java" -exec sed -i '/import net\.lab1024\.sa\.admin\.AdminApplication;/d' {} \;

echo "✓ Invalid imports removed"

# Phase 10: Domain object corrections
echo "[Phase 10] Domain object corrections..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.domain\.|net.lab1024.sa.common.core.domain.|g' {} \;
find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.constant\.|net.lab1024.sa.common.core.constant.|g' {} \;

echo "✓ Domain corrections applied"

# Phase 11: Util package corrections
echo "[Phase 11] Util package corrections..."

find src -name "*.java" -exec sed -i 's|net\.lab1024\.sa\.util\.|net.lab1024.sa.common.core.util.|g' {} \;

echo "✓ Util corrections applied"

echo ""
echo "=========================================="
echo "Package migration completed!"
echo "=========================================="
echo ""
echo "Summary of changes:"
echo "  - Foundation → Common.Core (with exceptions)"
echo "  - Base.Infrastructure → Common modules"
echo "  - Base.Module.Support → Support"
echo "  - Admin.Module → System"
echo "  - Web/MyBatis/Swagger path corrections"
echo "  - Invalid imports removed"
echo ""
echo "Next step: Compile System module"
echo "  cd ../.. && ./gradlew :smartadmin-modules:smartadmin-system:compileJava"
echo ""
