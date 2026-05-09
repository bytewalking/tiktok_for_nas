<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchFavoriteVideos, toggleFavorite, type Video, formatDuration } from '../api/videos'

const videos = ref<Video[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    videos.value = await fetchFavoriteVideos()
  } finally {
    loading.value = false
  }
}

async function removeFavorite(video: Video) {
  await toggleFavorite(video.id)
  videos.value = videos.value.filter(v => v.id !== video.id)
}

onMounted(load)
</script>

<template>
  <div class="h-full flex flex-col bg-black text-white overflow-hidden">

    <!-- Header -->
    <div class="flex-shrink-0 px-4 pt-6 pb-3">
      <div class="flex items-center justify-between">
        <h1 class="text-xl font-bold">收藏</h1>
        <span class="text-white/30 text-sm">{{ videos.length }} 个视频</span>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex-1 flex justify-center items-center">
      <div class="w-8 h-8 border-2 border-white/20 border-t-white rounded-full animate-spin"/>
    </div>

    <!-- Empty -->
    <div v-else-if="videos.length === 0"
      class="flex-1 flex flex-col items-center justify-center gap-3 px-8 text-center">
      <svg class="w-16 h-16 text-white/15" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0
             00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
      </svg>
      <p class="text-white/40 text-sm">还没有收藏</p>
      <p class="text-white/25 text-xs">在「发现」页点击视频右侧的心形按钮收藏</p>
    </div>

    <!-- List -->
    <div v-else class="flex-1 overflow-y-auto px-4 pb-4 space-y-2">
      <TransitionGroup name="list">
        <div
          v-for="video in videos"
          :key="video.id"
          class="flex items-center gap-3 bg-white/5 rounded-xl p-2.5 group"
        >
          <!-- Thumbnail -->
          <div class="w-24 h-14 rounded-lg overflow-hidden bg-white/10 flex-shrink-0 relative">
            <img
              v-if="video.cover"
              :src="video.cover"
              class="w-full h-full object-cover"
              loading="lazy"
            />
            <div v-else class="w-full h-full flex items-center justify-center">
              <svg class="w-6 h-6 text-white/20" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z"/>
              </svg>
            </div>
            <!-- Duration badge -->
            <div v-if="video.duration"
              class="absolute bottom-1 right-1 bg-black/70 rounded px-1 text-white text-xs">
              {{ formatDuration(video.duration) }}
            </div>
          </div>

          <!-- Info -->
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium leading-snug line-clamp-2">{{ video.title }}</p>
            <p v-if="video.resolution" class="text-xs text-white/30 mt-0.5">
              {{ video.resolution }}
            </p>
          </div>

          <!-- Remove button -->
          <button
            class="flex-shrink-0 p-2 text-red-400/60 hover:text-red-400
                   opacity-0 group-hover:opacity-100 transition-all"
            @click="removeFavorite(video)"
          >
            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0
                00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>

  </div>
</template>

<style scoped>
.list-enter-active, .list-leave-active { transition: all 0.25s ease; }
.list-enter-from { opacity: 0; transform: translateX(-12px); }
.list-leave-to { opacity: 0; transform: translateX(12px); height: 0; margin: 0; padding: 0; }
</style>
