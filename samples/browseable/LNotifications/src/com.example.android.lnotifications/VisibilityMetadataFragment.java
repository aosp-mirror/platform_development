/*
* Copyright 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.lnotifications;

import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.Random;


/**
 * Fragment that demonstrates how notifications with different visibility metadata differ on
 * a lockscreen.
 */
public class VisibilityMetadataFragment extends Fragment {

    private NotificationManager mNotificationManager;

    /**
     * {@link RadioGroup} that has Visibility RadioButton in its children.
     */
    private RadioGroup mRadioGroup;

    /**
     * Incremental Integer used for ID for notifications so that each notification will be
     * treated differently.
     */
    private Integer mIncrementalNotificationId = Integer.valueOf(0);

    /**
     * Button to show a notification.
     */
    private Button mShowNotificationButton;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NotificationFragment.
     */
    public static VisibilityMetadataFragment newInstance() {
        VisibilityMetadataFragment fragment = new VisibilityMetadataFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public VisibilityMetadataFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotificationManager = (NotificationManager) getActivity().getSystemService(Context
                .NOTIFICATION_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_visibility_metadata_notification, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mShowNotificationButton = (Button) view.findViewById(R.id.show_notification_button);
        mShowNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NotificationVisibility visibility = getVisibilityFromSelectedRadio(mRadioGroup);
                showNotificationClicked(visibility);
            }
        });
        mRadioGroup = (RadioGroup) view.findViewById(R.id.visibility_radio_group);
    }

    /**
     * Creates a new notification with a different visibility level.
     *
     * @param visibility The visibility of the notification to be created.
     *
     * @return A Notification instance.
     */
    //@VisibleForTesting
    Notification createNotification(NotificationVisibility visibility) {
        Notification.Builder notificationBuilder = new Notification.Builder(getActivity())
                .setContentTitle("Notification for Visibility metadata");

        notificationBuilder.setVisibility(visibility.getVisibility());
        notificationBuilder.setContentText(String.format("Visibility : %s",
                visibility.getDescription()));
        notificationBuilder.setSmallIcon(visibility.getNotificationIconId());

        return notificationBuilder.build();
    }

    /**
     * Returns a {@link NotificationVisibility} depending on which RadioButton in the radiogroup
     * is selected.
     *
     * @param radiogroup The RadioGroup.
     * @return The instance of {@link NotificationVisibility} corresponding to RadioButton.
     */
    private NotificationVisibility getVisibilityFromSelectedRadio(RadioGroup radiogroup) {
        switch (radiogroup.getCheckedRadioButtonId()) {
            case R.id.visibility_public_radio_button:
                return NotificationVisibility.PUBLIC;
            case R.id.visibility_private_radio_button:
                return NotificationVisibility.PRIVATE;
            case R.id.visibility_secret_radio_button:
                return NotificationVisibility.SECRET;
            default:
                //If not selected, returns PUBLIC as default.
                return NotificationVisibility.PUBLIC;
        }
    }

    /**
     * Invoked when {@link #mShowNotificationButton} is clicked.
     * Creates a new notification with a different visibility level.
     *
     * @param visibility The visibility of the notification to be created.
     */
    private void showNotificationClicked(NotificationVisibility visibility) {
        // Assigns a unique (incremented) notification ID in order to treat each notification as a
        // different one. This helps demonstrate how a notification with a different visibility
        // level differs on the lockscreen.
        mIncrementalNotificationId = new Integer(mIncrementalNotificationId + 1);
        mNotificationManager.notify(mIncrementalNotificationId, createNotification(visibility));
        Toast.makeText(getActivity(), "Show Notification clicked", Toast.LENGTH_SHORT).show();
    }

    /**
     * Enum indicating possible visibility levels for notifications and related data(String
     * representation of visibility levels, an icon ID to create a notification) to
     * create a notification.
     */
    //@VisibleForTesting
    static enum NotificationVisibility {
        PUBLIC(Notification.VISIBILITY_PUBLIC, "Public", R.drawable.ic_public_notification),
        PRIVATE(Notification.VISIBILITY_PRIVATE, "Private", R.drawable.ic_private_notification),
        SECRET(Notification.VISIBILITY_SECRET, "Secret", R.drawable.ic_secret_notification);

        /**
         * Visibility level of the notification.
         */
        private final int mVisibility;

        /**
         * String representation of the visibility.
         */
        private final String mDescription;

        /**
         * Id of an icon used for notifications created from the visibility.
         */
        private final int mNotificationIconId;

        NotificationVisibility(int visibility, String description, int notificationIconId) {
            mVisibility = visibility;
            mDescription = description;
            mNotificationIconId = notificationIconId;
        }

        public int getVisibility() {
            return mVisibility;
        }

        public String getDescription() {
            return mDescription;
        }

        public int getNotificationIconId() {
            return mNotificationIconId;
        }
    }
}
