#!/bin/bash
# scripts/install-hooks.sh
# SmartAdmin Git Hooks Installer
# Generates and installs pre-commit and pre-push hooks
#
# Usage:
#   ./scripts/install-hooks.sh           # Install hooks
#   ./scripts/install-hooks.sh --force   # Overwrite without backup prompt
#
# Version: 1.0.0
# Date: 2026-02-09

set -euo pipefail

# ============================================================================
# [1] Header & Configuration
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[OK]${NC} $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $*"; }

# Resolve project root (directory containing .git/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

HOOKS_DIR="$PROJECT_ROOT/.git/hooks"
FORCE_MODE="${1:-}"

echo ""
echo -e "${BOLD}============================================${NC}"
echo -e "${BOLD}  SmartAdmin Git Hooks Installer v1.0.0${NC}"
echo -e "${BOLD}============================================${NC}"
echo ""

# ============================================================================
# [2] Environment Check
# ============================================================================

log_info "Checking environment..."

# Verify git repository
if [[ ! -d "$PROJECT_ROOT/.git" ]]; then
    log_error "Not a git repository: $PROJECT_ROOT"
    log_error "Please run this script from the SmartAdmin project root."
    exit 1
fi

# Verify hooks directory
if [[ ! -d "$HOOKS_DIR" ]]; then
    mkdir -p "$HOOKS_DIR"
    log_info "Created hooks directory: $HOOKS_DIR"
fi

# Check existing validation scripts
EXISTING_SCRIPTS=()
MISSING_SCRIPTS=()

REQUIRED_SCRIPTS=(
    "scripts/detect-bonus-corruption.sh"
    "scripts/validate-file-numbering.sh"
    "scripts/validate_links.sh"
    "scripts/detect_ssot_violations.sh"
    "scripts/scan-broken-links.sh"
)

OPTIONAL_SCRIPTS=(
    "scripts/validate-mermaid.sh"
    "scripts/detect-mermaid-emoji.sh"
    "scripts/detect-statediagram-br.sh"
    "scripts/validate-requirements-purity.sh"
    "scripts/validate-architecture-completeness.sh"
)

for script in "${REQUIRED_SCRIPTS[@]}"; do
    if [[ -f "$PROJECT_ROOT/$script" ]]; then
        EXISTING_SCRIPTS+=("$script")
    else
        MISSING_SCRIPTS+=("$script")
        log_warn "Required script missing: $script"
    fi
done

for script in "${OPTIONAL_SCRIPTS[@]}"; do
    if [[ -f "$PROJECT_ROOT/$script" ]]; then
        EXISTING_SCRIPTS+=("$script")
    fi
done

log_success "Found ${#EXISTING_SCRIPTS[@]} validation scripts"

