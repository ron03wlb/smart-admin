#!/bin/bash
# SmartAdmin Constructor Injection Migration Script
# Purpose: Migrate from @Resource field injection to @RequiredArgsConstructor constructor injection
# Version: 1.0.0
# Author: Claude (SmartAdmin Architecture Analysis)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PROCESSED=0
MIGRATED=0
SKIPPED=0
FAILED=0

# Log functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if file needs migration
needs_migration() {
    local file="$1"
    grep -q "@Resource" "$file" && return 0 || return 1
}

# Function to check if file already uses constructor injection
has_constructor_injection() {
    local file="$1"
    grep -q "@RequiredArgsConstructor" "$file" && return 0 || return 1
}

# Function to migrate a single file
migrate_file() {
    local file="$1"
    local backup="${file}.backup"

    log_info "Processing: $file"

    # Check if file needs migration
    if ! needs_migration "$file"; then
        log_warning "Skipped (no @Resource found): $file"
        ((SKIPPED++))
        return 0
    fi

    # Create backup
    cp "$file" "$backup"

    # Step 1: Add 'final' to @Resource fields
    sed -i 's/@Resource private /@Resource private final /g' "$file"

    # Step 2: Check if @RequiredArgsConstructor already exists
    if ! has_constructor_injection "$file"; then
        # Find the line number of @Service annotation
        local service_line=$(grep -n "^@Service" "$file" | head -1 | cut -d: -f1)

        if [ -n "$service_line" ]; then
            # Add import for @RequiredArgsConstructor if not exists
            if ! grep -q "import lombok.RequiredArgsConstructor" "$file"; then
                # Find the last import line
                local last_import=$(grep -n "^import " "$file" | tail -1 | cut -d: -f1)

                if [ -n "$last_import" ]; then
                    sed -i "${last_import}a import lombok.RequiredArgsConstructor;" "$file"
                fi
            fi

            # Add @RequiredArgsConstructor annotation before @Service
            sed -i "${service_line}i @RequiredArgsConstructor" "$file"
        else
            log_warning "No @Service annotation found in $file"
        fi
    fi

    # Step 3: Remove @Resource annotations
    sed -i 's/@Resource private final /private final /g' "$file"

    # Step 4: Remove unused @Resource import if no longer needed
    if ! grep -q "@Resource" "$file"; then
        sed -i '/^import jakarta.annotation.Resource;$/d' "$file"
    fi

    # Step 5: Verify the migration
    if grep -q "@Resource" "$file"; then
        log_error "Migration incomplete (still has @Resource): $file"
        # Restore backup
        mv "$backup" "$file"
        ((FAILED++))
        return 1
    fi

    if ! has_constructor_injection "$file"; then
        log_error "Migration incomplete (no @RequiredArgsConstructor): $file"
        # Restore backup
        mv "$backup" "$file"
        ((FAILED++))
        return 1
    fi

    # Success - remove backup
    rm "$backup"
    log_success "Migrated: $file"
    ((MIGRATED++))
    return 0
}

# Function to process a directory
process_directory() {
    local dir="$1"

    log_info "Processing directory: $dir"

    # Find all Java files in the directory
    local java_files=$(find "$dir" -name "*.java" -type f | grep -E "(Controller|Service|Manager)\.java$")

    if [ -z "$java_files" ]; then
        log_warning "No Java files found in $dir"
        return 0
    fi

    # Process each file
    for file in $java_files; do
        ((PROCESSED++))
        migrate_file "$file"
    done
}

# Main script
main() {
    log_info "==================================="
    log_info "SmartAdmin Constructor Injection Migration"
    log_info "==================================="
    echo ""

    # Check if directory argument is provided
    if [ -z "$1" ]; then
        log_error "Usage: $0 <directory>"
        log_error "Example: $0 sa-admin/src/main/java/net/lab1024/sa/admin/module/business/goods"
        exit 1
    fi

    local target_dir="$1"

    # Check if directory exists
    if [ ! -d "$target_dir" ]; then
        log_error "Directory does not exist: $target_dir"
        exit 1
    fi

    # Process the directory
    process_directory "$target_dir"

    # Print summary
    echo ""
    log_info "==================================="
    log_info "Migration Summary"
    log_info "==================================="
    echo "Total processed: $PROCESSED"
    echo -e "${GREEN}Successfully migrated: $MIGRATED${NC}"
    echo -e "${YELLOW}Skipped: $SKIPPED${NC}"
    echo -e "${RED}Failed: $FAILED${NC}"

    if [ $FAILED -gt 0 ]; then
        log_error "Migration completed with errors"
        exit 1
    else
        log_success "Migration completed successfully"

        # Suggest running Spotless
        echo ""
        log_info "Next steps:"
        echo "1. Run Spotless to format the code:"
        echo "   ./gradlew spotlessApply"
        echo "2. Verify compilation:"
        echo "   ./gradlew :sa-admin:compileJava"
        echo "3. Run ArchUnit tests:"
        echo "   ./gradlew :sa-admin:test --tests ArchitectureTest.noResourceFieldInjection"

        exit 0
    fi
}

# Run main function
main "$@"
