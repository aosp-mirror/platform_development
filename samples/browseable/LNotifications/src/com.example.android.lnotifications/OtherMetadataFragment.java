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

import android.app.Activity;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Random;

/**
 * Fragment that demonstrates how to attach metadata introduced in Android L, such as
 * priority data, notification category and person data.
 */
public class OtherMetadataFragment extends Fragment {

    private static final String TAG = OtherMetadataFragment.class.getSimpleName();

    /**
     * Request code used for picking a contact.
     */
    public static final int REQUEST_CODE_PICK_CONTACT = 1;

    /**
     * Incremental Integer used for ID for notifications so that each notification will be
     * treated differently.
     */
    private Integer mIncrementalNotificationId = Integer.valueOf(0);

    private NotificationManager mNotificationManager;

    /**
     * Button to show a notification.
     */
    private Button mShowNotificationButton;

    /**
     *  Spinner that holds possible categories used for a notification as
     *  {@link Notification.Builder#setCategory(String)}.
     */
    private Spinner mCategorySpinner;

    /**
     * Spinner that holds possible priorities used for a notification as
     * {@link Notification.Builder#setPriority(int)}.
     */
    private Spinner mPrioritySpinner;

    /**
     * Holds a URI for the person to be attached to the notification.
     */
    //@VisibleForTesting
    Uri mContactUri;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NotificationFragment.
     */
    public static OtherMetadataFragment newInstance() {
        OtherMetadataFragment fragment = new OtherMetadataFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public OtherMetadataFragment() {
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
        return inflater.inflate(R.layout.fragment_other_metadata, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mShowNotificationButton = (Button) view.findViewById(R.id.show_notification_button);
        mShowNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Priority selectedPriority = (Priority) mPrioritySpinner.getSelectedItem();
                Category selectedCategory = (Category) mCategorySpinner.getSelectedItem();
                showNotificationClicked(selectedPriority, selectedCategory, mContactUri);
            }
        });

        mCategorySpinner = (Spinner) view.findViewById(R.id.category_spinner);
        ArrayAdapter<Category> categoryArrayAdapter = new ArrayAdapter<Category>(getActivity(),
                android.R.layout.simple_spinner_item, Category.values());
        categoryArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCategorySpinner.setAdapter(categoryArrayAdapter);

        mPrioritySpinner = (Spinner) view.findViewById(R.id.priority_spinner);
        ArrayAdapter<Priority> priorityArrayAdapter = new ArrayAdapter<Priority>(getActivity(),
                android.R.layout.simple_spinner_item, Priority.values());
        priorityArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPrioritySpinner.setAdapter(priorityArrayAdapter);

