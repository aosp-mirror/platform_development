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

import {assertDefined} from 'common/assert_utils';
import {android} from 'protos/transactions/udc/static';
import {FixedStringFormatter} from 'trace/tree_node/formatters';
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class TranslateChanges implements Operation<PropertyTreeNode> {
  apply(value: PropertyTreeNode): void {
    value
      .getChildByName('transactions')
      ?.getAllChildren()
      .forEach((transactionState) => {
        transactionState
          .getChildByName('layerChanges')
          ?.getAllChildren()
          .forEach((layerState) => {
            this.translateLayerChanges(layerState);
          });

        transactionState
          .getChildByName('displayChanges')
          ?.getAllChildren()
          .forEach((displayState) => {
            this.translateDisplayChanges(displayState);
          });
      });

    value
      .getChildByName('addedDisplays')
      ?.getAllChildren()
      .forEach((displayState) => {
        this.translateDisplayChanges(displayState);
      });
  }

  private translateLayerChanges(layerState: PropertyTreeNode) {
    const what = assertDefined(layerState.getChildByName('what'));

    const originalValue = what.getValue();
    let translation: string = '';

    if (originalValue.low !== undefined && originalValue.high !== undefined) {
      translation = this.concatBitsetTokens(
        this.decodeBitset32(
          originalValue.low,
          android.surfaceflinger.proto.LayerState.ChangesLsb,
        ).concat(
          this.decodeBitset32(
            originalValue.high,
            android.surfaceflinger.proto.LayerState.ChangesMsb,
          ),
        ),
      );
    } else {
      const bigintValue = BigInt(originalValue?.toString() ?? 0n);
      translation = this.concatBitsetTokens(
        this.decodeBitset32(
          Number(bigintValue),
          android.surfaceflinger.proto.LayerState.ChangesLsb,
        ).concat(
          this.decodeBitset32(
            Number(bigintValue >> 32n),
            android.surfaceflinger.proto.LayerState.ChangesMsb,
          ),
        ),
      );
    }

    what.setFormatter(new FixedStringFormatter(translation));
  }

  private translateDisplayChanges(displayState: PropertyTreeNode) {
    const what = assertDefined(displayState.getChildByName('what'));
    const originalValue = what.getValue();

    const translation = this.concatBitsetTokens(
      this.decodeBitset32(
        Number(originalValue),
        android.surfaceflinger.proto.DisplayState.Changes,
      ),
    );

    what.setFormatter(new FixedStringFormatter(translation));
  }

  private decodeBitset32(
    bitset: number,
    EnumProto: {[key: number]: string},
  ): string[] {
    const changes = Object.values(EnumProto)
      .filter((key) => {
        if (Number.isNaN(Number(key))) {
          return false;
        }
        return (bitset & Number(key)) !== 0;
      })
      .map((key) => EnumProto[Number(key)]);
    return changes;
  }

  private concatBitsetTokens(tokens: string[]): string {
    if (tokens.length === 0) {
      return '0';
    }
    return tokens.join(' | ');
  }
}
