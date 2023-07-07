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
const glob = require('glob');

const config = require('./webpack.config.common');

config['mode'] = 'development';

const allTestFiles = [...glob.sync('./src/**/*_test.js'), ...glob.sync('./src/**/*_test.ts')];
const unitTestFiles = allTestFiles
  .filter((file) => !file.match('.*_component_test\\.(js|ts)$'))
  .filter((file) => !file.match('.*e2e.*'));
config['entry'] = {
  tests: unitTestFiles,
};

config['output'] = {
  path: path.resolve(__dirname, 'dist/unit_test'),
  filename: 'bundle.js',
};

config['target'] = 'node';
config['node'] = {
  __dirname: false,
};

module.exports = config;
