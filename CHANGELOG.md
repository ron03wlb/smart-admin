# Changelog

All notable changes to SmartAdmin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.0.0] - 2026-01-23

### ⚠️ BREAKING CHANGES

#### Bridge Class Removal

**Removed all bridge classes in `net.lab1024.sa.common.core.*` packages.**

As part of the foundation package naming standardization initiative (Phase 2), all deprecated bridge classes have been permanently removed in v4.0.0. Code must now use `net.lab1024.sa.foundation.domain.*` packages.

**Removed packages:**
- `net.lab1024.sa.common.core.code.*` (ErrorCode, SystemErrorCode, UserErrorCode, UnexpectedErrorCode)
- `net.lab1024.sa.common.core.config.*` (CoreAutoConfiguration, BridgeDeprecationBanner)
- `net.lab1024.sa.common.core.constant.*` (StringConst, RequestHeaderConst)
- `net.lab1024.sa.common.core.domain.*` (ResponseDTO, PageResult, PageParam, RequestUser)
- `net.lab1024.sa.common.core.enumeration.*` (BaseEnum)
- `net.lab1024.sa.common.core.exception.*` (BusinessException)

**Migration mappings:**

| Removed Package | New Package |
|----------------|-------------|
| `net.lab1024.sa.common.core.code.*` | `net.lab1024.sa.foundation.domain.code.*` |
| `net.lab1024.sa.common.core.constant.*` | `net.lab1024.sa.foundation.domain.constant.*` |
| `net.lab1024.sa.common.core.domain.ResponseDTO` | `net.lab1024.sa.foundation.domain.response.ResponseDTO` |
| `net.lab1024.sa.common.core.domain.PageResult` | `net.lab1024.sa.foundation.domain.response.PageResult` |
| `net.lab1024.sa.common.core.domain.PageParam` | `net.lab1024.sa.foundation.domain.request.PageParam` |
| `net.lab1024.sa.common.core.domain.RequestUser` | `net.lab1024.sa.foundation.domain.request.RequestUser` |
| `net.lab1024.sa.common.core.enumeration.*` | `net.lab1024.sa.foundation.domain.enumeration.*` |
| `net.lab1024.sa.common.core.exception.*` | `net.lab1024.sa.foundation.domain.exception.*` |

**Exception:**
- `net.lab1024.sa.common.core.util.SmartBeanUtil` - **Remains unchanged** (intentionally not migrated, documented in build.gradle.kts)

### Changed

- **Architecture enforcement:** Added ArchUnit rule `noBridgeClassesInV4` to prevent dependencies on removed bridge packages
- **Documentation:** Updated CLAUDE.md with v4.0.0 breaking changes section
- **Architecture tests:** Updated ArchitectureTest.java comments to reflect bridge class removal

### Migration Guide

**Automated migration (recommended):**
```bash
cd smart-admin-api-java21-springboot3
./gradlew migrateToFoundation
```

**Manual migration:**
1. Search and replace imports:
   - OLD: `import net.lab1024.sa.common.core.domain.ResponseDTO;`
   - NEW: `import net.lab1024.sa.foundation.domain.response.ResponseDTO;`
2. Apply similar replacements for all removed packages
3. Rebuild: `./gradlew clean build`
4. Run tests: `./gradlew test`
5. Run architecture validation: `./gradlew :sa-admin:test --tests ArchitectureTest`

**For external projects:**
- **Option 1:** Migrate to v4.0.0 (run migration tool, test thoroughly, upgrade)
- **Option 2:** Stay on v3.9.x (receives security patches until Q2 2027)

See: [docs/migration/foundation-package-naming-standardization.md](docs/migration/foundation-package-naming-standardization.md)

### Deprecation Timeline

- **v3.7.0 (Q2 2026):** INFO warnings introduced, migration tool released
- **v3.8.0 (Q2 2026):** WARN level warnings
- **v3.9.0 (Q3 2026):** ERROR level warnings - LAST compatible version
- **v4.0.0 (Q4 2026):** Bridge classes **REMOVED** (this release)

### Extended Support

- **v3.9.x:** Security patches available until **Q2 2027** (6 months after v4.0.0 release)

---

## [3.9.0] - 2026-Q3 (Planned)

### Changed

- **FINAL WARNING:** Upgraded all bridge class deprecation warnings to ERROR level
- Added startup banner warning about v4.0.0 bridge class removal
- Last version compatible with `net.lab1024.sa.common.core.*` bridge classes

### Added

- BridgeDeprecationBanner component - displays prominent warning at application startup

---

## [3.8.0] - 2026-Q2 (Planned)

### Changed

- Upgraded bridge class deprecation warnings from INFO to WARN level
- Enhanced deprecation messages with migration deadlines

---

## [3.7.0] - 2026-Q2 (Planned)

### Added

- Migration tool: `./gradlew migrateToFoundation`
- Migration guide: docs/migration/foundation-package-naming-standardization.md

### Deprecated

- All bridge classes in `net.lab1024.sa.common.core.*` (INFO level warnings)
- Scheduled for removal in v4.0.0 (Q4 2026)

---

## [3.6.0] - Previous

### Changed

- Foundation package naming standardization (Phase 1: 8 infrastructure modules)
- Migrated modules: api-encrypt, cache, captcha, data-masking, mq, redis-lock, repeat-submit, security-protect
- Package naming: `net.lab1024.sa.common.*` → `net.lab1024.sa.foundation.*`

---

[4.0.0]: https://github.com/1024-lab/smart-admin/releases/tag/v4.0.0
[3.9.0]: https://github.com/1024-lab/smart-admin/releases/tag/v3.9.0
[3.8.0]: https://github.com/1024-lab/smart-admin/releases/tag/v3.8.0
[3.7.0]: https://github.com/1024-lab/smart-admin/releases/tag/v3.7.0
[3.6.0]: https://github.com/1024-lab/smart-admin/releases/tag/v3.6.0
