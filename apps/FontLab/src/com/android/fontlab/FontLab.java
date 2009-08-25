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

package com.android.fontlab;

import java.util.Map;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class FontLab extends Activity {
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 60;
    
    private static final float SCALE_X_RANGE = 20;
    private static final int MAX_SCALE_X = 20;
    private static final int MIN_SCALE_X = -19;   // -20 would make zero-scale

    private static final int MAX_GAMMA = 40;
    private static final int MIN_GAMMA = 1;
    private static final float GAMMA_RANGE = 20;

    private static final String[] sText = {
        "Applications Contacts Maps Google Browser Text messages Address book"
        + " Development Earth Quake Settings Voicemail Zoolander. Four score"
        + " and seven years ago our fathers brought forth on this continent, a"
        + " new nation, conceived in Liberty, and dedicated to the proposition"
        + " that all men are created equal. Now we are engaged in a great civil"
        + " war, testing whether that nation, or any nation so conceived and so"
        + " dedicated, can long endure. We are met on a great battle-field of"
        + " that war. We have come to dedicate a portion of that field, as a"
        + " final resting place for those who here gave their lives that that"
        + " nation might live. It is altogether fitting and proper that we"
        + " should do this. But, in a larger sense, we can not dedicate - we"
        + " can not consecrate - we can not hallow - this ground. The brave"
        + " men, living and dead, who struggled here, have consecrated it, far"
        + " above our poor power to add or detract. The world will little note,"
        + " nor long remember what we say here, but it can never forget what"
        + " they did here. It is for us the living, rather, to be dedicated"
        + " here to the unfinished work which they who fought here have thus"
        + " far so nobly advanced. It is rather for us to be here dedicated to"
        + " the great task remaining before us - that from these honored dead"
        + " we take increased devotion to that cause for which they gave the"
        + " last full measure of devotion - that we here highly resolve that"
        + " these dead shall not have died in vain - that this nation, under"
        + " God, shall have a new birth of freedom - and that government of the"
        + " people, by the people, for the people, shall not perish from the"
        + " earth."
        ,
        "A Spanish doctor on Tuesday stood by his opinion that Fidel Castro is recovering from stomach surgery despite a newspaper report stating the Cuban leader is in a serious condition after a number of failed operations."
        + " When Senator Wayne Allard, Republican of Colorado, announced Monday that he would not seek re-election, the uphill battle for his party to reclaim the Senate in 2008 became an even steeper climb."
        + " Naomi Campbell was today sentenced to five days' community service and ordered to attend an anger management course after she admitted throwing a mobile phone at her maid."
        ,
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789 !@#$%^&*()-_=+[]\\{}|;':\",./<>?"
        ,
        "HaH HbH HcH HdH HeH HfH HgH HhH HiH HjH HkH HlH HmH HnH HoH HpH HqH HrH HsH HtH HuH HvH HwH HxH HyH HzH"
        + "HAH HBH HCH HDH HEH HFH HGH HHH HIH HJH HKH HLH HMH HNH HOH HPH HQH HRH HSH HTH HUH HVH HWH HXH HYH HZH"
    };
    
    private void updateText() {
        mTextIndex %= sText.length;
        String s = sText[mTextIndex];
        mColumn1.setText(s);
        mColumn2.setText(s);
    }
    
    public FontLab() {}

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.font_lab);
        
        mColumn1 = (TextView)findViewById(R.id.column1);
        mColumn2 = (TextView)findViewById(R.id.column2);
        mContentView = findViewById(R.id.content);

        
        mColumn1.setTextSize(mFontSize);
        mColumn2.setTextSize(mFontSize);
        
        mColumn1.setTextColor(Color.BLACK);
        mColumn1.setBackgroundDrawable(new PaintDrawable(Color.WHITE));
        mColumn2.setTextColor(Color.WHITE);
        mColumn2.setBackgroundDrawable(new PaintDrawable(Color.BLACK));
        
        refreshFont();
        updateTitle();
        updateText();
        
        setDefaultKeyMode(Activity.DEFAULT_KEYS_SHORTCUT);

        mColumn1.setPadding(5, 0, 5, 0);
        mColumn2.setPadding(5, 0, 5, 0);
    }
    
    private void updateTitle() {
        Typeface tf = mColumn1.getTypeface();
        String title = " PS=" + mFontSize + " X="
                    + (1 + mTextScaleXDelta/SCALE_X_RANGE)
                    + " G=" + (mGamma/GAMMA_RANGE)
                    + " S=" + ((mColumn1.getPaintFlags() & Paint.SUBPIXEL_TEXT_FLAG) != 0 ? 1 : 0)
                    + " " + sTypefaceName[mFontIndex]
                    + " " + sStyleName[tf.getStyle()]
                    ;
        setTitle(title);
    }
    
    /** Called when it is time to initialize the activity state. */
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }

    protected void onResume() {
        super.onResume();
    }
    
    private static final String sStyleName[] = {
        "Regular", "Bold", "Italic", "Bold Italic"
    };
    private static final String sTypefaceName[] = {
        "Sans",
        "Serif",
        "Mono"
    };
    private static final Typeface sTypeface[] = {
        Typeface.SANS_SERIF,
        Typeface.SERIF,
        Typeface.MONOSPACE
    };
    private static final int FONT_INDEX_SANS = 0;   // index into sTypeface
    private static final int FONT_INDEX_SERIF = 1;  // index into sTypeface
    private static final int FONT_INDEX_MONO = 2;   // index into sTypeface
    
    private static boolean canSupportStyle(Typeface tf, int styleBits) {
        tf = Typeface.create(tf, styleBits);
        return (tf.getStyle() & styleBits) == styleBits;
    }

    private void refreshFont() {
        Typeface tf = Typeface.create(sTypeface[mFontIndex], mFontStyle);
        mColumn1.setTypeface(tf);
        mColumn2.setTypeface(tf);
        updateTitle();
    }
    
    private MenuItem.OnMenuItemClickListener mFontClickListener = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            mFontIndex = item.getItemId();
            refreshFont();
            return true;
        }
    };
    
    private void addFontMenu(Menu menu, int index) {
        MenuItem item = menu.add(0, index, 0, sTypefaceName[index]);
        item.setCheckable(true);
        item.setOnMenuItemClickListener(mFontClickListener);
        item.setChecked(index == mFontIndex);
    }
    
    private MenuItem.OnMenuItemClickListener mStyleClickListener = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            mFontStyle = mFontStyle ^ item.getItemId();
            refreshFont();
            return true;
        }
    };
    
    private void addStyleMenu(Menu menu, int style, char shortCut) {
        MenuItem item = menu.add(0, style, 0, (style == Typeface.BOLD) ? "Bold" : "Italic");
        item.setCheckable(true);
        item.setOnMenuItemClickListener(mStyleClickListener);
        item.setChecked((mFontStyle & style) != 0);

        item.setVisible(canSupportStyle(sTypeface[mFontIndex], style));
        if (shortCut != 0) {
            item.setAlphabeticShortcut(shortCut);
        }
    }
    
    private MenuItem.OnMenuItemClickListener mFlagClickListener = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            int mask = item.getItemId();
            mColumn1.setPaintFlags(mColumn1.getPaintFlags() ^ mask);
            mColumn2.setPaintFlags(mColumn2.getPaintFlags() ^ mask);
            updateTitle();
            return true;
        }
    };
    
    private
    void addFlagMenu(Menu menu, int paintFlag, String label, char shortCut) {
        MenuItem item = menu.add(0, paintFlag, 0, label);
        item.setCheckable(true);
        item.setOnMenuItemClickListener(mFlagClickListener);
        item.setChecked((mColumn1.getPaintFlags() & paintFlag) != 0);
        if (shortCut != 0) {
            item.setAlphabeticShortcut(shortCut);
        }
    }
    
    private static void addListenerMenu(MenuItem item,
                                        MenuItem.OnMenuItemClickListener listener,
                                        char keyChar) {
        item.setOnMenuItemClickListener(listener);
        if (keyChar != '\0') {
            item.setAlphabeticShortcut(keyChar);
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }
    
    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();

        addFontMenu(menu, FONT_INDEX_SANS);
        addFontMenu(menu, FONT_INDEX_SERIF);
//        addFontMenu(menu, FONT_INDEX_MONO);
        addStyleMenu(menu, Typeface.BOLD, 'b');
        addStyleMenu(menu, Typeface.ITALIC, 'i');
        addFlagMenu(menu, Paint.SUBPIXEL_TEXT_FLAG, "SubPixel", 's');
        //        addFlagMenu(menu, Paint.DEV_KERN_TEXT_FLAG, "DevKern", 'k');
        menu.add(0, 0, 0, "Text").setOnMenuItemClickListener(mTextCallback).setAlphabeticShortcut('t');
        
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    String data, Map extras) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case BACKGROUND_PICKED:
                {
                    int color = ((Integer)extras.get("text")).intValue();
                    mColumn1.setTextColor(color);
                    mColumn2.setTextColor(color);
                    
                    int colorTranslucent = (color & 0x00FFFFFF) + 0x77000000;
                    
                    setTitleColor(color);
                    
                    Integer texture = (Integer)extras.get("texture");
                    if (texture != null) {
                        mContentView.setBackgroundResource(texture.intValue());
                    } else {
                        color = ((Integer)extras.get("bgcolor")).intValue();
                        mContentView.setBackgroundColor(color);
                    }
                }
                break;   
            }
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        int size = mFontSize;
        int scaleX = mTextScaleXDelta;
        
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                size -= 1;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                size += 1;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                scaleX += 1;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                scaleX -= 1;
                break;
            case KeyEvent.KEYCODE_U:
            case KeyEvent.KEYCODE_VOLUME_UP:
                changeGamma(1);
                return true;
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                changeGamma(-1);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
        
        size = Math.min(MAX_SIZE, Math.max(MIN_SIZE, size));
        if (size != mFontSize) {
            mFontSize = size;
            mColumn1.setTextSize(mFontSize);
            mColumn2.setTextSize(mFontSize);
            updateTitle();
            return true;
        }
        
        scaleX = Math.min(MAX_SCALE_X, Math.max(MIN_SCALE_X, scaleX));
        if (scaleX != mTextScaleXDelta) {
            mTextScaleXDelta = scaleX;
            mColumn1.setTextScaleX(1 + scaleX / SCALE_X_RANGE);
            mColumn2.setTextScaleX(1 + scaleX / SCALE_X_RANGE);
            updateTitle();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // default to gamma of 1.0
    private int mGamma = Math.round(1.0f * GAMMA_RANGE);

    private void changeGamma(int delta) {
        int gamma = Math.min(MAX_GAMMA, Math.max(MIN_GAMMA, mGamma + delta));
        if (gamma != mGamma) {
            mGamma = gamma;
            updateTitle();
            float blackGamma = mGamma / GAMMA_RANGE;
            Typeface.setGammaForText(blackGamma, 1 / blackGamma);
            mContentView.invalidate();
        }
    }
    
    private void setFont(TextView t, TextView f, Map extras) {
        int style = ((Integer)extras.get("style")).intValue();
        String font = (String)extras.get("font");
        t.setTypeface(Typeface.create(font, style));
        
        f.setText((String)extras.get("title"));
    }

    MenuItem.OnMenuItemClickListener mTextCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            mTextIndex += 1;
            updateText();
            return true;
        }
    };

    private static final int BACKGROUND_PICKED = 1;
    
    private TextView mColumn1;
    private TextView mColumn2;
    private View mContentView;
    private int mFontIndex = FONT_INDEX_SANS;
    private int mFontStyle = Typeface.NORMAL;
    private int mFontSize = 18;
    private int mTextIndex;
    private int mTextScaleXDelta;
}

