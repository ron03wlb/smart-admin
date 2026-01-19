# 使用 Node.js 18 Alpine 作為基礎鏡像
FROM node:18-alpine

# 設置工作目錄
WORKDIR /app

# 複製 package.json 和 lock 文件
# 注意：這裡根據實際情況可能需要複製 package-lock.json 或 pnpm-lock.yaml
COPY package.json ./

# 安裝依賴
# 如果項目使用 npm
RUN npm install

# 複製所有源代碼
COPY . .

# 暴露端口 3000
EXPOSE 3000

# 設置環境變量
ENV NODE_ENV=development

# 啟動命令：使用 host 0.0.0.0 確保容器外部可訪問
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "3000"]
