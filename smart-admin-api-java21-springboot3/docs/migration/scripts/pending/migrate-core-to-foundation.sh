#!/bin/bash
# Phase 3: Migrate net.lab1024.sa.base.core.* to net.lab1024.sa.foundation.core.*

echo "=== Phase 3: Core Module Migration ==="
echo ""

# Step 1: Resolve SmartPageUtil duplication
echo "Step 1: Resolving SmartPageUtil duplication..."
echo "  - Canonical version: sa-base/infrastructure/mybatis/util/SmartPageUtil.java"
echo "  - Deleting duplicate: sa-base/foundation/core/.../base/core/util/SmartPageUtil.java"
echo "  - Updating 14 support module imports to use mybatis.util.SmartPageUtil"
echo ""

# Step 2: Package migration
echo "Step 2: Migrating base.core.* to foundation.core.*..."
echo "  - 19 files to migrate (excluding SmartPageUtil + SmartBeanUtil)"
echo "  - Package rename: net.lab1024.sa.base.core.* → net.lab1024.sa.foundation.core.*"
echo ""

# Step 3: Import updates
echo "Step 3: Updating import statements..."
echo "  - ~77 import statements to update"
echo "  - SmartBeanUtil remains in net.lab1024.sa.common.core.util (exception)"
echo ""

# Step 4: Directory structure
echo "Step 4: Target directory structure:"
echo "  sa-base/foundation/core/src/main/java/"
echo "  ├── net/lab1024/sa/foundation/core/"
echo "  │   ├── annotation/ (NoNeedLogin)"
echo "  │   ├── code/ (ErrorCodeRegister, ErrorCodeRangeContainer)"
echo "  │   ├── config/ (SystemEnvironmentConfig, YamlProcessor)"
echo "  │   ├── constant/ (LoginDeviceEnum, ReloadConst)"
echo "  │   ├── domain/ (UserPermission, RequestUrlVO, SystemEnvironment, DataScopePlugin)"
echo "  │   ├── enumeration/ (SystemEnvironmentEnum)"
echo "  │   └── util/ (Smart*Util - 6 files, NO SmartPageUtil)"
echo "  └── net/lab1024/sa/common/core/util/"
echo "      └── SmartBeanUtil.java (KEEP - documented exception)"
echo ""

echo "=== Ready to execute migration ==="
echo "Run: ./gradlew migrateCoreToFoundation"
