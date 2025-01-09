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

import {PersistentStoreProxy} from 'common/store/persistent_store_proxy';
import {Store} from 'common/store/store';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {
  AbstractHierarchyViewerPresenter,
  NotifyHierarchyViewCallbackType,
} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {VISIBLE_CHIP} from 'viewers/common/chip';
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
import {UserOptions} from 'viewers/common/user_options';
import {
  RectLegendFactory,
  TraceRectType,
} from 'viewers/components/rects/rect_spec';
import {UiRect} from 'viewers/components/rects/ui_rect';
import {UpdateDisplayNames} from './operations/update_display_names';
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
      'WmHierarchyOptions',
      {
        showDiff: {
          name: 'Show diff',
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
    [[TraceType.WINDOW_MANAGER, [new UpdateDisplayNames()]]],
  );
  protected override rectsPresenter = new RectsPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'WmRectsOptions',
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
    (tree: HierarchyTreeNode) => UI_RECT_FACTORY.makeUiRects(tree),
    this.getDisplays,
    this.convertRectIdtoContainerName,
  );
  protected override propertiesPresenter = new PropertiesPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'WmPropertyOptions',
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
  );
  protected override multiTraceType = undefined;

  constructor(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    storage: Readonly<Store>,
    notifyViewCallback: NotifyHierarchyViewCallbackType<UiData>,
  ) {
    const uiData = new UiData();
    uiData.rectSpec = {
      type: TraceRectType.WINDOW_STATES,
      icon: TRACE_INFO[TraceType.WINDOW_MANAGER].icon,
      legend: RectLegendFactory.makeLegendForWindowStateRects(),
      multiple: false,
    };
    super(trace, traces, storage, notifyViewCallback, uiData);
  }

  override async onHighlightedNodeChange(item: UiHierarchyTreeNode) {
    await this.applyHighlightedNodeChange(item);
    this.refreshUIData();
  }

  override async onHighlightedIdChange(newId: string) {
    await this.applyHighlightedIdChange(newId);
    this.refreshUIData();
  }

  protected override getOverrideDisplayName(
    selected: SelectedTree,
  ): string | undefined {
    if (!selected.tree.isRoot()) {
      return undefined;
    }
    return this.hierarchyPresenter
      .getCurrentHierarchyTreeNames(selected.trace)
      ?.at(0);
  }

  protected override keepCalculated(tree: HierarchyTreeNode): boolean {
    return false;
  }

  protected override refreshUIData() {
    this.refreshHierarchyViewerUiData();
  }

  private getDisplays(rects: UiRect[]): DisplayIdentifier[] {
    const ids: DisplayIdentifier[] = [];
    rects.forEach((rect: UiRect) => {
      if (!rect.isDisplay) return;
      const displayName = rect.label.slice(10, rect.label.length);
      ids.push({
        displayId: rect.id,
        groupId: rect.groupId,
        name: displayName,
        isActive: rect.isActiveDisplay,
      });
    });
    return ids.sort();
  }

  private convertRectIdtoContainerName(id: string) {
    const parts = id.split(' ');
    return parts.slice(2).join(' ');
  }
}
