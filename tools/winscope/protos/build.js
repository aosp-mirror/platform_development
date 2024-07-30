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
        // IME
        buildProtos([
            '../../../../frameworks/base/core/proto/android/view/inputmethod/inputmethodeditortrace.proto'
        ], 'ime/udc'),
        buildProtos([
            'ime/latest/wrapper.proto',
        ], 'ime/latest'),

        // ProtoLog
        buildProtos([
            'protolog/udc/protolog.proto'
        ], 'protolog/udc'),
        buildProtos([
            '../../../../external/perfetto/protos/perfetto/trace/android/protolog.proto'
        ], 'protolog/latest'),

        // SurfaceFlinger
        buildProtos([
            'surfaceflinger/udc/layerstrace.proto',
        ], 'surfaceflinger/udc'),
        buildProtos([
            '../../../../external/perfetto/protos/perfetto/trace/android/surfaceflinger_layers.proto',
        ], 'surfaceflinger/latest'),

        // Transactions
        buildProtos([
            'surfaceflinger/udc/transactions.proto',
        ], 'transactions/udc'),
        buildProtos([
            '../../../../external/perfetto/protos/perfetto/trace/android/surfaceflinger_transactions.proto',
        ], 'transactions/latest'),

        // Transitions
        buildProtos([
            'transitions/udc/windowmanagertransitiontrace.proto',
            'transitions/udc/wm_shell_transition_trace.proto'
        ], 'transitions/udc'),
        buildProtos([
            '../../../../external/perfetto/protos/perfetto/trace/android/shell_transition.proto',
        ], 'transitions/latest'),

        // ViewCapture
        buildProtos([
            '../../../../frameworks/libs/systemui/viewcapturelib/src/com/android/app/viewcapture/proto/view_capture.proto'
        ], 'viewcapture/udc'),
        buildProtos([
            'viewcapture/latest/wrapper.proto',
        ], 'viewcapture/latest'),

        // WindowManager
        buildProtos([
            '../../../../frameworks/base/core/proto/android/server/windowmanagertrace.proto',
        ], 'windowmanager/udc'),
        buildProtos([
            'windowmanager/latest/wrapper.proto',
        ], 'windowmanager/latest'),

        // Input
        buildProtos([
            '../../../../external/perfetto/protos/perfetto/trace/android/android_input_event.proto',
            'input/latest/input_event_wrapper.proto',
        ], 'input/latest'),

        // Test proto fields
        buildProtos([
            'test/fake_proto_test.proto',
        ], 'test/fake_proto'),

        // Test intdef translation
        buildProtos([
            'test/intdef_translation_test.proto',
        ], 'test/intdef_translation'),
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
