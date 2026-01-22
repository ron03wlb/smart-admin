# Frontend Testing Guide for SmartAdmin

Comprehensive guide for testing Vue 3 components in SmartAdmin project.

## Technology Stack

- **Test Framework:** Vitest (https://vitest.dev/)
- **Testing Library:** @testing-library/vue
- **Component Framework:** Vue 3 Composition API
- **UI Library:** Ant Design Vue
- **State Management:** Pinia
- **Build Tool:** Vite

## Test File Structure

```
smart-admin-web/
├── src/
│   ├── components/
│   │   ├── EmployeeList.vue
│   │   └── __tests__/
│   │       └── EmployeeList.spec.ts
│   ├── views/
│   │   ├── employee/
│   │   │   ├── employee-list.vue
│   │   │   └── __tests__/
│   │   │       └── employee-list.spec.ts
│   └── stores/
│       ├── employeeStore.ts
│       └── __tests__/
│           └── employeeStore.spec.ts
└── vitest.config.ts
```

## Testing Standards

### Coverage Requirements
- **Minimum:** 80% coverage for new components
- **Target:** >85% coverage for core modules
- **Critical paths:** 100% coverage (authentication, permissions, data mutations)

### Test Categories
1. **Unit Tests:** Individual functions, composables, utilities
2. **Component Tests:** Vue components in isolation
3. **Integration Tests:** Component interactions, API integration
4. **E2E Tests:** Full user workflows (Cypress/Playwright)

## Unit Testing Examples

### Testing Composables

```typescript
// src/composables/__tests__/usePermission.spec.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { usePermission } from '../usePermission'
import { setActivePinia, createPinia } from 'pinia'

describe('usePermission', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should check permission correctly', () => {
    const { hasPermission } = usePermission()
    expect(hasPermission('employee:add')).toBe(false)
  })

  it('should handle multiple permissions', () => {
    const { hasAnyPermission, hasAllPermissions } = usePermission()
    const perms = ['employee:add', 'employee:edit']

    expect(hasAnyPermission(perms)).toBe(false)
    expect(hasAllPermissions(perms)).toBe(false)
  })
})
```

### Testing Utilities

```typescript
// src/utils/__tests__/format.spec.ts
import { describe, it, expect } from 'vitest'
import { formatDate, formatCurrency } from '../format'

describe('format utilities', () => {
  describe('formatDate', () => {
    it('should format date correctly', () => {
      const date = new Date('2026-01-21T10:30:00')
      expect(formatDate(date, 'YYYY-MM-DD')).toBe('2026-01-21')
    })

    it('should handle null values', () => {
      expect(formatDate(null)).toBe('-')
    })
  })

  describe('formatCurrency', () => {
    it('should format currency with symbol', () => {
      expect(formatCurrency(1234.56)).toBe('¥1,234.56')
    })
  })
})
```

## Component Testing

### Testing Simple Components

```typescript
// src/components/__tests__/EmployeeCard.spec.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import EmployeeCard from '../EmployeeCard.vue'

describe('EmployeeCard', () => {
  it('should render employee information', () => {
    const employee = {
      id: 1,
      name: 'John Doe',
      department: 'Engineering',
      email: 'john@example.com'
    }

    const wrapper = mount(EmployeeCard, {
      props: { employee }
    })

    expect(wrapper.text()).toContain('John Doe')
    expect(wrapper.text()).toContain('Engineering')
    expect(wrapper.text()).toContain('john@example.com')
  })

  it('should emit edit event on button click', async () => {
    const wrapper = mount(EmployeeCard, {
      props: { employee: { id: 1, name: 'John' } }
    })

    await wrapper.find('[data-testid="edit-btn"]').trigger('click')
    expect(wrapper.emitted('edit')).toBeTruthy()
    expect(wrapper.emitted('edit')?.[0]).toEqual([1])
  })
})
```

### Testing Ant Design Vue Components

```typescript
// src/views/employee/__tests__/employee-list.spec.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import EmployeeList from '../employee-list.vue'
import Antd from 'ant-design-vue'

describe('EmployeeList', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render table with employee data', async () => {
    const wrapper = mount(EmployeeList, {
      global: {
        plugins: [Antd]
      }
    })

    // Mock API response
    const mockData = [
      { id: 1, name: 'John', department: 'Engineering' },
      { id: 2, name: 'Jane', department: 'Marketing' }
    ]

    await wrapper.vm.loadData(mockData)
    await wrapper.vm.$nextTick()

    const rows = wrapper.findAll('[data-testid="employee-row"]')
    expect(rows).toHaveLength(2)
  })

  it('should handle pagination correctly', async () => {
    const wrapper = mount(EmployeeList, {
      global: { plugins: [Antd] }
    })

    // Trigger page change
    await wrapper.vm.onPageChange(2)
    expect(wrapper.vm.currentPage).toBe(2)
  })
})
```

### Testing Form Components

```typescript
// src/views/employee/__tests__/employee-form-modal.spec.ts
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import EmployeeFormModal from '../employee-form-modal.vue'
import Antd from 'ant-design-vue'

describe('EmployeeFormModal', () => {
  it('should validate required fields', async () => {
    const wrapper = mount(EmployeeFormModal, {
      global: { plugins: [Antd] },
      props: { visible: true }
    })

    // Submit without filling required fields
    await wrapper.find('[data-testid="submit-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    // Check for validation errors
    expect(wrapper.text()).toContain('请输入员工姓名')
    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  it('should submit valid form data', async () => {
    const wrapper = mount(EmployeeFormModal, {
      global: { plugins: [Antd] },
      props: { visible: true }
    })

    // Fill form
    await wrapper.find('[data-testid="name-input"]').setValue('John Doe')
    await wrapper.find('[data-testid="email-input"]').setValue('john@example.com')
    await wrapper.find('[data-testid="submit-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    // Check emit
    expect(wrapper.emitted('submit')).toBeTruthy()
    expect(wrapper.emitted('submit')?.[0][0]).toMatchObject({
      name: 'John Doe',
      email: 'john@example.com'
    })
  })
})
```

## API Mocking

### Using vi.mock for API calls

```typescript
// src/api/__tests__/employee-api.spec.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getEmployeeList, addEmployee } from '../employee-api'
import { postRequest } from '@/lib/request'

// Mock the request module
vi.mock('@/lib/request', () => ({
  postRequest: vi.fn(),
  getRequest: vi.fn()
}))

describe('employee-api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should fetch employee list', async () => {
    const mockResponse = {
      success: true,
      data: {
        list: [{ id: 1, name: 'John' }],
        total: 1
      }
    }

    vi.mocked(postRequest).mockResolvedValue(mockResponse)

    const result = await getEmployeeList({ pageNum: 1, pageSize: 20 })

    expect(postRequest).toHaveBeenCalledWith('/employee/query', {
      pageNum: 1,
      pageSize: 20
    })
    expect(result).toEqual(mockResponse)
  })

  it('should add employee', async () => {
    const mockResponse = { success: true, data: 123 }
    vi.mocked(postRequest).mockResolvedValue(mockResponse)

    const employee = { name: 'John', email: 'john@example.com' }
    const result = await addEmployee(employee)

    expect(postRequest).toHaveBeenCalledWith('/employee/add', employee)
    expect(result.success).toBe(true)
  })
})
```

### Using MSW (Mock Service Worker)

```typescript
// src/mocks/handlers.ts
import { rest } from 'msw'

export const handlers = [
  rest.post('/api/employee/query', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        success: true,
        data: {
          list: [
            { id: 1, name: 'John Doe', department: 'Engineering' },
            { id: 2, name: 'Jane Smith', department: 'Marketing' }
          ],
          total: 2
        }
      })
    )
  }),

  rest.post('/api/employee/add', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({ success: true, data: 123 })
    )
  })
]

// vitest.setup.ts
import { setupServer } from 'msw/node'
import { handlers } from './mocks/handlers'

export const server = setupServer(...handlers)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

## Pinia Store Testing

```typescript
// src/stores/__tests__/employeeStore.spec.ts
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useEmployeeStore } from '../employeeStore'
import * as employeeApi from '@/api/employee-api'

