/**
 * Serial Number Record Modal
 * 單號生成記錄 Modal
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { serialNumberApi } from '@/api/support/serialNumberApi';
import { SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH } from '@/constants/support/serialNumberConst';
import { showTableTotal } from '@/constants/common-const';
import type { SerialNumberRecordVO, SerialNumberRecordQueryForm } from '../types';

export interface SerialNumberRecordModalRef {
  show: (serialNumberId: number) => void;
}

const SerialNumberRecordModal = forwardRef<SerialNumberRecordModalRef>((_, ref) => {
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [tableData, setTableData] = useState<SerialNumberRecordVO[]>([]);
  const [total, setTotal] = useState(0);
  const [queryForm, setQueryForm] = useState<SerialNumberRecordQueryForm>({
    serialNumberId: -1,
    pageNum: 1,
    pageSize: 10,
  });

  // 暴露 show 方法
  useImperativeHandle(ref, () => ({
    show: async (serialNumberId: number) => {
      setVisible(true);
      const newQueryForm: SerialNumberRecordQueryForm = {
        serialNumberId,
        pageNum: 1,
        pageSize: 10,
      };
      setQueryForm(newQueryForm);
      await fetchData(newQueryForm);
    },
  }));

  const fetchData = async (form: SerialNumberRecordQueryForm) => {
    try {
      setLoading(true);
      const res = await serialNumberApi.queryRecord(form);
      setTableData(res.data.list);
      setTotal(res.data.total);
    } catch (error) {
      console.error('Failed to fetch serial number records:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setVisible(false);
    setTableData([]);
    setTotal(0);
  };

  const handleTableChange = (page: number, pageSize: number) => {
    const newQueryForm = {
      ...queryForm,
      pageNum: page,
      pageSize,
    };
    setQueryForm(newQueryForm);
    fetchData(newQueryForm);
  };

  // 表格列定義
  const columns: ColumnsType<SerialNumberRecordVO> = [
    {
      title: '單號ID',
      dataIndex: 'serialNumberId',
      key: 'serialNumberId',
      width: SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.serialNumberId,
    },
    {
      title: '日期',
      dataIndex: 'recordDate',
      key: 'recordDate',
      width: SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.recordDate,
    },
    {
      title: '生成數量',
      dataIndex: 'count',
      key: 'count',
      width: SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.count,
    },
    {
      title: '最後更新值',
      dataIndex: 'lastNumber',
      key: 'lastNumber',
      width: SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.lastNumber,
    },
    {
      title: '上次生成時間',
      dataIndex: 'lastTime',
      key: 'lastTime',
      width: SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.lastTime,
    },
  ];

  return (
    <Modal
      open={visible}
      title="每日生成結果記錄"
      width="60%"
      footer={null}
      onCancel={handleClose}
      destroyOnClose
    >
      <Table
        rowKey={(record, index) => `${record.serialNumberId}-${record.recordDate}-${index}`}
        columns={columns}
        dataSource={tableData}
        loading={loading}
        pagination={{
          current: queryForm.pageNum,
          pageSize: queryForm.pageSize,
          total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: showTableTotal,
          onChange: handleTableChange,
        }}
        size="small"
        bordered
      />
    </Modal>
  );
});

SerialNumberRecordModal.displayName = 'SerialNumberRecordModal';

export default SerialNumberRecordModal;
