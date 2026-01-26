#!/bin/bash
# Batch migration for system module

echo "Starting system module migration..."

cd sa-admin/src/main/java/net/lab1024/sa/admin/module/system

# Find all files with @Resource
FILES=$(find . -name "*.java" -exec grep -l "@Resource" {} \;)

COUNT=0
for FILE in $FILES; do
  echo "[$((++COUNT))/39] Processing: $FILE"
  
  # Remove @Resource import
  sed -i '/^import jakarta.annotation.Resource;$/d' "$FILE"
  
  # Add @RequiredArgsConstructor import if lombok imports exist and RequiredArgsConstructor doesn't
  if grep -q "^import lombok" "$FILE" && ! grep -q "import lombok.RequiredArgsConstructor" "$FILE"; then
    # Add after first lombok import
    sed -i '0,/^import lombok/s//import lombok.RequiredArgsConstructor;\n&/' "$FILE"
  fi
  
  # Add @RequiredArgsConstructor annotation before class
  if ! grep -B1 "^public class\|^class" "$FILE" | grep -q "@RequiredArgsConstructor"; then
    sed -i 's/^\(@Service\|@RestController\|@Component\)$/@RequiredArgsConstructor\n&/' "$FILE"
  fi
  
  # Replace @Resource private with private final
  sed -i 's/@Resource private /private final /g' "$FILE"
done

echo "✓ System module migration completed! ($COUNT files processed)"
