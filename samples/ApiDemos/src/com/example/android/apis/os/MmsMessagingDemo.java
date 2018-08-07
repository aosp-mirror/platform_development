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

package com.example.android.apis.os;

import com.example.android.mmslib.ContentType;
import com.example.android.mmslib.InvalidHeaderValueException;
import com.example.android.mmslib.pdu.CharacterSets;
import com.example.android.mmslib.pdu.EncodedStringValue;
import com.example.android.mmslib.pdu.GenericPdu;
import com.example.android.mmslib.pdu.PduBody;
import com.example.android.mmslib.pdu.PduComposer;
import com.example.android.mmslib.pdu.PduHeaders;
import com.example.android.mmslib.pdu.PduParser;
import com.example.android.mmslib.pdu.PduPart;
import com.example.android.mmslib.pdu.RetrieveConf;
import com.example.android.mmslib.pdu.SendConf;
import com.example.android.mmslib.pdu.SendReq;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.example.android.apis.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

public class MmsMessagingDemo extends Activity {
    private static final String TAG = "MmsMessagingDemo";

    public static final String EXTRA_NOTIFICATION_URL = "notification_url";

    private static final String ACTION_MMS_SENT = "com.example.android.apis.os.MMS_SENT_ACTION";
    private static final String ACTION_MMS_RECEIVED =
            "com.example.android.apis.os.MMS_RECEIVED_ACTION";

    private EditText mRecipientsInput;
    private EditText mSubjectInput;
    private EditText mTextInput;
    private TextView mSendStatusView;
    private Button mSendButton;
    private File mSendFile;
    private File mDownloadFile;
    private Random mRandom = new Random();

