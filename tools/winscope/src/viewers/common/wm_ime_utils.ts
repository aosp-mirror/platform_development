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
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

class WindowManagerImeUtils {
  getFocusedActivity(entry: HierarchyTreeNode): HierarchyTreeNode | undefined {
    const focusedDisplay = this.getFocusedDisplay(entry);
    const focusedWindow = this.getFocusedWindow(entry);
    const resumedActivity =
      focusedDisplay?.getEagerPropertyByName('resumedActivity');

    let focusedActivity: HierarchyTreeNode | undefined;
    if (focusedDisplay && resumedActivity) {
      const rootTasks = this.getRootTasks(focusedDisplay);
      focusedActivity = this.getActivityByName(
        assertDefined(resumedActivity.getChildByName('title')).getValue(),
        rootTasks,
      );
    } else if (focusedDisplay && focusedWindow) {
      focusedActivity = this.getActivitiesForWindowState(
        focusedWindow,
        focusedDisplay,
      )?.at(0);
    }

    return focusedActivity;
  }

  getFocusedWindow(entry: HierarchyTreeNode): HierarchyTreeNode | undefined {
    const focusedWindowTitle = entry
      .getEagerPropertyByName('focusedWindow')
      ?.getChildByName('title')
      ?.getValue();
    return this.getVisibleWindows(entry).find(
      (window) => window.name === focusedWindowTitle,
    );
  }

  private getFocusedDisplay(
    entry: HierarchyTreeNode,
  ): HierarchyTreeNode | undefined {
    const focusedDisplayId: number | undefined = entry
      .getEagerPropertyByName('focusedDisplayId')
      ?.getValue();
    return entry
      .getAllChildren()
      .find(
        (node) =>
          node.getEagerPropertyByName('id')?.getValue() === focusedDisplayId,
      );
  }

  private getVisibleWindows(entry: HierarchyTreeNode): HierarchyTreeNode[] {
    const windowStates = entry.filterDfs((node) => {
      return node.id.startsWith('WindowState ');
    }, true);
    const display = assertDefined(
      entry
        .getAllChildren()
        .find(
          (node) =>
            node.getEagerPropertyByName('id')?.getValue() ===
            this.defaultDisplayId,
        ),
    );

    return windowStates.filter((state) => {
      const activities = this.getActivitiesForWindowState(state, display);
      const windowIsVisible =
        state.getEagerPropertyByName('isComputedVisible')?.getValue() ?? false;
      const activityIsVisible =
        activities.find((activity) =>
          activity.getEagerPropertyByName('isComputedVisible')?.getValue(),
        ) ?? false;
      return windowIsVisible && (activityIsVisible || activities.length === 0);
    });
  }

  private getActivitiesForWindowState(
    windowState: HierarchyTreeNode,
    display: HierarchyTreeNode,
  ): HierarchyTreeNode[] {
    return this.getRootTasks(display).reduce((activities, stack) => {
      const activity = this.getActivity(stack, (activity) =>
        this.hasWindowState(activity, windowState),
      );
      if (activity) {
        activities.push(activity);
      }
      return activities;
    }, new Array<HierarchyTreeNode>());
  }

  private hasWindowState(
    activity: HierarchyTreeNode,
    windowState: HierarchyTreeNode,
  ): boolean {
    return (
      activity.filterDfs((node) => {
        return (
          node.id.startsWith('WindowState ') && node.name === windowState.name
        );
      }, true).length > 0
    );
  }

  private getRootTasks(display: HierarchyTreeNode): HierarchyTreeNode[] {
    const tasks = display.filterDfs((node) => {
      const isTask = node.id.startsWith('Task ');
      if (!isTask) return false;

      const taskId = node.getEagerPropertyByName('id')?.getValue();
      const rootTaskId = node.getEagerPropertyByName('rootTaskId')?.getValue();
      return rootTaskId !== undefined && taskId === rootTaskId;
    }, true);

    const rootOrganizedTasks: HierarchyTreeNode[] = [];

    tasks.reverse().filter((task: HierarchyTreeNode) => {
      if (task.getEagerPropertyByName('createdByOrganiser')?.getValue()) {
        rootOrganizedTasks.push(task);
        return false;
      }
      return true;
    });
    // Add root tasks controlled by an organizer
    rootOrganizedTasks.reverse().forEach((rootOrganizedTask) => {
      tasks.push(...rootOrganizedTask.getAllChildren().slice().reverse());
    });

    return tasks;
  }

  private getActivityByName(
    activityName: string,
    rootTasks: HierarchyTreeNode[],
  ): HierarchyTreeNode | undefined {
    for (const rootTask of rootTasks) {
      const activity = this.getActivity(
        rootTask,
        (activity: HierarchyTreeNode) => activity.name.includes(activityName),
      );
      if (activity) {
        return activity;
      }
    }
    return undefined;
  }

  private getActivity(
    task: HierarchyTreeNode,
    predicate: (activity: HierarchyTreeNode) => boolean,
  ): HierarchyTreeNode | undefined {
    const children = task.getAllChildren().slice().reverse();
    let activity = children
      .filter((child) => child.id.startsWith('Activity '))
      .find(predicate);

    if (activity) {
      return activity;
    }

    for (const task of children.filter((child) =>
      child.id.startsWith('Task '),
    )) {
      activity = this.getActivity(task, predicate);
      if (activity) {
        return activity;
      }
    }
    for (const taskFragment of children.filter((child) =>
      child.id.startsWith('TaskFragment '),
    )) {
      activity = this.getActivity(taskFragment, predicate);
      if (activity) {
        return activity;
      }
    }
    return;
  }

  private readonly defaultDisplayId = 0;
}
export const WmImeUtils = new WindowManagerImeUtils();
