/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib;

import com.android.ddmlib.Device;
import com.android.ddmlib.EmulatorConsole;
import com.android.ddmlib.EmulatorConsole.GsmMode;
import com.android.ddmlib.EmulatorConsole.GsmStatus;
import com.android.ddmlib.EmulatorConsole.NetworkStatus;
import com.android.ddmuilib.location.CoordinateControls;
import com.android.ddmuilib.location.GpxParser;
import com.android.ddmuilib.location.KmlParser;
import com.android.ddmuilib.location.TrackContentProvider;
import com.android.ddmuilib.location.TrackLabelProvider;
import com.android.ddmuilib.location.TrackPoint;
import com.android.ddmuilib.location.WayPoint;
import com.android.ddmuilib.location.WayPointContentProvider;
import com.android.ddmuilib.location.WayPointLabelProvider;
import com.android.ddmuilib.location.GpxParser.Track;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * Panel to control the emulator using EmulatorConsole objects.
 */
public class EmulatorControlPanel extends SelectionDependentPanel {

    // default location: Patio outside Charlie's
    private final static double DEFAULT_LONGITUDE = -122.084095;
    private final static double DEFAULT_LATITUDE = 37.422006;
    
    private final static String SPEED_FORMAT = "Speed: %1$dX";


    /**
     * Map between the display gsm mode and the internal tag used by the display.
     */
    private final static String[][] GSM_MODES = new String[][] {
        { "unregistered", GsmMode.UNREGISTERED.getTag() },
        { "home", GsmMode.HOME.getTag() },
        { "roaming", GsmMode.ROAMING.getTag() },
        { "searching", GsmMode.SEARCHING.getTag() },
        { "denied", GsmMode.DENIED.getTag() },
    };

    private final static String[] NETWORK_SPEEDS = new String[] {
        "Full",
        "GSM",
        "HSCSD",
        "GPRS",
        "EDGE",
        "UMTS",
        "HSDPA",
    };

    private final static String[] NETWORK_LATENCIES = new String[] {
        "None",
        "GPRS",
        "EDGE",
        "UMTS",
    };
    
    private final static int[] PLAY_SPEEDS = new int[] { 1, 2, 5, 10, 20, 50 };

    private final static String RE_PHONE_NUMBER = "^[+#0-9]+$"; //$NON-NLS-1$
    private final static String PREFS_WAYPOINT_COL_NAME = "emulatorControl.waypoint.name"; //$NON-NLS-1$
    private final static String PREFS_WAYPOINT_COL_LONGITUDE = "emulatorControl.waypoint.longitude"; //$NON-NLS-1$
    private final static String PREFS_WAYPOINT_COL_LATITUDE = "emulatorControl.waypoint.latitude"; //$NON-NLS-1$
    private final static String PREFS_WAYPOINT_COL_ELEVATION = "emulatorControl.waypoint.elevation"; //$NON-NLS-1$
    private final static String PREFS_WAYPOINT_COL_DESCRIPTION = "emulatorControl.waypoint.desc"; //$NON-NLS-1$
    private final static String PREFS_TRACK_COL_NAME = "emulatorControl.track.name"; //$NON-NLS-1$
    private final static String PREFS_TRACK_COL_COUNT = "emulatorControl.track.count"; //$NON-NLS-1$
    private final static String PREFS_TRACK_COL_FIRST = "emulatorControl.track.first"; //$NON-NLS-1$
    private final static String PREFS_TRACK_COL_LAST = "emulatorControl.track.last"; //$NON-NLS-1$
    private final static String PREFS_TRACK_COL_COMMENT = "emulatorControl.track.comment"; //$NON-NLS-1$

    private IImageLoader mImageLoader;

    private EmulatorConsole mEmulatorConsole;

    private Composite mParent;

    private Label mVoiceLabel;
    private Combo mVoiceMode;
    private Label mDataLabel;
    private Combo mDataMode;
    private Label mSpeedLabel;
    private Combo mNetworkSpeed;
    private Label mLatencyLabel;
    private Combo mNetworkLatency;

    private Label mNumberLabel;
    private Text mPhoneNumber;

    private Button mVoiceButton;
    private Button mSmsButton;

    private Label mMessageLabel;
    private Text mSmsMessage;

    private Button mCallButton;
    private Button mCancelButton;

    private TabFolder mLocationFolders;
    
    private Button mDecimalButton;
    private Button mSexagesimalButton;
    private CoordinateControls mLongitudeControls;
    private CoordinateControls mLatitudeControls;
    private Button mGpxUploadButton;
    private Table mGpxWayPointTable;
    private Table mGpxTrackTable;
    private Button mKmlUploadButton;
    private Table mKmlWayPointTable;

    private Button mPlayGpxButton;
    private Button mGpxBackwardButton;
    private Button mGpxForwardButton;
    private Button mGpxSpeedButton;
    private Button mPlayKmlButton;
    private Button mKmlBackwardButton;
    private Button mKmlForwardButton;
    private Button mKmlSpeedButton;

    private Image mPlayImage;
    private Image mPauseImage;

    private Thread mPlayingThread;
    private boolean mPlayingTrack;
    private int mPlayDirection = 1;
    private int mSpeed;
    private int mSpeedIndex;
    
    private final SelectionAdapter mDirectionButtonAdapter = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
            Button b = (Button)e.getSource();
            if (b.getSelection() == false) {
                // basically the button was unselected, which we don't allow.
                // so we reselect it.
                b.setSelection(true);
                return;
            }
            
