<template>
  <form @submit.prevent="sendForm">
    <FileList
      v-model="incrementalSources"
      :disabled="!otaConfig.isIncremental"
      label="Source files"
    />
    <v-divider />
    <FileList
      v-model="targetBuilds"
      label="Target files"
    />
    <v-divider />
    <OTAOptions
      :targetDetails="targetDetails"
      :targetBuilds="targetBuilds"
      @update:otaConfig="otaConfig=$event"
    />
    <v-divider class="my-5" />
    <v-btn
      block
      type="submit"
    >
      Submit
    </v-btn>
  </form>
</template>

<script>
import OTAOptions from '@/components/OTAOptions.vue'
import FileList from '@/components/FileList.vue'
import { OTAConfiguration } from '@/services/JobSubmission.js'

export default {
  components: {
    OTAOptions,
    FileList,
  },
  props: {
    targetDetails: {
      type: Array,
      default: () => [],
    }
  },
  data() {
    return {
      incrementalSources: [],
      targetBuilds: [],
      otaConfig: new OTAConfiguration(),
    }
  },
  computed: {
    checkIncremental() {
      return this.otaConfig.isIncremental
    },
  },
  watch: {
    checkIncremental: {
      handler: function () {
        this.$emit('update:isIncremental', this.checkIncremental)
      },
    },
  },
  created() {
    this.$emit('update:isIncremental', this.checkIncremental)
    this.$emit('update:handler', this.addIncrementalSources, this.addTargetBuilds)
  },
  methods: {
    /**
     * Send the configuration to the backend.
     */
    async sendForm() {
      try {
        let response_messages = await this.otaConfig.sendForms(
          this.targetBuilds, this.incrementalSources)
        alert(response_messages.join('\n'))
        this.otaConfig.reset()
      } catch (err) {
        alert(
          'Job cannot be started properly for the following reasons: ' + err
        )
      }
    },
    addIncrementalSources (build) {
      if (!this.incrementalSources.includes(build)) {
        this.incrementalSources.push(build)
      }
    },
    addTargetBuilds (build) {
      if (!this.targetBuilds.includes(build)) {
        this.targetBuilds.push(build)
      }
    }
  },
}
</script>