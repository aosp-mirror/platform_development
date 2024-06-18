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
const {merge} = require('webpack-merge');
const configCommon = require('./webpack.config.common');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlWebpackInlineSourcePlugin = require('html-webpack-inline-source-plugin');

const configProd = {
  mode: 'production',
  entry: {
    polyfills: './src/polyfills.ts',
    styles: ['./src/material-theme.scss', './src/styles.css'],
    app: './src/main_prod.ts',
  },
  output: {
    path: path.resolve(__dirname, 'dist/prod'),
    publicPath: '/',
    filename: 'js/[name].[hash].js',
    chunkFilename: 'js/[name].[id].[hash].chunk.js',
  },
  optimization: {
    runtimeChunk: 'single',
    splitChunks: {
      chunks: 'all',
      maxInitialRequests: Infinity,
      minSize: 0,
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name(module) {
            const packageName = module.context.match(
              /[\\/]node_modules[\\/](.*?)([\\/]|$)/,
            )[1];
            return `npm.${packageName.replace('@', '')}`;
          },
        },
        styles: {
          test: /\.css$/,
          name: 'styles',
          chunks: 'all',
          enforce: true,
        },
      },
    },
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: 'src/index.html',
      inject: 'body',
      inlineSource: '.(css|js)$',
    }),
    new HtmlWebpackInlineSourcePlugin(HtmlWebpackPlugin),
  ],
};

module.exports = merge(configCommon, configProd);
