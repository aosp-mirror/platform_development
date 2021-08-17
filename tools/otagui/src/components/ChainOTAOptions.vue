<template>
  <form @submit.prevent="sendForm">
    <FileList
      v-model="targetBuilds"
      label="Target files"
      :movable="true"
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
      targetBuilds: [],
      otaConfig: new OTAConfiguration(),
    }
  },
  created() {
    this.$emit('update:isIncremental', false)
    this.$emit('update:handler', this.addIncrementalSources, this.addTargetBuilds)
  },
  methods: {
    /**
     * Send the configuration to the backend.
     */
    async sendForm() {
      if (this.targetBuilds.length<2) {
        alert(
          'At least two OTA packeges has to be given!'
        )
        return
      }
      try {
        let response_messages = await this.otaConfig
          .sendChainForms(this.targetBuilds)
        alert(response_messages.join('\n'))
        this.otaConfig.reset()
      } catch (err) {
        alert(
          'Job cannot be started properly for the following reasons: ' + err
        )
      }
    },
    addIncrementalSources (build) {},
    addTargetBuilds (build) {
      if (!this.targetBuilds.includes(build)) {
        this.targetBuilds.push(build)
      }
    }
  },
}
</script>