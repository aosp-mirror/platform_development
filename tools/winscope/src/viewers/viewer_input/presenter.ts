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
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Store} from 'common/store';
import {TabbedViewSwitchRequest} from 'messaging/winscope_event';
import {CustomQueryType} from 'trace/custom_query';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {RectsPresenter} from 'viewers/common/rects_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {LogField, LogFieldType, LogFilter} from 'viewers/common/ui_data_log';
import {UI_RECT_FACTORY} from 'viewers/common/ui_rect_factory';
import {UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {
  convertRectIdToLayerorDisplayName,
  makeDisplayIdentifiers,
} from 'viewers/viewer_surface_flinger/presenter';
import {DispatchEntryFormatter} from './operations/dispatch_entry_formatter';
import {InputEntry, UiData} from './ui_data';

enum InputEventType {
  KEY,
  MOTION,
}

export class Presenter extends AbstractLogViewerPresenter<UiData> {
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
  private readonly surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;
  protected override uiData: UiData = UiData.createEmpty();
  private allEntries: InputEntry[] | undefined;

  private readonly layerIdToName = new Map<number, string>();
  private readonly allInputLayerIds = new Set<number>();

  protected override logPresenter = new LogPresenter<InputEntry>();
  protected override propertiesPresenter = new PropertiesPresenter(
    {},
    PersistentStoreProxy.new<TextFilter>(
      'InputPropertiesFilter',
      new TextFilter('', []),
      this.storage,
    ),
    [],
  );
  protected dispatchPropertiesPresenter = new PropertiesPresenter(
    {},
    PersistentStoreProxy.new<TextFilter>(
      'InputDispatchPropertiesFilter',
      new TextFilter('', []),
      this.storage,
    ),
    Presenter.DENYLIST_DISPATCH_PROPERTIES,
    [new DispatchEntryFormatter(this.layerIdToName)],
  );
  private readonly currentTargetWindowIds = new Set<string>();

  private readonly rectsPresenter = new RectsPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'InputWindowRectsOptions',
      {
        showOnlyWithContent: {
          name: 'Has input',
          icon: 'pan_tool_alt',
          enabled: false,
        },
        showOnlyVisible: {
          name: 'Show only',
          chip: VISIBLE_CHIP,
          enabled: true,
        },
      },
      this.storage,
    ),
    (tree: HierarchyTreeNode) =>
      UI_RECT_FACTORY.makeInputRects(tree, (id) =>
        this.currentTargetWindowIds.has(id),
      ),
    makeDisplayIdentifiers,
    convertRectIdToLayerorDisplayName,
  );

  constructor(
    traces: Traces,
    mergedInputEventTrace: Trace<PropertyTreeNode>,
    private readonly storage: Store,
    readonly notifyInputViewCallback: NotifyLogViewCallbackType<UiData>,
  ) {
    const uiData = UiData.createEmpty();
    uiData.isDarkMode = storage.get('dark-mode') === 'true';
    super(
      mergedInputEventTrace,
      (uiData) => notifyInputViewCallback(uiData as UiData),
      uiData,
    );
    this.traces = traces;
    this.surfaceFlingerTrace = this.traces.getTrace(TraceType.SURFACE_FLINGER);
  }

  async onDispatchPropertiesFilterChange(textFilter: TextFilter) {
    this.dispatchPropertiesPresenter.applyPropertiesFilterChange(textFilter);
    await this.updateDispatchPropertiesTree();
    this.uiData.dispatchPropertiesFilter = textFilter;
    this.notifyViewChanged();
  }

  protected override async initializeIfNeeded() {
    if (this.allEntries !== undefined) {
      return;
    }

    if (this.surfaceFlingerTrace !== undefined) {
      const layerMappings = await this.surfaceFlingerTrace.customQuery(
        CustomQueryType.SF_LAYERS_ID_AND_NAME,
      );
      layerMappings.forEach(({id, name}) => this.layerIdToName.set(id, name));
    }

    this.allEntries = await this.makeInputEntries();

    this.logPresenter.setAllEntries(this.allEntries);
    this.logPresenter.setHeaders(Presenter.FIELD_TYPES);
    this.logPresenter.setFilters([
      new LogFilter(
        LogFieldType.INPUT_DISPATCH_WINDOWS,
        [...this.allInputLayerIds.values()].map((layerId) => {
          return this.getLayerDisplayName(layerId);
        }),
      ),
    ]);

    this.refreshUiData();
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
      dispatchTree.getAllChildren().forEach((dispatchEntry) => {
        const windowIdNode = dispatchEntry.getChildByName('windowId');
        const windowId = Number(windowIdNode?.getValue() ?? -1);
        this.allInputLayerIds.add(windowId);
      });

      let sfEntry: TraceEntry<HierarchyTreeNode> | undefined;
      if (this.surfaceFlingerTrace !== undefined && this.trace.hasFrameInfo()) {
        const frame = entry.getFramesRange()?.start;
        if (frame !== undefined) {
          const sfFrame = this.surfaceFlingerTrace.getFrame(frame);
          if (sfFrame.lengthEntries > 0) {
            sfEntry = sfFrame.getEntry(0);
          }
        }
      }

      entries.push(
        new InputEntry(
          entry,
          this.extractLogFields(eventTree, dispatchTree, type),
          eventTree,
          dispatchTree,
          sfEntry,
        ),
      );
    }
    return Promise.resolve(entries);
  }

  private getLayerDisplayName(layerId: number): string {
    // Surround the name using the invisible zero-width non-joiner character to ensure
    // the full string is matched while filtering.
    return `\u{200C}${
      this.layerIdToName.get(layerId) ?? layerId.toString()
    }\u{200C}`;
  }

  private extractLogFields(
    eventTree: PropertyTreeNode,
    dispatchTree: PropertyTreeNode,
    type: InputEventType,
  ): LogField[] {
    const targetWindows: string[] = [];
    dispatchTree.getAllChildren().forEach((dispatchEntry) => {
      const windowId = Number(
        dispatchEntry.getChildByName('windowId')?.getValue() ?? -1,
      );
      targetWindows.push(this.getLayerDisplayName(windowId));
    });

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
      {
        type: LogFieldType.INPUT_DISPATCH_WINDOWS,
        value: targetWindows.join(', '),
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
    await this.updateDispatchPropertiesTree();
    await this.updateRects();
  }

  private async updateDispatchPropertiesTree() {
    const inputEntry = this.getCurrentEntry();
    const tree = inputEntry?.dispatchPropertiesTree;
    this.dispatchPropertiesPresenter.setPropertiesTree(tree);
    await this.dispatchPropertiesPresenter.formatPropertiesTree(
      undefined,
      undefined,
      this.keepCalculated ?? false,
    );
    this.uiData.dispatchPropertiesTree =
      this.dispatchPropertiesPresenter.getFormattedTree();
  }

  private async updateRects() {
    if (this.surfaceFlingerTrace === undefined) {
      return;
    }
    const inputEntry = this.getCurrentEntry();

    this.currentTargetWindowIds.clear();
    inputEntry?.dispatchPropertiesTree
      ?.getAllChildren()
      ?.forEach((dispatchEntry) => {
        const windowId = dispatchEntry.getChildByName('windowId');
        if (windowId !== undefined) {
          this.currentTargetWindowIds.add(`${Number(windowId.getValue())}`);
        }
      });

    if (inputEntry?.surfaceFlingerEntry !== undefined) {
      const node = await inputEntry.surfaceFlingerEntry.getValue();
      this.rectsPresenter.applyHierarchyTreesChange([
        [this.surfaceFlingerTrace, [node]],
      ]);
      this.uiData.rectsToDraw = this.rectsPresenter.getRectsToDraw();
      this.uiData.rectIdToShowState =
        this.rectsPresenter.getRectIdToShowState();
    } else {
      this.uiData.rectsToDraw = [];
      this.uiData.rectIdToShowState = undefined;
    }
    this.uiData.rectsUserOptions = this.rectsPresenter.getUserOptions();
    this.uiData.displays = this.rectsPresenter.getDisplays();
  }

  private getCurrentEntry(): InputEntry | undefined {
    const entries = this.logPresenter.getFilteredEntries();
    const selectedIndex = this.logPresenter.getSelectedIndex();
    const currentIndex = this.logPresenter.getCurrentIndex();
    const index = selectedIndex ?? currentIndex;
    if (index === undefined) {
      return undefined;
    }
    return entries[index];
  }

  override addEventListeners(htmlElement: HTMLElement) {
    super.addEventListeners(htmlElement);

    htmlElement.addEventListener(
      ViewerEvents.HighlightedPropertyChange,
      (event) =>
        this.onHighlightedPropertyChange((event as CustomEvent).detail.id),
    );

    htmlElement.addEventListener(ViewerEvents.HighlightedIdChange, (event) =>
      this.onHighlightedIdChange((event as CustomEvent).detail.id),
    );

    htmlElement.addEventListener(
      ViewerEvents.RectsUserOptionsChange,
      async (event) => {
        await this.onRectsUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        );
      },
    );

    htmlElement.addEventListener(ViewerEvents.RectsDblClick, async (event) => {
      await this.onRectDoubleClick();
    });

    htmlElement.addEventListener(
      ViewerEvents.DispatchPropertiesFilterChange,
      async (event) => {
        const detail: TextFilter = (event as CustomEvent).detail;
        await this.onDispatchPropertiesFilterChange(detail);
      },
    );
  }

  onHighlightedPropertyChange(id: string) {
    this.propertiesPresenter.applyHighlightedPropertyChange(id);
    this.dispatchPropertiesPresenter.applyHighlightedPropertyChange(id);
    this.uiData.highlightedProperty =
      id === this.uiData.highlightedProperty ? '' : id;
    this.notifyViewChanged();
  }

  async onHighlightedIdChange(id: string) {
    this.uiData.highlightedRect = id === this.uiData.highlightedRect ? '' : id;
    await this.updateRects();
    this.notifyViewChanged();
  }

  async onRectsUserOptionsChange(userOptions: UserOptions) {
    this.rectsPresenter.applyRectsUserOptionsChange(userOptions);
    await this.updateRects();
    this.notifyViewChanged();
  }

  async onRectDoubleClick() {
    await this.emitAppEvent(
      new TabbedViewSwitchRequest(assertDefined(this.surfaceFlingerTrace)),
    );
  }
}
