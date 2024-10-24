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

const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlWebpackInlineSourcePlugin = require('html-webpack-inline-source-plugin');

module.exports = {
  resolve: {
    extensions: ['.ts', '.js', '.css'],
    modules: [
      __dirname + '/../../../node_modules',
      __dirname + '/../../../src',
      __dirname,
    ],
  },

  module: {
    rules: [
      {
        test: /\.ts$/,
        use: ['ts-loader', 'angular2-template-loader'],
      },
      {
        test: /\.html$/,
        use: ['html-loader'],
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader'],
      },
      {
        test: /\.s[ac]ss$/i,
        use: ['style-loader', 'css-loader', 'sass-loader'],
      },
    ],
  },

  mode: 'development',

  entry: {
    polyfills: __dirname + '/polyfills.ts',
    app: __dirname + '/main.ts',
  },

  output: {
    path: __dirname + '/../../../dist/remote_tool_mock',
    publicPath: '/',
    filename: 'js/[name].[hash].js',
    chunkFilename: 'js/[name].[id].[hash].chunk.js',
  },

  devtool: 'source-map',

  plugins: [
    new HtmlWebpackPlugin({
      template: __dirname + '/index.html',
      inject: 'body',
      inlineSource: '.(css|js)$',
    }),
    new HtmlWebpackInlineSourcePlugin(HtmlWebpackPlugin),
  ],
};
