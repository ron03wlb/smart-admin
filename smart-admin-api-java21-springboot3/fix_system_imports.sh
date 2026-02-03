#!/bin/bash
# Fix remaining import issues in System module

cd smartadmin-modules/smartadmin-system

echo "Fixing System module import statements..."

# Fix admin.constant references
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.admin\.constant\./import net.lab1024.sa.system.constant./g' {} \;

# Fix validation.util references
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.validation\.util\./import net.lab1024.sa.common.validation.util./g' {} \;

# Fix foundation.cache references
find src -name "*.java" -exec sed -i 's/net\.lab1024\.sa\.foundation\.cache\.CacheService/net.lab1024.sa.common.cache.CacheService/g' {} \;

# Fix base.swagger.annotation references
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.base\.swagger\.annotation\./import net.lab1024.sa.common.swagger.annotation./g' {} \;

# Fix base.web.util references
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.base\.web\.util\./import net.lab1024.sa.common.web.util./g' {} \;

# Fix admin.AdminApplication references - just remove these imports as they shouldn't be needed
find src -name "*.java" -exec sed -i '/import net\.lab1024\.sa\.admin\.AdminApplication;/d' {} \;

echo "Import fixes completed!"
