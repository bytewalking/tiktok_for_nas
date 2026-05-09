<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed, inject, type Ref } from 'vue'
import Hls from 'hls.js'
import { type Video, getVideoStreamUrl, saveProgress, formatDuration } from '../api/videos'

export type PlayStatus = 'active' | 'preload' | 'idle'

const props = defineProps<{
  video: Video
  status: PlayStatus
  isFavorited?: boolean
}>()

const emit = defineEmits<{
  'toggle-favorite': [videoId: number]
}>()

// ── fullscreen (provided by App.vue) ──────────────────────────────────────
const isFullscreenRef = inject<Ref<boolean>>('isFullscreen', ref(false))
const setFullscreen = inject<(v: boolean) => void>('setFullscreen', () => {})
const isFullscreen = computed(() => isFullscreenRef.value)

function toggleFullscreen() {
  setFullscreen(!isFullscreen.value)
}

// ── state ─────────────────────────────────────────────────────────────────
const videoRef = ref<HTMLVideoElement | null>(null)
const isPlaying = ref(false)
const isBuffering = ref(false)
const currentTime = ref(0)
const duration = ref(props.video.duration ?? 0)
const isMuted = ref(false)
const showPauseIcon = ref(false)
const seekIndicator = ref('')

let hls: Hls | null = null
let progressTimer: ReturnType<typeof setInterval> | null = null
let pauseIconTimer: ReturnType<typeof setTimeout> | null = null
let seekIndicatorTimer: ReturnType<typeof setTimeout> | null = null
let srcLoaded = false

// ── swipe-to-seek ─────────────────────────────────────────────────────────
let touchStartX = 0
let touchStartY = 0
let swipeDetected = false

function onTouchStart(e: TouchEvent) {
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
  swipeDetected = false
}

function onTouchEnd(e: TouchEvent) {
  const deltaX = e.changedTouches[0].clientX - touchStartX
  const deltaY = e.changedTouches[0].clientY - touchStartY
  if (Math.abs(deltaX) > 40 && Math.abs(deltaX) > Math.abs(deltaY)) {
    swipeDetected = true
    const v = videoRef.value
    if (v && duration.value) {
      const secs = deltaX > 0 ? 10 : -10
      v.currentTime = Math.max(0, Math.min(duration.value, v.currentTime + secs))
      flashSeekIndicator(secs)
    }
  }
}

function flashSeekIndicator(secs: number) {
  seekIndicator.value = secs > 0 ? `+${secs}s` : `${secs}s`
  if (seekIndicatorTimer) clearTimeout(seekIndicatorTimer)
  seekIndicatorTimer = setTimeout(() => { seekIndicator.value = '' }, 800)
}

// ── computed ──────────────────────────────────────────────────────────────
const progressPercent = computed(() =>
  duration.value ? (currentTime.value / duration.value) * 100 : 0
)

// ── source management ──────────────────────────────────────────────────────

function loadSource() {
  if (srcLoaded) return
  srcLoaded = true
  const video = videoRef.value
  if (!video) return
  const src = getVideoStreamUrl(props.video.id)
  if (props.video.hlsReady && Hls.isSupported()) {
    hls = new Hls({ enableWorker: true, lowLatencyMode: false, maxBufferLength: 30, maxMaxBufferLength: 60 })
    hls.loadSource(`/hls/${props.video.id}/index.m3u8`)
    hls.attachMedia(video)
  } else {
    video.src = src
    video.preload = 'auto'
  }
}

function unloadSource() {
  const video = videoRef.value
  if (!video) return
  video.pause()
  if (hls) { hls.destroy(); hls = null }
  video.src = ''
  video.load()
  srcLoaded = false
  isPlaying.value = false
  isBuffering.value = false
}

// ── status handler ────────────────────────────────────────────────────────

function applyStatus(status: PlayStatus) {
  const video = videoRef.value
  if (!video) return
  if (status === 'active') {
    isBuffering.value = true
    loadSource()
    video.play().catch(() => {
      // Autoplay blocked — retry muted so the switch always plays
      video.muted = true
      isMuted.value = true
      video.play().catch(() => { isBuffering.value = false })
    })
    startProgressTracking()
  } else if (status === 'preload') {
    loadSource()
    video.pause()
    stopProgressTracking()
  } else {
    stopProgressTracking()
    unloadSource()
  }
}

