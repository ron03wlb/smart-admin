/**
 * Reload List Page
 * 重載列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState, useRef } from 'react';
import { Card, Alert, Table, Button } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { reloadApi } from '@/api/support/reloadApi';
import { usePrivilege } from '@/hooks/usePrivilege';
import { RELOAD_PERMISSION, RELOAD_TABLE_COLUMNS_WIDTH } from '@/constants/support/reloadConst';
import type { ReloadVO } from './types';
import DoReloadFormModal, { type DoReloadFormModalRef } from './components/DoReloadFormModal';
import ReloadResultModal, { type ReloadResultModalRef } from './components/ReloadResultModal';

const ReloadListPage: React.FC = () => {
  const hasExecutePrivilege = usePrivilege(RELOAD_PERMISSION.EXECUTE);
  const hasResultPrivilege = usePrivilege(RELOAD_PERMISSION.RESULT);

  const [tableData, setTableData] = useState<ReloadVO[]>([]);
  const [tableLoading, setTableLoading] = useState(false);

  const doReloadFormRef = useRef<DoReloadFormModalRef>(null);
  const reloadResultRef = useRef<ReloadResultModalRef>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setTableLoading(true);
      const res = await reloadApi.queryList();
      setTableData(res.data);
    } catch (error) {
      console.error('Failed to fetch reload list:', error);
    } finally {
      setTableLoading(false);
    }
  };

  const handleDoReload = (tag: string) => {
    doReloadFormRef.current?.showModal(tag);
  };

  const handleShowResult = (tag: string) => {
    reloadResultRef.current?.showModal(tag);
  };

  const columns: ColumnsType<ReloadVO> = [
    {
      title: '標籤',
      dataIndex: 'tag',
      key: 'tag',
      width: RELOAD_TABLE_COLUMNS_WIDTH.tag,
    },
    {
      title: '運行標識',
      dataIndex: 'identification',
      key: 'identification',
      width: RELOAD_TABLE_COLUMNS_WIDTH.identification,
    },
    {
      title: '參數',
      dataIndex: 'args',
      key: 'args',
      width: RELOAD_TABLE_COLUMNS_WIDTH.args,
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: RELOAD_TABLE_COLUMNS_WIDTH.updateTime,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: RELOAD_TABLE_COLUMNS_WIDTH.createTime,
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      fixed: 'right',
      width: RELOAD_TABLE_COLUMNS_WIDTH.action,
      render: (_: any, record: ReloadVO) => (
        <div>
          {hasExecutePrivilege && (
            <Button type="link" size="small" onClick={() => handleDoReload(record.tag)}>
              執行
            </Button>
          )}
          {hasResultPrivilege && (
            <Button type="link" size="small" onClick={() => handleShowResult(record.tag)}>
              查看結果
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <Card size="small" bordered={false} hoverable>
      <Alert
        message={<h4>Smart-Reload 心跳服務介紹：</h4>}
        description={
          <pre>
            {`簡介：SmartReload是一個可以在不重啟進程的情況下動態重新加載配置或者執行某些預先設置的代碼。

原理：
- Java後端會在項目啟動的時候開啟一個Daemon線程，這個Daemon線程會每隔幾秒輪詢t_smart_item表的狀態。
- 如果【狀態標識】與【上次狀態標識】比較發生變化，會將參數傳入SmartReload實現類，進行自定義操作。
用途：
· 用於刷新內存中的緩存
· 用於執行某些後門代碼
· 用於進行Java熱加載（前提是類結構不發生變化）
· 其他不能重啟服務的應用`}
          </pre>
        }
        type="info"
        style={{ marginBottom: 16 }}
      />

      <Table
        rowKey="tag"
        columns={columns}
        dataSource={tableData}
        loading={tableLoading}
        pagination={false}
        size="small"
        bordered
      />

      <DoReloadFormModal ref={doReloadFormRef} onRefresh={fetchData} />
      <ReloadResultModal ref={reloadResultRef} />
    </Card>
  );
};

export default ReloadListPage;
