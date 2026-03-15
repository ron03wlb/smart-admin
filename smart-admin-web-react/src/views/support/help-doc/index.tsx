/**
 * Help Doc Management Page
 * 幫助文檔管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useState } from 'react';
import { Row, Col } from 'antd';
import HelpDocCatalogTree from './components/HelpDocCatalogTree';
import HelpDocList from './components/HelpDocList';

const HelpDocManagePage: React.FC = () => {
  const [selectedHelpDocCatalogId, setSelectedHelpDocCatalogId] = useState<number | null>(null);

  const handleCatalogSelect = (catalogId: number | null) => {
    setSelectedHelpDocCatalogId(catalogId);
  };

  return (
    <div style={{ height: '100%' }}>
      <Row gutter={16} style={{ height: '100%' }}>
        <Col span={6} style={{ height: '100%' }}>
          <HelpDocCatalogTree onCatalogSelect={handleCatalogSelect} />
        </Col>

        <Col span={18} style={{ height: '100%' }}>
          <HelpDocList helpDocCatalogId={selectedHelpDocCatalogId} />
        </Col>
      </Row>
    </div>
  );
};

export default HelpDocManagePage;
