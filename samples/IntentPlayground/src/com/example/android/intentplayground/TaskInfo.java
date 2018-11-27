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

import android.app.ActivityManager.RecentTaskInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TaskInfo {

  private final static String TAG = "TaskInfo";

  /**
   * Access the ActivityInstanceInfo without depending on it at compile time.
   *
   * @param info the recent task mInfo to try and access the activity information of.
   * @return The activity instance mInfo in our own mirrored value object or an empty list.
   */
  public static List<ActivityInstanceInfoMirror> getActivities(RecentTaskInfo info) {
    try {
      List<Object> activities = (List<Object>) info.getClass().getField("activities").get(info);
      List<ActivityInstanceInfoMirror> activityInfoMirrors = new ArrayList<>();

      for (Object activityInstanceInfo : activities) {
        Class<?> activityInfoClass = activityInstanceInfo.getClass();
        System.out.println(Arrays.toString(activityInfoClass.getDeclaredFields()));
        ActivityInfo activityInfo = (ActivityInfo) activityInfoClass.getMethod("getInfo")
            .invoke(activityInstanceInfo);
        Intent intent = (Intent) activityInfoClass.getMethod("getIntent")
            .invoke(activityInstanceInfo);
        ComponentName name = (ComponentName) activityInfoClass.getMethod("getName")
            .invoke(activityInstanceInfo);
        Integer hashId = (Integer) activityInfoClass.getMethod("getHashId")
            .invoke(activityInstanceInfo);

        activityInfoMirrors.add(new ActivityInstanceInfoMirror(activityInfo, intent, name, hashId));
      }

      return activityInfoMirrors;
    } catch (IllegalAccessException | NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
      Log.e(TAG, "ActivityInstanceInfo not available on the current api level", e);
      return new ArrayList<>();
    }
  }

  //TODO (b/119894108): Replace the mirror with actual class once the framework-api is merged.
  public static class ActivityInstanceInfoMirror {

    private final ActivityInfo mInfo;
    private final Intent mIntent;
    private final ComponentName mComponentName;
    private final int mHashId;

    public ActivityInstanceInfoMirror(ActivityInfo info, Intent intent,
        ComponentName componentName, int hashId) {
      this.mInfo = info;
      this.mIntent = intent;
      this.mComponentName = componentName;
      this.mHashId = hashId;
    }

    public ActivityInfo getInfo() {
      return mInfo;
    }

    public Intent getIntent() {
      return mIntent;
    }

    public ComponentName getName() {
      return mComponentName;
    }

    public int getHashId() {
      return mHashId;
    }
  }
}
