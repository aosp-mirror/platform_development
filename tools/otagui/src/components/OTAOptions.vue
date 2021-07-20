<template>
  <form @submit.prevent="sendForm">
    <FileSelect
      v-if="input.isIncremental"
      v-model="input.incremental"
      label="Select the source file"
      :options="targetDetails"
    />
    <FileSelect
      v-model="input.target"
      label="Select a target file"
      :options="targetDetails"
    />
    <v-row>
      <v-col
        v-for="flag in basicFlags"
        :key="flag.key"
        cols="12"
        md="4"
      >
        <BaseCheckbox
          v-model="input[flag.key]"
          :label="flag.label"
        />
      </v-col>
    </v-row>
    <div v-if="input.isPartial">
      <v-divider />
      <h3>Select Partitions</h3>
      <PartialCheckbox
        v-model="input.partial"
        :labels="updatablePartitions"
      />
      <v-divider />
    </div>
    <v-btn
      block
      @click="moreOptions = !moreOptions"
    >
      More Options
    </v-btn>
    <v-row v-if="moreOptions">
      <v-col
        v-for="flag in extraFlags"
        :key="flag.key"
        cols="12"
        md="4"
      >
        <BaseCheckbox
          v-model="input[flag.key]"
          :label="flag.label"
        />
      </v-col>
    </v-row>
    <v-divider class="my-5" />
    <BaseInput
      v-model="input.extra"
      :label="'Extra Configurations'"
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
import BaseInput from '@/components/BaseInput.vue'
import BaseCheckbox from '@/components/BaseCheckbox.vue'
import FileSelect from '@/components/FileSelect.vue'
import PartialCheckbox from '@/components/PartialCheckbox.vue'
import { OTAConfiguration, OTABasicFlags, OTAExtraFlags } from '@/services/JobSubmission.js'

export default {
  components: {
    BaseInput,
    BaseCheckbox,
    FileSelect,
    PartialCheckbox,
  },
  props: {
    targetDetails: {
      type: Array,
      default: () => [],
    },
    incrementalSource: {
      type: String,
      default: '',
    },
    targetBuild: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      input: new OTAConfiguration(),
      moreOptions: false,
      basicFlags: OTABasicFlags,
      extraFlags: OTAExtraFlags
    }
  },
  computed: {
    /**
     * Return the partition list of the selected target build.
     * @return Array<String>
     */
    updatablePartitions() {
      if (!this.input.target) return []
      let target = this.targetDetails.filter(
        (d) => d.path === this.input.target
      )
      return target[0].partitions
    },
    checkIncremental() {
      return this.input.isIncremental
    },
  },
  watch: {
    incrementalSource: {
      handler: function () {
        this.input.isIncremental = true
        this.input.incremental = this.incrementalSource
      },
    },
    targetBuild: {
      handler: function () {
        this.input.target = this.targetBuild
      },
    },
    checkIncremental: {
      handler: function () {
        this.$emit('update:isIncremental', this.checkIncremental)
      },
    },
  },
  methods: {
    /**
     * Send the configuration to the backend.
     */
    async sendForm() {
      try {
        let response_message = await this.input.sendForm()
        alert(response_message)
      } catch (err) {
        alert(
          'Job cannot be started properly for the following reasons: ' + err
        )
      }
      this.input = new OTAInput()
    },
  },
}
</script>