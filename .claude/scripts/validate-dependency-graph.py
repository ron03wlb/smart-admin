#!/usr/bin/env python3

"""
SmartAdmin Skill Dependency Graph Validator
============================================
Purpose: Validate skill dependencies and detect circular dependencies
Version: 1.0.0
Created: 2026-01-29

Features:
- Parse skill-registry.yml dependencies
- Build directed dependency graph
- Detect circular dependencies (topological sort)
- Generate dependency graph visualization (optional)
"""

import sys
import yaml
import argparse
from pathlib import Path
from typing import Dict, List, Set, Tuple
from collections import defaultdict, deque

# Colors for terminal output
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'  # No Color

def print_success(msg: str):
    print(f"{Colors.GREEN}✅ {msg}{Colors.NC}")

def print_error(msg: str):
    print(f"{Colors.RED}❌ {msg}{Colors.NC}")

def print_warning(msg: str):
    print(f"{Colors.YELLOW}⚠️  {msg}{Colors.NC}")

def print_info(msg: str):
    print(f"{Colors.BLUE}ℹ️  {msg}{Colors.NC}")

def print_header(msg: str):
    print()
    print(f"{Colors.BLUE}{'=' * 60}{Colors.NC}")
    print(f"{Colors.BLUE}{msg}{Colors.NC}")
    print(f"{Colors.BLUE}{'=' * 60}{Colors.NC}")
    print()

class SkillDependencyGraph:
    def __init__(self, registry_file: Path):
        self.registry_file = registry_file
        self.skills: Dict[str, dict] = {}
        self.graph: Dict[str, List[str]] = defaultdict(list)
        self.reverse_graph: Dict[str, List[str]] = defaultdict(list)

    def load_registry(self) -> bool:
        """Load skill-registry.yml"""
        try:
            with open(self.registry_file, 'r', encoding='utf-8') as f:
                data = yaml.safe_load(f)
                self.skills = data.get('skills', {})
                print_success(f"Loaded {len(self.skills)} skills from registry")
                return True
        except Exception as e:
            print_error(f"Failed to load registry: {e}")
            return False

    def build_graph(self):
        """Build dependency graph"""
        print_header("Building Dependency Graph")

        for skill_name, skill_data in self.skills.items():
            depends_on = skill_data.get('depends_on', [])
            depended_by = skill_data.get('depended_by', [])

            # Build forward graph (A depends on B)
            for dep in depends_on:
                self.graph[skill_name].append(dep)
                self.reverse_graph[dep].append(skill_name)

            # Verify depended_by consistency
            for dep in depended_by:
                if skill_name not in self.graph.get(dep, []):
                    print_warning(f"{skill_name}: depended_by includes '{dep}', but '{dep}' doesn't depend on '{skill_name}'")

        total_edges = sum(len(deps) for deps in self.graph.values())
        print_info(f"Total dependency edges: {total_edges}")

        # Print skills with dependencies
        skills_with_deps = [s for s, deps in self.graph.items() if deps]
        if skills_with_deps:
            print_info(f"Skills with dependencies: {len(skills_with_deps)}")
            for skill in sorted(skills_with_deps):
                deps = self.graph[skill]
                print(f"  - {skill} → {', '.join(deps)}")

    def detect_circular_dependencies(self) -> List[List[str]]:
        """Detect circular dependencies using DFS"""
        print_header("Detecting Circular Dependencies")

        visited = set()
        rec_stack = set()
        cycles = []

        def dfs(node: str, path: List[str]) -> bool:
            visited.add(node)
            rec_stack.add(node)
            path.append(node)

            for neighbor in self.graph.get(node, []):
                if neighbor not in visited:
                    if dfs(neighbor, path):
                        return True
                elif neighbor in rec_stack:
                    # Found cycle
                    cycle_start = path.index(neighbor)
                    cycle = path[cycle_start:] + [neighbor]
                    cycles.append(cycle)
                    return True

            path.pop()
            rec_stack.remove(node)
            return False

        for skill in self.skills.keys():
            if skill not in visited:
                dfs(skill, [])

        if cycles:
            print_error(f"Found {len(cycles)} circular dependency cycle(s)")
            for i, cycle in enumerate(cycles, 1):
                print(f"\n  Cycle {i}: {' → '.join(cycle)}")
            return cycles
        else:
            print_success("No circular dependencies detected")
            return []

    def topological_sort(self) -> Tuple[bool, List[str]]:
        """Perform topological sort (Kahn's algorithm)"""
        print_header("Topological Sort (Dependency Order)")

        # Calculate in-degrees
        in_degree = {skill: 0 for skill in self.skills.keys()}
        for skill in self.graph:
            for dep in self.graph[skill]:
                if dep in in_degree:
                    in_degree[dep] += 1

        # Queue with zero in-degree nodes
        queue = deque([skill for skill, degree in in_degree.items() if degree == 0])
        sorted_order = []

        while queue:
            node = queue.popleft()
            sorted_order.append(node)

            # Reduce in-degree for neighbors
            for neighbor in self.reverse_graph.get(node, []):
                in_degree[neighbor] -= 1
                if in_degree[neighbor] == 0:
                    queue.append(neighbor)

        if len(sorted_order) != len(self.skills):
            print_error("Topological sort FAILED (circular dependency detected)")
            missing = set(self.skills.keys()) - set(sorted_order)
            print(f"  Skills not sorted: {', '.join(missing)}")
            return False, []
        else:
            print_success("Topological sort successful")
            print_info("Dependency tiers:")

            # Group by tiers
            tiers = self._calculate_tiers()
            for tier, skills in tiers.items():
                print(f"  Tier {tier}: {', '.join(sorted(skills))}")

            return True, sorted_order

    def _calculate_tiers(self) -> Dict[int, List[str]]:
        """Calculate dependency tiers"""
        tiers = defaultdict(list)
        tier_map = {}

        # BFS to assign tiers
        queue = deque()
        for skill in self.skills.keys():
            if not self.graph.get(skill):  # No dependencies
                queue.append((skill, 0))
                tier_map[skill] = 0

        while queue:
            skill, tier = queue.popleft()
            tiers[tier].append(skill)

            for dependent in self.reverse_graph.get(skill, []):
                if dependent not in tier_map:
                    # Check if all dependencies are resolved
                    deps = self.graph.get(dependent, [])
                    if all(d in tier_map for d in deps):
                        max_dep_tier = max((tier_map[d] for d in deps), default=-1)
                        tier_map[dependent] = max_dep_tier + 1
                        queue.append((dependent, max_dep_tier + 1))

        return dict(tiers)

    def validate_dependency_exists(self) -> bool:
        """Validate all dependencies exist in registry"""
        print_header("Validating Dependency References")

        all_valid = True
        for skill_name, skill_data in self.skills.items():
            depends_on = skill_data.get('depends_on', [])
            for dep in depends_on:
                if dep not in self.skills:
                    print_error(f"{skill_name}: depends on non-existent skill '{dep}'")
                    all_valid = False

        if all_valid:
            print_success("All dependency references are valid")

        return all_valid

    def generate_summary(self):
        """Generate summary report"""
        print_header("Dependency Graph Summary")

        total_skills = len(self.skills)
        skills_with_deps = len([s for s, deps in self.graph.items() if deps])
        total_edges = sum(len(deps) for deps in self.graph.values())

        print(f"Total Skills: {total_skills}")
        print(f"Skills with Dependencies: {skills_with_deps}")
        print(f"Total Dependency Edges: {total_edges}")

        # Find skills depended upon most
        dependency_count = defaultdict(int)
        for deps in self.graph.values():
            for dep in deps:
                dependency_count[dep] += 1

        if dependency_count:
            print("\nMost Depended Upon Skills:")
            sorted_deps = sorted(dependency_count.items(), key=lambda x: x[1], reverse=True)
            for skill, count in sorted_deps[:5]:
                print(f"  - {skill}: {count} dependent(s)")

