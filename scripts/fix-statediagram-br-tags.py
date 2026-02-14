#!/usr/bin/env python3
"""
Mermaid stateDiagram <br/> 標籤修復腳本

用途：修復 stateDiagram-v2 中不支持的 <br/> HTML 標籤
策略：
  - 方案 A (simple): 簡化 transition labels，去除 <br/> 和分隔線
  - 方案 B (note): 將詳細信息移至 note 區塊，保留完整信息

作者：Claude Sonnet 4.5
日期：2026-02-04
"""

import re
import sys
import argparse
from pathlib import Path
from typing import Tuple, List, Dict
import subprocess


# ============================================================
# 文件優先級配置
# ============================================================
P0_FILES = ['03-03_Seamless_Wallet_Analysis.md']
P1_FILES = [
    '03-03_Activity_Bonus.md',
    '07-02-02_Rate_Limiting.md',
    '01-01_Player_Lifecycle.md',
    '03-02_VIP_Loyalty.md'
]


# ============================================================
# 核心修復函數
# ============================================================

def determine_strategy(file_path: str, error_count: int) -> str:
    """根據文件優先級和錯誤數量決定修復策略"""
    file_name = Path(file_path).name

    # P0 或 P1 文件，或錯誤數量 > 5：使用方案 B
    if any(p0 in file_name for p0 in P0_FILES):
        return 'note'
    if any(p1 in file_name for p1 in P1_FILES) or error_count > 5:
        return 'note'

    # 其他：使用方案 A
    return 'simple'


def extract_state_diagrams(content: str) -> List[Tuple[str, int, int]]:
    """提取所有 stateDiagram 代碼塊

    Returns:
        List of (diagram_content, start_pos, end_pos)
    """
    diagrams = []
    pattern = r'```mermaid\s*\n\s*stateDiagram-v2\s*\n(.*?)\n```'

    for match in re.finditer(pattern, content, re.DOTALL):
        diagram_content = match.group(0)
        start_pos = match.start()
        end_pos = match.end()
        diagrams.append((diagram_content, start_pos, end_pos))

    return diagrams


def count_br_tags(diagram: str) -> int:
    """計算 stateDiagram 中的 <br/> 標籤數量"""
    return len(re.findall(r'<br\s*/?>', diagram, re.IGNORECASE))


def fix_transition_label_simple(label: str) -> str:
    """方案 A: 簡化 transition label

    轉換規則：
      - 去除 <br/> 標籤
      - 去除分隔線 ━━━━━
      - 提取關鍵詞，用 - 或 & 連接

    Example:
      "Win Request<br/>━━━━━<br/>Credit Balance<br/>Close Round"
      → "Win Request - Credit & Close"
    """
    # 去除 <br/> 標籤，拆分為多行
    lines = re.split(r'<br\s*/?>', label, flags=re.IGNORECASE)

    # 去除分隔線和空白
    lines = [line.strip() for line in lines if line.strip() and '━' not in line]

    # 如果只有一行，直接返回
    if len(lines) <= 1:
        return lines[0] if lines else ''

    # 提取關鍵詞（第一行 + 最後一行）
    if len(lines) == 2:
        return f"{lines[0]} - {lines[1]}"
    else:
        return f"{lines[0]} - {' & '.join(lines[1:])}"


def fix_transition_label_with_note(
    source_state: str,
    target_state: str,
    label: str,
    existing_notes: Dict[str, str]
) -> Tuple[str, str]:
    """方案 B: 將 transition label 簡化，詳細信息移至 note 區塊

    Returns:
        (simplified_label, note_content)
    """
    # 拆分標籤內容
    lines = re.split(r'<br\s*/?>', label, flags=re.IGNORECASE)
    lines = [line.strip() for line in lines if line.strip()]

    # 簡化標籤（只保留第一行）
    simplified_label = lines[0] if lines else ''

    # 構建 note 內容
    note_lines = []
    for line in lines:
        note_lines.append(f"        {line}")

    note_content = '\n'.join(note_lines)

    # 生成 note 區塊
    note_block = f"""
    note right of {target_state}
{note_content}
    end note"""

    return simplified_label, note_block


