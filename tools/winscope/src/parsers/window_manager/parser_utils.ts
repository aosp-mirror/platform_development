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
import {perfetto} from 'protos/windowmanager/latest/static';
import {com} from 'protos/windowmanager/udc/static';
import {
  LazyPropertiesStrategyType,
  PropertiesProvider,
} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';
import {WindowTypePrefix} from 'trace/window_type';
import {DENYLIST_PROPERTIES} from './denylist_properties';
import {EAGER_PROPERTIES} from './eager_properties';
import {OperationLists, WmOperationLists} from './operations/operation_lists';
import {ProtoType} from './proto_type';
import {TamperedProtos} from './tampered_protos';

type WindowContainerChildTypeUdc =
  | com.android.server.wm.IWindowContainerProto
  | com.android.server.wm.IDisplayContentProto
  | com.android.server.wm.IDisplayAreaProto
  | com.android.server.wm.ITaskProto
  | com.android.server.wm.IActivityRecordProto
  | com.android.server.wm.IWindowTokenProto
  | com.android.server.wm.IWindowStateProto
  | com.android.server.wm.ITaskFragmentProto;
type WindowContainerChildTypeLatest =
  | perfetto.protos.IWindowContainerProto
  | perfetto.protos.IDisplayContentProto
  | perfetto.protos.IDisplayAreaProto
  | perfetto.protos.ITaskProto
  | perfetto.protos.IActivityRecordProto
  | perfetto.protos.IWindowTokenProto
  | perfetto.protos.IWindowStateProto
  | perfetto.protos.ITaskFragmentProto;
type WindowContainerChildType =
  | WindowContainerChildTypeUdc
  | WindowContainerChildTypeLatest;

type IdentifierProto =
  | com.android.server.wm.IIdentifierProto
  | perfetto.protos.IIdentifierProto;
type WindowManagerServiceDumpProto =
  | com.android.server.wm.IWindowManagerServiceDumpProto
  | perfetto.protos.WindowManagerServiceDumpProto;
type WindowContainerChildProto =
  | com.android.server.wm.IWindowContainerChildProto
  | perfetto.protos.IWindowContainerChildProto;

export class ParserUtils {
  private readonly operationLists: WmOperationLists;

  constructor(tamperedProtos: TamperedProtos) {
    this.operationLists = new WmOperationLists(tamperedProtos);
  }

  makeEntryProperties(
    entryProto: WindowManagerServiceDumpProto,
  ): PropertiesProvider {
    const operations = assertDefined(
      this.operationLists.get(ProtoType.WindowManagerService),
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
    entryProto: WindowManagerServiceDumpProto,
  ): PropertiesProvider[] {
    let currChildren: WindowContainerChildProto[] = assertDefined(
      entryProto.rootWindowContainer?.windowContainer?.children,
    );

    const rootContainer = assertDefined(entryProto.rootWindowContainer);
    const rootContainerProperties = this.getContainerChildProperties(
      rootContainer,
      currChildren,
      this.operationLists.get(ProtoType.RootWindowContainer),
    );

    const containers = [rootContainerProperties];

    while (currChildren && currChildren.length > 0) {
      const nextChildren: WindowContainerChildProto[] = [];
      containers.push(
        ...currChildren.map((containerChild: WindowContainerChildProto) => {
          const children = this.getChildren(containerChild);
          nextChildren.push(...children);
          const containerProperties = this.getContainerChildProperties(
            containerChild,
            children,
          );
          return containerProperties;
        }),
      );
      currChildren = nextChildren;
    }

    return containers;
  }

  private makeEntryEagerPropertiesTree(
    entry: WindowManagerServiceDumpProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    const eagerProperties = assertDefined(
      EAGER_PROPERTIES.get(ProtoType.WindowManagerService),
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
    entry: WindowManagerServiceDumpProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entry)
        .setRootId('WindowManagerState')
        .setRootName('root')
        .setDenyList(
          assertDefined(
            DENYLIST_PROPERTIES.get(ProtoType.WindowManagerService),
          ),
        )
        .build();
    };
  }

  private getChildren(
    child: WindowContainerChildProto,
  ): WindowContainerChildProto[] {
    let children: WindowContainerChildProto[] = [];
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
    containerChild: WindowContainerChildProto,
    children: WindowContainerChildProto[],
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
      operations = assertDefined(this.operationLists.get(containerChildType));
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

  private getContainerChildType(child: WindowContainerChildProto): ProtoType {
    if (child.displayContent) {
      return ProtoType.DisplayContent;
    } else if (child.displayArea) {
      return ProtoType.DisplayArea;
    } else if (child.task) {
      return ProtoType.Task;
    } else if (child.taskFragment) {
      return ProtoType.TaskFragment;
    } else if (child.activity) {
      return ProtoType.Activity;
    } else if (child.windowToken) {
      return ProtoType.WindowToken;
    } else if (child.window) {
      return ProtoType.WindowState;
    }

    return ProtoType.WindowContainer;
  }

  private makeContainerChildEagerPropertiesTree(
    containerChild: WindowContainerChildProto,
    children: WindowContainerChildProto[],
    containerChildType: ProtoType,
  ): PropertyTreeNode {
    const identifier = this.getIdentifier(containerChild);
    const name = this.getName(containerChild, identifier);
    const token = this.makeToken(identifier);

    const eagerProperties = assertDefined(
      EAGER_PROPERTIES.get(containerChildType),
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
    containerChild: WindowContainerChildProto,
    containerChildType: ProtoType,
  ): LazyPropertiesStrategyType {
    return async () => {
      const identifier = this.getIdentifier(containerChild);
      const name = this.getName(containerChild, identifier);
      const token = this.makeToken(identifier);
      const containerDenylistProperties = assertDefined(
        DENYLIST_PROPERTIES.get(containerChildType),
      );

      const container = this.getContainer(containerChild);

      return new PropertyTreeBuilderFromProto()
        .setData(container)
        .setRootId(`${containerChildType} ${token}`)
        .setRootName(name)
        .setDenyList(containerDenylistProperties)
        .build();
    };
  }

  private getIdentifier(
    child: WindowContainerChildProto,
  ): IdentifierProto | undefined {
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
    child: WindowContainerChildProto,
    identifier: IdentifierProto | undefined,
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

  private makeToken(identifier: IdentifierProto | undefined): string {
    return identifier?.hashCode?.toString(16) ?? '';
  }

  private getContainer(
    containerChild: WindowContainerChildProto,
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

  private mapChildrenToTokens(children: WindowContainerChildProto[]): string[] {
    return children
      .map((child) => {
        const identifier = this.getIdentifier(child);
        return this.makeToken(identifier);
      })
      .filter((token) => token.length > 0);
  }
}
