/**
 * Role Data Scope
 *
 * Corresponds to Vue's role/components/role-data-scope/index.vue
 * Displays data scope types with radio groups for view type selection.
 */
import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Button, Radio, Row, Col, message, Space } from 'antd';
import { roleApi } from '@/api/system/role-api';
import type { DataScopeDefinition, RoleDataScopeVO } from '@/types/role.types';
import { RoleContext } from '../index';

const RoleDataScope: React.FC = () => {
  const { selectedRoleId } = useContext(RoleContext);
  const [dataScopeList, setDataScopeList] = useState<DataScopeDefinition[]>([]);
  const [selectedScopes, setSelectedScopes] = useState<(RoleDataScopeVO & { viewType?: number })[]>([]);

  /** Fetch all available data scope definitions */
  const loadDataScopes = useCallback(async () => {
    const res = await roleApi.getDataScopeList();
    if (res.code === 1 && res.data) {
      setDataScopeList(res.data);
      // Initialize selections with empty viewType
      setSelectedScopes(
        res.data.map((item) => ({
          dataScopeType: item.dataScopeType,
          viewType: undefined as unknown as number,
        }))
      );
      // Then load role-specific selections
      if (selectedRoleId) {
        loadRoleDataScope(res.data);
      }
    }
  }, [selectedRoleId]); // eslint-disable-line react-hooks/exhaustive-deps

  /** Fetch role-specific data scope selections and merge */
  const loadRoleDataScope = useCallback(
    async (allScopes: DataScopeDefinition[]) => {
      if (!selectedRoleId) return;
      const res = await roleApi.getRoleDataScopeList(selectedRoleId);
      if (res.code === 1 && res.data) {
        setSelectedScopes(
          allScopes.map((item) => {
            const found = res.data.find((e) => e.dataScopeType === item.dataScopeType);
            return {
              dataScopeType: item.dataScopeType,
              viewType: found ? found.viewType : (undefined as unknown as number),
            };
          })
        );
      }
    },
    [selectedRoleId]
  );

  useEffect(() => {
    loadDataScopes();
  }, [loadDataScopes]);

  useEffect(() => {
    if (selectedRoleId && dataScopeList.length > 0) {
      loadRoleDataScope(dataScopeList);
    }
  }, [selectedRoleId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleViewTypeChange = (index: number, viewType: number) => {
    setSelectedScopes((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], viewType };
      return copy;
    });
  };

  const handleSave = async () => {
    if (!selectedRoleId) return;
    const filtered = selectedScopes.filter((e) => e.viewType !== undefined);
    await roleApi.updateRoleDataScope({
      roleId: selectedRoleId,
      dataScopeItemList: filtered,
    });
    message.success('保存成功');
    loadDataScopes();
  };

  return (
    <div>
      <div style={{ textAlign: 'right', marginBottom: 10 }}>
        <Space>
          <Button type="primary" onClick={handleSave}>保存</Button>
          <Button onClick={loadDataScopes}>刷新</Button>
        </Space>
      </div>

      <Row style={{ borderBottom: '1px solid #f2f2f2', fontWeight: 600, padding: '10px 0' }}>
        <Col span={4} style={{ textAlign: 'center' }}>业务单据</Col>
        <Col span={8}>查看数据范围</Col>
        <Col span={12} />
      </Row>

      <div style={{ maxHeight: 680, overflow: 'auto' }}>
        {dataScopeList.map((item, index) => (
          <Row
            key={item.dataScopeType}
            align="middle"
            style={{ borderBottom: '1px solid #f2f2f2', padding: '10px 0' }}
          >
            <Col span={4} style={{ textAlign: 'center' }}>
              {item.dataScopeTypeName}
            </Col>
            <Col span={8}>
              <Radio.Group
                value={selectedScopes[index]?.viewType}
                onChange={(e) => handleViewTypeChange(index, e.target.value)}
              >
                {item.viewTypeList?.map((scope) => (
                  <Radio
                    key={`${item.dataScopeType}-${scope.viewType}`}
                    value={scope.viewType}
                    style={{ display: 'block', height: 30, lineHeight: '30px' }}
                  >
                    {scope.viewTypeName}
                  </Radio>
                ))}
              </Radio.Group>
            </Col>
            <Col span={12} style={{ fontSize: 14, lineHeight: '30px' }}>
              {item.dataScopeTypeDesc}
            </Col>
          </Row>
        ))}
      </div>
    </div>
  );
};

export default RoleDataScope;
