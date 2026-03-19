/**
 * Goods Catalog - Product Category Management
 * 商品目錄 - 商品分類管理
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/catalog/goods-catalog.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { CategoryTreeTable } from '@/views/business/category';
import { CategoryTypeEnum } from '@/views/business/category/types';

/**
 * 商品分類管理頁面
 *
 * 這是一個包裝器組件，使用 CategoryTreeTable 並配置為商品分類類型
 */
export default function GoodsCatalog() {
  return <CategoryTreeTable categoryType={CategoryTypeEnum.GOODS} />;
}
