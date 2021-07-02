export class EchartsData {
  /**
   * Given a set of [key, value] pairs and title, create an object for further
   * usage in Vue-Echarts.
   * @param {Map} statisticData
   * @param {String} title
   * @param {String} unit
   */
  constructor(statisticData, title, unit) {
    this.statisticData = statisticData
    this.title = title
    this.unit = unit
  }

  /**
   * Convert the raw data into a string.
   * @return {String} A list of [key, value].
   */
  listData() {
    let /** String */ table = ''
    for (let [key, value] of this.statisticData) {
      table += key + ' : ' + value.toString() + ' Blocks' + '\n'
    }
    return table
  }

  /**
   * Generate necessary parameters (option) for vue-echarts.
   * Format of the parameters can be found here:
   * https://echarts.apache.org/en/option.html
   * @param {String} unit
   * @return {Object} an ECharts option object.
   */
  getEchartsOption() {
    let /** Object */ option = new Object()
    option.title = {
      text: this.title,
      left: "center"
    }
    option.tooltip = {
      trigger: "item",
      formatter: "{a} <br/>{b} : {c} " + this.unit + " ({d}%)"
    }
    option.legend = {
      orient: "horizontal",
      left: "top",
      top: "10%",
      data: Array.from(this.statisticData.keys())
    }
    option.series = [
      {
        name: this.title,
        type: "pie",
        radius: "55%",
        center: ["50%", "60%"],
        data: Array.from(this.statisticData).map((pair) => {
          return { value: pair[1], name: pair[0] }
        }),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: "rgba(0, 0, 0, 0.5)"
          }
        }
      }
    ]
    return option
  }
}