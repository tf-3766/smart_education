<script setup lang="ts">
withDefaults(defineProps<{ variant?: 'student' | 'admin' }>(), { variant: 'student' })
</script>

<template>
  <div class="line-chart" :class="`line-chart--${variant}`" aria-label="趋势折线图">
    <svg viewBox="0 0 620 190" role="img">
      <line v-for="y in [30, 70, 110, 150]" :key="y" x1="42" :y1="y" x2="600" :y2="y" class="chart-grid" />
      <template v-if="variant === 'student'">
        <path d="M50 132 C115 118, 145 110, 205 104 S310 86, 360 90 S470 66, 590 58" class="chart-line chart-line--teal" />
        <path d="M50 150 C110 143, 155 137, 205 132 S315 122, 360 116 S480 105, 590 92" class="chart-line chart-line--blue" />
        <path d="M50 160 C120 162, 165 168, 205 170 S315 156, 360 151 S480 142, 590 130" class="chart-line chart-line--orange" />
      </template>
      <template v-else>
        <path d="M50 154 C110 146, 160 134, 205 124 S310 112, 360 94 S480 70, 590 54" class="chart-line chart-line--blue chart-line--bold" />
        <g v-for="(point, index) in [[50,154],[205,124],[360,94],[590,54]]" :key="index">
          <circle :cx="point[0]" :cy="point[1]" r="5" class="chart-point" />
        </g>
      </template>
      <text x="42" y="181">第8周</text><text x="182" y="181">第9周</text><text x="324" y="181">第10周</text><text x="536" y="181">本周</text>
    </svg>
  </div>
</template>
