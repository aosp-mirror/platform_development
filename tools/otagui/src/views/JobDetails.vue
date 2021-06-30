<template>
  <div v-if="job">
    <h3>Job. {{ job.id }} {{ job.status }}</h3>
    <JobConfiguration
      :job="job"
      :build-detail="true"
    />
    <div>
      <h4>STDERR</h4>
      <div
        ref="stderr"
        class="stderr"
      >
        {{ job.stderr }}
        <p ref="stderrBottom" />
      </div>
      <h4>STDOUT</h4>
      <div
        ref="stdout"
        class="stdout"
      >
        {{ job.stdout }}
        <p ref="stdoutBottom" />
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
import { ref } from 'vue'
import ApiService from '../services/ApiService.js'
import JobConfiguration from '../components/JobConfiguration.vue'

export default {
  components: {
    JobConfiguration,
  },
  props: {
    id: {
      type: String,
      required: true
    }
  },
  setup() {
    const stderr = ref()
    const stdout = ref()
    const stderrBottom = ref()
    const stdoutBottom = ref()
    return { stderr, stdout, stderrBottom, stdoutBottom }
  },
  data() {
    return {
      job: null,
    }
  },
  computed: {
    download() {
      return 'http://localhost:8000/download/' + this.job.output
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
      } catch (err) {
        console.log(err)
      }
      try {
        await this.$nextTick(() => {
          this.stderr.scrollTo({
            top: this.stderrBottom.offsetTop,
            behavior: 'smooth',
          })
          this.stdout.scrollTo({
            top: this.stdoutBottom.offsetTop,
            behavior: 'smooth',
          })
        })
      } catch (err) {
        console.log(err)
      }
      if (this.job.status == 'Running') {
        setTimeout(this.updateStatus, 1000)
      }
    }
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