<script setup lang="ts">
import { ref, watch, provide, computed } from 'vue'
import VideoFeed from './components/VideoFeed.vue'
import FavoritesView from './components/FavoritesView.vue'
import ScanSettings from './components/ScanSettings.vue'
import ProfileView from './components/ProfileView.vue'

type Tab = 'feed' | 'favorites' | 'profile' | 'settings'
const activeTab = ref<Tab>('feed')

// Fullscreen state — shared with VideoPlayer via provide/inject
const isFullscreen = ref(false)
provide('isFullscreen', isFullscreen)
provide('setFullscreen', (v: boolean) => { isFullscreen.value = v })

// Fix 5: pause video when leaving feed tab
const isAppPaused = computed(() => activeTab.value !== 'feed')
provide('isAppPaused', isAppPaused)

// Fix 3: allow FavoritesView to jump to a video in the feed
const requestVideoId = ref<number | null>(null)
provide('requestVideoId', requestVideoId)
provide('requestPlayVideo', (id: number) => {
  requestVideoId.value = id
  activeTab.value = 'feed'
})

// Reload favorites/profile list whenever the tab becomes active
const favoritesRef = ref<InstanceType<typeof FavoritesView> | null>(null)
const profileRef   = ref<InstanceType<typeof ProfileView>   | null>(null)
watch(activeTab, tab => {
  if (tab === 'favorites') favoritesRef.value?.reload()
  if (tab === 'profile')   profileRef.value?.reload()
})
</script>

<template>
  <div class="w-full h-full flex flex-col bg-black overflow-hidden">
    <!-- Main content -->
    <div class="flex-1 relative overflow-hidden">
      <VideoFeed    v-show="activeTab === 'feed'"      class="absolute inset-0" />
      <FavoritesView ref="favoritesRef" v-show="activeTab === 'favorites'" class="absolute inset-0" />
      <ProfileView   ref="profileRef"   v-show="activeTab === 'profile'"   class="absolute inset-0" />
      <ScanSettings  v-show="activeTab === 'settings'" class="absolute inset-0" />
    </div>

    <!-- Bottom navigation — hidden in fullscreen -->
    <nav v-show="!isFullscreen" class="flex-shrink-0 flex items-center bg-black/90 border-t border-white/10 pb-safe">

      <!-- 发现 -->
      <button
        class="flex-1 flex flex-col items-center py-2 gap-0.5 transition-colors"
        :class="activeTab === 'feed' ? 'text-white' : 'text-white/40'"
        @click="activeTab = 'feed'"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 22V12h6v10"/>
        </svg>
        <span class="text-xs">发现</span>
      </button>

      <!-- 收藏 -->
      <button
        class="flex-1 flex flex-col items-center py-2 gap-0.5 transition-colors"
        :class="activeTab === 'favorites' ? 'text-white' : 'text-white/40'"
        @click="activeTab = 'favorites'"
      >
        <svg
          class="w-6 h-6 transition-colors"
          :fill="activeTab === 'favorites' ? 'currentColor' : 'none'"
          stroke="currentColor"
          stroke-width="2"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0
               00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
        </svg>
        <span class="text-xs">收藏</span>
      </button>

      <!-- 个人中心 -->
      <button
        class="flex-1 flex flex-col items-center py-2 gap-0.5 transition-colors"
        :class="activeTab === 'profile' ? 'text-white' : 'text-white/40'"
        @click="activeTab = 'profile'"
      >
        <svg
          class="w-6 h-6"
          :fill="activeTab === 'profile' ? 'currentColor' : 'none'"
          stroke="currentColor"
          stroke-width="2"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
        </svg>
        <span class="text-xs">我的</span>
      </button>

      <!-- 设置 -->
      <button
        class="flex-1 flex flex-col items-center py-2 gap-0.5 transition-colors"
        :class="activeTab === 'settings' ? 'text-white' : 'text-white/40'"
        @click="activeTab = 'settings'"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573
               1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426
               1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37
               2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724
               1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0
               00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0
               001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07
               2.572-1.065z"/>
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
        </svg>
        <span class="text-xs">设置</span>
      </button>

    </nav>
  </div>
</template>
