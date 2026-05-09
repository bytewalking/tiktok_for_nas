# TikTok for NAS

基于 NAS 私人媒体库的抖音式刷视频系统，支持竖屏滑动浏览、HLS 流媒体、收藏、观看统计等功能。

## 功能

- **抖音式浏览**：竖向滑动切换视频，随机播放，自动续播
- **手势操作**：左划后退 10s，右划前进 10s；点击暂停/播放
- **HLS 流媒体**：自动转码，支持进度跳转与断点续传
- **收藏**：右侧操作栏一键收藏，收藏页统一管理
- **个人中心**：今日/本周/全年观看时长统计，柱状图报表
- **全屏模式**：隐藏底部导航，沉浸式观看
- **进度条**：大号触摸友好进度条，支持拖拽定位
- **跨设备**：响应式布局，手机/平板/桌面均可使用

## 快速部署（NAS）

### 1. 修改视频目录

编辑 `docker-compose.yml`，将视频挂载路径改为你的实际目录：

```yaml
volumes:
  - /volume4/Media/Private/Movie:/videos:ro  # 绿联云示例
  # - /volume1/video:/videos:ro              # Synology 示例
  # - /share/CACHEDEV1_DATA/Video:/videos:ro # QNAP 示例
```

### 2. 加载镜像

```bash
docker load -i tiktoknas-backend.tar
docker load -i tiktoknas-frontend.tar
```

### 3. 启动

```bash
docker compose up -d
```

### 4. 访问

```
http://<NAS_IP>:13000
```

> 后端 API 端口 `18080`，前端端口 `13000`

---

## 开发模式

**后端**

```bash
cd backend
./mvnw spring-boot:run
# API 运行在 http://localhost:8080
```

**前端**

```bash
cd frontend
npm install
npm run dev
# 页面运行在 http://localhost:5173
```

## 目录结构

```
TikTokforNAS/
├── backend/
│   ├── src/main/java/com/tiktoknas/
│   │   ├── controller/
│   │   │   ├── VideoController.java    # 视频流、历史记录
│   │   │   ├── FavoriteController.java # 收藏管理
│   │   │   ├── StatsController.java    # 观看统计
│   │   │   ├── HlsController.java      # HLS 分片服务
│   │   │   └── SettingsController.java # 扫描设置
│   │   ├── service/                    # 扫描/流媒体/HLS 转码
│   │   ├── model/                      # JPA 实体
│   │   └── repository/                 # 数据库访问
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── VideoFeed.vue      # 抖音滑动流（随机播放）
│   │   │   ├── VideoPlayer.vue    # 播放器（HLS/手势/全屏）
│   │   │   ├── FavoritesView.vue  # 收藏列表
│   │   │   ├── ProfileView.vue    # 个人中心/观看统计
│   │   │   ├── HistoryView.vue    # 最近观看
│   │   │   └── ScanSettings.vue   # 扫描设置
│   │   └── api/videos.ts          # API 封装
│   └── Dockerfile
├── docker-compose.yml
└── nginx.conf
```

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/videos | 获取视频列表 |
| GET | /api/video/{id} | 视频流（支持 Range） |
| GET | /api/video/{id}/hls | 获取 HLS 地址 |
| GET | /hls/{id}/{file} | HLS 分片文件 |
| GET | /api/covers/{file} | 封面图 |
| POST | /api/history/{id} | 更新播放进度 |
| GET | /api/history | 获取播放历史 |
| POST | /api/favorites/{id} | 切换收藏状态 |
| GET | /api/favorites | 获取收藏视频列表 |
| GET | /api/favorites/ids | 获取收藏 ID 列表 |
| GET | /api/stats/watch-summary | 观看时长统计（按天聚合） |
| POST | /api/scan | 触发视频重新扫描 |

## 技术栈

- **后端**：Java 17, Spring Boot 3, SQLite, ffmpeg
- **前端**：Vue 3, Vite, TypeScript, TailwindCSS, Swiper.js, hls.js
- **部署**：Docker Compose, Nginx

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| APP_VIDEO_DIR | /videos | 视频目录（NAS 挂载点） |
| APP_COVER_DIR | /data/covers | 封面缓存目录 |
| APP_HLS_DIR | /data/hls | HLS 分片缓存目录 |
| SPRING_DATASOURCE_URL | jdbc:sqlite:/data/tiktoknas.db | 数据库路径 |
