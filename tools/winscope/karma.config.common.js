/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const path = require('path');
const webpackConfig = require('./webpack.config.common');
delete webpackConfig.entry;
delete webpackConfig.output;

module.exports = (config) => {
  config.set({
    frameworks: ['jasmine', 'webpack'],
    plugins: [
      'karma-webpack',
      'karma-chrome-launcher',
      'karma-jasmine',
      'karma-sourcemap-loader',
    ],
    files: [
      {pattern: 'src/main_unit_test.ts', watched: false},
      {pattern: 'src/test/fixtures/**/*', included: false, served: true},
      {
        pattern: 'deps_build/trace_processor/to_be_served/engine_bundle.js',
        included: false,
        served: true,
      },
      {
        pattern: 'deps_build/trace_processor/to_be_served/trace_processor.wasm',
        included: false,
        served: true,
      },
    ],
    reporters: ['progress'],
    preprocessors: {
      'src/main_unit_test.ts': ['webpack', 'sourcemap'],
    },
    verbose: true, // output config used by istanbul for debugging
    webpack: webpackConfig,
  });
};
