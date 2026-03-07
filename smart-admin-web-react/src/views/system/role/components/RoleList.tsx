/**
 * Role List Component (Left Panel)
 *
 * Corresponds to Vue's role/components/role-list/index.vue
 * Displays all roles in a vertical menu with popover actions (edit/delete).
 */
import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Card, Menu, Popover, Button, Modal, message } from 'antd';
import { roleApi } from '@/api/system/role-api';
import type { RoleVO } from '@/types/role.types';
import { RoleContext } from '../index';
import RoleFormModal from './RoleFormModal';

const RoleList: React.FC = () => {
  const { selectedRoleId, setSelectedRoleId } = useContext(RoleContext);
  const [roleList, setRoleList] = useState<RoleVO[]>([]);
  const [formVisible, setFormVisible] = useState(false);
  const [currentRole, setCurrentRole] = useState<RoleVO | undefined>();

  const queryAllRole = useCallback(async () => {
    const res = await roleApi.getAll();
    if (res.code === 1 && res.data) {
      setRoleList(res.data);
      if (res.data.length > 0 && !selectedRoleId) {
        setSelectedRoleId(res.data[0].roleId);
      }
    }
  }, [selectedRoleId, setSelectedRoleId]);

  useEffect(() => {
    queryAllRole();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAdd = () => {
    setCurrentRole(undefined);
    setFormVisible(true);
  };

  const handleEdit = (role: RoleVO) => {
    setCurrentRole(role);
    setFormVisible(true);
  };

  const handleDelete = (roleId: number) => {
    Modal.confirm({
      title: '提示',
      content: '确定要删除该角色么？',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      async onOk() {
        await roleApi.delete(roleId);
        message.success('删除成功');
        queryAllRole();
      },
    });
  };

  const handleFormSuccess = () => {
    setFormVisible(false);
    queryAllRole();
  };

  return (
    <>
      <Card
        title="角色列表"
        style={{ height: '100%', overflow: 'auto' }}
        styles={{ body: { padding: 5 } }}
        extra={
          <Button type="primary" size="small" onClick={handleAdd}>
            添加
          </Button>
        }
      >
        <Menu
          mode="vertical"
          selectedKeys={selectedRoleId ? [String(selectedRoleId)] : []}
          onSelect={({ key }) => setSelectedRoleId(Number(key))}
          style={{ borderRight: 'none' }}
          items={roleList.map((item) => ({
            key: String(item.roleId),
            label: (
              <Popover
                placement="right"
                content={
                  <div style={{ display: 'flex', flexDirection: 'column' }}>
                    <Button type="text" onClick={() => handleDelete(item.roleId)}>删除</Button>
                    <Button type="text" onClick={() => handleEdit(item)}>编辑</Button>
                  </div>
                }
              >
                {item.roleName}
              </Popover>
            ),
          }))}
        />
      </Card>
      <RoleFormModal
        open={formVisible}
        role={currentRole}
        onCancel={() => setFormVisible(false)}
        onSuccess={handleFormSuccess}
      />
    </>
  );
};

export default RoleList;
