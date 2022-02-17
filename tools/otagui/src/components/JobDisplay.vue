<template>
  <router-link :to="{ name: 'JobDetails', params: { id: job.id } }">
    <div class="job-display">
      <span>Status of Job.{{ job.id }}</span>
      <h4>{{ job.status }}</h4>
      <div v-show="active">
        <v-divider class="my-3" />
        <JobConfiguration
          :job="job"
          :build-detail="false"
        />
      </div>
    </div>
  </router-link>
</template>

<script>
import JobConfiguration from '../components/JobConfiguration.vue'

export default {
  components: {
    JobConfiguration
  },
  props: {
    job: {
      type: Object,
      required: true,
    },
    active: {
      type: Boolean,
      default: false,
    },
  }
}
</script>

<style>
.job-display {
  padding: 5%;
  width: 90%;
  cursor: pointer;
  border: 1px solid #39495c;
  margin-bottom: 18px;
}

.job-display:hover {
  transform: scale(1.01);
  box-shadow: 0 3px 12px 0 rgba(0, 0, 0, 0.2);
}
</style>
