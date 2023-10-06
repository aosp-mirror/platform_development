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
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';
import {ImeUtils} from './ime_utils';

describe('ImeUtils', () => {
  it('processes WindowManager trace entry', async () => {
    const entries = await UnitTestUtils.getImeTraceEntries();
    const processed = ImeUtils.processWindowManagerTraceEntry(
      entries.get(TraceType.WINDOW_MANAGER)
    );

    expect(processed.focusedApp).toEqual(
      'com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity'
    );

    expect(processed.focusedActivity.token).toEqual('9d8c2ef');
    expect(processed.focusedActivity.layerId).toEqual(260);

    expect(processed.focusedWindow.token).toEqual('928b3d');
    expect(processed.focusedWindow.title).toEqual(
      'com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity'
    );

    expect(processed.protoImeControlTarget.windowContainer.identifier.title).toEqual(
      'com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity'
    );
    expect(processed.protoImeControlTarget.windowContainer.identifier.hashCode).toEqual(247026562);

    expect(processed.protoImeInputTarget.windowContainer.identifier.title).toEqual(
      'com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity'
    );
    expect(processed.protoImeInputTarget.windowContainer.identifier.hashCode).toEqual(247026562);

    expect(processed.protoImeInsetsSourceProvider.insetsSourceProvider).toBeDefined();

    expect(processed.protoImeLayeringTarget.windowContainer.identifier.title).toEqual(
      'SnapshotStartingWindow for taskId=1393'
    );
    expect(processed.protoImeLayeringTarget.windowContainer.identifier.hashCode).toEqual(222907471);

    expect(processed.isInputMethodWindowVisible).toBeFalse();
  });

  it('processes SurfaceFlinger trace entry', async () => {
    const entries = await UnitTestUtils.getImeTraceEntries();
    const processedWindowManagerState = ImeUtils.processWindowManagerTraceEntry(
      entries.get(TraceType.WINDOW_MANAGER)
    );
    const layers = ImeUtils.getImeLayers(
      entries.get(TraceType.SURFACE_FLINGER),
      processedWindowManagerState
    )!;

    expect(layers.inputMethodSurface.id).toEqual(280);
    expect(layers.inputMethodSurface.isVisible).toEqual(false);
    expect(layers.inputMethodSurface.rect.label).toEqual(
      'Surface(name=77f1069 InputMethod)/@0xb4afb8f - animation-leash of insets_animation#280'
    );
    expect(layers.inputMethodSurface.screenBounds).toBeDefined();

    expect(layers.imeContainer.id).toEqual(12);
    expect(layers.imeContainer.z).toEqual(1);
    expect(layers.imeContainer.zOrderRelativeOfId).toEqual(115);

    expect(String(layers.focusedWindow.color)).toEqual('r:0 g:0 b:0 a:1');

    expect(layers.taskOfImeContainer.kind).toEqual('SF subtree - 114');
    expect(layers.taskOfImeContainer.name).toEqual('Task=1391#114');

    expect(layers.taskOfImeSnapshot).toBeUndefined();
  });
});