vi.mock('@/api/employee-api')

describe('employeeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should load employee list', async () => {
    const store = useEmployeeStore()
    const mockData = {
      success: true,
      data: {
        list: [{ id: 1, name: 'John' }],
        total: 1
      }
    }

    vi.mocked(employeeApi.getEmployeeList).mockResolvedValue(mockData)

    await store.loadEmployees({ pageNum: 1, pageSize: 20 })

    expect(store.employees).toHaveLength(1)
    expect(store.total).toBe(1)
  })

  it('should handle API errors', async () => {
    const store = useEmployeeStore()
    vi.mocked(employeeApi.getEmployeeList).mockRejectedValue(new Error('API Error'))

    await expect(store.loadEmployees({ pageNum: 1, pageSize: 20 })).rejects.toThrow()
    expect(store.employees).toHaveLength(0)
  })
})
```

## Testing SmartAdmin-Specific Patterns

### Testing v-privilege Directive

```typescript
// src/directives/__tests__/privilege.spec.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { privilegeDirective } from '../privilege'
import { useUserStore } from '@/stores/userStore'

describe('v-privilege directive', () => {
  it('should show element when user has permission', async () => {
    const userStore = useUserStore()
    userStore.permissions = ['employee:add']

    const TestComponent = defineComponent({
      directives: { privilege: privilegeDirective },
      template: '<button v-privilege="\'employee:add\'">Add</button>'
    })

    const wrapper = mount(TestComponent)
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('should hide element when user lacks permission', () => {
    const userStore = useUserStore()
    userStore.permissions = []

    const TestComponent = defineComponent({
      directives: { privilege: privilegeDirective },
      template: '<button v-privilege="\'employee:add\'">Add</button>'
    })

    const wrapper = mount(TestComponent)
    expect(wrapper.find('button').exists()).toBe(false)
  })
})
```

### Testing ResponseModel Integration

```typescript
// src/composables/__tests__/useRequest.spec.ts
import { describe, it, expect, vi } from 'vitest'
import { useRequest } from '../useRequest'
import { postRequest } from '@/lib/request'

vi.mock('@/lib/request')

describe('useRequest composable', () => {
  it('should handle successful response', async () => {
    const mockResponse = {
      success: true,
      data: { id: 1, name: 'John' },
      message: 'Success'
    }

    vi.mocked(postRequest).mockResolvedValue(mockResponse)

    const { data, error, execute } = useRequest('/api/test')
    await execute()

    expect(data.value).toEqual({ id: 1, name: 'John' })
    expect(error.value).toBeNull()
  })

  it('should handle error response', async () => {
    const mockResponse = {
      success: false,
      message: 'Error occurred'
    }

    vi.mocked(postRequest).mockResolvedValue(mockResponse)

    const { data, error, execute } = useRequest('/api/test')
    await execute()

    expect(data.value).toBeNull()
    expect(error.value).toBe('Error occurred')
  })
})
```

## Running Tests

```bash
# Run all tests
npm run test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage

# Run specific test file
npm run test employee-list.spec.ts

# Run tests matching pattern
npm run test employee

# UI mode (interactive)
npm run test:ui
```

## Coverage Requirements

```json
// vitest.config.ts
export default defineConfig({
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      lines: 85,
      functions: 85,
      branches: 80,
      statements: 85,
      exclude: [
        'node_modules/',
        'src/**/__tests__/**',
        'src/**/*.spec.ts',
        'src/mocks/**'
      ]
    }
  }
})
```

## Best Practices

### 1. Test Structure (AAA Pattern)
```typescript
it('should do something', async () => {
  // Arrange: Set up test data
  const employee = { name: 'John', email: 'john@example.com' }

  // Act: Execute the operation
  const result = await addEmployee(employee)

  // Assert: Verify the outcome
  expect(result.success).toBe(true)
})
```

### 2. Use data-testid Attributes
```vue
<template>
  <button data-testid="add-employee-btn" @click="handleAdd">
    Add Employee
  </button>
