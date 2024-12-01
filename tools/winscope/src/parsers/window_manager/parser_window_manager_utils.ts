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
import {com} from 'protos/windowmanager/latest/static';
import {
  LazyPropertiesStrategyType,
  PropertiesProvider,
} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';
import {WindowTypePrefix} from 'trace/window_type';
import {OperationLists, WM_OPERATION_LISTS} from './operations/operation_lists';
import {WM_DENYLIST_PROPERTIES} from './wm_denylist_properties';
import {WM_EAGER_PROPERTIES} from './wm_eager_properties';
import {WmProtoType} from './wm_proto_type';

type WindowContainerChildType =
  | com.android.server.wm.IWindowContainerProto
  | com.android.server.wm.IDisplayContentProto
  | com.android.server.wm.IDisplayAreaProto
  | com.android.server.wm.ITaskProto
  | com.android.server.wm.IActivityRecordProto
  | com.android.server.wm.IWindowTokenProto
  | com.android.server.wm.IWindowStateProto
  | com.android.server.wm.ITaskFragmentProto;

class ParserWindowManagerUtils {
  makeEntryProperties(
    entryProto: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): PropertiesProvider {
    const operations = assertDefined(
      WM_OPERATION_LISTS.get(WmProtoType.WindowManagerService),
    );
    return new PropertiesProviderBuilder()
      .setEagerProperties(
        this.makeEntryEagerPropertiesTree(assertDefined(entryProto)),
      )
      .setLazyPropertiesStrategy(
        this.makeEntryLazyPropertiesStrategy(assertDefined(entryProto)),
      )
      .setCommonOperations(operations.common)
      .setEagerOperations(operations.eager)
      .setLazyOperations(operations.lazy)
      .build();
  }

  extractContainers(
    entryProto: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): PropertiesProvider[] {
    let currChildren: com.android.server.wm.IWindowContainerChildProto[] =
      assertDefined(entryProto.rootWindowContainer?.windowContainer?.children);

    const rootContainer = assertDefined(entryProto.rootWindowContainer);
    const rootContainerProperties = this.getContainerChildProperties(
      rootContainer,
      currChildren,
      WM_OPERATION_LISTS.get(WmProtoType.RootWindowContainer),
    );

    const containers = [rootContainerProperties];

    while (currChildren && currChildren.length > 0) {
      const nextChildren: com.android.server.wm.IWindowContainerChildProto[] =
        [];
      containers.push(
        ...currChildren.map(
          (
            containerChild: com.android.server.wm.IWindowContainerChildProto,
          ) => {
            const children = this.getChildren(containerChild);
            nextChildren.push(...children);
            const containerProperties = this.getContainerChildProperties(
              containerChild,
              children,
            );
            return containerProperties;
          },
        ),
      );
      currChildren = nextChildren;
    }

    return containers;
  }

  private makeEntryEagerPropertiesTree(
    entry: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    const eagerProperties = assertDefined(
      WM_EAGER_PROPERTIES.get(WmProtoType.WindowManagerService),
    );
    let obj = entry;
    do {
      Object.getOwnPropertyNames(obj).forEach((it) => {
        if (!eagerProperties.includes(it)) {
          denyList.push(it);
        }
      });
      obj = Object.getPrototypeOf(obj);
    } while (obj);

    return new PropertyTreeBuilderFromProto()
      .setData(entry)
      .setRootId('WindowManagerState')
      .setRootName('root')
      .setDenyList(denyList)
      .build();
  }

