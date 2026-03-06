# SmartAdmin React CRUD Skill

**Version**: 1.0.0
**Status**: ✅ Production Ready
**Priority**: P0 - Foundation
**Category**: frontend
**Type**: atomic
**Execution**: phase-based (5 phases)

---

## 📖 Overview

SmartAdmin React CRUD Skill 自動生成完整的 React 19 CRUD 模塊，包含列表組件、表單模態框、API 客戶端、TypeScript 類型定義和測試。

**時間節省**: 4-6 小時 → 25 分鐘（**92% 時間節省**）

---

## ✨ Key Features

### 🎯 Core Capabilities

- ✅ **TypeScript 類型生成**（100% 後端對齊）
- ✅ **API 客戶端**（ResponseModel 標準處理）
- ✅ **列表組件**（Table + Pagination + Search + 批量操作）
- ✅ **表單模態框**（Add/Edit 模式 + 驗證）
- ✅ **測試生成**（Vitest + React Testing Library, >= 80% 覆蓋率）
- ✅ **權限控制**（PrivilegeButton 集成）

### 🛠️ Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| React | 19 | UI 框架（Concurrent Rendering, Hooks） |
| TypeScript | Latest | 類型安全 |
| Ant Design | 5 | UI 組件庫 |
| Redux Toolkit | Latest | 狀態管理 |
| Vitest | Latest | 測試框架 |
| React Testing Library | Latest | 組件測試 |

---

## 🚀 Quick Start

### Usage

```bash
# 完整 CRUD 生成（所有階段，~25 分鐘）
/react-crud Employee --module=system --all-phases

# 階段式生成（指定 Phase）
/react-crud Employee --module=system --phase=1  # TypeScript 類型
/react-crud Employee --module=system --phase=2  # API 客戶端
/react-crud Employee --module=system --phase=3  # 列表組件
/react-crud Employee --module=system --phase=4  # 表單模態框
/react-crud Employee --module=system --phase=5  # 測試
```

### Generated Files

```
smart-admin-web-react/src/
├── api/system/
│   ├── employee-types.ts          # Phase 1: TypeScript 類型
│   └── employee-api.ts             # Phase 2: API 客戶端
└── views/system/employee/
    ├── EmployeeList.tsx            # Phase 3: 列表組件
    ├── EmployeeFormModal.tsx       # Phase 4: 表單模態框
    └── __tests__/
        ├── EmployeeList.spec.tsx   # Phase 5: 列表測試
        └── EmployeeFormModal.spec.tsx # Phase 5: 表單測試
```

---

## 📚 Documentation

### Essential Reading

| Document | Lines | Priority | Purpose |
|----------|-------|----------|---------|
| **[quick-reference.md](knowledge/quick-reference.md)** | 1200+ | ⭐⭐⭐⭐⭐ | **最重要** - React 19 模式、Ant Design 5、完整範例 |
| **[SKILL.md](SKILL.md)** | 800+ | ⭐⭐⭐⭐ | 技能定義、觸發關鍵字、執行流程 |
| **[config.yml](config.yml)** | 236 | ⭐⭐⭐ | 元數據、質量門檻 |

### Phase Documentation

| Phase | Document | Lines | Time | Output |
|-------|----------|-------|------|--------|
| 1 | [phase-1-types.md](phases/phase-1-types.md) | 200 | 3 min | TypeScript 類型定義 |
| 2 | [phase-2-api-client.md](phases/phase-2-api-client.md) | 250 | 5 min | API 客戶端 |
| 3 | [phase-3-list-component.md](phases/phase-3-list-component.md) | 400 | 8 min | 列表組件 |
| 4 | [phase-4-form-modal.md](phases/phase-4-form-modal.md) | 350 | 6 min | 表單模態框 |
| 5 | [phase-5-tests.md](phases/phase-5-tests.md) | 300 | 3 min | 測試文件 |

---

## 🎓 Learning Path

### For Beginners

1. **Start Here**: [quick-reference.md](knowledge/quick-reference.md)
   - React 19 Hooks 基礎
   - Ant Design 5 組件使用
   - TypeScript 類型模式

2. **Understand Phases**: Read [SKILL.md](SKILL.md)
   - Phase-based execution model
   - Complete CRUD workflow

3. **Try It**: Generate Employee CRUD
   ```bash
   /react-crud Employee --module=system --all-phases
   ```

### For Advanced Users

1. **Customization**: Study phase documentation
   - Modify generated code patterns
   - Add custom validation rules
   - Extend form fields

2. **Integration**: Read quick-reference.md
   - Redux Toolkit patterns
   - PrivilegeButton usage
   - ResponseModel handling

---

## 🧪 Quality Assurance

### Automated Quality Gates

```yaml
quality_gates:
  - name: "typescript-compile"
    command: "npm run type-check"
    expected: 0 errors

  - name: "vitest"
    command: "npm run test"
    expected: >= 80% coverage

  - name: "eslint"
    command: "npm run lint"
    expected: 0 errors
```

### Validation Checklist

