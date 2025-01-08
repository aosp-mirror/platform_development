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
import {PersistentStoreProxy} from 'common/store/persistent_store_proxy';
import {Store} from 'common/store/store';
import {
  TabbedViewSwitchRequest,
  TracePositionUpdate,
} from 'messaging/winscope_event';
import {LayerFlag} from 'parsers/surface_flinger/layer_flag';
import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {
  EMPTY_OBJ_STRING,
  FixedStringFormatter,
} from 'trace/tree_node/formatters';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractHierarchyViewerPresenter,
  NotifyHierarchyViewCallbackType,
} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {
  SfCuratedProperties,
  SfLayerSummary,
  SfSummaryProperty,
} from 'viewers/common/curated_properties';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {
  HierarchyPresenter,
  SelectedTree,
} from 'viewers/common/hierarchy_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {RectsPresenter} from 'viewers/common/rects_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UI_RECT_FACTORY} from 'viewers/common/ui_rect_factory';
import {RectType, UiRectType} from 'viewers/common/ui_rect_type';
import {UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {UiRect} from 'viewers/components/rects/ui_rect';
import {UiData} from './ui_data';

export class Presenter extends AbstractHierarchyViewerPresenter<UiData> {
  static readonly DENYLIST_PROPERTY_NAMES = [
    'name',
    'children',
    'dpiX',
    'dpiY',
  ];

  protected override hierarchyPresenter = new HierarchyPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'SfHierarchyOptions',
      {
        showDiff: {
          name: 'Show diff', // TODO: PersistentStoreObject.Ignored("Show diff") or something like that to instruct to not store this info
          enabled: false,
          isUnavailable: false,
        },
        showOnlyVisible: {
          name: 'Show only',
          chip: VISIBLE_CHIP,
          enabled: false,
        },
        simplifyNames: {
          name: 'Simplify names',
          enabled: true,
        },
        flat: {
          name: 'Flat',
          enabled: false,
        },
      },
      this.storage,
    ),
    new TextFilter(),
    Presenter.DENYLIST_PROPERTY_NAMES,
    true,
    false,
    this.getEntryFormattedTimestamp,
  );
  protected override rectsPresenter = new RectsPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'SfRectsOptions',
      {
        ignoreRectShowState: {
          name: 'Ignore',
          icon: 'visibility',
          enabled: false,
        },
        showOnlyVisible: {
          name: 'Show only',
          chip: VISIBLE_CHIP,
          enabled: false,
        },
      },
      this.storage,
    ),
    (tree: HierarchyTreeNode) => {
      if (this.rectTypes[this.rectTypeIndex].type === RectType.LAYERS) {
        return UI_RECT_FACTORY.makeUiRects(tree, this.viewCapturePackageNames);
      }
      return UI_RECT_FACTORY.makeInputRects(tree, (id) => false);
    },
    (displays: UiRect[]) =>
      makeDisplayIdentifiers(displays, this.wmFocusedDisplayId),
    convertRectIdToLayerorDisplayName,
  );
  protected override propertiesPresenter = new PropertiesPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'SfPropertyOptions',
      {
        showDiff: {
          name: 'Show diff',
          enabled: false,
          isUnavailable: false,
        },
        showDefaults: {
          name: 'Show defaults',
          enabled: false,
          tooltip: `If checked, shows the value of all properties.
Otherwise, hides all properties whose value is
the default for its data type.`,
        },
      },
      this.storage,
    ),
    new TextFilter(),
    Presenter.DENYLIST_PROPERTY_NAMES,
    undefined,
    ['a', 'type'],
  );
  protected override multiTraceType = undefined;

  private viewCapturePackageNames: string[] | undefined;
  private curatedProperties: SfCuratedProperties | undefined;
  private wmTrace: Trace<HierarchyTreeNode> | undefined;
  private wmFocusedDisplayId: number | undefined;
  private rectTypes: UiRectType[] = [
    {type: RectType.LAYERS, icon: TRACE_INFO[TraceType.SURFACE_FLINGER].icon},
    {
      type: RectType.INPUT_WINDOWS,
      icon: TRACE_INFO[TraceType.INPUT_EVENT_MERGED].icon,
    },
  ];
  private rectTypeIndex = 0;

  constructor(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    storage: Readonly<Store>,
    notifyViewCallback: NotifyHierarchyViewCallbackType<UiData>,
  ) {
    super(trace, traces, storage, notifyViewCallback, new UiData());
    this.wmTrace = traces.getTrace(TraceType.WINDOW_MANAGER);
  }

  async onRectDoubleClick(rectId: string) {
    if (!this.viewCapturePackageNames) {
      return;
    }
    const rectHasViewCapture = this.viewCapturePackageNames.some(
      (packageName) => rectId.includes(packageName),
    );
    if (!rectHasViewCapture) {
      return;
    }
    const newActiveTrace = assertDefined(
      this.traces.getTrace(TraceType.VIEW_CAPTURE),
    );
    await this.emitWinscopeEvent(new TabbedViewSwitchRequest(newActiveTrace));
  }

  override async onHighlightedNodeChange(item: UiHierarchyTreeNode) {
    await this.applyHighlightedNodeChange(item);
    this.updateCuratedProperties();
    this.refreshUIData();
  }

  override async onHighlightedIdChange(newId: string) {
    await this.applyHighlightedIdChange(newId);
    this.updateCuratedProperties();
    this.refreshUIData();
  }

  onUiRectTypeButtonClicked() {
    this.rectTypeIndex =
      this.rectTypeIndex < this.rectTypes.length - 1
        ? this.rectTypeIndex + 1
        : 0;
    const currentHierarchyTrees =
      this.hierarchyPresenter.getAllCurrentHierarchyTrees();
    if (currentHierarchyTrees) {
      this.rectsPresenter?.applyHierarchyTreesChange(currentHierarchyTrees);
    }
    this.refreshUIData();
  }

  protected override getOverrideDisplayName(
    selected: SelectedTree,
  ): string | undefined {
    return selected.tree.isRoot()
      ? this.hierarchyPresenter
          .getCurrentHierarchyTreeNames(selected.trace)
          ?.at(0)
      : undefined;
  }

  protected override keepCalculated(tree: HierarchyTreeNode): boolean {
    return tree.isRoot();
  }

  protected override async initializeIfNeeded(event: TracePositionUpdate) {
    if (!this.viewCapturePackageNames) {
      const tracesVc = this.traces.getTraces(TraceType.VIEW_CAPTURE);
      const promisesPackageName = tracesVc.map(async (trace) => {
        const packageAndWindow = await trace.customQuery(
          CustomQueryType.VIEW_CAPTURE_METADATA,
        );
        return packageAndWindow.packageName;
      });
      this.viewCapturePackageNames = await Promise.all(promisesPackageName);
    }
    await this.setInitialWmActiveDisplay(event);
  }

  protected override async processDataAfterPositionUpdate(): Promise<void> {
    this.updateCuratedProperties();
  }

  protected override refreshUIData() {
    this.uiData.curatedProperties = this.curatedProperties;
    this.uiData.rectType = this.rectTypes[this.rectTypeIndex];
    this.refreshHierarchyViewerUiData();
  }

  protected override addViewerSpecificListeners(htmlElement: HTMLElement) {
    htmlElement.addEventListener(ViewerEvents.RectsDblClick, async (event) => {
      const rectId = (event as CustomEvent).detail.clickedRectId;
      await this.onRectDoubleClick(rectId);
    });
    htmlElement.addEventListener(ViewerEvents.RectTypeButtonClick, (event) => {
      this.onUiRectTypeButtonClicked();
    });
  }

  private updateCuratedProperties() {
    const selectedHierarchyTree = this.hierarchyPresenter.getSelectedTree();
    const propertiesTree = this.propertiesPresenter.getPropertiesTree();

    if (selectedHierarchyTree && propertiesTree) {
      if (selectedHierarchyTree.tree.isRoot()) {
        this.curatedProperties = undefined;
      } else {
        this.curatedProperties = this.getCuratedProperties(
          selectedHierarchyTree.tree,
          propertiesTree,
        );
      }
    } else {
      this.curatedProperties = undefined;
    }
  }

  private getCuratedProperties(
    hTree: HierarchyTreeNode,
    pTree: PropertyTreeNode,
  ): SfCuratedProperties {
    const inputWindowInfo = pTree.getChildByName('inputWindowInfo');
    const hasInputChannel =
      inputWindowInfo !== undefined &&
      inputWindowInfo.getAllChildren().length > 0;

    const cropLayerId = hasInputChannel
      ? assertDefined(
          inputWindowInfo.getChildByName('cropLayerId'),
        ).formattedValue()
      : '-1';

    const verboseFlags = pTree.getChildByName('verboseFlags')?.formattedValue();
    const flags = assertDefined(pTree.getChildByName('flags'));
    const curatedFlags =
      verboseFlags !== '' && verboseFlags !== undefined
        ? verboseFlags
        : flags.formattedValue();

    const bufferTransform = pTree.getChildByName('bufferTransform');
    const bufferTransformTypeFlags =
      bufferTransform?.getChildByName('type')?.formattedValue() ?? 'null';

    const zOrderRelativeOfNode = assertDefined(
      pTree.getChildByName('zOrderRelativeOf'),
    );
    let relativeParent: string | SfLayerSummary =
      zOrderRelativeOfNode.formattedValue();
    if (relativeParent !== 'none') {
      // update zOrderRelativeOf property formatter to zParent node id
      zOrderRelativeOfNode.setFormatter(
        new FixedStringFormatter(assertDefined(hTree.getZParent()).id),
      );
      relativeParent = this.getLayerSummary(
        zOrderRelativeOfNode.formattedValue(),
      );
    }

    const curated: SfCuratedProperties = {
      summary: this.getSummaryOfVisibility(pTree),
      flags: curatedFlags,
      calcTransform: pTree.getChildByName('transform'),
      calcCrop: this.getCropPropertyValue(pTree, 'bounds'),
      finalBounds: assertDefined(
        pTree.getChildByName('screenBounds'),
      ).formattedValue(),
      reqTransform: pTree.getChildByName('requestedTransform'),
      bufferSize: assertDefined(
        pTree.getChildByName('activeBuffer'),
      ).formattedValue(),
      frameNumber: assertDefined(
        pTree.getChildByName('currFrame'),
      ).formattedValue(),
      bufferTransformType: bufferTransformTypeFlags,
      destinationFrame: assertDefined(
        pTree.getChildByName('destinationFrame'),
      ).formattedValue(),
      z: assertDefined(pTree.getChildByName('z')).formattedValue(),
      relativeParent,
      relativeChildren: hTree
        .getRelativeChildren()
        .map((c) => this.getLayerSummary(c.id)),
      calcColor: this.getColorPropertyValue(pTree, 'color'),
      calcShadowRadius: this.getPixelPropertyValue(pTree, 'shadowRadius'),
      calcCornerRadius: this.getPixelPropertyValue(pTree, 'cornerRadius'),
      calcCornerRadiusCrop: this.getCropPropertyValue(
        pTree,
        'cornerRadiusCrop',
      ),
      backgroundBlurRadius: this.getPixelPropertyValue(
        pTree,
        'backgroundBlurRadius',
      ),
      reqColor: this.getColorPropertyValue(pTree, 'requestedColor'),
      reqCornerRadius: this.getPixelPropertyValue(
        pTree,
        'requestedCornerRadius',
      ),
      inputTransform: inputWindowInfo?.getChildByName('transform'),
      inputRegion: inputWindowInfo
        ?.getChildByName('touchableRegion')
        ?.formattedValue(),
      focusable: hasInputChannel
        ? assertDefined(
            inputWindowInfo.getChildByName('focusable'),
          ).formattedValue()
        : 'null',
      cropTouchRegionWithItem: cropLayerId,
      replaceTouchRegionWithCrop: hasInputChannel
        ? inputWindowInfo
            .getChildByName('replaceTouchableRegionWithCrop')
            ?.formattedValue() ?? 'false'
        : 'false',
      inputConfig:
        inputWindowInfo?.getChildByName('inputConfig')?.formattedValue() ??
        'null',
      ignoreDestinationFrame:
        (flags.getValue() & LayerFlag.IGNORE_DESTINATION_FRAME) ===
        LayerFlag.IGNORE_DESTINATION_FRAME,
      hasInputChannel,
    };
    return curated;
  }

  private getSummaryOfVisibility(tree: PropertyTreeNode): SfSummaryProperty[] {
    const summary: SfSummaryProperty[] = [];
    const visibilityReason = tree.getChildByName('visibilityReason');
    if (visibilityReason && visibilityReason.getAllChildren().length > 0) {
      const reason = this.mapNodeArrayToString(
        visibilityReason.getAllChildren(),
      );
      summary.push({key: 'Invisible due to', simpleValue: reason});
    }

    const occludedBy = tree.getChildByName('occludedBy')?.getAllChildren();
    if (occludedBy && occludedBy.length > 0) {
      summary.push({
        key: 'Occluded by',
        layerValues: occludedBy.map((layer) =>
          this.getLayerSummary(layer.formattedValue()),
        ),
        desc: 'Fully occluded by these opaque layers',
      });
    }

    const partiallyOccludedBy = tree
      .getChildByName('partiallyOccludedBy')
      ?.getAllChildren();
    if (partiallyOccludedBy && partiallyOccludedBy.length > 0) {
      summary.push({
        key: 'Partially occluded by',
        layerValues: partiallyOccludedBy.map((layer) =>
          this.getLayerSummary(layer.formattedValue()),
        ),
        desc: 'Partially occluded by these opaque layers',
      });
    }

    const coveredBy = tree.getChildByName('coveredBy')?.getAllChildren();
    if (coveredBy && coveredBy.length > 0) {
      summary.push({
        key: 'Covered by',
        layerValues: coveredBy.map((layer) =>
          this.getLayerSummary(layer.formattedValue()),
        ),
        desc: 'Partially or fully covered by these likely translucent layers',
      });
    }
    return summary;
  }

  private mapNodeArrayToString(nodes: readonly PropertyTreeNode[]): string {
    return nodes.map((reason) => reason.formattedValue()).join(', ');
  }

  private getLayerSummary(nodeId: string): SfLayerSummary {
    const parts = nodeId.split(' ');
    return {
      layerId: parts[0],
      nodeId,
      name: parts.slice(1).join(' '),
    };
  }

  private getPixelPropertyValue(tree: PropertyTreeNode, label: string): string {
    const propVal = assertDefined(tree.getChildByName(label)).formattedValue();
    return propVal !== 'null' ? `${propVal} px` : '0 px';
  }

  private getCropPropertyValue(tree: PropertyTreeNode, label: string): string {
    const propVal = assertDefined(tree.getChildByName(label)).formattedValue();
    return propVal !== 'null' ? propVal : EMPTY_OBJ_STRING;
  }

  private getColorPropertyValue(tree: PropertyTreeNode, label: string): string {
    const propVal = assertDefined(tree.getChildByName(label)).formattedValue();
    return propVal !== 'null' ? propVal : 'no color found';
  }

  private async setInitialWmActiveDisplay(event: TracePositionUpdate) {
    if (!this.wmTrace || this.wmFocusedDisplayId !== undefined) {
      return;
    }
    const wmEntry: HierarchyTreeNode | undefined =
      await TraceEntryFinder.findCorrespondingEntry<HierarchyTreeNode>(
        this.wmTrace,
        event.position,
      )?.getValue();
    if (wmEntry) {
      this.wmFocusedDisplayId = wmEntry
        .getEagerPropertyByName('focusedDisplayId')
        ?.getValue();
    }
  }
}

