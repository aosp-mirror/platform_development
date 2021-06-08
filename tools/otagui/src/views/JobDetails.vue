<template>
  <div v-if="job">
    <h3>Job. {{ job.id }} {{ job.status }}</h3>
    <div>
      <h4>STDERR</h4>
      <div class="stderr">
        {{ job.stderr }}
        <p ref="stderr_bottom" />
      </div>
      <h4>STDOUT</h4>
      <div class="stdout">
        {{ job.stdout }}
        <p ref="stdout_bottom" />
      </div>
    </div>
    <br>
    <a
      v-if="job.status == 'Finished'"
      :href="download"
    > Download </a>
  </div>
</template>

<script>
import ApiService from '../services/ApiService.js'
export default {
  props: ['id'],
  data() {
    return {
      job: null,
    }
  },
  computed: {
    download() {
      return 'http://localhost:8000/download/' + this.job.path
    },
  },
  created() {
    this.updateStatus()
  },
  methods: {
    async updateStatus() {
      // fetch job (by id) and set local job data
      try {
        let response = await ApiService.getJobById(this.id)
        this.job = response.data
        await this.$refs.stdout_bottom.scrollIntoView({ behavior: 'smooth' })
        await this.$refs.stderr_bottom.scrollIntoView({ behavior: 'smooth' })
      } catch (err) {
        console.log(err)
      }
      if (this.job.status == 'Running') {
        setTimeout(this.updateStatus, 1000)
      }
    },
  },
}
</script>

<style scoped>
.stderr,
.stdout {
	overflow: scroll;
	height: 200px;
}
</style>