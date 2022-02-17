<template>
  <form @submit.prevent="sendForm">
    <FileList
      v-model="incrementalSources"
      :disabled="!checkIncremental"
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
    }
  },
  computed: {
    checkIncremental() {
      return this.$store.state.otaConfig.isIncremental
    },
    incrementalSources: {
      get() {
        return this.$store.state.sourceBuilds
      },
      set(target) {
        this.$store.commit('SET_SOURCES', target)
      }
    },
    targetBuilds: {
      get() {
        return this.$store.state.targetBuilds
      },
      set(target) {
        this.$store.commit('SET_TARGETS', target)
      }
    }
  },
  created() {
    this.$emit('update:handler', this.addIncrementalSources, this.addTargetBuilds)
  },
  methods: {
    /**
     * Send the configuration to the backend.
     */
    async sendForm() {
      try {
        let response_data = await this.$store.state.otaConfig.sendForms(
          this.targetBuilds, this.incrementalSources);
        let response_messages = response_data.map(d => d.msg);
        alert(response_messages.join('\n'))
        this.$store.state.otaConfig.reset()
        this.$store.commit('SET_TARGETS', [])
        this.$store.commit('SET_SOURCES', [])
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