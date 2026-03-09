/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  // 更多環境變量...
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
