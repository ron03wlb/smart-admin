/**
 * Tab 5: Query Field Configuration
 *
 * Add/remove query conditions with @dnd-kit drag-and-drop sorting.
 * Each row has: query type, column selection, label, fieldName, width.
 */
import { useState, useEffect, useImperativeHandle, forwardRef, useCallback, useRef } from 'react';
import { Table, Button, Input, Select } from 'antd';
import { DragOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import type { DragEndEvent } from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useCodeGeneratorContext } from '../../CodeGeneratorContext';
import { convertLowerCamel, CODE_QUERY_TYPE_LIST } from '../../code-generator-util';
import type { QueryFieldConfig } from '@/types/code-generator.types';

export interface QueryFieldRef {
  getForm: () => QueryFieldConfig[];
}

interface QueryRow {
  rowKey: string;
  label: string;
  fieldName: string;
  queryType: string;
  columnNameList: string | string[] | null;
  width: string;
}

interface SortableRowProps extends React.HTMLAttributes<HTMLTableRowElement> {
  'data-row-key'?: string;
}

function SortableRow(props: SortableRowProps) {
  const rowKey = props['data-row-key'] || '';
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: rowKey });
  const style: React.CSSProperties = {
    ...props.style,
    transform: CSS.Transform.toString(transform),
    transition,
    ...(isDragging ? { position: 'relative', zIndex: 9999 } : {}),
  };

  return <tr {...props} ref={setNodeRef} style={style} {...attributes} {...listeners} />;
}

let rowKeyCounter = 1;

const QueryField = forwardRef<QueryFieldRef>((_props, ref) => {
  const { tableColumns, tableConfig } = useCodeGeneratorContext();
  const [tableData, setTableData] = useState<QueryRow[]>([]);
  const initializedRef = useRef(false);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  useEffect(() => {
    if (!tableConfig || initializedRef.current) return;
    initializedRef.current = true;

    const data = tableConfig.queryFields || [];
    rowKeyCounter = 1;
    const rows: QueryRow[] = data.map((item) => ({
      rowKey: `rowKey${rowKeyCounter++}`,
      label: item.label || '',
      fieldName: item.fieldName || '',
      queryType: item.queryType || '',
      columnNameList: item.columnNameList || null,
      width: item.width || '200px',
    }));
    setTableData(rows);
  }, [tableConfig]);

  const addQuery = useCallback(() => {
    setTableData((prev) => [
      ...prev,
      {
        rowKey: `rowKey${rowKeyCounter++}`,
        label: '',
        fieldName: '',
        queryType: '',
        columnNameList: null,
        width: '200px',
      },
    ]);
  }, []);

  const onDelete = useCallback((index: number) => {
    setTableData((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const updateRow = useCallback((index: number, key: keyof QueryRow, value: unknown) => {
    setTableData((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [key]: value };
      return next;
    });
  }, []);

  const onChangeQueryType = useCallback((value: string, index: number) => {
    setTableData((prev) => {
      const next = [...prev];
      next[index] = {
        ...next[index],
        queryType: value,
        columnNameList: value === 'Like' ? [] : null,
      };
      return next;
    });
  }, []);

  const onSelectColumn = useCallback((record: QueryRow, index: number) => {
    if (Array.isArray(record.columnNameList)) return;
    const columnName = record.columnNameList;
    if (!columnName) return;
    const column = tableColumns.find((c) => c.columnName === columnName);
    setTableData((prev) => {
      const next = [...prev];
      next[index] = {
        ...next[index],
        fieldName: column ? convertLowerCamel(column.columnName) : '',
        label: column?.columnComment || '',
      };
      return next;
    });
  }, [tableColumns]);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      setTableData((prev) => {
        const oldIndex = prev.findIndex((item) => item.rowKey === active.id);
        const newIndex = prev.findIndex((item) => item.rowKey === over.id);
        return arrayMove(prev, oldIndex, newIndex);
      });
    }
  }, []);

  useImperativeHandle(ref, () => ({
    getForm: () =>
      tableData.map((item) => ({
        ...item,
        columnNameList:
          item.columnNameList && typeof item.columnNameList === 'string'
            ? [item.columnNameList]
            : item.columnNameList,
      })),
  }));

  const columns: ColumnsType<QueryRow> = [
    {
      title: '拖拽', dataIndex: 'drag', width: 60,
      render: () => <Button type="text" size="small" style={{ cursor: 'grab', width: '100%', textAlign: 'left' }}><DragOutlined /></Button>,
    },
    {
      title: '查询类型', dataIndex: 'queryType', width: 130,
      render: (val, _r, idx) => (
        <Select value={val || undefined} onChange={(v) => onChangeQueryType(v, idx)} style={{ width: '100%' }} placeholder="选择">
          {CODE_QUERY_TYPE_LIST.map((item) => (
            <Select.Option key={item.value} value={item.value}>{item.label}</Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: '查询列', dataIndex: 'columnNameList',
      render: (val, record, idx) => (
        <Select
          showSearch
          mode={record.queryType === 'Like' ? 'multiple' : undefined}
          value={val ?? undefined}
          onChange={(v) => {
            updateRow(idx, 'columnNameList', v);
            // Auto-fill label and fieldName for single select
            if (typeof v === 'string') {
              const updated = { ...record, columnNameList: v };
              onSelectColumn(updated, idx);
            }
          }}
          style={{ width: '100%' }}
        >
          {tableColumns.map((col) => (
            <Select.Option key={col.columnName} value={col.columnName}>
              {col.columnName}
              {col.columnComment && <span> ({col.columnComment})</span>}
            </Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: '条件名称', dataIndex: 'label', width: 150,
      render: (val, _r, idx) => <Input value={val} onChange={(e) => updateRow(idx, 'label', e.target.value)} placeholder="关键字查询" />,
    },
    {
      title: '字段命名', dataIndex: 'fieldName', width: 150,
      render: (val, _r, idx) => <Input value={val} onChange={(e) => updateRow(idx, 'fieldName', e.target.value)} placeholder="keywords" />,
    },
    {
      title: '宽度', dataIndex: 'width', width: 100,
      render: (val, _r, idx) => <Input value={val} onChange={(e) => updateRow(idx, 'width', e.target.value)} placeholder="150px" />,
    },
    {
      title: '操作', dataIndex: 'operate', width: 60,
      render: (_v, _r, idx) => <Button type="link" danger onClick={() => onDelete(idx)}>删除</Button>,
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 10 }}>
        <Button type="primary" onClick={addQuery}>添加查询条件</Button>
      </div>
      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
        <SortableContext items={tableData.map((d) => d.rowKey)} strategy={verticalListSortingStrategy}>
          <Table<QueryRow>
            size="small"
            bordered
            style={{ marginTop: 10 }}
            dataSource={tableData}
            columns={columns}
            rowKey="rowKey"
            pagination={false}
            components={{ body: { row: SortableRow } }}
          />
        </SortableContext>
      </DndContext>
    </>
  );
});

QueryField.displayName = 'QueryField';
export default QueryField;
