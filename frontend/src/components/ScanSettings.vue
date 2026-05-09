<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchSmbSettings, saveAndScan, type SmbSettings } from '../api/videos'

const loading = ref(true)
const saving = ref(false)
const scanResult = ref('')
const error = ref('')

const form = ref<SmbSettings>({
  enabled: false,
  host: '',
  share: 'videos',
  path: '',
  domain: '',
  username: '',
  password: '',
  videoDir: '',
})

const showPassword = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    form.value = await fetchSmbSettings()
    form.value.password = '' // never pre-fill password
  } catch (e) {
    error.value = '加载设置失败'
  } finally {
    loading.value = false
  }
}

async function handleSaveAndScan() {
  saving.value = true
  scanResult.value = ''
  error.value = ''
  try {
    await saveAndScan(form.value)
    scanResult.value = '扫描已开始，新视频稍后出现在列表中'
  } catch (e) {
    error.value = '保存失败，请检查后端服务'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="h-full overflow-y-auto bg-black text-white">
    <div class="px-4 py-6 max-w-lg mx-auto">

      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-xl font-bold">扫描设置</h1>
        <button
          class="text-white/40 hover:text-white/70 transition-colors"
          @click="load"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-8 h-8 border-2 border-white/30 border-t-white rounded-full animate-spin"/>
      </div>

      <template v-else>
        <!-- Source toggle -->
        <div class="mb-6">
          <p class="text-xs text-white/40 uppercase tracking-wider mb-2">视频来源</p>
          <div class="flex rounded-xl overflow-hidden border border-white/10">
            <button
              class="flex-1 py-2.5 text-sm font-medium transition-colors"
              :class="!form.enabled
                ? 'bg-white text-black'
                : 'bg-transparent text-white/50 hover:text-white/70'"
              @click="form.enabled = false"
            >
              本地目录
            </button>
            <button
              class="flex-1 py-2.5 text-sm font-medium transition-colors"
              :class="form.enabled
                ? 'bg-white text-black'
                : 'bg-transparent text-white/50 hover:text-white/70'"
              @click="form.enabled = true"
            >
              SMB 网络共享
            </button>
          </div>
        </div>

        <!-- Local directory settings -->
        <div v-if="!form.enabled" class="space-y-4 mb-6">
          <div>
            <label class="block text-xs text-white/40 uppercase tracking-wider mb-1.5">
              视频目录路径
            </label>
            <input
              v-model="form.videoDir"
              type="text"
              placeholder="例: /videos 或 Z:\Private\Movie"
              class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm
                     text-white placeholder-white/25 focus:outline-none focus:border-white/30
                     focus:bg-white/8 transition-colors"
            />
            <p class="text-xs text-white/30 mt-1.5">
              服务器挂载目录 / Windows 网络驱动器路径
            </p>
          </div>
        </div>

        <!-- SMB settings -->
        <div v-else class="space-y-4 mb-6">
          <div class="grid grid-cols-3 gap-3">
            <div class="col-span-2">
              <label class="block text-xs text-white/40 uppercase tracking-wider mb-1.5">
                NAS 地址
              </label>
              <input
                v-model="form.host"
                type="text"
                placeholder="192.168.1.100"
                class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm
                       text-white placeholder-white/25 focus:outline-none focus:border-white/30
                       transition-colors"
              />
            </div>
            <div>
              <label class="block text-xs text-white/40 uppercase tracking-wider mb-1.5">
                共享名
              </label>
              <input
                v-model="form.share"
                type="text"
                placeholder="videos"
                class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm
                       text-white placeholder-white/25 focus:outline-none focus:border-white/30
                       transition-colors"
              />
            </div>
          </div>

          <div>
            <label class="block text-xs text-white/40 uppercase tracking-wider mb-1.5">
              子目录（可选）
            </label>
            <input
              v-model="form.path"
              type="text"
              placeholder="Movie/2024（留空则读共享根目录）"
              class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm
                     text-white placeholder-white/25 focus:outline-none focus:border-white/30
                     transition-colors"
            />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs text-white/40 uppercase tracking-wider mb-1.5">
                用户名
              </label>
              <input
                v-model="form.username"
                type="text"
                placeholder="admin"
                autocomplete="username"
                class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm
                       text-white placeholder-white/25 focus:outline-none focus:border-white/30
                       transition-colors"
              />
            </div>
            <div>
              <label class="block text-xs text-white/40 uppercase tracking-wider mb-1.5">
                密码
              </label>
              <div class="relative">
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="留空则保持不变"
                  autocomplete="current-password"
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 pr-10
                         text-sm text-white placeholder-white/25 focus:outline-none
                         focus:border-white/30 transition-colors"
                />
                <button
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-white/30
                         hover:text-white/60 transition-colors"
                  @click.prevent="showPassword = !showPassword"
                >
                  <svg v-if="!showPassword" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7
                         -1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                  </svg>
                  <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7
                         a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878
                         9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29
                         M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943
                         9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div>
            <label class="block text-xs text-white/40 uppercase tracking-wider mb-1.5">
              域（可选）
            </label>
            <input
              v-model="form.domain"
              type="text"
              placeholder="WORKGROUP（通常留空）"
              class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm
                     text-white placeholder-white/25 focus:outline-none focus:border-white/30
                     transition-colors"
            />
          </div>

          <!-- Preview URL -->
          <div
            v-if="form.host && form.share"
            class="bg-white/5 rounded-xl px-4 py-3 text-xs font-mono text-white/40 break-all"
          >
            smb://{{ form.host }}/{{ form.share }}<span v-if="form.path">/{{ form.path }}</span>
          </div>
        </div>

        <!-- Result / Error -->
        <Transition name="slide">
          <div
            v-if="scanResult"
            class="mb-4 px-4 py-3 bg-green-500/10 border border-green-500/20
                   rounded-xl text-sm text-green-400"
          >
            {{ scanResult }}
          </div>
          <div
            v-else-if="error"
            class="mb-4 px-4 py-3 bg-red-500/10 border border-red-500/20
                   rounded-xl text-sm text-red-400"
          >
            {{ error }}
          </div>
        </Transition>

        <!-- Save & Scan button -->
        <button
          class="w-full py-3.5 rounded-xl font-semibold text-sm transition-all
                 flex items-center justify-center gap-2"
          :class="saving
            ? 'bg-white/10 text-white/40 cursor-not-allowed'
            : 'bg-white text-black hover:bg-white/90 active:scale-[0.98]'"
          :disabled="saving"
          @click="handleSaveAndScan"
        >
          <svg
            class="w-4 h-4"
            :class="{ 'animate-spin': saving }"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0
                 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
          {{ saving ? '扫描中...' : '保存并扫描' }}
        </button>

        <p class="text-center text-xs text-white/25 mt-3">
          扫描在后台运行，完成后刷新「发现」页查看新视频
        </p>
      </template>
    </div>
  </div>
</template>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: all 0.2s ease;
}
.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
