/**
 * Help Doc User View
 *
 * Corresponds to Vue's support/help-doc/user-view/help-doc-user-view.vue (328L)
 * User-facing page to read help documents.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Row, Col, Tree, Input, Typography, Divider, Empty, Table, Space } from 'antd';
import { helpDocApi, helpDocCatalogApi } from '@/api/support/help-doc-api';
import type { HelpDocVO, HelpDocCatalogVO } from '@/api/support/help-doc-api';
import type { DataNode } from 'antd/es/tree';
import type { ColumnsType } from 'antd/es/table';

const { Title, Text } = Typography;
const PAGE_SIZE = 10;

const buildTreeData = (list: HelpDocCatalogVO[]): DataNode[] =>
  list.map((item) => ({
    key: item.helpDocCatalogId,
    title: item.name,
    children: item.children ? buildTreeData(item.children) : [],
  }));

interface ViewRecordVO {
  userName: string;
  pageViewCount: number;
  firstIp: string;
  firstTime: string;
  lastIp: string;
  lastTime: string;
}

const HelpDocUserView: React.FC = () => {
  const [catalogTree, setCatalogTree] = useState<DataNode[]>([]);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [docList, setDocList] = useState<HelpDocVO[]>([]);
  const [currentDoc, setCurrentDoc] = useState<HelpDocVO | undefined>();
  const [viewRecords, setViewRecords] = useState<ViewRecordVO[]>([]);
  const [recordTotal, setRecordTotal] = useState(0);
  const [recordPage, setRecordPage] = useState(1);

  const loadCatalogs = useCallback(async () => {
    const res = await helpDocCatalogApi.getAll();
    if (res.code === 1 && res.data) {
      const tree = buildTreeData(res.data);
      setCatalogTree(tree);
      setExpandedKeys(tree.map((n) => n.key));
    }
  }, []);

  const loadDocList = useCallback(async () => {
    const res = await helpDocApi.queryAllForUser();
    if (res.code === 1 && res.data) {
      setDocList(res.data);
    }
  }, []);

  useEffect(() => { loadCatalogs(); loadDocList(); }, [loadCatalogs, loadDocList]);

  const handleSelectDoc = async (doc: HelpDocVO) => {
    const res = await helpDocApi.userView(doc.helpDocId);
    if (res.code === 1 && res.data) {
      setCurrentDoc(res.data);
      setRecordPage(1);
      loadViewRecords(doc.helpDocId, 1);
    }
  };

  const loadViewRecords = async (helpDocId: number, page: number) => {
    const res = await helpDocApi.queryViewRecord({ helpDocId, pageNum: page, pageSize: PAGE_SIZE });
    if (res.code === 1 && res.data) {
      setViewRecords(res.data.list || []);
      setRecordTotal(res.data.total || 0);
    }
  };

  const recordColumns: ColumnsType<ViewRecordVO> = [
    { title: '用户', dataIndex: 'userName', width: 120 },
    { title: '浏览次数', dataIndex: 'pageViewCount', width: 80, align: 'center' },
    { title: '首次IP', dataIndex: 'firstIp', width: 130 },
    { title: '首次时间', dataIndex: 'firstTime', width: 170 },
    { title: '最近IP', dataIndex: 'lastIp', width: 130 },
    { title: '最近时间', dataIndex: 'lastTime', width: 170 },
  ];

  return (
    <Card>
      <Row gutter={16}>
        <Col span={5}>
          <Input.Search style={{ marginBottom: 8 }} placeholder="搜索目录" allowClear />
          <Tree treeData={catalogTree} expandedKeys={expandedKeys} onExpand={setExpandedKeys} blockNode />
          <Divider />
          <div style={{ maxHeight: 400, overflow: 'auto' }}>
            {docList.map((doc) => (
              <div
                key={doc.helpDocId}
                style={{ padding: '6px 8px', cursor: 'pointer', borderRadius: 4, background: currentDoc?.helpDocId === doc.helpDocId ? '#e6f4ff' : undefined }}
                onClick={() => handleSelectDoc(doc)}
              >
                <Text ellipsis style={{ display: 'block' }}>{doc.title}</Text>
              </div>
            ))}
          </div>
        </Col>
        <Col span={19}>
          {currentDoc ? (
            <>
              <Title level={4}>{currentDoc.title}</Title>
              <Space split={<Divider type="vertical" />}>
                <Text type="secondary">作者：{currentDoc.author}</Text>
                <Text type="secondary">浏览：{currentDoc.pageViewCount}</Text>
                <Text type="secondary">创建：{currentDoc.createTime}</Text>
              </Space>
              <Divider />
              <div dangerouslySetInnerHTML={{ __html: currentDoc.contentHtml || '' }} style={{ minHeight: 200, lineHeight: 1.8 }} />
              <Divider />
              <Title level={5}>浏览记录</Title>
              <Table
                rowKey="userName" columns={recordColumns} dataSource={viewRecords} size="small"
                pagination={{ current: recordPage, pageSize: PAGE_SIZE, total: recordTotal, showTotal: (t) => `共${t}条`, onChange: (page) => { setRecordPage(page); loadViewRecords(currentDoc.helpDocId, page); } }}
              />
            </>
          ) : (
            <Empty description="请选择文档" style={{ marginTop: 100 }} />
          )}
        </Col>
      </Row>
    </Card>
  );
};

export default HelpDocUserView;
