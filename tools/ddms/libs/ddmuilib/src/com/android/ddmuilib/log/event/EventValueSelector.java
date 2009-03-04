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

import com.android.ddmlib.log.EventLogParser;
import com.android.ddmlib.log.EventValueDescription;
import com.android.ddmlib.log.EventContainer.CompareMethod;
import com.android.ddmlib.log.EventContainer.EventValueType;
import com.android.ddmuilib.log.event.EventDisplay.OccurrenceDisplayDescriptor;
import com.android.ddmuilib.log.event.EventDisplay.ValueDisplayDescriptor;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

final class EventValueSelector extends Dialog {
    private static final int DLG_WIDTH = 400;
    private static final int DLG_HEIGHT = 300;

    private Shell mParent;
    private Shell mShell;
    private boolean mEditStatus;
    private Combo mEventCombo;
    private Combo mValueCombo;
    private Combo mSeriesCombo;
    private Button mDisplayPidCheckBox;
    private Combo mFilterCombo;
    private Combo mFilterMethodCombo;
    private Text mFilterValue;
    private Button mOkButton;

    private EventLogParser mLogParser;
    private OccurrenceDisplayDescriptor mDescriptor;
    
    /** list of event integer in the order of the combo. */
    private Integer[] mEventTags;
    
    /** list of indices in the {@link EventValueDescription} array of the current event
     * that are of type string. This lets us get back the {@link EventValueDescription} from the
     * index in the Series {@link Combo}.
     */
    private final ArrayList<Integer> mSeriesIndices = new ArrayList<Integer>();
    
    public EventValueSelector(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
    }

