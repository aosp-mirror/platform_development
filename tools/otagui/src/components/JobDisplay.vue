<template>
  <router-link
    :to="{ name: 'JobDetails', params: {id: job.id} }"
  >
    <div class="job-display">
      <span>Status of Job.{{ job.id }}</span>
      <h4>{{ job.status }}</h4>
    </div>
  </router-link>
</template>

<script>
export default {
  props: {
    job: {
      type: Object,
      required: true
    }
  }
}
</script>

<style scoped>
.job-display {
  padding: 20px;
  width: 250px;
  cursor: pointer;
  border: 1px solid #39495c;
  margin-bottom: 18px;
}

.job-display:hover {
  transform: scale(1.01);
  box-shadow: 0 3px 12px 0 rgba(0, 0, 0, 0.2);
}
</style>
