<template>
  <ul>
    <h3>Build Library</h3>
    <UploadFile @file-uploaded="fetchTargetList" />
    <BuildTable
      v-if="targetDetails && targetDetails.length>0"
      :builds="targetDetails"
    />
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
          :disabled="!isIncremental"
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
</template>

<script>
import UploadFile from '@/components/UploadFile.vue'
import BuildTable from '@/components/BuildTable.vue'
import ApiService from '../services/ApiService.js'
import FormDate from '@/services/FormDate.js'

export default {
  components: {
    UploadFile,
    BuildTable
  },
  props: {
    isIncremental: {
      type: Boolean,
      required: true
    }
  },
  data() {
    return {
      incrementalSource: '',
      targetBuild: '',
      targetDetails: []
    }
  },
  mounted () {
    this.fetchTargetList()
  },
  methods: {
    /**
     * Fetch the build list from backend.
     */
    async fetchTargetList() {
      try {
        this.targetDetails = await ApiService.getBuildList()
        this.$emit('update:targetDetails', this.targetDetails)
      } catch (err) {
        alert(
          "Cannot fetch Android Builds list from the backend, for the following reasons:"
          + err
        )
      }
    },
    /**
     * Let the backend reload the builds from disk. This will overwrite the
     * original upload time.
     */
    async updateBuildLib() {
      try {
        this.targetDetails = await ApiService.reconstructBuildList();
        this.$emit('update:targetDetails', this.targetDetails);
      } catch (err) {
        alert(
          "Cannot fetch Android Builds list from the backend, for the following reasons: "
          + err
        )
      }
    },
    selectTarget(path) {
      this.targetBuild = path
      this.$emit('update:targetBuild', path)
    },
    selectIncremental(path) {
      this.incrementalSource = path
      this.$emit('update:incrementalSource', path)
    },
    formDate(unixTime) {
      return FormDate.formDate(unixTime)
    }
  },
}
</script>

<style scoped>
ul > li {
  list-style: none
}
</style>