</template>
```

### 3. Test User Behavior, Not Implementation
```typescript
// BAD: Testing implementation details
it('should call loadData method', () => {
  expect(wrapper.vm.loadData).toHaveBeenCalled()
})

// GOOD: Testing user-visible behavior
it('should display loading spinner while fetching data', async () => {
  expect(wrapper.find('[data-testid="loading"]').exists()).toBe(true)
})
```

### 4. Mock External Dependencies
```typescript
// Mock router
const mockRouter = {
  push: vi.fn()
}

// Mount with mocked router
mount(Component, {
  global: {
    mocks: {
      $router: mockRouter
    }
  }
})
```

### 5. Clean Up After Tests
```typescript
afterEach(() => {
  vi.clearAllMocks()
  // Reset stores
  const pinia = getActivePinia()
  pinia._s.clear()
})
```

## Integration with java-architect

**Coordinate on:**
- API contract testing (ensure frontend tests match backend ResponseDTO)
- Mock data should match backend entity structures
- Permission strings match @SaCheckPermission values
- Form validation rules match backend @Valid constraints

## Integration with devops-engineer

**Coordinate on:**
- CI/CD test automation
- Coverage reporting in pipelines
- Test environments and databases
- Performance testing integration

---

Last Updated: 2026-01-21 (v2.3.0)