if [[ ${#MISSING_SCRIPTS[@]} -gt 0 ]]; then
    log_warn "${#MISSING_SCRIPTS[@]} required scripts missing (hooks will skip them)"
fi

# Check optional tools
if command -v mmdc &> /dev/null; then
    log_success "mmdc (Mermaid CLI) found"
else
    log_warn "mmdc not installed (Mermaid syntax validation will be limited)"
    log_warn "  Install: npm install -g @mermaid-js/mermaid-cli"
fi

if command -v python3 &> /dev/null; then
    log_success "python3 found"
else
    log_warn "python3 not found (some link validation may be skipped)"
fi

echo ""

# ============================================================================
# [3] Backup Existing Hooks
# ============================================================================

HOOKS_TO_INSTALL=("pre-commit" "pre-push")
NEEDS_BACKUP=false

for hook in "${HOOKS_TO_INSTALL[@]}"; do
    hook_path="$HOOKS_DIR/$hook"
    if [[ -L "$hook_path" ]]; then
        # Symlink detected
        NEEDS_BACKUP=true
        log_warn "Found existing symlink hook: $hook -> $(readlink "$hook_path")"
    elif [[ -f "$hook_path" ]] && [[ ! -f "$hook_path.sample" || "$hook_path" -nt "$hook_path.sample" ]]; then
        # Regular file hook (not just a sample)
        if grep -q "SmartAdmin Git" "$hook_path" 2>/dev/null; then
            log_info "Found existing SmartAdmin hook: $hook (will be replaced)"
        else
            NEEDS_BACKUP=true
            log_warn "Found existing custom hook: $hook"
        fi
    fi
done

if [[ "$NEEDS_BACKUP" == "true" ]]; then
    BACKUP_DIR="$HOOKS_DIR/backup-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$BACKUP_DIR"

    for hook in "${HOOKS_TO_INSTALL[@]}"; do
        hook_path="$HOOKS_DIR/$hook"
        if [[ -L "$hook_path" ]]; then
            # Backup symlink target, then record symlink info
            cp -L "$hook_path" "$BACKUP_DIR/$hook" 2>/dev/null || true
            echo "symlink:$(readlink "$hook_path")" > "$BACKUP_DIR/$hook.symlink"
        elif [[ -f "$hook_path" ]]; then
            cp "$hook_path" "$BACKUP_DIR/$hook"
        fi
    done

    # Write backup metadata
    cat > "$BACKUP_DIR/BACKUP_INFO.txt" <<BINFO
SmartAdmin Git Hooks Backup
Date: $(date)
Backed up hooks: ${HOOKS_TO_INSTALL[*]}
Restore: ./scripts/uninstall-hooks.sh
BINFO

    log_success "Existing hooks backed up to: $BACKUP_DIR"
fi

echo ""

# ============================================================================
# [4] Generate pre-commit Hook
# ============================================================================

log_info "Generating pre-commit hook..."

# Remove existing hook (including symlinks) to ensure clean write
rm -f "$HOOKS_DIR/pre-commit"

cat > "$HOOKS_DIR/pre-commit" <<'PRECOMMIT_EOF'
#!/bin/bash
# SmartAdmin Git Pre-commit Hook
# Auto-generated by: scripts/install-hooks.sh
# Do NOT edit directly - re-run install-hooks.sh to update
#
# Checks:
#   [Java]     Spotless code formatting
#   [Markdown] Bonus corruption, file numbering, Mermaid validation
#
# Bypass: git commit --no-verify

set -uo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

# --- Project root ---
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
ERRORS=0
WARNINGS=0

echo -e "${BOLD}[pre-commit]${NC} Running SmartAdmin pre-commit checks..."

# --- Helper: run script if it exists ---
run_if_exists() {
    local script="$1"; shift
    local logfile="$1"; shift
    if [[ -x "$PROJECT_ROOT/$script" ]]; then
        "$PROJECT_ROOT/$script" "$@" > "$logfile" 2>&1
        return $?
    else
        return 0  # Skip missing scripts silently
    fi
}

# ===== [1] Detect staged files =====
STAGED_JAVA=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.java$' || true)
STAGED_MD=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.md$' || true)
STAGED_MD_NEW=$(git diff --cached --name-only --diff-filter=A | grep -E '\.md$' || true)

if [[ -z "$STAGED_JAVA" ]] && [[ -z "$STAGED_MD" ]]; then
    echo -e "${GREEN}[pre-commit]${NC} No .java or .md files staged. Skipping."
    exit 0
fi

# ===== [2] Java formatting (Spotless) =====
if [[ -n "$STAGED_JAVA" ]]; then
    echo -e "  ${BOLD}[Java]${NC} Checking code formatting (Spotless)..."

    JAVA_API_DIR="$PROJECT_ROOT/smart-admin-api-java21-springboot3"
    if [[ -d "$JAVA_API_DIR" ]] && [[ -f "$JAVA_API_DIR/gradlew" ]]; then
        if ! (cd "$JAVA_API_DIR" && ./gradlew spotlessApply --quiet 2>/dev/null); then
            echo -e "  ${YELLOW}[Java]${NC} Spotless formatting applied with warnings"
            WARNINGS=$((WARNINGS + 1))
        fi

        # Re-stage formatted Java files
        for file in $STAGED_JAVA; do
            if [[ -f "$PROJECT_ROOT/$file" ]]; then
                git add "$PROJECT_ROOT/$file"
            fi
        done
        echo -e "  ${GREEN}[Java]${NC} Code formatting complete"
    else
        echo -e "  ${YELLOW}[Java]${NC} Gradle project not found, skipping Spotless"
        WARNINGS=$((WARNINGS + 1))
    fi
fi

# ===== [3] Markdown validation (parallel) =====
if [[ -n "$STAGED_MD" ]]; then
    echo -e "  ${BOLD}[Docs]${NC} Validating markdown files..."

    # Create temp directory with staged .md files (preserve structure)
    TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/smartadmin-precommit.XXXXXX")
    trap "rm -rf '$TEMP_DIR'" EXIT

    for file in $STAGED_MD; do
        target_dir="$TEMP_DIR/$(dirname "$file")"
        mkdir -p "$target_dir"
        # Use git show to get the staged version (not working tree)
        git show ":$file" > "$TEMP_DIR/$file" 2>/dev/null || cp "$PROJECT_ROOT/$file" "$TEMP_DIR/$file" 2>/dev/null || true
    done

    # Create temp directory for results
    RESULT_DIR=$(mktemp -d "${TMPDIR:-/tmp}/smartadmin-results.XXXXXX")

    # --- [3.1] Bonus corruption detection (parallel) ---
    (
        run_if_exists "scripts/detect-bonus-corruption.sh" "$RESULT_DIR/bonus.log" "$TEMP_DIR"
        echo $? > "$RESULT_DIR/bonus.exit"
    ) &
    PID_BONUS=$!

    # --- [3.2] File numbering validation (parallel, new files only) ---
    (
        if [[ -n "$STAGED_MD_NEW" ]]; then
            # Create a dir with only new files for numbering check
            NEW_DIR=$(mktemp -d "${TMPDIR:-/tmp}/smartadmin-new.XXXXXX")
            for file in $STAGED_MD_NEW; do
                ndir="$NEW_DIR/$(dirname "$file")"
                mkdir -p "$ndir"
                cp "$TEMP_DIR/$file" "$NEW_DIR/$file" 2>/dev/null || true
            done
            run_if_exists "scripts/validate-file-numbering.sh" "$RESULT_DIR/numbering.log" "$NEW_DIR"
            echo $? > "$RESULT_DIR/numbering.exit"
            rm -rf "$NEW_DIR"
        else
            echo 0 > "$RESULT_DIR/numbering.exit"
        fi
    ) &
    PID_NUMBERING=$!

    # --- [3.3] Mermaid validation (parallel, optional) ---
    (
        run_if_exists "scripts/validate-mermaid.sh" "$RESULT_DIR/mermaid.log" "$TEMP_DIR"
        echo $? > "$RESULT_DIR/mermaid.exit"
    ) &
    PID_MERMAID=$!

    # --- [3.4] Emoji detection (parallel, optional) ---
    (
        run_if_exists "scripts/detect-mermaid-emoji.sh" "$RESULT_DIR/emoji.log" "$TEMP_DIR"
        echo $? > "$RESULT_DIR/emoji.exit"
    ) &
    PID_EMOJI=$!

    # --- [3.5] stateDiagram br detection (parallel, optional) ---
    (
        run_if_exists "scripts/detect-statediagram-br.sh" "$RESULT_DIR/statediagram.log" "$TEMP_DIR"
        echo $? > "$RESULT_DIR/statediagram.exit"
    ) &
    PID_STATE=$!

    # Wait for all parallel tasks
    wait $PID_BONUS $PID_NUMBERING $PID_MERMAID $PID_EMOJI $PID_STATE 2>/dev/null || true

    # --- Collect results ---
    CHECKS=(
        "bonus:Bonus corruption detection"
        "numbering:File numbering validation"
        "mermaid:Mermaid syntax validation"
        "emoji:Mermaid emoji detection"
        "statediagram:stateDiagram br/ detection"
    )

    for check_entry in "${CHECKS[@]}"; do
        key="${check_entry%%:*}"
        label="${check_entry#*:}"
        exit_file="$RESULT_DIR/$key.exit"
        log_file="$RESULT_DIR/$key.log"

        if [[ -f "$exit_file" ]]; then
            exit_code=$(cat "$exit_file")
            if [[ "$exit_code" -ne 0 ]]; then
                echo -e "  ${RED}[FAIL]${NC} $label"
                # Show first 5 error lines from log
                if [[ -f "$log_file" ]] && [[ -s "$log_file" ]]; then
                    grep -E '(❌|ERROR|FAIL)' "$log_file" 2>/dev/null | head -5 | while IFS= read -r line; do
                        echo -e "         $line"
                    done
                fi
                ERRORS=$((ERRORS + 1))
            else
                echo -e "  ${GREEN}[PASS]${NC} $label"
            fi
        fi
        # If exit file doesn't exist, the script was skipped (not installed)
    done

    # Cleanup result dir
    rm -rf "$RESULT_DIR"
fi

# ===== [4] Summary =====
echo ""
if [[ $ERRORS -gt 0 ]]; then
    echo -e "${RED}${BOLD}[pre-commit] FAILED${NC} - $ERRORS check(s) failed, $WARNINGS warning(s)"
    echo ""
    echo -e "  ${BOLD}Fix suggestions:${NC}"
    echo "    1. Review the errors above and fix the files"
    echo "    2. Re-stage fixed files: git add <files>"
    echo "    3. Try again: git commit"
    echo ""
    echo -e "  ${YELLOW}Emergency bypass:${NC} git commit --no-verify"
    exit 1
else
    if [[ $WARNINGS -gt 0 ]]; then
        echo -e "${GREEN}${BOLD}[pre-commit] PASSED${NC} with $WARNINGS warning(s)"
    else
        echo -e "${GREEN}${BOLD}[pre-commit] PASSED${NC}"
    fi
    exit 0
fi
PRECOMMIT_EOF

chmod +x "$HOOKS_DIR/pre-commit"
log_success "pre-commit hook generated"

# ============================================================================
# [5] Generate pre-push Hook
# ============================================================================

log_info "Generating pre-push hook..."

# Remove existing hook (including symlinks) to ensure clean write
rm -f "$HOOKS_DIR/pre-push"

cat > "$HOOKS_DIR/pre-push" <<'PREPUSH_EOF'
#!/bin/bash
# SmartAdmin Git Pre-push Hook
# Auto-generated by: scripts/install-hooks.sh
# Do NOT edit directly - re-run install-hooks.sh to update
#
# Checks:
#   [Quality] Link validation, SSOT violations, requirements purity, architecture completeness
#
# Bypass: git push --no-verify

set -uo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

# --- Project root ---
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
ERRORS=0
TOTAL_CHECKS=0
PASSED_CHECKS=0
SKIPPED_CHECKS=0

echo -e "${BOLD}[pre-push]${NC} Running SmartAdmin quality gate checks..."
echo ""

# Create temp directory for results
RESULT_DIR=$(mktemp -d "${TMPDIR:-/tmp}/smartadmin-prepush.XXXXXX")
trap "rm -rf '$RESULT_DIR'" EXIT

# --- Helper: run check ---
run_check() {
    local key="$1"
    local script="$2"
    shift 2
    local args=("$@")

    if [[ -x "$PROJECT_ROOT/$script" ]]; then
        "$PROJECT_ROOT/$script" "${args[@]}" > "$RESULT_DIR/$key.log" 2>&1
        echo $? > "$RESULT_DIR/$key.exit"
    else
        echo "skipped" > "$RESULT_DIR/$key.exit"
    fi
}

# ===== [1] Launch parallel quality checks =====

# [1.1] Link validation
(run_check "links" "scripts/validate_links.sh" "docs/iGaming") &
PID_LINKS=$!

# [1.2] SSOT violation detection
(run_check "ssot" "scripts/detect_ssot_violations.sh") &
PID_SSOT=$!

# [1.3] Broken link scanning
(run_check "broken" "scripts/scan-broken-links.sh" "docs/iGaming") &
PID_BROKEN=$!

# [1.4] Requirements purity (optional)
(run_check "purity" "scripts/validate-requirements-purity.sh") &
PID_PURITY=$!

# [1.5] Architecture completeness (optional)
(run_check "arch" "scripts/validate-architecture-completeness.sh") &
PID_ARCH=$!

# Wait for all tasks
wait $PID_LINKS $PID_SSOT $PID_BROKEN $PID_PURITY $PID_ARCH 2>/dev/null || true

# ===== [2] Collect and display results =====
CHECKS=(
    "links:Link validation"
    "ssot:SSOT violation detection"
    "broken:Broken link scanning"
    "purity:Requirements purity"
    "arch:Architecture completeness"
)

echo -e "  ${BOLD}Quality Gate Results:${NC}"
echo ""

for check_entry in "${CHECKS[@]}"; do
    key="${check_entry%%:*}"
    label="${check_entry#*:}"
    exit_file="$RESULT_DIR/$key.exit"
    log_file="$RESULT_DIR/$key.log"

    if [[ -f "$exit_file" ]]; then
        exit_code=$(cat "$exit_file")

        if [[ "$exit_code" == "skipped" ]]; then
            echo -e "  ${YELLOW}[SKIP]${NC} $label (script not found)"
            SKIPPED_CHECKS=$((SKIPPED_CHECKS + 1))
        elif [[ "$exit_code" -eq 0 ]]; then
            echo -e "  ${GREEN}[PASS]${NC} $label"
            PASSED_CHECKS=$((PASSED_CHECKS + 1))
            TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
        else
            echo -e "  ${RED}[FAIL]${NC} $label"
            if [[ -f "$log_file" ]] && [[ -s "$log_file" ]]; then
                grep -E '(❌|⚠️|ERROR|FAIL|broken|violation)' "$log_file" 2>/dev/null | head -3 | while IFS= read -r line; do
                    echo -e "         $line"
                done
            fi
            ERRORS=$((ERRORS + 1))
            TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
        fi
    fi
done

# ===== [3] Final decision =====
echo ""
echo -e "  ${BOLD}Summary:${NC} $PASSED_CHECKS/$TOTAL_CHECKS passed, $SKIPPED_CHECKS skipped"
echo ""

if [[ $ERRORS -gt 0 ]]; then
    echo -e "${RED}${BOLD}[pre-push] BLOCKED${NC} - $ERRORS quality check(s) failed"
    echo ""
    echo -e "  ${BOLD}Fix suggestions:${NC}"
    echo "    1. Review errors above"
    echo "    2. Fix issues and commit fixes"
    echo "    3. Try again: git push"
    echo ""
    echo -e "  ${BOLD}Detailed logs:${NC}"
    for check_entry in "${CHECKS[@]}"; do
        key="${check_entry%%:*}"
        label="${check_entry#*:}"
        log_file="$RESULT_DIR/$key.log"
        if [[ -f "$log_file" ]] && [[ -s "$log_file" ]]; then
            echo "    $label: $log_file"
        fi
    done
    echo ""
    echo -e "  ${YELLOW}Emergency bypass:${NC} git push --no-verify"
    exit 1
else
    echo -e "${GREEN}${BOLD}[pre-push] PASSED${NC} - All quality gates cleared"
    exit 0
fi
PREPUSH_EOF

chmod +x "$HOOKS_DIR/pre-push"
log_success "pre-push hook generated"

echo ""

# ============================================================================
# [6] Post-install Validation
# ============================================================================

log_info "Validating generated hooks..."

INSTALL_OK=true

for hook in "${HOOKS_TO_INSTALL[@]}"; do
    hook_path="$HOOKS_DIR/$hook"

    # Check file exists
    if [[ ! -f "$hook_path" ]]; then
        log_error "Hook not found: $hook_path"
        INSTALL_OK=false
        continue
    fi

    # Check executable
    if [[ ! -x "$hook_path" ]]; then
        log_error "Hook not executable: $hook_path"
        INSTALL_OK=false
        continue
    fi

    # Syntax check
    if bash -n "$hook_path" 2>/dev/null; then
        log_success "$hook - syntax OK, executable"
    else
        log_error "$hook - syntax error!"
        INSTALL_OK=false
    fi
done

echo ""

if [[ "$INSTALL_OK" == "true" ]]; then
    echo -e "${GREEN}${BOLD}============================================${NC}"
    echo -e "${GREEN}${BOLD}  Installation Complete!${NC}"
    echo -e "${GREEN}${BOLD}============================================${NC}"
    echo ""
    echo "  Installed hooks:"
    echo "    - pre-commit (Java formatting + Markdown validation)"
    echo "    - pre-push   (Quality gate checks)"
    echo ""
    echo "  Available validation scripts: ${#EXISTING_SCRIPTS[@]}"
    for script in "${EXISTING_SCRIPTS[@]}"; do
        echo "    - $script"
    done
    echo ""
    echo "  Usage:"
    echo "    Hooks run automatically on git commit / git push"
    echo "    Bypass: git commit --no-verify / git push --no-verify"
    echo "    Uninstall: ./scripts/uninstall-hooks.sh"
    echo ""
else
    log_error "Installation completed with errors. Please check the output above."
    exit 1
fi