export function makeDisplayIdentifiers(
  rects: UiRect[],
  focusedDisplayId?: number,
): DisplayIdentifier[] {
  const ids: DisplayIdentifier[] = [];

  const isActive = (display: UiRect) => {
    if (focusedDisplayId !== undefined) {
      return display.groupId === focusedDisplayId;
    }
    return display.isActiveDisplay;
  };

  rects.forEach((rect: UiRect) => {
    if (!rect.isDisplay) return;

    const displayId = rect.id.slice(10, rect.id.length);
    ids.push({
      displayId,
      groupId: rect.groupId,
      name: rect.label,
      isActive: isActive(rect),
    });
  });

  let offscreenDisplayCount = 0;
  rects.forEach((rect: UiRect) => {
    if (rect.isDisplay) return;

    if (!ids.find((identifier) => identifier.groupId === rect.groupId)) {
      offscreenDisplayCount++;
      const name =
        'Offscreen Display' +
        (offscreenDisplayCount > 1 ? ` ${offscreenDisplayCount}` : '');
      ids.push({displayId: -1, groupId: rect.groupId, name, isActive: false});
    }
  });

  return ids;
}

export function convertRectIdToLayerorDisplayName(id: string) {
  if (id.startsWith('Display')) return id.split('-').slice(1).join('-').trim();
  const idMinusStartLayerId = id.split(' ').slice(1).join(' ');
  const idSplittingEndLayerId = idMinusStartLayerId.split('#');
  return idSplittingEndLayerId
    .slice(0, idSplittingEndLayerId.length - 1)
    .join('#');
}
