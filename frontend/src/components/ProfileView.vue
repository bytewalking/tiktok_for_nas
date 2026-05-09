<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { fetchWatchSummary, formatWatchTime, type WatchSummary } from '../api/videos'

type Period = 'day' | 'week' | 'year'

const summary = ref<WatchSummary | null>(null)
const loading = ref(true)
const period = ref<Period>('week')

const PERIODS = [
  { value: 'day' as Period,  label: '今日' },
  { value: 'week' as Period, label: '本周' },
  { value: 'year' as Period, label: '全年' },
]

const WEEK_LABELS = ['日', '一', '二', '三', '四', '五', '六']
const MONTH_LABELS = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
const CHART_H = 120 // px

async function load() {
  loading.value = true
  try {
    summary.value = await fetchWatchSummary()
  } finally {
    loading.value = false
  }
}

defineExpose({ reload: load })
onMounted(load)

// ── helpers ────────────────────────────────────────────────────────────────

function todayStr() { return new Date().toISOString().slice(0, 10) }
function thisMonthStr() { return new Date().toISOString().slice(0, 7) }

// ── period totals ──────────────────────────────────────────────────────────

const periodTotal = computed(() => {
  const data = summary.value?.byDay ?? []
  if (period.value === 'day') {
    const e = data.find(d => d.date === todayStr())
    return { seconds: e?.seconds ?? 0, count: e?.count ?? 0 }
  }
  const cutoff = new Date()
  if (period.value === 'week') cutoff.setDate(cutoff.getDate() - 6)
  else cutoff.setFullYear(cutoff.getFullYear() - 1)
  const cutoffStr = cutoff.toISOString().slice(0, 10)
  const filtered = data.filter(d => d.date >= cutoffStr)
  return {
    seconds: filtered.reduce((s, d) => s + d.seconds, 0),
    count:   filtered.reduce((s, d) => s + d.count, 0),
  }
})

// ── chart bars ─────────────────────────────────────────────────────────────

interface Bar { label: string; seconds: number; isCurrent: boolean; px: number }

const chartBars = computed((): Bar[] => {
  if (!summary.value || period.value === 'day') return []
  const data = summary.value.byDay
  const dataMap = new Map(data.map(d => [d.date, d]))
  const bars: Omit<Bar, 'px'>[] = []

  if (period.value === 'week') {
    for (let i = 6; i >= 0; i--) {
      const d = new Date(); d.setDate(d.getDate() - i)
      const ds = d.toISOString().slice(0, 10)
      bars.push({ label: WEEK_LABELS[d.getDay()], seconds: dataMap.get(ds)?.seconds ?? 0, isCurrent: i === 0 })
    }
  } else {
    const thisM = thisMonthStr()
    for (let i = 11; i >= 0; i--) {
      const d = new Date(); d.setDate(1); d.setMonth(d.getMonth() - i)
      const mk = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
      const secs = data.filter(day => day.date.startsWith(mk)).reduce((s, d) => s + d.seconds, 0)
      bars.push({ label: MONTH_LABELS[d.getMonth()], seconds: secs, isCurrent: mk === thisM })
    }
  }

  const maxSec = Math.max(...bars.map(b => b.seconds), 1)
  return bars.map(b => ({
    ...b,
    px: b.seconds === 0 ? 4 : Math.max(8, Math.round((b.seconds / maxSec) * CHART_H))
  }))
})
</script>

<template>
  <div class="h-full flex flex-col bg-black text-white overflow-hidden">

    <!-- Header -->
    <div class="flex-shrink-0 px-4 pt-6 pb-4">
      <h1 class="text-xl font-bold">个人中心</h1>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <div class="w-8 h-8 border-2 border-white/20 border-t-white rounded-full animate-spin"/>
    </div>

    <div v-else class="flex-1 overflow-y-auto px-4 pb-6 space-y-5">

      <!-- Period switcher -->
      <div class="flex bg-white/8 rounded-2xl p-1 gap-1">
        <button
          v-for="p in PERIODS" :key="p.value"
          class="flex-1 text-sm py-2 rounded-xl transition-all font-medium"
          :class="period === p.value
            ? 'bg-white text-black shadow-sm'
            : 'text-white/50 hover:text-white/80'"
          @click="period = p.value"
        >{{ p.label }}</button>
      </div>

      <!-- Period total card -->
      <div class="bg-white/6 rounded-2xl px-5 py-5">
        <p class="text-white/40 text-xs mb-1">
          {{ period === 'day' ? '今日' : period === 'week' ? '本周' : '今年' }}观看时长
        </p>
        <p class="text-3xl font-bold tracking-tight">
          {{ formatWatchTime(periodTotal.seconds) }}
        </p>
        <p class="text-white/40 text-sm mt-1">共 {{ periodTotal.count }} 个视频</p>
      </div>

      <!-- Today detail (no chart) -->
      <div v-if="period === 'day'" class="bg-white/6 rounded-2xl px-5 py-5 flex items-center gap-4">
        <div class="w-14 h-14 rounded-full bg-violet-500/20 flex items-center justify-center flex-shrink-0">
          <svg class="w-7 h-7 text-violet-400" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
        <div>
          <p class="text-white/40 text-xs">今日共开始观看</p>
          <p class="text-lg font-semibold mt-0.5">{{ periodTotal.count }} 个视频</p>
          <p class="text-white/30 text-xs mt-0.5">累计进度 {{ formatWatchTime(periodTotal.seconds) }}</p>
        </div>
      </div>

      <!-- Bar chart (week / year) -->
      <div v-else class="bg-white/6 rounded-2xl px-4 pt-4 pb-3">
        <p class="text-white/40 text-xs mb-3">
          {{ period === 'week' ? '最近 7 天' : '最近 12 个月' }}
        </p>

        <!-- Bars -->
        <div class="flex gap-1.5 items-end" :style="{ height: `${CHART_H}px` }">
          <div
            v-for="bar in chartBars" :key="bar.label"
            class="flex-1 rounded-t-sm transition-all duration-500"
            :class="bar.isCurrent ? 'bg-violet-400' : 'bg-violet-700/60'"
            :style="{ height: `${bar.px}px` }"
          />
        </div>

        <!-- Labels -->
        <div class="flex gap-1.5 mt-2">
          <div
            v-for="bar in chartBars" :key="bar.label"
            class="flex-1 text-center text-xs"
            :class="bar.isCurrent ? 'text-violet-400' : 'text-white/30'"
          >{{ bar.label }}</div>
        </div>
      </div>

      <!-- Divider -->
      <div class="border-t border-white/8"/>

      <!-- All-time stats -->
      <div class="space-y-3">
        <p class="text-white/40 text-xs uppercase tracking-widest">累计数据</p>
        <div class="flex gap-3">
          <div class="flex-1 bg-white/6 rounded-2xl px-4 py-4 text-center">
            <p class="text-2xl font-bold">{{ formatWatchTime(summary?.totalSeconds ?? 0) }}</p>
            <p class="text-white/40 text-xs mt-1">总观看时长</p>
          </div>
          <div class="flex-1 bg-white/6 rounded-2xl px-4 py-4 text-center">
            <p class="text-2xl font-bold">{{ summary?.totalCount ?? 0 }}</p>
            <p class="text-white/40 text-xs mt-1">观看视频数</p>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>
