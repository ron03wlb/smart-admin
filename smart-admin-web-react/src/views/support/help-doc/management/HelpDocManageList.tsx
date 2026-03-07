/**
 * Help Doc Management List (Container)
 *
 * Corresponds to Vue's support/help-doc/management/help-doc-manage-list.vue
 * Two-column layout: Catalog Tree (left) + Doc List (right)
 */
import React, { useState } from 'react';
import { Card, Row, Col } from 'antd';
import HelpDocCatalogTree from './HelpDocCatalogTree';
import HelpDocList from './HelpDocList';

const HelpDocManageList: React.FC = () => {
  const [selectedCatalogId, setSelectedCatalogId] = useState<number | undefined>();

  return (
    <Card>
      <Row gutter={16}>
        <Col span={6}>
          <HelpDocCatalogTree onSelect={setSelectedCatalogId} />
        </Col>
        <Col span={18}>
          <HelpDocList helpDocCatalogId={selectedCatalogId} />
        </Col>
      </Row>
    </Card>
  );
};

export default HelpDocManageList;