        view.findViewById(R.id.attach_person).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findContact();
            }
        });

        view.findViewById(R.id.contact_entry).setVisibility(View.GONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_PICK_CONTACT:
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactUri = data.getData();
                    mContactUri = contactUri;
                    updateContactEntryFromUri(contactUri);
                }
                break;
        }
    }

    /**
     * Invoked when {@link #mShowNotificationButton} is clicked.
     * Creates a new notification and sets metadata passed as arguments.
     *
     * @param priority   The priority metadata.
     * @param category   The category metadata.
     * @param contactUri The URI to be added to the new notification as metadata.
     *
     * @return A Notification instance.
     */
    //@VisibleForTesting
    Notification createNotification(Priority priority, Category category, Uri contactUri) {
        Notification.Builder notificationBuilder = new Notification.Builder(getActivity())
                .setContentTitle("Notification with other metadata")
                .setSmallIcon(R.drawable.ic_launcher_notification)
                .setPriority(priority.value)
                .setCategory(category.value)
                .setContentText(String.format("Category %s, Priority %s", category.value,
                        priority.name()));
        if (contactUri != null) {
            notificationBuilder.addPerson(contactUri.toString());
            Bitmap photoBitmap = loadBitmapFromContactUri(contactUri);
            if (photoBitmap != null) {
                notificationBuilder.setLargeIcon(photoBitmap);
            }
        }
        return notificationBuilder.build();
    }

    /**
     * Invoked when {@link #mShowNotificationButton} is clicked.
     * Creates a new notification and sets metadata passed as arguments.
     *
     * @param priority   The priority metadata.
     * @param category   The category metadata.
     * @param contactUri The URI to be added to the new notification as metadata.
     */
    private void showNotificationClicked(Priority priority, Category category, Uri contactUri) {
        // Assigns a unique (incremented) notification ID in order to treat each notification as a
        // different one. This helps demonstrate how a priority flag affects ordering.
        mIncrementalNotificationId = new Integer(mIncrementalNotificationId + 1);
        mNotificationManager.notify(mIncrementalNotificationId, createNotification(priority,
                category, contactUri));
        Toast.makeText(getActivity(), "Show Notification clicked", Toast.LENGTH_SHORT).show();
    }

    private void findContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }

    /**
     * Returns a {@link Bitmap} from the Uri specified as the argument.
     *
     * @param contactUri The Uri from which the result Bitmap is created.
     * @return The {@link Bitmap} instance retrieved from the contactUri.
     */
    private Bitmap loadBitmapFromContactUri(Uri contactUri) {
        if (contactUri == null) {
            return null;
        }
        Bitmap result = null;
        Cursor cursor = getActivity().getContentResolver().query(contactUri, null, null, null,
                null);
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
            String hasPhoto = cursor.getString(idx);
            Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo
                    .CONTENT_DIRECTORY);
            if (hasPhoto != null) {
                try {
                    result = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver()
                            , photoUri);
                } catch (IOException e) {
                    Log.e(TAG, String.format("Failed to load resource. Uri %s", photoUri), e);
                }
            } else {
                Drawable defaultContactDrawable = getActivity().getResources().getDrawable(R
                        .drawable.ic_contact_picture);
                result = ((BitmapDrawable) defaultContactDrawable).getBitmap();
            }
        }
        return result;
    }

    /**
     * Updates the Contact information on the screen when a contact is picked.
     *
     * @param contactUri The Uri from which the contact is retrieved.
     */
    private void updateContactEntryFromUri(Uri contactUri) {
        Cursor cursor = getActivity().getContentResolver().query(contactUri, null, null, null,
                null);
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            String name = cursor.getString(idx);
            idx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
            String hasPhoto = cursor.getString(idx);

            Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo
                    .CONTENT_DIRECTORY);
            ImageView contactPhoto = (ImageView) getActivity().findViewById(R.id.contact_photo);
            if (hasPhoto != null) {
                contactPhoto.setImageURI(photoUri);
            } else {
                Drawable defaultContactDrawable = getActivity().getResources().getDrawable(R
                        .drawable.ic_contact_picture);
                contactPhoto.setImageDrawable(defaultContactDrawable);
            }
            TextView contactName = (TextView) getActivity().findViewById(R.id.contact_name);
            contactName.setText(name);

            getActivity().findViewById(R.id.contact_entry).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.attach_person).setVisibility(View.GONE);
            getActivity().findViewById(R.id.click_to_change).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    findContact();
                }
            });
            Log.i(TAG, String.format("Contact updated. Name %s, PhotoUri %s", name, photoUri));
        }
    }

    /**
     * Enum indicating possible categories in {@link Notification} used from
     * {@link #mCategorySpinner}.
     */
    //@VisibleForTesting
    static enum Category {
        ALARM("alarm"),
        CALL("call"),
        EMAIL("email"),
        ERROR("err"),
        EVENT("event"),
        MESSAGE("msg"),
        PROGRESS("progress"),
        PROMO("promo"),
        RECOMMENDATION("recommendation"),
        SERVICE("service"),
        SOCIAL("social"),
        STATUS("status"),
        SYSTEM("sys"),
        TRANSPORT("transport");

        private final String value;

        Category(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Enum indicating possible priorities in {@link Notification} used from
     * {@link #mPrioritySpinner}.
     */
    //@VisibleForTesting
    static enum Priority {
        DEFAULT(Notification.PRIORITY_DEFAULT),
        MAX(Notification.PRIORITY_MAX),
        HIGH(Notification.PRIORITY_HIGH),
        LOW(Notification.PRIORITY_LOW),
        MIN(Notification.PRIORITY_MIN);

        private final int value;

        Priority(int value) {
            this.value = value;
        }
    }
}
