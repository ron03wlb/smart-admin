#!/usr/bin/env python3
"""
Mermaid Emoji Fixer - 自動修復 Mermaid 圖表中的 emoji
═══════════════════════════════════════════════════════════════
用途: 將 Mermaid 圖表中的 emoji 替換為安全的替代品
用法: ./fix-mermaid-emoji.py [選項] <文件路徑>

選項:
  --strategy <策略>    修復策略 (css/text/hybrid)
  --dry-run           預覽模式（不修改文件）
  --verify            使用 Mermaid CLI 驗證修復結果
  -v, --verbose       詳細輸出

策略說明:
  - css: 使用 CSS classDef 樣式（適用於 P0 文件）
  - text: 純文字替換（適用於 P2 文件）
  - hybrid: 混合策略（適用於 P1 文件）

範例:
  ./fix-mermaid-emoji.py --strategy hybrid --dry-run docs/iGaming/05-06_MFA_Implementation.md
  ./fix-mermaid-emoji.py --strategy css --verify docs/iGaming/03-02_VIP_Loyalty.md
═══════════════════════════════════════════════════════════════
"""

import re
import json
import sys
import argparse
import subprocess
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass


@dataclass
class EmojiReplacement:
    """Emoji 替換規則"""
    text: str
    css_class: str
    css_style: str


