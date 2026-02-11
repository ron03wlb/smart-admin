#!/usr/bin/env python3
"""
Cross-Reference Bidirectional Validation Script

Purpose: Verify bidirectional cross-references between Requirements and Architecture layers
Usage: python scripts/validate-cross-references.py
Exit: 0 if all references are bidirectional, 1 if missing back-references detected
"""

import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple

def parse_requirements_references() -> Dict[str, List[Tuple[str, str]]]:
    """
    Parse Architecture references from Requirements layer documents.

    Returns:
        Dict mapping Requirements filename to list of (title, arch_path) tuples
    """
    req_dir = Path("docs/iGaming/requirements")
    references = {}

    if not req_dir.exists():
        print(f"❌ ERROR: Requirements directory not found: {req_dir}")
        sys.exit(1)

    for md_file in req_dir.rglob("*.md"):
        if md_file.name == "README.md":
            continue

        try:
            content = md_file.read_text(encoding="utf-8")
        except Exception as e:
            print(f"⚠️  Warning: Could not read {md_file}: {e}")
            continue

        # Match cross-reference pattern: → **[Title](../../architecture/path/to/doc.md#anchor)**
        matches = re.findall(
            r'→ \*\*\[([^\]]+)\]\(\.\./\.\./architecture/([^\)]+)\)',
            content
        )

        if matches:
            references[md_file.name] = matches

    return references


def parse_architecture_backrefs() -> Dict[str, str]:
    """
    Parse Business Requirements back-references from Architecture layer documents.

    Returns:
        Dict mapping Architecture filename to Requirements path
    """
    arch_dir = Path("docs/iGaming/architecture")
    backrefs = {}

    if not arch_dir.exists():
        print(f"❌ ERROR: Architecture directory not found: {arch_dir}")
        sys.exit(1)

    for md_file in arch_dir.rglob("*.md"):
        if md_file.name == "README.md":
            continue

        try:
            content = md_file.read_text(encoding="utf-8")
        except Exception as e:
            print(f"⚠️  Warning: Could not read {md_file}: {e}")
            continue

        # Match back-reference pattern: > **Business Requirements**: [Title](../../requirements/path/to/doc.md)
        match = re.search(
            r'> \*\*Business Requirements\*\*: \[([^\]]+)\]\(\.\./\.\./requirements/([^\)]+)\)',
            content
        )

        if match:
            backrefs[md_file.name] = match.group(2)

    return backrefs


def validate_bidirectional() -> int:
    """
    Validate that all cross-references are bidirectional.

    Returns:
        0 if all references are bidirectional, 1 otherwise
    """
    print("=" * 60)
    print("Cross-Reference Bidirectional Validation")
    print("=" * 60)
    print()

    req_to_arch = parse_requirements_references()
    arch_to_req = parse_architecture_backrefs()

    print(f"Requirements → Architecture references: {sum(len(refs) for refs in req_to_arch.values())}")
    print(f"Architecture → Requirements back-references: {len(arch_to_req)}")
    print()

    missing_backrefs = []

    for req_file, arch_refs in req_to_arch.items():
        for title, arch_path in arch_refs:
            # Extract filename from path (remove #anchor if present)
            arch_file = Path(arch_path.split('#')[0]).name

            if arch_file not in arch_to_req:
                missing_backrefs.append((req_file, arch_file, title))
                print(f"❌ MISSING backref: {req_file} → {arch_file}")
                print(f"   Reference title: {title}")

    print()
    print("=" * 60)
    print("Summary")
    print("=" * 60)

    if not missing_backrefs:
        print("✅ PASSED: All cross-references are bidirectional")
        print()
        print(f"Total valid references: {sum(len(refs) for refs in req_to_arch.values())}")
        print(f"Total back-references: {len(arch_to_req)}")
        return 0
    else:
        print(f"❌ FAILED: {len(missing_backrefs)} missing back-reference(s)")
        print()
        print("Missing Back-References:")
        for req_file, arch_file, title in missing_backrefs:
            print(f"  - {arch_file} (referenced from {req_file})")
        print()
        print("Action Items:")
        print("1. Add '> **Business Requirements**: [...]' header to Architecture documents listed above")
        print("2. Ensure back-reference points to correct Requirements document")
        return 1


if __name__ == "__main__":
    exit_code = validate_bidirectional()
    sys.exit(exit_code)
