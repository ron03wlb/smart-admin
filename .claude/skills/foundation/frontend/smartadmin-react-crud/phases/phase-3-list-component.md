# Phase 3: 列表組件生成

**預計時間**: ~8 分鐘
**輸出文件**: `smart-admin-web-react/src/views/{module}/{entity}/{Entity}List.tsx`
**依賴**: Phase 1（類型）、Phase 2（API 客戶端）

---

## 概述

Phase 3 生成 React 列表組件，包含 Table、Pagination、Search、PrivilegeButton 等核心功能。

---

## 組件結構

```tsx
import React, { useState, useEffect } from 'react';
import { Table, Button, message, Form, Input, Space, Modal } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { PrivilegeButton } from '@/components/framework/privilege/PrivilegeButton';
import { {entity}Api } from '@/api/{module}/{entity}-api';
import type { {Entity}VO, {Entity}QueryForm } from '@/api/{module}/{entity}-types';
import { {Entity}FormModal } from './{Entity}FormModal';

/**
 * {Entity} 列表頁面
 */
export const {Entity}List: React.FC = () => {
  // ===== State Management =====
  const [data, setData] = useState<{Entity}VO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [queryForm, setQueryForm] = useState<{Entity}QueryForm>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
  });
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<{Entity}VO | null>(null);

  // ===== Data Fetching =====
  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await {entity}Api.query(queryForm);
      if (response.success) {
        setData(response.data.list);
        setTotal(response.data.total);
      } else {
        message.error(response.msg || '查詢失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [queryForm]);

  // ===== Event Handlers =====
  const handleAdd = () => {
    setCurrentRecord(null);
    setFormModalVisible(true);
  };

  const handleEdit = (record: {Entity}VO) => {
    setCurrentRecord(record);
    setFormModalVisible(true);
  };

  const handleDelete = (record: {Entity}VO) => {
    Modal.confirm({
      title: '確認刪除',
      content: `確定要刪除「${record.name}」嗎？`,
      onOk: async () => {
        const response = await {entity}Api.delete(record.{entity}Id);
        if (response.success) {
          message.success('刪除成功');
          fetchData();
        } else {
          message.error(response.msg || '刪除失敗');
        }
      },
    });
  };

  const handleBatchDelete = () => {
    Modal.confirm({
      title: '批量刪除確認',
      content: `確定要刪除選中的 ${selectedRowKeys.length} 條記錄嗎？`,
      onOk: async () => {
        const response = await {entity}Api.batchDelete({
          {entity}IdList: selectedRowKeys as number[],
        });
        if (response.success) {
          message.success('批量刪除成功');
          setSelectedRowKeys([]);
          fetchData();
        } else {
          message.error(response.msg || '批量刪除失敗');
        }
      },
    });
  };

  // ===== Table Columns =====
  const columns: ColumnsType<{Entity}VO> = [
    {
      title: 'ID',
      dataIndex: '{entity}Id',
      key: '{entity}Id',
      width: 80,
      fixed: 'left',
    },
    // 根據實體字段添加列
    {
      title: '名稱',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_: any, record: {Entity}VO) => (
        <Space>
          <PrivilegeButton
            permissionCode="{module}:{entity}:update"
            type="link"
            size="small"
            onClick={() => handleEdit(record)}
          >
            編輯
          </PrivilegeButton>
          <PrivilegeButton
            permissionCode="{module}:{entity}:delete"
            type="link"
            size="small"
            danger
            onClick={() => handleDelete(record)}
          >
            刪除
          </PrivilegeButton>
        </Space>
      ),
    },
  ];

  // ===== Render =====
  return (
    <div style={{ padding: 24 }}>
      {/* 搜索表單 */}
      <Form layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item label="關鍵字">
          <Input
            placeholder="請輸入搜索關鍵字"
            value={queryForm.keyword}
            onChange={(e) =>
              setQueryForm({ ...queryForm, keyword: e.target.value })
            }
            onPressEnter={fetchData}
            style={{ width: 200 }}
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={fetchData}>
            查詢
          </Button>
          <Button
            onClick={() =>
              setQueryForm({ pageNum: 1, pageSize: 10, keyword: '' })
            }
            style={{ marginLeft: 8 }}
          >
            重置
          </Button>
        </Form.Item>
      </Form>

      {/* 操作按鈕 */}
      <Space style={{ marginBottom: 16 }}>
        <PrivilegeButton
          permissionCode="{module}:{entity}:add"
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleAdd}
        >
          新增
        </PrivilegeButton>
        <PrivilegeButton
          permissionCode="{module}:{entity}:delete"
          danger
          icon={<DeleteOutlined />}
          disabled={selectedRowKeys.length === 0}
          onClick={handleBatchDelete}
        >
          批量刪除
        </PrivilegeButton>
      </Space>

      {/* 數據表格 */}
      <Table
        dataSource={data}
        columns={columns}
        loading={loading}
        rowKey="{entity}Id"
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{
          current: queryForm.pageNum,
          pageSize: queryForm.pageSize,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 條`,
          onChange: (pageNum, pageSize) =>
            setQueryForm({ ...queryForm, pageNum, pageSize }),
        }}
      />

      {/* 表單模態框 */}
      <{Entity}FormModal
        visible={formModalVisible}
        record={currentRecord}
        onCancel={() => setFormModalVisible(false)}
        onSuccess={() => {
          setFormModalVisible(false);
          fetchData();
        }}
      />
    </div>
  );
};
```

---

## 關鍵模式

### 1. State Management

```tsx
// 數據狀態
const [data, setData] = useState<EmployeeVO[]>([]);      // 表格數據
const [loading, setLoading] = useState(false);          // 加載狀態
const [total, setTotal] = useState(0);                  // 總記錄數

