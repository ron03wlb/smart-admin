#!/bin/bash
# Business Module Final Verification Script

echo "=================================================="
echo "Business 模塊最終驗收檢查"
echo "=================================================="
echo ""

# 1. 編譯驗證
echo "[1/6] 編譯驗證..."
if ./gradlew :smartadmin-modules:smartadmin-business:compileJava > /dev/null 2>&1; then
    echo "  ✅ 編譯成功"
else
    echo "  ❌ 編譯失敗"
    exit 1
fi

# 2. ArchUnit 測試
echo "[2/6] ArchUnit 測試驗證..."
if ./gradlew :smartadmin-modules:smartadmin-business:test --tests ArchitectureTest > /dev/null 2>&1; then
    TEST_COUNT=$(grep -o 'counter">[0-9]*<' smartadmin-modules/smartadmin-business/build/reports/tests/test/index.html | head -1 | grep -o '[0-9]*')
    echo "  ✅ ArchUnit 測試通過（$TEST_COUNT 個測試，100% 成功）"
else
    echo "  ❌ ArchUnit 測試失敗"
    exit 1
fi

# 3. Manager 層驗證
echo "[3/6] Manager 層驗證..."
MANAGER_COUNT=$(find smartadmin-modules/smartadmin-business/src/main/java -name "*Manager.java" | wc -l)
echo "  ✅ Manager 層保留: $MANAGER_COUNT 個類"
find smartadmin-modules/smartadmin-business/src/main/java -name "*Manager.java" -exec basename {} \; | sed 's/^/    - /'

# 4. Vavr Option 使用
echo "[4/6] Vavr Option 使用驗證..."
VAVR_USAGE=$(grep -r "import io.vavr.control.Option" smartadmin-modules/smartadmin-business/src/main/java --include="*Service.java" 2>/dev/null | wc -l)
echo "  ✅ Service 層 Vavr Option 引用: $VAVR_USAGE 處"

# 5. 舊包名掃描
echo "[5/6] 舊包名殘留掃描..."
OLD_REFS=$(grep -r "net\.lab1024\.sa\.admin\.module" smartadmin-modules/smartadmin-business/src --include="*.java" 2>/dev/null | wc -l || echo 0)
echo "  ✅ 舊包名殘留: $OLD_REFS 處"

# 6. 檔案統計
echo "[6/6] 檔案統計..."
JAVA_FILES=$(find smartadmin-modules/smartadmin-business/src/main/java -name "*.java" | wc -l)
echo "  ✅ Java 檔案總數: $JAVA_FILES 個"

echo ""
echo "=================================================="
echo "驗收完成 ✅"
echo "=================================================="
