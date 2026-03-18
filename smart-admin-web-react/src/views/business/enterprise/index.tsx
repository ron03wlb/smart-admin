/**
 * Enterprise Management Page
 * 企業管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  message,
  Modal,
  Row,
  Space,
  Table,
  Tag,
  DatePicker,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { enterpriseApi } from '@/api/business/enterpriseApi';
import type { EnterpriseVO, EnterpriseQueryForm } from './types';
import {
  ENTERPRISE_PERMISSION,
  ENTERPRISE_TABLE_COLUMNS_WIDTH,
  ENTERPRISE_TYPE_LABELS,
  ENTERPRISE_TYPE_COLORS,
  DISABLED_FLAG_LABELS,
  DISABLED_FLAG_COLORS,
} from '@/constants/business/enterpriseConst';
import PrivilegeButton from '@/components/PrivilegeButton';
import EnterpriseFormModal from './components/EnterpriseFormModal';

const { RangePicker } = DatePicker;
const { confirm } = Modal;

export default function EnterprisePage() {
  const [queryForm] = Form.useForm<EnterpriseQueryForm>();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<EnterpriseVO[]>([]);
  const [total, setTotal] = useState(0);

  // Pagination state
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Form Modal state
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<EnterpriseVO | undefined>(undefined);

  /**
   * 查詢企業列表
   */
  const fetchEnterpriseList = async () => {
    try {
      setLoading(true);
      const values = queryForm.getFieldsValue();

      const params: EnterpriseQueryForm = {
        ...values,
        pageNum,
        pageSize,
      };

      const res = await enterpriseApi.pageQuery(params);
      if (res.ok && res.data) {
        setDataSource(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } catch (error) {
      message.error('查詢企業列表失敗');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 搜尋處理
   */
  const handleSearch = () => {
    setPageNum(1); // Reset to first page
    fetchEnterpriseList();
  };

  /**
   * 重置搜尋
   */
  const handleReset = () => {
    queryForm.resetFields();
    setPageNum(1);
    setPageSize(10);
    fetchEnterpriseList();
  };

  /**
   * 顯示新增表單
   */
  const handleAdd = () => {
    setFormInitialData(undefined);
    setFormModalVisible(true);
  };

  /**
   * 顯示編輯表單
   */
  const handleEdit = async (enterpriseId: number) => {
    try {
      const res = await enterpriseApi.detail(enterpriseId);
      if (res.ok && res.data) {
        setFormInitialData(res.data);
        setFormModalVisible(true);
      }
    } catch (error) {
      message.error('獲取企業詳情失敗');
      console.error(error);
    }
  };

  /**
   * 刪除企業
   */
  const handleDelete = (record: EnterpriseVO) => {
    confirm({
      title: '刪除企業',
      icon: <ExclamationCircleOutlined />,
      content: `確定要刪除企業「${record.enterpriseName}」嗎？`,
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await enterpriseApi.delete(record.enterpriseId);
          if (res.ok) {
            message.success('刪除成功');
            fetchEnterpriseList();
          }
        } catch (error) {
          message.error('刪除失敗');
          console.error(error);
        }
      },
    });
  };

  /**
   * Form Modal 成功回調
   */
  const handleFormSuccess = () => {
    setFormModalVisible(false);
    setFormInitialData(undefined);
    fetchEnterpriseList();
  };

  /**
   * Form Modal 取消回調
   */
  const handleFormCancel = () => {
    setFormModalVisible(false);
    setFormInitialData(undefined);
  };

  /**
   * 分頁變化處理
   */
  const handlePageChange = (page: number, size: number) => {
    setPageNum(page);
    setPageSize(size);
  };

  /**
   * 日期範圍變化處理
   */
  const handleDateChange = (_dates: any, dateStrings: [string, string]) => {
    queryForm.setFieldsValue({
      createTimeBegin: dateStrings[0],
      createTimeEnd: dateStrings[1],
    });
  };

  useEffect(() => {
    fetchEnterpriseList();
  }, [pageNum, pageSize]);

  /**
   * 表格列定義
   */
  const columns: ColumnsType<EnterpriseVO> = [
    {
      title: '企業名稱',
      dataIndex: 'enterpriseName',
      key: 'enterpriseName',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.enterpriseName,
      ellipsis: true,
    },
    {
      title: '統一社會信用代碼',
      dataIndex: 'unifiedSocialCreditCode',
      key: 'unifiedSocialCreditCode',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.unifiedSocialCreditCode,
      ellipsis: true,
    },
    {
      title: '企業類型',
      dataIndex: 'type',
      key: 'type',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.type,
      render: (type) => (
        <Tag color={ENTERPRISE_TYPE_COLORS[type]}>
          {ENTERPRISE_TYPE_LABELS[type]}
        </Tag>
      ),
    },
    {
      title: '聯系人',
      dataIndex: 'contact',
      key: 'contact',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.contact,
    },
    {
      title: '聯系電話',
      dataIndex: 'contactPhone',
      key: 'contactPhone',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.contactPhone,
    },
    {
      title: '郵箱',
      dataIndex: 'email',
      key: 'email',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.email,
      ellipsis: true,
      render: (text: string) => text || '無',
    },
    {
      title: '狀態',
      dataIndex: 'disabledFlag',
      key: 'disabledFlag',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.disabledFlag,
      render: (disabledFlag: boolean) => (
        <Tag color={DISABLED_FLAG_COLORS[String(disabledFlag) as 'true' | 'false']}>
          {DISABLED_FLAG_LABELS[String(disabledFlag) as 'true' | 'false']}
        </Tag>
      ),
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.createTime,
    },
    {
      title: '操作',
      key: 'action',
      width: ENTERPRISE_TABLE_COLUMNS_WIDTH.action,
      fixed: 'right',
      render: (_: unknown, record: EnterpriseVO) => (
        <Space size="small">
          <PrivilegeButton
            type="link"
            size="small"
            permission={ENTERPRISE_PERMISSION.UPDATE}
            onClick={() => handleEdit(record.enterpriseId)}
          >
            編輯
          </PrivilegeButton>
          <PrivilegeButton
            type="link"
            size="small"
            danger
            permission={ENTERPRISE_PERMISSION.DELETE}
            onClick={() => handleDelete(record)}
          >
            刪除
          </PrivilegeButton>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card>
        {/* 搜尋表單 */}
        <Form form={queryForm} layout="inline" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 16]} style={{ width: '100%' }}>
            <Col>
              <Form.Item name="keywords" label="關鍵字">
                <Input
                  placeholder="企業名稱/聯系人/聯系電話"
                  allowClear
                  style={{ width: 250 }}
                  onPressEnter={handleSearch}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item label="創建時間">
                <RangePicker
                  onChange={handleDateChange}
                  style={{ width: 300 }}
                />
              </Form.Item>
            </Col>
            <Col>
              <Space size="small">
                <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                  搜尋
                </Button>
                <Button icon={<ReloadOutlined />} onClick={handleReset}>
                  重置
                </Button>
              </Space>
            </Col>
          </Row>
        </Form>

        {/* 操作按鈕 */}
        <div style={{ marginBottom: 16 }}>
          <PrivilegeButton
            type="primary"
            icon={<PlusOutlined />}
            permission={ENTERPRISE_PERMISSION.ADD}
            onClick={handleAdd}
          >
            新建企業
          </PrivilegeButton>
        </div>

        {/* 表格 */}
        <Table
          rowKey="enterpriseId"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 條`,
            pageSizeOptions: ['10', '20', '50', '100'],
            onChange: handlePageChange,
          }}
          scroll={{ x: 1300 }}
        />
      </Card>

      {/* Form Modal */}
      <EnterpriseFormModal
        visible={formModalVisible}
        onCancel={handleFormCancel}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
      />
    </div>
  );
}
