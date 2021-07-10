export class EchartsData {
  /**
   * Given a set of [key, value] pairs and title, create an object for further
   * usage in Vue-Echarts.
   * @param {Map} statisticData
   * @param {String} title
   * @param {String} unit
   * @param {Number} maximumEntries
   */
  constructor(statisticData, title, unit, maximumEntries = 15) {
    this.statisticData = statisticData
    this.title = title
    this.unit = unit
    this.maximumEntries = maximumEntries
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
    if (this.statisticData.size > this.maximumEntries) {
      this.statisticData = trimMap(this.statisticData, this.maximumEntries)
    }
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

/**
 * When there are too many entries in the map, the pie chart can be very
 * crowded. This function will return the entries that have high values.
 * Specifically, the top <maximumEntries> will be stored and the others
 * will be added into an entry called 'other'.
 * @param {Map} map
 * @param {Number} maximumEntries
 * @return {Map}
 */
function trimMap(map, maximumEntries) {
  if (map.size <= maximumEntries) return map
  let /** Map */ new_map = new Map()
  for (let i=0; i<maximumEntries; i++) {
    let /** Number */ curr = 0
    let /** String */ currKey = ''
    for (let [key, value] of map) {
      if (!new_map.get(key)) {
        if (value > curr) {
          curr = value
          currKey = key
        }
      }
    }
    new_map.set(currKey, curr)
  }
  let /** Number */ restTotal = 0
  for (let [key, value] of map) {
    if (!new_map.get(key)) {
      restTotal += value
    }
  }
  new_map.set('other', restTotal)
  return new_map
}