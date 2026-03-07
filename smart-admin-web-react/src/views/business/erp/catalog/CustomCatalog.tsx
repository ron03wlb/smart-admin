/**
 * Custom Catalog
 *
 * Corresponds to Vue's business/erp/catalog/custom-catalog.vue (18L)
 */
import React from 'react';
import CategoryTreeTable from './CategoryTreeTable';

const CUSTOM_CATEGORY_TYPE = 2;

const CustomCatalog: React.FC = () => {
  return <CategoryTreeTable categoryType={CUSTOM_CATEGORY_TYPE} />;
};

export default CustomCatalog;
