/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.apis.accessibility;

import com.example.android.apis.R;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import java.util.Locale;

/** The TaskBackService listens for AccessibilityEvents, and turns them into information it can
 *  communicate to the user with speech.
 */
public class TaskBackService extends AccessibilityService implements OnInitListener {

    private final String LOG_TAG = "TaskBackService/onAccessibilityEvent";
    private boolean mTextToSpeechInitialized = false;
    private TextToSpeech mTts = null;
    private static final String SEPARATOR = ", ";



    /** Initializes the Text-To-Speech engine as soon as the service is connected. */
    @Override
    public void onServiceConnected() {
        mTts = new TextToSpeech(getApplicationContext(), this);
    }

    /** Processes an AccessibilityEvent, by traversing the View's tree and putting together a
     *  message to speak to the user.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!mTextToSpeechInitialized) {
            Log.e(LOG_TAG, "Text-To-Speech engine not ready.  Bailing out.");
            return;
        }

        int eventType = event.getEventType();
        if (eventType != AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return;
        }

        /* This AccessibilityNodeInfo represents the view that fired the
         * AccessibilityEvent.  The following code will use it to traverse
         * the view hierarchy, using this node as a starting point.
         */
        AccessibilityNodeInfo entryNode = event.getSource();

        /* Every method that returns an AccessibilityNodeInfo may return null,
         * because the explored window is in another process and the corresponding
         * View might be gone by the time your request reaches the view hierarchy."
         */
        if (entryNode == null) {
          return;
        }
        // Grab the parent of the view that fired the event.
        AccessibilityNodeInfo rowNode = entryNode.getParent();

        if (rowNode == null) {
          return;
        }

        /* Using this parent, get references to both child nodes,
         * the label and the checkbox.
         */
        AccessibilityNodeInfo labelNode = rowNode.getChild(0);
        AccessibilityNodeInfo completeNode = rowNode.getChild(1);

        if (labelNode == null || completeNode == null) {
          return;
        }

        /* Using these to determine what the task is and whether or not
         * it's complete, based on the text inside the label, and the state
         * of the checkbox.
         */

        // Quick check to make sure we're not in the ApiDemos nav.
        if (rowNode.getChildCount() < 2 || !rowNode.getChild(1).isCheckable()) {
            return;
        }

        CharSequence taskLabel = labelNode.getText();
        boolean isComplete = completeNode.isChecked();

        String completeStr = null;;
        if (isComplete) {
            completeStr = getString(R.string.task_complete);
        } else {
            completeStr = getString(R.string.task_not_complete);
        }

        String taskStr = getString(R.string.task_complete_template, taskLabel, completeStr);
        StringBuilder forSpeech = new StringBuilder(taskStr);

        /* The custom listview added extra context to the event by adding
         * an AccessibilityRecord to it.  Extract that from the event and read it.
         */
        int records = event.getRecordCount();

        for (int i = 0; i < records; i++) {
            AccessibilityRecord record = event.getRecord(i);
            CharSequence contentDescription = record.getContentDescription();
            if (contentDescription != null) {
                forSpeech.append(SEPARATOR).append(contentDescription);
            }
        }

        /* Speak the forSpeech string to the user.  QUEUE_ADD adds the string to the end of the
         * queue, QUEUE_FLUSH would interrupt whatever was currently being said.
         */
        mTts.speak(forSpeech.toString() ,  TextToSpeech.QUEUE_ADD, null);
        Log.d(LOG_TAG, forSpeech.toString());
    }

    @Override
    public void onInterrupt() {
      /* do nothing */
    }

    /** Sets a flag so that the TaskBackService knows that the Text-To-Speech engine has been
     *  initialized, and can now handle speaking requests.
     */
    @Override
    public void onInit (int status) {
        if (status == TextToSpeech.SUCCESS) {
            mTts.setLanguage(Locale.US);
            mTextToSpeechInitialized = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTextToSpeechInitialized) {
            mTts.shutdown();
        }
    }
}
