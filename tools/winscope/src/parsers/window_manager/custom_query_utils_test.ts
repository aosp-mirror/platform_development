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
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {TraceType} from 'trace/trace_type';

describe('WmCustomQueryUtils', () =>
  (async () => {
    it('parseWindowsTokenAndTitle()', async () => {
      const trace = await UnitTestUtils.getTrace(
        TraceType.WINDOW_MANAGER,
        'traces/elapsed_and_real_timestamp/WindowManager.pb',
      );
      const tokenAndTitles = await trace
        .sliceEntries(0, 1)
        .customQuery(CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE);

      expect(tokenAndTitles.length).toEqual(69);

      // RootWindowContainerProto
      expect(tokenAndTitles).toContain({
        token: '478edff',
        title: 'WindowContainer',
      });
      // DisplayContentProto
      expect(tokenAndTitles).toContain({
        token: '1f3454e',
        title: 'Built-in Screen',
      });
      // DisplayAreaProto
      expect(tokenAndTitles).toContain({token: 'c06766f', title: 'Leaf:36:36'});
      // WindowTokenProto
      expect(tokenAndTitles).toContain({token: '509ad2f', title: '509ad2f'});
      // WindowStateProto
      expect(tokenAndTitles).toContain({
        token: 'b3b210d',
        title: 'ScreenDecorOverlay',
      });
      // TaskProto
      expect(tokenAndTitles).toContain({token: '7493986', title: 'Task'});
      // ActivityRecordProto
      expect(tokenAndTitles).toContain({
        token: 'f7092ed',
        title: 'com.google.android.apps.nexuslauncher/.NexusLauncherActivity',
      });
    });
  })());
