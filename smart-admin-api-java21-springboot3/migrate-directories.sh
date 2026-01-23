#!/bin/bash

echo "Moving files from common.* to foundation.* directory structure..."

modules=(
    "api-encrypt:apiencrypt"
    "cache:cache"
    "captcha:captcha"
    "data-masking:datamasking"
    "mq:mq"
    "redis-lock:redislock"
    "repeat-submit:repeatsubmit"
    "security-protect:securityprotect"
)

for module_pair in "${modules[@]}"; do
    IFS=':' read -r module_name package_name <<< "$module_pair"
    
    old_dir="sa-base/foundation/$module_name/src/main/java/net/lab1024/sa/common/$package_name"
    new_dir="sa-base/foundation/$module_name/src/main/java/net/lab1024/sa/foundation/$package_name"
    
    if [ -d "$old_dir" ]; then
        echo "[$module_name] Moving $old_dir -> $new_dir"
        mkdir -p "$(dirname "$new_dir")"
        mv "$old_dir" "$new_dir"
        
        # Remove empty parent directories
        rmdir --ignore-fail-on-non-empty -p "sa-base/foundation/$module_name/src/main/java/net/lab1024/sa/common" 2>/dev/null || true
    else
        echo "[$module_name] Already migrated or not found: $old_dir"
    fi
    
    # Also move test files if they exist
    old_test_dir="sa-base/foundation/$module_name/src/test/java/net/lab1024/sa/common/$package_name"
    new_test_dir="sa-base/foundation/$module_name/src/test/java/net/lab1024/sa/foundation/$package_name"
    
    if [ -d "$old_test_dir" ]; then
        echo "[$module_name] Moving test: $old_test_dir -> $new_test_dir"
        mkdir -p "$(dirname "$new_test_dir")"
        mv "$old_test_dir" "$new_test_dir"
        rmdir --ignore-fail-on-non-empty -p "sa-base/foundation/$module_name/src/test/java/net/lab1024/sa/common" 2>/dev/null || true
    fi
done

echo "Directory migration complete!"
