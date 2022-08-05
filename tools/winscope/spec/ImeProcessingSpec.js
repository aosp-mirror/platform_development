import fs from 'fs';
import path from 'path';

import {dataFile, decodeAndTransformProto, FILE_DECODERS, FILE_TYPES} from '../src/decode';
import {combineWmSfWithImeDataIfExisting} from '../src/ime_processing';

const inputMethodClientFile =
    '../spec/traces/ime_processing_traces/input_method_clients_trace.winscope';
const inputMethodServiceFile =
    '../spec/traces/ime_processing_traces/input_method_service_trace.winscope';
const inputMethodManagerServiceFile =
    '../spec/traces/ime_processing_traces/input_method_manager_service_trace.winscope';
const sfFile = '../spec/traces/ime_processing_traces/sf_trace_for_ime.winscope';
const wmFile = '../spec/traces/ime_processing_traces/wm_trace_for_ime.winscope';

/**
 * Copied from decode.js but with the blobUrl mocked (due to some issues faced
 * with importing Blob, which is used in the decode.js version of this function)
 */
function protoDecoder(buffer, params, fileName, store) {
  const transformed =
      decodeAndTransformProto(buffer, params, store.displayDefaults);

  // add tagGenerationTrace to dataFile for WM/SF traces so tags can be
  // generated
  var tagGenerationTrace = null;
  if (params.type === FILE_TYPES.WINDOW_MANAGER_TRACE ||
      params.type === FILE_TYPES.SURFACE_FLINGER_TRACE) {
    tagGenerationTrace = transformed;
  }

  let data;
  if (params.timeline) {
    data = transformed.entries || transformed.children;
  } else {
    data = [transformed];
  }
  const blobUrl = '';

  return dataFile(
      fileName, data.map((x) => x.timestamp), data, blobUrl, params.type,
      tagGenerationTrace);
}

