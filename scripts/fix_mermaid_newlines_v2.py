#!/usr/bin/env python3
"""
Mermaid 換行符精準修復工具 v2
===============================

用途：
1. sequenceDiagram Note 區塊：\\n → <br/>
2. graph/flowchart 節點標籤：確保使用雙引號包圍 \\n

使用方法：
    python fix_mermaid_newlines_v2.py <file_path>
    python fix_mermaid_newlines_v2.py --dry-run <file_path>
"""

import re
import sys
from pathlib import Path
from typing import Tuple


def fix_sequence_diagram_notes(content: str) -> Tuple[str, int]:
    """
    修復 sequenceDiagram 的 Note 區塊
    將 \\n 改回 <br/>

    返回: (修復後的內容, Note 區塊修復數量)
    """
    note_fixes = 0

    # 正規表達式：匹配 sequenceDiagram 區塊
    pattern = r'```mermaid\n([\s\S]*?sequenceDiagram[\s\S]*?)```'

    def fix_notes_in_sequence(match):
        nonlocal note_fixes
        block = match.group(1)

        # 匹配 Note 語句中的 \\n
        # Note over XXX: Text\\nMore Text → Note over XXX: Text<br/>More Text
        # 需要匹配多個 \\n（例如：Text\\nLine2\\nLine3）
        def replace_backslash_n(note_match):
            nonlocal note_fixes
            note_line = note_match.group(0)
            if '\\n' in note_line:
                note_fixes += note_line.count('\\n')
                return note_line.replace('\\n', '<br/>')
            return note_line

        # 匹配 Note 開頭到行尾的所有內容
        note_pattern = r'(Note (?:over|left of|right of) [^:]+:.*?)(?=\n|$)'
        fixed = re.sub(note_pattern, replace_backslash_n, block, flags=re.MULTILINE)

        return f'```mermaid\n{fixed}```'

    fixed_content = re.sub(pattern, fix_notes_in_sequence, content, flags=re.DOTALL)
    return fixed_content, note_fixes


def ensure_double_quotes_in_graph_nodes(content: str) -> Tuple[str, int]:
    """
    確保 graph/flowchart 節點標籤使用雙引號包圍 \\n

    返回: (修復後的內容, 節點修復數量)
    """
    node_fixes = 0

    # 匹配 graph/flowchart 區塊
    pattern = r'```mermaid\n([\s\S]*?(?:graph|flowchart)[\s\S]*?)```'

    def fix_nodes(match):
        nonlocal node_fixes
        block = match.group(1)

        # 匹配節點定義：A[Text\\nMore] 或 A{Text\\nMore} 或 A(Text\\nMore)
        # 只修改沒有雙引號且包含 \\n 的節點
        # 模式：NODE_ID + 括號類型 + 內容（包含\\n且無雙引號）+ 括號結束

        # 方括號節點: A[Text\\nMore] → A["Text\\nMore"]
        node_pattern_square = r'([A-Z_][A-Z0-9_]*)\[([^"\[\]]*\\n[^"\[\]]*)\]'

        def add_quotes_square(m):
            nonlocal node_fixes
            node_fixes += 1
            return f'{m.group(1)}["{m.group(2)}"]'

        block = re.sub(node_pattern_square, add_quotes_square, block)

        # 花括號決策節點: B{Text\\nMore} → B["Text\\nMore"] (Mermaid decision nodes also need quotes)
        node_pattern_curly = r'([A-Z_][A-Z0-9_]*)\{([^"\{\}]*\\n[^"\{\}]*)\}'

        def add_quotes_curly(m):
            nonlocal node_fixes
            node_fixes += 1
            # Decision nodes use {} but content still needs quotes if multi-line
            # Actually, {} decision nodes should use {"text\nline2"} format
            return f'{m.group(1)}{{"{m.group(2)}"}}'

        block = re.sub(node_pattern_curly, add_quotes_curly, block)

        # 圓括號節點: C(Text\\nMore) → C("Text\\nMore")
        node_pattern_round = r'([A-Z_][A-Z0-9_]*)\(([^"\(\)]*\\n[^"\(\)]*)\)'

        def add_quotes_round(m):
            nonlocal node_fixes
            node_fixes += 1
            return f'{m.group(1)}("{m.group(2)}")'

        block = re.sub(node_pattern_round, add_quotes_round, block)

        return f'```mermaid\n{block}```'

    fixed_content = re.sub(pattern, fix_nodes, content, flags=re.DOTALL)
    return fixed_content, node_fixes


def fix_mermaid_file(file_path: Path, dry_run: bool = False) -> Tuple[int, int]:
    """
    修復單個檔案的 Mermaid 換行問題

    返回: (Note 修復數, 節點修復數)
    """
    if not file_path.exists():
        print(f"錯誤: 檔案不存在 - {file_path}")
        sys.exit(1)

    # 讀取檔案
    content = file_path.read_text(encoding='utf-8')
    original_content = content

    # 步驟 1: 修復 sequenceDiagram Note 區塊
    content, note_fixes = fix_sequence_diagram_notes(content)

    # 步驟 2: 確保 graph/flowchart 節點有雙引號
    content, node_fixes = ensure_double_quotes_in_graph_nodes(content)

    total_fixes = note_fixes + node_fixes

    if total_fixes == 0:
        print("  無需修改")
        return 0, 0

    if dry_run:
        print(f"  [DRY RUN] 將修復:")
        if note_fixes > 0:
            print(f"    - sequenceDiagram Note: {note_fixes} 處 \\n → <br/>")
        if node_fixes > 0:
            print(f"    - graph/flowchart 節點: {node_fixes} 個添加雙引號")
        print(f"  總計: {total_fixes} 處修復")
        return note_fixes, node_fixes

    # 寫回檔案
    file_path.write_text(content, encoding='utf-8')

    print(f"  修復了 {total_fixes} 處:")
    if note_fixes > 0:
        print(f"    - sequenceDiagram Note: {note_fixes} 處 \\n → <br/>")
    if node_fixes > 0:
        print(f"    - graph/flowchart 節點: {node_fixes} 個添加雙引號")

    return note_fixes, node_fixes


def main():
    """主函數"""
    if len(sys.argv) < 2:
        print("用法: python fix_mermaid_newlines_v2.py [--dry-run] <file_path>")
        sys.exit(1)

    dry_run = False
    file_arg_index = 1

    if sys.argv[1] == '--dry-run':
        dry_run = True
        file_arg_index = 2
        if len(sys.argv) < 3:
            print("錯誤: 需要提供檔案路徑")
            print("用法: python fix_mermaid_newlines_v2.py --dry-run <file_path>")
            sys.exit(1)

    file_path = Path(sys.argv[file_arg_index])

    print(f"{'[DRY RUN] ' if dry_run else ''}處理檔案: {file_path}")

    note_fixes, node_fixes = fix_mermaid_file(file_path, dry_run)

    if note_fixes + node_fixes == 0:
        sys.exit(1)  # 無需修改
    else:
        sys.exit(0)  # 修復成功


if __name__ == '__main__':
    main()
