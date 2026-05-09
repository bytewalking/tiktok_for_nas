<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchHistory, fetchVideos, type PlayHistory, type Video, formatDuration } from '../api/videos'

const history = ref<PlayHistory[]>([])
const videos = ref<Map<number, Video>>(new Map())
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const [hist, vids] = await Promise.all([fetchHistory(), fetchVideos()])
    history.value = hist
    const map = new Map<number, Video>()
    vids.forEach(v => map.set(v.id, v))
    videos.value = map
  } finally {
    loading.value = false
  }
}

function formatTime(iso: string): string {
  const d = new Date(iso)
  return d.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(load)
</script>

<template>
  <div class="h-full overflow-y-auto bg-black text-white">
    <div class="px-4 py-6">
      <h1 class="text-xl font-bold mb-4">最近观看</h1>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-8 h-8 border-2 border-white/30 border-t-white rounded-full animate-spin" />
      </div>

      <div v-else-if="history.length === 0" class="text-center py-12 text-white/40">
        暂无观看记录
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="item in history"
          :key="item.id"
          class="flex items-center gap-3 bg-white/5 rounded-xl p-3"
        >
          <!-- Thumbnail -->
          <div class="w-20 h-14 rounded-lg overflow-hidden bg-white/10 flex-shrink-0">
            <img
              v-if="videos.get(item.videoId)?.cover"
              :src="videos.get(item.videoId)?.cover ?? ''"
              class="w-full h-full object-cover"
              loading="lazy"
            />
            <div v-else class="w-full h-full flex items-center justify-center">
              <svg class="w-6 h-6 text-white/30" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z" />
              </svg>
            </div>
          </div>

          <!-- Info -->
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium truncate">
              {{ videos.get(item.videoId)?.title ?? `视频 #${item.videoId}` }}
            </p>
            <p class="text-xs text-white/40 mt-0.5">
              进度 {{ formatDuration(item.progress) }}
              <span v-if="videos.get(item.videoId)?.duration">
                / {{ formatDuration(videos.get(item.videoId)!.duration) }}
              </span>
            </p>
            <p class="text-xs text-white/30 mt-0.5">{{ formatTime(item.updatedAt) }}</p>
          </div>

          <!-- Progress bar -->
          <div class="w-1 self-stretch bg-white/10 rounded-full overflow-hidden flex-shrink-0">
            <div
              class="w-full bg-white/60 rounded-full"
              :style="{
                height: videos.get(item.videoId)?.duration
                  ? (item.progress / videos.get(item.videoId)!.duration!) * 100 + '%'
                  : '0%'
              }"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
