<template>
  <form @submit.prevent="sendForm">
    <FileSelect
      v-if="otaConfig.isIncremental"
      v-model="incrementalSource"
      label="Select the source file"
      :options="targetDetails"
    />
    <FileSelect
      v-model="targetBuild"
      label="Select a target file"
      :options="targetDetails"
    />
    <OTAOptions
      :targetDetails="targetDetails"
      :targetBuilds="[targetBuild]"
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
import FileSelect from '@/components/FileSelect.vue'
import { OTAConfiguration } from '@/services/JobSubmission.js'

export default {
  components: {
    OTAOptions,
    FileSelect,
  },
  props: {
    targetDetails: {
      type: Array,
      default: () => [],
    }
  },
  data() {
    return {
      incrementalSource: '',
      targetBuild: '',
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
    this.$emit('update:handler', this.setIncrementalSource, this.setTargetBuild)
  },
  methods: {
    /**
     * Send the configuration to the backend.
     */
    async sendForm() {
      try {
        let response_message = await this.otaConfig.sendForm(
          this.targetBuild, this.incrementalSource)
        alert(response_message)
        this.otaConfig.reset()
      } catch (err) {
        alert(
          'Job cannot be started properly for the following reasons: ' + err
        )
      }
    },
    setIncrementalSource (build) {
      this.incrementalSource = build
    },
    setTargetBuild (build) {
      this.targetBuild = build
    }
  },
}
</script>