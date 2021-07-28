import axios from 'axios'

const apiClient = axios.create({
  baseURL: 'http://localhost:8000',
  withCredentials: false,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json'
  }
})

export default {
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
