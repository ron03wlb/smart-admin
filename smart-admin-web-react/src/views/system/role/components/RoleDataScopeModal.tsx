/**
 * Role Data Scope Configuration Modal Component
 * 角色數據範圍配置 Modal 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-23
 */

import { useEffect, useState } from 'react';
import { Modal, Radio, Row, Col, message, Alert, Spin } from 'antd';
import type { RadioChangeEvent } from 'antd';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO, DataScopeVO, DataScopeItem, DataScopeUpdateForm } from '../types';

interface RoleDataScopeModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  role?: RoleVO;
}

export default function RoleDataScopeModal({ visible, onCancel, onSuccess, role }: RoleDataScopeModalProps) {
  const [loading, setLoading] = useState(false);
  const [dataLoading, setDataLoading] = useState(false);
  const [dataScopeList, setDataScopeList] = useState<DataScopeVO[]>([]);
  const [selectedDataScope, setSelectedDataScope] = useState<Map<number, number>>(new Map());

  /**
   * 加載數據範圍列表
   */
  const loadDataScopeList = async () => {
    try {
      setDataLoading(true);
      const res = await roleApi.getDataScopeList();
      if (res.ok && res.data) {
        setDataScopeList(res.data);
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '加載數據範圍失敗');
      }
    } finally {
      setDataLoading(false);
    }
  };

  /**
   * 加載角色數據範圍配置
   */
  const loadRoleDataScope = async () => {
    if (!role) return;

    try {
      setDataLoading(true);
      const res = await roleApi.getDataScopeByRoleId(role.roleId);
      if (res.ok && res.data) {
        const newMap = new Map<number, number>();
        res.data.forEach((item) => {
          if (item.viewType !== undefined) {
            newMap.set(item.dataScopeType, item.viewType);
          }
        });
        setSelectedDataScope(newMap);
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '加載數據範圍配置失敗');
      }
    } finally {
      setDataLoading(false);
    }
  };

  /**
   * 數據範圍選擇變化
   */
  const handleDataScopeChange = (dataScopeType: number, e: RadioChangeEvent) => {
    const newMap = new Map(selectedDataScope);
    newMap.set(dataScopeType, e.target.value);
    setSelectedDataScope(newMap);
  };

  /**
   * 保存數據範圍配置
   */
  const handleSubmit = async () => {
    if (!role) return;

    try {
      setLoading(true);

      // 構建數據範圍項列表
      const dataScopeItemList: DataScopeItem[] = [];
      selectedDataScope.forEach((viewType, dataScopeType) => {
        if (viewType !== undefined) {
          dataScopeItemList.push({ dataScopeType, viewType });
        }
      });

      const params: DataScopeUpdateForm = {
        roleId: role.roleId,
        dataScopeItemList,
      };

      const res = await roleApi.updateDataScope(params);
      if (res.ok) {
        message.success('保存成功');
        onSuccess();
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '保存失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * Modal 關閉處理
   */
  const handleCancel = () => {
    setSelectedDataScope(new Map());
    onCancel();
  };

  /**
   * Modal 顯示時加載數據
   */
  useEffect(() => {
    if (visible && role) {
      loadDataScopeList();
      loadRoleDataScope();
    }
  }, [visible, role]);

  return (
    <Modal
      title={`設置數據範圍 - ${role?.roleName || ''}`}
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={800}
      destroyOnClose
      bodyStyle={{ maxHeight: '60vh', overflowY: 'auto' }}
    >
      <Alert
        message="提示"
        description="數據範圍決定該角色在查看業務數據時的可見範圍"
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Spin spinning={dataLoading}>
        {dataScopeList.length > 0 ? (
          <>
            {/* 表頭 */}
            <Row style={{ fontWeight: 600, borderBottom: '1px solid #f0f0f0', paddingBottom: 12, marginBottom: 16 }}>
              <Col span={4} style={{ textAlign: 'center' }}>
                業務單據
              </Col>
              <Col span={12}>查看數據範圍</Col>
              <Col span={8}>說明</Col>
            </Row>

            {/* 數據範圍配置項 */}
            {dataScopeList.map((dataScope) => (
              <Row
                key={dataScope.dataScopeType}
                align="middle"
                style={{ borderBottom: '1px solid #f0f0f0', paddingTop: 16, paddingBottom: 16 }}
              >
                <Col span={4} style={{ textAlign: 'center' }}>
                  {dataScope.dataScopeTypeName}
                </Col>
                <Col span={12}>
                  <Radio.Group
                    value={selectedDataScope.get(dataScope.dataScopeType)}
                    onChange={(e) => handleDataScopeChange(dataScope.dataScopeType, e)}
                  >
                    {dataScope.viewTypeList.map((viewType) => (
                      <Radio key={viewType.viewType} value={viewType.viewType} style={{ display: 'block', height: 32 }}>
                        {viewType.viewTypeName}
                      </Radio>
                    ))}
                  </Radio.Group>
                </Col>
                <Col span={8} style={{ fontSize: 14, color: '#666', lineHeight: 1.6 }}>
                  {dataScope.dataScopeTypeDesc}
                </Col>
              </Row>
            ))}
          </>
        ) : (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>暫無數據範圍配置</div>
        )}
      </Spin>
    </Modal>
  );
}