def main():
    parser = argparse.ArgumentParser(description='Validate SmartAdmin skill dependency graph')
    parser.add_argument('--registry', type=str,
                       default='.claude/skills/skill-registry.yml',
                       help='Path to skill-registry.yml')
    parser.add_argument('--visualize', action='store_true',
                       help='Generate dependency graph visualization (requires graphviz)')

    args = parser.parse_args()

    # Find project root
    script_dir = Path(__file__).parent
    project_root = script_dir.parent.parent
    registry_path = project_root / args.registry

    if not registry_path.exists():
        print_error(f"Registry file not found: {registry_path}")
        sys.exit(1)

    print_header("SmartAdmin Skill Dependency Graph Validator")
    print_info(f"Registry: {registry_path}")

    # Build and validate graph
    graph = SkillDependencyGraph(registry_path)

    if not graph.load_registry():
        sys.exit(1)

    graph.build_graph()

    # Run validations
    valid_refs = graph.validate_dependency_exists()
    cycles = graph.detect_circular_dependencies()
    sort_success, _ = graph.topological_sort()

    graph.generate_summary()

    # Final result
    print_header("Validation Result")

    if valid_refs and not cycles and sort_success:
        print_success("✅ Dependency graph is VALID")
        sys.exit(0)
    else:
        print_error("❌ Dependency graph validation FAILED")
        if not valid_refs:
            print("  - Invalid dependency references found")
        if cycles:
            print(f"  - {len(cycles)} circular dependency cycle(s) found")
        if not sort_success:
            print("  - Topological sort failed")
        sys.exit(1)

if __name__ == '__main__':
    main()
