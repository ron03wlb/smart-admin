#!/usr/bin/env python3
"""
Mermaid <br/> 標籤修復工具
精準替換 Mermaid 程式碼區塊內的 <br/> 為 \\n
保留非 Mermaid 區塊中的 HTML 標籤
"""

import re
import sys
from pathlib import Path

def fix_mermaid_br_tags(content: str) -> tuple:
    """
    修復 Mermaid 程式碼區塊中的 <br/> 標籤

    返回: (修復後的內容, 替換次數)
    """
    replacements = 0

    def replace_br_in_mermaid(match):
        nonlocal replacements
        mermaid_block = match.group(1)

        # 計算替換次數
        br_count = mermaid_block.count('<br/>')
        replacements += br_count

        # 替換 <br/> 為 \\n
        fixed_block = mermaid_block.replace('<br/>', '\\n')

        return f'```mermaid\n{fixed_block}```'

    # 正規表達式：匹配 ```mermaid ... ``` 區塊（支援多行）
    pattern = r'```mermaid\n(.*?)```'
    fixed_content = re.sub(pattern, replace_br_in_mermaid, content, flags=re.DOTALL)

    return fixed_content, replacements

def main():
    if len(sys.argv) != 2:
        print("Usage: python3 replace_mermaid_br.py <file_path>")
        sys.exit(1)

    file_path = Path(sys.argv[1])

    if not file_path.exists():
        print(f"錯誤: 檔案不存在 - {file_path}")
        sys.exit(1)

    # 讀取檔案
    content = file_path.read_text(encoding='utf-8')

    # 修復 Mermaid <br/> 標籤
    fixed_content, replacements = fix_mermaid_br_tags(content)

    if replacements > 0:
        # 寫回檔案
        file_path.write_text(fixed_content, encoding='utf-8')
        print(f"  替換了 {replacements} 個 <br/> 標籤")
        sys.exit(0)
    else:
        print("  無需修改")
        sys.exit(1)

if __name__ == '__main__':
    main()