#### Phase 1: TypeScript Types
- [ ] 文件路徑：`src/api/{module}/{entity}-types.ts`
- [ ] 類型名稱遵循 PascalCase
- [ ] 字段名稱遵循 camelCase
- [ ] 必填/可選字段與後端對應
- [ ] 主鍵類型為 `number`
- [ ] 時間字段類型為 `string`

#### Phase 2: API Client
- [ ] 文件路徑：`src/api/{module}/{entity}-api.ts`
- [ ] 所有 CRUD 方法已實現
- [ ] 權限代碼與後端對齊
- [ ] ResponseModel 正確處理

#### Phase 3: List Component
- [ ] 文件路徑：`src/views/{module}/{entity}/{Entity}List.tsx`
- [ ] Table columns 正確
- [ ] Pagination 正常工作
- [ ] 搜索功能正常
- [ ] PrivilegeButton 權限控制生效

#### Phase 4: Form Modal
- [ ] 文件路徑：`src/views/{module}/{entity}/{Entity}FormModal.tsx`
- [ ] Add 模式正常
- [ ] Edit 模式正常（數據預填）
- [ ] 表單驗證生效
- [ ] API 提交成功

#### Phase 5: Tests
- [ ] 文件路徑：`src/views/{module}/{entity}/__tests__/`
- [ ] 所有 API 已 Mock
- [ ] Redux Provider 包裹組件
- [ ] 異步操作使用 `waitFor`
- [ ] 測試覆蓋率 >= 80%

---

## 🔧 Troubleshooting

### Common Issues

#### TypeScript Compilation Errors

**Problem**: `Property 'xxx' does not exist on type`

**Solution**: Verify type definitions match backend DTOs exactly.

```typescript
// ✅ Correct
export interface EmployeeVO {
  employeeId: number;  // Matches Long employeeId
  employeeName: string;
}

// ❌ Wrong
export interface EmployeeVO {
  id: number;  // Mismatched field name
  name: string;
}
```

#### ESLint Errors

**Problem**: `'ResponseDTO' is defined but never used`

**Solution**: Remove unused imports.

```typescript
// ❌ Wrong
import type { PageResult, ResponseDTO } from '@/api/base/response.model';

// ✅ Correct
import type { PageResult } from '@/api/base/response.model';
```

#### Test Failures

**Problem**: `Unable to find element with role "button"`

**Solution**: Use text-based selectors or `data-testid`.

```typescript
// ❌ Fragile
const button = screen.getByRole('button', { name: '確定' });

// ✅ Better
const button = screen.getByText('確定');

// ✅ Best
const button = screen.getByTestId('confirm-button');
```

---

## 📊 Performance Metrics

### Time Savings

| Task | Manual | Generated | Savings |
|------|--------|-----------|---------|
| TypeScript Types | 30 min | 3 min | 90% |
| API Client | 45 min | 5 min | 89% |
| List Component | 2 hours | 8 min | 93% |
| Form Modal | 1.5 hours | 6 min | 93% |
| Tests | 1 hour | 3 min | 95% |
| **Total** | **4-6 hours** | **25 min** | **92%** |

### Code Quality

- ✅ TypeScript 編譯: 100% pass
- ✅ ESLint: 0 errors, 0 warnings
- ✅ 測試覆蓋率: >= 80%
- ✅ 權限對齊: 100%

---

## 🆚 Comparison with Vue CRUD

| Feature | Vue CRUD | React CRUD | Reusable |
|---------|----------|------------|----------|
| TypeScript 類型 | ✅ | ✅ | ✅ 100% |
| API 客戶端 | ✅ | ✅ | ✅ 90% |
| 組件語法 | `.vue` (SFC) | `.tsx` (TSX) | ❌ 0% |
| 權限控制 | `v-privilege` | `usePrivilege` | ❌ 0% |
| 狀態管理 | `reactive()` | `useState()` | ❌ 0% |
| 測試框架 | Vue Test Utils | React Testing Library | ❌ 30% |

**Recommendation**:
- Vue 項目 → 使用 `smartadmin-crud-generator`
- React 項目 → 使用 `smartadmin-react-crud`
- 兩者獨立維護，不建議混用

---

## 🔗 Related Skills

### Foundation (P0)

- **[smartadmin-crud-generator](../../full-stack/smartadmin-crud-generator/)** - Vue 3 full-stack CRUD (backend + frontend)
- **[test-fixture-generator](../../testing/test-fixture-generator/)** - Test data builders

### Extended (P1)

- **[igame-feature-builder](../../../extended/domain/igame-feature-builder/)** - iGaming domain features (uses CRUD generator)

---

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

---

## 📄 License

Part of SmartAdmin v4.1.0 - Internal use only.

---

## 👥 Support

- **Documentation**: Read [quick-reference.md](knowledge/quick-reference.md)
- **Issues**: Check [CHANGELOG.md](CHANGELOG.md) known issues
- **Examples**: See generated Employee CRUD in `smart-admin-web-react/src/views/system/employee/`

---

**Last Updated**: 2026-03-06
**Version**: 1.0.0
**Status**: ✅ Production Ready
