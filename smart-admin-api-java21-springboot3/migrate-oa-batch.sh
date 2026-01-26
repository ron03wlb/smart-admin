#!/bin/bash
# Batch migration script for OA modules

FILES=(
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/enterprise/controller/EnterpriseController.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/enterprise/service/EnterpriseService.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/enterprise/manager/EnterpriseManager.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/invoice/controller/InvoiceController.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/invoice/service/InvoiceService.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/invoice/manager/InvoiceManager.java"
)

for FILE in "${FILES[@]}"; do
  echo "Processing: $FILE"
  # Remove @Resource import
  sed -i '/^import jakarta.annotation.Resource;$/d' "$FILE"
  
  # Add @RequiredArgsConstructor import after lombok.extern.slf4j.Slf4j or other lombok imports
  if grep -q "import lombok" "$FILE" && ! grep -q "import lombok.RequiredArgsConstructor" "$FILE"; then
    sed -i '/^import lombok/a import lombok.RequiredArgsConstructor;' "$FILE" | head -1
  fi
  
  # Add @RequiredArgsConstructor annotation before class declaration
  sed -i 's/^\(@Service\|@RestController\)$/@RequiredArgsConstructor\n&/' "$FILE"
  
  # Replace @Resource private with private final
  sed -i 's/@Resource private /private final /g' "$FILE"
  
  echo "✓ Migrated: $FILE"
done

echo "Batch migration completed!"
