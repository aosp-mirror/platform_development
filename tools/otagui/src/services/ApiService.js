import axios from 'axios'

const baseURL = process.env.NODE_ENV === 'production' ? '' : 'http://localhost:8000';

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
  getFileList(path) {
    return apiClient.get("/file" + path)
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
      const response = await apiClient.post(
        '/run/' + id, input)
      return response
    } catch (err) {
      console.log('err:', err)
      return
    }
  }
}
