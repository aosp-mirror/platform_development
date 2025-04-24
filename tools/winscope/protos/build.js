/*
 * Copyright (C) 2023 The Android Open Source Project
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
const {exec} = require('child_process');

const ANDROID_BUILD_TOP = __dirname + '/../../../../';
const WINSCOPE_TOP = __dirname + '/..';
const PERFETTO_TOP = ANDROID_BUILD_TOP + '/external/perfetto';
const OUT_TOP = __dirname + '/../deps_build/protos';

build();

async function build() {
    await runCommand(`rm -rf ${OUT_TOP}`);

    const promises = [
        // IME udc
        buildProtos([
            '../../../../frameworks/base/core/proto/android/view/inputmethod/inputmethodeditortrace.proto'
        ], 'ime/udc'),

        // ProtoLog udc
        buildProtos([
            'protolog/udc/protolog.proto'
        ], 'protolog/udc'),

        // SurfaceFlinger udc
        buildProtos([
            'surfaceflinger/udc/layerstrace.proto',
        ], 'surfaceflinger/udc'),

        // Transactions udc
        buildProtos([
            'surfaceflinger/udc/transactions.proto',
        ], 'transactions/udc'),

        // Transitions udc
        buildProtos([
            'transitions/udc/windowmanagertransitiontrace.proto',
            'transitions/udc/wm_shell_transition_trace.proto'
        ], 'transitions/udc'),

        // ViewCapture udc
        buildProtos([
            '../../../../frameworks/libs/systemui/viewcapturelib/src/com/android/app/viewcapture/proto/view_capture.proto'
        ], 'viewcapture/udc'),

        // WindowManager udc
        buildProtos([
            '../../../../frameworks/base/core/proto/android/server/windowmanagertrace.proto',
        ], 'windowmanager/udc'),

        // Test proto fields
        buildProtos([
            'test/fake_proto_test.proto',
        ], 'test/fake_proto'),

        // Test intdef translation
        buildProtos([
            'test/intdef_translation_test.proto',
        ], 'test/intdef_translation'),

        // Perfetto trace
        buildProtos([
            '../../../../external/perfetto/protos/perfetto/trace/trace.proto',
            '../../../../external/perfetto/protos/perfetto/trace/android/winscope_extensions_impl.proto'
        ], 'perfetto/trace'),
    ];

    await Promise.all(promises);
}

async function buildProtos(protoPaths, outSubdir) {
    const outDir = OUT_TOP + '/' + outSubdir;
    const protoFullPaths = protoPaths.map((path) => __dirname + '/' + path);
    const rootName = outSubdir.replaceAll('/', '_');

    const commandBuildJson = [
        'npx',
        'pbjs',
        //TODO(b/318480413): for perfetto traces use '--force-bigint' as soon as available,
        // i.e. when this PR is merged https://github.com/protobufjs/protobuf.js/pull/1557
        '--force-long',
        '--target json-module',
        '--wrap es6',
        `--out ${outDir}/json.js`,
        `--root ${rootName}`,
        `--path ${PERFETTO_TOP}`,
        `--path ${WINSCOPE_TOP}`,
        `--path ${ANDROID_BUILD_TOP}`,
        protoFullPaths.join(' ')
    ].join(' ');

    const commandBuildJs = [
        'npx',
        'pbjs',
        '--force-long',
        '--target static-module',
        `--root ${outSubdir.replace('/', '')}`,
        `--out ${outDir}/static.js`,
        `--path ${PERFETTO_TOP}`,
        `--path ${WINSCOPE_TOP}`,
        `--path ${ANDROID_BUILD_TOP}`,
        protoFullPaths.join(' '),
    ].join(' ');

    const commandBuildTs = [
        'npx',
        'pbts',
        `--out ${outDir}/static.d.ts`,
        `${outDir}/static.js`
    ].join(' ');

    await runCommand(`mkdir -p ${outDir}`)
    await runCommand(commandBuildJson);
    await runCommand(commandBuildJs);
    await runCommand(commandBuildTs);
}

function runCommand(command) {
    return new Promise((resolve, reject) => {
        exec(command, (err, stdout, stderr) => {
            if (err) {
                const errorMessage =
                    "Failed to execute command" +
                    `\n\ncommand: ${command}` +
                    `\n\nstdout: ${stdout}` +
                    `\n\nstderr: ${stderr}`;
                reject(errorMessage);
            }
            resolve();
        });
    });
}