onMounted(() => applyStatus(props.status))
watch(() => props.status, applyStatus)

onUnmounted(() => {
  unloadSource()
  stopProgressTracking()
  if (pauseIconTimer) clearTimeout(pauseIconTimer)
  if (seekIndicatorTimer) clearTimeout(seekIndicatorTimer)
})

// ── video events ──────────────────────────────────────────────────────────

function onPlay() { isPlaying.value = true; isBuffering.value = false }
function onPause() { isPlaying.value = false }
function onWaiting() { if (props.status === 'active') isBuffering.value = true }
function onCanPlay() {
  isBuffering.value = false
  if (props.status === 'active' && videoRef.value?.paused) {
    videoRef.value.play().catch(() => {})
  }
}
function onTimeUpdate() {
  const v = videoRef.value
  if (!v) return
  currentTime.value = v.currentTime
  if (v.duration && isFinite(v.duration)) duration.value = v.duration
}
function onLoadedMetadata() {
  const v = videoRef.value
  if (v?.duration && isFinite(v.duration)) duration.value = v.duration
}

// ── controls ──────────────────────────────────────────────────────────────

function handleClick() {
  if (swipeDetected) { swipeDetected = false; return }
  togglePlay()
}

function togglePlay() {
  const v = videoRef.value
  if (!v) return
  if (v.paused) { v.play().catch(() => {}) }
  else { v.pause(); flashPauseIcon() }
}

function flashPauseIcon() {
  showPauseIcon.value = true
  if (pauseIconTimer) clearTimeout(pauseIconTimer)
  pauseIconTimer = setTimeout(() => { showPauseIcon.value = false }, 800)
}

function toggleMute() {
  const v = videoRef.value
  if (!v) return
  v.muted = !v.muted
  isMuted.value = v.muted
}

function seekFromX(clientX: number, el: HTMLElement) {
  const v = videoRef.value
  if (!v || !duration.value) return
  const rect = el.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width))
  v.currentTime = ratio * duration.value
}

function onProgressClick(e: MouseEvent) {
  seekFromX(e.clientX, e.currentTarget as HTMLElement)
}
function onProgressTouchStart(e: TouchEvent) {
  seekFromX(e.touches[0].clientX, e.currentTarget as HTMLElement)
}
function onProgressTouchMove(e: TouchEvent) {
  seekFromX(e.touches[0].clientX, e.currentTarget as HTMLElement)
}

// ── progress tracking ─────────────────────────────────────────────────────

function startProgressTracking() {
  stopProgressTracking()
  progressTimer = setInterval(() => {
    const v = videoRef.value
    if (v && !v.paused) saveProgress(props.video.id, v.currentTime).catch(() => {})
  }, 5000)
}

function stopProgressTracking() {
  if (progressTimer) { clearInterval(progressTimer); progressTimer = null }
}
</script>

