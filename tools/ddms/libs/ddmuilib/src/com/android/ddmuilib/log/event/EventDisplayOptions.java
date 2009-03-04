/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ddmuilib.log.event;

import com.android.ddmlib.log.EventContainer;
import com.android.ddmlib.log.EventLogParser;
import com.android.ddmlib.log.EventValueDescription;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.IImageLoader;
import com.android.ddmuilib.log.event.EventDisplay.OccurrenceDisplayDescriptor;
import com.android.ddmuilib.log.event.EventDisplay.ValueDisplayDescriptor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

class EventDisplayOptions  extends Dialog {
    private static final int DLG_WIDTH = 700;
    private static final int DLG_HEIGHT = 700;
    
    private IImageLoader mImageLoader;

    private Shell mParent;
    private Shell mShell;
    
    private boolean mEditStatus = false;
    private final ArrayList<EventDisplay> mDisplayList = new ArrayList<EventDisplay>();

    /* LEFT LIST */
    private List mEventDisplayList;
    private Button mEventDisplayNewButton;
    private Button mEventDisplayDeleteButton;
    private Button mEventDisplayUpButton;
    private Button mEventDisplayDownButton;
    private Text mDisplayWidthText;
    private Text mDisplayHeightText;

    /* WIDGETS ON THE RIGHT */
    private Text mDisplayNameText;
    private Combo mDisplayTypeCombo;
    private Group mChartOptions;
    private Group mHistOptions;
    private Button mPidFilterCheckBox;
    private Text mPidText;

    /** Map with (event-tag, event name) */
    private Map<Integer, String> mEventTagMap;

    /** Map with (event-tag, array of value info for the event) */
    private Map<Integer, EventValueDescription[]> mEventDescriptionMap;

    /** list of current pids */
    private ArrayList<Integer> mPidList;

    private EventLogParser mLogParser;

    private Group mInfoGroup;

    private static class SelectionWidgets {
        private List mList;
        private Button mNewButton;
        private Button mEditButton;
        private Button mDeleteButton;
        
        private void setEnabled(boolean enable) {
            mList.setEnabled(enable);
            mNewButton.setEnabled(enable);
            mEditButton.setEnabled(enable);
            mDeleteButton.setEnabled(enable);
        }
    }
    
    private SelectionWidgets mValueSelection;
    private SelectionWidgets mOccurrenceSelection;
    
    /** flag to temporarly disable processing of {@link Text} changes, so that 
     * {@link Text#setText(String)} can be called safely. */
    private boolean mProcessTextChanges = true;
    private Text mTimeLimitText;
    private Text mHistWidthText;

    EventDisplayOptions(IImageLoader imageLoader, Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
        mImageLoader = imageLoader;
    }

