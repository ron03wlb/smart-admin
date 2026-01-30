# Translation Management Workflow Guide

**Skill:** i18n-generator
**Component:** Translation Workflow / Key Extraction / Quality Assurance
**Purpose:** Establish systematic translation management process for SmartAdmin projects

---

## Translation Workflow Overview

```
1. Development Phase
   └─> Extract translation keys from code
       └─> Generate translation file templates

2. Translation Phase
   └─> Translators add translations
       └─> Review and approve translations

3. Integration Phase
   └─> Validate translations (syntax, completeness)
       └─> Integrate into codebase

4. QA Phase
   └─> Test with all locales
       └─> Fix missing/incorrect translations

5. Deployment
   └─> Deploy to production
       └─> Monitor translation usage
```

---

## Pattern 1: Translation Key Extraction

### Backend (Java) - Extract from Code

```bash
# Regex pattern for Java: getMessage("key") or i18nService.getMessage("key")
grep -r "getMessage(\"[^\"]*\")" src/main/java/
```

### Frontend (Vue) - Extract from Code

```javascript
// scripts/extract-i18n-keys.js
const fs = require('fs');
const path = require('path');

const extractedKeys = new Set();

function extractKeysFromFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8');

  // Match t('key'), t("key"), $t('key'), $t("key")
  const regex = /\$?t\(['"]([a-zA-Z0-9._-]+)['"]\)/g;
  let match;

  while ((match = regex.exec(content)) !== null) {
    extractedKeys.add(match[1]);
  }
}

function scanDirectory(dir) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const filePath = path.join(dir, file);
    if (fs.statSync(filePath).isDirectory()) {
      scanDirectory(filePath);
    } else if (file.endsWith('.vue') || file.endsWith('.js')) {
      extractKeysFromFile(filePath);
    }
  }
}

scanDirectory('./src');
console.log('Extracted keys:', Array.from(extractedKeys).sort());
```

---

## Pattern 2: Missing Translation Detection

```javascript
// scripts/check-missing-translations.js
const fs = require('fs');
const path = require('path');

function loadTranslations(locale) {
  const filePath = `./src/i18n/locales/${locale}.json`;
  return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
}

function findMissingKeys(keys, translations) {
  const missing = [];
  for (const key of keys) {
    const parts = key.split('.');
    let obj = translations;
    for (const part of parts) {
      if (!obj[part]) {
        missing.push(key);
        break;
      }
      obj = obj[part];
    }
  }
  return missing;
}

const locales = ['zh-CN', 'en-US', 'ja-JP', 'ar-SA'];
const extractedKeys = extractKeysFromProject();

for (const locale of locales) {
  const translations = loadTranslations(locale);
  const missing = findMissingKeys(extractedKeys, translations);

  if (missing.length > 0) {
    console.log(`Missing translations for ${locale}:`);
    missing.forEach(key => console.log(`  - ${key}`));
  } else {
    console.log(`✅ All keys translated for ${locale}`);
  }
}
```

---

## Pattern 3: Translation Validation

```javascript
// scripts/validate-translations.js
function validateTranslationFile(filePath) {
  const errors = [];
  const content = fs.readFileSync(filePath, 'utf-8');

  // Check JSON syntax
  try {
    const translations = JSON.parse(content);

    // Check for TODO markers
    function checkTODO(obj, prefix = '') {
      for (const [key, value] of Object.entries(obj)) {
        const fullKey = prefix ? `${prefix}.${key}` : key;
        if (typeof value === 'string' && value.includes('TODO')) {
          errors.push({ key: fullKey, error: 'Incomplete translation' });
        } else if (typeof value === 'object') {
          checkTODO(value, fullKey);
        }
      }
    }

    checkTODO(translations);
  } catch (e) {
    errors.push({ error: `Invalid JSON: ${e.message}` });
  }

  return errors;
}
```

---

## Pattern 4: CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/i18n-check.yml
name: i18n Translation Check

on:
  pull_request:
    paths:
      - 'src/**'
      - 'smart-admin-web/src/**'

jobs:
  check-translations:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Extract and validate i18n keys
        run: |
          node scripts/extract-i18n-keys.js
          node scripts/validate-translations.js

      - name: Check coverage
        run: node scripts/translation-coverage.js
```

---

## Best Practices

1. **Automated Extraction:** Run key extraction in CI/CD
2. **Translation Templates:** Generate templates for translators
3. **Quality Validation:** Check syntax and completeness
4. **Version Control:** Track translation changes in Git
5. **Continuous Monitoring:** Monitor translation usage and coverage

---

**Version:** 1.0.0
**Last Updated:** 2026-01-26
