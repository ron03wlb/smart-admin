#!/usr/bin/env python3
"""
Mermaid Style 語法修復工具

功能：
1. 檢測節點 ID 包含空格但未加引號
2. 自動添加雙引號
3. 生成修復報告

使用方式：
python fix_style_syntax.py <file_path>
"""

import re
import sys
from pathlib import Path
from typing import Tuple

def fix_style_node_quotes(line: str) -> Tuple[str, bool]:
    """為包含空格的節點 ID 添加雙引號"""
    pattern = r'^(\s*)(style)\s+([^"][^\s]+(?:\s+[^"]+)+)\s+(fill:.*)$'
    match = re.match(pattern, line)

    if match:
        indent = match.group(1)
        keyword = match.group(2)
        node_id = match.group(3).strip()
        style_def = match.group(4)

        fixed_line = f'{indent}{keyword} "{node_id}" {style_def}'
        return fixed_line, True

    return line, False

def clean_color_pollution(line: str) -> Tuple[str, bool]:
    """清理顏色碼中的 "Bonus" 文本污染"""
    if 'Bonus' not in line or 'fill:#' not in line:
        return line, False

    cleaned = re.sub(r'(fill:#[0-9A-Fa-f]*)Bonus+', r'\1', line)

    if cleaned != line:
        return cleaned, True

    return line, False

def fix_file(file_path: Path) -> Tuple[int, int]:
    """修復單個文件，返回 (quote_fixes, color_fixes)"""
    content = file_path.read_text(encoding='utf-8')
    lines = content.split('\n')

    quote_fixes = 0
    color_fixes = 0
    fixed_lines = []

    for line in lines:
        # 嘗試修復節點引號
        fixed_line, quote_fixed = fix_style_node_quotes(line)
        if quote_fixed:
            quote_fixes += 1
            line = fixed_line

        # 嘗試清理顏色碼污染
        fixed_line, color_fixed = clean_color_pollution(line)
        if color_fixed:
            color_fixes += 1
            line = fixed_line

        fixed_lines.append(line)

    # 寫回文件
    file_path.write_text('\n'.join(fixed_lines), encoding='utf-8')

    return quote_fixes, color_fixes

def main():
    if len(sys.argv) != 2:
        print("Usage: python fix_style_syntax.py <file_path>")
        sys.exit(1)

    file_path = Path(sys.argv[1])

    if not file_path.exists():
        print(f"錯誤: 文件不存在 - {file_path}")
        sys.exit(1)

    quote_fixes, color_fixes = fix_file(file_path)

    print(f"處理文件: {file_path}")
    print(f"  節點引號修復: {quote_fixes} 處")
    print(f"  顏色碼清理: {color_fixes} 處")
    print(f"  總計修復: {quote_fixes + color_fixes} 處")

if __name__ == '__main__':
    main()
