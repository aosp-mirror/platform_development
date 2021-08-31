<template>
  <div v-if="job">
    <h3>Job. {{ job.id }} {{ job.status }}</h3>
    <JobConfiguration
      :job="job"
      :build-detail="true"
    />
    <router-link :to="{name: 'Create'}">
      <v-btn
        block
        @click="updateConfig()"
      >
        Reuse this configuration.
      </v-btn>
    </router-link>
    <v-divider class="my-5" />
    <div>
      <h3>STDERR</h3>
      <pre
        ref="stderr"
        class="stderr"
      >
        {{ job.stderr }}
        <p ref="stderrBottom" />
      </pre>
      <h3>STDOUT</h3>
      <pre
        ref="stdout"
        class="stdout"
      >
        {{ job.stdout }}
        <p ref="stdoutBottom" />
      </pre>
    </div>
    <v-divider class="my-5" />
    <div class="download">
      <a
        v-if="job.status == 'Finished'"
        :href="download"
      > Download </a>
    </div>
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
      pending_task: null,
    }
  },
  computed: {
    download() {
      return ApiService.getDownloadURLForJob(this.job);
    },
  },
  created() {
    this.updateStatus()
  },
  unmounted() {
    if (this.pending_task) {
      clearTimeout(this.pending_task);
      this.pending_task = null;
    }
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
        this.pending_task = setTimeout(this.updateStatus, 1000)
      }
    },
    updateConfig() {
      this.$store.commit("REUSE_CONFIG", this.job)
    }
  },
}
</script>

<style scoped>
.stderr,
.stdout {
  overflow: scroll;
  height: 160px;
}

.download {
  margin: auto;
  text-align: center;
  font-size: 160%;
}
</style>