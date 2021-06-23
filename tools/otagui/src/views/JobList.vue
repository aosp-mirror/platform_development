<template>
  <div class="jobs">
    <JobDisplay
      v-for="job in jobs"
      :key="job.id"
      :job="job"
      :active="overStatus.get(job.id)"
      @mouseover="mouseOver(job.id, true)"
      @mouseout="mouseOver(job.id, false)"
    />
    <button @click="updateStatus">
      Update
    </button>
  </div>
</template>

<script>
import JobDisplay from '@/components/JobDisplay.vue'
import ApiService from '../services/ApiService.js'

export default {
  name: 'JobList',
  components: {
    JobDisplay,
  },
  data() {
    return {
      jobs: null,
      overStatus: new Map()
    }
  },
  created (){
    this.updateStatus()
  },
  methods:{
    async updateStatus() {
      try {
        let response = await ApiService.getJobs()
        this.jobs = response.data;
      } catch (err) {
        console.log(err);
      }
    },
    mouseOver(id, status) {
      this.overStatus.set(id, status)
    }
  }
}

</script>

<style scoped>
.jobs {
  display: flex;
  flex-direction: column;
  align-items: center;
}

</style>
