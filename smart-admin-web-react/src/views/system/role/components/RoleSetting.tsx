/**
 * Role Setting Container (Tab layout)
 *
 * Corresponds to Vue's role/components/role-setting/index.vue
 * Three tabs: Function Permissions (Tree), Data Scope, Employee List.
 */
import React from 'react';
import { Card, Tabs } from 'antd';
import RoleTree from './RoleTree';
import RoleDataScope from './RoleDataScope';
import RoleEmployeeList from './RoleEmployeeList';

const RoleSetting: React.FC = () => {
  return (
    <Card style={{ height: '100%' }}>
      <Tabs
        items={[
          { key: '1', label: '角色-功能权限', children: <RoleTree /> },
          { key: '2', label: '角色-数据范围', children: <RoleDataScope /> },
          { key: '3', label: '角色-员工列表', children: <RoleEmployeeList /> },
        ]}
      />
    </Card>
  );
};

export default RoleSetting;
