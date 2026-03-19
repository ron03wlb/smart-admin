/**
 * Custom Catalog - Demo Category Management
 * 自定義目錄 - 演示分類管理
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/catalog/custom-catalog.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { CategoryTreeTable } from '@/views/business/category';
import { CategoryTypeEnum } from '@/views/business/category/types';

/**
 * 自定義分類管理頁面（演示用途）
 *
 * 這是一個包裝器組件，使用 CategoryTreeTable 並配置為演示分類類型
 * 並使用自定義權限前綴 'custom:'
 */
export default function CustomCatalog() {
  return <CategoryTreeTable categoryType={CategoryTypeEnum.DEMO} privilegePrefix="custom:" />;
}
