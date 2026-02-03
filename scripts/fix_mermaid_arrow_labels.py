#!/usr/bin/env python3
"""
Mermaid sequenceDiagram 箭頭標籤 \\n 修復工具
替換箭頭標籤中的 \\n 為 <br/>（不含雙引號的標籤）
"""

import re
import sys
from pathlib import Path
from typing import Tuple

def fix_arrow_labels_in_sequence(content: str) -> Tuple[str, int]:
    """
    修復 sequenceDiagram 箭頭標籤中的 \\n → <br/>
    只處理不含雙引號的箭頭標籤

    返回: (修復後的內容, 替換次數)
    """
    fixes = 0

    # 匹配 ```mermaid ... sequenceDiagram ... ``` 區塊
    pattern = r'```mermaid\n([\s\S]*?sequenceDiagram[\s\S]*?)```'

    def fix_arrows(match):
        nonlocal fixes
        block = match.group(1)

        # 匹配箭頭標籤模式（不含雙引號的情況）
        # 支持: A->>B:, A-->>B:, A->>+B:, A-->>-B: 等
        # 模式: [參與者]-[箭頭][參與者]: [文字內容含\n但無雙引號]

        # 先處理簡單的箭頭標籤替換（行內的 \n）
        lines = block.split('\n')
        fixed_lines = []

        for line in lines:
            # 跳過 Note 行（已在 Phase 2 修復）
            if re.match(r'\s*Note (over|left of|right of)', line):
                fixed_lines.append(line)
                continue

            # 檢查是否為箭頭標籤行，且含 \n 但無雙引號
            # 模式: 任意字符->>或-->>任意字符: 文字含\n
            if re.search(r'(?:->>?|->>?)(?:\+|-)?\s*[^:]+:', line):
                # 找到箭頭行
                # 檢查是否含 \n 且不在雙引號內
                if '\\n' in line:
                    # 分割為箭頭部分和內容部分
                    arrow_match = re.match(r'(.*?(?:->>?|->>?)(?:\+|-)?\s*[^:]+:\s*)(.+)', line)
                    if arrow_match:
                        prefix = arrow_match.group(1)
                        content = arrow_match.group(2)

                        # 檢查內容是否被雙引號包圍
                        if not (content.strip().startswith('"') and content.strip().endswith('"')):
                            # 沒有雙引號，替換 \n
                            newline_count = content.count('\\n')
                            if newline_count > 0:
                                fixes += newline_count
                                content = content.replace('\\n', '<br/>')
                            line = prefix + content

                fixed_lines.append(line)
            else:
                fixed_lines.append(line)

        fixed_block = '\n'.join(fixed_lines)
        return f'```mermaid\n{fixed_block}```'

    fixed_content = re.sub(pattern, fix_arrows, content, flags=re.DOTALL)
    return fixed_content, fixes

def main():
    if len(sys.argv) != 2:
        print("Usage: python3 fix_mermaid_arrow_labels.py <file_path>")
        sys.exit(1)

    file_path = Path(sys.argv[1])

    if not file_path.exists():
        print(f"錯誤: 檔案不存在 - {file_path}")
        sys.exit(1)

    # 讀取檔案
    content = file_path.read_text(encoding='utf-8')

    # 修復箭頭標籤
    fixed_content, replacements = fix_arrow_labels_in_sequence(content)

    if replacements > 0:
        # 寫回檔案
        file_path.write_text(fixed_content, encoding='utf-8')
        print(f"處理檔案: {file_path}")
        print(f"  替換了 {replacements} 個箭頭標籤中的 \\n → <br/>")
        sys.exit(0)
    else:
        print(f"處理檔案: {file_path}")
        print(f"  無需修改")
        sys.exit(1)

if __name__ == '__main__':
    main()
