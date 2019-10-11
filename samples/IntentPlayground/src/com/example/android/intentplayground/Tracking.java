package com.example.android.intentplayground;

import static java.util.stream.Collectors.toList;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Provides information about the current runnings tasks and activities in the system, by tracking
 * all the lifecycle events happening in the app using {@link Tracker}. {@link Tracker} can be
 * observed for changes in this state. Information regarding the order of activities is kept inside
 * {@link Task}.
 */
public class Tracking {

    /**
     * Stores the {@link com.android.server.wm.Task}-s in MRU order together with the activities
     * within that task and their order. Classes can be notified of changes in this state through
     * {@link Tracker#addListener(Consumer)}
     */
    public static class Tracker {
        private static final String TAG = "Tracker";

        /**
         * Stores {@link Task} by their id.
         */
        private HashMap<Integer, Task> mTaskOverView = new HashMap<>();

        /**
         * {@link Task} belonging to this application, most recently resumed
         * task at front.
         */
        private ArrayDeque<Task> mTaskOrdering = new ArrayDeque<>();

        /**
         * Listeners that get notified whenever the tasks get modified.
         * This also includes reordering of activities within the task.
         */
        private List<Consumer<List<Task>>> mListeners = new ArrayList<>();

        /**
         * When an {@link Activity} becomes resumed, it should be put at the top within it's task.
         * Furthermore the task it belongs to should become the most recent task.
         *
         * We also check if any {@link Activity} we have thinks it's {@link Activity#getTaskId()}
         * does not correspond to the {@link Task} we associated it to.
         * If so we move them to the {@link Task} they report they should belong to.
         *
         * @param activity the {@link Activity} that has been resumed.
         */
        public synchronized void onResume(Activity activity) {
            logNameEventAndTask(activity, "onResume");

            int id = activity.getTaskId();
            Task task = getOrCreateTask(mTaskOverView, id);
            task.activityResumed(activity);
            bringToFront(task);

            checkForMovedActivities().ifPresent(this::moveActivitiesInOrder);

            notifyListeners();
        }

        /**
         * When an {@link Activity} is being destroyed, we remove it from the task it is in.
         * If this activity was the last activity in the task, we also remove the
         * {@link Task}.
         *
         * @param activity the {@link Activity} that has been resumed.
         */
        public synchronized void onDestroy(Activity activity) {
            logNameEventAndTask(activity, "onDestroy");

            // Find the activity by identity in case it has been moved.
            Optional<Task> existingTask = mTaskOverView.values().stream()
                    .filter(t -> t.containsActivity(activity))
                    .findAny();

            if (existingTask.isPresent()) {
                Task task = existingTask.get();
                task.activityDestroyed(activity);

                // If this was the last activity in the task, remove it.
                if (task.mActivities.isEmpty()) {
                    mTaskOverView.remove(task.id);
                    mTaskOrdering.remove(task);
                }
            }

            notifyListeners();
        }

        // If it's not already at the front of the queue, remove it and add it at the front.
        private void bringToFront(Task task) {
            if (mTaskOrdering.peekFirst() != task) {
                mTaskOrdering.remove(task);
                mTaskOrdering.addFirst(task);
            }
        }

        // Check if there is a task that has activities that belong to another task.
        private Optional<Task> checkForMovedActivities() {
            for (Task task : mTaskOverView.values()) {
                for (Activity activity : task.mActivities) {
                    if (activity.getTaskId() != task.id) {
                        return Optional.of(task);
                    }
                }
            }
            return Optional.empty();
        }

        // When a task contains activities that belong to another task, we move them
        // to the other task, in the same order they had in the current task.
        private void moveActivitiesInOrder(Task task) {
            Iterator<Activity> iterator = task.mActivities.iterator();
            while (iterator.hasNext()) {
                Activity activity = iterator.next();
                int id = activity.getTaskId();
                if (id != task.id) {
                    Task target = mTaskOverView.get(id);
                    //the task the activity moved to was not yet known
                    if (target == null) {
                        Task newTask = Task.newTask(id);
                        mTaskOverView.put(id, newTask);
                        // we're not sure where this task should belong now
                        // we put it behind the current front task
                        putBehindFront(newTask);
                        target = newTask;
                    }
                    target.mActivities.add(activity);
                    iterator.remove();
                }
            }
        }

        // If activities moved to a new task that we don't know about yet, we put it behind
        // the most recent task.
        private void putBehindFront(Task task) {
            Task first = mTaskOrdering.removeFirst();
            mTaskOrdering.addFirst(task);
            mTaskOrdering.addFirst(first);
        }


        public static void logNameEventAndTask(Activity activity, String event) {
            Log.i(TAG, activity.getClass().getSimpleName() + " " + event + "task id: "
                    + activity.getTaskId());
        }

        public synchronized int size() {
            return mTaskOverView.size();
        }

        private synchronized void notifyListeners() {
            List<Task> tasks = mTaskOrdering.stream().map(Task::copyForUi).collect(toList());

            for (Consumer<List<Task>> listener : mListeners) {
                listener.accept(tasks);
            }
        }

        public synchronized void addListener(Consumer<List<Task>> listener) {
            mListeners.add(listener);
        }

        public synchronized void removeListener(Consumer<List<Task>> listener) {
            mListeners.remove(listener);
        }
    }

    private static Task getOrCreateTask(Map<Integer, Task> map, int id) {
        Task backup = Task.newTask(id);
        Task task = map.putIfAbsent(id, backup);
        if (task == null) {
            return backup;
        } else {
            return task;
        }
    }

    static class Task {
        public final int id;
        /**
         * The activities in this task,
         * element 0 being the least recent and the last element being the most recent
         */
        protected final List<Activity> mActivities;


        Task(int id, List<Activity> activities) {
            this.id = id;
            mActivities = activities;
        }

        static Task newTask(int id) {
            return new Task(id, new ArrayList<>());
        }


        public void activityResumed(Activity activity) {
            ensureSameTask(activity);

            Iterator<Activity> activityIterator = mActivities.iterator();
            while (activityIterator.hasNext()) {
                Activity next = activityIterator.next();
                //the activity is being moved up.
                if (next == activity) {
                    activityIterator.remove();
                    break;
                }
            }

            mActivities.add(activity);
        }

        public boolean containsActivity(Activity activity) {
            for (Activity activity1 : mActivities) {
                if (activity1 == activity) {
                    return true;
                }
            }

            return false;
        }

        private void ensureSameTask(Activity activity) {
            if (activity.getTaskId() != id) {
                throw new RuntimeException("adding activity to task with different id");
            }
        }

        public void activityDestroyed(Activity activity) {
            ensureSameTask(activity);
            mActivities.removeIf(a -> a == activity);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Task task = (Task) o;
            return id == task.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Task{" +
                    "id=" + id +
                    ", mActivities=" + mActivities +
                    '}';
        }

        public static Task copyForUi(Task task) {
            return new Task(task.id, reverseAndCopy(task.mActivities));
        }

        public static <T> List<T> reverseAndCopy(List<T> ts) {
            ListIterator<T> iterator = ts.listIterator(ts.size());
            List<T> result = new ArrayList<>();

            while (iterator.hasPrevious()) {
                result.add(iterator.previous());
            }

            return result;
        }
    }
}
