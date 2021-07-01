<template>
  <v-chart
    class="chart"
    :option="getEchartsOption"
  />
</template>

<script>
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components'
import VChart, { THEME_KEY } from 'vue-echarts'
import { EchartsData } from '@/services/echarts_data.js'

use([
  CanvasRenderer,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
])

export default {
  components: {
    VChart,
  },
  provide: {
    [THEME_KEY]: 'light',
  },
  props: {
    echartsData: {
      type: EchartsData,
      required: true,
    },
  },
  computed: {
    getEchartsOption() {
      return this.echartsData.getEchartsOption()
    },
  },
}
</script>

<style scoped>
.chart {
  height: 400px;
}
</style>