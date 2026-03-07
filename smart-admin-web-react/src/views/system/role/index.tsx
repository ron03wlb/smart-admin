/**
 * Role Management Index
 *
 * Corresponds to Vue's system/role/index.vue
 * Two-panel layout: left = role list (200px), right = role settings (tabs)
 */
import React, { createContext, useCallback, useState } from 'react';
import { Row, Col } from 'antd';
import RoleList from './components/RoleList';
import RoleSetting from './components/RoleSetting';

/** Context for sharing selected roleId across child components */
export const RoleContext = createContext<{
  selectedRoleId: number | null;
  setSelectedRoleId: (id: number | null) => void;
}>({ selectedRoleId: null, setSelectedRoleId: () => {} });

const RoleIndex: React.FC = () => {
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);

  const handleSetSelectedRoleId = useCallback((id: number | null) => {
    setSelectedRoleId(id);
  }, []);

  return (
    <RoleContext.Provider value={{ selectedRoleId, setSelectedRoleId: handleSetSelectedRoleId }}>
      <Row gutter={10} style={{ height: '100%', flexWrap: 'nowrap' }}>
        <Col flex="200px">
          <RoleList />
        </Col>
        <Col flex="1" style={{ width: 'calc(100% - 250px)' }}>
          <RoleSetting />
        </Col>
      </Row>
    </RoleContext.Provider>
  );
};

export default RoleIndex;
