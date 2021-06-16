<template>
  <div>
    <form @submit.prevent="sendForm">
      <UploadFile @file-uploaded="fetchTargetList" />
      <br>
      <FileSelect
        v-if="input.isIncremental"
        v-model="input.incremental"
        label="Select the source file"
        :options="targetDetails"
      />
      <FileSelect
        v-model="input.target"
        label="Select the target file"
        :options="targetDetails"
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
        /> &emsp;
        <BaseCheckbox
          v-model="input.isIncremental"
          :label="'Incremental'"
        />
      </div>
      <div>
        <BaseCheckbox
          v-model="input.isPartial"
          :label="'Partial'"
        />
        <PartialCheckbox
          v-if="input.isPartial"
          v-model="partitionInclude"
          :labels="updatePartitions"
        />
        <div v-if="input.isPartial">
          Partial list: {{ partitionList }}
        </div>
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
  <div>
    <ul>
      <h4>Build Library</h4>
      <strong>
        Careful: Use a same filename will overwrite the original build.
      </strong>
      <br>
      <button @click="updateBuildLib">
        Refresh the build Library (use with cautions)
      </button>
      <li
        v-for="targetDetail in targetDetails"
        :key="targetDetail.file_name"
      >
        <div>
          <h5>Build File Name: {{ targetDetail.file_name }}</h5>
          Uploaded time: {{ formDate(targetDetail.time) }}
          <br>
          Build ID: {{ targetDetail.build_id }}
          <br>
          Build Version: {{ targetDetail.build_version }}
          <br>
          Build Flavor: {{ targetDetail.build_flavor }}
          <br>
          <button
            :disabled="!input.isIncremental"
            @click="selectIncremental(targetDetail.path)"
          >
            Select as Incremental File
          </button>
          &emsp;
          <button @click="selectTarget(targetDetail.path)">
            Select as Target File
          </button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script>
import BaseInput from '@/components/BaseInput.vue'
import BaseCheckbox from '@/components/BaseCheckbox.vue'
import FileSelect from '@/components/FileSelect.vue'
import ApiService from '../services/ApiService.js'
import UploadFile from '@/components/UploadFile.vue'
import PartialCheckbox from '@/components/PartialCheckbox.vue'
import { uuid } from 'vue-uuid'

export default {
  components: {
    BaseInput,
    BaseCheckbox,
    UploadFile,
    FileSelect,
    PartialCheckbox,
  },
  data() {
    return {
      id: 0,
      input: {},
      inputs: [],
      response_message: '',
      targetDetails: [],
      partitionInclude: new Map(),
    }
  },
  computed: {
    updatePartitions() {
      let target = this.targetDetails.filter(
        (d) => d.path === this.input.target
      )
      return target[0].partitions
    },
    partitionList() {
      let list = ''
      for (let [key, value] of this.partitionInclude) {
        if (value) {
          list += key + ' '
        }
      }
      return list
    },
  },
  watch: {
    partitionList: {
      handler: function () {
        this.input.partial = this.partitionList
      },
    },
  },
  created() {
    this.resetInput()
    this.fetchTargetList()
    this.updateUUID()
  },
  methods: {
    resetInput() {
      this.input = {
        verbose: false,
        target: '',
        output: 'output/',
        incremental: '',
        isIncremental: false,
        partial: '',
        isPartial: false,
        extra: '',
      }
    },
    async sendForm(e) {
      try {
        let response = await ApiService.postInput(this.input, this.id)
        this.response_message = response.data
        alert(this.response_message)
      } catch (err) {
        alert('Job cannot be started properly, please check.')
        console.log(err)
      }
      this.resetInput()
      this.updateUUID()
    },
    async fetchTargetList() {
      try {
        let response = await ApiService.getFileList('')
        this.targetDetails = response.data
      } catch (err) {
        console.log('Fetch Error', err)
      }
    },
    updateUUID() {
      this.id = uuid.v1()
      this.input.output += String(this.id) + '.zip'
    },
    formDate(unixTime) {
      let formTime = new Date(unixTime * 1000)
      let date =
        formTime.getFullYear() +
        '-' +
        (formTime.getMonth() + 1) +
        '-' +
        formTime.getDate()
      let time =
        formTime.getHours() +
        ':' +
        formTime.getMinutes() +
        ':' +
        formTime.getSeconds()
      return date + ' ' + time
    },
    selectTarget(path) {
      this.input.target = path
    },
    selectIncremental(path) {
      this.input.incremental = path
    },
    async updateBuildLib() {
      try {
        let response = await ApiService.getFileList('/target')
        this.targetDetails = response.data
      } catch (err) {
        console.log('Fetch Error', err)
      }
    },
  },
}
</script>

<style scoped>
</style>