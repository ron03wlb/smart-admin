#!/bin/bash

# fix_oa_imports_part4.sh
# 修復 OA 模塊的 RequestEmployee 轉型問題

cd smartadmin-modules/smartadmin-oa

echo "=== Phase 1: 添加 RequestEmployee import 到 EnterpriseController ==="
# 在 RequestUser import 之後添加 RequestEmployee import
sed -i '/import net\.lab1024\.sa\.common\.core\.domain\.request\.RequestUser;/a import net.lab1024.sa.system.login.domain.RequestEmployee;' \
    src/main/java/net/lab1024/sa/oa/enterprise/controller/EnterpriseController.java

echo "=== Phase 2: 修復 getActualName() 轉型問題 ==="
# 將 SmartRequestUtil.getRequestUser().getActualName() 改為使用 RequestEmployee 轉型
sed -i 's/SmartRequestUtil\.getRequestUser()\.getActualName()/((RequestEmployee) SmartRequestUtil.getRequestUser()).getActualName()/g' \
    src/main/java/net/lab1024/sa/oa/enterprise/controller/EnterpriseController.java

echo "=== 驗證修改結果 ==="
echo "檢查 RequestEmployee import:"
grep -n "import.*RequestEmployee" src/main/java/net/lab1024/sa/oa/enterprise/controller/EnterpriseController.java

echo ""
echo "檢查 getActualName() 調用:"
grep -n "getActualName()" src/main/java/net/lab1024/sa/oa/enterprise/controller/EnterpriseController.java

echo ""
echo "✅ fix_oa_imports_part4.sh 執行完成"