  private makeEntryLazyPropertiesStrategy(
    entry: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entry)
        .setRootId('WindowManagerState')
        .setRootName('root')
        .setDenyList(WM_DENYLIST_PROPERTIES)
        .build();
    };
  }

  private getChildren(
    child: com.android.server.wm.IWindowContainerChildProto,
  ): com.android.server.wm.IWindowContainerChildProto[] {
    let children: com.android.server.wm.IWindowContainerChildProto[] = [];
    if (child.displayContent) {
      children =
        child.displayContent.rootDisplayArea?.windowContainer?.children ?? [];
    } else if (child.displayArea) {
      children = child.displayArea.windowContainer?.children ?? [];
    } else if (child.task) {
      const taskContainer =
        child.task.taskFragment?.windowContainer ?? child.task.windowContainer;
      children = taskContainer?.children ?? [];
    } else if (child.taskFragment) {
      children = child.taskFragment.windowContainer?.children ?? [];
    } else if (child.activity) {
      children = child.activity.windowToken?.windowContainer?.children ?? [];
    } else if (child.windowToken) {
      children = child.windowToken.windowContainer?.children ?? [];
    } else if (child.window) {
      children = child.window.windowContainer?.children ?? [];
    } else if (child.windowContainer) {
      children = child.windowContainer?.children ?? [];
    }

    return children;
  }

  private getContainerChildProperties(
    containerChild: com.android.server.wm.IWindowContainerChildProto,
    children: com.android.server.wm.IWindowContainerChildProto[],
    operations?: OperationLists,
  ): PropertiesProvider {
    const containerChildType = this.getContainerChildType(containerChild);

    const eagerProperties = this.makeContainerChildEagerPropertiesTree(
      containerChild,
      children,
      containerChildType,
    );
    const lazyPropertiesStrategy =
      this.makeContainerChildLazyPropertiesStrategy(
        containerChild,
        containerChildType,
      );

    if (!operations) {
      operations = assertDefined(WM_OPERATION_LISTS.get(containerChildType));
    }

    const containerProperties = new PropertiesProviderBuilder()
      .setEagerProperties(eagerProperties)
      .setLazyPropertiesStrategy(lazyPropertiesStrategy)
      .setCommonOperations(operations.common)
      .setEagerOperations(operations.eager)
      .setLazyOperations(operations.lazy)
      .build();
    return containerProperties;
  }

  private getContainerChildType(
    child: com.android.server.wm.IWindowContainerChildProto,
  ): WmProtoType {
    if (child.displayContent) {
      return WmProtoType.DisplayContent;
    } else if (child.displayArea) {
      return WmProtoType.DisplayArea;
    } else if (child.task) {
      return WmProtoType.Task;
    } else if (child.taskFragment) {
      return WmProtoType.TaskFragment;
    } else if (child.activity) {
      return WmProtoType.Activity;
    } else if (child.windowToken) {
      return WmProtoType.WindowToken;
    } else if (child.window) {
      return WmProtoType.WindowState;
    }

    return WmProtoType.WindowContainer;
  }

  private makeContainerChildEagerPropertiesTree(
    containerChild: com.android.server.wm.IWindowContainerChildProto,
    children: com.android.server.wm.IWindowContainerChildProto[],
    containerChildType: WmProtoType,
  ): PropertyTreeNode {
    const identifier = this.getIdentifier(containerChild);
    const name = this.getName(containerChild, identifier);
    const token = this.makeToken(identifier);

    const eagerProperties = assertDefined(
      WM_EAGER_PROPERTIES.get(containerChildType),
    );

    const denyList: string[] = [];

    const container = this.getContainer(containerChild);
    let obj = container;
    do {
      Object.getOwnPropertyNames(obj).forEach((it) => {
        if (!eagerProperties.includes(it)) denyList.push(it);
      });
      obj = Object.getPrototypeOf(obj);
    } while (obj);

    const containerProperties = new PropertyTreeBuilderFromProto()
      .setData(container)
      .setRootId(`${containerChildType} ${token}`)
      .setRootName(name)
      .setDenyList(denyList)
      .build();

    if (children.length > 0) {
      containerProperties.addOrReplaceChild(
        DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
          containerProperties.id,
          'children',
          this.mapChildrenToTokens(children),
        ),
      );
    }

    containerProperties.addOrReplaceChild(
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
        containerProperties.id,
        'token',
        token,
      ),
    );

    return containerProperties;
  }

  private makeContainerChildLazyPropertiesStrategy(
    containerChild: com.android.server.wm.IWindowContainerChildProto,
    containerChildType: WmProtoType,
  ): LazyPropertiesStrategyType {
    return async () => {
      const identifier = this.getIdentifier(containerChild);
      const name = this.getName(containerChild, identifier);
      const token = this.makeToken(identifier);
      const container = this.getContainer(containerChild);

      return new PropertyTreeBuilderFromProto()
        .setData(container)
        .setRootId(`${containerChildType} ${token}`)
        .setRootName(name)
        .setDenyList(WM_DENYLIST_PROPERTIES)
        .build();
    };
  }

  private getIdentifier(
    child: com.android.server.wm.IWindowContainerChildProto,
  ): com.android.server.wm.IIdentifierProto | undefined {
    if (child.displayContent) {
      return (
        child.displayContent.rootDisplayArea?.windowContainer?.identifier ??
        undefined
      );
    }
    if (child.displayArea) {
      return child.displayArea.windowContainer?.identifier ?? undefined;
    }
    if (child.task) {
      return (
        child.task.taskFragment?.windowContainer?.identifier ??
        child.task.windowContainer?.identifier ??
        undefined
      );
    }
    if (child.taskFragment) {
      return child.taskFragment.windowContainer?.identifier ?? undefined;
    }
    if (child.activity) {
      return (
        child.activity.identifier ??
        child.activity.windowToken?.windowContainer?.identifier ??
        undefined
      );
    }
    if (child.windowToken) {
      return child.windowToken ?? undefined;
    }
    if (child.window) {
      return (
        child.window.windowContainer?.identifier ??
        child.window.identifier ??
        undefined
      );
    }
    if (child.windowContainer) {
      return child.windowContainer?.identifier ?? undefined;
    }
    return undefined;
  }

  private getName(
    child: com.android.server.wm.IWindowContainerChildProto,
    identifier: com.android.server.wm.IIdentifierProto | undefined,
  ): string {
    let nameOverride: string | undefined;
    if (child.displayContent) {
      nameOverride = child.displayContent.displayInfo?.name;
    } else if (child.displayArea) {
      nameOverride = child.displayArea.name ?? undefined;
    } else if (child.activity) {
      nameOverride = child.activity.name ?? undefined;
    } else if (child.windowToken) {
      nameOverride = child.windowToken.hashCode?.toString(16);
    } else if (child.window) {
      nameOverride =
        child.window.windowContainer?.identifier?.title ??
        child.window.identifier?.title ??
        '';

      if (nameOverride.startsWith(WindowTypePrefix.STARTING)) {
        nameOverride = nameOverride.substring(WindowTypePrefix.STARTING.length);
      } else if (nameOverride.startsWith(WindowTypePrefix.DEBUGGER)) {
        nameOverride = nameOverride.substring(WindowTypePrefix.DEBUGGER.length);
      }
    }

    return nameOverride ?? identifier?.title ?? '';
  }

  private makeToken(
    identifier: com.android.server.wm.IIdentifierProto | undefined,
  ): string {
    return identifier?.hashCode?.toString(16) ?? '';
  }

  private getContainer(
    containerChild: com.android.server.wm.IWindowContainerChildProto,
  ): WindowContainerChildType {
    if (containerChild.displayContent) {
      return containerChild.displayContent;
    }
    if (containerChild.displayArea) {
      return containerChild.displayArea;
    }
    if (containerChild.task) {
      return containerChild.task;
    }
    if (containerChild.activity) {
      return containerChild.activity;
    }
    if (containerChild.windowToken) {
      return containerChild.windowToken;
    }
    if (containerChild.window) {
      return containerChild.window;
    }
    if (containerChild.taskFragment) {
      return containerChild.taskFragment;
    }
    return assertDefined(containerChild.windowContainer);
  }

  private mapChildrenToTokens(
    children: com.android.server.wm.IWindowContainerChildProto[],
  ): string[] {
    return children
      .map((child) => {
        const identifier = this.getIdentifier(child);
        return this.makeToken(identifier);
      })
      .filter((token) => token.length > 0);
  }
}

export const ParserWmUtils = new ParserWindowManagerUtils();
