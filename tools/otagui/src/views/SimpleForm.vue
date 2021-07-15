<template>
  <v-row>
    <v-col cols="6">
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
            cols="4"
            align="center"
          >
            <BaseCheckbox
              v-model="input.verbose"
              :label="'Verbose'"
            />
          </v-col>
          <v-col
            cols="4"
            align="center"
          >
            <BaseCheckbox
              v-model="input.isIncremental"
              :label="'Incremental'"
            />
          </v-col>
          <v-col
            cols="4"
            align="center"
          >
            <BaseCheckbox
              v-model="input.isPartial"
              :label="'Partial'"
            />
          </v-col>
        </v-row>
        <div>
          <PartialCheckbox
            v-if="input.isPartial"
            v-model="partitionInclude"
            :labels="updatePartitions"
          />
        </div>
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
    </v-col>
    <v-divider vertical />
    <v-col cols="6">
      <ul>
        <h3>Build Library</h3>
        <UploadFile @file-uploaded="fetchTargetList" />
        <li
          v-for="targetDetail in targetDetails"
          :key="targetDetail.file_name"
        >
          <div>
            <h3> Build File Name: {{ targetDetail.file_name }} </h3>
            <strong> Uploaded time: </strong> {{ formDate(targetDetail.time) }}
            <br>
            <strong> Build ID: </strong> {{ targetDetail.build_id }}
            <br>
            <strong> Build Version: </strong> {{ targetDetail.build_version }}
            <br>
            <strong> Build Flavor: </strong> {{ targetDetail.build_flavor }}
            <br>
            <v-btn
              :disabled="!input.isIncremental"
              @click="selectIncremental(targetDetail.path)"
            >
              Select as Incremental File
            </v-btn>
            &emsp;
            <v-btn @click="selectTarget(targetDetail.path)">
              Select as Target File
            </v-btn>
          </div>
          <v-divider class="my-5" />
        </li>
        <v-btn @click="updateBuildLib">
          Refresh the build Library (use with cautions)
        </v-btn>
      </ul>
    </v-col>
  </v-row>
</template>

<script>
import BaseInput from '@/components/BaseInput.vue'
import BaseCheckbox from '@/components/BaseCheckbox.vue'
import FileSelect from '@/components/FileSelect.vue'
import ApiService from '../services/ApiService.js'
import UploadFile from '@/components/UploadFile.vue'
import PartialCheckbox from '@/components/PartialCheckbox.vue'
import FormDate from '../services/FormDate.js'
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
      if (!this.input.target) return []
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
      },
      this.partitionInclude = new Map()
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
      return FormDate.formDate(unixTime)
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
ul > li {
  list-style: none
}
</style>