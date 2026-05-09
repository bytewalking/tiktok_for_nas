export interface Video {
  id: number
  title: string
  cover: string | null
  duration: number | null
  size: number | null
  codec: string | null
  resolution: string | null
  hlsReady: boolean
  createdAt: string
}

export interface PlayHistory {
  id: number
  videoId: number
  progress: number
  updatedAt: string
}

const BASE = ''

export async function fetchVideos(): Promise<Video[]> {
  const res = await fetch(`${BASE}/api/videos`)
  if (!res.ok) throw new Error('Failed to fetch videos')
  return res.json()
}

export async function fetchHlsUrl(id: number): Promise<string> {
  const res = await fetch(`${BASE}/api/video/${id}/hls`)
  if (!res.ok) throw new Error('Failed to get HLS URL')
  const data = await res.json()
  return data.url as string
}

export function getVideoStreamUrl(id: number): string {
  return `${BASE}/api/video/${id}`
}

export async function saveProgress(videoId: number, progress: number): Promise<void> {
  await fetch(`${BASE}/api/history/${videoId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ progress: Math.floor(progress) }),
  })
}

export async function fetchHistory(): Promise<PlayHistory[]> {
  const res = await fetch(`${BASE}/api/history`)
  if (!res.ok) throw new Error('Failed to fetch history')
  return res.json()
}

export async function triggerScan(): Promise<void> {
  await fetch(`${BASE}/api/scan`, { method: 'POST' })
}

export async function fetchFavoriteIds(): Promise<number[]> {
  const res = await fetch(`${BASE}/api/favorites/ids`)
  if (!res.ok) return []
  return res.json()
}

export async function fetchFavoriteVideos(): Promise<Video[]> {
  const res = await fetch(`${BASE}/api/favorites`)
  if (!res.ok) return []
  return res.json()
}

export async function toggleFavorite(videoId: number): Promise<boolean> {
  const res = await fetch(`${BASE}/api/favorites/${videoId}`, { method: 'POST' })
  const data = await res.json()
  return data.favorited as boolean
}

export interface SmbSettings {
  enabled: boolean
  host: string
  share: string
  path: string
  domain: string
  username: string
  password: string
  videoDir: string
}

export async function fetchSmbSettings(): Promise<SmbSettings> {
  const res = await fetch(`${BASE}/api/settings/smb`)
  if (!res.ok) throw new Error('Failed to fetch settings')
  return res.json()
}

export async function saveAndScan(settings: SmbSettings): Promise<void> {
  const res = await fetch(`${BASE}/api/settings/smb/scan`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(settings),
  })
  if (!res.ok) throw new Error('Failed to save settings')
}

export function formatDuration(seconds: number | null): string {
  if (!seconds) return '0:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}
