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
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {
  TabbedViewSwitchRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {LayerFlag} from 'parsers/surface_flinger/layer_flag';
import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {EMPTY_OBJ_STRING} from 'trace/tree_node/formatters';
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
import {HierarchyPresenter} from 'viewers/common/hierarchy_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {RectsPresenter} from 'viewers/common/rects_presenter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UI_RECT_FACTORY} from 'viewers/common/ui_rect_factory';
import {UserOptions} from 'viewers/common/user_options';
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
    (tree: HierarchyTreeNode) =>
      UI_RECT_FACTORY.makeUiRects(tree, this.viewCapturePackageNames),
    makeDisplayIdentifiers,
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
          tooltip: `
              If checked, shows the value of all properties.
              Otherwise, hides all properties whose value is
              the default for its data type.
            `,
        },
      },
      this.storage,
    ),
    Presenter.DENYLIST_PROPERTY_NAMES,
    undefined,
    ['a', 'type'],
  );
  protected override multiTraceType = undefined;

  private viewCapturePackageNames: string[] = [];
  private curatedProperties: SfCuratedProperties | undefined;
  private displayPropertyGroups = false;

  constructor(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    storage: Readonly<Storage>,
    notifyViewCallback: NotifyHierarchyViewCallbackType<UiData>,
  ) {
    super(trace, traces, storage, notifyViewCallback, new UiData());
  }

  async onRectDoubleClick(rectId: string) {
    const rectHasViewCapture = this.viewCapturePackageNames.some(
      (packageName) => rectId.includes(packageName),
    );
    if (!rectHasViewCapture) {
      return;
    }
    const newActiveTrace = this.traces.getTrace(TraceType.VIEW_CAPTURE);
    if (!newActiveTrace) {
      return;
    }
    await this.emitWinscopeEvent(new TabbedViewSwitchRequest(newActiveTrace));
  }

  override async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        await this.initializeIfNeeded();
        await this.applyTracePositionUpdate(event);
        this.updateCuratedProperties();
        this.refreshUIData();
      },
    );
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

  protected override getOverrideDisplayName(
    selected: [Trace<HierarchyTreeNode>, HierarchyTreeNode],
  ): string | undefined {
    return selected[1].isRoot()
      ? this.hierarchyPresenter.getCurrentHierarchyTreeNames(selected[0])?.at(0)
      : undefined;
  }

  protected override keepCalculated(tree: HierarchyTreeNode): boolean {
    return tree.isRoot();
  }

  private async initializeIfNeeded() {
    const tracesVc = this.traces.getTraces(TraceType.VIEW_CAPTURE);
    const promisesPackageName = tracesVc.map(async (trace) => {
      const packageAndWindow = await trace.customQuery(
        CustomQueryType.VIEW_CAPTURE_METADATA,
      );
      return packageAndWindow.packageName;
    });
    this.viewCapturePackageNames = await Promise.all(promisesPackageName);
  }

  private updateCuratedProperties() {
    const selectedHierarchyTree = this.hierarchyPresenter.getSelectedTree();
    const propertiesTree = this.propertiesPresenter.getPropertiesTree();

    if (selectedHierarchyTree && propertiesTree) {
      if (selectedHierarchyTree[1].isRoot()) {
        this.curatedProperties = undefined;
        this.displayPropertyGroups = false;
      } else {
        this.curatedProperties = this.getCuratedProperties(propertiesTree);
        this.displayPropertyGroups = true;
      }
    }
  }

  private getCuratedProperties(tree: PropertyTreeNode): SfCuratedProperties {
    const inputWindowInfo = tree.getChildByName('inputWindowInfo');
    const hasInputChannel =
      inputWindowInfo !== undefined &&
      inputWindowInfo.getAllChildren().length > 0;

    const cropLayerId = hasInputChannel
      ? assertDefined(
          inputWindowInfo.getChildByName('cropLayerId'),
        ).formattedValue()
      : '-1';

    const verboseFlags = tree.getChildByName('verboseFlags')?.formattedValue();
    const flags = assertDefined(tree.getChildByName('flags'));
    const curatedFlags =
      verboseFlags !== '' && verboseFlags !== undefined
        ? verboseFlags
        : flags.formattedValue();

    const bufferTransform = tree.getChildByName('bufferTransform');
    const bufferTransformTypeFlags =
      bufferTransform?.getChildByName('type')?.formattedValue() ?? 'null';

    const curated: SfCuratedProperties = {
      summary: this.getSummaryOfVisibility(tree),
      flags: curatedFlags,
      calcTransform: tree.getChildByName('transform'),
      calcCrop: assertDefined(tree.getChildByName('bounds')).formattedValue(),
      finalBounds: assertDefined(
        tree.getChildByName('screenBounds'),
      ).formattedValue(),
      reqTransform: tree.getChildByName('requestedTransform'),
      reqCrop: this.getCropPropertyValue(tree, 'bounds'),
      bufferSize: assertDefined(
        tree.getChildByName('activeBuffer'),
      ).formattedValue(),
      frameNumber: assertDefined(
        tree.getChildByName('currFrame'),
      ).formattedValue(),
      bufferTransformType: bufferTransformTypeFlags,
      destinationFrame: assertDefined(
        tree.getChildByName('destinationFrame'),
      ).formattedValue(),
      z: assertDefined(tree.getChildByName('z')).formattedValue(),
      relativeParent: assertDefined(
        tree.getChildByName('zOrderRelativeOf'),
      ).formattedValue(),
      calcColor: this.getColorPropertyValue(tree, 'color'),
      calcShadowRadius: this.getPixelPropertyValue(tree, 'shadowRadius'),
      calcCornerRadius: this.getPixelPropertyValue(tree, 'cornerRadius'),
      calcCornerRadiusCrop: this.getCropPropertyValue(tree, 'cornerRadiusCrop'),
      backgroundBlurRadius: this.getPixelPropertyValue(
        tree,
        'backgroundBlurRadius',
      ),
      reqColor: this.getColorPropertyValue(tree, 'requestedColor'),
      reqCornerRadius: this.getPixelPropertyValue(
        tree,
        'requestedCornerRadius',
      ),
      inputTransform: hasInputChannel
        ? inputWindowInfo.getChildByName('transform')
        : undefined,
      inputRegion: tree.getChildByName('inputRegion')?.formattedValue(),
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
        layerValues: occludedBy.map((layer) => this.getLayerSummary(layer)),
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
          this.getLayerSummary(layer),
        ),
        desc: 'Partially occluded by these opaque layers',
      });
    }

    const coveredBy = tree.getChildByName('coveredBy')?.getAllChildren();
    if (coveredBy && coveredBy.length > 0) {
      summary.push({
        key: 'Covered by',
        layerValues: coveredBy.map((layer) => this.getLayerSummary(layer)),
        desc: 'Partially or fully covered by these likely translucent layers',
      });
    }
    return summary;
  }

  private mapNodeArrayToString(nodes: readonly PropertyTreeNode[]): string {
    return nodes.map((reason) => reason.formattedValue()).join(', ');
  }

  private getLayerSummary(layer: PropertyTreeNode): SfLayerSummary {
    const nodeId = layer.formattedValue();
    const [layerId, name] = nodeId.split(' ');
    return {
      layerId,
      nodeId,
      name,
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

  private refreshUIData() {
    this.uiData.curatedProperties = this.curatedProperties;
    this.uiData.displayPropertyGroups = this.displayPropertyGroups;
    this.refreshHierarchyViewerUiData();
  }
}

export function makeDisplayIdentifiers(
  displayRects: UiRect[],
): DisplayIdentifier[] {
  const ids: DisplayIdentifier[] = [];

  displayRects.forEach((rect: UiRect) => {
    if (!rect.isDisplay) return;
    const displayId = rect.id.slice(10, rect.id.length);
    ids.push({displayId, groupId: rect.groupId, name: rect.label});
  });

  let offscreenDisplayCount = 0;
  displayRects.forEach((rect: UiRect) => {
    if (rect.isDisplay) return;

    if (!ids.find((identifier) => identifier.groupId === rect.groupId)) {
      offscreenDisplayCount++;
      const name =
        'Offscreen Display' +
        (offscreenDisplayCount > 1 ? ` ${offscreenDisplayCount}` : '');
      ids.push({displayId: -1, groupId: rect.groupId, name});
    }
  });

  return ids.sort((a, b) => {
    if (a.name < b.name) {
      return -1;
    }
    if (a.name > b.name) {
      return 1;
    }
    return 0;
  });
}