class MermaidEmojiFixer:
    """Mermaid Emoji 修復器"""

    def __init__(self, mapping_file: Path, verbose: bool = False):
        self.verbose = verbose
        self.emoji_map = self._load_emoji_mapping(mapping_file)
        self.fix_count = 0
        self.error_count = 0

    def _load_emoji_mapping(self, mapping_file: Path) -> Dict[str, Dict[str, EmojiReplacement]]:
        """載入 emoji 語義映射"""
        if not mapping_file.exists():
            raise FileNotFoundError(f"Emoji mapping file not found: {mapping_file}")

        with open(mapping_file, 'r', encoding='utf-8') as f:
            raw_map = json.load(f)

        # 轉換為 EmojiReplacement 對象
        emoji_map = {}
        for category, mappings in raw_map.items():
            emoji_map[category] = {}
            for emoji, replacement in mappings.items():
                emoji_map[category][emoji] = EmojiReplacement(
                    text=replacement['text'],
                    css_class=replacement['class'],
                    css_style=replacement['style']
                )

        if self.verbose:
            print(f"✓ 載入 {sum(len(m) for m in emoji_map.values())} 個 emoji 映射規則")

        return emoji_map

    def detect_mermaid_blocks(self, content: str) -> List[Tuple[int, int, str]]:
        """
        提取 Mermaid 代碼塊及行號
        返回: [(start_line, end_line, block_content), ...]
        """
        blocks = []
        pattern = r'```mermaid\n(.*?)```'

        for match in re.finditer(pattern, content, re.DOTALL):
            start_line = content[:match.start()].count('\n') + 1
            block_content = match.group(1)
            end_line = start_line + block_content.count('\n')
            blocks.append((start_line, end_line, block_content))

        return blocks

    def count_emoji_in_text(self, text: str) -> int:
        """計算文本中的 emoji 數量"""
        count = 0
        for category_map in self.emoji_map.values():
            for emoji in category_map.keys():
                count += text.count(emoji)
        return count

    def replace_emoji_in_node_label(self, text: str, strategy: str) -> Tuple[str, List[str]]:
        """
        替換節點標籤中的 emoji
        返回: (替換後的文本, 需要添加的 CSS 類列表)
        """
        css_classes_needed = []
        modified_text = text

        for category, category_map in self.emoji_map.items():
            for emoji, replacement in category_map.items():
                if emoji in modified_text:
                    if strategy == 'css':
                        # CSS 策略：替換 emoji 為文字，收集 CSS 類
                        modified_text = modified_text.replace(emoji, replacement.text)
                        css_classes_needed.append(replacement.css_class)

                    elif strategy == 'text':
                        # 純文字策略：簡單替換
                        modified_text = modified_text.replace(emoji, replacement.text)

                    elif strategy == 'hybrid':
                        # 混合策略：狀態指示器和交通燈用 CSS，其他用文字
                        if category in ['status_indicators', 'traffic_lights']:
                            modified_text = modified_text.replace(emoji, replacement.text)
                            css_classes_needed.append(replacement.css_class)
                        else:
                            modified_text = modified_text.replace(emoji, replacement.text)

        return modified_text, css_classes_needed

    def generate_css_definitions(self, css_classes: List[str]) -> str:
        """生成 CSS classDef 定義"""
        if not css_classes:
            return ""

        css_defs = []
        class_assignments = []

        # 去重
        unique_classes = list(set(css_classes))

        for css_class in unique_classes:
            # 找到對應的樣式
            for category_map in self.emoji_map.values():
                for emoji, replacement in category_map.items():
                    if replacement.css_class == css_class:
                        css_defs.append(f"    classDef {css_class} {replacement.css_style}")
                        break

        if css_defs:
            return "\n\n" + "\n".join(css_defs)
        return ""

    def fix_mermaid_block(self, block: str, strategy: str) -> Tuple[str, bool]:
        """
        修復單個 Mermaid 代碼塊
        返回: (修復後的代碼塊, 是否有修改)
        """
        original_block = block
        emoji_count_before = self.count_emoji_in_text(block)

        if emoji_count_before == 0:
            return block, False

        modified_block = block
        all_css_classes = []

        # 使用正則提取節點標籤
        # 匹配格式: ID[Label] 或 ID["Label"] 或 ID(Label) 等
        node_pattern = r'(\w+)(\[|\(|>|\{)([^\]\)\}>]+)(\]|\)|>|\})'

        def replace_node_label(match):
            node_id = match.group(1)
            open_bracket = match.group(2)
            label = match.group(3)
            close_bracket = match.group(4)

            # 替換 label 中的 emoji
            new_label, css_classes = self.replace_emoji_in_node_label(label, strategy)
            all_css_classes.extend(css_classes)

            return f"{node_id}{open_bracket}{new_label}{close_bracket}"

        modified_block = re.sub(node_pattern, replace_node_label, modified_block)

        # 如果使用 CSS 策略，添加 CSS 定義
        if strategy in ['css', 'hybrid'] and all_css_classes:
            css_defs = self.generate_css_definitions(all_css_classes)
            modified_block += css_defs

        emoji_count_after = self.count_emoji_in_text(modified_block)
        has_changes = emoji_count_after < emoji_count_before

        if self.verbose and has_changes:
            print(f"  ✓ 替換 {emoji_count_before - emoji_count_after} 個 emoji")

        return modified_block, has_changes

    def fix_file(self, file_path: Path, strategy: str, dry_run: bool = False) -> int:
        """
        修復單個文件
        返回: 修復的代碼塊數量
        """
        if not file_path.exists():
            print(f"❌ 文件不存在: {file_path}", file=sys.stderr)
            self.error_count += 1
            return 0

        # 讀取文件
        try:
            content = file_path.read_text(encoding='utf-8')
        except Exception as e:
            print(f"❌ 讀取文件失敗: {file_path} - {e}", file=sys.stderr)
            self.error_count += 1
            return 0

        # 檢測 Mermaid 代碼塊
        blocks = self.detect_mermaid_blocks(content)

        if not blocks:
            if self.verbose:
                print(f"⚠ 文件中未找到 Mermaid 代碼塊: {file_path}")
            return 0

        # 修復每個代碼塊
        modified_content = content
        fixed_block_count = 0

        for start_line, end_line, block in blocks:
            fixed_block, has_changes = self.fix_mermaid_block(block, strategy)

            if has_changes:
                # 替換原始代碼塊
                modified_content = modified_content.replace(
                    f'```mermaid\n{block}```',
                    f'```mermaid\n{fixed_block}```',
                    1  # 只替換第一次出現
                )
                fixed_block_count += 1

                if self.verbose:
                    print(f"  修復代碼塊: Line {start_line}-{end_line}")

        if fixed_block_count == 0:
            if self.verbose:
                print(f"✓ 文件無需修復: {file_path}")
            return 0

        # 寫入文件
        if dry_run:
            print(f"[DRY-RUN] {file_path}: {fixed_block_count} 個代碼塊將被修復")
        else:
            try:
                file_path.write_text(modified_content, encoding='utf-8')
                print(f"✓ 已修復: {file_path} ({fixed_block_count} 個代碼塊)")
                self.fix_count += fixed_block_count
            except Exception as e:
                print(f"❌ 寫入文件失敗: {file_path} - {e}", file=sys.stderr)
                self.error_count += 1
                return 0

        return fixed_block_count

    def verify_with_mermaid_cli(self, file_path: Path) -> bool:
        """使用 Mermaid CLI 驗證修復結果"""
        try:
            # 提取 Mermaid 代碼塊到臨時文件
            content = file_path.read_text(encoding='utf-8')
            blocks = self.detect_mermaid_blocks(content)

            if not blocks:
                return True

            # 驗證第一個代碼塊（簡化）
            _, _, first_block = blocks[0]
            temp_mmd = Path("/tmp/temp_mermaid_verify.mmd")
            temp_mmd.write_text(first_block, encoding='utf-8')

            # 運行 mmdc 驗證
            result = subprocess.run(
                ['mmdc', '-i', str(temp_mmd), '-o', '/tmp/temp_output.svg', '-q'],
                capture_output=True,
                text=True,
                timeout=10
            )

            temp_mmd.unlink(missing_ok=True)
            Path('/tmp/temp_output.svg').unlink(missing_ok=True)

            if result.returncode == 0:
                print(f"  ✓ Mermaid CLI 驗證通過")
                return True
            else:
                print(f"  ❌ Mermaid CLI 驗證失敗: {result.stderr}")
                return False

        except FileNotFoundError:
            print("  ⚠ Mermaid CLI 未安裝，跳過驗證")
            return True
        except subprocess.TimeoutExpired:
            print("  ❌ Mermaid CLI 驗證超時")
            return False
        except Exception as e:
            print(f"  ❌ Mermaid CLI 驗證錯誤: {e}")
            return False


