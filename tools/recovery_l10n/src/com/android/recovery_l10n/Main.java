/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.recovery_l10n;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

/**
 * This activity assists in generating the specially-formatted bitmaps
 * of text needed for recovery's localized text display.  Each image
 * contains all the translations of a single string; above each
 * translation is a "header row" that encodes that subimage's width,
 * height, and locale using pixel values.
 *
 * To use this app to generate new translations:
 *
 *   - Update the string resources in res/values-*
 *
 *   - Build and run the app.  Select the string you want to
 *     translate, and press the "Go" button.
 *
 *   - Wait for it to finish cycling through all the strings, then
 *     pull /data/data/com.android.recovery_l10n/files/text-out.png
 *     from the device.
 *
 *   - "pngcrush -c 0 text-out.png output.png"
 *
 *   - Put output.png in bootable/recovery/res/images/ (renamed
 *     appropriately).
 *
 * Recovery expects 8-bit 1-channel images (white text on black
 * background).  pngcrush -c 0 will convert the output of this program
 * to such an image.  If you use any other image handling tools,
 * remember that they must be lossless to preserve the exact values of
 * pixels in the header rows; don't convert them to jpeg or anything.
 */

public class Main extends Activity {
    private static final String TAG = "RecoveryL10N";

    HashMap<Locale, Bitmap> savedBitmaps;
    TextView mText;
    int mStringId = R.string.recovery_installing;

    public class TextCapture implements Runnable {
        private Locale nextLocale;
        private Locale thisLocale;
        private Runnable next;

        TextCapture(Locale thisLocale, Locale nextLocale, Runnable next) {
            this.nextLocale = nextLocale;
            this.thisLocale = thisLocale;
            this.next = next;
        }

        public void run() {
            Bitmap b = mText.getDrawingCache();
            savedBitmaps.put(thisLocale, b.copy(Bitmap.Config.ARGB_8888, false));

            if (nextLocale != null) {
                switchTo(nextLocale);
            }

            if (next != null) {
                mText.postDelayed(next, 200);
            }
        }
    }

    private void switchTo(Locale locale) {
        Resources standardResources = getResources();
        AssetManager assets = standardResources.getAssets();
        DisplayMetrics metrics = standardResources.getDisplayMetrics();
        Configuration config = new Configuration(standardResources.getConfiguration());
        config.locale = locale;
        Resources defaultResources = new Resources(assets, metrics, config);

        mText.setText(mStringId);

        mText.setDrawingCacheEnabled(false);
        mText.setDrawingCacheEnabled(true);
        mText.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.main);

        savedBitmaps = new HashMap<Locale, Bitmap>();

