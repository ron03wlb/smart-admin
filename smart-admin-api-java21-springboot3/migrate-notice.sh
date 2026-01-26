#!/bin/bash
# Notice module migration

FILES=(
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/notice/controller/NoticeController.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/notice/service/NoticeService.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/notice/service/NoticeTypeService.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/notice/service/NoticeEmployeeService.java"
  "sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/notice/manager/NoticeManager.java"
)

for FILE in "${FILES[@]}"; do
  echo "Processing: $FILE"
  sed -i '/^import jakarta.annotation.Resource;$/d' "$FILE"
  
  if grep -q "import lombok" "$FILE" && ! grep -q "import lombok.RequiredArgsConstructor" "$FILE"; then
    sed -i '/^import lombok/a import lombok.RequiredArgsConstructor;' "$FILE" | head -1
  fi
  
  sed -i 's/^\(@Service\|@RestController\)$/@RequiredArgsConstructor\n&/' "$FILE"
  sed -i 's/@Resource private /private final /g' "$FILE"
  
  echo "✓ Migrated: $FILE"
done

echo "Notice module migration completed!"
