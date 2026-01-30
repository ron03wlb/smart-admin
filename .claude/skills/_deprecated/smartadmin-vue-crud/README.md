# [已廢棄] smartadmin-vue-crud

## ⚠️ 廢棄通知

- **軟廢棄時間**：2026-01-27
- **硬廢棄時間**：2026-06-30（屆時移除）
- **廢棄原因**：功能已整合至 `smartadmin-crud-generator`（frontend mode）

---

## 🔄 遷移路徑

**新技能**：`smartadmin-crud-generator`（frontend mode）

### 主要差異

| 特性 | 舊技能（vue-crud） | 新技能（crud-generator） |
|------|------------------|------------------------|
| Vue 版本 | Vue 2 | Vue 3 |
| API 風格 | Options API | Composition API |
| UI 庫 | Ant Design Vue 2 | Ant Design Vue 4 |
| 生成範圍 | 僅前端 | 前端 + 後端 + 測試 |
| 執行時間 | 10 分鐘 | 8 分鐘（frontend-only 模式） |

---

## 🔧 遷移步驟

### Step 1: 使用新技能重新生成模塊

```bash
# 舊命令（已廢棄）
/vue-crud Employee

# 新命令（推薦）
/crud Employee --frontend-only
```

---

### Step 2: 手動遷移自定義業務邏輯（如有）

**Vue 2 Options API → Vue 3 Composition API**：

**舊代碼（Vue 2）**：
```vue
<script>
export default {
  data() {
    return {
      employeeList: [],
      loading: false
    }
  },
  methods: {
    async loadData() {
      this.loading = true
      const response = await employeeApi.queryPage(this.queryForm)
      this.employeeList = response.data.list
      this.loading = false
    }
  },
  mounted() {
    this.loadData()
  }
}
</script>
```

**新代碼（Vue 3）**：
```vue
<script setup>
import { ref, onMounted } from 'vue'
import employeeApi from '@/api/employee-api'

const employeeList = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  const response = await employeeApi.queryPage(queryForm.value)
  employeeList.value = response.data.list
  loading.value = false
}

onMounted(() => {
  loadData()
})
</script>
```

**遷移工具**：
- Vue 3 遷移助手：`vue-migration-helper`
- 自動轉換：`gogocode-plugin-vue`

---

### Step 3: 更新測試用例（Composition API）

**舊測試（Vue 2）**：
```javascript
import { mount } from '@vue/test-utils'
import EmployeeList from '@/views/employee/employee-list.vue'

test('renders employee list', () => {
  const wrapper = mount(EmployeeList)
  expect(wrapper.vm.employeeList).toEqual([])
})
```

**新測試（Vue 3）**：
```javascript
import { mount } from '@vue/test-utils'
import EmployeeList from '@/views/employee/employee-list.vue'

test('renders employee list', () => {
  const wrapper = mount(EmployeeList)
  expect(wrapper.vm.employeeList.value).toEqual([])  // 注意 .value
})
```

---

## 📚 相關資源

- **新技能文檔**：[smartadmin-crud-generator](../../foundation/full-stack/smartadmin-crud-generator/README.md)
- **Vue 3 遷移指南**：[Vue 3 Migration Guide](https://v3-migration.vuejs.org/)
- **Composition API 指南**：[Vue 3 Composition API](https://vuejs.org/guide/extras/composition-api-faq.html)

---

## 📞 需要幫助？

如果遷移過程中遇到問題，請：
1. 查看 [Vue 3 Migration Guide](https://v3-migration.vuejs.org/)
2. 查看 [smartadmin-crud-generator examples](../../foundation/full-stack/smartadmin-crud-generator/examples/)
3. 提交 Issue：[GitHub Issues](https://github.com/1024-lab/smart-admin/issues)

---

**Last Updated**: 2026-01-30
