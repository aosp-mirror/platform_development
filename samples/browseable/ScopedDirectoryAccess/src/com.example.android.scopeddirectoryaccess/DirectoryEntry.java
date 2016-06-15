/*
* Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.scopeddirectoryaccess;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Entity class that represents a directory entry.
 */
public class DirectoryEntry implements Parcelable {
    public String fileName;
    public String mimeType;

    public DirectoryEntry() {}

    protected DirectoryEntry(Parcel in) {
        fileName = in.readString();
        mimeType = in.readString();
    }

    public static final Creator<DirectoryEntry> CREATOR = new Creator<DirectoryEntry>() {
        @Override
        public DirectoryEntry createFromParcel(Parcel in) {
            return new DirectoryEntry(in);
        }

        @Override
        public DirectoryEntry[] newArray(int size) {
            return new DirectoryEntry[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(fileName);
        parcel.writeString(mimeType);
    }
}

