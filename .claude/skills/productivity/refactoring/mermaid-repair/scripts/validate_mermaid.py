#!/usr/bin/env python3
"""
Mermaid 語法驗證工具

功能：
1. 解析 Markdown 文件中的 Mermaid 代碼塊
2. 檢測語法錯誤
3. 生成驗證報告

使用方式：
python validate_mermaid.py <file_path>
"""

import re
import sys
from pathlib import Path
from typing import List, Dict

def extract_mermaid_blocks(content: str) -> List[Dict]:
    """提取所有 Mermaid 代碼塊"""
    pattern = r'```mermaid\n([\s\S]*?)```'
    blocks = []

    for match in re.finditer(pattern, content):
        block_content = match.group(1)
        start_pos = content[:match.start()].count('\n') + 1

        blocks.append({
            'content': block_content,
            'start_line': start_pos,
            'end_line': start_pos + block_content.count('\n')
        })

    return blocks

def validate_block(block: Dict) -> List[Dict]:
    """驗證單個 Mermaid 代碼塊"""
    errors = []
    lines = block['content'].split('\n')

    for line_num, line in enumerate(lines, 1):
        absolute_line = block['start_line'] + line_num

        # 檢查節點名空格未加引號（Type A）
        if re.search(r'^\s*style\s+[^"]*\s[^"]*fill:', line):
            errors.append({
                'line': absolute_line,
                'type': 'style_quote',
                'severity': '🔴',
                'message': 'Type A: 節點名包含空格但未加引號',
                'content': line.strip()
            })

        # 檢查顏色碼污染（Type B）
        if re.search(r'fill:#[0-9A-Fa-f]*Bonus', line):
            errors.append({
                'line': absolute_line,
                'type': 'color_pollution',
                'severity': '🔴',
                'message': 'Type B: 顏色碼被 "Bonus" 污染',
                'content': line.strip()
            })

        # 檢查逗號分隔的樣式屬性（Type D）
        if re.search(r'^\s*style\s+\S+\s+fill:#[0-9A-Fa-f]+,', line):
            errors.append({
                'line': absolute_line,
                'type': 'comma_separated_properties',
                'severity': '🔴',
                'message': 'Type D: 逗號分隔的樣式屬性（Mermaid 不支持）',
                'content': line.strip()
            })

    return errors

def main():
    if len(sys.argv) != 2:
        print("Usage: python validate_mermaid.py <file_path>")
        sys.exit(1)

    file_path = Path(sys.argv[1])

    if not file_path.exists():
        print(f"錯誤: 文件不存在 - {file_path}")
        sys.exit(1)

    content = file_path.read_text(encoding='utf-8')
    blocks = extract_mermaid_blocks(content)

    print(f"驗證文件: {file_path}")
    print(f"找到 {len(blocks)} 個 Mermaid 代碼塊\n")

    all_errors = []
    for block in blocks:
        errors = validate_block(block)
        all_errors.extend(errors)

    if not all_errors:
        print("✅ 語法驗證通過，未發現錯誤")
        sys.exit(0)

    print(f"❌ 發現 {len(all_errors)} 個錯誤：\n")

    for error in all_errors:
        print(f"{error['severity']} 行 {error['line']}: {error['message']}")
        print(f"   {error['content']}\n")

    sys.exit(1)

if __name__ == '__main__':
    main()
