/**
 * CodeGenerator Config Drawer
 *
 * 6-Tab drawer container. Provides CodeGeneratorContext to all child tabs.
 * Aggregates data from all tabs on save.
 */
import { useState, useRef, useImperativeHandle, forwardRef, useCallback } from 'react';
import { Drawer, Tabs, Button, Space, message, Spin } from 'antd';
import {
  InfoCircleOutlined,
  UnorderedListOutlined,
  SaveOutlined,
  DeleteOutlined,
  FileSearchOutlined,
  TableOutlined,
} from '@ant-design/icons';
import { codeGeneratorApi } from '@/api/support/code-generator-api';
import type { ColumnInfo, CodeGeneratorConfig } from '@/types/code-generator.types';
import { CodeGeneratorContext } from '../../CodeGeneratorContext';
import type { TableInfo } from '../../CodeGeneratorContext';
import BasicNaming from './BasicNaming';
import FieldList from './FieldList';
import InsertAndUpdate from './InsertAndUpdate';
import DeleteConfig from './DeleteConfig';
import QueryField from './QueryField';
import TableField from './TableField';
import type { BasicNamingRef } from './BasicNaming';
import type { FieldListRef } from './FieldList';
import type { InsertAndUpdateRef } from './InsertAndUpdate';
import type { DeleteConfigRef } from './DeleteConfig';
import type { QueryFieldRef } from './QueryField';
import type { TableFieldRef } from './TableField';

export interface CodeGeneratorConfigDrawerRef {
  open: (table: TableInfo) => void;
}

interface Props {
  onReloadList: () => void;
}

export const CodeGeneratorConfigDrawer = forwardRef<CodeGeneratorConfigDrawerRef, Props>(
  ({ onReloadList }, ref) => {
    const [visible, setVisible] = useState(false);
    const [activeKey, setActiveKey] = useState('1');
    const [loading, setLoading] = useState(false);
    const [tableInfo, setTableInfo] = useState<TableInfo>({ tableName: '', tableComment: '' });
    const [tableColumns, setTableColumns] = useState<ColumnInfo[]>([]);
    const [tableConfig, setTableConfig] = useState<CodeGeneratorConfig | null>(null);

    const basicRef = useRef<BasicNamingRef>(null);
    const fieldRef = useRef<FieldListRef>(null);
    const insertAndUpdateRef = useRef<InsertAndUpdateRef>(null);
    const deleteRef = useRef<DeleteConfigRef>(null);
    const queryRef = useRef<QueryFieldRef>(null);
    const tableFieldRef = useRef<TableFieldRef>(null);

    const loadData = useCallback(async (table: TableInfo) => {
      setLoading(true);
      try {
        const [columnRes, configRes] = await Promise.all([
          codeGeneratorApi.getTableColumns(table.tableName),
          codeGeneratorApi.getConfig(table.tableName),
        ]);

        if (columnRes.success && configRes.success) {
          setTableColumns(columnRes.data);
          setTableConfig(configRes.data);
        }
      } catch {
        message.error('加载配置失败');
      } finally {
        setLoading(false);
      }
    }, []);

    useImperativeHandle(ref, () => ({
      open: (table: TableInfo) => {
        setTableInfo({
          ...table,
          createTime: table.createTime || new Date().toISOString(),
        });
        setActiveKey('1');
        setVisible(true);
        loadData(table);
      },
    }));

    const onClose = () => {
      setVisible(false);
    };

    const onSave = async () => {
      try {
        // Validate forms that have validation
        const basicValid = await basicRef.current?.validateForm();
        const insertValid = await insertAndUpdateRef.current?.validateForm();
        const deleteValid = await deleteRef.current?.validateForm();

        if (!basicValid || !insertValid || !deleteValid) {
          return;
        }

        setLoading(true);

        const basic = basicRef.current!.getForm();
        const fields = fieldRef.current!.getForm();
        const insertAndUpdate = insertAndUpdateRef.current!.getForm();
        const deleteInfo = deleteRef.current!.getForm();
        const queryFields = queryRef.current!.getForm();
        const tableFields = tableFieldRef.current!.getForm();

        const res = await codeGeneratorApi.updateConfig({
          tableName: tableInfo.tableName,
          basic,
          fields,
          insertAndUpdate,
          deleteInfo,
          queryFields,
          tableFields,
        });

        if (res.success) {
          message.success('保存成功');
          onReloadList();
          onClose();
        } else {
          message.error(res.msg || '保存失败');
        }
      } catch {
        message.error('保存失败');
      } finally {
        setLoading(false);
      }
    };

    const tabItems = [
      {
        key: '1',
        label: <span><InfoCircleOutlined /> 1.基础命名</span>,
        forceRender: true,
        children: <BasicNaming ref={basicRef} />,
      },
      {
        key: '2',
        label: <span><UnorderedListOutlined /> 2.字段列表</span>,
        forceRender: true,
        children: <FieldList ref={fieldRef} />,
      },
      {
        key: '3',
        label: <span><SaveOutlined /> 3.增加、修改</span>,
        forceRender: true,
        children: <InsertAndUpdate ref={insertAndUpdateRef} />,
      },
      {
        key: '4',
        label: <span><DeleteOutlined /> 4.删除</span>,
        forceRender: true,
        children: <DeleteConfig ref={deleteRef} />,
      },
      {
        key: '5',
        label: <span><FileSearchOutlined /> 5.查询条件</span>,
        forceRender: true,
        children: <QueryField ref={queryRef} />,
      },
      {
        key: '6',
        label: <span><TableOutlined /> 6.列表</span>,
        forceRender: true,
        children: <TableField ref={tableFieldRef} />,
      },
    ];

    return (
      <Drawer
        title="代码配置"
        open={visible}
        width={1200}
        onClose={onClose}
        maskClosable={false}
        destroyOnClose
        footer={
          <div style={{ textAlign: 'right' }}>
            <Space>
              <Button onClick={onClose}>取消</Button>
              <Button type="primary" onClick={onSave} loading={loading}>保存</Button>
            </Space>
          </div>
        }
      >
        <CodeGeneratorContext.Provider value={{ tableInfo, tableColumns, tableConfig }}>
          <Spin spinning={loading}>
            <Tabs activeKey={activeKey} onChange={setActiveKey} items={tabItems} />
          </Spin>
        </CodeGeneratorContext.Provider>
      </Drawer>
    );
  },
);

CodeGeneratorConfigDrawer.displayName = 'CodeGeneratorConfigDrawer';
