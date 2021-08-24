const defaults = require('jest-config')

module.exports = {
  preset: '@vue/cli-plugin-unit-jest',
  transform: {
    '^.+\\.vue$': 'vue-jest'
  },
  globals: {
    ...defaults.globals,
    crypto: require('crypto'),
    TextEncoder: require('util').TextEncoder,
    TextDecoder: require('util').TextDecoder,
  }
}