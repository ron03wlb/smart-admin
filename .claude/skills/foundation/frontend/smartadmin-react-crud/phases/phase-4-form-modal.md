# Phase 4: 表單模態框生成

**預計時間**: ~6 分鐘
**輸出文件**: `smart-admin-web-react/src/views/{module}/{entity}/{Entity}FormModal.tsx`
**依賴**: Phase 1（類型）、Phase 2（API 客戶端）

---

## 概述

Phase 4 生成表單模態框組件，支持 Add/Edit 兩種模式，包含表單驗證和 API 提交邏輯。

---

## 組件結構

```tsx
import React, { useEffect } from 'react';
import { Modal, Form, Input, message, Select, InputNumber } from 'antd';
import { {entity}Api } from '@/api/{module}/{entity}-api';
import type { {Entity}VO, {Entity}AddForm, {Entity}UpdateForm } from '@/api/{module}/{entity}-types';

interface {Entity}FormModalProps {
  visible: boolean;
  record: {Entity}VO | null;
  onCancel: () => void;
  onSuccess: () => void;
}

/**
 * {Entity} 表單模態框（Add/Edit 模式）
 */
export const {Entity}FormModal: React.FC<{Entity}FormModalProps> = ({
  visible,
  record,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);
  const isEdit = record !== null;

  // ===== Form Initialization =====
  useEffect(() => {
    if (visible && record) {
      // Edit mode: fill form with existing data
      form.setFieldsValue(record);
    } else if (visible) {
      // Add mode: reset form
      form.resetFields();
    }
  }, [visible, record, form]);

  // ===== Form Submission =====
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const response = isEdit
        ? await {entity}Api.update({
            ...values,
            {entity}Id: record.{entity}Id,
          } as {Entity}UpdateForm)
        : await {entity}Api.add(values as {Entity}AddForm);

      if (response.success) {
        message.success(isEdit ? '更新成功' : '添加成功');
        onSuccess();
      } else {
        message.error(response.msg || (isEdit ? '更新失敗' : '添加失敗'));
      }
    } catch (error) {
      // Validation failed or API error
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '編輯{Entity}' : '新增{Entity}'}
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        labelCol={{ span: 6 }}
        wrapperCol={{ span: 16 }}
        autoComplete="off"
      >
        {/* 根據實體字段添加表單項 */}

        {/* 文本輸入 */}
        <Form.Item
          label="名稱"
          name="name"
          rules={[
            { required: true, message: '請輸入名稱' },
            { min: 2, max: 100, message: '長度為 2-100 個字符' },
          ]}
        >
          <Input placeholder="請輸入名稱" />
        </Form.Item>

        {/* 郵箱輸入 */}
        <Form.Item
          label="郵箱"
          name="email"
          rules={[
            { required: true, message: '請輸入郵箱' },
            { type: 'email', message: '郵箱格式不正確' },
          ]}
        >
          <Input placeholder="請輸入郵箱" />
        </Form.Item>

        {/* 下拉選擇 */}
        <Form.Item
          label="部門"
          name="departmentId"
          rules={[{ required: true, message: '請選擇部門' }]}
        >
          <Select placeholder="請選擇部門">
            <Select.Option value={1}>技術部</Select.Option>
            <Select.Option value={2}>產品部</Select.Option>
            <Select.Option value={3}>運營部</Select.Option>
          </Select>
        </Form.Item>

        {/* 數字輸入 */}
        <Form.Item label="薪資" name="salary">
          <InputNumber
            min={0}
            max={1000000}
            precision={2}
            style={{ width: '100%' }}
            placeholder="請輸入薪資"
          />
        </Form.Item>

        {/* 文本域 */}
        <Form.Item label="備註" name="remark">
          <Input.TextArea rows={4} placeholder="請輸入備註" />
        </Form.Item>
      </Form>
    </Modal>
  );
};
```

---

## 關鍵模式

### 1. Add/Edit Mode Detection

```tsx
const isEdit = record !== null;

// 標題
title={isEdit ? '編輯Employee' : '新增Employee'}

// API 調用
const response = isEdit
  ? await employeeApi.update({
      ...values,
      employeeId: record.employeeId,
    })
  : await employeeApi.add(values);

// 成功消息
message.success(isEdit ? '更新成功' : '添加成功');
```

### 2. Form Initialization

