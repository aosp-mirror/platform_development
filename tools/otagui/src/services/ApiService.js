import axios from 'axios'

const baseURL = process.env.NODE_ENV === 'production' ? '' : 'http://localhost:5000';

console.log(`Build mode: ${process.env.NODE_ENV}, API base url ${baseURL}`);

const apiClient = axios.create({
  baseURL,
  withCredentials: false,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json'
  }
});

export default {
  getDownloadURLForJob(job) {
    return `${baseURL}/download/${job.output}`;
  },
  getJobs() {
    return apiClient.get("/check")
  },
  getJobById(id) {
    return apiClient.get("/check/" + id)
  },
  async getBuildList() {
    let resp = await apiClient.get("/file");
    return resp.data || [];
  },
  async reconstructBuildList() {
    let resp = await apiClient.get("/reconstruct_build_list");
    return resp.data;
  },
  uploadTarget(file, onUploadProgress) {
    let formData = new FormData()
    formData.append('file', file)
    return apiClient.post("/file/" + file.name,
      formData,
      {
        onUploadProgress
      })
  },
  async postInput(input, id) {
    try {
      let resp = await apiClient.post(
        '/run/' + id, JSON.stringify(input));
      return resp.data;
    } catch (error) {
      if (error.response.data) {
        return error.response.data;
      } else {
        throw error;
      }
    }
  }
}