describe('Ime Processing', () => {
  it('can combine wm & sf properties into ime traces', () => {
    const inputMethodClientBuffer = new Uint8Array(
        fs.readFileSync(path.resolve(__dirname, inputMethodClientFile)));
    const inputMethodServiceBuffer = new Uint8Array(
        fs.readFileSync(path.resolve(__dirname, inputMethodServiceFile)));
    const inputMethodManagerServiceBuffer = new Uint8Array(fs.readFileSync(
        path.resolve(__dirname, inputMethodManagerServiceFile)));
    const sfBuffer =
        new Uint8Array(fs.readFileSync(path.resolve(__dirname, sfFile)));
    const wmBuffer =
        new Uint8Array(fs.readFileSync(path.resolve(__dirname, wmFile)));

    // (buffer, params, fileName, store)
    const store = {displayDefaults: true};
    const inputMethodClientTrace = protoDecoder(
        inputMethodClientBuffer,
        FILE_DECODERS[FILE_TYPES.IME_TRACE_CLIENTS].decoderParams,
        'input_method_clients_trace.winscope', store);
    const inputMethodServiceTrace = protoDecoder(
        inputMethodServiceBuffer,
        FILE_DECODERS[FILE_TYPES.IME_TRACE_SERVICE].decoderParams,
        'input_method_service_trace.winscope', store);
    const inputMethodManagerServiceTrace = protoDecoder(
        inputMethodManagerServiceBuffer,
        FILE_DECODERS[FILE_TYPES.IME_TRACE_MANAGERSERVICE].decoderParams,
        'input_method_manager_service_trace.winscope', store);
    const wmTrace = protoDecoder(
        wmBuffer, FILE_DECODERS[FILE_TYPES.WINDOW_MANAGER_TRACE].decoderParams,
        'sf_trace.winscope', store);
    const sfTrace = protoDecoder(
        sfBuffer, FILE_DECODERS[FILE_TYPES.SURFACE_FLINGER_TRACE].decoderParams,
        'wm_trace.winscope', store);

    const dataFiles = {
      'ImeTrace Clients': inputMethodClientTrace,
      'ImeTrace InputMethodService': inputMethodServiceTrace,
      'ImeTrace InputMethodManagerService': inputMethodManagerServiceTrace,
      'WindowManagerTrace': wmTrace,
      'SurfaceFlingerTrace': sfTrace,
    };
    combineWmSfWithImeDataIfExisting(dataFiles);

    expect(dataFiles.length == 5);

    const processedImeClientTrace = dataFiles['ImeTrace Clients'];
    expect(processedImeClientTrace.type).toEqual('ImeTraceClients');
    expect(processedImeClientTrace.data).toBeDefined();
    expect(processedImeClientTrace.data[0].hasWmSfProperties).toBeTrue();

    expect(processedImeClientTrace.data[1].wmProperties).toBeDefined();
    expect(processedImeClientTrace.data[1].wmProperties.name)
        .toEqual('0d0h8m22s938ms');
    expect(processedImeClientTrace.data[1].wmProperties.focusedApp)
        .toEqual(
            'com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity');
    expect(processedImeClientTrace.data[1].wmProperties.focusedActivity.token)
        .toEqual("9d8c2ef");
    expect(processedImeClientTrace.data[1].wmProperties.focusedActivity.layerId)
        .toEqual(260);
    expect(processedImeClientTrace.data[1].wmProperties.focusedWindow.token)
        .toEqual("928b3d");
    expect(processedImeClientTrace.data[1].wmProperties.focusedWindow.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedImeClientTrace.data[1].wmProperties.imeControlTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    expect(processedImeClientTrace.data[1].wmProperties.imeControlTarget.windowContainer.identifier.hashCode)
        .toEqual(247026562);
    expect(processedImeClientTrace.data[1].wmProperties.imeInputTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    expect(processedImeClientTrace.data[1].wmProperties.imeInputTarget.windowContainer.identifier.hashCode)
        .toEqual(247026562);
    expect(processedImeClientTrace.data[1].wmProperties.imeInsetsSourceProvider
        .insetsSourceProvider).toBeDefined();
    expect(processedImeClientTrace.data[1].wmProperties.imeLayeringTarget.windowContainer.identifier.title)
        .toEqual("SnapshotStartingWindow for taskId=1393");
    expect(processedImeClientTrace.data[1].wmProperties.imeLayeringTarget.windowContainer.identifier.hashCode)
        .toEqual(222907471);
    expect(
        processedImeClientTrace.data[1].wmProperties.isInputMethodWindowVisible)
        .toBeFalse();
    expect(processedImeClientTrace.data[1].wmProperties.proto).toBeDefined();
    expect(processedImeClientTrace.data[1].sfProperties.name)
        .toEqual('0d0h8m22s942ms');
    expect(processedImeClientTrace.data[1]
               .sfProperties.isInputMethodSurfaceVisible)
        .toEqual(false);
    expect(processedImeClientTrace.data[1].sfProperties.imeContainer.id)
        .toEqual(12);
    expect(processedImeClientTrace.data[1].sfProperties.inputMethodSurface.id)
        .toEqual(280);
    expect(processedImeClientTrace.data[1].sfProperties.rect.label).toEqual(
        "Surface(name=77f1069 InputMethod)/@0xb4afb8f - animation-leash of insets_animation#280");
    expect(processedImeClientTrace.data[1].sfProperties.screenBounds)
        .toBeDefined();
    expect(processedImeClientTrace.data[1].sfProperties.z).toEqual(1);
    expect(processedImeClientTrace.data[1].sfProperties.zOrderRelativeOfId)
        .toEqual(115);
    expect(processedImeClientTrace.data[1].sfProperties.focusedWindowRgba)
        .toBeDefined();
    expect(processedImeClientTrace.data[1].sfProperties.proto).toBeDefined();

    expect(processedImeClientTrace.data[10].wmProperties).toBeDefined();
    expect(processedImeClientTrace.data[10].wmProperties.name)
        .toEqual('0d0h8m23s69ms');
    expect(processedImeClientTrace.data[10].wmProperties.focusedApp)
        .toEqual(
            'com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity');
    expect(processedImeClientTrace.data[10].wmProperties.focusedActivity.token)
        .toEqual("9d8c2ef");
    expect(processedImeClientTrace.data[10].wmProperties.focusedActivity.layerId)
        .toEqual(260);
    expect(processedImeClientTrace.data[10].wmProperties.focusedWindow.token)
        .toEqual("928b3d");
    expect(processedImeClientTrace.data[10].wmProperties.focusedWindow.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedImeClientTrace.data[10].wmProperties.imeControlTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    expect(processedImeClientTrace.data[10].wmProperties.imeControlTarget.windowContainer.identifier.hashCode)
        .toEqual(247026562);
    expect(processedImeClientTrace.data[10].wmProperties.imeInputTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    expect(processedImeClientTrace.data[10].wmProperties.imeInputTarget.windowContainer.identifier.hashCode)
        .toEqual(247026562);
    expect(processedImeClientTrace.data[10].wmProperties.imeInsetsSourceProvider
        .insetsSourceProvider).toBeDefined();
    expect(processedImeClientTrace.data[10].wmProperties.imeLayeringTarget.windowContainer.identifier.title)
        .toEqual("SnapshotStartingWindow for taskId=1393");
    expect(processedImeClientTrace.data[10].wmProperties.imeLayeringTarget.windowContainer.identifier.hashCode)
        .toEqual(222907471);
    expect(
        processedImeClientTrace.data[10].wmProperties.isInputMethodWindowVisible)
        .toBeFalse();
    expect(processedImeClientTrace.data[10].wmProperties.proto).toBeDefined();
    expect(processedImeClientTrace.data[10].sfProperties.name)
        .toEqual('0d0h8m23s73ms');
    expect(processedImeClientTrace.data[10]
               .sfProperties.isInputMethodSurfaceVisible)
        .toEqual(false);
    expect(processedImeClientTrace.data[10].sfProperties.imeContainer.id)
        .toEqual(12);
    expect(processedImeClientTrace.data[10].sfProperties.inputMethodSurface.id)
        .toEqual(280);
    expect(processedImeClientTrace.data[10].sfProperties.rect.label).toEqual(
        "Surface(name=77f1069 InputMethod)/@0xb4afb8f - animation-leash of insets_animation#280");
    expect(processedImeClientTrace.data[10].sfProperties.screenBounds)
        .toBeDefined();
    expect(processedImeClientTrace.data[10].sfProperties.z).toEqual(1);
    expect(processedImeClientTrace.data[10].sfProperties.zOrderRelativeOfId)
        .toEqual(115);
    expect(processedImeClientTrace.data[10].sfProperties.focusedWindowRgba)
        .toBeDefined();
    expect(processedImeClientTrace.data[10].sfProperties.proto).toBeDefined();


    const processedInputMethodServiceTrace =
        dataFiles['ImeTrace InputMethodService'];
    expect(processedInputMethodServiceTrace.type)
        .toEqual('ImeTrace InputMethodService');
    expect(processedInputMethodServiceTrace.data[0].hasWmSfProperties)
        .toBeTrue();

    expect(processedInputMethodServiceTrace.data[1].wmProperties).toBeDefined();
    expect(processedInputMethodServiceTrace.data[1].wmProperties.name)
        .toEqual('0d0h8m23s6ms');
    expect(processedInputMethodServiceTrace.data[1].wmProperties.focusedApp)
        .toEqual(
            'com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity');
    expect(processedInputMethodServiceTrace.data[1].wmProperties.focusedActivity.token)
        .toEqual("9d8c2ef");
    expect(processedInputMethodServiceTrace.data[1].wmProperties.focusedActivity.layerId)
        .toEqual(260);
    expect(processedInputMethodServiceTrace.data[1].wmProperties.focusedWindow.token)
        .toEqual("928b3d");
    expect(processedInputMethodServiceTrace.data[1].wmProperties.focusedWindow.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodServiceTrace.data[1].wmProperties.imeControlTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    expect(processedInputMethodServiceTrace.data[1].wmProperties.imeControlTarget.windowContainer.identifier.hashCode)
        .toEqual(247026562);
    expect(processedInputMethodServiceTrace.data[1].wmProperties.imeInputTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    expect(processedInputMethodServiceTrace.data[1].wmProperties.imeInputTarget.windowContainer.identifier.hashCode)
        .toEqual(247026562);
    expect(processedInputMethodServiceTrace.data[1].wmProperties.imeInsetsSourceProvider
        .insetsSourceProvider).toBeDefined();
    expect(processedInputMethodServiceTrace.data[1].wmProperties.imeLayeringTarget.windowContainer.identifier.title)
        .toEqual("SnapshotStartingWindow for taskId=1393");
    expect(processedInputMethodServiceTrace.data[1].wmProperties.imeLayeringTarget.windowContainer.identifier.hashCode)
        .toEqual(222907471);
    expect(
        processedInputMethodServiceTrace.data[1].wmProperties.isInputMethodWindowVisible)
        .toBeFalse();
    expect(processedInputMethodServiceTrace.data[1].wmProperties.proto).toBeDefined();
    expect(processedInputMethodServiceTrace.data[1].sfProperties.name)
        .toEqual('0d0h8m23s26ms');
    expect(processedInputMethodServiceTrace.data[1]
               .sfProperties.isInputMethodSurfaceVisible)
        .toEqual(false);
    expect(processedInputMethodServiceTrace.data[1].sfProperties.imeContainer.id)
        .toEqual(12);
    expect(processedInputMethodServiceTrace.data[1].sfProperties.inputMethodSurface.id)
        .toEqual(280);
    expect(processedInputMethodServiceTrace.data[1].sfProperties.rect.label).toEqual(
        "Surface(name=77f1069 InputMethod)/@0xb4afb8f - animation-leash of insets_animation#280");
    expect(processedInputMethodServiceTrace.data[1].sfProperties.screenBounds)
        .toBeDefined();
    expect(processedInputMethodServiceTrace.data[1].sfProperties.z).toEqual(1);
    expect(processedInputMethodServiceTrace.data[1]
               .sfProperties.zOrderRelativeOfId)
        .toEqual(115);
    expect(
        processedInputMethodServiceTrace.data[1].sfProperties.focusedWindowRgba)
        .toBeDefined();
    expect(processedInputMethodServiceTrace.data[1].sfProperties.proto)
        .toBeDefined();

    expect(processedInputMethodServiceTrace.data[10].wmProperties)
        .toBeDefined();
    expect(processedInputMethodServiceTrace.data[10].wmProperties.name)
        .toEqual('0d0h8m23s186ms');
    expect(processedInputMethodServiceTrace.data[10].wmProperties.focusedApp)
        .toEqual(
            'com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity');
    expect(processedInputMethodServiceTrace.data[10].wmProperties.focusedActivity.token)
        .toEqual("9d8c2ef");
    expect(processedInputMethodServiceTrace.data[10].wmProperties.focusedActivity.layerId)
        .toEqual(260);
    expect(processedInputMethodServiceTrace.data[10].wmProperties.focusedWindow.token)
        .toEqual("928b3d");
    expect(processedInputMethodServiceTrace.data[10].wmProperties.focusedWindow.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodServiceTrace.data[10].wmProperties.imeControlTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodServiceTrace.data[10].wmProperties.imeControlTarget.windowContainer.identifier.hashCode)
        .toEqual(9603901);
    expect(processedInputMethodServiceTrace.data[10].wmProperties.imeInputTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodServiceTrace.data[10].wmProperties.imeInputTarget.windowContainer.identifier.hashCode)
        .toEqual(9603901);
    expect(processedInputMethodServiceTrace.data[10].wmProperties.imeInsetsSourceProvider
        .insetsSourceProvider).toBeDefined();
    expect(processedInputMethodServiceTrace.data[10].wmProperties.imeLayeringTarget.windowContainer.identifier.title)
        .toEqual("SnapshotStartingWindow for taskId=1393");
    expect(processedInputMethodServiceTrace.data[10].wmProperties.imeLayeringTarget.windowContainer.identifier.hashCode)
        .toEqual(222907471);
    expect(
        processedInputMethodServiceTrace.data[10].wmProperties.isInputMethodWindowVisible)
        .toBeTrue();
    expect(processedInputMethodServiceTrace.data[10].wmProperties.proto)
        .toBeDefined();
    expect(processedInputMethodServiceTrace.data[10].sfProperties.name)
        .toEqual('0d0h8m23s173ms');
    expect(processedInputMethodServiceTrace.data[10]
               .sfProperties.isInputMethodSurfaceVisible)
        .toEqual(false);
    expect(processedInputMethodServiceTrace.data[10].sfProperties.imeContainer.id)
        .toEqual(12);
    expect(processedInputMethodServiceTrace.data[10].sfProperties.inputMethodSurface.id)
        .toEqual(291);
    expect(processedInputMethodServiceTrace.data[10].sfProperties.rect.label).toEqual(
        "Surface(name=77f1069 InputMethod)/@0xb4afb8f - animation-leash of" +
        " insets_animation#291");
    expect(processedInputMethodServiceTrace.data[10].sfProperties.screenBounds)
        .toBeDefined();
    expect(processedInputMethodServiceTrace.data[10].sfProperties.z).toEqual(1);
    expect(processedInputMethodServiceTrace.data[10]
               .sfProperties.zOrderRelativeOfId)
        .toEqual(260);
    expect(processedInputMethodServiceTrace.data[10]
               .sfProperties.focusedWindowRgba)
        .toBeDefined();
    expect(processedInputMethodServiceTrace.data[10].sfProperties.proto)
        .toBeDefined();

    const processedInputMethodManagerServiceTrace = dataFiles['ImeTrace' +
    ' InputMethodManagerService'];
    expect(processedInputMethodManagerServiceTrace.type)
        .toEqual(
            'ImeTrace' +
            ' InputMethodManagerService');
    expect(processedInputMethodManagerServiceTrace.data[0].hasWmSfProperties)
        .toBeTrue();

    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties)
        .toBeDefined();
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.name)
        .toEqual('0d0h8m23s52ms');
    expect(
        processedInputMethodManagerServiceTrace.data[1].wmProperties.focusedApp)
        .toEqual(
            'com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity');
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.focusedActivity.token)
        .toEqual("9d8c2ef");
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.focusedActivity.layerId)
        .toEqual(260);
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.focusedWindow.token)
        .toEqual("928b3d");
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.focusedWindow.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.imeControlTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.imeControlTarget.windowContainer.identifier.hashCode)
        .toEqual(247026562);
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.imeInputTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.imeInputTarget.windowContainer.identifier.hashCode)
        .toEqual(247026562);
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.imeInsetsSourceProvider
        .insetsSourceProvider).toBeDefined();
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.imeLayeringTarget.windowContainer.identifier.title)
        .toEqual("SnapshotStartingWindow for taskId=1393");
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.imeLayeringTarget.windowContainer.identifier.hashCode)
        .toEqual(222907471);
    expect(
        processedInputMethodManagerServiceTrace.data[1].wmProperties.isInputMethodWindowVisible)
        .toBeFalse();
    expect(processedInputMethodManagerServiceTrace.data[1].wmProperties.proto).toBeDefined();

    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties)
        .toBeDefined();
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.name)
        .toEqual('0d0h8m27s309ms');
    expect(processedInputMethodManagerServiceTrace.data[10]
               .wmProperties.focusedApp)
        .toEqual(
            'com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity');
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.focusedActivity.token)
        .toEqual("b1ce2cd");
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.focusedActivity.layerId)
        .toEqual(305);
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.focusedWindow.token)
        .toEqual("31d0b");
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.focusedWindow.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.imeControlTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.imeControlTarget.windowContainer.identifier.hashCode)
        .toEqual(204043);
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.imeInputTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.imeInputTarget.windowContainer.identifier.hashCode)
        .toEqual(204043);
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.imeInsetsSourceProvider
        .insetsSourceProvider).toBeDefined();
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.imeLayeringTarget.windowContainer.identifier.title)
        .toEqual("com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity");
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.imeLayeringTarget.windowContainer.identifier.hashCode)
        .toEqual(204043);
    expect(
        processedInputMethodManagerServiceTrace.data[10].wmProperties.isInputMethodWindowVisible)
        .toBeTrue();
    expect(processedInputMethodManagerServiceTrace.data[10].wmProperties.proto).toBeDefined();

  });
});
