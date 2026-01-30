# Vue 3 Composition API Patterns for SmartAdmin

## Reactive State

```typescript
import { ref, reactive } from 'vue';

// ref - for primitive values
const count = ref(0);
const visible = ref(false);

// reactive - for objects
const queryForm = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
});
```

## Computed Properties

```typescript
import { computed } from 'vue';

const isEdit = computed(() => !!props.formData);
const hasPermission = computed(() => store.hasPermission('brand:add'));
```

## Watchers

```typescript
import { watch } from 'vue';

watch(() => props.visible, (visible) => {
  if (visible) {
    loadData();
  }
});
```

## Lifecycle Hooks

```typescript
import { onMounted, onUnmounted } from 'vue';

onMounted(() => {
  loadTableData();
});

onUnmounted(() => {
  cleanup();
});
```

## Ant Design Vue Components

```vue
<a-table :columns="columns" :data-source="data" />
<a-form :model="formModel" :rules="rules" />
<a-modal v-model:visible="visible" title="Title" />
```
