#!/usr/bin/env python3
"""
SmartAdmin Skill Registry Synchronization Script

Purpose: Prevent drift between .claude/skills/ and .agent/skills/ registries
Usage: python3 scripts/sync-skill-registries.py [--dry-run] [--verbose]

Author: SmartAdmin Skills Team
Version: 1.0.0
Last Updated: 2026-02-07
"""

import yaml
import os
import sys
import argparse
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Set, Tuple

# Configuration
PROJECT_ROOT = Path(__file__).parent.parent
CLAUDE_REGISTRY = PROJECT_ROOT / ".claude" / "skills" / "skill-registry.yml"
AGENT_REGISTRY = PROJECT_ROOT / ".agent" / "skills" / "skill-registry.yml"
AGENT_SKILLS_DIR = PROJECT_ROOT / ".agent" / "skills"

# Skills to exclude from sync (Claude-specific features)
EXCLUDE_FROM_AGENT = {
    # Deprecated skills - no need to sync
    "smartadmin-mybatis",
    "smartadmin-vue-crud",
    "smartadmin-api-docs",
    "java-performance-pro",  # Soft-deprecated
}

# Priority levels to sync
SYNC_PRIORITIES = {"P0", "P1", "P2"}


def load_yaml(path: Path) -> Dict:
    """Load YAML file and return parsed content."""
    if not path.exists():
        print(f"Error: File not found: {path}")
        sys.exit(1)

    with open(path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


def get_claude_skills(registry: Dict) -> Dict[str, Dict]:
    """Extract skills from Claude registry."""
    return registry.get('skills', {})


def get_agent_skills(registry: Dict) -> Dict[str, Dict]:
    """Extract skills from Agent registry."""
    return registry.get('skills', {})


def check_skill_file_exists(skill_name: str, skill_info: Dict) -> Tuple[bool, str]:
    """Check if SKILL.md file exists for a skill in .agent directory."""
    skill_path = skill_info.get('path', '')
    skill_md_path = AGENT_SKILLS_DIR / skill_path / "SKILL.md"

    if skill_md_path.exists():
        return True, str(skill_md_path)
    return False, str(skill_md_path)


def analyze_sync_status(claude_skills: Dict, agent_skills: Dict, verbose: bool = False) -> Dict:
    """Analyze synchronization status between registries."""

    results = {
        'in_sync': [],           # Skills in both registries
        'missing_in_agent': [],   # Skills in Claude but not in Agent
        'extra_in_agent': [],     # Skills in Agent but not in Claude
        'missing_skill_files': [], # Skills in agent registry but no SKILL.md
        'orphaned_directories': [], # Directories without registry entry
        'priority_mismatch': [],  # Skills with different priorities
    }

    # Skills that should be in agent (exclude deprecated)
    claude_active = {
        name: info for name, info in claude_skills.items()
        if name not in EXCLUDE_FROM_AGENT
        and info.get('status') != 'deprecated'
        and info.get('priority', 'P3') in SYNC_PRIORITIES
    }

    agent_names = set(agent_skills.keys())
    claude_active_names = set(claude_active.keys())

    # Find skills in both
    in_both = claude_active_names & agent_names
    for name in sorted(in_both):
        results['in_sync'].append(name)

        # Check for priority mismatch
        claude_priority = claude_active[name].get('priority', 'P2')
        agent_priority = agent_skills[name].get('priority', 'P2')
        if claude_priority != agent_priority:
            results['priority_mismatch'].append({
                'skill': name,
                'claude_priority': claude_priority,
                'agent_priority': agent_priority
            })

    # Find skills missing in agent
    missing = claude_active_names - agent_names
    for name in sorted(missing):
        results['missing_in_agent'].append({
            'skill': name,
            'priority': claude_active[name].get('priority', 'P2'),
            'path': claude_active[name].get('path', '')
        })

    # Find extra skills in agent (not in claude)
    extra = agent_names - claude_active_names - EXCLUDE_FROM_AGENT
    for name in sorted(extra):
        results['extra_in_agent'].append(name)

    # Check for missing SKILL.md files
    for name, info in agent_skills.items():
        exists, path = check_skill_file_exists(name, info)
        if not exists:
            results['missing_skill_files'].append({
                'skill': name,
                'expected_path': path
            })

    # Check for orphaned directories (directories without registry entries)
    for category_dir in ['foundation', 'extended', 'productivity']:
        category_path = AGENT_SKILLS_DIR / category_dir
        if category_path.exists():
            for subcat in category_path.iterdir():
                if subcat.is_dir():
                    for skill_dir in subcat.iterdir():
                        if skill_dir.is_dir() and (skill_dir / "SKILL.md").exists():
                            skill_name = skill_dir.name
                            if skill_name not in agent_skills:
                                results['orphaned_directories'].append({
                                    'directory': str(skill_dir.relative_to(PROJECT_ROOT)),
                                    'skill_name': skill_name
                                })

    return results


def print_report(results: Dict, verbose: bool = False):
    """Print synchronization report."""

    print("\n" + "=" * 60)
    print("SmartAdmin Skill Registry Synchronization Report")
    print("=" * 60)
    print(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Claude Registry: {CLAUDE_REGISTRY}")
    print(f"Agent Registry: {AGENT_REGISTRY}")
    print("=" * 60)

    # Summary
    print("\n## Summary\n")
    print(f"- Skills in sync: {len(results['in_sync'])}")
    print(f"- Missing in .agent/: {len(results['missing_in_agent'])}")
    print(f"- Extra in .agent/: {len(results['extra_in_agent'])}")
    print(f"- Missing SKILL.md files: {len(results['missing_skill_files'])}")
    print(f"- Orphaned directories: {len(results['orphaned_directories'])}")
    print(f"- Priority mismatches: {len(results['priority_mismatch'])}")

    # In Sync (only in verbose mode)
    if verbose and results['in_sync']:
        print(f"\n## Skills In Sync ({len(results['in_sync'])})\n")
        for name in results['in_sync']:
            print(f"  [OK] {name}")

    # Missing in Agent
    if results['missing_in_agent']:
        print(f"\n## Missing in .agent/ ({len(results['missing_in_agent'])})\n")
        print("These skills exist in .claude/ but not in .agent/:")
        for item in results['missing_in_agent']:
            print(f"  [MISSING] {item['skill']} ({item['priority']})")
            print(f"            Path: {item['path']}")

    # Extra in Agent
    if results['extra_in_agent']:
        print(f"\n## Extra in .agent/ ({len(results['extra_in_agent'])})\n")
        print("These skills exist in .agent/ but not in .claude/:")
        for name in results['extra_in_agent']:
            print(f"  [EXTRA] {name}")

    # Missing SKILL.md
    if results['missing_skill_files']:
        print(f"\n## Missing SKILL.md Files ({len(results['missing_skill_files'])})\n")
        print("Skills in registry but missing SKILL.md file:")
        for item in results['missing_skill_files']:
            print(f"  [NO FILE] {item['skill']}")
            print(f"            Expected: {item['expected_path']}")

    # Orphaned Directories
    if results['orphaned_directories']:
        print(f"\n## Orphaned Directories ({len(results['orphaned_directories'])})\n")
        print("Directories with SKILL.md but not in registry:")
        for item in results['orphaned_directories']:
            print(f"  [ORPHAN] {item['skill_name']}")
            print(f"           Directory: {item['directory']}")

    # Priority Mismatches
    if results['priority_mismatch']:
        print(f"\n## Priority Mismatches ({len(results['priority_mismatch'])})\n")
        for item in results['priority_mismatch']:
            print(f"  [MISMATCH] {item['skill']}")
            print(f"             Claude: {item['claude_priority']}, Agent: {item['agent_priority']}")

    # Overall Status
    print("\n" + "=" * 60)
    total_issues = (
        len(results['missing_in_agent']) +
        len(results['extra_in_agent']) +
        len(results['missing_skill_files']) +
        len(results['orphaned_directories']) +
        len(results['priority_mismatch'])
    )

    if total_issues == 0:
        print("Status: SYNCHRONIZED")
        print("All skills are in sync between .claude/ and .agent/")
    else:
        print(f"Status: OUT OF SYNC ({total_issues} issues)")
        print("\nRecommended Actions:")
        if results['missing_in_agent']:
            print("  1. Create SKILL.md files for missing skills in .agent/")
            print("     Or add entries to .agent/skills/skill-registry.yml")
        if results['orphaned_directories']:
            print("  2. Add orphaned skills to registry or remove directories")
        if results['missing_skill_files']:
            print("  3. Create missing SKILL.md files or remove registry entries")

    print("=" * 60 + "\n")

    return total_issues


def main():
    parser = argparse.ArgumentParser(
        description='Sync SmartAdmin skill registries between .claude/ and .agent/'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Only report differences, do not make changes'
    )
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help='Show detailed output including in-sync skills'
    )
    parser.add_argument(
        '--json',
        action='store_true',
        help='Output results in JSON format'
    )

    args = parser.parse_args()

    # Load registries
    print("Loading skill registries...")
    claude_registry = load_yaml(CLAUDE_REGISTRY)
    agent_registry = load_yaml(AGENT_REGISTRY)

    claude_skills = get_claude_skills(claude_registry)
    agent_skills = get_agent_skills(agent_registry)

    print(f"Claude registry: {len(claude_skills)} skills")
    print(f"Agent registry: {len(agent_skills)} skills")

    # Analyze sync status
    results = analyze_sync_status(claude_skills, agent_skills, args.verbose)

    # Output results
    if args.json:
        import json
        print(json.dumps(results, indent=2, ensure_ascii=False))
    else:
        issues = print_report(results, args.verbose)
        sys.exit(0 if issues == 0 else 1)


if __name__ == '__main__':
    main()
