<template>
  <div ref="host" class="fchart" />
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Chart } from 'frappe-charts'

const props = defineProps<{
  type: 'line' | 'bar' | 'percentage' | 'pie' | 'donut'
  labels: string[]
  values: number[]
  name?: string
  colors?: string[]
  height?: number
}>()

const host = ref<HTMLDivElement>()

function render() {
  if (!host.value) return
  host.value.innerHTML = ''
  new Chart(host.value, {
    type: props.type,
    height: props.height ?? 220,
    animate: true,
    colors: props.colors ?? ['#0d9488'],
    data: {
      labels: props.labels,
      datasets: [{ name: props.name ?? '', values: props.values }],
    },
    lineOptions: { hideDots: 0, regionFill: 1, spline: 1 },
    barOptions: { spaceRatio: 0.4 },
    axisOptions: { xAxisMode: 'tick', yAxisMode: 'tick' },
  })
}

onMounted(render)
watch(() => [props.type, props.labels, props.values], render, { deep: true })
</script>