<template>
  <div
    class="relative w-full h-full bg-black flex items-center justify-center select-none overflow-hidden"
    @click="handleClick"
    @touchstart.passive="onTouchStart"
    @touchend.passive="onTouchEnd"
  >
    <!-- Video -->
    <video
      ref="videoRef"
      class="w-full h-full object-contain"
      :poster="video.cover ?? undefined"
      playsinline
      loop
      preload="none"
      @play="onPlay"
      @pause="onPause"
      @waiting="onWaiting"
      @canplay="onCanPlay"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
    />

    <!-- Buffering spinner -->
    <Transition name="fade">
      <div
        v-if="isBuffering && status === 'active'"
        class="absolute inset-0 flex items-center justify-center pointer-events-none"
      >
        <div
          v-if="video.cover"
          class="absolute inset-0 bg-cover bg-center"
          :style="{ backgroundImage: `url(${video.cover})`, filter: 'blur(20px) brightness(0.4)' }"
        />
        <div class="relative z-10 w-10 h-10 border-2 border-white/30 border-t-white rounded-full animate-spin" />
      </div>
    </Transition>

    <!-- Pause icon flash -->
    <Transition name="scale-fade">
      <div
        v-if="showPauseIcon"
        class="absolute inset-0 flex items-center justify-center pointer-events-none"
      >
        <div class="bg-black/50 rounded-full p-5 backdrop-blur-sm">
          <svg class="w-10 h-10 text-white" fill="currentColor" viewBox="0 0 24 24">
            <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>
          </svg>
        </div>
      </div>
    </Transition>

    <!-- Seek indicator -->
    <Transition name="scale-fade">
      <div
        v-if="seekIndicator"
        class="absolute inset-0 flex items-center justify-center pointer-events-none"
      >
        <div class="bg-black/60 rounded-2xl px-6 py-3 backdrop-blur-sm">
          <span class="text-white text-2xl font-bold tracking-wide">{{ seekIndicator }}</span>
        </div>
      </div>
    </Transition>

    <!-- Right action bar -->
    <div
      class="absolute right-3 bottom-28 flex flex-col gap-5 items-center z-10"
      @click.stop
    >
      <!-- Mute -->
      <button class="flex flex-col items-center gap-1" @click="toggleMute">
        <div class="bg-black/30 rounded-full p-2.5 backdrop-blur-sm">
          <svg v-if="!isMuted" class="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
            <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
          </svg>
          <svg v-else class="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
            <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>
          </svg>
        </div>
        <span class="text-white text-xs drop-shadow">{{ isMuted ? '静音' : '声音' }}</span>
      </button>

      <!-- Favorite -->
      <button
        class="flex flex-col items-center gap-1 transition-transform active:scale-90"
        @click.stop="emit('toggle-favorite', video.id)"
      >
        <div
          class="rounded-full p-2.5 backdrop-blur-sm transition-colors"
          :class="isFavorited ? 'bg-red-500/80' : 'bg-black/30'"
        >
          <svg
            class="w-6 h-6"
            :fill="isFavorited ? 'currentColor' : 'none'"
            stroke="currentColor"
            stroke-width="2"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0
                 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
          </svg>
        </div>
        <span class="text-white text-xs drop-shadow">{{ isFavorited ? '已收藏' : '收藏' }}</span>
      </button>

      <!-- Fullscreen -->
      <button class="flex flex-col items-center gap-1" @click="toggleFullscreen">
        <div class="bg-black/30 rounded-full p-2.5 backdrop-blur-sm">
          <svg v-if="!isFullscreen" class="w-6 h-6 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4"/>
          </svg>
          <svg v-else class="w-6 h-6 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25"/>
          </svg>
        </div>
        <span class="text-white text-xs drop-shadow">{{ isFullscreen ? '退出' : '全屏' }}</span>
      </button>
    </div>

    <!-- Video info -->
    <div class="absolute bottom-14 left-4 right-20 z-10 pointer-events-none">
      <h3 class="text-white font-semibold text-sm leading-snug drop-shadow-lg line-clamp-2">
        {{ video.title }}
      </h3>
      <p class="text-white/50 text-xs mt-1">
        {{ formatDuration(video.duration) }}
        <span v-if="video.resolution" class="ml-2">· {{ video.resolution }}</span>
      </p>
    </div>

    <!-- Time -->
    <div class="absolute bottom-6 right-3 z-10 pointer-events-none">
      <span class="text-white/50 text-xs tabular-nums drop-shadow">
        {{ formatDuration(currentTime) }} / {{ formatDuration(duration) }}
      </span>
    </div>

    <!-- Progress bar — h-5 touch area, h-1.5 visible track -->
    <div
      class="absolute bottom-0 left-0 right-0 h-5 cursor-pointer z-10"
      @click.stop="onProgressClick"
      @touchstart.stop="onProgressTouchStart"
      @touchmove.stop.prevent="onProgressTouchMove"
      @touchend.stop
    >
      <div class="absolute bottom-0 left-0 right-0 h-1.5 bg-white/20">
        <div
          class="h-full bg-white/80 transition-none"
          :style="{ width: progressPercent + '%' }"
        />
        <div
          class="absolute top-1/2 -translate-y-1/2 w-4 h-4 bg-white rounded-full shadow-lg"
          :style="{ left: `calc(${progressPercent}% - 8px)` }"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.scale-fade-enter-active { transition: opacity 0.1s, transform 0.1s; }
.scale-fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.scale-fade-enter-from, .scale-fade-leave-to { opacity: 0; transform: scale(1.3); }
</style>
