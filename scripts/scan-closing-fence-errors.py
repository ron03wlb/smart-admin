#!/usr/bin/env python3
"""
IGaming 文檔閉合標記錯誤掃描器
功能：檢測使用語言標識符作為閉合標記的錯誤（如 ```text 而非 ```）
"""

import os
import re
from pathlib import Path
from typing import List, Tuple

# 配置
TARGET_DIR = "docs/IGaming"
EXCLUDE_PATTERNS = [
    "*REPORT*",
    "CORRECTION*",
    "*backup*"
]

# 語言標識符模式（三個反引號 + 語言名稱）
FENCE_PATTERN = re.compile(r'^```([a-z]+)$')

def should_exclude(file_path: str) -> bool:
    """判斷文件是否應該被排除"""
    for pattern in EXCLUDE_PATTERNS:
        if pattern.replace('*', '') in file_path:
            return True
    return False

def is_closing_fence_error(lines: List[str], line_idx: int, language: str) -> bool:
    """
    判斷是否為閉合標記錯誤

    判斷邏輯：
    1. 前一行不為空（有內容）
    2. 後一行為空、分隔符（---）或標題（#開頭）
    """
    # 檢查邊界
    if line_idx == 0 or line_idx >= len(lines) - 1:
        return False

    prev_line = lines[line_idx - 1].strip()
    next_line = lines[line_idx + 1].strip() if line_idx + 1 < len(lines) else ""

    # 前一行有內容（不為空）
    has_prev_content = bool(prev_line)

    # 後一行為空或分隔符或標題
    next_is_separator = (
        not next_line or  # 空行
        next_line.startswith('---') or  # 分隔符
        next_line.startswith('#')  # 標題
    )

    return has_prev_content and next_is_separator

def scan_file(file_path: Path) -> List[Tuple[int, str, str]]:
    """
    掃描單個文件

    返回：[(line_number, language, context), ...]
    """
    errors = []

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        for i, line in enumerate(lines):
            match = FENCE_PATTERN.match(line.strip())
            if match:
                language = match.group(1)

                # 檢查是否為閉合標記錯誤
                if is_closing_fence_error(lines, i, language):
                    # 獲取上下文（前一行內容）
                    context = lines[i - 1].strip()[:50] if i > 0 else ""
                    errors.append((i + 1, language, context))  # 行號從 1 開始

    except Exception as e:
        print(f"❌ 讀取文件失敗: {file_path}, 錯誤: {e}")

    return errors

def scan_directory(target_dir: str) -> dict:
    """
    掃描整個目錄

    返回：{file_path: [(line_number, language, context), ...], ...}
    """
    target_path = Path(target_dir)
    all_errors = {}

    # 遍歷所有 .md 文件
    for md_file in target_path.rglob("*.md"):
        # 排除審計報告等文件
        if should_exclude(str(md_file)):
            continue

        errors = scan_file(md_file)
        if errors:
            # 使用相對路徑
            relative_path = md_file.relative_to(target_path.parent)
            all_errors[str(relative_path)] = errors

    return all_errors

def generate_report(all_errors: dict) -> str:
    """生成掃描報告"""
    report_lines = []
    report_lines.append("=" * 80)
    report_lines.append("IGaming 文檔閉合標記錯誤掃描報告")
    report_lines.append("=" * 80)
    report_lines.append("")

    # 統計
    total_files = len(all_errors)
    total_errors = sum(len(errors) for errors in all_errors.values())

    # 按語言類型統計
    lang_stats = {}
    for errors in all_errors.values():
        for _, lang, _ in errors:
            lang_stats[lang] = lang_stats.get(lang, 0) + 1

    report_lines.append(f"📊 掃描統計")
    report_lines.append(f"  - 受影響文件數: {total_files} 個")
    report_lines.append(f"  - 總錯誤數: {total_errors} 處")
    report_lines.append("")

    report_lines.append(f"📈 錯誤類型分布")
    for lang, count in sorted(lang_stats.items(), key=lambda x: -x[1]):
        percentage = (count / total_errors * 100) if total_errors > 0 else 0
        report_lines.append(f"  - ```{lang}: {count} 處 ({percentage:.1f}%)")
    report_lines.append("")

    report_lines.append("=" * 80)
    report_lines.append("📁 詳細錯誤清單")
    report_lines.append("=" * 80)
    report_lines.append("")

    # 按文件列出錯誤
    for file_path, errors in sorted(all_errors.items()):
        report_lines.append(f"文件: {file_path}")
        report_lines.append(f"  錯誤數: {len(errors)} 處")
        report_lines.append("")

        for line_num, lang, context in errors:
            report_lines.append(f"  Line {line_num}: ```{lang}")
            if context:
                report_lines.append(f"    上下文: {context}...")
        report_lines.append("")

    report_lines.append("=" * 80)
    report_lines.append("✅ 掃描完成")
    report_lines.append("=" * 80)

    return "\n".join(report_lines)

def main():
    print("🚀 開始掃描 IGaming 文檔...")
    print(f"📂 目標目錄: {TARGET_DIR}")
    print("")

    # 執行掃描
    all_errors = scan_directory(TARGET_DIR)

    # 生成報告
    report = generate_report(all_errors)

    # 輸出到控制台
    print(report)

    # 保存到文件
    output_file = "docs/IGaming/CLOSING_FENCE_ERRORS_REPORT.txt"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(report)

    print(f"\n📄 報告已保存至: {output_file}")

    # 返回錯誤數量（用於後續處理）
    total_errors = sum(len(errors) for errors in all_errors.values())
    return total_errors, all_errors

if __name__ == "__main__":
    total_errors, all_errors = main()

    if total_errors > 0:
        print(f"\n⚠️  發現 {total_errors} 處閉合標記錯誤")
        exit(1)
    else:
        print("\n✅ 未發現閉合標記錯誤")
        exit(0)
