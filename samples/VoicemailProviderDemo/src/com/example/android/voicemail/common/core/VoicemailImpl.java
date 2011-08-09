/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.voicemail.common.core;

import android.net.Uri;

/**
 * A simple immutable data object to represent a voicemail.
 */
public final class VoicemailImpl implements Voicemail {
    private final Long mTimestamp;
    private final String mNumber;
    private final Long mId;
    private final Long mDuration;
    private final String mSource;
    private final String mProviderData;
    private final Uri mUri;
    private final Boolean mIsRead;
    private final boolean mHasContent;

    // TODO: 5. We should probably consider changing "number" everywhere to "contact", given that
    // it's not clear that these will be restricted to telephone numbers.

    private VoicemailImpl(
            Long timestamp,
            String number,
            Long id,
            Long duration,
            String source,
            String providerData,
            Uri uri,
            Boolean isRead,
            boolean hasContent) {
        mId = id;
        mNumber = number;
        mDuration = duration;
        mTimestamp = timestamp;
        mSource = source;
        mProviderData = providerData;
        mUri = uri;
        mIsRead = isRead;
        mHasContent = hasContent;
    }

    /**
     * Create a {@link Builder} for a new {@link Voicemail} to be inserted.
     * <p>
     * The number and the timestamp are mandatory for insertion.
     */
    public static Builder createForInsertion(long timestamp, String number) {
        return new Builder().setNumber(number).setTimestamp(timestamp);
    }

    /**
     * Create a {@link Builder} for updating a {@link Voicemail}.
     * <p>
     * Only the id of the voicemail to be updated is mandatory.
     */
    public static Builder createForUpdate(long id) {
        return new Builder().setId(id);
    }

    /**
     * Create a {@link Builder} for a new {@link Voicemail}, such as one suitable for returning from
     * a list of results or creating from scratch.
     */
    public static Builder createEmptyBuilder() {
        return new Builder();
    }

    /**
     * Builder pattern for creating a {@link VoicemailImpl}.
     * <p>
     * All fields are optional, and can be set with the various {@code setXXX} methods.
     * <p>
     * This class is <b>not thread safe</b>
     */
    public static class Builder {
        private Long mBuilderTimestamp;
        private String mBuilderNumber;
        private Long mBuilderId;
        private Long mBuilderDuration;
        private String mBuilderSourcePackage;
        private String mBuilderSourceData;
        private Uri mBuilderUri;
        private Boolean mBuilderIsRead;
        private boolean mBuilderHasContent;

        /** You should use the correct factory method to construct a builder. */
        private Builder() {
        }

        public Builder setNumber(String number) {
            mBuilderNumber = number;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            mBuilderTimestamp = timestamp;
            return this;
        }

        public Builder setId(long id) {
            mBuilderId = id;
            return this;
        }

        public Builder setDuration(long duration) {
            mBuilderDuration = duration;
            return this;
        }

        public Builder setSourcePackage(String sourcePackage) {
            mBuilderSourcePackage = sourcePackage;
            return this;
        }

        public Builder setSourceData(String sourceData) {
            mBuilderSourceData = sourceData;
            return this;
        }

        public Builder setUri(Uri uri) {
            mBuilderUri = uri;
            return this;
        }

        public Builder setIsRead(boolean isRead) {
            mBuilderIsRead = isRead;
            return this;
        }

        public Builder setHasContent(boolean hasContent) {
            mBuilderHasContent = hasContent;
            return this;
        }

        public VoicemailImpl build() {
            return new VoicemailImpl(mBuilderTimestamp, mBuilderNumber, mBuilderId,
                    mBuilderDuration,
                    mBuilderSourcePackage, mBuilderSourceData, mBuilderUri,
                    mBuilderIsRead,
                    mBuilderHasContent);
        }
    }

    @Override
    public long getId() {
        return hasId() ? mId : -1;
    }

    @Override
    public boolean hasId() {
        return mId != null;
    }

    @Override
    public String getNumber() {
        return mNumber;
    }

    @Override
    public boolean hasNumber() {
        return mNumber != null;
    }

    @Override
    public long getTimestampMillis() {
        return hasTimestampMillis() ? mTimestamp : 0;
    }

    @Override
    public boolean hasTimestampMillis() {
        return mTimestamp != null;
    }

    @Override
    public long getDuration() {
        return hasDuration() ? mDuration : 0;
    }

    @Override
    public boolean hasDuration() {
        return mDuration != null;
    }

    @Override
    public String getSourcePackage() {
        return mSource;
    }

    @Override
    public boolean hasSourcePackage() {
        return mSource != null;
    }

    @Override
    public String getSourceData() {
        return mProviderData;
    }

    @Override
    public boolean hasSourceData() {
        return mProviderData != null;
    }

    @Override
    public Uri getUri() {
        return mUri;
    }

    @Override
    public boolean hasUri() {
        return mUri != null;
    }

    @Override
    public boolean isRead() {
        return hasRead() ? mIsRead : false;
    }

    @Override
    public boolean hasRead() {
        return mIsRead != null;
    }

    @Override
    public boolean hasContent() {
        return mHasContent;
    }

    @Override
    public String toString() {
        return "VoicemailImpl [mTimestamp=" + mTimestamp + ", mNumber=" + mNumber + ", mId=" + mId
                + ", mDuration=" + mDuration + ", mSource=" + mSource + ", mProviderData="
                + mProviderData + ", mUri=" + mUri + ", mIsRead=" + mIsRead + ", mHasContent="
                + mHasContent + "]";
    }
}