            // now handle selection change.
            if (b == mGpxForwardButton || b == mKmlForwardButton) {
                mGpxBackwardButton.setSelection(false);
                mGpxForwardButton.setSelection(true);
                mKmlBackwardButton.setSelection(false);
                mKmlForwardButton.setSelection(true);
                mPlayDirection = 1;
                
            } else {
                mGpxBackwardButton.setSelection(true);
                mGpxForwardButton.setSelection(false);
                mKmlBackwardButton.setSelection(true);
                mKmlForwardButton.setSelection(false);
                mPlayDirection = -1;
            }
        }
    };
    
    private final SelectionAdapter mSpeedButtonAdapter = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
            mSpeedIndex = (mSpeedIndex+1) % PLAY_SPEEDS.length;
            mSpeed = PLAY_SPEEDS[mSpeedIndex];
            
            mGpxSpeedButton.setText(String.format(SPEED_FORMAT, mSpeed));
            mGpxPlayControls.pack();
            mKmlSpeedButton.setText(String.format(SPEED_FORMAT, mSpeed));
            mKmlPlayControls.pack();
            
            if (mPlayingThread != null) {
                mPlayingThread.interrupt();
            }
        } 
     };
    private Composite mKmlPlayControls;
    private Composite mGpxPlayControls;

    
    public EmulatorControlPanel(IImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    /**
     * Sent when a new device is selected. The new device can be accessed
     * with {@link #getCurrentDevice()}
     */
    @Override
    public void deviceSelected() {
        handleNewDevice(getCurrentDevice());
    }

    /**
     * Sent when a new client is selected. The new client can be accessed
     * with {@link #getCurrentClient()}
     */
    @Override
    public void clientSelected() {
        // pass
    }

    /**
     * Creates a control capable of displaying some information.  This is
     * called once, when the application is initializing, from the UI thread.
     */
    @Override
    protected Control createControl(Composite parent) {
        mParent = parent;

        final ScrolledComposite scollingParent = new ScrolledComposite(parent, SWT.V_SCROLL);
        scollingParent.setExpandVertical(true);
        scollingParent.setExpandHorizontal(true);
        scollingParent.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Composite top = new Composite(scollingParent, SWT.NONE);
        scollingParent.setContent(top);
        top.setLayout(new GridLayout(1, false));

        // set the resize for the scrolling to work (why isn't that done automatically?!?)
        scollingParent.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = scollingParent.getClientArea();
                scollingParent.setMinSize(top.computeSize(r.width, SWT.DEFAULT));
            }
        });
        
        createRadioControls(top);

        createCallControls(top);
        
        createLocationControls(top);

        doEnable(false);

        top.layout();
        Rectangle r = scollingParent.getClientArea();
        scollingParent.setMinSize(top.computeSize(r.width, SWT.DEFAULT));

        return scollingParent;
    }

    /**
     * Create Radio (on/off/roaming, for voice/data) controls.
     * @param top
     */
    private void createRadioControls(final Composite top) {
        Group g1 = new Group(top, SWT.NONE);
        g1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        g1.setLayout(new GridLayout(2, false));
        g1.setText("Telephony Status");

        // the inside of the group is 2 composite so that all the column of the controls (mainly
        // combos) have the same width, while not taking the whole screen width
        Composite insideGroup = new Composite(g1, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginBottom = gl.marginHeight = gl.marginLeft = gl.marginRight = 0;
        insideGroup.setLayout(gl);

        mVoiceLabel = new Label(insideGroup, SWT.NONE);
        mVoiceLabel.setText("Voice:");
        mVoiceLabel.setAlignment(SWT.RIGHT);

        mVoiceMode = new Combo(insideGroup, SWT.READ_ONLY);
        mVoiceMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String[] mode : GSM_MODES) {
            mVoiceMode.add(mode[0]);
        }
        mVoiceMode.addSelectionListener(new SelectionAdapter() {
            // called when selection changes
            @Override
            public void widgetSelected(SelectionEvent e) {
                setVoiceMode(mVoiceMode.getSelectionIndex());
            }
        });

        mSpeedLabel = new Label(insideGroup, SWT.NONE);
        mSpeedLabel.setText("Speed:");
        mSpeedLabel.setAlignment(SWT.RIGHT);

        mNetworkSpeed = new Combo(insideGroup, SWT.READ_ONLY);
        mNetworkSpeed.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String mode : NETWORK_SPEEDS) {
            mNetworkSpeed.add(mode);
        }
        mNetworkSpeed.addSelectionListener(new SelectionAdapter() {
            // called when selection changes
            @Override
            public void widgetSelected(SelectionEvent e) {
                setNetworkSpeed(mNetworkSpeed.getSelectionIndex());
            }
        });

        mDataLabel = new Label(insideGroup, SWT.NONE);
        mDataLabel.setText("Data:");
        mDataLabel.setAlignment(SWT.RIGHT);

        mDataMode = new Combo(insideGroup, SWT.READ_ONLY);
        mDataMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String[] mode : GSM_MODES) {
            mDataMode.add(mode[0]);
        }
        mDataMode.addSelectionListener(new SelectionAdapter() {
            // called when selection changes
            @Override
            public void widgetSelected(SelectionEvent e) {
                setDataMode(mDataMode.getSelectionIndex());
            }
        });

        mLatencyLabel = new Label(insideGroup, SWT.NONE);
        mLatencyLabel.setText("Latency:");
        mLatencyLabel.setAlignment(SWT.RIGHT);

        mNetworkLatency = new Combo(insideGroup, SWT.READ_ONLY);
        mNetworkLatency.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String mode : NETWORK_LATENCIES) {
            mNetworkLatency.add(mode);
        }
        mNetworkLatency.addSelectionListener(new SelectionAdapter() {
            // called when selection changes
            @Override
            public void widgetSelected(SelectionEvent e) {
                setNetworkLatency(mNetworkLatency.getSelectionIndex());
            }
        });

        // now an empty label to take the rest of the width of the group
        Label l = new Label(g1, SWT.NONE);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
    
    /**
     * Create Voice/SMS call/hang up controls
     * @param top
     */
    private void createCallControls(final Composite top) {
        GridLayout gl;
        Group g2 = new Group(top, SWT.NONE);
        g2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        g2.setLayout(new GridLayout(1, false));
        g2.setText("Telephony Actions");

        // horizontal composite for label + text field
        Composite phoneComp = new Composite(g2, SWT.NONE);
        phoneComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        gl = new GridLayout(2, false);
        gl.marginBottom = gl.marginHeight = gl.marginLeft = gl.marginRight = 0;
        phoneComp.setLayout(gl);

        mNumberLabel = new Label(phoneComp, SWT.NONE);
        mNumberLabel.setText("Incoming number:");

        mPhoneNumber = new Text(phoneComp, SWT.BORDER | SWT.LEFT | SWT.SINGLE);
        mPhoneNumber.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mPhoneNumber.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                // Reenable the widgets based on the content of the text.
                // doEnable checks the validity of the phone number to enable/disable some
                // widgets.
                // Looks like we're getting a callback at creation time, so we can't
                // suppose that we are enabled when the text is modified...
                doEnable(mEmulatorConsole != null);
            }
        });

        mVoiceButton = new Button(phoneComp, SWT.RADIO);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        mVoiceButton.setText("Voice");
        mVoiceButton.setLayoutData(gd);
        mVoiceButton.setEnabled(false);
        mVoiceButton.setSelection(true);
        mVoiceButton.addSelectionListener(new SelectionAdapter() {
            // called when selection changes
            @Override
            public void widgetSelected(SelectionEvent e) {
                doEnable(true);

                if (mVoiceButton.getSelection()) {
                    mCallButton.setText("Call");
                } else {
                    mCallButton.setText("Send");
                }
            }
        });

        mSmsButton = new Button(phoneComp, SWT.RADIO);
        mSmsButton.setText("SMS");
        gd = new GridData();
        gd.horizontalSpan = 2;
        mSmsButton.setLayoutData(gd);
        mSmsButton.setEnabled(false);
        // Since there are only 2 radio buttons, we can put a listener on only one (they
        // are both called on select and unselect event.

        mMessageLabel = new Label(phoneComp, SWT.NONE);
        gd = new GridData();
        gd.verticalAlignment = SWT.TOP;
        mMessageLabel.setLayoutData(gd);
        mMessageLabel.setText("Message:");
        mMessageLabel.setEnabled(false);

        mSmsMessage = new Text(phoneComp, SWT.BORDER | SWT.LEFT | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        mSmsMessage.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.heightHint = 70;
        mSmsMessage.setEnabled(false);

        // composite to put the 2 buttons horizontally
        Composite g2ButtonComp = new Composite(g2, SWT.NONE);
        g2ButtonComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        gl = new GridLayout(2, false);
        gl.marginWidth = gl.marginHeight = 0;
        g2ButtonComp.setLayout(gl);

        // now a button below the phone number
        mCallButton = new Button(g2ButtonComp, SWT.PUSH);
        mCallButton.setText("Call");
        mCallButton.setEnabled(false);
        mCallButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mEmulatorConsole != null) {
                    if (mVoiceButton.getSelection()) {
                        processCommandResult(mEmulatorConsole.call(mPhoneNumber.getText().trim()));
                    } else {
                        // we need to encode the message. We need to replace the carriage return
                        // character by the 2 character string \n.
                        // Because of this the \ character needs to be escaped as well.
                        // ReplaceAll() expects regexp so \ char are escaped twice.
                        String message = mSmsMessage.getText();
                        message = message.replaceAll("\\\\", //$NON-NLS-1$
                                "\\\\\\\\"); //$NON-NLS-1$

                        // While the normal line delimiter is returned by Text.getLineDelimiter()
                        // it seems copy pasting text coming from somewhere else could have another
                        // delimited. For this reason, we'll replace is several steps

                        // replace the dual CR-LF
                        message = message.replaceAll("\r\n", "\\\\n"); //$NON-NLS-1$ //$NON-NLS-1$

                        // replace remaining stand alone \n
                        message = message.replaceAll("\n", "\\\\n"); //$NON-NLS-1$ //$NON-NLS-1$

                        // replace remaining stand alone \r
                        message = message.replaceAll("\r", "\\\\n"); //$NON-NLS-1$ //$NON-NLS-1$

                        processCommandResult(mEmulatorConsole.sendSms(mPhoneNumber.getText().trim(),
                                message));
                    }
                }
            }
        });

        mCancelButton = new Button(g2ButtonComp, SWT.PUSH);
        mCancelButton.setText("Hang Up");
        mCancelButton.setEnabled(false);
        mCancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mEmulatorConsole != null) {
                    if (mVoiceButton.getSelection()) {
                        processCommandResult(mEmulatorConsole.cancelCall(
                                mPhoneNumber.getText().trim()));
                    }
                }
            }
        });
    }
    
    /**
     * Create Location controls.
     * @param top
     */
    private void createLocationControls(final Composite top) {
        Label l = new Label(top, SWT.NONE);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        l.setText("Location Controls");
        
        mLocationFolders = new TabFolder(top, SWT.NONE);
        mLocationFolders.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Composite manualLocationComp = new Composite(mLocationFolders, SWT.NONE);
        TabItem item = new TabItem(mLocationFolders, SWT.NONE);
        item.setText("Manual");
        item.setControl(manualLocationComp);
        
        createManualLocationControl(manualLocationComp);

        mPlayImage = mImageLoader.loadImage("play.png", mParent.getDisplay()); // $NON-NLS-1$
        mPauseImage = mImageLoader.loadImage("pause.png", mParent.getDisplay()); // $NON-NLS-1$

        Composite gpxLocationComp = new Composite(mLocationFolders, SWT.NONE);
        item = new TabItem(mLocationFolders, SWT.NONE);
        item.setText("GPX");
        item.setControl(gpxLocationComp);
        
        createGpxLocationControl(gpxLocationComp);

        Composite kmlLocationComp = new Composite(mLocationFolders, SWT.NONE);
        kmlLocationComp.setLayout(new FillLayout());
        item = new TabItem(mLocationFolders, SWT.NONE);
        item.setText("KML");
        item.setControl(kmlLocationComp);
        
        createKmlLocationControl(kmlLocationComp);
    }

    private void createManualLocationControl(Composite manualLocationComp) {
        final StackLayout sl;
        GridLayout gl;
        Label label;

        manualLocationComp.setLayout(new GridLayout(1, false));
        mDecimalButton = new Button(manualLocationComp, SWT.RADIO);
        mDecimalButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDecimalButton.setText("Decimal");
        mSexagesimalButton = new Button(manualLocationComp, SWT.RADIO);
        mSexagesimalButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSexagesimalButton.setText("Sexagesimal");

        // composite to hold and switching between the 2 modes.
        final Composite content = new Composite(manualLocationComp, SWT.NONE);
        content.setLayout(sl = new StackLayout());
        
        // decimal display
        final Composite decimalContent = new Composite(content, SWT.NONE);
        decimalContent.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;
        
        mLongitudeControls = new CoordinateControls();
        mLatitudeControls = new CoordinateControls();
        
        label = new Label(decimalContent, SWT.NONE);
        label.setText("Longitude");
        
        mLongitudeControls.createDecimalText(decimalContent);
        
        label = new Label(decimalContent, SWT.NONE);
        label.setText("Latitude");
        
        mLatitudeControls.createDecimalText(decimalContent);

        // sexagesimal content
        final Composite sexagesimalContent = new Composite(content, SWT.NONE);
        sexagesimalContent.setLayout(gl = new GridLayout(7, false));
        gl.marginHeight = gl.marginWidth = 0;
        
        label = new Label(sexagesimalContent, SWT.NONE);
        label.setText("Longitude");
        
        mLongitudeControls.createSexagesimalDegreeText(sexagesimalContent);
        
        label = new Label(sexagesimalContent, SWT.NONE);
        label.setText("\u00B0"); // degree character
        
        mLongitudeControls.createSexagesimalMinuteText(sexagesimalContent);
        
        label = new Label(sexagesimalContent, SWT.NONE);
        label.setText("'");

        mLongitudeControls.createSexagesimalSecondText(sexagesimalContent);
        
        label = new Label(sexagesimalContent, SWT.NONE);
        label.setText("\"");

        label = new Label(sexagesimalContent, SWT.NONE);
        label.setText("Latitude");
        
        mLatitudeControls.createSexagesimalDegreeText(sexagesimalContent);
        
        label = new Label(sexagesimalContent, SWT.NONE);
        label.setText("\u00B0");
        
        mLatitudeControls.createSexagesimalMinuteText(sexagesimalContent);
        
        label = new Label(sexagesimalContent, SWT.NONE);
        label.setText("'");

        mLatitudeControls.createSexagesimalSecondText(sexagesimalContent);
        
        label = new Label(sexagesimalContent, SWT.NONE);
        label.setText("\"");

        // set the default display to decimal
        sl.topControl = decimalContent;
        mDecimalButton.setSelection(true);

        mDecimalButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mDecimalButton.getSelection()) {
                    sl.topControl = decimalContent;
                } else {
                    sl.topControl = sexagesimalContent;
                }
                content.layout();
            }
        });
        
        Button sendButton = new Button(manualLocationComp, SWT.PUSH);
        sendButton.setText("Send");
        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mEmulatorConsole != null) {
                    processCommandResult(mEmulatorConsole.sendLocation(
                            mLongitudeControls.getValue(), mLatitudeControls.getValue(), 0));
                }
            }
        });
        
        mLongitudeControls.setValue(DEFAULT_LONGITUDE);
        mLatitudeControls.setValue(DEFAULT_LATITUDE);
    }

    private void createGpxLocationControl(Composite gpxLocationComp) {
        GridData gd;

        IPreferenceStore store = DdmUiPreferences.getStore();

        gpxLocationComp.setLayout(new GridLayout(1, false));

        mGpxUploadButton = new Button(gpxLocationComp, SWT.PUSH);
        mGpxUploadButton.setText("Load GPX...");

        // Table for way point
        mGpxWayPointTable = new Table(gpxLocationComp,
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
        mGpxWayPointTable.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.heightHint = 100;
        mGpxWayPointTable.setHeaderVisible(true);
        mGpxWayPointTable.setLinesVisible(true);
        
        TableHelper.createTableColumn(mGpxWayPointTable, "Name", SWT.LEFT,
                "Some Name",
                PREFS_WAYPOINT_COL_NAME, store);
        TableHelper.createTableColumn(mGpxWayPointTable, "Longitude", SWT.LEFT,
                "-199.999999",
                PREFS_WAYPOINT_COL_LONGITUDE, store);
        TableHelper.createTableColumn(mGpxWayPointTable, "Latitude", SWT.LEFT,
                "-199.999999",
                PREFS_WAYPOINT_COL_LATITUDE, store);
        TableHelper.createTableColumn(mGpxWayPointTable, "Elevation", SWT.LEFT,
                "99999.9",
                PREFS_WAYPOINT_COL_ELEVATION, store);
        TableHelper.createTableColumn(mGpxWayPointTable, "Description", SWT.LEFT,
                "Some Description",
                PREFS_WAYPOINT_COL_DESCRIPTION, store);

        final TableViewer gpxWayPointViewer = new TableViewer(mGpxWayPointTable);
        gpxWayPointViewer.setContentProvider(new WayPointContentProvider());
        gpxWayPointViewer.setLabelProvider(new WayPointLabelProvider());
        
        gpxWayPointViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structuredSelection = (IStructuredSelection)selection;
                    Object selectedObject = structuredSelection.getFirstElement();
                    if (selectedObject instanceof WayPoint) {
                        WayPoint wayPoint = (WayPoint)selectedObject;
                        
                        if (mEmulatorConsole != null && mPlayingTrack == false) {
                            processCommandResult(mEmulatorConsole.sendLocation(
                                    wayPoint.getLongitude(), wayPoint.getLatitude(),
                                    wayPoint.getElevation()));
                        }
                    }
                }
            }
        });

        // table for tracks.
        mGpxTrackTable = new Table(gpxLocationComp,
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
        mGpxTrackTable.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.heightHint = 100;
        mGpxTrackTable.setHeaderVisible(true);
        mGpxTrackTable.setLinesVisible(true);

        TableHelper.createTableColumn(mGpxTrackTable, "Name", SWT.LEFT,
                "Some very long name",
                PREFS_TRACK_COL_NAME, store);
        TableHelper.createTableColumn(mGpxTrackTable, "Point Count", SWT.RIGHT,
                "9999",
                PREFS_TRACK_COL_COUNT, store);
        TableHelper.createTableColumn(mGpxTrackTable, "First Point Time", SWT.LEFT,
                "999-99-99T99:99:99Z",
                PREFS_TRACK_COL_FIRST, store);
        TableHelper.createTableColumn(mGpxTrackTable, "Last Point Time", SWT.LEFT,
                "999-99-99T99:99:99Z",
                PREFS_TRACK_COL_LAST, store);
        TableHelper.createTableColumn(mGpxTrackTable, "Comment", SWT.LEFT,
                "-199.999999",
                PREFS_TRACK_COL_COMMENT, store);

        final TableViewer gpxTrackViewer = new TableViewer(mGpxTrackTable);
        gpxTrackViewer.setContentProvider(new TrackContentProvider());
        gpxTrackViewer.setLabelProvider(new TrackLabelProvider());
        
        gpxTrackViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structuredSelection = (IStructuredSelection)selection;
                    Object selectedObject = structuredSelection.getFirstElement();
                    if (selectedObject instanceof Track) {
                        Track track = (Track)selectedObject;
                        
                        if (mEmulatorConsole != null && mPlayingTrack == false) {
                            TrackPoint[] points = track.getPoints();
                            processCommandResult(mEmulatorConsole.sendLocation(
                                    points[0].getLongitude(), points[0].getLatitude(),
                                    points[0].getElevation()));
                        }
                        
                        mPlayGpxButton.setEnabled(true);
                        mGpxBackwardButton.setEnabled(true);
                        mGpxForwardButton.setEnabled(true);
                        mGpxSpeedButton.setEnabled(true);
                        
                        return;
                    }
                }

                mPlayGpxButton.setEnabled(false);
                mGpxBackwardButton.setEnabled(false);
                mGpxForwardButton.setEnabled(false);
                mGpxSpeedButton.setEnabled(false);
            }
        });
        
        mGpxUploadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(mParent.getShell(), SWT.OPEN);

                fileDialog.setText("Load GPX File");
                fileDialog.setFilterExtensions(new String[] { "*.gpx" } );

                String fileName = fileDialog.open();
                if (fileName != null) {
                    GpxParser parser = new GpxParser(fileName);
                    if (parser.parse()) {
                        gpxWayPointViewer.setInput(parser.getWayPoints());
                        gpxTrackViewer.setInput(parser.getTracks());
                    }
                }
            }
        });
        
        mGpxPlayControls = new Composite(gpxLocationComp, SWT.NONE);
        GridLayout gl;
        mGpxPlayControls.setLayout(gl = new GridLayout(5, false));
        gl.marginHeight = gl.marginWidth = 0;
        mGpxPlayControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mPlayGpxButton = new Button(mGpxPlayControls, SWT.PUSH | SWT.FLAT);
        mPlayGpxButton.setImage(mPlayImage);
        mPlayGpxButton.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent e) {
               if (mPlayingTrack == false) {
                   ISelection selection = gpxTrackViewer.getSelection();
                   if (selection.isEmpty() == false && selection instanceof IStructuredSelection) {
                       IStructuredSelection structuredSelection = (IStructuredSelection)selection;
                       Object selectedObject = structuredSelection.getFirstElement();
                       if (selectedObject instanceof Track) {
                           Track track = (Track)selectedObject;
                           playTrack(track);
                       }
                   }
               } else {
                   // if we're playing, then we pause
                   mPlayingTrack = false;
                   if (mPlayingThread != null) {
                       mPlayingThread.interrupt();
                   }
               }
            } 
        });
        
        Label separator = new Label(mGpxPlayControls, SWT.SEPARATOR | SWT.VERTICAL);
        separator.setLayoutData(gd = new GridData(
                GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
        gd.heightHint = 0;
        
        mGpxBackwardButton = new Button(mGpxPlayControls, SWT.TOGGLE | SWT.FLAT);
        mGpxBackwardButton.setImage(mImageLoader.loadImage("backward.png", mParent.getDisplay())); // $NON-NLS-1$
        mGpxBackwardButton.setSelection(false);
        mGpxBackwardButton.addSelectionListener(mDirectionButtonAdapter);
        mGpxForwardButton = new Button(mGpxPlayControls, SWT.TOGGLE | SWT.FLAT);
        mGpxForwardButton.setImage(mImageLoader.loadImage("forward.png", mParent.getDisplay())); // $NON-NLS-1$
        mGpxForwardButton.setSelection(true);
        mGpxForwardButton.addSelectionListener(mDirectionButtonAdapter);

        mGpxSpeedButton = new Button(mGpxPlayControls, SWT.PUSH | SWT.FLAT);

        mSpeedIndex = 0;
        mSpeed = PLAY_SPEEDS[mSpeedIndex];

        mGpxSpeedButton.setText(String.format(SPEED_FORMAT, mSpeed));
        mGpxSpeedButton.addSelectionListener(mSpeedButtonAdapter);
        
        mPlayGpxButton.setEnabled(false);
        mGpxBackwardButton.setEnabled(false);
        mGpxForwardButton.setEnabled(false);
        mGpxSpeedButton.setEnabled(false);

    }

    private void createKmlLocationControl(Composite kmlLocationComp) {
        GridData gd;

        IPreferenceStore store = DdmUiPreferences.getStore();

        kmlLocationComp.setLayout(new GridLayout(1, false));

        mKmlUploadButton = new Button(kmlLocationComp, SWT.PUSH);
        mKmlUploadButton.setText("Load KML...");

        // Table for way point
        mKmlWayPointTable = new Table(kmlLocationComp,
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
        mKmlWayPointTable.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.heightHint = 200;
        mKmlWayPointTable.setHeaderVisible(true);
        mKmlWayPointTable.setLinesVisible(true);
        
        TableHelper.createTableColumn(mKmlWayPointTable, "Name", SWT.LEFT,
                "Some Name",
                PREFS_WAYPOINT_COL_NAME, store);
        TableHelper.createTableColumn(mKmlWayPointTable, "Longitude", SWT.LEFT,
                "-199.999999",
                PREFS_WAYPOINT_COL_LONGITUDE, store);
        TableHelper.createTableColumn(mKmlWayPointTable, "Latitude", SWT.LEFT,
                "-199.999999",
                PREFS_WAYPOINT_COL_LATITUDE, store);
        TableHelper.createTableColumn(mKmlWayPointTable, "Elevation", SWT.LEFT,
                "99999.9",
                PREFS_WAYPOINT_COL_ELEVATION, store);
        TableHelper.createTableColumn(mKmlWayPointTable, "Description", SWT.LEFT,
                "Some Description",
                PREFS_WAYPOINT_COL_DESCRIPTION, store);

        final TableViewer kmlWayPointViewer = new TableViewer(mKmlWayPointTable);
        kmlWayPointViewer.setContentProvider(new WayPointContentProvider());
        kmlWayPointViewer.setLabelProvider(new WayPointLabelProvider());

        mKmlUploadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(mParent.getShell(), SWT.OPEN);

                fileDialog.setText("Load KML File");
                fileDialog.setFilterExtensions(new String[] { "*.kml" } );

                String fileName = fileDialog.open();
                if (fileName != null) {
                    KmlParser parser = new KmlParser(fileName);
                    if (parser.parse()) {
                        kmlWayPointViewer.setInput(parser.getWayPoints());
                        
                        mPlayKmlButton.setEnabled(true);
                        mKmlBackwardButton.setEnabled(true);
                        mKmlForwardButton.setEnabled(true);
                        mKmlSpeedButton.setEnabled(true);
                    }
                }
            }
        });
        
        kmlWayPointViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structuredSelection = (IStructuredSelection)selection;
                    Object selectedObject = structuredSelection.getFirstElement();
                    if (selectedObject instanceof WayPoint) {
                        WayPoint wayPoint = (WayPoint)selectedObject;
                        
                        if (mEmulatorConsole != null && mPlayingTrack == false) {
                            processCommandResult(mEmulatorConsole.sendLocation(
                                    wayPoint.getLongitude(), wayPoint.getLatitude(),
                                    wayPoint.getElevation()));
                        }
                    }
                }
            }
        });
        
        
        
        mKmlPlayControls = new Composite(kmlLocationComp, SWT.NONE);
        GridLayout gl;
        mKmlPlayControls.setLayout(gl = new GridLayout(5, false));
        gl.marginHeight = gl.marginWidth = 0;
        mKmlPlayControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mPlayKmlButton = new Button(mKmlPlayControls, SWT.PUSH | SWT.FLAT);
        mPlayKmlButton.setImage(mPlayImage);
        mPlayKmlButton.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent e) {
               if (mPlayingTrack == false) {
                   Object input = kmlWayPointViewer.getInput();
                   if (input instanceof WayPoint[]) {
                       playKml((WayPoint[])input);
                   }
               } else {
                   // if we're playing, then we pause
                   mPlayingTrack = false;
                   if (mPlayingThread != null) {
                       mPlayingThread.interrupt();
                   }
               }
            } 
        });
        
        Label separator = new Label(mKmlPlayControls, SWT.SEPARATOR | SWT.VERTICAL);
        separator.setLayoutData(gd = new GridData(
                GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
        gd.heightHint = 0;
        
        mKmlBackwardButton = new Button(mKmlPlayControls, SWT.TOGGLE | SWT.FLAT);
        mKmlBackwardButton.setImage(mImageLoader.loadImage("backward.png", mParent.getDisplay())); // $NON-NLS-1$
        mKmlBackwardButton.setSelection(false);
        mKmlBackwardButton.addSelectionListener(mDirectionButtonAdapter);
        mKmlForwardButton = new Button(mKmlPlayControls, SWT.TOGGLE | SWT.FLAT);
        mKmlForwardButton.setImage(mImageLoader.loadImage("forward.png", mParent.getDisplay())); // $NON-NLS-1$
        mKmlForwardButton.setSelection(true);
        mKmlForwardButton.addSelectionListener(mDirectionButtonAdapter);

        mKmlSpeedButton = new Button(mKmlPlayControls, SWT.PUSH | SWT.FLAT);

        mSpeedIndex = 0;
        mSpeed = PLAY_SPEEDS[mSpeedIndex];

        mKmlSpeedButton.setText(String.format(SPEED_FORMAT, mSpeed));
        mKmlSpeedButton.addSelectionListener(mSpeedButtonAdapter);
        
        mPlayKmlButton.setEnabled(false);
        mKmlBackwardButton.setEnabled(false);
        mKmlForwardButton.setEnabled(false);
        mKmlSpeedButton.setEnabled(false);
    }

    /**
     * Sets the focus to the proper control inside the panel.
     */
    @Override
    public void setFocus() {
    }

    @Override
    protected void postCreation() {
        // pass
    }

    private synchronized void setDataMode(int selectionIndex) {
        if (mEmulatorConsole != null) {
            processCommandResult(mEmulatorConsole.setGsmDataMode(
                    GsmMode.getEnum(GSM_MODES[selectionIndex][1])));
        }
    }

    private synchronized void setVoiceMode(int selectionIndex) {
        if (mEmulatorConsole != null) {
            processCommandResult(mEmulatorConsole.setGsmVoiceMode(
                    GsmMode.getEnum(GSM_MODES[selectionIndex][1])));
        }
    }

    private synchronized void setNetworkLatency(int selectionIndex) {
        if (mEmulatorConsole != null) {
            processCommandResult(mEmulatorConsole.setNetworkLatency(selectionIndex));
        }
    }

    private synchronized void setNetworkSpeed(int selectionIndex) {
        if (mEmulatorConsole != null) {
            processCommandResult(mEmulatorConsole.setNetworkSpeed(selectionIndex));
        }
    }


    /**
     * Callback on device selection change.
     * @param device the new selected device
     */
    public void handleNewDevice(Device device) {
        if (mParent.isDisposed()) {
            return;
        }
        // unlink to previous console.
        synchronized (this) {
            mEmulatorConsole = null;
        }

        try {
            // get the emulator console for this device
            // First we need the device itself
            if (device != null) {
                GsmStatus gsm = null;
                NetworkStatus netstatus = null;

                synchronized (this) {
                    mEmulatorConsole = EmulatorConsole.getConsole(device);
                    if (mEmulatorConsole != null) {
                        // get the gsm status
                        gsm = mEmulatorConsole.getGsmStatus();
                        netstatus = mEmulatorConsole.getNetworkStatus();
                        
                        if (gsm == null || netstatus == null) {
                            mEmulatorConsole = null;
                        }
                    }
                }

                if (gsm != null && netstatus != null) {
                    Display d = mParent.getDisplay();
                    if (d.isDisposed() == false) {
                        final GsmStatus f_gsm = gsm;
                        final NetworkStatus f_netstatus = netstatus;
                        
                        d.asyncExec(new Runnable() {
                            public void run() {
                                if (f_gsm.voice != GsmMode.UNKNOWN) {
                                    mVoiceMode.select(getGsmComboIndex(f_gsm.voice));
                                } else {
                                    mVoiceMode.clearSelection();
                                }
                                if (f_gsm.data != GsmMode.UNKNOWN) {
                                    mDataMode.select(getGsmComboIndex(f_gsm.data));
                                } else {
                                    mDataMode.clearSelection();
                                }

                                if (f_netstatus.speed != -1) {
                                    mNetworkSpeed.select(f_netstatus.speed);
                                } else {
                                    mNetworkSpeed.clearSelection();
                                }

                                if (f_netstatus.latency != -1) {
                                    mNetworkLatency.select(f_netstatus.latency);
                                } else {
                                    mNetworkLatency.clearSelection();
                                }
                            }
                        });
                    }
                }
            }
        } finally {
            // enable/disable the ui
            boolean enable = false;
            synchronized (this) {
                enable = mEmulatorConsole != null;
            }
            
            enable(enable);
        }
    }

    /**
     * Enable or disable the ui. Can be called from non ui threads.
     * @param enabled
     */
    private void enable(final boolean enabled) {
        try {
            Display d = mParent.getDisplay();
            d.asyncExec(new Runnable() {
                public void run() {
                    if (mParent.isDisposed() == false) {
                        doEnable(enabled);
                    }
                }
            });
        } catch (SWTException e) {
            // disposed. do nothing
        }
    }

    private boolean isValidPhoneNumber() {
        String number = mPhoneNumber.getText().trim();

        return number.matches(RE_PHONE_NUMBER);
    }

    /**
     * Enable or disable the ui. Cannot be called from non ui threads.
     * @param enabled
     */
    protected void doEnable(boolean enabled) {
        mVoiceLabel.setEnabled(enabled);
        mVoiceMode.setEnabled(enabled);

        mDataLabel.setEnabled(enabled);
        mDataMode.setEnabled(enabled);

        mSpeedLabel.setEnabled(enabled);
        mNetworkSpeed.setEnabled(enabled);

        mLatencyLabel.setEnabled(enabled);
        mNetworkLatency.setEnabled(enabled);

        // Calling setEnabled on a text field will trigger a modifyText event, so we don't do it
        // if we don't need to.
        if (mPhoneNumber.isEnabled() != enabled) {
            mNumberLabel.setEnabled(enabled);
            mPhoneNumber.setEnabled(enabled);
        }

        boolean valid = isValidPhoneNumber();

        mVoiceButton.setEnabled(enabled && valid);
        mSmsButton.setEnabled(enabled && valid);

        boolean smsValid = enabled && valid && mSmsButton.getSelection();

        // Calling setEnabled on a text field will trigger a modifyText event, so we don't do it
        // if we don't need to.
        if (mSmsMessage.isEnabled() != smsValid) {
            mMessageLabel.setEnabled(smsValid);
            mSmsMessage.setEnabled(smsValid);
        }
        if (enabled == false) {
            mSmsMessage.setText(""); //$NON-NLs-1$
        }

        mCallButton.setEnabled(enabled && valid);
        mCancelButton.setEnabled(enabled && valid && mVoiceButton.getSelection());

        if (enabled == false) {
            mVoiceMode.clearSelection();
            mDataMode.clearSelection();
            mNetworkSpeed.clearSelection();
            mNetworkLatency.clearSelection();
            if (mPhoneNumber.getText().length() > 0) {
                mPhoneNumber.setText(""); //$NON-NLS-1$
            }
        }

        // location controls
        mLocationFolders.setEnabled(enabled);

        mDecimalButton.setEnabled(enabled);
        mSexagesimalButton.setEnabled(enabled);
        mLongitudeControls.setEnabled(enabled);
        mLatitudeControls.setEnabled(enabled);

        mGpxUploadButton.setEnabled(enabled);
        mGpxWayPointTable.setEnabled(enabled);
        mGpxTrackTable.setEnabled(enabled);
        mKmlUploadButton.setEnabled(enabled);
        mKmlWayPointTable.setEnabled(enabled);
    }

    /**
     * Returns the index of the combo item matching a specific GsmMode.
     * @param mode
     */
    private int getGsmComboIndex(GsmMode mode) {
        for (int i = 0 ; i < GSM_MODES.length; i++) {
            String[] modes = GSM_MODES[i];
            if (mode.getTag().equals(modes[1])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Processes the result of a command sent to the console.
     * @param result the result of the command.
     */
    private boolean processCommandResult(final String result) {
        if (result != EmulatorConsole.RESULT_OK) {
            try {
                mParent.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        if (mParent.isDisposed() == false) {
                            MessageDialog.openError(mParent.getShell(), "Emulator Console",
                                    result);
                        }
                    }
                });
            } catch (SWTException e) {
                // we're quitting, just ignore
            }
            
            return false;
        }
        
        return true;
    }

    /**
     * @param track
     */
    private void playTrack(final Track track) {
        // no need to synchronize this check, the worst that can happen, is we start the thread
        // for nothing.
        if (mEmulatorConsole != null) {
            mPlayGpxButton.setImage(mPauseImage);
            mPlayKmlButton.setImage(mPauseImage);
            mPlayingTrack = true;

            mPlayingThread = new Thread() {
                @Override
                public void run() {
                    try {
                        TrackPoint[] trackPoints = track.getPoints();
                        int count = trackPoints.length;
                        
                        // get the start index.
                        int start = 0;
                        if (mPlayDirection == -1) {
                            start = count - 1;
                        }
                        
                        for (int p = start; p >= 0 && p < count; p += mPlayDirection) {
                            if (mPlayingTrack == false) {
                                return;
                            }

                            // get the current point and send its location to
                            // the emulator.
                            final TrackPoint trackPoint = trackPoints[p];

                            synchronized (EmulatorControlPanel.this) {
                                if (mEmulatorConsole == null ||
                                        processCommandResult(mEmulatorConsole.sendLocation(
                                                trackPoint.getLongitude(), trackPoint.getLatitude(),
                                                trackPoint.getElevation())) == false) {
                                    return;
                                }
                            }

                            // if this is not the final point, then get the next one and
                            // compute the delta time
                            int nextIndex = p + mPlayDirection;
                            if (nextIndex >=0 && nextIndex < count) {
                                TrackPoint nextPoint = trackPoints[nextIndex];

                                long delta = nextPoint.getTime() - trackPoint.getTime();
                                if (delta < 0) {
                                    delta = -delta;
                                }
                                
                                long startTime = System.currentTimeMillis();

                                try {
                                    sleep(delta / mSpeed);
                                } catch (InterruptedException e) {
                                    if (mPlayingTrack == false) {
                                        return;
                                    }
                                    
                                    // we got interrupted, lets make sure we can play
                                    do {
                                        long waited = System.currentTimeMillis() - startTime;
                                        long needToWait = delta / mSpeed;
                                        if (waited < needToWait) {
                                            try {
                                                sleep(needToWait - waited);
                                            } catch (InterruptedException e1) {
                                                // we'll just loop and wait again if needed.
                                                // unless we're supposed to stop
                                                if (mPlayingTrack == false) {
                                                    return;
                                                }
                                            }
                                        } else {
                                            break;
                                        }
                                    } while (true);
                                }
                            }
                        }
                    } finally {
                        mPlayingTrack = false;
                        try {
                            mParent.getDisplay().asyncExec(new Runnable() {
                                public void run() {
                                    if (mPlayGpxButton.isDisposed() == false) {
                                        mPlayGpxButton.setImage(mPlayImage);
                                        mPlayKmlButton.setImage(mPlayImage);
                                    }
                                }
                            });
                        } catch (SWTException e) {
                            // we're quitting, just ignore
                        }
                    }
                }
            };

            mPlayingThread.start();
        }
    }
    
    private void playKml(final WayPoint[] trackPoints) {
        // no need to synchronize this check, the worst that can happen, is we start the thread
        // for nothing.
        if (mEmulatorConsole != null) {
            mPlayGpxButton.setImage(mPauseImage);
            mPlayKmlButton.setImage(mPauseImage);
            mPlayingTrack = true;

            mPlayingThread = new Thread() {
                @Override
                public void run() {
                    try {
                        int count = trackPoints.length;
                        
                        // get the start index.
                        int start = 0;
                        if (mPlayDirection == -1) {
                            start = count - 1;
                        }
                        
                        for (int p = start; p >= 0 && p < count; p += mPlayDirection) {
                            if (mPlayingTrack == false) {
                                return;
                            }

                            // get the current point and send its location to
                            // the emulator.
                            WayPoint trackPoint = trackPoints[p];

                            synchronized (EmulatorControlPanel.this) {
                                if (mEmulatorConsole == null ||
                                        processCommandResult(mEmulatorConsole.sendLocation(
                                                trackPoint.getLongitude(), trackPoint.getLatitude(),
                                                trackPoint.getElevation())) == false) {
                                    return;
                                }
                            }

                            // if this is not the final point, then get the next one and
                            // compute the delta time
                            int nextIndex = p + mPlayDirection;
                            if (nextIndex >=0 && nextIndex < count) {

                                long delta = 1000; // 1 second
                                if (delta < 0) {
                                    delta = -delta;
                                }
                                
                                long startTime = System.currentTimeMillis();

                                try {
                                    sleep(delta / mSpeed);
                                } catch (InterruptedException e) {
                                    if (mPlayingTrack == false) {
                                        return;
                                    }
                                    
                                    // we got interrupted, lets make sure we can play
                                    do {
                                        long waited = System.currentTimeMillis() - startTime;
                                        long needToWait = delta / mSpeed;
                                        if (waited < needToWait) {
                                            try {
                                                sleep(needToWait - waited);
                                            } catch (InterruptedException e1) {
                                                // we'll just loop and wait again if needed.
                                                // unless we're supposed to stop
                                                if (mPlayingTrack == false) {
                                                    return;
                                                }
                                            }
                                        } else {
                                            break;
                                        }
                                    } while (true);
                                }
                            }
                        }
                    } finally {
                        mPlayingTrack = false;
                        try {
                            mParent.getDisplay().asyncExec(new Runnable() {
                                public void run() {
                                    if (mPlayGpxButton.isDisposed() == false) {
                                        mPlayGpxButton.setImage(mPlayImage);
                                        mPlayKmlButton.setImage(mPlayImage);
                                    }
                                }
                            });
                        } catch (SWTException e) {
                            // we're quitting, just ignore
                        }
                    }
                }
            };

            mPlayingThread.start();
        }        
    }
}