    /**
     * Opens the display option dialog to edit a new descriptor.
     * @param decriptorClass the class of the object to instantiate. Must extend
     * {@link OccurrenceDisplayDescriptor}
     * @param logParser
     * @return true if the object is to be created, false if the creation was canceled.
     */
    boolean open(Class<? extends OccurrenceDisplayDescriptor> descriptorClass,
            EventLogParser logParser) {
        try {
            OccurrenceDisplayDescriptor descriptor = descriptorClass.newInstance();
            setModified();
            return open(descriptor, logParser);
        } catch (InstantiationException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * Opens the display option dialog, to edit a {@link OccurrenceDisplayDescriptor} object or
     * a {@link ValueDisplayDescriptor} object.
     * @param descriptor The descriptor to edit.
     * @return true if the object was modified.
     */
    boolean open(OccurrenceDisplayDescriptor descriptor, EventLogParser logParser) {
        // make a copy of the descriptor as we'll use a working copy.
        if (descriptor instanceof ValueDisplayDescriptor) {
            mDescriptor = new ValueDisplayDescriptor((ValueDisplayDescriptor)descriptor);
        } else if (descriptor instanceof OccurrenceDisplayDescriptor) {
            mDescriptor = new OccurrenceDisplayDescriptor(descriptor);
        } else {
            return false;
        }

        mLogParser = logParser;

        createUI();

        if (mParent == null || mShell == null) {
            return false;
        }

        loadValueDescriptor();
        
        checkValidity();

        // Set the dialog size.
        try { 
            mShell.setMinimumSize(DLG_WIDTH, DLG_HEIGHT);
            Rectangle r = mParent.getBounds();
            // get the center new top left.
            int cx = r.x + r.width/2;
            int x = cx - DLG_WIDTH / 2;
            int cy = r.y + r.height/2;
            int y = cy - DLG_HEIGHT / 2;
            mShell.setBounds(x, y, DLG_WIDTH, DLG_HEIGHT);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
    
    OccurrenceDisplayDescriptor getDescriptor() {
        return mDescriptor;
    }
    
    private void createUI() {
        GridData gd;

        mParent = getParent();
        mShell = new Shell(mParent, getStyle());
        mShell.setText("Event Display Configuration");

        mShell.setLayout(new GridLayout(2, false));
        
        Label l = new Label(mShell, SWT.NONE);
        l.setText("Event:");
        
        mEventCombo = new Combo(mShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        mEventCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // the event tag / event name map
        Map<Integer, String> eventTagMap = mLogParser.getTagMap();
        Map<Integer, EventValueDescription[]> eventInfoMap = mLogParser.getEventInfoMap();
        Set<Integer> keys = eventTagMap.keySet();
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (Integer i : keys) {
            if (eventInfoMap.get(i) != null) {
                String eventName = eventTagMap.get(i);
                mEventCombo.add(eventName);
                
                list.add(i);
            }
        }
        mEventTags = list.toArray(new Integer[list.size()]);
        
        mEventCombo.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleEventComboSelection();
                setModified();
            }
        });

        l = new Label(mShell, SWT.NONE);
        l.setText("Value:");
        
        mValueCombo = new Combo(mShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        mValueCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mValueCombo.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleValueComboSelection();
                setModified();
            }
        });

        l = new Label(mShell, SWT.NONE);
        l.setText("Series Name:");

        mSeriesCombo = new Combo(mShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        mSeriesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSeriesCombo.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSeriesComboSelection();
                setModified();
            }
        });

        // empty comp
        new Composite(mShell, SWT.NONE).setLayoutData(gd = new GridData());
        gd.heightHint = gd.widthHint = 0;

        mDisplayPidCheckBox = new Button(mShell, SWT.CHECK);
        mDisplayPidCheckBox.setText("Also Show pid");
        mDisplayPidCheckBox.setEnabled(false);
        mDisplayPidCheckBox.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                mDescriptor.includePid = mDisplayPidCheckBox.getSelection();
                setModified();
            }
        });

        l = new Label(mShell, SWT.NONE);
        l.setText("Filter By:");

        mFilterCombo = new Combo(mShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        mFilterCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mFilterCombo.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleFilterComboSelection();
                setModified();
            }
        });

        l = new Label(mShell, SWT.NONE);
        l.setText("Filter Method:");

        mFilterMethodCombo = new Combo(mShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        mFilterMethodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (CompareMethod method : CompareMethod.values()) {
            mFilterMethodCombo.add(method.toString());
        }
        mFilterMethodCombo.select(0);
        mFilterMethodCombo.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleFilterMethodComboSelection();
                setModified();
            }
        });

        l = new Label(mShell, SWT.NONE);
        l.setText("Filter Value:");
        
        mFilterValue = new Text(mShell, SWT.BORDER | SWT.SINGLE);
        mFilterValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mFilterValue.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (mDescriptor.filterValueIndex != -1) {
                    // get the current selection in the event combo
                    int index = mEventCombo.getSelectionIndex();
    
                    if (index != -1) {
                        // match it to an event
                        int eventTag = mEventTags[index];
                        mDescriptor.eventTag = eventTag;
                        
                        // get the EventValueDescription for this tag
                        EventValueDescription valueDesc = mLogParser.getEventInfoMap()
                            .get(eventTag)[mDescriptor.filterValueIndex];
                        
                        // let the EventValueDescription convert the String value into an object
                        // of the proper type.
                        mDescriptor.filterValue = valueDesc.getObjectFromString(
                                mFilterValue.getText().trim());
                        setModified();
                    }
                }
            }
        });
        
        // add a separator spanning the 2 columns
        
        l = new Label(mShell, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        l.setLayoutData(gd);
        
        // add a composite to hold the ok/cancel button, no matter what the columns size are.
        Composite buttonComp = new Composite(mShell, SWT.NONE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        buttonComp.setLayoutData(gd);
        GridLayout gl;
        buttonComp.setLayout(gl = new GridLayout(6, true));
        gl.marginHeight = gl.marginWidth = 0;

        Composite padding = new Composite(mShell, SWT.NONE);
        padding.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mOkButton = new Button(buttonComp, SWT.PUSH);
        mOkButton.setText("OK");
        mOkButton.setLayoutData(new GridData(GridData.CENTER));
        mOkButton.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                mShell.close();
            }
        });

        padding = new Composite(mShell, SWT.NONE);
        padding.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        padding = new Composite(mShell, SWT.NONE);
        padding.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Button cancelButton = new Button(buttonComp, SWT.PUSH);
        cancelButton.setText("Cancel");
        cancelButton.setLayoutData(new GridData(GridData.CENTER));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                // cancel the edit
                mEditStatus = false;
                mShell.close();
            }
        });

        padding = new Composite(mShell, SWT.NONE);
        padding.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        mShell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                event.doit = true;
            }
        });
    }

    private void setModified() {
        mEditStatus = true;
    }

    private void handleEventComboSelection() {
        // get the current selection in the event combo
        int index = mEventCombo.getSelectionIndex();

        if (index != -1) {
            // match it to an event
            int eventTag = mEventTags[index];
            mDescriptor.eventTag = eventTag;
            
            // get the EventValueDescription for this tag
            EventValueDescription[] values = mLogParser.getEventInfoMap().get(eventTag);
            
            // fill the combo for the values
            mValueCombo.removeAll();
            if (values != null) {
                if (mDescriptor instanceof ValueDisplayDescriptor) {
                    ValueDisplayDescriptor valueDescriptor = (ValueDisplayDescriptor)mDescriptor;
    
                    mValueCombo.setEnabled(true);
                    for (EventValueDescription value : values) {
                        mValueCombo.add(value.toString());
                    }
                    
                    if (valueDescriptor.valueIndex != -1) {
                        mValueCombo.select(valueDescriptor.valueIndex);
                    } else {
                        mValueCombo.clearSelection();
                    }
                } else {
                    mValueCombo.setEnabled(false);
                }

                // fill the axis combo
                mSeriesCombo.removeAll();
                mSeriesCombo.setEnabled(false);
                mSeriesIndices.clear();
                int axisIndex = 0;
                int selectionIndex = -1;
                for (EventValueDescription value : values) {
                    if (value.getEventValueType() == EventValueType.STRING) {
                        mSeriesCombo.add(value.getName());
                        mSeriesCombo.setEnabled(true);
                        mSeriesIndices.add(axisIndex);
                        
                        if (mDescriptor.seriesValueIndex != -1 &&
                                mDescriptor.seriesValueIndex == axisIndex) {
                            selectionIndex = axisIndex;
                        }
                    }
                    axisIndex++;
                }

                if (mSeriesCombo.isEnabled()) {
                    mSeriesCombo.add("default (pid)", 0 /* index */);
                    mSeriesIndices.add(0 /* index */, -1 /* value */);

                    // +1 because we added another item at index 0
                    mSeriesCombo.select(selectionIndex + 1);
                    
                    if (selectionIndex >= 0) {
                        mDisplayPidCheckBox.setSelection(mDescriptor.includePid);
                        mDisplayPidCheckBox.setEnabled(true);
                    } else {
                        mDisplayPidCheckBox.setEnabled(false);
                        mDisplayPidCheckBox.setSelection(false);
                    }
                } else {
                    mDisplayPidCheckBox.setSelection(false);
                    mDisplayPidCheckBox.setEnabled(false);
                }
                
                // fill the filter combo
                mFilterCombo.setEnabled(true);
                mFilterCombo.removeAll();
                mFilterCombo.add("(no filter)");
                for (EventValueDescription value : values) {
                    mFilterCombo.add(value.toString());
                }
                
                // select the current filter
                mFilterCombo.select(mDescriptor.filterValueIndex + 1);
                mFilterMethodCombo.select(getFilterMethodIndex(mDescriptor.filterCompareMethod));

                // fill the current filter value
                if (mDescriptor.filterValueIndex != -1) {
                    EventValueDescription valueInfo = values[mDescriptor.filterValueIndex];
                    if (valueInfo.checkForType(mDescriptor.filterValue)) {
                        mFilterValue.setText(mDescriptor.filterValue.toString());
                    } else {
                        mFilterValue.setText("");
                    }
                } else {
                    mFilterValue.setText("");
                }
            } else {
                disableSubCombos();
            }
        } else {
            disableSubCombos();
        }
        
        checkValidity();
    }

    /**
     * 
     */
    private void disableSubCombos() {
        mValueCombo.removeAll();
        mValueCombo.clearSelection();
        mValueCombo.setEnabled(false);

        mSeriesCombo.removeAll();
        mSeriesCombo.clearSelection();
        mSeriesCombo.setEnabled(false);
        
        mDisplayPidCheckBox.setEnabled(false);
        mDisplayPidCheckBox.setSelection(false);
        
        mFilterCombo.removeAll();
        mFilterCombo.clearSelection();
        mFilterCombo.setEnabled(false);
        
        mFilterValue.setEnabled(false);
        mFilterValue.setText("");
        mFilterMethodCombo.setEnabled(false);
    }

    private void handleValueComboSelection() {
        ValueDisplayDescriptor valueDescriptor = (ValueDisplayDescriptor)mDescriptor;

        // get the current selection in the value combo
        int index = mValueCombo.getSelectionIndex();
        valueDescriptor.valueIndex = index;
        
        // for now set the built-in name

        // get the current selection in the event combo
        int eventIndex = mEventCombo.getSelectionIndex();
        
        // match it to an event
        int eventTag = mEventTags[eventIndex];
        
        // get the EventValueDescription for this tag
        EventValueDescription[] values = mLogParser.getEventInfoMap().get(eventTag);

        valueDescriptor.valueName = values[index].getName();
        
        checkValidity();
    }

    private void handleSeriesComboSelection() {
        // get the current selection in the axis combo
        int index = mSeriesCombo.getSelectionIndex();
        
        // get the actual value index from the list.
        int valueIndex = mSeriesIndices.get(index);
        
        mDescriptor.seriesValueIndex = valueIndex;
        
        if (index > 0) {
            mDisplayPidCheckBox.setEnabled(true);
            mDisplayPidCheckBox.setSelection(mDescriptor.includePid);
        } else {
            mDisplayPidCheckBox.setSelection(false);
            mDisplayPidCheckBox.setEnabled(false);
        }
    }

    private void handleFilterComboSelection() {
        // get the current selection in the axis combo
        int index = mFilterCombo.getSelectionIndex();
        
        // decrement index by 1 since the item 0 means
        // no filter (index = -1), and the rest is offset by 1
        index--;

        mDescriptor.filterValueIndex = index;
        
        if (index != -1) {
            mFilterValue.setEnabled(true);
            mFilterMethodCombo.setEnabled(true);
            if (mDescriptor.filterValue instanceof String) {
                mFilterValue.setText((String)mDescriptor.filterValue);
            }
        } else {
            mFilterValue.setText("");
            mFilterValue.setEnabled(false);
            mFilterMethodCombo.setEnabled(false);
        }
    }
    
    private void handleFilterMethodComboSelection() {
        // get the current selection in the axis combo
        int index = mFilterMethodCombo.getSelectionIndex();
        CompareMethod method = CompareMethod.values()[index];
        
        mDescriptor.filterCompareMethod = method;
    }

    /**
     * Returns the index of the filter method
     * @param filterCompareMethod the {@link CompareMethod} enum.
     */
    private int getFilterMethodIndex(CompareMethod filterCompareMethod) {
        CompareMethod[] values = CompareMethod.values();
        for (int i = 0 ; i < values.length ; i++) {
            if (values[i] == filterCompareMethod) {
                return i;
            }
        }
        return -1;
    }


    private void loadValueDescriptor() {
        // get the index from the eventTag.
        int eventIndex = 0;
        int comboIndex = -1;
        for (int i : mEventTags) {
            if (i == mDescriptor.eventTag) {
                comboIndex = eventIndex;
                break;
            }
            eventIndex++;
        }
        
        if (comboIndex == -1) {
            mEventCombo.clearSelection();
        } else {
            mEventCombo.select(comboIndex);
        }

        // get the event from the descriptor
        handleEventComboSelection();
    }
    
    private void checkValidity() {
        mOkButton.setEnabled(mEventCombo.getSelectionIndex() != -1 &&
                (((mDescriptor instanceof ValueDisplayDescriptor) == false) ||
                        mValueCombo.getSelectionIndex() != -1));
    }
}
