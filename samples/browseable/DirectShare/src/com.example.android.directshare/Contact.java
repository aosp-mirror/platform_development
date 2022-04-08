/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.directshare;

/**
 * Provides the list of dummy contacts. This sample implements this as constants, but real-life apps
 * should use a database and such.
 */
public class Contact {

    /**
     * The list of dummy contacts.
     */
    public static final Contact[] CONTACTS = {
            new Contact("Tereasa"),
            new Contact("Chang"),
            new Contact("Kory"),
            new Contact("Clare"),
            new Contact("Landon"),
            new Contact("Kyle"),
            new Contact("Deana"),
            new Contact("Daria"),
            new Contact("Melisa"),
            new Contact("Sammie"),
    };

    /**
     * The contact ID.
     */
    public static final String ID = "contact_id";

    /**
     * Representative invalid contact ID.
     */
    public static final int INVALID_ID = -1;

    /**
     * The name of this contact.
     */
    private final String mName;

    /**
     * Instantiates a new {@link Contact}.
     *
     * @param name The name of the contact.
     */
    public Contact(String name) {
        mName = name;
    }

    /**
     * Finds a {@link Contact} specified by a contact ID.
     *
     * @param id The contact ID. This needs to be a valid ID.
     * @return A {@link Contact}
     */
    public static Contact byId(int id) {
        return CONTACTS[id];
    }

    /**
     * Gets the name of this contact.
     *
     * @return The name of this contact.
     */
    public String getName() {
        return mName;
    }

    /**
     * Gets the icon of this contact.
     *
     * @return The icon.
     */
    public int getIcon() {
        return R.mipmap.logo_avatar;
    }

}
