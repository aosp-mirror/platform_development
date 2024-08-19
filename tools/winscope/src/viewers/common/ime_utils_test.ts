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
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';
import {ImeUtils} from './ime_utils';

describe('ImeUtils', () => {
  let userNotifierChecker: UserNotifierChecker;

  beforeAll(() => {
    userNotifierChecker = new UserNotifierChecker();
  });

  afterEach(() => {
    userNotifierChecker.expectNone();
    userNotifierChecker.reset();
  });

  it('processes WindowManager trace entry', async () => {
    const entries = (await UnitTestUtils.getImeTraceEntries())[0];
    const processed = ImeUtils.processWindowManagerTraceEntry(
      assertDefined(entries.get(TraceType.WINDOW_MANAGER)),
      undefined,
    );

    expect(processed.wmStateProperties.focusedApp).toEqual(
      'com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity',
    );

    expect(processed.wmStateProperties.focusedActivity).toEqual(
      '{9d8c2ef com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity} state=RESUMED visible=true',
    );

    expect(processed.wmStateProperties.focusedWindow).toEqual(
      '{928b3d com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity EXITING} type=TYPE_BASE_APPLICATION cf={empty} pf=(0, 0) - (1080, 2400)',
    );

    const imeControlTarget = assertDefined(
      processed.wmStateProperties.imeControlTarget,
    );

    expect(
      imeControlTarget
        .getChildByName('windowContainer')
        ?.getChildByName('identifier')
        ?.getChildByName('title')
        ?.getValue(),
    ).toEqual(
      'com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity',
    );

    const imeInputTarget = assertDefined(
      processed.wmStateProperties.imeInputTarget,
    );
    expect(
      imeInputTarget
        .getChildByName('windowContainer')
        ?.getChildByName('identifier')
        ?.getChildByName('title')
        ?.getValue(),
    ).toEqual(
      'com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity',
    );

    expect(
      processed.wmStateProperties.imeInsetsSourceProvider?.getChildByName(
        'insetsSourceProvider',
      ),
    ).toBeDefined();

    const imeLayeringTarget = assertDefined(
      processed.wmStateProperties.imeLayeringTarget,
    );
    expect(
      imeLayeringTarget
        .getChildByName('windowContainer')
        ?.getChildByName('identifier')
        ?.getChildByName('title')
        ?.getValue(),
    ).toEqual('SnapshotStartingWindow for taskId=1393');

    expect(processed.wmStateProperties.isInputMethodWindowVisible).toBeFalse();
  });

  it('processes SurfaceFlinger trace entry', async () => {
    const entries = (await UnitTestUtils.getImeTraceEntries())[0];
    const processedWindowManagerState = ImeUtils.processWindowManagerTraceEntry(
      assertDefined(entries.get(TraceType.WINDOW_MANAGER)),
      undefined,
    );
    const layers = assertDefined(
      ImeUtils.getImeLayers(
        assertDefined(entries.get(TraceType.SURFACE_FLINGER)),
        processedWindowManagerState,
        undefined,
      ),
    );

    const inputMethodSurface = assertDefined(
      layers.properties.inputMethodSurface,
    );
    const inputMethodSurfaceRect = assertDefined(inputMethodSurface.rect);
    expect(inputMethodSurface.id).toEqual(
      '280 Surface(name=77f1069 InputMethod)/@0xb4afb8f - animation-leash of insets_animation#280',
    );
    expect(inputMethodSurface.isVisible).toEqual(false);
    expect(inputMethodSurfaceRect.getChildByName('left')?.getValue()).toEqual(
      -10800,
    );
    expect(inputMethodSurfaceRect.getChildByName('top')?.getValue()).toEqual(
      -24136,
    );
    expect(inputMethodSurfaceRect.getChildByName('right')?.getValue()).toEqual(
      10800,
    );
    expect(inputMethodSurfaceRect.getChildByName('bottom')?.getValue()).toEqual(
      23864,
    );
    expect(inputMethodSurface.screenBounds).toBeDefined();

    const imeContainer = assertDefined(layers.properties.imeContainer);
    expect(imeContainer.id).toEqual('12 ImeContainer#12');
    expect(imeContainer.z).toEqual(1);
    expect(imeContainer.zOrderRelativeOfId).toEqual(115);

    expect(
      assertDefined(layers.properties.focusedWindowColor).formattedValue(),
    ).toEqual('(0, 0, 0), alpha: 1');

    const taskLayerOfImeContainer = assertDefined(
      layers.taskLayerOfImeContainer,
    );
    expect(taskLayerOfImeContainer.id).toEqual('114 Task=1391#114');
    expect(taskLayerOfImeContainer.name).toEqual('Task=1391#114');

    expect(layers.taskLayerOfImeSnapshot).toBeUndefined();
  });
});
