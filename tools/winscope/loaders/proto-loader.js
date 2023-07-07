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
const fs = require('fs');
const protobuf = require('protobufjs');
const loaderUtils = require('loader-utils');

module.exports = function(source) {
  const webpackContext = this;
  const root = new protobuf.Root();
  const paths = loaderUtils.getOptions(this)['paths'] || [];

  root.resolvePath = function resolvePath(origin, target) {
    const normOrigin = protobuf.util.path.normalize(origin);
    const normTarget = protobuf.util.path.normalize(target);

    let candidates = [
      protobuf.util.path.resolve(normOrigin, normTarget, true)
    ];
    candidates = candidates.concat(
      paths.map(path => protobuf.util.path.resolve(path + "/", target))
    );

    for (const path of candidates) {
      if (fs.existsSync(path)) {
        webpackContext.addDependency(path);
        return path;
      }
    }

    throw Error(`Failed to resolve path: origin=${origin}, target=${target}, candidates=${candidates}`);
  };

  root.loadSync(webpackContext.resourcePath).resolveAll();

  const result = JSON.stringify(root, null, 2);

  return `module.exports = ${result}`;
};
