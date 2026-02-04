#!/bin/bash
# Final fixes for System module - IpGeolocationUtil and RequestEmployee casting

cd smartadmin-modules/smartadmin-system

echo "Applying final System module fixes..."

# 1. Fix IpGeolocationUtil import path (common.core.ipgeo → common.ipgeolocation)
echo "  [1/3] IpGeolocationUtil import path correction..."
find src -name "*.java" -exec sed -i 's|import net\.lab1024\.sa\.common\.core\.ipgeo\.util\.IpGeolocationUtil;|import net.lab1024.sa.common.ipgeolocation.util.IpGeolocationUtil;|g' {} \;

# 2. Fix LoginController.java - cast RequestUser to RequestEmployee
echo "  [2/3] LoginController RequestEmployee casting..."
# Find the specific line and replace it
sed -i 's|loginService\.getLoginResult(SmartRequestUtil\.getRequestUser(), tokenValue)|loginService.getLoginResult((RequestEmployee) SmartRequestUtil.getRequestUser(), tokenValue)|g' \
  src/main/java/net/lab1024/sa/system/login/controller/LoginController.java

# 3. Add RequestEmployee import if missing
echo "  [3/3] Adding RequestEmployee import if needed..."
# Check if RequestEmployee import exists in LoginController
if ! grep -q "import net.lab1024.sa.system.login.domain.RequestEmployee;" src/main/java/net/lab1024/sa/system/login/controller/LoginController.java; then
    # Add import after the last import statement
    sed -i '/^import.*$/a import net.lab1024.sa.system.login.domain.RequestEmployee;' \
      src/main/java/net/lab1024/sa/system/login/controller/LoginController.java
fi

echo ""
echo "Final fixes completed!"
echo ""
echo "Next: Verify compilation"
echo "  cd ../.. && ./gradlew :smartadmin-modules:smartadmin-system:compileJava"
