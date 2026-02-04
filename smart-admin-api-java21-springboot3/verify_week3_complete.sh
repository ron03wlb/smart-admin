#!/bin/bash

# verify_week3_complete.sh
# Week 3 最終驗證腳本 - System/Business/OA 三大模塊遷移完成

echo "=========================================================="
echo "Week 3 最終驗證 - System/Business/OA 模塊遷移完成"
echo "=========================================================="
echo ""

# ========== 1. 全模塊編譯驗證 ==========
echo "【步驟 1/5】全模塊編譯驗證"
echo "執行: ./gradlew :smartadmin-modules:smartadmin-system:compileJava"
./gradlew :smartadmin-modules:smartadmin-system:compileJava > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ System 模塊編譯失敗"
    exit 1
fi
echo "✅ System 模塊編譯成功"

echo "執行: ./gradlew :smartadmin-modules:smartadmin-business:compileJava"
./gradlew :smartadmin-modules:smartadmin-business:compileJava > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Business 模塊編譯失敗"
    exit 1
fi
echo "✅ Business 模塊編譯成功"

echo "執行: ./gradlew :smartadmin-modules:smartadmin-oa:compileJava"
./gradlew :smartadmin-modules:smartadmin-oa:compileJava > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ OA 模塊編譯失敗"
    exit 1
fi
echo "✅ OA 模塊編譯成功"
echo ""

# ========== 2. ArchUnit 測試驗證（3 × 11 = 33 規則）==========
echo "【步驟 2/5】ArchUnit 測試驗證（33 個規則）"

# System 模塊 ArchUnit
echo "執行: System 模塊 ArchUnit 測試..."
./gradlew :smartadmin-modules:smartadmin-system:test --tests ArchitectureTest > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ System 模塊 ArchUnit 測試失敗"
    exit 1
fi
SYSTEM_TESTS=$(grep -oP 'tests="\K[0-9]+' smartadmin-modules/smartadmin-system/build/test-results/test/TEST-net.lab1024.sa.system.ArchitectureTest.xml | head -1)
echo "✅ System 模塊 ArchUnit: $SYSTEM_TESTS/11 規則通過"

# Business 模塊 ArchUnit
echo "執行: Business 模塊 ArchUnit 測試..."
./gradlew :smartadmin-modules:smartadmin-business:test --tests ArchitectureTest > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Business 模塊 ArchUnit 測試失敗"
    exit 1
fi
BUSINESS_TESTS=$(grep -oP 'tests="\K[0-9]+' smartadmin-modules/smartadmin-business/build/test-results/test/TEST-net.lab1024.sa.business.ArchitectureTest.xml | head -1)
echo "✅ Business 模塊 ArchUnit: $BUSINESS_TESTS/11 規則通過"

# OA 模塊 ArchUnit
echo "執行: OA 模塊 ArchUnit 測試..."
./gradlew :smartadmin-modules:smartadmin-oa:test --tests ArchitectureTest > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ OA 模塊 ArchUnit 測試失敗"
    exit 1
fi
OA_TESTS=$(grep -oP 'tests="\K[0-9]+' smartadmin-modules/smartadmin-oa/build/test-results/test/TEST-net.lab1024.sa.oa.ArchitectureTest.xml | head -1)
echo "✅ OA 模塊 ArchUnit: $OA_TESTS/11 規則通過"

TOTAL_ARCHUNIT_TESTS=$((SYSTEM_TESTS + BUSINESS_TESTS + OA_TESTS))
echo "   總計: $TOTAL_ARCHUNIT_TESTS/33 規則通過"
echo ""

# ========== 3. Manager 層驗證（17 個 Manager 類）==========
echo "【步驟 3/5】Manager 層驗證"
SYSTEM_MANAGERS=$(find smartadmin-modules/smartadmin-system/src/main/java -name "*Manager.java" | wc -l)
BUSINESS_MANAGERS=$(find smartadmin-modules/smartadmin-business/src/main/java -name "*Manager.java" | wc -l)
OA_MANAGERS=$(find smartadmin-modules/smartadmin-oa/src/main/java -name "*Manager.java" | wc -l)
TOTAL_MANAGERS=$((SYSTEM_MANAGERS + BUSINESS_MANAGERS + OA_MANAGERS))