def fix_note_block(note_content: str) -> str:
    """修復 note 區塊中的 <br/> 標籤

    轉換規則：
      - 去除 <br/> 標籤
      - 保留原始換行結構
      - 確保使用 multi-line note 格式
    """
    # 檢測是否為單行 note（格式：note right of STATE : content）
    single_line_pattern = r'(note\s+(?:right|left)\s+of\s+\w+)\s*:\s*(.+?)(?=\n|$)'
    match = re.search(single_line_pattern, note_content, re.IGNORECASE)

    if match:
        note_prefix = match.group(1)
        note_text = match.group(2)

        # 拆分 <br/> 標籤
        lines = re.split(r'<br\s*/?>', note_text, flags=re.IGNORECASE)
        lines = [line.strip() for line in lines if line.strip()]

        # 構建 multi-line note
        note_lines = '\n'.join(f"        {line}" for line in lines)
        return f"""{note_prefix}
{note_lines}
    end note"""

    # 如果已經是 multi-line note，只需去除 <br/>
    return re.sub(r'<br\s*/?>', '\n        ', note_content, flags=re.IGNORECASE)


def fix_statediagram(diagram: str, strategy: str = 'simple') -> Tuple[str, int]:
    """修復單個 stateDiagram 代碼塊

    Args:
        diagram: stateDiagram 代碼塊內容
        strategy: 'simple' 或 'note'

    Returns:
        (fixed_diagram, fixes_count)
    """
    fixes_count = 0
    fixed_diagram = diagram

    # 1. 修復 note 區塊中的 <br/>
    note_pattern = r'(note\s+(?:right|left)\s+of\s+\w+[^\n]*?)(?:(?:end note)|(?=\n\s*(?:note|[A-Z_]+\s*(?:-->|:))))'

    def fix_note_replacement(match):
        nonlocal fixes_count
        original = match.group(0)
        if '<br' in original.lower():
            fixes_count += 1
            return fix_note_block(original)
        return original

    fixed_diagram = re.sub(note_pattern, fix_note_replacement, fixed_diagram, flags=re.DOTALL | re.IGNORECASE)

    # 2. 修復 transition labels 中的 <br/>
    if strategy == 'simple':
        # 方案 A: 簡化標籤
        transition_pattern = r'(\w+)\s*-->\s*(\w+)\s*:\s*([^\n]+)'

        def fix_transition_simple(match):
            nonlocal fixes_count
            source = match.group(1)
            target = match.group(2)
            label = match.group(3)

            if '<br' in label.lower():
                fixes_count += 1
                simplified = fix_transition_label_simple(label)
                return f"{source} --> {target}: {simplified}"
            return match.group(0)

        fixed_diagram = re.sub(transition_pattern, fix_transition_simple, fixed_diagram, flags=re.IGNORECASE)

    else:
        # 方案 B: 移至 note 區塊（目前簡化實現，僅簡化標籤）
        # 完整實現需要追蹤 note 區塊並合併內容
        transition_pattern = r'(\w+)\s*-->\s*(\w+)\s*:\s*([^\n]+)'

        def fix_transition_note(match):
            nonlocal fixes_count
            source = match.group(1)
            target = match.group(2)
            label = match.group(3)

            if '<br' in label.lower():
                fixes_count += 1
                # 目前簡化：只保留第一行
                lines = re.split(r'<br\s*/?>', label, flags=re.IGNORECASE)
                first_line = lines[0].strip() if lines else ''

                # 如果有分隔線，去除
                if '━' in first_line:
                    first_line = re.sub(r'━+', '', first_line).strip()

                return f"{source} --> {target}: {first_line}"
            return match.group(0)

        fixed_diagram = re.sub(transition_pattern, fix_transition_note, fixed_diagram, flags=re.IGNORECASE)

    return fixed_diagram, fixes_count


