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
import {assertDefined} from 'common/assert_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';
import {EMPTY_OBJ_STRING} from 'trace/tree_node/formatters';
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
    const layers = assertDefined(
      ImeUtils.getImeLayers(
        entries.get(TraceType.SURFACE_FLINGER),
        processedWindowManagerState,
        undefined
      )
    );

    const inputMethodSurface = assertDefined(layers.properties.inputMethodSurface);
    const inputMethodSurfaceRect = assertDefined(inputMethodSurface.rect);
    expect(inputMethodSurface.id).toEqual(
      '280 Surface(name=77f1069 InputMethod)/@0xb4afb8f - animation-leash of insets_animation#280'
    );
    expect(inputMethodSurface.isVisible).toEqual(false);
    expect(inputMethodSurfaceRect.getChildByName('left')?.getValue()).toEqual(-10800);
    expect(inputMethodSurfaceRect.getChildByName('top')?.getValue()).toEqual(-24136);
    expect(inputMethodSurfaceRect.getChildByName('right')?.getValue()).toEqual(10800);
    expect(inputMethodSurfaceRect.getChildByName('bottom')?.getValue()).toEqual(23864);
    expect(inputMethodSurface.screenBounds).toBeDefined();

    const imeContainer = assertDefined(layers.properties.imeContainer);
    expect(imeContainer.id).toEqual('12 ImeContainer#12');
    expect(imeContainer.z).toEqual(1);
    expect(imeContainer.zOrderRelativeOfId).toEqual(115);

    expect(assertDefined(layers.properties.focusedWindowColor).formattedValue()).toEqual(
      `${EMPTY_OBJ_STRING}, alpha: 1`
    );

    const taskLayerOfImeContainer = assertDefined(layers.taskLayerOfImeContainer);
    expect(taskLayerOfImeContainer.id).toEqual('114 Task=1391#114');
    expect(taskLayerOfImeContainer.name).toEqual('Task=1391#114');

    expect(layers.taskLayerOfImeSnapshot).toBeUndefined();
  });
});