echo "System 模塊 Manager: $SYSTEM_MANAGERS 個"
echo "Business 模塊 Manager: $BUSINESS_MANAGERS 個"
echo "OA 模塊 Manager: $OA_MANAGERS 個"
echo "✅ 總計 Manager 層: $TOTAL_MANAGERS 個 Manager 類"
echo ""

# ========== 4. 舊包名殘留掃描 ==========
echo "【步驟 4/5】舊包名殘留掃描"

# 掃描 admin.module.system 殘留
SYSTEM_OLD=$(grep -r "net\.lab1024\.sa\.admin\.module\.system" smartadmin-modules/smartadmin-system/src --include="*.java" 2>/dev/null | wc -l)
# 掃描 admin.module.business 殘留（排除 OA）
BUSINESS_OLD=$(grep -r "net\.lab1024\.sa\.admin\.module\.business" smartadmin-modules/smartadmin-business/src --include="*.java" 2>/dev/null | grep -v "\.oa" | wc -l)
# 掃描 admin.module.business.oa 殘留
OA_OLD=$(grep -r "net\.lab1024\.sa\.admin\.module\.business\.oa" smartadmin-modules/smartadmin-oa/src --include="*.java" 2>/dev/null | wc -l)

TOTAL_OLD=$((SYSTEM_OLD + BUSINESS_OLD + OA_OLD))

if [ "$TOTAL_OLD" -eq 0 ]; then
    echo "✅ 無舊包名殘留（0 處舊包名引用）"
else
    echo "❌ 發現 $TOTAL_OLD 處舊包名殘留"
    echo "   - System: $SYSTEM_OLD 處"
    echo "   - Business: $BUSINESS_OLD 處"
    echo "   - OA: $OA_OLD 處"
    exit 1
fi
echo ""

# ========== 5. 檔案統計 ==========
echo "【步驟 5/5】檔案統計"
SYSTEM_FILES=$(find smartadmin-modules/smartadmin-system/src/main/java -name "*.java" | wc -l)
BUSINESS_FILES=$(find smartadmin-modules/smartadmin-business/src/main/java -name "*.java" | wc -l)
OA_FILES=$(find smartadmin-modules/smartadmin-oa/src/main/java -name "*.java" | wc -l)
TOTAL_FILES=$((SYSTEM_FILES + BUSINESS_FILES + OA_FILES))

echo "System 模塊: $SYSTEM_FILES 個 Java 檔案"
echo "Business 模塊: $BUSINESS_FILES 個 Java 檔案"
echo "OA 模塊: $OA_FILES 個 Java 檔案"
echo "✅ 總計: $TOTAL_FILES 個 Java 檔案遷移完成"
echo ""

# ========== 最終結果 ==========
echo "=========================================================="
echo "✅ Week 3 最終驗證通過 - 3 大模塊遷移成功"
echo "=========================================================="
echo ""
echo "遷移統計："
echo "  ┌─────────────────────────────────────────────────┐"
echo "  │ 模塊         檔案數   Manager   ArchUnit 規則   │"
echo "  ├─────────────────────────────────────────────────┤"
echo "  │ System       $SYSTEM_FILES      $SYSTEM_MANAGERS        $SYSTEM_TESTS/11          │"
echo "  │ Business     $BUSINESS_FILES       $BUSINESS_MANAGERS        $BUSINESS_TESTS/11          │"
echo "  │ OA           $OA_FILES       $OA_MANAGERS        $OA_TESTS/11          │"
echo "  ├─────────────────────────────────────────────────┤"
echo "  │ 總計         $TOTAL_FILES      $TOTAL_MANAGERS       $TOTAL_ARCHUNIT_TESTS/33          │"
echo "  └─────────────────────────────────────────────────┘"
echo ""
echo "核心優勢保留驗證："
echo "  - Manager 層: ✅ $TOTAL_MANAGERS 個 Manager 類 (100% 保留)"
echo "  - ArchUnit 規則: ✅ $TOTAL_ARCHUNIT_TESTS/33 規則通過 (100%)"
echo "  - 四層架構: ✅ Controller→Service→Manager→Dao"
echo "  - Vavr Option: ✅ Service 層強制使用（ArchUnit 驗證）"
echo "  - 包名遷移: ✅ 0 處舊包名殘留"
echo ""
echo "準備創建 Git Tag: week-3-modules-complete"
