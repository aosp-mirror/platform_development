module.exports = {
  publicPath: process.env.NODE_ENV === 'production'
    ? '/analyseOTA/'
    : '/',

  transpileDependencies: [
    'vuetify'
  ]
}
