<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Mousewheel, Virtual } from 'swiper/modules'
import type { Swiper as SwiperInstance } from 'swiper'
import 'swiper/css'
import VideoPlayer, { type PlayStatus } from './VideoPlayer.vue'
import { fetchVideos, fetchFavoriteIds, toggleFavorite, type Video } from '../api/videos'

const modules = [Mousewheel, Virtual]

const videos = ref<Video[]>([])
const currentIndex = ref(0)
const loading = ref(true)
const error = ref('')
const favoriteIds = ref<Set<number>>(new Set())

/**
 * Status logic:
 *   active  – playing
 *   preload – src loaded, silently buffering (next video)
 *   idle    – no src, no memory usage
 *
 * Only 3 DOM nodes exist at a time (Swiper Virtual).
 */
function getStatus(index: number): PlayStatus {
  if (index === currentIndex.value) return 'active'
  if (index === currentIndex.value + 1) return 'preload'
  return 'idle'
}

async function loadVideos() {
  try {
    loading.value = true
    error.value = ''
    const [vids, ids] = await Promise.all([fetchVideos(), fetchFavoriteIds()])
    videos.value = vids
    favoriteIds.value = new Set(ids)
  } catch {
    error.value = '加载失败，请检查后端服务是否运行'
  } finally {
    loading.value = false
  }
}

async function onToggleFavorite(videoId: number) {
  const nowFavorited = await toggleFavorite(videoId)
  if (nowFavorited) {
    favoriteIds.value.add(videoId)
  } else {
    favoriteIds.value.delete(videoId)
  }
  // Trigger reactivity for Set
  favoriteIds.value = new Set(favoriteIds.value)
}

function onSlideChange(swiper: SwiperInstance) {
  currentIndex.value = swiper.activeIndex
}

onMounted(loadVideos)
</script>

<template>
  <div class="w-full h-full bg-black">

    <!-- Loading -->
    <div v-if="loading" class="w-full h-full flex flex-col items-center justify-center gap-4">
      <div class="w-10 h-10 border-2 border-white/20 border-t-white rounded-full animate-spin"/>
      <p class="text-white/40 text-sm">加载视频库…</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="w-full h-full flex flex-col items-center justify-center gap-4 px-8">
      <svg class="w-14 h-14 text-white/20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M15 10l4.553-2.069A1 1 0 0121 8.845v6.31a1 1 0 01-1.447.894L15 14
             M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
      </svg>
      <p class="text-white/40 text-sm text-center">{{ error }}</p>
      <button
        class="px-5 py-2 bg-white/10 rounded-full text-sm text-white hover:bg-white/20 transition-colors"
        @click="loadVideos"
      >重试</button>
    </div>

    <!-- Empty -->
    <div v-else-if="videos.length === 0"
      class="w-full h-full flex flex-col items-center justify-center gap-3 px-8">
      <svg class="w-14 h-14 text-white/20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
      </svg>
      <p class="text-white/40 text-sm">视频库为空</p>
      <p class="text-white/25 text-xs text-center">在「设置」页配置视频目录后点「保存并扫描」</p>
    </div>

    <!-- Feed — Virtual mode: only 3 slides in DOM at once -->
    <Swiper
      v-else
      :modules="modules"
      direction="vertical"
      :slides-per-view="1"
      :space-between="0"
      :speed="280"
      :mousewheel="{ sensitivity: 1, thresholdDelta: 50 }"
      :resistance="true"
      :resistance-ratio="0.7"
      :virtual="{ enabled: true, addSlidesAfter: 1, addSlidesBefore: 1 }"
      class="w-full h-full"
      @slide-change="onSlideChange"
    >
      <SwiperSlide
        v-for="(video, index) in videos"
        :key="video.id"
        :virtualIndex="index"
      >
        <VideoPlayer
          :video="video"
          :status="getStatus(index)"
          :is-favorited="favoriteIds.has(video.id)"
          @toggle-favorite="onToggleFavorite"
        />
      </SwiperSlide>
    </Swiper>

    <!-- Counter -->
    <div v-if="videos.length > 0"
      class="absolute top-4 right-4 z-50 pointer-events-none">
      <span class="text-white/30 text-xs tabular-nums">
        {{ currentIndex + 1 }} / {{ videos.length }}
      </span>
    </div>
  </div>
</template>
