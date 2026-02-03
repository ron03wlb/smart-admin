#!/usr/bin/env python3
"""
Mermaid 批次修復工具

功能：
1. 掃描目錄下所有 Markdown 文件
2. 批次修復 Mermaid 語法錯誤
3. 生成修復報告

使用方式：
python batch_repair.py <directory_path>
"""

import sys
from pathlib import Path
from typing import List, Dict
import subprocess

def find_all_md_files(directory: Path) -> List[Path]:
    """找出所有 Markdown 文件（排除 archive）"""
    all_files = []
    for md_file in directory.rglob("*.md"):
        if 'archive' not in str(md_file):
            all_files.append(md_file)
    return sorted(all_files)

def repair_file(file_path: Path, script_path: Path) -> Dict:
    """修復單個文件"""
    try:
        result = subprocess.run(
            [sys.executable, str(script_path), str(file_path)],
            capture_output=True,
            text=True,
            encoding='utf-8'
        )

        if result.returncode == 0:
            # 解析輸出統計
            import re
            quote_match = re.search(r'節點引號修復: (\d+)', result.stdout)
            color_match = re.search(r'顏色碼清理: (\d+)', result.stdout)

            quote_fixes = int(quote_match.group(1)) if quote_match else 0
            color_fixes = int(color_match.group(1)) if color_match else 0

            if quote_fixes + color_fixes > 0:
                return {
                    'status': 'modified',
                    'quote_fixes': quote_fixes,
                    'color_fixes': color_fixes,
                    'file': file_path
                }
            else:
                return {'status': 'skipped', 'file': file_path}
        else:
            return {'status': 'error', 'file': file_path, 'error': result.stderr}

    except Exception as e:
        return {'status': 'error', 'file': file_path, 'error': str(e)}

def main():
    if len(sys.argv) != 2:
        print("Usage: python batch_repair.py <directory_path>")
        sys.exit(1)

    directory = Path(sys.argv[1])

    if not directory.exists() or not directory.is_dir():
        print(f"錯誤: 目錄不存在 - {directory}")
        sys.exit(1)

    # 查找 fix_style_syntax.py
    script_path = Path(__file__).parent / 'fix_style_syntax.py'
    if not script_path.exists():
        print(f"錯誤: 找不到 fix_style_syntax.py")
        sys.exit(1)

    print(f"批次修復開始...")
    print(f"掃描目錄: {directory}\n")

    all_files = find_all_md_files(directory)
    total_files = len(all_files)

    print(f"找到 {total_files} 個文件")
    print("="*70 + "\n")

    results = {
        'modified': [],
        'skipped': [],
        'errors': []
    }

    total_quote_fixes = 0
    total_color_fixes = 0

    for idx, file_path in enumerate(all_files, 1):
        print(f"[{idx}/{total_files}] 處理: {file_path.relative_to(directory)}")

        result = repair_file(file_path, script_path)

        if result['status'] == 'modified':
            results['modified'].append(result)
            total_quote_fixes += result['quote_fixes']
            total_color_fixes += result['color_fixes']
            print(f"  [OK] {result['quote_fixes']} 引號 + {result['color_fixes']} 顏色")
        elif result['status'] == 'error':
            results['errors'].append(result)
            print(f"  [ERROR] {result['error']}")
        else:
            results['skipped'].append(result)
            print(f"  [SKIP] No changes needed")

        print()

    # 輸出總結
    print("="*70)
    print("批次修復完成！\n")
    print(f"總文件數: {total_files}")
    print(f"已修改檔案: {len(results['modified'])}")
    print(f"  節點引號修復: {total_quote_fixes} 處")
    print(f"  顏色碼清理: {total_color_fixes} 處")
    print(f"無需修改: {len(results['skipped'])}")
    print(f"錯誤: {len(results['errors'])}\n")

    if results['modified']:
        print("修改的檔案清單:")
        for result in results['modified']:
            rel_path = result['file'].relative_to(directory)
            print(f"  - {rel_path} ({result['quote_fixes']} 引號 + {result['color_fixes']} 顏色)")

    if results['errors']:
        print("\n錯誤檔案清單:")
        for result in results['errors']:
            rel_path = result['file'].relative_to(directory)
            print(f"  - {rel_path}: {result['error']}")

if __name__ == '__main__':
    main()
