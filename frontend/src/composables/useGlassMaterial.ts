import { computed, reactive, watch } from 'vue'

export interface GlassMaterialSettings {
  displacementScale: number
  blur: number
  saturation: number
  surfaceOpacity: number
  warpOpacity: number
}

export const DEFAULT_GLASS_MATERIAL: Readonly<GlassMaterialSettings> = Object.freeze({
  displacementScale: 22,
  blur: 18,
  saturation: 175,
  surfaceOpacity: 14,
  warpOpacity: 8,
})

const STORAGE_KEY = 'smart-education-glass-material'
const limits: Record<keyof GlassMaterialSettings, readonly [number, number]> = {
  displacementScale: [0, 50],
  blur: [0, 40],
  saturation: [100, 220],
  surfaceOpacity: [4, 40],
  warpOpacity: [0, 24],
}

function clampValue(key: keyof GlassMaterialSettings, value: unknown) {
  const fallback = DEFAULT_GLASS_MATERIAL[key]
  const parsed = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(parsed)) return fallback
  const [min, max] = limits[key]
  return Math.min(max, Math.max(min, Math.round(parsed)))
}

function loadSettings(): GlassMaterialSettings {
  if (typeof localStorage === 'undefined') return { ...DEFAULT_GLASS_MATERIAL }
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}') as Partial<GlassMaterialSettings>
    return Object.fromEntries(
      Object.keys(DEFAULT_GLASS_MATERIAL).map((key) => [key, clampValue(key as keyof GlassMaterialSettings, saved[key as keyof GlassMaterialSettings])]),
    ) as unknown as GlassMaterialSettings
  } catch {
    return { ...DEFAULT_GLASS_MATERIAL }
  }
}

const settings = reactive<GlassMaterialSettings>(loadSettings())

watch(settings, (value) => {
  if (typeof localStorage !== 'undefined') localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
}, { deep: true })

const cssVariables = computed(() => ({
  '--liquid-surface': `rgba(255,255,255,${settings.surfaceOpacity / 100})`,
  '--liquid-warp-surface': `rgba(255,255,255,${settings.warpOpacity / 100})`,
  '--liquid-border': 'rgba(255,255,255,.76)',
  '--student-glass-surface': `rgba(255,255,255,${settings.surfaceOpacity / 100})`,
  '--student-glass-blur': `${settings.blur}px`,
  '--student-glass-saturation': `${settings.saturation}%`,
}))

export function useGlassMaterial() {
  function reset() {
    Object.assign(settings, DEFAULT_GLASS_MATERIAL)
  }

  return { settings, cssVariables, reset }
}