    /**
     * Opens the display option dialog, to edit the {@link EventDisplay} objects provided in the
     * list.
     * @param logParser
     * @param displayList
     * @param eventList
     * @return true if the list of {@link EventDisplay} objects was updated.
     */
    boolean open(EventLogParser logParser, ArrayList<EventDisplay> displayList,
            ArrayList<EventContainer> eventList) {
        mLogParser = logParser;

        if (logParser != null) {
            // we need 2 things from the parser.
            // the event tag / event name map
            mEventTagMap = logParser.getTagMap();
            
            // the event info map
            mEventDescriptionMap = logParser.getEventInfoMap();
        }

        // make a copy of the EventDisplay list since we'll use working copies.
        duplicateEventDisplay(displayList);
        
        // build a list of pid from the list of events.
        buildPidList(eventList);

        createUI();

        if (mParent == null || mShell == null) {
            return false;
        }

        // Set the dialog size.
        mShell.setMinimumSize(DLG_WIDTH, DLG_HEIGHT);
        Rectangle r = mParent.getBounds();
        // get the center new top left.
        int cx = r.x + r.width/2;
        int x = cx - DLG_WIDTH / 2;
        int cy = r.y + r.height/2;
        int y = cy - DLG_HEIGHT / 2;
        mShell.setBounds(x, y, DLG_WIDTH, DLG_HEIGHT);

        mShell.layout();

        // actually open the dialog
        mShell.open();

        // event loop until the dialog is closed.
        Display display = mParent.getDisplay();
        while (!mShell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        
        return mEditStatus;
    }
    
    ArrayList<EventDisplay> getEventDisplays() {
        return mDisplayList;
    }
    
    private void createUI() {
        mParent = getParent();
        mShell = new Shell(mParent, getStyle());
        mShell.setText("Event Display Configuration");

        mShell.setLayout(new GridLayout(1, true));

        final Composite topPanel = new Composite(mShell, SWT.NONE);
        topPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        topPanel.setLayout(new GridLayout(2, false));
        
        // create the tree on the left and the controls on the right.
        Composite leftPanel = new Composite(topPanel, SWT.NONE);
        Composite rightPanel = new Composite(topPanel, SWT.NONE);

        createLeftPanel(leftPanel);
        createRightPanel(rightPanel);

        mShell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                event.doit = true;
            }
        });
        
        Label separator = new Label(mShell, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Composite bottomButtons = new Composite(mShell, SWT.NONE);
        bottomButtons.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout gl;
        bottomButtons.setLayout(gl = new GridLayout(2, true));
        gl.marginHeight = gl.marginWidth = 0;
        
        Button okButton = new Button(bottomButtons, SWT.PUSH);
        okButton.setText("OK");
        okButton.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                mShell.close();
            }
        });

        Button cancelButton = new Button(bottomButtons, SWT.PUSH);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                // cancel the modification flag.
                mEditStatus = false;
                
                // and close
                mShell.close();
            }
        });

        enable(false);
        
        // fill the list with the current display
        fillEventDisplayList();
    }

    private void createLeftPanel(Composite leftPanel) {
        final IPreferenceStore store = DdmUiPreferences.getStore();

        GridLayout gl;

        leftPanel.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        leftPanel.setLayout(gl = new GridLayout(1, false));
        gl.verticalSpacing = 1;

        mEventDisplayList = new List(leftPanel,
                SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION);
        mEventDisplayList.setLayoutData(new GridData(GridData.FILL_BOTH));
        mEventDisplayList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleEventDisplaySelection();
            }
        });

        Composite bottomControls = new Composite(leftPanel, SWT.NONE);
        bottomControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        bottomControls.setLayout(gl = new GridLayout(5, false));
        gl.marginHeight = gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;

        mEventDisplayNewButton = new Button(bottomControls, SWT.PUSH | SWT.FLAT);
        mEventDisplayNewButton.setImage(mImageLoader.loadImage("add.png", // $NON-NLS-1$
                leftPanel.getDisplay()));
        mEventDisplayNewButton.setToolTipText("Adds a new event display");
        mEventDisplayNewButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        mEventDisplayNewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createNewEventDisplay();
            }
        });
        
        mEventDisplayDeleteButton = new Button(bottomControls, SWT.PUSH | SWT.FLAT);
        mEventDisplayDeleteButton.setImage(mImageLoader.loadImage("delete.png", // $NON-NLS-1$
                leftPanel.getDisplay()));
        mEventDisplayDeleteButton.setToolTipText("Deletes the selected event display");
        mEventDisplayDeleteButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        mEventDisplayDeleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteEventDisplay();
            }
        });

        mEventDisplayUpButton = new Button(bottomControls, SWT.PUSH | SWT.FLAT);
        mEventDisplayUpButton.setImage(mImageLoader.loadImage("up.png", // $NON-NLS-1$
                leftPanel.getDisplay()));
        mEventDisplayUpButton.setToolTipText("Moves the selected event display up");
        mEventDisplayUpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // get current selection.
                int selection = mEventDisplayList.getSelectionIndex();
                if (selection > 0) {
                    // update the list of EventDisplay.
                    EventDisplay display = mDisplayList.remove(selection);
                    mDisplayList.add(selection - 1, display);
                    
                    // update the list widget
                    mEventDisplayList.remove(selection);
                    mEventDisplayList.add(display.getName(), selection - 1);
                    
                    // update the selection and reset the ui.
                    mEventDisplayList.select(selection - 1);
                    handleEventDisplaySelection();
                    mEventDisplayList.showSelection();

                    setModified();
                }
            }
        });

        mEventDisplayDownButton = new Button(bottomControls, SWT.PUSH | SWT.FLAT);
        mEventDisplayDownButton.setImage(mImageLoader.loadImage("down.png", // $NON-NLS-1$
                leftPanel.getDisplay()));
        mEventDisplayDownButton.setToolTipText("Moves the selected event display down");
        mEventDisplayDownButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // get current selection.
                int selection = mEventDisplayList.getSelectionIndex();
                if (selection != -1 && selection < mEventDisplayList.getItemCount() - 1) {
                    // update the list of EventDisplay.
                    EventDisplay display = mDisplayList.remove(selection);
                    mDisplayList.add(selection + 1, display);
                    
                    // update the list widget
                    mEventDisplayList.remove(selection);
                    mEventDisplayList.add(display.getName(), selection + 1);
                    
                    // update the selection and reset the ui.
                    mEventDisplayList.select(selection + 1);
                    handleEventDisplaySelection();
                    mEventDisplayList.showSelection();

                    setModified();
                }
            }
        });
        
        Group sizeGroup = new Group(leftPanel, SWT.NONE);
        sizeGroup.setText("Display Size:");
        sizeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sizeGroup.setLayout(new GridLayout(2, false));

        Label l = new Label(sizeGroup, SWT.NONE);
        l.setText("Width:");
        
        mDisplayWidthText = new Text(sizeGroup, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
        mDisplayWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDisplayWidthText.setText(Integer.toString(
                store.getInt(EventLogPanel.PREFS_DISPLAY_WIDTH)));
        mDisplayWidthText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String text = mDisplayWidthText.getText().trim();
                try {
                    store.setValue(EventLogPanel.PREFS_DISPLAY_WIDTH, Integer.parseInt(text));
                    setModified();
                } catch (NumberFormatException nfe) {
                    // do something?
                }
            }
        });

        l = new Label(sizeGroup, SWT.NONE);
        l.setText("Height:");

        mDisplayHeightText = new Text(sizeGroup, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
        mDisplayHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDisplayHeightText.setText(Integer.toString(
                store.getInt(EventLogPanel.PREFS_DISPLAY_HEIGHT)));
        mDisplayHeightText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String text = mDisplayHeightText.getText().trim();
                try {
                    store.setValue(EventLogPanel.PREFS_DISPLAY_HEIGHT, Integer.parseInt(text));
                    setModified();
                } catch (NumberFormatException nfe) {
                    // do something?
                }
            }
        });
    }

    private void createRightPanel(Composite rightPanel) {
        rightPanel.setLayout(new GridLayout(1, true));
        rightPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

        mInfoGroup = new Group(rightPanel, SWT.NONE);
        mInfoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mInfoGroup.setLayout(new GridLayout(2, false));
        
        Label nameLabel = new Label(mInfoGroup, SWT.LEFT);
        nameLabel.setText("Name:");

        mDisplayNameText = new Text(mInfoGroup, SWT.BORDER | SWT.LEFT | SWT.SINGLE);
        mDisplayNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDisplayNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (mProcessTextChanges) {
                    EventDisplay eventDisplay = getCurrentEventDisplay();
                    if (eventDisplay != null) {
                        eventDisplay.setName(mDisplayNameText.getText());
                        int index = mEventDisplayList.getSelectionIndex();
                        mEventDisplayList.remove(index);
                        mEventDisplayList.add(eventDisplay.getName(), index);
                        mEventDisplayList.select(index);
                        handleEventDisplaySelection();
                        setModified();
                    }
                }
            }
        });

        Label displayLabel = new Label(mInfoGroup, SWT.LEFT);
        displayLabel.setText("Type:");
        
        mDisplayTypeCombo = new Combo(mInfoGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
        mDisplayTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // add the combo values. This must match the values EventDisplay.DISPLAY_TYPE_*
        mDisplayTypeCombo.add("Log All");
        mDisplayTypeCombo.add("Filtered Log");
        mDisplayTypeCombo.add("Graph");
        mDisplayTypeCombo.add("Sync");
        mDisplayTypeCombo.add("Sync Histogram");
        mDisplayTypeCombo.add("Sync Performance");
        mDisplayTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EventDisplay eventDisplay = getCurrentEventDisplay();
                if (eventDisplay != null && eventDisplay.getDisplayType() != mDisplayTypeCombo.getSelectionIndex()) {
                    /* Replace the EventDisplay object with a different subclass */
                    setModified();
                    String name = eventDisplay.getName();
                    EventDisplay newEventDisplay = EventDisplay.eventDisplayFactory(mDisplayTypeCombo.getSelectionIndex(), name);
                    setCurrentEventDisplay(newEventDisplay);
                    fillUiWith(newEventDisplay);
                }
            }
        });
        
        mChartOptions = new Group(mInfoGroup, SWT.NONE);
        mChartOptions.setText("Chart Options");
        GridData gd;
        mChartOptions.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 2;
        mChartOptions.setLayout(new GridLayout(2, false));
        
        Label l = new Label(mChartOptions, SWT.NONE);
        l.setText("Time Limit (seconds):");
        
        mTimeLimitText = new Text(mChartOptions, SWT.BORDER);
        mTimeLimitText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTimeLimitText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent arg0) {
                String text = mTimeLimitText.getText().trim();
                EventDisplay eventDisplay = getCurrentEventDisplay();
                if (eventDisplay != null) {
                    try {
                        if (text.length() == 0) {
                            eventDisplay.resetChartTimeLimit();
                        } else {
                            eventDisplay.setChartTimeLimit(Long.parseLong(text));
                        }
                    } catch (NumberFormatException nfe) {
                        eventDisplay.resetChartTimeLimit();
                    } finally {
                        setModified();
                    }
                }
            }
        });

        mHistOptions = new Group(mInfoGroup, SWT.NONE);
        mHistOptions.setText("Histogram Options");
        GridData gdh;
        mHistOptions.setLayoutData(gdh = new GridData(GridData.FILL_HORIZONTAL));
        gdh.horizontalSpan = 2;
        mHistOptions.setLayout(new GridLayout(2, false));
        
        Label lh = new Label(mHistOptions, SWT.NONE);
        lh.setText("Histogram width (hours):");
        
        mHistWidthText = new Text(mHistOptions, SWT.BORDER);
        mHistWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mHistWidthText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent arg0) {
                String text = mHistWidthText.getText().trim();
                EventDisplay eventDisplay = getCurrentEventDisplay();
                if (eventDisplay != null) {
                    try {
                        if (text.length() == 0) {
                            eventDisplay.resetHistWidth();
                        } else {
                            eventDisplay.setHistWidth(Long.parseLong(text));
                        }
                    } catch (NumberFormatException nfe) {
                        eventDisplay.resetHistWidth();
                    } finally {
                        setModified();
                    }
                }
            }
        });

        mPidFilterCheckBox = new Button(mInfoGroup, SWT.CHECK);
        mPidFilterCheckBox.setText("Enable filtering by pid");
        mPidFilterCheckBox.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 2;
        mPidFilterCheckBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EventDisplay eventDisplay = getCurrentEventDisplay();
                if (eventDisplay != null) {
                    eventDisplay.setPidFiltering(mPidFilterCheckBox.getSelection());
                    mPidText.setEnabled(mPidFilterCheckBox.getSelection());
                    setModified();
                }
            }
        });

        Label pidLabel = new Label(mInfoGroup, SWT.NONE);
        pidLabel.setText("Pid Filter:");
        pidLabel.setToolTipText("Enter all pids, separated by commas");
        
        mPidText = new Text(mInfoGroup, SWT.BORDER);
        mPidText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mPidText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (mProcessTextChanges) {
                    EventDisplay eventDisplay = getCurrentEventDisplay();
                    if (eventDisplay != null && eventDisplay.getPidFiltering()) {
                        String pidText = mPidText.getText().trim();
                        String[] pids = pidText.split("\\s*,\\s*"); //$NON-NLS-1$
    
                        ArrayList<Integer> list = new ArrayList<Integer>();
                        for (String pid : pids) {
                            try {
                                list.add(Integer.valueOf(pid));
                            } catch (NumberFormatException nfe) {
                                // just ignore non valid pid
                            }
                        }
                        
                        eventDisplay.setPidFilterList(list);
                        setModified();
                    }
                }
            }
        });
        
        /* ------------------
         * EVENT VALUE/OCCURRENCE SELECTION
         * ------------------ */
        mValueSelection = createEventSelection(rightPanel, ValueDisplayDescriptor.class,
                "Event Value Display");
        mOccurrenceSelection = createEventSelection(rightPanel, OccurrenceDisplayDescriptor.class,
                "Event Occurrence Display");
    }

    private SelectionWidgets createEventSelection(Composite rightPanel,
            final Class<? extends OccurrenceDisplayDescriptor> descriptorClass,
            String groupMessage) {

        Group eventSelectionPanel = new Group(rightPanel, SWT.NONE);
        eventSelectionPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl;
        eventSelectionPanel.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;
        eventSelectionPanel.setText(groupMessage);
        
        final SelectionWidgets widgets = new SelectionWidgets();
        
        widgets.mList = new List(eventSelectionPanel, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        widgets.mList.setLayoutData(new GridData(GridData.FILL_BOTH));
        widgets.mList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = widgets.mList.getSelectionIndex();
                if (index != -1) {
                    widgets.mDeleteButton.setEnabled(true);
                    widgets.mEditButton.setEnabled(true);
                } else {
                    widgets.mDeleteButton.setEnabled(false);
                    widgets.mEditButton.setEnabled(false);
                }
            }
        });

        Composite rightControls = new Composite(eventSelectionPanel, SWT.NONE);
        rightControls.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        rightControls.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;

        widgets.mNewButton = new Button(rightControls, SWT.PUSH | SWT.FLAT);
        widgets.mNewButton.setText("New...");
        widgets.mNewButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        widgets.mNewButton.setEnabled(false);
        widgets.mNewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // current event
                try {
                    EventDisplay eventDisplay = getCurrentEventDisplay();
                    if (eventDisplay != null) {
                        EventValueSelector dialog = new EventValueSelector(mShell);
                        if (dialog.open(descriptorClass, mLogParser)) {
                            eventDisplay.addDescriptor(dialog.getDescriptor());
                            fillUiWith(eventDisplay);
                            setModified();
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        widgets.mEditButton = new Button(rightControls, SWT.PUSH | SWT.FLAT);
        widgets.mEditButton.setText("Edit...");
        widgets.mEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        widgets.mEditButton.setEnabled(false);
        widgets.mEditButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // current event
                EventDisplay eventDisplay = getCurrentEventDisplay();
                if (eventDisplay != null) {
                    // get the current descriptor index
                    int index = widgets.mList.getSelectionIndex();
                    if (index != -1) {
                        // get the descriptor itself
                        OccurrenceDisplayDescriptor descriptor = eventDisplay.getDescriptor(
                                descriptorClass, index);
    
                        // open the edit dialog.
                        EventValueSelector dialog = new EventValueSelector(mShell);
                        if (dialog.open(descriptor, mLogParser)) {
                            descriptor.replaceWith(dialog.getDescriptor());
                            eventDisplay.updateValueDescriptorCheck();
                            fillUiWith(eventDisplay);

                            // reselect the item since fillUiWith remove the selection.
                            widgets.mList.select(index);
                            widgets.mList.notifyListeners(SWT.Selection, null);
                            
                            setModified();
                        }
                    }
                }
            }
        });

        widgets.mDeleteButton = new Button(rightControls, SWT.PUSH | SWT.FLAT);
        widgets.mDeleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        widgets.mDeleteButton.setText("Delete");
        widgets.mDeleteButton.setEnabled(false);
        widgets.mDeleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // current event
                EventDisplay eventDisplay = getCurrentEventDisplay();
                if (eventDisplay != null) {
                    // get the current descriptor index
                    int index = widgets.mList.getSelectionIndex();
                    if (index != -1) {
                        eventDisplay.removeDescriptor(descriptorClass, index);
                        fillUiWith(eventDisplay);
                        setModified();
                    }
                }
            }
        });

        return widgets;
    }
   
    
    private void duplicateEventDisplay(ArrayList<EventDisplay> displayList) {
        for (EventDisplay eventDisplay : displayList) {
            mDisplayList.add(EventDisplay.clone(eventDisplay));
        }
    }
    
    private void buildPidList(ArrayList<EventContainer> eventList) {
        mPidList = new ArrayList<Integer>();
        for (EventContainer event : eventList) {
            if (mPidList.indexOf(event.pid) == -1) {
                mPidList.add(event.pid);
            }
        }
    }

    private void setModified() {
        mEditStatus = true;
    }

    
    private void enable(boolean status) {
        mEventDisplayDeleteButton.setEnabled(status);

        // enable up/down
        int selection = mEventDisplayList.getSelectionIndex();
        int count = mEventDisplayList.getItemCount();
        mEventDisplayUpButton.setEnabled(status && selection > 0);
        mEventDisplayDownButton.setEnabled(status && selection != -1 && selection < count - 1);

        mDisplayNameText.setEnabled(status);
        mDisplayTypeCombo.setEnabled(status);
        mPidFilterCheckBox.setEnabled(status);

        mValueSelection.setEnabled(status);
        mOccurrenceSelection.setEnabled(status);
        mValueSelection.mNewButton.setEnabled(status);
        mOccurrenceSelection.mNewButton.setEnabled(status);
        if (status == false) {
            mPidText.setEnabled(false);
        }
    }
    
    private void fillEventDisplayList() {
        for (EventDisplay eventDisplay : mDisplayList) {
            mEventDisplayList.add(eventDisplay.getName());
        }
    }
    
    private void createNewEventDisplay() {
        int count = mDisplayList.size();
        
        String name = String.format("display %1$d", count + 1);
        
        EventDisplay eventDisplay = EventDisplay.eventDisplayFactory(0 /* type*/, name);
        
        mDisplayList.add(eventDisplay);
        mEventDisplayList.add(name);
        
        mEventDisplayList.select(count);
        handleEventDisplaySelection();
        mEventDisplayList.showSelection();
        
        setModified();
    }
    
    private void deleteEventDisplay() {
        int selection = mEventDisplayList.getSelectionIndex();
        if (selection != -1) {
            mDisplayList.remove(selection);
            mEventDisplayList.remove(selection);
            if (mDisplayList.size() < selection) {
                selection--;
            }
            mEventDisplayList.select(selection);
            handleEventDisplaySelection();

            setModified();
        }
    }

    private EventDisplay getCurrentEventDisplay() {
        int selection = mEventDisplayList.getSelectionIndex();
        if (selection != -1) {
            return mDisplayList.get(selection);
        }
        
        return null;
    }

    private void setCurrentEventDisplay(EventDisplay eventDisplay) {
        int selection = mEventDisplayList.getSelectionIndex();
        if (selection != -1) {
            mDisplayList.set(selection, eventDisplay);
        }
    }
    
    private void handleEventDisplaySelection() {
        EventDisplay eventDisplay = getCurrentEventDisplay();
        if (eventDisplay != null) {
            // enable the UI
            enable(true);

            // and fill it
            fillUiWith(eventDisplay);
        } else {
            // disable the UI
            enable(false);

            // and empty it.
            emptyUi();
        }
    }

    private void emptyUi() {
        mDisplayNameText.setText("");
        mDisplayTypeCombo.clearSelection();
        mValueSelection.mList.removeAll();
        mOccurrenceSelection.mList.removeAll();
    }

    private void fillUiWith(EventDisplay eventDisplay) {
        mProcessTextChanges = false;

        mDisplayNameText.setText(eventDisplay.getName());
        int displayMode = eventDisplay.getDisplayType();
        mDisplayTypeCombo.select(displayMode);
        if (displayMode == EventDisplay.DISPLAY_TYPE_GRAPH) {
            GridData gd = (GridData) mChartOptions.getLayoutData();
            gd.exclude = false;
            mChartOptions.setVisible(!gd.exclude);
            long limit = eventDisplay.getChartTimeLimit();
            if (limit != -1) {
                mTimeLimitText.setText(Long.toString(limit));
            } else {
                mTimeLimitText.setText(""); //$NON-NLS-1$
            }
        } else {
            GridData gd = (GridData) mChartOptions.getLayoutData();
            gd.exclude = true;
            mChartOptions.setVisible(!gd.exclude);
            mTimeLimitText.setText(""); //$NON-NLS-1$
        }

        if (displayMode == EventDisplay.DISPLAY_TYPE_SYNC_HIST) {
            GridData gd = (GridData) mHistOptions.getLayoutData();
            gd.exclude = false;
            mHistOptions.setVisible(!gd.exclude);
            long limit = eventDisplay.getHistWidth();
            if (limit != -1) {
                mHistWidthText.setText(Long.toString(limit));
            } else {
                mHistWidthText.setText(""); //$NON-NLS-1$
            }
        } else {
            GridData gd = (GridData) mHistOptions.getLayoutData();
            gd.exclude = true;
            mHistOptions.setVisible(!gd.exclude);
            mHistWidthText.setText(""); //$NON-NLS-1$
        }
        mInfoGroup.layout(true);
        mShell.layout(true);
        mShell.pack();
        
        if (eventDisplay.getPidFiltering()) {
            mPidFilterCheckBox.setSelection(true);
            mPidText.setEnabled(true);

            // build the pid list.
            ArrayList<Integer> list = eventDisplay.getPidFilterList();
            if (list != null) {
                StringBuilder sb = new StringBuilder();
                int count = list.size();
                for (int i = 0 ; i < count ; i++) {
                    sb.append(list.get(i));
                    if (i < count - 1) {
                        sb.append(", ");//$NON-NLS-1$
                    }
                }
                mPidText.setText(sb.toString());
            } else {
                mPidText.setText(""); //$NON-NLS-1$
            }
        } else {
            mPidFilterCheckBox.setSelection(false);
            mPidText.setEnabled(false);
            mPidText.setText(""); //$NON-NLS-1$
        }

        mProcessTextChanges = true;

        mValueSelection.mList.removeAll();
        mOccurrenceSelection.mList.removeAll();
        
        if (eventDisplay.getDisplayType() == EventDisplay.DISPLAY_TYPE_FILTERED_LOG ||
                eventDisplay.getDisplayType() == EventDisplay.DISPLAY_TYPE_GRAPH) {
            mOccurrenceSelection.setEnabled(true);
            mValueSelection.setEnabled(true);

            Iterator<ValueDisplayDescriptor> valueIterator = eventDisplay.getValueDescriptors();
    
            while (valueIterator.hasNext()) {
                ValueDisplayDescriptor descriptor = valueIterator.next();
                mValueSelection.mList.add(String.format("%1$s: %2$s [%3$s]%4$s",
                        mEventTagMap.get(descriptor.eventTag), descriptor.valueName,
                        getSeriesLabelDescription(descriptor), getFilterDescription(descriptor)));
            }

            Iterator<OccurrenceDisplayDescriptor> occurrenceIterator =
                eventDisplay.getOccurrenceDescriptors();
    
            while (occurrenceIterator.hasNext()) {
                OccurrenceDisplayDescriptor descriptor = occurrenceIterator.next();
    
                mOccurrenceSelection.mList.add(String.format("%1$s [%2$s]%3$s",
                        mEventTagMap.get(descriptor.eventTag),
                        getSeriesLabelDescription(descriptor),
                        getFilterDescription(descriptor)));
            }

            mValueSelection.mList.notifyListeners(SWT.Selection, null);
            mOccurrenceSelection.mList.notifyListeners(SWT.Selection, null);
        } else {
            mOccurrenceSelection.setEnabled(false);
            mValueSelection.setEnabled(false);
        }
        
    }
    
    /**
     * Returns a String describing what is used as the series label
     * @param descriptor the descriptor of the display.
     */
    private String getSeriesLabelDescription(OccurrenceDisplayDescriptor descriptor) {
        if (descriptor.seriesValueIndex != -1) {
            if (descriptor.includePid) {
                return String.format("%1$s + pid",
                        mEventDescriptionMap.get(
                                descriptor.eventTag)[descriptor.seriesValueIndex].getName());
            } else {
                return mEventDescriptionMap.get(descriptor.eventTag)[descriptor.seriesValueIndex]
                                                                     .getName();
            }
        }
        return "pid";
    }
    
    private String getFilterDescription(OccurrenceDisplayDescriptor descriptor) {
        if (descriptor.filterValueIndex != -1) {
            return String.format(" [%1$s %2$s %3$s]",
                    mEventDescriptionMap.get(
                            descriptor.eventTag)[descriptor.filterValueIndex].getName(),
                            descriptor.filterCompareMethod.testString(),
                            descriptor.filterValue != null ?
                                    descriptor.filterValue.toString() : "?"); //$NON-NLS-1$
        }
        return ""; //$NON-NLS-1$
    }

}
