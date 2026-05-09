# NAS 短视频流媒体系统（TikTok for NAS）PRD

## 项目简介

打造一个基于 NAS 私人媒体库的「抖音式刷视频系统」。

系统通过读取 NAS 中的视频文件（SMB 共享目录），将其转换为适合 Web 播放的视频流，并在前端实现类似抖音的全屏滑动、自动播放、无限刷视频体验。

项目定位：

- 内网私人视频浏览系统
- 私人版 TikTok / Plex
- 面向 NAS 用户的视频消费体验升级
- 支持未来 AI 视频理解与推荐扩展

---

# 一、核心目标

实现：

- NAS 视频读取
- Web 视频播放
- 抖音式上下滑动
- 自动播放
- 视频预加载
- 播放历史记录
- HLS 流媒体支持
- 移动端适配

---

# 二、系统架构

```text
[NAS]
   └── SMB共享目录

        ↓

[Spring Boot 后端]
   ├── SMB读取
   ├── 视频扫描
   ├── ffmpeg转码
   ├── HLS切片
   ├── 缩略图生成
   ├── REST API
   └── 播放历史记录

        ↓

[Vue3 Web前端]
   ├── 抖音式滑动
   ├── 自动播放
   ├── 视频预加载
   ├── HLS播放
   └── 移动端适配
```

---

# 三、技术栈

## 后端

- Java 21
- Spring Boot 3
- Spring WebMVC
- SQLite / PostgreSQL
- ffmpeg
- Lombok

## 前端

- Vue3
- Vite
- TypeScript
- TailwindCSS
- swiper.js
- hls.js

## 部署

- Docker
- Docker Compose

---

# 四、核心功能设计

## 1. 视频扫描

系统启动后自动扫描 NAS 挂载目录：

```bash
/mnt/nas/videos
```

提取：

- 文件名
- 视频时长
- 分辨率
- 文件大小
- 视频编码
- 创建时间

写入数据库。

---

## 2. 视频播放

支持：

- MP4
- MKV
- MOV
- AVI

后端支持：

- HTTP Range 请求
- 视频拖动
- 分段读取

---

## 3. HLS 流媒体

系统自动使用 ffmpeg 生成：

```text
video.m3u8
0001.ts
0002.ts
```

提升：

- 首帧速度
- 移动端体验
- 拖动体验
- 网络稳定性

---

## 4. 抖音式滑动

支持：

- 全屏播放
- 上下滑动切换
- 自动暂停上一视频
- 自动播放当前视频
- 无限刷视频

---

## 5. 视频预加载

自动预加载：

- 当前视频
- 下一条视频

减少卡顿。

---

## 6. 播放历史

记录：

- 播放时间
- 播放进度
- 最后观看时间

支持：

- 继续播放
- 最近观看

---

## 7. 视频封面

自动生成缩略图：

```bash
ffmpeg -ss 00:00:03 -i input.mp4 -vframes 1 cover.jpg
```

---

# 五、数据库设计

## video

| 字段 | 类型 | 描述 |
|---|---|---|
| id | bigint | 主键 |
| title | varchar | 视频标题 |
| path | varchar | 文件路径 |
| duration | bigint | 时长 |
| size | bigint | 文件大小 |
| codec | varchar | 编码 |
| cover | varchar | 封面路径 |
| created_at | datetime | 创建时间 |

---

## play_history

| 字段 | 类型 | 描述 |
|---|---|---|
| id | bigint | 主键 |
| video_id | bigint | 视频ID |
| progress | bigint | 播放进度 |
| updated_at | datetime | 更新时间 |

---

# 六、API 设计

## 获取视频列表

```http
GET /api/videos
```

返回：

```json
[
  {
    "id": 1,
    "title": "movie.mp4",
    "cover": "/covers/1.jpg",
    "duration": 1200
  }
]
```

---

## 获取视频流

```http
GET /api/video/{id}
```

支持：

```http
Range: bytes=0-100000
```

返回：

```http
206 Partial Content
```

---

## 获取 HLS 地址

```http
GET /api/video/{id}/hls
```

返回：

```json
{
  "url": "/hls/1/index.m3u8"
}
```

---

# 七、前端设计

## 页面结构

```text
首页
 ├── 视频流
 ├── 播放页面
 ├── 最近观看
 └── 收藏
```

---

## 播放页面

类似抖音：

- 全屏视频
- 左右操作栏
- 底部进度条
- 上下滑动切换

---

# 八、Docker 部署

## docker-compose

```yaml
version: '3'

services:
  app:
    image: openjdk:21
    ports:
      - "8080:8080"
    volumes:
      - /mnt/nas/videos:/videos
```

---

# 九、未来扩展方向

## AI 自动标签

支持：

- 风景
- 动漫
- 赛车
- 游戏
- 猫咪

---

## Whisper 字幕识别

自动：

- 提取字幕
- 搜索台词
- 视频摘要

---

## CLIP 视频理解

自动识别：

- 人物
- 场景
- 动作

---

## 推荐系统

根据：

- 播放历史
- 标签
- 时长偏好

实现推荐。

---

# 十、MVP 开发阶段

## 第一阶段

实现：

- NAS挂载
- 视频扫描
- 视频播放
- 抖音滑动

---

## 第二阶段

实现：

- HLS
- 缩略图
- 历史记录

---

## 第三阶段

实现：

- AI标签
- 搜索
- 推荐

---

# 十一、项目目标

打造：

> 一个属于自己的私人短视频世界。

不是算法平台的信息洪流。

而是：

那些年下载、收藏、保存下来的视频，
在夜晚重新流动起来。
