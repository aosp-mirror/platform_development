<template>
  <OTAJobTable
    v-if="jobs"
    :jobs="jobs"
  />
  <v-row>
    <v-col
      v-for="job in jobs"
      :key="job.id"
      cols="12"
      sm="3"
    >
      <JobDisplay
        :job="job"
        :active="overStatus.get(job.id)"
        @mouseover="mouseOver(job.id, true)"
        @mouseout="mouseOver(job.id, false)"
      />
    </v-col>
  </v-row>
  <v-btn
    block
    @click="updateStatus"
  >
    Update
  </v-btn>
</template>

<script>
import JobDisplay from '@/components/JobDisplay.vue'
import ApiService from '../services/ApiService.js'
import OTAJobTable from '@/components/OTAJobTable.vue'

export default {
  name: 'JobList',
  components: {
    JobDisplay,
    OTAJobTable
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