// 查詢參數
const [queryForm, setQueryForm] = useState<EmployeeQueryForm>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
});

// 選中行
const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

// 模態框控制
const [formModalVisible, setFormModalVisible] = useState(false);
const [currentRecord, setCurrentRecord] = useState<EmployeeVO | null>(null);
```

### 2. Data Fetching with useEffect

```tsx
const fetchData = async () => {
  setLoading(true);
  try {
    const response = await employeeApi.query(queryForm);
    if (response.success) {
      setData(response.data.list);
      setTotal(response.data.total);
    } else {
      message.error(response.msg || '查詢失敗');
    }
  } finally {
    setLoading(false);
  }
};

// 當 queryForm 變化時重新查詢
useEffect(() => {
  fetchData();
}, [queryForm]);
```

### 3. Pagination Handling

```tsx
<Table
  pagination={{
    current: queryForm.pageNum,
    pageSize: queryForm.pageSize,
    total: total,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total) => `共 ${total} 條`,
    onChange: (pageNum, pageSize) =>
      setQueryForm({ ...queryForm, pageNum, pageSize }),
  }}
/>
```

### 4. Row Selection

```tsx
<Table
  rowSelection={{
    selectedRowKeys,
    onChange: setSelectedRowKeys,
  }}
/>

// 批量操作按鈕
<Button
  danger
  disabled={selectedRowKeys.length === 0}
  onClick={handleBatchDelete}
>
  批量刪除
</Button>
```

### 5. Confirmation Dialog

```tsx
const handleDelete = (record: EmployeeVO) => {
  Modal.confirm({
    title: '確認刪除',
    content: `確定要刪除「${record.employeeName}」嗎？`,
    onOk: async () => {
      const response = await employeeApi.delete(record.employeeId);
      if (response.success) {
        message.success('刪除成功');
        fetchData();
      } else {
        message.error(response.msg || '刪除失敗');
      }
    },
  });
};
```

---

## 驗證清單

- [ ] 文件路徑正確：`smart-admin-web-react/src/views/{module}/{entity}/{Entity}List.tsx`
- [ ] 導入 Phase 1-2 的類型和 API
- [ ] Table columns 包含實體的所有關鍵字段
- [ ] Pagination 正常工作（翻頁、改變頁大小）
- [ ] 搜索功能正常（輸入關鍵字、點擊查詢）
- [ ] 權限控制生效（PrivilegeButton）
- [ ] 刪除操作有確認框
- [ ] 批量刪除正常工作
- [ ] TypeScript 編譯無錯誤
- [ ] ESLint 無警告

---

**Phase 3 完成標準**：
- ✅ 列表組件創建成功
- ✅ 數據正常加載和展示
- ✅ 分頁、搜索、刪除功能正常
- ✅ 權限控制生效

**下一步**：進入 [Phase 4: 表單模態框生成](phase-4-form-modal.md)
