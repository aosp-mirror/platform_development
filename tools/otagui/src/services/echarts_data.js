// This function will be used later for generating a pie chart through eCharts
export class EchartsData {
  constructor(statisticData) {
    this.statisticData = statisticData
  }

  listData() {
    let table = ''
    for (let [key, value] of this.statisticData) {
      table += key + ' : ' + value.toString() + ' Blocks' + '\n'
    }
    return table
  }
}