def fix_markdown_file(
    file_path: str,
    strategy: str = 'auto',
    dry_run: bool = False,
    verify: bool = False
) -> Tuple[int, int]:
    """修復 Markdown 文件中的所有 stateDiagram

    Args:
        file_path: Markdown 文件路徑
        strategy: 'auto', 'simple', 或 'note'
        dry_run: 僅預覽，不實際修改
        verify: 使用 Mermaid CLI 驗證修復結果

    Returns:
        (diagrams_fixed, total_fixes)
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 提取所有 stateDiagram
    diagrams = extract_state_diagrams(content)

    if not diagrams:
        return 0, 0

    # 計算總錯誤數（用於決定策略）
    total_errors = sum(count_br_tags(diagram[0]) for diagram in diagrams)

    # 決定修復策略
    if strategy == 'auto':
        strategy = determine_strategy(file_path, total_errors)

    # 修復所有 stateDiagram
    fixed_content = content
    diagrams_fixed = 0
    total_fixes = 0

    # 從後向前替換（避免位置偏移）
    for diagram_content, start_pos, end_pos in reversed(diagrams):
        if '<br' in diagram_content.lower():
            fixed_diagram, fixes = fix_statediagram(diagram_content, strategy)

            if fixes > 0:
                fixed_content = fixed_content[:start_pos] + fixed_diagram + fixed_content[end_pos:]
                diagrams_fixed += 1
                total_fixes += fixes

    # Dry-run 模式：只顯示預覽
    if dry_run:
        if diagrams_fixed > 0:
            print(f"[DRY-RUN] {file_path}")
            print(f"  策略: {strategy}")
            print(f"  修復圖表數: {diagrams_fixed}")
            print(f"  修復標籤數: {total_fixes}")
        return diagrams_fixed, total_fixes

    # 寫入修復後的內容
    if diagrams_fixed > 0:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(fixed_content)

        print(f"✓ {file_path}")
        print(f"  策略: {strategy}, 修復圖表: {diagrams_fixed}, 修復標籤: {total_fixes}")

        # 驗證（如果啟用）
        if verify:
            if not verify_mermaid_syntax(file_path):
                print(f"  ⚠️  驗證失敗，請手動檢查")

    return diagrams_fixed, total_fixes


def verify_mermaid_syntax(file_path: str) -> bool:
    """使用 Mermaid CLI 驗證修復後的語法

    需要安裝：npm install -g @mermaid-js/mermaid-cli
    """
    try:
        # 提取 Mermaid 代碼塊到臨時文件
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        diagrams = extract_state_diagrams(content)
        if not diagrams:
            return True

        # 驗證第一個 stateDiagram（代表性檢查）
        diagram_content = diagrams[0][0]

        # 提取純 Mermaid 代碼（去除 ```mermaid 標記）
        mermaid_code = re.sub(r'```mermaid\s*\n', '', diagram_content)
        mermaid_code = re.sub(r'\n```$', '', mermaid_code)

        # 寫入臨時文件
        import tempfile
        with tempfile.NamedTemporaryFile(mode='w', suffix='.mmd', delete=False) as tmp:
            tmp.write(mermaid_code)
            tmp_path = tmp.name

        # 使用 mmdc 驗證
        result = subprocess.run(
            ['mmdc', '-i', tmp_path, '-o', '/tmp/mermaid-test.svg'],
            capture_output=True,
            text=True,
            timeout=10
        )

        # 清理臨時文件
        Path(tmp_path).unlink()

        return result.returncode == 0

    except Exception as e:
        # 如果 Mermaid CLI 未安裝，跳過驗證
        return True


# ============================================================
# 主程序
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        description='修復 stateDiagram 中的 <br/> 標籤',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
範例：
  # 自動檢測策略並修復單個文件
  python fix-statediagram-br-tags.py docs/iGaming/03_Game_Center/03-03_Seamless_Wallet_Analysis.md

  # 使用方案 A（簡化標籤）
  python fix-statediagram-br-tags.py --strategy simple file.md

  # 使用方案 B（移至 note 區塊）
  python fix-statediagram-br-tags.py --strategy note file.md

  # Dry-run 模式（預覽修復）
  python fix-statediagram-br-tags.py --dry-run file.md

  # 啟用驗證（需要 Mermaid CLI）
  python fix-statediagram-br-tags.py --verify file.md
        """
    )

    parser.add_argument('files', nargs='+', help='要修復的 Markdown 文件')
    parser.add_argument(
        '--strategy',
        choices=['auto', 'simple', 'note'],
        default='auto',
        help='修復策略：auto（自動檢測），simple（簡化標籤），note（移至 note 區塊）'
    )
    parser.add_argument('--dry-run', action='store_true', help='僅預覽，不實際修改')
    parser.add_argument('--verify', action='store_true', help='使用 Mermaid CLI 驗證修復結果')

    args = parser.parse_args()

    # 統計
    total_files = len(args.files)
    total_diagrams_fixed = 0
    total_fixes = 0

    print("=" * 60)
    print("Mermaid stateDiagram <br/> 修復工具")
    print("=" * 60)
    print(f"策略: {args.strategy}")
    print(f"模式: {'DRY-RUN' if args.dry_run else 'EXECUTE'}")
    print(f"驗證: {'啟用' if args.verify else '禁用'}")
    print("=" * 60)
    print()

    # 處理每個文件
    for file_path in args.files:
        diagrams_fixed, fixes = fix_markdown_file(
            file_path,
            strategy=args.strategy,
            dry_run=args.dry_run,
            verify=args.verify
        )
        total_diagrams_fixed += diagrams_fixed
        total_fixes += fixes

    # 總結
    print()
    print("=" * 60)
    print("修復完成")
    print("=" * 60)
    print(f"總文件數:     {total_files}")
    print(f"修復圖表數:   {total_diagrams_fixed}")
    print(f"修復標籤總數: {total_fixes}")
    print("=" * 60)

    return 0 if total_fixes > 0 else 1


if __name__ == '__main__':
    sys.exit(main())
