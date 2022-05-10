export default{
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
  }
}