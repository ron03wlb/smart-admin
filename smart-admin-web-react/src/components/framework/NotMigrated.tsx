/**
 * 未遷移頁面佔位組件
 *
 * 當後端菜單中的組件路徑在 React 專案中找不到對應 .tsx 文件時，
 * 顯示此佔位頁面。
 */
import { Result } from 'antd';
import { useLocation } from 'react-router-dom';

export default function NotMigrated() {
  const location = useLocation();

  return (
    <Result
      status="info"
      title="Page Not Migrated Yet"
      subTitle={`The page at "${location.pathname}" has not been migrated to React yet.`}
    />
  );
}
