<template>
  <ul v-if="job">
    <li>Start Time: {{ formDate(job.start_time) }}</li>
    <li v-if="job.finish_time > 0">
      Finish Time: {{ formDate(job.finish_time) }}
    </li>
    <li v-if="job.isIncremental">
      Incremental source: {{ job.incremental_name }}
    </li>
    <li v-if="job.isIncremental && buildDetail">
      Incremental source version: {{ job.incremental_build_version }}
    </li>
    <li>Target source: {{ job.target_name }}</li>
    <li v-if="buildDetail">
      Target source version: {{ job.target_build_version }}
    </li>
    <li v-if="job.isPartial">
      Partial: {{ job.partial }}
    </li>
  </ul>
</template>

<script>
import FormDate from '../services/FormDate.js'

export default {
  components: {
    FormDate,
  },
  props: {
    job: {
      type: Object,
      required: true,
      default: null,
    },
    buildDetail: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    formDate(unixTime) {
      return FormDate.formDate(unixTime)
    },
  },
}
</script>