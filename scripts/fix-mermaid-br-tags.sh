#!/bin/bash
# Mermaid <br/> 標籤修復腳本
# 用途：將 Mermaid 程式碼區塊內的 <br/> 替換為 \\n

set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 計數器
total_files=0
modified_files=0

echo -e "${GREEN}開始修復 Mermaid 圖表中的 <br/> 標籤...${NC}"

# 找出所有 iGaming 文檔中的 Markdown 檔案
find docs/iGaming -name "*.md" -type f | while read -r file; do
    total_files=$((total_files + 1))

    # 檢查檔案是否包含 Mermaid 程式碼區塊和 <br/> 標籤
    if grep -q '```mermaid' "$file" && grep -q '<br/>' "$file"; then
        echo -e "${YELLOW}處理檔案: $file${NC}"

        # 使用 Python 腳本精準替換 Mermaid 區塊內的 <br/>
        py scripts/replace_mermaid_br.py "$file"

        if [ $? -eq 0 ]; then
            modified_files=$((modified_files + 1))
            echo -e "${GREEN}  ✓ 修復完成${NC}"
        fi
    fi
done

echo -e "${GREEN}修復完成！${NC}"
echo "處理的檔案數: $total_files"
echo "修改的檔案數: $modified_files"
