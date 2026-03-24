/**
 * Help Doc Catalog Tree
 * 幫助文檔目錄樹
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState, useRef, forwardRef, useImperativeHandle } from 'react';
import { Card, Input, Switch, Button, Tree, Popover, Modal, Row, message } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import { helpDocCatalogApi } from '@/api/support/helpDocCatalogApi';
import { usePrivilege } from '@/hooks/usePrivilege';
import { HELP_DOC_CATALOG_PERMISSION } from '@/constants/support/helpDocConst';
import type { HelpDocCatalogVO } from '../types';
import HelpDocCatalogFormModal, {
  type HelpDocCatalogFormModalRef,
} from './HelpDocCatalogFormModal';
import type { DataNode } from 'antd/es/tree';

const { confirm } = Modal;

const HELP_DOC_CATALOG_PARENT_ID = 0;

export interface HelpDocCatalogTreeProps {
  onCatalogSelect?: (catalogId: number | null) => void;
}

export interface HelpDocCatalogTreeRef {
  selectedKeys: React.Key[];
  refresh: () => Promise<void>;
}

const HelpDocCatalogTree = forwardRef<HelpDocCatalogTreeRef, HelpDocCatalogTreeProps>(
  ({ onCatalogSelect }, ref) => {
    const hasAddPrivilege = usePrivilege(HELP_DOC_CATALOG_PERMISSION.ADD);
    const hasEditPrivilege = usePrivilege(HELP_DOC_CATALOG_PERMISSION.EDIT);
    const hasDeletePrivilege = usePrivilege(HELP_DOC_CATALOG_PERMISSION.DELETE);

    const [keywords, setKeywords] = useState('');
    const [showSortFlag, setShowSortFlag] = useState(false);
    const [helpDocCatalogList, setHelpDocCatalogList] = useState<HelpDocCatalogVO[]>([]);
    const [helpDocCatalogTreeData, setHelpDocCatalogTreeData] = useState<HelpDocCatalogVO[]>([]);
    const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
    const [topHelpDocCatalogId, setTopHelpDocCatalogId] = useState<number>();
    const [idInfoMap, setIdInfoMap] = useState<Map<number, HelpDocCatalogVO>>(new Map());

    const catalogFormModalRef = useRef<HelpDocCatalogFormModalRef>(null);

    useEffect(() => {
      queryHelpDocCatalogTree();
    }, []);

    useEffect(() => {
      onSearch();
    }, [keywords]);

    const queryHelpDocCatalogTree = async () => {
      try {
        const res = await helpDocCatalogApi.getAll();
        const data = res.data;
        setHelpDocCatalogList(data);
        setHelpDocCatalogTreeData(buildHelpDocCatalogTree(data, HELP_DOC_CATALOG_PARENT_ID));

        const map = new Map<number, HelpDocCatalogVO>();
        data.forEach(e => {
          map.set(e.helpDocCatalogId, e);
        });
        setIdInfoMap(map);

        const treeData = buildHelpDocCatalogTree(data, HELP_DOC_CATALOG_PARENT_ID);
        if (treeData && treeData.length > 0) {
          setTopHelpDocCatalogId(treeData[0].helpDocCatalogId);
          selectTree(treeData[0].helpDocCatalogId);
        }
      } catch (error) {
        console.error('Failed to fetch catalog tree:', error);
      }
    };

    const buildHelpDocCatalogTree = (
      data: HelpDocCatalogVO[],
      parentId: number
    ): HelpDocCatalogVO[] => {
      let children = data.filter(e => e.parentId === parentId) || [];
      children = children.sort((a, b) => a.sort - b.sort);
      children.forEach(e => {
        e.children = buildHelpDocCatalogTree(data, e.helpDocCatalogId);
      });
      updateHelpDocCatalogPreIdAndNextId(children);
      return children;
    };

    const updateHelpDocCatalogPreIdAndNextId = (data: HelpDocCatalogVO[]) => {
      for (let index = 0; index < data.length; index++) {
        if (index === 0) {
          data[index].nextId = data.length > 1 ? data[1].helpDocCatalogId : undefined;
          continue;
        }

        if (index === data.length - 1) {
          data[index].preId = data[index - 1].helpDocCatalogId;
          data[index].nextId = undefined;
          continue;
        }

        data[index].preId = data[index - 1].helpDocCatalogId;
        data[index].nextId = data[index + 1].helpDocCatalogId;
      }
    };

    const selectTree = (id: number) => {
      setSelectedKeys([id]);
      if (onCatalogSelect) {
        onCatalogSelect(id);
      }
    };

    const handleTreeSelect = (selectedKeysValue: React.Key[]) => {
      setSelectedKeys(selectedKeysValue);
      if (selectedKeysValue.length > 0 && onCatalogSelect) {
        onCatalogSelect(selectedKeysValue[0] as number);
      } else if (onCatalogSelect) {
        onCatalogSelect(null);
      }
    };

    const onSearch = () => {
      if (!keywords) {
        setHelpDocCatalogTreeData(
          buildHelpDocCatalogTree(helpDocCatalogList, HELP_DOC_CATALOG_PARENT_ID)
        );
        return;
      }

      const originData = [...helpDocCatalogList];
      if (!originData) {
        return;
      }

      const filterCatalog = originData.filter(e => e.name.indexOf(keywords) > -1);
      const filterHelpDocCatalogList: HelpDocCatalogVO[] = [];

      filterCatalog.forEach(e => {
        recursionFilterHelpDocCatalog(filterHelpDocCatalogList, e.helpDocCatalogId, false);
      });

      setHelpDocCatalogTreeData(
        buildHelpDocCatalogTree(filterHelpDocCatalogList, HELP_DOC_CATALOG_PARENT_ID)
      );
    };

    const recursionFilterHelpDocCatalog = (
      resList: HelpDocCatalogVO[],
      id: number,
      unshift: boolean
    ) => {
      const info = idInfoMap.get(id);
      if (!info || resList.some(e => e.helpDocCatalogId === id)) {
        return;
      }
      if (unshift) {
        resList.unshift(info);
      } else {
        resList.push(info);
      }
      if (info.parentId && info.parentId !== 0) {
        recursionFilterHelpDocCatalog(resList, info.parentId, unshift);
      }
    };

    const addTop = () => {
      catalogFormModalRef.current?.showModal({
        helpDocCatalogId: 0,
        name: '',
        parentId: 0,
        sort: 0,
      });
    };

    const addHelpDocCatalog = (record: HelpDocCatalogVO) => {
      catalogFormModalRef.current?.showModal({
        helpDocCatalogId: 0,
        name: '',
        parentId: record.helpDocCatalogId,
        sort: 0,
      });
    };

    const updateHelpDocCatalog = (record: HelpDocCatalogVO) => {
      catalogFormModalRef.current?.showModal(record);
    };

    const deleteHelpDocCatalog = (id: number) => {
      confirm({
        title: '提醒',
        icon: <ExclamationCircleOutlined />,
        content: '確定要刪除該目錄嗎?',
        okText: '刪除',
        okType: 'danger',
        onOk: async () => {
          try {
            let selectedKey = null;
            if (selectedKeys.length > 0) {
              selectedKey = selectedKeys[0] as number;
              if (selectedKey === id) {
                const selectInfo = helpDocCatalogList.find(e => e.helpDocCatalogId === id);
                if (selectInfo && selectInfo.parentId) {
                  selectedKey = selectInfo.parentId;
                }
              }
            }

            await helpDocCatalogApi.delete(id);
            message.success('刪除成功');
            await queryHelpDocCatalogTree();

            if (selectedKey) {
              selectTree(selectedKey);
            }
          } catch (error) {
            console.error('Failed to delete catalog:', error);
          }
        },
        cancelText: '取消',
      });
    };

    const refresh = async () => {
      await queryHelpDocCatalogTree();
    };

    useImperativeHandle(ref, () => ({
      selectedKeys,
      refresh,
    }));

    const treeData = helpDocCatalogTreeData.map(item => convertToTreeNode(item));

    function convertToTreeNode(item: HelpDocCatalogVO): DataNode {
      const node: DataNode = {
        key: item.helpDocCatalogId,
        title: (
          <Popover
            placement="right"
            content={
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                {hasAddPrivilege && (
                  <Button type="text" onClick={() => addHelpDocCatalog(item)}>
                    添加下級
                  </Button>
                )}
                {hasEditPrivilege && (
                  <Button type="text" onClick={() => updateHelpDocCatalog(item)}>
                    修改
                  </Button>
                )}
                {hasDeletePrivilege && item.helpDocCatalogId !== topHelpDocCatalogId && (
                  <Button type="text" onClick={() => deleteHelpDocCatalog(item.helpDocCatalogId)}>
                    刪除
                  </Button>
                )}
              </div>
            }
          >
            {item.name}
            {showSortFlag && <span style={{ marginLeft: 5 }}>({item.sort})</span>}
          </Popover>
        ),
      };

      if (item.children && item.children.length > 0) {
        node.children = item.children.map(child => convertToTreeNode(child));
      }

      return node;
    }

    return (
      <>
        <Card size="small" style={{ height: '100%' }}>
          <Row>
            <Input
              value={keywords}
              onChange={e => setKeywords(e.target.value)}
              placeholder="請輸入目錄名稱"
            />
          </Row>
          <Row
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              marginTop: 10,
              marginBottom: 10,
            }}
          >
            <span>
              排序
              {showSortFlag && ' （越小越靠前） '}
              ：
              <Switch checked={showSortFlag} onChange={setShowSortFlag} />
            </span>
            {hasAddPrivilege && (
              <Button type="primary" size="small" onClick={addTop}>
                新建
              </Button>
            )}
          </Row>
          {helpDocCatalogTreeData.length > 0 ? (
            <Tree
              selectedKeys={selectedKeys}
              treeData={treeData}
              style={{ width: '100%', overflowX: 'auto' }}
              showLine
              defaultExpandAll
              onSelect={handleTreeSelect}
            />
          ) : (
            <div style={{ margin: 10 }}>暫無結果</div>
          )}
        </Card>

        <HelpDocCatalogFormModal ref={catalogFormModalRef} onRefresh={refresh} />
      </>
    );
  }
);

HelpDocCatalogTree.displayName = 'HelpDocCatalogTree';

export default HelpDocCatalogTree;
