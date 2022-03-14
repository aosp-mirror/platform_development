/*
 * Copyright 2020, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

const path = require('path');
const fs = require('fs');

const { VueLoaderPlugin } = require("vue-loader")
const HtmlWebpackPlugin = require('html-webpack-plugin');
const KotlinWebpackPlugin = require('@jetbrains/kotlin-webpack-plugin');
const HtmlWebpackInlineSourcePlugin =
  require('html-webpack-inline-source-plugin');

const isDev = process.env.NODE_ENV === 'development';


function getWaylandSafePath() {
  const waylandPath =
    path.resolve(__dirname, '../../../vendor/google_arc/libs/wayland_service');

  if (fs.existsSync(waylandPath)) {
    return waylandPath;
  }

  return path.resolve(__dirname, 'src/stubs');
}

const webpackConfig = {
  entry: {
    polyfill: '@babel/polyfill',
    main: './src/main.js',
  },
  externals: {
    _: 'lodash',
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.js', '.vue'],
    alias: {
      'vue$': isDev ? 'vue/dist/vue.runtime.js' : 'vue/dist/vue.runtime.min.js',
      '@': path.resolve(__dirname, 'src'),
      'WaylandSafePath': getWaylandSafePath(),
    },
    modules: [
      'node_modules',
      'kotlin_build',
      path.resolve(__dirname, '../../..'),
    ],
  },
  resolveLoader: {
    modules: [
      'node_modules',
      path.resolve(__dirname, 'loaders'),
    ],
  },
  module: {
    rules: [
      {
        test: /\.vue$/,
        loader: 'vue-loader',
        include: path.resolve(__dirname, './src'),
        exclude: /node_modules/,
      },
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        include: path.resolve(__dirname, './src'),
        exclude: /node_modules/,
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        include: path.resolve(__dirname, './src'),
        exclude: /node_modules/,
      },
      {
        test: /\.css$/,
        use: [
          'vue-style-loader',
          {loader: 'css-loader', options: {sourceMap: isDev}},
        ],
        include: path.resolve(__dirname, './src'),
        exclude: /node_modules/,
      },
      {
        test: /\.proto$/,
        loader: 'proto-loader',
        options: {
          paths: [
            path.resolve(__dirname, '../../..'),
            path.resolve(__dirname, '../../../external/protobuf/src'),
          ],
        },
      },
      {
        test: /\.(png|jpg|gif|svg)$/,
        loader: 'file-loader',
        options: {
          name: '[name].[ext]?[hash]',
        },
      },
      {
        test: /\.(ttf|otf|eot|woff|woff2)$/,
        use: {
          loader: 'file-loader',
          options: {
            name: 'fonts/[name].[ext]',
          },
        },
      },
    ],
  },
  plugins: [
    new VueLoaderPlugin(),
    new HtmlWebpackPlugin({
      inlineSource: isDev ? false : '.(js|css)',
      template: 'src/index_template.html',
    }),
    new HtmlWebpackInlineSourcePlugin(HtmlWebpackPlugin),
    new KotlinWebpackPlugin({
      src: [
        path.join(__dirname, '../../../platform_testing/libraries/flicker/' +
          'src/com/android/server/wm/traces/common/'),
      ],
      output: 'kotlin_build',
      moduleName: 'flicker',
      librariesAutoLookup: true,
      sourceMaps: true,
      sourceMapEmbedSources: 'always',
      verbose: true,
      optimize: true,
    }),
  ],
};

module.exports = webpackConfig;
