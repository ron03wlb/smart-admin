#!/usr/bin/env python3
"""
Extract Mermaid blocks from iGaming documentation and perform basic validation.
"""

import os
import re
from pathlib import Path
from typing import List, Dict, Tuple

def extract_mermaid_blocks(file_path: str) -> List[Tuple[int, str]]:
    """Extract all Mermaid code blocks from a markdown file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    blocks = []
    # Match ```mermaid ... ``` blocks with their line numbers
    pattern = r'```mermaid\n(.*?)```'
    matches = re.finditer(pattern, content, re.DOTALL)

    for match in matches:
        # Calculate line number
        start_pos = match.start()
        line_num = content[:start_pos].count('\n') + 1
        block_content = match.group(1)
        blocks.append((line_num, block_content))

    return blocks

def basic_mermaid_validation(block: str) -> List[str]:
    """Perform basic validation on a Mermaid block."""
    errors = []

    # Check for stateDiagram-v2 with <br/> tags
    if 'stateDiagram-v2' in block:
        if '<br/>' in block:
            errors.append("stateDiagram-v2 contains <br/> tag (not supported)")

    # Check for \n in labels (should use <br/> instead, except in stateDiagram-v2)
    if r'\n' in block and 'stateDiagram-v2' not in block:
        errors.append("Contains \\n in labels (should use <br/>)")

    # Check for unmatched subgraph/end (excluding rect...end in sequenceDiagram)
    # rect...end is valid in sequenceDiagram, so only check flowchart/graph types
    if 'sequenceDiagram' not in block:
        subgraph_count = len(re.findall(r'\bsubgraph\b', block))
        rect_count = len(re.findall(r'\brect\b', block))
        end_count = len(re.findall(r'^\s*end\s*$', block, re.MULTILINE))

        expected_end = subgraph_count + rect_count
        if expected_end != end_count:
            errors.append(f"Unmatched block/end: {subgraph_count} subgraph + {rect_count} rect = {expected_end}, but found {end_count} end")

    # Check for invalid arrow syntax (excluding valid ERD and special arrows)
    # Valid arrows: --> ==> -.-> --x --o --|> (ERD cardinality)
    # Invalid: --< (missing >), -- (space after) BUT NOT ----- (separator)
    # Need to check arrows in context, not inside quoted labels or bracket labels

    # Remove quoted strings and bracket labels to avoid false positives from ----- separators
    block_without_labels = re.sub(r'"[^"]*"', '', block)
    block_without_labels = re.sub(r"'[^']*'", '', block_without_labels)
    block_without_labels = re.sub(r'\[[^\]]*\]', '', block_without_labels)  # Remove [...] labels
    block_without_labels = re.sub(r'\([^\)]*\)', '', block_without_labels)  # Remove (...) labels

    invalid_patterns = [
        (r'--<(?!>)', "Missing > after arrow head: --<"),
        (r'(?<![->])\-\-\s+(?![|-])', "Arrow with trailing space: -- "),
    ]

    for pattern, desc in invalid_patterns:
        matches = re.findall(pattern, block_without_labels)
        if matches:
            errors.append(f"{desc} (found {len(matches)} times)")

    return errors

def scan_directory(base_path: str) -> Dict[str, List[Dict]]:
    """Scan all markdown files in directory for Mermaid blocks."""
    results = {}

    base = Path(base_path)
    for md_file in base.rglob('*.md'):
        # Skip source-archive (P1 guardrail)
        if 'source-archive' in str(md_file):
            continue

        blocks = extract_mermaid_blocks(str(md_file))
        if blocks:
            file_results = []
            for line_num, block in blocks:
                errors = basic_mermaid_validation(block)
                file_results.append({
                    'line': line_num,
                    'errors': errors,
                    'block_preview': block[:200] + '...' if len(block) > 200 else block
                })

            if file_results:
                rel_path = str(md_file.relative_to(base))
                results[rel_path] = file_results

    return results

def main():
    base_path = '/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/docs/iGaming'

    print("Scanning iGaming documentation for Mermaid blocks...")
    results = scan_directory(base_path)

    # Categorize issues
    total_files = len(results)
    total_blocks = sum(len(blocks) for blocks in results.values())
    total_errors = sum(len([e for b in blocks for e in b['errors']]) for blocks in results.values())

    print(f"\n=== Scan Results ===")
    print(f"Files with Mermaid: {total_files}")
    print(f"Total Mermaid blocks: {total_blocks}")
    print(f"Blocks with issues: {total_errors}")

    if results:
        print(f"\n=== Files with Issues ===")
        for file_path, blocks in sorted(results.items()):
            for block_info in blocks:
                if block_info['errors']:
                    print(f"\n{file_path}:{block_info['line']}")
                    for error in block_info['errors']:
                        print(f"  - {error}")

if __name__ == '__main__':
    main()