def main():
    parser = argparse.ArgumentParser(
        description='修復 Mermaid 圖表中的 emoji',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    parser.add_argument('files', nargs='+', help='要修復的文件路徑')
    parser.add_argument('--strategy', choices=['css', 'text', 'hybrid'],
                        default='text', help='修復策略 (默認: text)')
    parser.add_argument('--dry-run', action='store_true',
                        help='預覽模式（不修改文件）')
    parser.add_argument('--verify', action='store_true',
                        help='使用 Mermaid CLI 驗證修復結果')
    parser.add_argument('-v', '--verbose', action='store_true',
                        help='詳細輸出')

    args = parser.parse_args()

    # 獲取腳本所在目錄
    script_dir = Path(__file__).parent
    mapping_file = script_dir / 'emoji-replacement-map.json'

    try:
        fixer = MermaidEmojiFixer(mapping_file, verbose=args.verbose)
    except FileNotFoundError as e:
        print(f"❌ 錯誤: {e}", file=sys.stderr)
        print(f"請確保 emoji-replacement-map.json 存在於: {script_dir}", file=sys.stderr)
        return 1

    print("═══════════════════════════════════════════════════════════════")
    print(f"  Mermaid Emoji Fixer")
    print("═══════════════════════════════════════════════════════════════")
    print(f"策略: {args.strategy}")
    print(f"模式: {'預覽模式 (不修改文件)' if args.dry_run else '修復模式'}")
    print(f"驗證: {'啟用 Mermaid CLI 驗證' if args.verify else '跳過驗證'}")
    print()

    total_fixed = 0
    total_files = len(args.files)

    for file_path_str in args.files:
        file_path = Path(file_path_str)
        print(f"處理文件: {file_path}")

        fixed_count = fixer.fix_file(file_path, args.strategy, args.dry_run)
        total_fixed += fixed_count

        if args.verify and fixed_count > 0 and not args.dry_run:
            fixer.verify_with_mermaid_cli(file_path)

        print()

    print("═══════════════════════════════════════════════════════════════")
    print(f"  修復統計")
    print("═══════════════════════════════════════════════════════════════")
    print(f"處理文件數: {total_files}")
    print(f"修復代碼塊數: {total_fixed}")
    print(f"錯誤數: {fixer.error_count}")
    print()

    if args.dry_run:
        print("✓ 預覽完成（文件未修改）")
    else:
        print("✓ 修復完成")

    return 0 if fixer.error_count == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
