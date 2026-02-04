#!/bin/bash
# Fix remaining System module imports - Part 3
# Final corrections for AdminRequestUtil, NoNeedLogin, DataMasking, IpGeolocation

cd smartadmin-modules/smartadmin-system

echo "Fixing remaining System module import statements (Part 3)..."

# 1. Fix AdminRequestUtil → SmartRequestUtil
echo "  [1/5] AdminRequestUtil → SmartRequestUtil..."
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.admin\.util\.AdminRequestUtil;/import net.lab1024.sa.common.web.web.util.SmartRequestUtil;/g' {} \;
find src -name "*.java" -exec sed -i 's/AdminRequestUtil\.getRequestUser()/SmartRequestUtil.getRequestUser()/g' {} \;

# 2. Fix NoNeedLogin annotation path
echo "  [2/5] NoNeedLogin annotation path..."
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.annotation\.NoNeedLogin;/import net.lab1024.sa.common.core.annotation.NoNeedLogin;/g' {} \;

# 3. Fix DataMasking annotation path (should be in common-data-masking)
echo "  [3/5] DataMasking annotation path..."
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.foundation\.datamasking\./import net.lab1024.sa.common.datamasking./g' {} \;
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.datamasking\./import net.lab1024.sa.common.datamasking./g' {} \;

# 4. Fix IpGeolocation path
echo "  [4/5] IpGeolocation path..."
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.foundation\.ipgeolocation\./import net.lab1024.sa.common.ipgeolocation./g' {} \;
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.ipgeolocation\./import net.lab1024.sa.common.ipgeolocation./g' {} \;

# 5. Fix any remaining admin.constant references (should be system.constant)
echo "  [5/5] Remaining admin.constant references..."
find src -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.admin\.constant\./net.lab1024.sa.system.constant./g' {} \;

echo ""
echo "Part 3 import fixes completed!"
echo ""
echo "Next: Verify compilation"
echo "  cd ../.. && ./gradlew :smartadmin-modules:smartadmin-system:compileJava"
