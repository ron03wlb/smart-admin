#!/bin/bash
# Fix remaining System module imports - Part 2

cd smartadmin-modules/smartadmin-system

echo "Fixing remaining System module import statements..."

# Fix common.web.util references (should be common.web.web.util)
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.common\.web\.util\./import net.lab1024.sa.common.web.web.util./g' {} \;

# Fix common.core.securityprotect references (should be foundation.securityprotect)
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.common\.core\.securityprotect\./import net.lab1024.sa.foundation.securityprotect./g' {} \;

# Fix admin.module.support.securityprotect references (should be support.securityprotect)
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.admin\.module\.support\.securityprotect\./import net.lab1024.sa.support.securityprotect./g' {} \;

# Fix base.mybatis.util references (should be common.mybatis.util)
find src -name "*.java" -exec sed -i 's/import net\.lab1024\.sa\.base\.mybatis\.util\./import net.lab1024.sa.common.mybatis.util./g' {} \;

echo "Part 2 import fixes completed!"