```tsx
useEffect(() => {
  if (visible && record) {
    // Edit mode: fill form with existing data
    form.setFieldsValue(record);
  } else if (visible) {
    // Add mode: reset form
    form.resetFields();
  }
}, [visible, record, form]);
```

### 3. Form Validation Rules

```tsx
<Form.Item
  label="姓名"
  name="employeeName"
  rules={[
    { required: true, message: '請輸入姓名' },
    { min: 2, max: 50, message: '長度為 2-50 個字符' },
    { pattern: /^[a-zA-Z\u4e00-\u9fa5]+$/, message: '只能包含中英文字符' },
  ]}
>
  <Input placeholder="請輸入姓名" />
</Form.Item>
```

**常用驗證規則**：
- `required: true` - 必填
- `type: 'email'` - 郵箱格式
- `min: 2, max: 50` - 長度限制
- `pattern: /regex/` - 正則表達式
- `validator: (rule, value) => Promise` - 自定義驗證

### 4. Form Submission

```tsx
const handleSubmit = async () => {
  try {
    // 驗證所有字段
    const values = await form.validateFields();

    setLoading(true);

    // 調用 API
    const response = isEdit
      ? await employeeApi.update({
          ...values,
          employeeId: record.employeeId,
        })
      : await employeeApi.add(values);

    // 處理響應
    if (response.success) {
      message.success(isEdit ? '更新成功' : '添加成功');
      onSuccess();  // 通知父組件刷新數據
    } else {
      message.error(response.msg || '操作失敗');
    }
  } catch (error) {
    // 驗證失敗（自動顯示錯誤信息）
  } finally {
    setLoading(false);
  }
};
```

---

## 表單字段類型

### 1. 文本輸入（Input）

```tsx
<Form.Item label="名稱" name="name">
  <Input placeholder="請輸入名稱" />
</Form.Item>
```

### 2. 文本域（TextArea）

```tsx
<Form.Item label="備註" name="remark">
  <Input.TextArea rows={4} placeholder="請輸入備註" />
</Form.Item>
```

### 3. 數字輸入（InputNumber）

```tsx
<Form.Item label="年齡" name="age">
  <InputNumber min={18} max={100} style={{ width: '100%' }} />
</Form.Item>
```

### 4. 下拉選擇（Select）

```tsx
<Form.Item label="部門" name="departmentId">
  <Select placeholder="請選擇部門">
    <Select.Option value={1}>技術部</Select.Option>
    <Select.Option value={2}>產品部</Select.Option>
  </Select>
</Form.Item>
```

### 5. 日期選擇（DatePicker）

```tsx
import { DatePicker } from 'antd';

<Form.Item label="入職日期" name="entryDate">
  <DatePicker style={{ width: '100%' }} />
</Form.Item>
```

### 6. 單選框（Radio）

```tsx
import { Radio } from 'antd';

<Form.Item label="狀態" name="status">
  <Radio.Group>
    <Radio value={1}>啟用</Radio>
    <Radio value={2}>禁用</Radio>
  </Radio.Group>
</Form.Item>
```

### 7. 複選框（Checkbox）

```tsx
import { Checkbox } from 'antd';

<Form.Item label="權限" name="permissions" valuePropName="checked">
  <Checkbox.Group>
    <Checkbox value="read">讀取</Checkbox>
    <Checkbox value="write">寫入</Checkbox>
    <Checkbox value="delete">刪除</Checkbox>
  </Checkbox.Group>
</Form.Item>
```

---

## 驗證清單

- [ ] 文件路徑正確：`smart-admin-web-react/src/views/{module}/{entity}/{Entity}FormModal.tsx`
- [ ] Add 模式：表單為空，點擊確定調用 `add` API
- [ ] Edit 模式：表單預填數據，點擊確定調用 `update` API（含 ID）
- [ ] 表單驗證規則與後端 `@Valid` 註解對應
- [ ] 必填字段有 `required: true` 規則
- [ ] 提交成功後調用 `onSuccess()` 刷新列表
- [ ] 提交失敗顯示錯誤消息
- [ ] Modal 關閉時銷毀表單（`destroyOnClose`）
- [ ] TypeScript 編譯無錯誤

---

**Phase 4 完成標準**：
- ✅ 表單模態框創建成功
- ✅ Add 模式正常工作
- ✅ Edit 模式正常工作（預填數據）
- ✅ 表單驗證生效
- ✅ API 提交成功

**下一步**：進入 [Phase 5: 測試生成](phase-5-tests.md)
