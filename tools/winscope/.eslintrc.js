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

module.exports = {
  extends: ['eslint-config-prettier', 'eslint:recommended'],
  plugins: ['eslint-plugin-prettier', '@typescript-eslint'],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  env: {
    es2022: true,
    node: true,
    browser: true,
    webextensions: true,
    jasmine: true,
    protractor: true,
  },
  ignorePatterns: [
    // Perfetto trace processor sources. Either auto-generated (we want to keep them untouched)
    // or copied from external/perfetto (we want to touch them as little as possible to allow
    // future upgrading, diffing, conflicts merging, ...)
    'src/trace_processor/',
  ],
  rules: {
    'no-unused-vars': 'off', // not very robust rule

    // Partially taken from https://github.com/google/eslint-config-google
    // Omitted layout & formatting rules because that's handled by prettier
    'no-var': 'error',
    'prefer-const': ['error', {destructuring: 'all'}],
    'prefer-rest-params': 'error',
    'prefer-spread': 'error',
    'no-restricted-imports': [
      'error',
      {
        'patterns': ['..*'],
      },
    ],
  },
  globals: {
    // Specify NodeJS global as temporary workaround for eslint bug:
    // https://stackoverflow.com/questions/64089216/after-upgrade-eslint-says-nodejs-is-undefined
    // https://github.com/Chatie/eslint-config/issues/45
    NodeJS: true,
  },
};
