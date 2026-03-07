/**
 * IFrame 外鏈頁面組件
 *
 * 對應 Vue 的 iframe-index.vue
 * 用於在 MainLayout 中嵌入外部頁面
 */

interface IFramePageProps {
  url?: string;
}

export default function IFramePage({ url }: IFramePageProps) {
  if (!url) {
    return <div style={{ padding: 24 }}>No URL provided for iframe.</div>;
  }

  return (
    <iframe
      src={url}
      style={{
        width: '100%',
        height: 'calc(100vh - 140px)',
        border: 'none',
      }}
      title="External Page"
    />
  );
}
