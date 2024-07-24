/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {DiffType} from 'viewers/common/diff_type';
import {AddDiffs} from './add_diffs';
import {DiffNode} from './diff_node';

export function executeAddDiffsTests<T extends DiffNode>(
  nodeEqualityTester: (first: any, second: any) => boolean | undefined,
  makeRoot: (value?: string) => T,
  makeChildAndAddToRoot: (rootNode: T, value?: string) => T,
  addDiffs: AddDiffs<T>,
) {
  describe('AddDiffs', () => {
    let newRoot: T;
    let oldRoot: T;
    let expectedRoot: T;

    beforeEach(() => {
      jasmine.addCustomEqualityTester(nodeEqualityTester);
      newRoot = makeRoot();
      oldRoot = makeRoot();
      expectedRoot = makeRoot();
    });

    it('handles two identical trees', async () => {
      await addDiffs.executeInPlace(newRoot, newRoot);
      expect(newRoot).toEqual(expectedRoot);
    });

    it('adds MODIFIED', async () => {
      makeChildAndAddToRoot(newRoot);
      makeChildAndAddToRoot(oldRoot, 'oldValue');

      const expectedChild = makeChildAndAddToRoot(expectedRoot);
      expectedChild.setDiff(DiffType.MODIFIED);

      await addDiffs.executeInPlace(newRoot, oldRoot);
      expect(newRoot).toEqual(expectedRoot);
    });

    it('adds ADDED', async () => {
      makeChildAndAddToRoot(newRoot);

      const expectedChild = makeChildAndAddToRoot(expectedRoot);
      expectedChild.setDiff(DiffType.ADDED);

      await addDiffs.executeInPlace(newRoot, oldRoot);
      expect(newRoot).toEqual(expectedRoot);
    });

    it('adds DELETED', async () => {
      makeChildAndAddToRoot(oldRoot);

      const expectedChild = makeChildAndAddToRoot(expectedRoot);
      expectedChild.setDiff(DiffType.DELETED);

      await addDiffs.executeInPlace(newRoot, oldRoot);
      expect(newRoot).toEqual(expectedRoot);
    });
  });
}
