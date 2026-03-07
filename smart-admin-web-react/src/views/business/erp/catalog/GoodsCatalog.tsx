/**
 * Goods Catalog
 *
 * Corresponds to Vue's business/erp/catalog/goods-catalog.vue (18L)
 */
import React from 'react';
import CategoryTreeTable from './CategoryTreeTable';

const GOODS_CATEGORY_TYPE = 1;

const GoodsCatalog: React.FC = () => {
  return <CategoryTreeTable categoryType={GOODS_CATEGORY_TYPE} />;
};

export default GoodsCatalog;