    private BroadcastReceiver mSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleSentResult(getResultCode(), intent);
        }
    };
    private IntentFilter mSentFilter = new IntentFilter(ACTION_MMS_SENT);

    private BroadcastReceiver mReceivedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleReceivedResult(context, getResultCode(), intent);
        }
    };
    private IntentFilter mReceivedFilter = new IntentFilter(ACTION_MMS_RECEIVED);

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final String notificationIndUrl = intent.getStringExtra(EXTRA_NOTIFICATION_URL);
        if (!TextUtils.isEmpty(notificationIndUrl)) {
            downloadMessage(notificationIndUrl);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mms_demo);

        // Enable or disable the broadcast receiver depending on the checked
        // state of the checkbox.
        final CheckBox enableCheckBox = (CheckBox) findViewById(R.id.mms_enable_receiver);
        final PackageManager pm = this.getPackageManager();
        final ComponentName componentName = new ComponentName("com.example.android.apis",
                "com.example.android.apis.os.MmsWapPushReceiver");
        enableCheckBox.setChecked(pm.getComponentEnabledSetting(componentName) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        enableCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, (isChecked ? "Enabling" : "Disabling") + " MMS receiver");
                pm.setComponentEnabledSetting(componentName,
                        isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        });

        mRecipientsInput = (EditText) findViewById(R.id.mms_recipients_input);
        mSubjectInput = (EditText) findViewById(R.id.mms_subject_input);
        mTextInput = (EditText) findViewById(R.id.mms_text_input);
        mSendStatusView = (TextView) findViewById(R.id.mms_send_status);
        mSendButton = (Button) findViewById(R.id.mms_send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(
                        mRecipientsInput.getText().toString(),
                        mSubjectInput.getText().toString(),
                        mTextInput.getText().toString());
            }
        });
        registerReceiver(mSentReceiver, mSentFilter);
        registerReceiver(mReceivedReceiver, mReceivedFilter);
        final Intent intent = getIntent();
        final String notificationIndUrl = intent.getStringExtra(EXTRA_NOTIFICATION_URL);
        if (!TextUtils.isEmpty(notificationIndUrl)) {
            downloadMessage(notificationIndUrl);
        }
    }

    private void sendMessage(final String recipients, final String subject, final String text) {
        Log.d(TAG, "Sending");
        mSendStatusView.setText(getResources().getString(R.string.mms_status_sending));
        mSendButton.setEnabled(false);
        final String fileName = "send." + String.valueOf(Math.abs(mRandom.nextLong())) + ".dat";
        mSendFile = new File(getCacheDir(), fileName);

        // Making RPC call in non-UI thread
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                final byte[] pdu = buildPdu(MmsMessagingDemo.this, recipients, subject, text);
                Uri writerUri = (new Uri.Builder())
                       .authority("com.example.android.apis.os.MmsFileProvider")
                       .path(fileName)
                       .scheme(ContentResolver.SCHEME_CONTENT)
                       .build();
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        MmsMessagingDemo.this, 0, new Intent(ACTION_MMS_SENT), 0);
                FileOutputStream writer = null;
                Uri contentUri = null;
                try {
                    writer = new FileOutputStream(mSendFile);
                    writer.write(pdu);
                    contentUri = writerUri;
                } catch (final IOException e) {
                    Log.e(TAG, "Error writing send file", e);
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                        }
                    }
                }

                if (contentUri != null) {
                    SmsManager.getDefault().sendMultimediaMessage(getApplicationContext(),
                            contentUri, null/*locationUrl*/, null/*configOverrides*/,
                            pendingIntent);
                } else {
                    Log.e(TAG, "Error writing sending Mms");
                    try {
                        pendingIntent.send(SmsManager.MMS_ERROR_IO_ERROR);
                    } catch (CanceledException ex) {
                        Log.e(TAG, "Mms pending intent cancelled?", ex);
                    }
                }
            }
        });
    }

    private void downloadMessage(final String locationUrl) {
        Log.d(TAG, "Downloading " + locationUrl);
        mSendStatusView.setText(getResources().getString(R.string.mms_status_downloading));
        mSendButton.setEnabled(false);
        mRecipientsInput.setText("");
        mSubjectInput.setText("");
        mTextInput.setText("");
        final String fileName = "download." + String.valueOf(Math.abs(mRandom.nextLong())) + ".dat";
        mDownloadFile = new File(getCacheDir(), fileName);
        // Making RPC call in non-UI thread
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Uri contentUri = (new Uri.Builder())
                        .authority("com.example.android.apis.os.MmsFileProvider")
                        .path(fileName)
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .build();
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        MmsMessagingDemo.this, 0, new Intent(ACTION_MMS_RECEIVED), 0);
                SmsManager.getDefault().downloadMultimediaMessage(getApplicationContext(),
                        locationUrl, contentUri, null/*configOverrides*/, pendingIntent);
            }
        });
    }

    private void handleSentResult(int code, Intent intent) {
        mSendFile.delete();
        int status = R.string.mms_status_failed;
        if (code == Activity.RESULT_OK) {
            final byte[] response = intent.getByteArrayExtra(SmsManager.EXTRA_MMS_DATA);
            if (response != null) {
                final GenericPdu pdu = new PduParser(
                        response, PduParserUtil.shouldParseContentDisposition()).parse();
                if (pdu instanceof SendConf) {
                    final SendConf sendConf = (SendConf) pdu;
                    if (sendConf.getResponseStatus() == PduHeaders.RESPONSE_STATUS_OK) {
                        status = R.string.mms_status_sent;
                    } else {
                        Log.e(TAG, "MMS sent, error=" + sendConf.getResponseStatus());
                    }
                } else {
                    Log.e(TAG, "MMS sent, invalid response");
                }
            } else {
                Log.e(TAG, "MMS sent, empty response");
            }
        } else {
            Log.e(TAG, "MMS not sent, error=" + code);
        }

        mSendFile = null;
        mSendStatusView.setText(status);
        mSendButton.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSentReceiver != null) {
            unregisterReceiver(mSentReceiver);
        }
        if (mReceivedReceiver != null) {
            unregisterReceiver(mReceivedReceiver);
        }
    }

    private void handleReceivedResult(Context context, int code, Intent intent) {
        int status = R.string.mms_status_failed;
        if (code == Activity.RESULT_OK) {
            try {
                final int nBytes = (int) mDownloadFile.length();
                FileInputStream reader = new FileInputStream(mDownloadFile);
                final byte[] response = new byte[nBytes];
                final int read = reader.read(response, 0, nBytes);
                if (read == nBytes) {
                    final GenericPdu pdu = new PduParser(
                            response, PduParserUtil.shouldParseContentDisposition()).parse();
                    if (pdu instanceof RetrieveConf) {
                        final RetrieveConf retrieveConf = (RetrieveConf) pdu;
                        mRecipientsInput.setText(getRecipients(context, retrieveConf));
                        mSubjectInput.setText(getSubject(retrieveConf));
                        mTextInput.setText(getMessageText(retrieveConf));
                        status = R.string.mms_status_downloaded;
                    } else {
                        Log.e(TAG, "MMS received, invalid response");
                    }
                } else {
                    Log.e(TAG, "MMS received, empty response");
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "MMS received, file not found exception", e);
            } catch (IOException e) {
                Log.e(TAG, "MMS received, io exception", e);
            } finally {
                mDownloadFile.delete();
            }
        } else {
            Log.e(TAG, "MMS not received, error=" + code);
        }
        mDownloadFile = null;
        mSendStatusView.setText(status);
        mSendButton.setEnabled(true);
    }

    public static final long DEFAULT_EXPIRY_TIME = 7 * 24 * 60 * 60;
    public static final int DEFAULT_PRIORITY = PduHeaders.PRIORITY_NORMAL;

    private static final String TEXT_PART_FILENAME = "text_0.txt";
    private static final String sSmilText =
            "<smil>" +
                "<head>" +
                    "<layout>" +
                        "<root-layout/>" +
                        "<region height=\"100%%\" id=\"Text\" left=\"0%%\" top=\"0%%\" width=\"100%%\"/>" +
                    "</layout>" +
                "</head>" +
                "<body>" +
                    "<par dur=\"8000ms\">" +
                        "<text src=\"%s\" region=\"Text\"/>" +
                    "</par>" +
                "</body>" +
            "</smil>";

    private static byte[] buildPdu(Context context, String recipients, String subject,
            String text) {
        final SendReq req = new SendReq();
        // From, per spec
        final String lineNumber = getSimNumber(context);
        if (!TextUtils.isEmpty(lineNumber)) {
            req.setFrom(new EncodedStringValue(lineNumber));
        }
        // To
        EncodedStringValue[] encodedNumbers =
                EncodedStringValue.encodeStrings(recipients.split(" "));
        if (encodedNumbers != null) {
            req.setTo(encodedNumbers);
        }
        // Subject
        if (!TextUtils.isEmpty(subject)) {
            req.setSubject(new EncodedStringValue(subject));
        }
        // Date
        req.setDate(System.currentTimeMillis() / 1000);
        // Body
        PduBody body = new PduBody();
        // Add text part. Always add a smil part for compatibility, without it there
        // may be issues on some carriers/client apps
        final int size = addTextPart(body, text, true/* add text smil */);
        req.setBody(body);
        // Message size
        req.setMessageSize(size);
        // Message class
        req.setMessageClass(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes());
        // Expiry
        req.setExpiry(DEFAULT_EXPIRY_TIME);
        try {
            // Priority
            req.setPriority(DEFAULT_PRIORITY);
            // Delivery report
            req.setDeliveryReport(PduHeaders.VALUE_NO);
            // Read report
            req.setReadReport(PduHeaders.VALUE_NO);
        } catch (InvalidHeaderValueException e) {}

        return new PduComposer(context, req).make();
    }

    private static int addTextPart(PduBody pb, String message, boolean addTextSmil) {
        final PduPart part = new PduPart();
        // Set Charset if it's a text media.
        part.setCharset(CharacterSets.UTF_8);
        // Set Content-Type.
        part.setContentType(ContentType.TEXT_PLAIN.getBytes());
        // Set Content-Location.
        part.setContentLocation(TEXT_PART_FILENAME.getBytes());
        int index = TEXT_PART_FILENAME.lastIndexOf(".");
        String contentId = (index == -1) ? TEXT_PART_FILENAME
                : TEXT_PART_FILENAME.substring(0, index);
        part.setContentId(contentId.getBytes());
        part.setData(message.getBytes());
        pb.addPart(part);
        if (addTextSmil) {
            final String smil = String.format(sSmilText, TEXT_PART_FILENAME);
            addSmilPart(pb, smil);
        }
        return part.getData().length;
    }

    private static void addSmilPart(PduBody pb, String smil) {
        final PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(smil.getBytes());
        pb.addPart(0, smilPart);
    }

    private static String getRecipients(Context context, RetrieveConf retrieveConf) {
        final String self = getSimNumber(context);
        final StringBuilder sb = new StringBuilder();
        if (retrieveConf.getFrom() != null) {
            sb.append(retrieveConf.getFrom().getString());
        }
        if (retrieveConf.getTo() != null) {
            for (EncodedStringValue to : retrieveConf.getTo()) {
                final String number = to.getString();
                if (!PhoneNumberUtils.compare(number, self)) {
                    sb.append(" ").append(to.getString());
                }
            }
        }
        if (retrieveConf.getCc() != null) {
            for (EncodedStringValue cc : retrieveConf.getCc()) {
                final String number = cc.getString();
                if (!PhoneNumberUtils.compare(number, self)) {
                    sb.append(" ").append(cc.getString());
                }
            }
        }
        return sb.toString();
    }

    private static String getSubject(RetrieveConf retrieveConf) {
        final EncodedStringValue subject = retrieveConf.getSubject();
        return subject != null ? subject.getString() : "";
    }

    private static String getMessageText(RetrieveConf retrieveConf) {
        final StringBuilder sb = new StringBuilder();
        final PduBody body = retrieveConf.getBody();
        if (body != null) {
            for (int i = 0; i < body.getPartsNum(); i++) {
                final PduPart part = body.getPart(i);
                if (part != null
                        && part.getContentType() != null
                        && ContentType.isTextType(new String(part.getContentType()))) {
                    sb.append(new String(part.getData()));
                }
            }
        }
        return sb.toString();
    }

    private static String getSimNumber(Context context) {
        final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        return telephonyManager.getLine1Number();
    }
}
