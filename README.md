# TikTok for NAS

基于 NAS 私人媒体库的抖音式刷视频系统。

## 快速启动

### 开发模式

**后端**

```bash
cd backend
./mvnw spring-boot:run
# 访问 http://localhost:8080
```

**前端**

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### Docker 部署

```bash
# 修改 docker-compose.yml 中的 NAS 挂载路径
# volumes: /mnt/nas/videos:/videos:ro

docker compose up -d
# 访问 http://你的NAS_IP
```

## 目录结构

```
TikTokforNAS/
├── backend/          # Spring Boot 3 后端
│   ├── src/main/java/com/tiktoknas/
│   │   ├── controller/   # REST API
│   │   ├── service/      # 业务逻辑 (扫描/流媒体/HLS)
│   │   ├── model/        # JPA 实体
│   │   └── repository/   # 数据库访问
│   └── Dockerfile
├── frontend/         # Vue3 前端
│   ├── src/
│   │   ├── components/
│   │   │   ├── VideoFeed.vue    # Swiper 抖音流
│   │   │   ├── VideoPlayer.vue  # HLS/直播播放器
│   │   │   └── HistoryView.vue  # 最近观看
│   │   └── api/videos.ts        # API 封装
│   └── Dockerfile
└── docker-compose.yml
```

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/videos | 获取视频列表 |
| GET | /api/video/{id} | 视频流（支持 Range） |
| GET | /api/video/{id}/hls | 获取 HLS 地址（异步生成） |
| GET | /hls/{id}/{file} | HLS 分片文件 |
| GET | /api/covers/{file} | 封面图 |
| POST | /api/history/{id} | 更新播放进度 |
| GET | /api/history | 获取播放历史 |
| POST | /api/scan | 触发视频重新扫描 |

## 技术栈

- **后端**: Java 21, Spring Boot 3, SQLite, ffmpeg
- **前端**: Vue 3, Vite, TypeScript, TailwindCSS, Swiper.js, hls.js
- **部署**: Docker Compose

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| APP_VIDEO_DIR | /videos | 视频目录（NAS 挂载点） |
| APP_COVER_DIR | /data/covers | 封面缓存目录 |
| APP_HLS_DIR | /data/hls | HLS 分片缓存目录 |
| SPRING_DATASOURCE_URL | jdbc:sqlite:/data/tiktoknas.db | 数据库路径 |
