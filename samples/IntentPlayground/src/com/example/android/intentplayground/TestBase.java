/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.android.intentplayground;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.example.android.intentplayground.TaskInfo.ActivityInstanceInfoMirror;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.android.intentplayground.FlagUtils.getActivityFlags;
import static com.example.android.intentplayground.FlagUtils.hasActivityFlag;
import static com.example.android.intentplayground.FlagUtils.hasIntentFlag;
import static java.util.Collections.singletonList;

/**
 * TestBase holds methods to query, test and compare task hierarchies.
 */
public class TestBase {
    static final String TAG = "TestBase";
    private List<TaskStackBuilder> mBuilders;
    private Context mContext;
    private PackageInfo mPackageInfo;

    TestBase(Context context, Node hierarchy) {
        mBuilders = new LinkedList<>();
        mContext = context;
        setActivities(hierarchy);
    }

    /**
     * Launch the activities specified by the constructor.
     *
     * @param style An enum that chooses which method to use to launch the activities.
     */
    void startActivities(LaunchStyle style) {
        switch (style) {
            // COMMAND_LINE will only work if the application is installed with system permissions
            // that allow it to use am shell command "am start ..."
            case COMMAND_LINE:
                mBuilders.forEach(tsb -> Arrays.stream(tsb.getIntents())
                        .forEach(AMControl::launchInBackground));
                break;
            case TASK_STACK_BUILDER:
                mBuilders.forEach(tsb -> {
                    // TODO: does this indicate bug in ActivityManager?
                    // The launch of each activity needs to be delayed a bit or ActivityManager will7
                    // skip creating most of them
                    try {
                        Thread.sleep(500);
                        tsb.startActivities();
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Log.e(LauncherActivity.TAG, ie.getMessage());
                    }
                });
                break;
            case LAUNCH_FORWARD:
                mBuilders.forEach(tsb -> {
                    // The launch of each activity needs to be delayed a bit or ActivityManager will
                    // skip creating most of them
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Log.e(LauncherActivity.TAG, ie.getMessage());
                    }
                    ArrayList<Intent> nextIntents = new ArrayList<>(Arrays.asList(
                            tsb.getIntents()));
                    Intent launch = nextIntents.remove(0)
                            .putParcelableArrayListExtra(BaseActivity.EXTRA_LAUNCH_FORWARD, nextIntents);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Launching " + launch.getComponent().toString());
                    }
                    mContext.startActivity(launch);
                });
                break;
        }
    }

    /**
     * This method examines the flags on the given intent, as well as the <activity> flags of
     * the intended component, and computes what the hierarchy of activities should look like
     * after the launch of the intended component (WORK IN PROGRESS).
     * @param intent The intent that will be passed to startActivity().
     * @return A Node object that models the expected hierarchy.
     */
    Node computeExpected(Intent intent) {
        // Determine the effect of selected mIntent flags on expected hierarchy
        Node currentTasks = describeTaskHierarchy(mContext);
        Node startActivity = new Node(intent.getComponent()).setIntent(intent);
        Node targetTask = findReusableTarget(currentTasks, intent)
                .orElseGet(() -> createNewTaskIfNeeded(currentTasks, intent)
                        .orElse(findCurrentTask(currentTasks).get()));
        clearIfNeeded(targetTask, intent);
        if (needsStartActivity(currentTasks, intent)) {
            targetTask.addFirstChild(startActivity);
        }
        return currentTasks;
    }

    /**
     * Finds the taskAffinity of the target component
     * @param actName The component for which to find the corresponding affinity
     * @return the task affinity, or null if there is none associated with the component
     */
    Optional<String> affinityOf(ComponentName actName) {
        String affinity = null;
        for (ActivityInfo activityInfo : mPackageInfo.activities) {
            if (activityInfo.name.equals(actName.getClassName())) {
                affinity = activityInfo.taskAffinity;
            }
        }
        return Optional.ofNullable(affinity);
    }

    /**
     * Describes the current set of tasks open in the application as
     * a tree of Nodes. Returns a root node, whose children are task nodes.
     * The children of those task nodes are activities, in order of most recently used.
     * @param context The context of an activity in the application.
     * @return A Node that models the current task hierarchy of the application.
     */
    public static Node describeTaskHierarchy(Context context) {
        ActivityManager am = context.getSystemService(ActivityManager.class);
        Node root = Node.newRootNode();
        int currentTaskId = ((Activity) context).getTaskId();
        List<ActivityManager.RecentTaskInfo> tasks = am.getAppTasks().stream()
                .map(ActivityManager.AppTask::getTaskInfo).collect(Collectors.toList());
        for (ActivityManager.AppTask task : am.getAppTasks()) {
            ActivityManager.RecentTaskInfo rti = task.getTaskInfo();
            List<ActivityInstanceInfoMirror> activities = TaskInfo.getActivities(rti);
            if (!activities.isEmpty()) {
                Intent baseIntent = activities.get(0).getIntent();
                Node taskRoot = new Node(rti.persistentId).setIntent(baseIntent);
                if (taskRoot.mTaskId == currentTaskId) taskRoot.setCurrent(true);
                activities.forEach(activity -> {
                    taskRoot.addChild(new Node(activity.getName().clone())
                            .setIntent(activity.getIntent()));
                });
                root.addChild(taskRoot);
            }
        }
        return root;
    }

    private Optional<Node> findReusableTarget(Node tasks, Intent intent) {
        ComponentName target = intent.getComponent();
        boolean hasNewTask = hasIntentFlag(intent, IntentFlag.NEW_TASK);
        boolean hasMultipleTask = hasIntentFlag(intent, IntentFlag.MULTIPLE_TASK);
        boolean isSingleInstance = hasActivityFlag(mContext, target,
                ActivityFlag.LAUNCH_MODE_SINGLE_INSTANCE);
        boolean isSingleTask = hasActivityFlag(mContext, target,
                ActivityFlag.LAUNCH_MODE_SINGLE_TASK);
        boolean isIntoExisting = hasActivityFlag(mContext, target,
                ActivityFlag.DOCUMENT_LAUNCH_MODE_INTO_EXISTING);
        if (isSingleInstance || isSingleTask) {
            Log.d(TAG, "found resuable target singleInstance/singleTask");
            return findTaskOfActivity(tasks, target);
        } else if (isIntoExisting || (hasNewTask && !hasMultipleTask)) {
            Optional<Node> rootTask =  findTaskWithRoot(tasks, target);
            if (rootTask.isPresent()) {
                Log.d(TAG, "found resuable target, same root task");
                return rootTask;
            }
            else if (!isDocument(intent)) {
                Log.d(TAG, "found resuable target, same affinity task");
                return findTaskWithAffinity(tasks, affinityOf(target).
                        orElse(target.getPackageName()));
            }
        }
        Log.d(TAG, "did not find resuable target");
        return Optional.empty();
    }

    private Optional<Node> createNewTaskIfNeeded(Node tasks, Intent intent) {
        // Everything in this method runs assuming there is no reuseable target for the intent
        ComponentName target = intent.getComponent();
        boolean hasNewTask = hasIntentFlag(intent, IntentFlag.NEW_TASK);
        boolean hasMultipleTask = hasIntentFlag(intent, IntentFlag.MULTIPLE_TASK);
        boolean hasNewDocument = hasIntentFlag(intent, IntentFlag.NEW_DOCUMENT);
        Set<ActivityFlag> flags = getActivityFlags(mContext, target);
        boolean isSingleInstance = flags.contains(ActivityFlag.LAUNCH_MODE_SINGLE_INSTANCE);
        boolean isSingleTask = flags.contains(ActivityFlag.LAUNCH_MODE_SINGLE_TASK);
        boolean isDocument = flags.contains(ActivityFlag.DOCUMENT_LAUNCH_MODE_ALWAYS)
                || flags.contains(ActivityFlag.DOCUMENT_LAUNCH_MODE_INTO_EXISTING);
        if (hasNewTask || hasNewDocument || isDocument || isSingleInstance || isSingleTask) {
            if (hasMultipleTask) {
                // remove task with same root if present
                findTaskWithRoot(tasks, target).ifPresent(task -> {
                    tasks.mChildren.remove(task);
                });
            }
            Node newNode = new Node(Node.NEW_TASK_ID).setIntent(intent);
            newNode.setNew(true);
            tasks.addFirstChild(newNode);
            Log.d(TAG, "create new task");
            return Optional.of(newNode);
        }
        Log.d(TAG, "did not create new task");
        return Optional.empty();
    }

    private static Optional<Node> findCurrentTask(Node stack) {
        return stack.mChildren.stream().filter(Node::isCurrent).findFirst();
    }

    private static Optional<Node> findTaskOfActivity(Node stack, ComponentName target) {
        for (Node task : stack.mChildren) {
            for (Node activity : task.mChildren) {
                if (activity.mName.equals(target)) return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    public static Optional<Node> findTaskWithRoot(Node stack, ComponentName target) {
        return stack.mChildren.stream().filter(task -> task.mChildren.get(task.mChildren.size() - 1)
                    .mName.equals(target))
                .findFirst();
    }

    public static List<Node> findTasksWithRoot(Node stack, ComponentName target) {
        return stack.mChildren.stream().filter(task -> task.mChildren.get(task.mChildren.size() - 1)
                    .mName.equals(target))
                .collect(Collectors.toList());
    }

    private Optional<Node> findTaskWithAffinity(Node stack, String affinity) {
        // Need to iterate through tasks from least to most recent
        ListIterator<Node> iterator = stack.mChildren.listIterator(stack.mChildren.size());
        while (iterator.hasPrevious()) {
            Node task = iterator.previous();
            if (!hasActivityFlag(mContext, task.mChildren.get(0).mName,
                    ActivityFlag.LAUNCH_MODE_SINGLE_INSTANCE)) { // Exclude singleInstance
                if (affinityOf(task.mChildren.get(0).mName)
                        .filter(a -> a.equals(affinity)).isPresent()) { // find matching affinity
                    return Optional.of(task);
                }
            }
        }
        return Optional.empty();
    }

    private boolean needsStartActivity(Node tasks, Intent intent) {
        ComponentName sourceActivity = findCurrentTask(tasks).orElse(tasks.mChildren.get(0))
                .mChildren.get(0).mName;
        ComponentName target = intent.getComponent();
        boolean hasSingleTop = hasIntentFlag(intent, IntentFlag.SINGLE_TOP);
        boolean isSingleInstance = hasActivityFlag(mContext, target,
                ActivityFlag.LAUNCH_MODE_SINGLE_INSTANCE);
        boolean isSingleTask = hasActivityFlag(mContext, target,
                ActivityFlag.LAUNCH_MODE_SINGLE_TASK);
        boolean isSingleTop = hasActivityFlag(mContext, target,
                ActivityFlag.LAUNCH_MODE_SINGLE_TOP);
        if (sourceActivity.equals(target) &&
                (isSingleInstance || isSingleTask || isSingleTop || hasSingleTop )) {
            return false;
        }
        return true;
    }

    private boolean isDocument(Intent intent) {
        Set<ActivityFlag> flags = getActivityFlags(mContext, intent.getComponent());
        return flags.contains(ActivityFlag.DOCUMENT_LAUNCH_MODE_ALWAYS) ||
                flags.contains(ActivityFlag.DOCUMENT_LAUNCH_MODE_INTO_EXISTING) ||
                hasIntentFlag(intent, IntentFlag.NEW_DOCUMENT);
    }

    void setActivities(Node hierarchy) {
        // load the package info for this app (used later to get <activity> flags)
        PackageManager pm = mContext.getPackageManager();
        try {
            mPackageInfo = pm.getPackageInfo(mContext.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            // Should not happen
            throw new RuntimeException(e);
        }
        // Build list of TaskStackBuilders from task hierarchy modeled by Node
        if (hierarchy.mChildren.isEmpty()) return;
        mBuilders.clear();
        hierarchy.mChildren.forEach(taskParent -> {
            TaskStackBuilder tb = TaskStackBuilder.create(mContext);
            Intent taskRoot = new Intent()
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    .setComponent(taskParent.mChildren.get(0).mName);
            tb.addNextIntent(taskRoot);
            taskParent.mChildren.subList(1, taskParent.mChildren.size()).forEach(activity ->
                    tb.addNextIntent(new Intent().setComponent(activity.mName)));
            mBuilders.add(tb);
        });
        // Edit the mIntent of the last activity in the last task so that it will relaunch the
        // activity that constructed this TestBase
        TaskStackBuilder tsb = mBuilders.get(mBuilders.size() - 1);
        Intent lastIntent = tsb.editIntentAt(tsb.getIntentCount() - 1);
        Intent launcherIntent =  new Intent(mContext, mContext.getClass());
        lastIntent.putParcelableArrayListExtra(BaseActivity.EXTRA_LAUNCH_FORWARD,
                new ArrayList<>(singletonList(launcherIntent)));
    }

    private void clearIfNeeded(Node task, Intent intent) {
        ComponentName target = intent.getComponent();
        boolean hasClearTop = hasIntentFlag(intent, IntentFlag.CLEAR_TOP);
        boolean shouldClearTask = hasIntentFlag(intent, IntentFlag.CLEAR_TASK)
                && hasIntentFlag(intent, IntentFlag.NEW_TASK);
        boolean isDocument = isDocument(intent);
        boolean isSingleInstance = hasActivityFlag(mContext, target,
                ActivityFlag.LAUNCH_MODE_SINGLE_INSTANCE);
        boolean isSingleTask = hasActivityFlag(mContext, target,
                ActivityFlag.LAUNCH_MODE_SINGLE_TASK);
        if (hasClearTop) {
            int targetIndex = 0;
            for (int i = 0; i < task.mChildren.size(); i++) {
                if (task.mChildren.get(i).mName.equals(target)) targetIndex = i;
            }
            task.mChildren = task.mChildren.subList(targetIndex, task.mChildren.size());
        } else if (shouldClearTask || isDocument || isSingleInstance || isSingleTask) {
            task.clearChildren();
        }
    }

    public static void clearRunningTasks(Context context) {
        ComponentName launcher = new ComponentName(context, LauncherActivity.class);
        context.getSystemService(ActivityManager.class).getAppTasks().stream()
                .filter(task -> {
                    ActivityManager.RecentTaskInfo info = task.getTaskInfo();
                    return (info.baseActivity != null) && (!info.baseActivity.equals(launcher));
                })
                .forEach(ActivityManager.AppTask::finishAndRemoveTask);
    }

    public Context getContext() { return mContext; }

    /**
     * An enum representing options for launching a series of tasks using this TestBase.
     */
    enum LaunchStyle { TASK_STACK_BUILDER, COMMAND_LINE, LAUNCH_FORWARD}
}

