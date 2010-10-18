/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apps.tag.record;

import com.android.apps.tag.R;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Bytes;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.nfc.NdefRecord;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.charset.Charsets;
import java.util.Arrays;
import java.util.List;

/**
 * A parsed record containing a Uri.
 */
public class UriRecord implements ParsedNdefRecord, OnClickListener {
    private static final class ClickInfo {
        public Activity activity;
        public Intent intent;
        
        public ClickInfo(Activity activity, Intent intent) {
            this.activity = activity;
            this.intent = intent;
        }
    }
    
    /**
     * NFC Forum "URI Record Type Definition"
     *
     * This is a mapping of "URI Identifier Codes" to URI string prefixes,
     * per section 3.2.2 of the NFC Forum URI Record Type Definition document.
     */
    private static final BiMap<Byte, String> URI_PREFIX_MAP = ImmutableBiMap.<Byte, String>builder()
            .put((byte) 0x00, "")
            .put((byte) 0x01, "http://www.")
            .put((byte) 0x02, "https://www.")
            .put((byte) 0x03, "http://")
            .put((byte) 0x04, "https://")
            .put((byte) 0x05, "tel:")
            .put((byte) 0x06, "mailto:")
            .put((byte) 0x07, "ftp://anonymous:anonymous@")
            .put((byte) 0x08, "ftp://ftp.")
            .put((byte) 0x09, "ftps://")
            .put((byte) 0x0A, "sftp://")
            .put((byte) 0x0B, "smb://")
            .put((byte) 0x0C, "nfs://")
            .put((byte) 0x0D, "ftp://")
            .put((byte) 0x0E, "dav://")
            .put((byte) 0x0F, "news:")
            .put((byte) 0x10, "telnet://")
            .put((byte) 0x11, "imap:")
            .put((byte) 0x12, "rtsp://")
            .put((byte) 0x13, "urn:")
            .put((byte) 0x14, "pop:")
            .put((byte) 0x15, "sip:")
            .put((byte) 0x16, "sips:")
            .put((byte) 0x17, "tftp:")
            .put((byte) 0x18, "btspp://")
            .put((byte) 0x19, "btl2cap://")
            .put((byte) 0x1A, "btgoep://")
            .put((byte) 0x1B, "tcpobex://")
            .put((byte) 0x1C, "irdaobex://")
            .put((byte) 0x1D, "file://")
            .put((byte) 0x1E, "urn:epc:id:")
            .put((byte) 0x1F, "urn:epc:tag:")
            .put((byte) 0x20, "urn:epc:pat:")
            .put((byte) 0x21, "urn:epc:raw:")
            .put((byte) 0x22, "urn:epc:")
            .put((byte) 0x23, "urn:nfc:")
            .build();

    private final Uri mUri;

    private UriRecord(Uri uri) {
        this.mUri = Preconditions.checkNotNull(uri);
    }

    @Override
    public String getRecordType() {
        return "Uri";
    }

    public Intent getIntentForUri() {
        String scheme = mUri.getScheme();
        if ("tel".equals(scheme)) {
            return new Intent(Intent.ACTION_CALL, mUri);
        } else if ("sms".equals(scheme) || "smsto".equals(scheme)) {
            return new Intent(Intent.ACTION_SENDTO, mUri);
        } else {
            return new Intent(Intent.ACTION_VIEW, mUri);
        }
    }

    public String getPrettyUriString(Context context) {
        String scheme = mUri.getScheme();
        boolean tel = "tel".equals(scheme);
        boolean sms = "sms".equals(scheme) || "smsto".equals(scheme); 
        if (tel || sms) {
            String ssp = mUri.getSchemeSpecificPart();
            int offset = ssp.indexOf('?');
            if (offset >= 0) {
                ssp = ssp.substring(0, offset);
            }
            if (tel) {
                return context.getString(R.string.action_call, PhoneNumberUtils.formatNumber(ssp));
            } else {
                return context.getString(R.string.action_text, PhoneNumberUtils.formatNumber(ssp));
            }
        } else {
            return mUri.toString();
        }
    }

    @Override
    public View getView(Activity activity, LayoutInflater inflater, ViewGroup parent) {
        Intent intent = getIntentForUri();
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        int numActivities = activities.size();
        if (numActivities == 0) {
            TextView text = (TextView) inflater.inflate(R.layout.tag_text, parent, false);
            text.setText(mUri.toString());
            return text;
        } else if (numActivities == 1) {
            return buildActivityView(activity, activities.get(0), pm, inflater, parent);
        } else {
            // Build a container to hold the multiple entries
            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            // Create an entry for each activity that can handle the URI
            for (ResolveInfo resolveInfo : activities) {
                if (container.getChildCount() > 0) {
                    inflater.inflate(R.layout.tag_divider, container);
                }
                container.addView(buildActivityView(activity, resolveInfo, pm, inflater, container));
            }
            return container;
        }
    }

    private View buildActivityView(Activity activity, ResolveInfo resolveInfo, PackageManager pm,
            LayoutInflater inflater, ViewGroup parent) {
        Intent intent = getIntentForUri();
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        intent.setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));

        View item = inflater.inflate(R.layout.tag_uri, parent, false);
        item.setOnClickListener(this);
        item.setTag(new ClickInfo(activity, intent));

        ImageView icon = (ImageView) item.findViewById(R.id.icon);
        icon.setImageDrawable(resolveInfo.loadIcon(pm));

        TextView text = (TextView) item.findViewById(R.id.secondary);
        text.setText(resolveInfo.loadLabel(pm));

        text = (TextView) item.findViewById(R.id.primary);
        text.setText(getPrettyUriString(activity));

        return item;
    }

    @Override
    public void onClick(View view) {
        ClickInfo info = (ClickInfo) view.getTag();
        info.activity.startActivity(info.intent);
        info.activity.finish();
    }

    public Uri getUri() {
        return mUri;
    }

    /**
     * Convert {@link android.nfc.NdefRecord} into a {@link android.net.Uri}.
     *
     * TODO: This class does not handle NdefRecords where the TNF
     * (Type Name Format) of the class is {@link android.nfc.NdefRecord#TNF_ABSOLUTE_URI}.
     * This should be fixed.
     *
     * @throws IllegalArgumentException if the NdefRecord is not a
     *     record containing a URI.
     */
    public static UriRecord parse(NdefRecord record) {
        Preconditions.checkArgument(record.getTnf() == NdefRecord.TNF_WELL_KNOWN);
        Preconditions.checkArgument(Arrays.equals(record.getType(), NdefRecord.RTD_URI));

        byte[] payload = record.getPayload();

        /*
         * payload[0] contains the URI Identifier Code, per the
         * NFC Forum "URI Record Type Definition" section 3.2.2.
         *
         * payload[1]...payload[payload.length - 1] contains the rest of
         * the URI.
         */

        String prefix = URI_PREFIX_MAP.get(payload[0]);
        byte[] fullUri = Bytes.concat(
                prefix.getBytes(Charsets.UTF_8),
                Arrays.copyOfRange(payload, 1, payload.length));

        return new UriRecord(Uri.parse(new String(fullUri, Charsets.UTF_8)));
    }

    public static boolean isUri(NdefRecord record) {
        try {
            parse(record);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
