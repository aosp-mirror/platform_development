<template>
  <div>
    <form @submit.prevent="sendForm">
      <UploadFile @file-uploaded="fetchTargetList" />
      <br>
      <BaseSelect
        v-if="input.incrementalStatus"
        v-model="input.incremental"
        label="Select the source file"
        :options="targetList"
      />
      <BaseSelect
        v-model="input.target"
        label="Select the target file"
        :options="targetList"
      />
      <button
        type="button"
        @click="fetchTargetList"
      >
        Update File List
      </button>
      <div>
        <BaseCheckbox
          v-model="input.verbose"
          :label="'Verbose'"
        />
        &emsp;
        <BaseCheckbox
          v-model="input.incrementalStatus"
          :label="'Incremental'"
        />
      </div>
      <br>
      <BaseInput
        v-model="input.extra"
        :label="'Extra Configurations'"
      />
      <br>
      <button type="submit">
        Submit
      </button>
    </form>
  </div>
</template>

<script>
import BaseInput from '@/components/BaseInput.vue'
import BaseCheckbox from '@/components/BaseCheckbox.vue'
import BaseSelect from '@/components/BaseSelect.vue'
import ApiService from '../services/ApiService.js'
import UploadFile from '@/components/UploadFile.vue'
import { uuid } from 'vue-uuid'

export default {
  components: {
    BaseInput,
    BaseCheckbox,
    UploadFile,
    BaseSelect,
  },
  data() {
    return {
      id: 0,
      input: {
        verbose: false,
        target: '',
        output: 'output/',
        incremental: '',
        incrementalStatus: false,
        extra: '',
      },
      inputs: [],
      response_message: '',
      targetList: [],
    }
  },
  computed: {
    updateOutput() {
      return 'output/' + String(this.id) + '.zip'
    },
  },
  created() {
    this.fetchTargetList()
    this.updateUUID()
  },
  methods: {
    sendForm(e) {
      // console.log(this.input)
      ApiService.postInput(this.input, this.id)
        .then((Response) => {
          this.response_message = Response.data
          alert(this.response_message)
        })
        .catch((err) => {
          this.response_message = 'Error! ' + err
        })
      this.input = {
        verbose: false,
        target: '',
        output: 'output/',
        incremental: '',
        incrementalStatus: false,
        extra: '',
      }
      this.updateUUID()
    },
    async fetchTargetList() {
      try {
        let response = await ApiService.getFileList('/target')
        this.targetList = response.data
      } catch (err) {
        console.log('Fetch Error', err)
      }
    },
    updateUUID() {
      this.id = uuid.v1()
      this.input.output += String(this.id) + '.zip'
    },
  },
}
</script>

<style scoped>
</style>