        Spinner spinner = (Spinner) findViewById(R.id.which);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.string_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView parent, View view,
                                       int pos, long id) {
                switch (pos) {
                    case 0: mStringId = R.string.recovery_installing; break;
                    case 1: mStringId = R.string.recovery_erasing; break;
                    case 2: mStringId = R.string.recovery_no_command; break;
                    case 3: mStringId = R.string.recovery_error; break;
                }
            }
            @Override public void onNothingSelected(AdapterView parent) { }
            });

        mText = (TextView) findViewById(R.id.text);

        String[] localeNames = getAssets().getLocales();
        Arrays.sort(localeNames);
        ArrayList<Locale> locales = new ArrayList<Locale>();
        for (String ln : localeNames) {
            int u = ln.indexOf('_');
            if (u >= 0) {
                Log.i(TAG, "locale = " + ln);
                locales.add(new Locale(ln.substring(0, u), ln.substring(u+1)));
            }
        }

        final Runnable seq = buildSequence(locales.toArray(new Locale[0]));

        Button b = (Button) findViewById(R.id.go);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View ignore) {
                mText.post(seq);
            }
            });
    }

    private Runnable buildSequence(final Locale[] locales) {
        Runnable head = new Runnable() { public void run() { mergeBitmaps(locales); } };
        Locale prev = null;
        for (Locale loc : locales) {
            head = new TextCapture(loc, prev, head);
            prev = loc;
        }
        final Runnable fhead = head;
        final Locale floc = prev;
        return new Runnable() { public void run() { startSequence(fhead, floc); } };
    }

    private void startSequence(Runnable firstRun, Locale firstLocale) {
        savedBitmaps.clear();
        switchTo(firstLocale);
        mText.postDelayed(firstRun, 200);
    }

    private void saveBitmap(Bitmap b, String filename) {
        try {
            FileOutputStream fos = openFileOutput(filename, 0);
            b.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            Log.i(TAG, "failed to write PNG", e);
        }
    }

    private int colorFor(byte b) {
        return 0xff000000 | (b<<16) | (b<<8) | b;
    }

    private int colorFor(int b) {
        return 0xff000000 | (b<<16) | (b<<8) | b;
    }

    private void mergeBitmaps(final Locale[] locales) {
        HashMap<String, Integer> countByLanguage = new HashMap<String, Integer>();

        int height = 2;
        int width = 10;
        int maxHeight = 0;
        for (Locale loc : locales) {
            Bitmap b = savedBitmaps.get(loc);
            int h = b.getHeight();
            int w = b.getWidth();
            height += h+1;
            if (h > maxHeight) maxHeight = h;
            if (w > width) width = w;

            String lang = loc.getLanguage();
            if (countByLanguage.containsKey(lang)) {
                countByLanguage.put(lang, countByLanguage.get(lang)+1);
            } else {
                countByLanguage.put(lang, 1);
            }
        }

        Log.i(TAG, "output bitmap is " + width + " x " + height);
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.eraseColor(0xff000000);
        int[] pixels = new int[maxHeight * width];

        int p = 0;
        for (Locale loc : locales) {
            Bitmap bm = savedBitmaps.get(loc);
            int h = bm.getHeight();
            int w = bm.getWidth();

            bm.getPixels(pixels, 0, w, 0, 0, w, h);

            // Find the rightmost and leftmost columns with any
            // nonblack pixels; we'll copy just that region to the
            // output image.

            int right = w;
            while (right > 1) {
                boolean all_black = true;
                for (int j = 0; j < h; ++j) {
                    if (pixels[j*w+right-1] != 0xff000000) {
                        all_black = false;
                        break;
                    }
                }
                if (all_black) {
                    --right;
                } else {
                    break;
                }
            }

            int left = 0;
            while (left < right-1) {
                boolean all_black = true;
                for (int j = 0; j < h; ++j) {
                    if (pixels[j*w+left] != 0xff000000) {
                        all_black = false;
                        break;
                    }
                }
                if (all_black) {
                    ++left;
                } else {
                    break;
                }
            }

            // Make the last country variant for a given language be
            // the catch-all for that language (because recovery will
            // take the first one that matches).
            String lang = loc.getLanguage();
            if (countByLanguage.get(lang) > 1) {
                countByLanguage.put(lang, countByLanguage.get(lang)-1);
                lang = loc.toString();
            }
            int tw = right - left;
            Log.i(TAG, "encoding \"" + loc + "\" as \"" + lang + "\": " + tw + " x " + h);
            byte[] langBytes = lang.getBytes();
            out.setPixel(0, p, colorFor(tw & 0xff));
            out.setPixel(1, p, colorFor(tw >>> 8));
            out.setPixel(2, p, colorFor(h & 0xff));
            out.setPixel(3, p, colorFor(h >>> 8));
            out.setPixel(4, p, colorFor(langBytes.length));
            int x = 5;
            for (byte b : langBytes) {
                out.setPixel(x, p, colorFor(b));
                x++;
            }
            out.setPixel(x, p, colorFor(0));

            p++;

            out.setPixels(pixels, left, w, 0, p, tw, h);
            p += h;
        }

        // if no languages match, suppress text display by using a
        // single black pixel as the image.
        out.setPixel(0, p, colorFor(1));
        out.setPixel(1, p, colorFor(0));
        out.setPixel(2, p, colorFor(1));
        out.setPixel(3, p, colorFor(0));
        out.setPixel(4, p, colorFor(0));
        p++;

        saveBitmap(out, "text-out.png");
        Log.i(TAG, "wrote text-out.png");
    }
}
