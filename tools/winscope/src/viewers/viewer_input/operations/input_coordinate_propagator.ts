/*
 * Copyright 2025 The Android Open Source Project
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
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';

/**
 * A single input event can be dispatched to multiple windows, where each dispatch
 * is represented by a separate AndroidWindowInputDispatchEvent. The dispatch event
 * can contain axis values in the window and raw coordinate spaces, but to save space,
 * these values are only traced if they are different from the events'. In this
 * operation, we propagate the coordinate values in the cases that they were not
 * logged to save space.
 */
export class InputCoordinatePropagator implements Operation<PropertyTreeNode> {
  static readonly AXIS_X = 0;
  static readonly AXIS_Y = 1;

  private propertyTreeNodeFactory = new PropertyTreeNodeFactory();

  apply(root: PropertyTreeNode): void {
    const motionEventTree = root.getChildByName('motionEvent');
    if (
      motionEventTree === undefined ||
      motionEventTree.getAllChildren().length === 0
    ) {
      return;
    }

    const pointersById = this.getPointerCoordsById(motionEventTree);
    if (pointersById.size === 0) return;

    const dispatchTree = assertDefined(
      root.getChildByName('windowDispatchEvents'),
    );
    dispatchTree.getAllChildren().forEach((dispatchEntry) => {
      dispatchEntry
        .getChildByName('dispatchedPointer')
        ?.getAllChildren()
        ?.forEach((pointer) => {
          const pointerId = pointer.getChildByName('pointerId')?.getValue();
          if (pointerId === undefined) return;

          const eventXY = pointersById.get(Number(pointerId));
          if (eventXY === undefined) return;

          let axisValues = pointer.getChildByName('axisValueInWindow');
          if (axisValues === undefined) {
            axisValues = this.addPropertyTo(pointer, 'axisValueInWindow');
          }

          const populatedAxes = new Set<number>();
          axisValues.getAllChildren().forEach((axisValue) => {
            const axis = Number(axisValue.getChildByName('axis')?.getValue());
            if (
              axis !== InputCoordinatePropagator.AXIS_X &&
              axis !== InputCoordinatePropagator.AXIS_Y
            ) {
              return;
            }
            if (axisValue.getChildByName('value')?.getValue() === undefined) {
              // This value is not populated, so remove it so that it will be replaced with the
              // values propagated from the event.
              axisValues?.removeChild(axisValue.id);
              return;
            }
            populatedAxes.add(axis);
          });

          // Populate the X and Y axis values
          if (!populatedAxes.has(InputCoordinatePropagator.AXIS_X)) {
            const xAxisValue = this.addPropertyTo(axisValues, 'x');
            this.addPropertyTo(
              xAxisValue,
              'axis',
              InputCoordinatePropagator.AXIS_X,
            );
            this.addPropertyTo(xAxisValue, 'value', eventXY[0]);
          }
          if (!populatedAxes.has(InputCoordinatePropagator.AXIS_Y)) {
            const yAxisValue = this.addPropertyTo(axisValues, 'y');
            this.addPropertyTo(
              yAxisValue,
              'axis',
              InputCoordinatePropagator.AXIS_Y,
            );
            this.addPropertyTo(yAxisValue, 'value', eventXY[1]);
          }

          // Populate Raw X and Raw Y values (x/y in display)
          if (pointer.getChildByName('xInDisplay')?.getValue() === undefined) {
            this.addPropertyTo(pointer, 'xInDisplay', eventXY[0]);
          }
          if (pointer.getChildByName('yInDisplay')?.getValue() === undefined) {
            this.addPropertyTo(pointer, 'yInDisplay', eventXY[1]);
          }
        });
    });
  }

  private addPropertyTo(
    parent: PropertyTreeNode,
    name: string,
    value: any = undefined,
  ): PropertyTreeNode {
    const node = this.propertyTreeNodeFactory.makeCalculatedProperty(
      parent.id,
      name,
      value,
    );
    parent.addOrReplaceChild(node);
    return node;
  }

  private getPointerCoordsById(
    motionEventTree: PropertyTreeNode,
  ): Map<number, [number, number]> {
    const pointersById = new Map<number, [number, number]>();

    motionEventTree
      .getChildByName('pointer')
      ?.getAllChildren()
      ?.forEach((pointer) => {
        const pointerId = pointer.getChildByName('pointerId')?.getValue();
        if (pointerId === undefined) return;

        const axisValues = pointer.getChildByName('axisValue');
        let x: number | undefined;
        let y: number | undefined;
        axisValues?.getAllChildren()?.forEach((axisValue) => {
          const axis = Number(axisValue.getChildByName('axis')?.getValue());
          if (axis === InputCoordinatePropagator.AXIS_X) {
            x = axisValue.getChildByName('value')?.getValue();
            return;
          }
          if (axis === InputCoordinatePropagator.AXIS_Y) {
            y = axisValue.getChildByName('value')?.getValue();
            return;
          }
        });
        if (x === undefined || y === undefined) return;

        pointersById.set(pointerId, [x, y]);
      });

    return pointersById;
  }
}
