<template>
  <component
    :is="as"
    ref="root"
    class="liquid-glass"
    :class="{ 'liquid-glass--interactive': interactive }"
    :style="glassStyle"
    @pointermove="trackPointer"
    @pointerleave="resetPointer"
  >
    <svg class="liquid-glass__filter" aria-hidden="true">
      <defs>
        <filter :id="filterId" x="-12%" y="-12%" width="124%" height="124%" color-interpolation-filters="sRGB">
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.008 0.022"
            :seed="filterSeed"
            numOctaves="2"
            result="noise"
          />
          <feGaussianBlur in="noise" stdDeviation="0.55" result="softNoise" />
          <feDisplacementMap
            in="SourceGraphic"
            in2="softNoise"
            :scale="effectiveDisplacementScale"
            xChannelSelector="R"
            yChannelSelector="B"
            result="refracted"
          />
          <feGaussianBlur in="refracted" stdDeviation="0.18" />
        </filter>
      </defs>
    </svg>

    <span class="liquid-glass__warp" aria-hidden="true" />
    <span class="liquid-glass__rim liquid-glass__rim--screen" aria-hidden="true" />
    <span class="liquid-glass__rim liquid-glass__rim--overlay" aria-hidden="true" />
    <div class="liquid-glass__content"><slot /></div>
  </component>
</template>

<script setup lang="ts">
import { computed, ref, useId } from 'vue'
import { useGlassMaterial } from '@/composables/useGlassMaterial'

// Vue adaptation of the browser technique demonstrated by rdev/liquid-glass-react (MIT):
// a displaced backdrop layer stays behind sharp content, with two masked rim highlights.
const props = withDefaults(defineProps<{
  as?: string
  displacementScale?: number
  blur?: number
  saturation?: number
  interactive?: boolean
}>(), {
  as: 'div',
  displacementScale: undefined,
  blur: undefined,
  saturation: undefined,
  interactive: false,
})

const glassMaterial = useGlassMaterial()
const effectiveDisplacementScale = computed(() => props.displacementScale ?? glassMaterial.settings.displacementScale)
const effectiveBlur = computed(() => props.blur ?? glassMaterial.settings.blur)
const effectiveSaturation = computed(() => props.saturation ?? glassMaterial.settings.saturation)

const root = ref<HTMLElement | null>(null)
const rawId = useId().replace(/[^a-zA-Z0-9_-]/g, '')
const filterId = `liquid-glass-${rawId}`
const filterSeed = [...rawId].reduce((sum, char) => sum + char.charCodeAt(0), 0) % 97
const pointer = ref({ x: 0, y: 0 })

const glassStyle = computed(() => ({
  '--liquid-filter': `url(#${filterId})`,
  '--liquid-blur': `${effectiveBlur.value}px`,
  '--liquid-saturation': `${effectiveSaturation.value}%`,
  '--liquid-x': `${pointer.value.x}%`,
  '--liquid-y': `${pointer.value.y}%`,
  '--liquid-rim-a': `${48 + pointer.value.y * .08}%`,
  '--liquid-rim-b': `${72 + pointer.value.x * .06}%`,
  '--liquid-angle': `${135 + pointer.value.x * .35}deg`,
}))

function trackPointer(event: PointerEvent) {
  if (!props.interactive || !root.value) return
  const bounds = root.value.getBoundingClientRect()
  pointer.value = {
    x: ((event.clientX - bounds.left) / bounds.width - .5) * 100,
    y: ((event.clientY - bounds.top) / bounds.height - .5) * 100,
  }
}

function resetPointer() {
  pointer.value = { x: 0, y: 0 }
}
</script>

<style scoped>
.liquid-glass {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  border: 1px solid var(--liquid-border, rgba(255, 255, 255, .76));
  background: var(--liquid-surface, rgba(255, 255, 255, .14));
  box-shadow:
    0 14px 36px rgba(39, 83, 126, .13),
    inset 0 1px 0 rgba(255, 255, 255, .86),
    inset 0 -1px 0 rgba(181, 218, 247, .24);
}

.liquid-glass__filter {
  position: absolute;
  width: 0;
  height: 0;
  pointer-events: none;
}

.liquid-glass__warp {
  position: absolute;
  z-index: -2;
  inset: -10px;
  pointer-events: none;
  background: var(--liquid-warp-surface, rgba(255, 255, 255, .08));
  backdrop-filter: blur(var(--liquid-blur)) saturate(var(--liquid-saturation));
  -webkit-backdrop-filter: blur(var(--liquid-blur)) saturate(var(--liquid-saturation));
  filter: var(--liquid-filter);
  transform: scale(1.025);
}

.liquid-glass__rim {
  position: absolute;
  z-index: 2;
  inset: 0;
  padding: 1.5px;
  pointer-events: none;
  background: linear-gradient(
    var(--liquid-angle),
    rgba(255,255,255,.08) 4%,
    rgba(255,255,255,.86) var(--liquid-rim-a),
    rgba(188,224,255,.34) var(--liquid-rim-b),
    rgba(255,255,255,.08) 96%
  );
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
}

.liquid-glass__rim--screen { opacity: .72; mix-blend-mode: screen; }
.liquid-glass__rim--overlay { opacity: .32; mix-blend-mode: overlay; }
.liquid-glass__content { position: relative; z-index: 1; width: 100%; min-width: 0; }

.liquid-glass--interactive::after {
  content: '';
  position: absolute;
  z-index: 0;
  inset: 0;
  pointer-events: none;
  opacity: 0;
  background: radial-gradient(circle at calc(50% + var(--liquid-x)) calc(35% + var(--liquid-y)), rgba(255,255,255,.48), transparent 52%);
  mix-blend-mode: overlay;
  transition: opacity 180ms ease;
}
.liquid-glass--interactive:hover::after { opacity: .6; }

@supports not ((backdrop-filter: blur(2px)) or (-webkit-backdrop-filter: blur(2px))) {
  .liquid-glass { background: rgba(247, 251, 255, .93); }
  .liquid-glass__warp { display: none; }
}

@media (prefers-reduced-transparency: reduce) {
  .liquid-glass { background: rgba(255, 255, 255, .94); }
  .liquid-glass__warp { backdrop-filter: none; -webkit-backdrop-filter: none; filter: none; }
}
</style>
