## Common Patterns

### Enum Handling

**Backend:**
```java
public enum ProductStatusEnum implements BaseEnum {
    AVAILABLE(1, "可用"),
    OUT_OF_STOCK(2, "缺货"),
    DISCONTINUED(3, "已下架");

    private final Integer value;
    private final String desc;
}
```

**Frontend:**
```typescript
export const ProductStatusEnum = {
  AVAILABLE: { value: 1, label: '可用' },
  OUT_OF_STOCK: { value: 2, label: '缺货' },
  DISCONTINUED: { value: 3, label: '已下架' },
} as const;
```

### Foreign Key Relationships

**Service layer joins category name:**
```java
// In ProductDao.xml
LEFT JOIN t_category c ON p.category_id = c.category_id

// In ProductVO
private String categoryName;  // Populated from join
```

**Frontend displays category:**
```vue
<template>
  <a-select v-model:value="form.categoryId">
    <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">
      {{ cat.name }}
    </a-select-option>
  </a-select>
</template>
```

### Unique Constraint Validation

**Service validates before insert/update:**
```java
ProductEntity existing = productDao.getByProductCode(form.getProductCode(), null);
if (existing != null) {
    return ResponseDTO.userErrorParam("商品编码已存在");
}
```

**Frontend shows immediate feedback:**
```vue
<a-form-item label="商品编码" name="productCode">
  <a-input
    v-model:value="form.productCode"
    @blur="checkProductCodeUnique"
  />
</a-form-item>
```

