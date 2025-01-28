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
import {PersistentStoreProxy} from 'common/store/persistent_store_proxy';
import {Store} from 'common/store/store';
import {TabbedViewSwitchRequest} from 'messaging/winscope_event';
import {CustomQueryType} from 'trace/custom_query';
import {Trace, TraceEntry, TraceEntryLazy} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {RectsPresenter} from 'viewers/common/rects_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {ColumnSpec, LogEntry, LogHeader} from 'viewers/common/ui_data_log';
import {UI_RECT_FACTORY} from 'viewers/common/ui_rect_factory';
import {UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {
  RectLegendFactory,
  TraceRectType,
} from 'viewers/components/rects/rect_spec';
import {
  convertRectIdToLayerorDisplayName,
  makeDisplayIdentifiers,
} from 'viewers/viewer_surface_flinger/presenter';
import {DispatchEntryFormatter} from './operations/dispatch_entry_formatter';
import {InputCoordinatePropagator} from './operations/input_coordinate_propagator';
import {InputEntry, UiData} from './ui_data';

enum InputEventType {
  KEY,
  MOTION,
}

export class Presenter extends AbstractLogViewerPresenter<
  UiData,
  PropertyTreeNode
> {
  private static readonly COLUMNS = {
    type: {
      name: 'Type',
      cssClass: 'input-type inline',
    },
    source: {
      name: 'Source',
      cssClass: 'input-source',
    },
    action: {
      name: 'Action',
      cssClass: 'input-action',
    },
    deviceId: {
      name: 'Device',
      cssClass: 'input-device-id right-align',
    },
    displayId: {
      name: 'Display',
      cssClass: 'input-display-id right-align',
    },
    details: {
      name: 'Details',
      cssClass: 'input-details',
    },
    dispatchWindows: {
      name: 'Target Windows',
      cssClass: 'input-windows',
    },
  };
  static readonly DENYLIST_DISPATCH_PROPERTIES = ['eventId'];

  private readonly traces: Traces;
  private readonly surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;

  private readonly inputCoordinatePropagator = new InputCoordinatePropagator();

  private readonly layerIdToName = new Map<number, string>();
  private readonly allInputLayerIds = new Set<number>();

  protected override logPresenter = new LogPresenter<InputEntry>();
  protected override propertiesPresenter = new PropertiesPresenter(
    {},
    new TextFilter(),
    [],
  );
  protected dispatchPropertiesPresenter = new PropertiesPresenter(
    {},
    new TextFilter(),
    Presenter.DENYLIST_DISPATCH_PROPERTIES,
    [new DispatchEntryFormatter(this.layerIdToName)],
  );
  protected override keepCalculated = true;
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
        this.currentTargetWindowIds.has(id.split(' ')[0]),
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
    uiData.rectSpec = {
      type: TraceRectType.INPUT_WINDOWS,
      icon: TRACE_INFO[TraceType.INPUT_EVENT_MERGED].icon,
      legend: RectLegendFactory.makeLegendForInputWindowRects(false),
    };
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

  protected override async initializeTraceSpecificData() {
    if (this.surfaceFlingerTrace !== undefined) {
      const layerMappings = await this.surfaceFlingerTrace.customQuery(
        CustomQueryType.SF_LAYERS_ID_AND_NAME,
      );
      layerMappings.forEach(({id, name}) => this.layerIdToName.set(id, name));
    }
  }

  protected override makeHeaders(): LogHeader[] {
    return [
      new LogHeader(
        Presenter.COLUMNS.type,
        new LogSelectFilter([], false, '80'),
      ),
      new LogHeader(
        Presenter.COLUMNS.source,
        new LogSelectFilter([], false, '200'),
      ),
      new LogHeader(
        Presenter.COLUMNS.action,
        new LogSelectFilter([], false, '100'),
      ),
      new LogHeader(
        Presenter.COLUMNS.deviceId,
        new LogSelectFilter([], false, '80'),
      ),
      new LogHeader(
        Presenter.COLUMNS.displayId,
        new LogSelectFilter([], false, '80'),
      ),
      new LogHeader(Presenter.COLUMNS.details),
      new LogHeader(
        Presenter.COLUMNS.dispatchWindows,
        new LogSelectFilter([], true, '300'),
      ),
    ];
  }

  protected override async makeUiDataEntries(): Promise<InputEntry[]> {
    const entries: InputEntry[] = [];
    for (let i = 0; i < this.trace.lengthEntries; i++) {
      const traceEntry = assertDefined(this.trace.getEntry(i));
      const entry = await this.makeInputEntry(traceEntry);
      entries.push(entry);
    }
    return Promise.resolve(entries);
  }

  private static getUniqueFieldValues(
    headers: LogHeader[],
    entries: LogEntry[],
  ): Map<ColumnSpec, Set<string>> {
    const uniqueFieldValues = new Map<ColumnSpec, Set<string>>();
    headers.forEach((header) => {
      if (!header.filter || header.spec === Presenter.COLUMNS.dispatchWindows) {
        return;
      }
      uniqueFieldValues.set(header.spec, new Set());
    });
    entries.forEach((entry) => {
      entry.fields.forEach((field) => {
        uniqueFieldValues.get(field.spec)?.add(field.value.toString());
      });
    });
    return uniqueFieldValues;
  }

  protected override updateFiltersInHeaders(
    headers: LogHeader[],
    entries: LogEntry[],
  ) {
    const uniqueFieldValues = Presenter.getUniqueFieldValues(headers, entries);
    headers.forEach((header) => {
      if (!(header.filter instanceof LogSelectFilter)) {
        return;
      }
      if (header.spec === Presenter.COLUMNS.dispatchWindows) {
        header.filter.options = [...this.allInputLayerIds.values()].map(
          (layerId) => {
            return this.getLayerDisplayName(layerId);
          },
        );
        return;
      }
      header.filter.options = Array.from(
        assertDefined(uniqueFieldValues.get(header.spec)),
      );
      header.filter.options.sort();
    });
  }

  private async makeInputEntry(
    traceEntry: TraceEntryLazy<PropertyTreeNode>,
  ): Promise<InputEntry> {
    const wrapperTree = await traceEntry.getValue();
    this.inputCoordinatePropagator.apply(wrapperTree);

    let eventTree = wrapperTree.getChildByName('keyEvent');
    let type = InputEventType.KEY;
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
      const frame = traceEntry.getFramesRange()?.start;
      if (frame !== undefined) {
        const sfFrame = this.surfaceFlingerTrace.getFrame(frame);
        if (sfFrame.lengthEntries > 0) {
          sfEntry = sfFrame.getEntry(0);
        }
      }
    }

    return new InputEntry(
      traceEntry,
      [
        {
          spec: Presenter.COLUMNS.type,
          value: type === InputEventType.KEY ? 'KEY' : 'MOTION',
          propagateEntryTimestamp: true,
        },
        {
          spec: Presenter.COLUMNS.source,
          value: assertDefined(eventTree.getChildByName('source'))
            .formattedValue()
            .replace('SOURCE_', ''),
        },
        {
          spec: Presenter.COLUMNS.action,
          value: assertDefined(eventTree.getChildByName('action'))
            .formattedValue()
            .replace('ACTION_', ''),
        },
        {
          spec: Presenter.COLUMNS.deviceId,
          value: assertDefined(eventTree.getChildByName('deviceId')).getValue(),
        },
        {
          spec: Presenter.COLUMNS.displayId,
          value: assertDefined(
            eventTree.getChildByName('displayId'),
          ).getValue(),
        },
        {
          spec: Presenter.COLUMNS.details,
          value:
            type === InputEventType.KEY
              ? Presenter.extractKeyDetails(eventTree, dispatchTree)
              : Presenter.extractDispatchDetails(dispatchTree),
        },
        {
          spec: Presenter.COLUMNS.dispatchWindows,
          value: dispatchTree
            .getAllChildren()
            .map((dispatchEntry) => {
              const windowId = Number(
                dispatchEntry.getChildByName('windowId')?.getValue() ?? -1,
              );
              return this.getLayerDisplayName(windowId);
            })
            .join(', '),
        },
      ],
      eventTree,
      dispatchTree,
      sfEntry,
    );
  }

  private getLayerDisplayName(layerId: number): string {
    // Surround the name using the invisible zero-width non-joiner character to ensure
    // the full string is matched while filtering.
    return `\u{200C}${
      this.layerIdToName.get(layerId) ?? layerId.toString()
    }\u{200C}`;
  }

  private static extractKeyDetails(
    eventTree: PropertyTreeNode,
    dispatchTree: PropertyTreeNode,
  ): string {
    const keyDetails =
      'Keycode: ' +
        eventTree
          .getChildByName('keyCode')
          ?.formattedValue()
          ?.replace(/^KEYCODE_/, '') ?? '<?>';
    return keyDetails + ' ' + Presenter.extractDispatchDetails(dispatchTree);
  }

  private static extractDispatchDetails(
    dispatchTree: PropertyTreeNode,
  ): string {
    let details = '';
    dispatchTree.getAllChildren().forEach((dispatchEntry) => {
      const windowIdNode = dispatchEntry.getChildByName('windowId');
      if (windowIdNode === undefined) {
        return;
      }
      if (windowIdNode.formattedValue() === '0') {
        // Skip showing windowId 0, which is an omnipresent system window.
        return;
      }
      details += windowIdNode.getValue() + ', ';
    });
    return '[' + details.slice(0, -2) + ']';
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
      this.trace.type,
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
        {trace: this.surfaceFlingerTrace, trees: [node]},
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

  protected override addViewerSpecificListeners(htmlElement: HTMLElement) {
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
