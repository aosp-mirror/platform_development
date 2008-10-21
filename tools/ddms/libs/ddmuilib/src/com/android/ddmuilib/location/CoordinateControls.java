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

package com.android.ddmuilib.location;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Encapsulation of controls handling a location coordinate in decimal and sexagesimal.
 * <p/>This handle the conversion between both modes automatically by using a {@link ModifyListener}
 * on all the {@link Text} widgets.
 * <p/>To get/set the coordinate, use {@link #setValue(double)} and {@link #getValue()} (preceded by
 * a call to {@link #isValueValid()})
 */
public final class CoordinateControls {
    private double mValue;
    private boolean mValueValidity = false;
    private Text mDecimalText;
    private Text mSexagesimalDegreeText;
    private Text mSexagesimalMinuteText;
    private Text mSexagesimalSecondText;
    
    /** Internal flag to prevent {@link ModifyEvent} to be sent when {@link Text#setText(String)}
     * is called. This is an int instead of a boolean to act as a counter. */
    private int mManualTextChange = 0;
    
    /**
     * ModifyListener for the 3 {@link Text} controls of the sexagesimal mode.
     */
    private ModifyListener mSexagesimalListener = new ModifyListener() {
        public void modifyText(ModifyEvent event) {
            if (mManualTextChange > 0) {
                return;
            }
            try {
                mValue = getValueFromSexagesimalControls();
                setValueIntoDecimalControl(mValue);
                mValueValidity = true;
            } catch (NumberFormatException e) {
                // wrong format empty the decimal controls.
                mValueValidity = false;
                resetDecimalControls();
            }
        }
    };
    
    /**
     * Creates the {@link Text} control for the decimal display of the coordinate.
     * <p/>The control is expected to be placed in a Composite using a {@link GridLayout}.
     * @param parent The {@link Composite} parent of the control.
     */
    public void createDecimalText(Composite parent) {
        mDecimalText = createTextControl(parent, "-199.999999", new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                if (mManualTextChange > 0) {
                    return;
                }
                try {
                    mValue = Double.parseDouble(mDecimalText.getText());
                    setValueIntoSexagesimalControl(mValue);
                    mValueValidity = true;
                } catch (NumberFormatException e) {
                    // wrong format empty the sexagesimal controls.
                    mValueValidity = false;
                    resetSexagesimalControls();
                }
            }
        });
    }
    
    /**
     * Creates the {@link Text} control for the "degree" display of the coordinate in sexagesimal
     * mode.
     * <p/>The control is expected to be placed in a Composite using a {@link GridLayout}.
     * @param parent The {@link Composite} parent of the control.
     */
    public void createSexagesimalDegreeText(Composite parent) {
        mSexagesimalDegreeText = createTextControl(parent, "-199", mSexagesimalListener); //$NON-NLS-1$
    }
    
    /**
     * Creates the {@link Text} control for the "minute" display of the coordinate in sexagesimal
     * mode.
     * <p/>The control is expected to be placed in a Composite using a {@link GridLayout}.
     * @param parent The {@link Composite} parent of the control.
     */
    public void createSexagesimalMinuteText(Composite parent) {
        mSexagesimalMinuteText = createTextControl(parent, "99", mSexagesimalListener); //$NON-NLS-1$
    }

    /**
     * Creates the {@link Text} control for the "second" display of the coordinate in sexagesimal
     * mode.
     * <p/>The control is expected to be placed in a Composite using a {@link GridLayout}.
     * @param parent The {@link Composite} parent of the control.
     */
    public void createSexagesimalSecondText(Composite parent) {
        mSexagesimalSecondText = createTextControl(parent, "99.999", mSexagesimalListener); //$NON-NLS-1$
    }
    
    /**
     * Sets the coordinate into the {@link Text} controls.
     * @param value the coordinate value to set.
     */
    public void setValue(double value) {
        mValue = value;
        mValueValidity = true;
        setValueIntoDecimalControl(value);
        setValueIntoSexagesimalControl(value);
    }
    
    /**
     * Returns whether the value in the control(s) is valid.
     */
    public boolean isValueValid() {
        return mValueValidity;
    }

    /**
     * Returns the current value set in the control(s).
     * <p/>This value can be erroneous, and a check with {@link #isValueValid()} should be performed
     * before any call to this method.
     */
    public double getValue() {
        return mValue;
    }
    
    /**
     * Enables or disables all the {@link Text} controls.
     * @param enabled the enabled state.
     */
    public void setEnabled(boolean enabled) {
        mDecimalText.setEnabled(enabled);
        mSexagesimalDegreeText.setEnabled(enabled);
        mSexagesimalMinuteText.setEnabled(enabled);
        mSexagesimalSecondText.setEnabled(enabled);
    }
    
    private void resetDecimalControls() {
        mManualTextChange++;
        mDecimalText.setText(""); //$NON-NLS-1$
        mManualTextChange--;
    }

    private void resetSexagesimalControls() {
        mManualTextChange++;
        mSexagesimalDegreeText.setText(""); //$NON-NLS-1$
        mSexagesimalMinuteText.setText(""); //$NON-NLS-1$
        mSexagesimalSecondText.setText(""); //$NON-NLS-1$
        mManualTextChange--;
    }
    
    /**
     * Creates a {@link Text} with a given parent, default string and a {@link ModifyListener}
     * @param parent the parent {@link Composite}.
     * @param defaultString the default string to be used to compute the {@link Text} control
     * size hint.
     * @param listener the {@link ModifyListener} to be called when the {@link Text} control is
     * modified.
     */
    private Text createTextControl(Composite parent, String defaultString,
            ModifyListener listener) {
        // create the control
        Text text = new Text(parent, SWT.BORDER | SWT.LEFT | SWT.SINGLE);
        
        // add the standard listener to it.
        text.addModifyListener(listener);
        
        // compute its size/
        mManualTextChange++;
        text.setText(defaultString);
        text.pack();
        Point size = text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        text.setText(""); //$NON-NLS-1$
        mManualTextChange--;
        
        GridData gridData = new GridData();
        gridData.widthHint = size.x;
        text.setLayoutData(gridData);
        
        return text;
    }
    
    private double getValueFromSexagesimalControls() throws NumberFormatException {
        double degrees = Double.parseDouble(mSexagesimalDegreeText.getText());
        double minutes = Double.parseDouble(mSexagesimalMinuteText.getText());
        double seconds = Double.parseDouble(mSexagesimalSecondText.getText());
        
        boolean isPositive = (degrees >= 0.);
        degrees = Math.abs(degrees);

        double value = degrees + minutes / 60. + seconds / 3600.; 
        return isPositive ? value : - value;
    }

    private void setValueIntoDecimalControl(double value) {
        mManualTextChange++;
        mDecimalText.setText(String.format("%.6f", value));
        mManualTextChange--;
    }
    
    private void setValueIntoSexagesimalControl(double value) {
        // get the sign and make the number positive no matter what.
        boolean isPositive = (value >= 0.);
        value = Math.abs(value);
        
        // get the degree
        double degrees = Math.floor(value);
        
        // get the minutes
        double minutes = Math.floor((value - degrees) * 60.);
        
        // get the seconds.
        double seconds = (value - degrees) * 3600. - minutes * 60.;
        
        mManualTextChange++;
        mSexagesimalDegreeText.setText(
                Integer.toString(isPositive ? (int)degrees : (int)- degrees));
        mSexagesimalMinuteText.setText(Integer.toString((int)minutes));
        mSexagesimalSecondText.setText(String.format("%.3f", seconds)); //$NON-NLS-1$
        mManualTextChange--;
    }
}
