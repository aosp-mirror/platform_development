/*
 * Copyright 2024 The Android Open Source Project
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
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbstractLogViewerPresenter} from 'viewers/common/abstract_log_viewer_presenter';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {LogField, LogFieldType} from 'viewers/common/ui_data_log';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {InputEntry, UiData} from './ui_data';

type NotifyViewCallbackType = (uiData: UiData) => void;

enum InputEventType {
  KEY,
  MOTION,
}

export class Presenter extends AbstractLogViewerPresenter {
  static readonly FIELD_TYPES = [
    LogFieldType.INPUT_TYPE,
    LogFieldType.INPUT_SOURCE,
    LogFieldType.INPUT_ACTION,
    LogFieldType.INPUT_DEVICE_ID,
    LogFieldType.INPUT_DISPLAY_ID,
    LogFieldType.INPUT_EVENT_DETAILS,
  ];

  static readonly DENYLIST_DISPATCH_PROPERTIES = ['eventId'];

  private readonly traces: Traces;
  protected override uiData: UiData = UiData.createEmpty();
  private allEntries: InputEntry[] | undefined;

  protected override logPresenter = new LogPresenter(false);
  protected override propertiesPresenter = new PropertiesPresenter({}, []);
  protected dispatchPropertiesPresenter = new PropertiesPresenter(
    {},
    Presenter.DENYLIST_DISPATCH_PROPERTIES,
  );

  constructor(
    traces: Traces,
    mergedInputEventTrace: Trace<PropertyTreeNode>,
    private readonly storage: Storage,
    private readonly notifyInputViewCallback: NotifyViewCallbackType,
  ) {
    super(
      mergedInputEventTrace,
      (uiData) => notifyInputViewCallback(uiData as UiData),
      UiData.createEmpty(),
    );
    this.traces = traces;
  }

  protected override async initializeIfNeeded() {
    if (this.allEntries !== undefined) {
      return;
    }

    this.allEntries = await this.makeInputEntries();

    this.logPresenter.setAllEntries(this.allEntries);
    this.logPresenter.setHeaders(Presenter.FIELD_TYPES);
    this.refreshUIData(UiData.createEmpty());
  }

  private async makeInputEntries(): Promise<InputEntry[]> {
    const entries: InputEntry[] = [];
    for (let i = 0; i < this.trace.lengthEntries; i++) {
      const entry = assertDefined(this.trace.getEntry(i));
      const wrapperTree = await entry.getValue();

      let type = InputEventType.KEY;
      let eventTree = wrapperTree.getChildByName('keyEvent');
      if (eventTree === undefined || eventTree.getAllChildren().length === 0) {
        eventTree = assertDefined(wrapperTree.getChildByName('motionEvent'));
        type = InputEventType.MOTION;
      }
      eventTree.setIsRoot(true);

      const dispatchTree = assertDefined(
        wrapperTree.getChildByName('windowDispatchEvents'),
      );
      dispatchTree.setIsRoot(true);

      entries.push(
        new InputEntry(
          entry,
          Presenter.extractLogFields(eventTree, type),
          eventTree,
          dispatchTree,
        ),
      );
    }
    return Promise.resolve(entries);
  }

  private static extractLogFields(
    eventTree: PropertyTreeNode,
    type: InputEventType,
  ): LogField[] {
    return [
      {
        type: LogFieldType.INPUT_TYPE,
        value: type === InputEventType.KEY ? 'KEY' : 'MOTION',
      },
      {
        type: LogFieldType.INPUT_SOURCE,
        value: assertDefined(eventTree.getChildByName('source'))
          .formattedValue()
          .replace('SOURCE_', ''),
      },
      {
        type: LogFieldType.INPUT_ACTION,
        value: assertDefined(eventTree.getChildByName('action'))
          .formattedValue()
          .replace('ACTION_', ''),
      },
      {
        type: LogFieldType.INPUT_DEVICE_ID,
        value: assertDefined(eventTree.getChildByName('deviceId')).getValue(),
      },
      {
        type: LogFieldType.INPUT_DISPLAY_ID,
        value: assertDefined(eventTree.getChildByName('displayId')).getValue(),
      },
      {
        type: LogFieldType.INPUT_EVENT_DETAILS,
        value:
          type === InputEventType.KEY
            ? Presenter.extractKeyDetails(eventTree)
            : Presenter.extractMotionDetails(eventTree),
      },
    ];
  }

  private static extractKeyDetails(eventTree: PropertyTreeNode): string {
    return (
      'Keycode: ' + eventTree.getChildByName('keyCode')?.formattedValue() ??
      'not present'
    );
  }

  private static extractMotionDetails(eventTree: PropertyTreeNode): string {
    let details = '';
    const pointers =
      eventTree.getChildByName('pointer')?.getAllChildren() ?? [];
    pointers.forEach((pointer) => {
      const id = pointer.getChildByName('pointerId')?.formattedValue() ?? '?';
      let x = '?';
      let y = '?';
      const axisValues =
        pointer.getChildByName('axisValue')?.getAllChildren() ?? [];
      axisValues.forEach((axisValue) => {
        if (axisValue.getChildByName('axis')?.formattedValue() === 'AXIS_X') {
          x = axisValue.getChildByName('value')?.getValue();
          return;
        }
        if (axisValue.getChildByName('axis')?.formattedValue() === 'AXIS_Y') {
          y = axisValue.getChildByName('value')?.getValue();
          return;
        }
      });

      if (details.length !== 0) {
        details += ', ';
      }
      details += '[' + id + ': (' + x + ', ' + y + ')]';
    });
    return details;
  }

  protected override async updatePropertiesTree() {
    await super.updatePropertiesTree();

    const tree = this.getDispatchTree();
    this.dispatchPropertiesPresenter.setPropertiesTree(tree);
    await this.dispatchPropertiesPresenter.formatPropertiesTree(
      undefined,
      undefined,
      this.keepCalculated ?? false,
    );
    this.uiData.dispatchPropertiesTree =
      this.dispatchPropertiesPresenter.getFormattedTree();
  }

  private getDispatchTree(): PropertyTreeNode | undefined {
    const entries = this.logPresenter.getFilteredEntries();
    const selectedIndex = this.logPresenter.getSelectedIndex();
    const currentIndex = this.logPresenter.getCurrentIndex();
    const index = selectedIndex ?? currentIndex;
    if (index === undefined) {
      return undefined;
    }
    return (entries[index] as InputEntry).dispatchPropertiesTree;
  }

  override addEventListeners(htmlElement: HTMLElement) {
    super.addEventListeners(htmlElement);

    htmlElement.addEventListener(
      ViewerEvents.HighlightedPropertyChange,
      (event) =>
        this.onHighlightedPropertyChange((event as CustomEvent).detail.id),
    );
  }

  onHighlightedPropertyChange(id: string) {
    this.propertiesPresenter.applyHighlightedPropertyChange(id);
    this.dispatchPropertiesPresenter.applyHighlightedPropertyChange(id);
    this.uiData.highlightedProperty = id;
  }
}
