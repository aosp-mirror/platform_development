<template>
  <OTAJobTable
    v-if="jobs"
    :jobs="jobs"
  />
  <v-btn
    block
    @click="updateStatus"
  >
    Update
  </v-btn>
</template>

<script>
import ApiService from '../services/ApiService.js'
import OTAJobTable from '@/components/OTAJobTable.vue'

export default {
  name: 'JobList',
  components: {
    OTAJobTable
  },
  data() {
    return {
      jobs: null,
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
  }
}

</script>