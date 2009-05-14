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


package com.android.ide.eclipse.adt.internal.editors;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.sdklib.SdkConstants;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import java.util.HashMap;

/**
 * Factory to generate icons for Android Editors.
 * <p/>
 * Icons are kept here and reused.
 */
public class IconFactory {

    public static final int COLOR_RED     = SWT.COLOR_DARK_RED;
    public static final int COLOR_GREEN   = SWT.COLOR_DARK_GREEN;
    public static final int COLOR_BLUE    = SWT.COLOR_DARK_BLUE;
    public static final int COLOR_DEFAULT = SWT.COLOR_BLACK;

    public static final int SHAPE_CIRCLE  = 'C';
    public static final int SHAPE_RECT    = 'R';
    public static final int SHAPE_DEFAULT = SHAPE_CIRCLE;
    
    private static IconFactory sInstance;

    private HashMap<String, Image> mIconMap = new HashMap<String, Image>();
    private HashMap<String, ImageDescriptor> mImageDescMap = new HashMap<String, ImageDescriptor>();
    
    private IconFactory() {
    }
    
    public static synchronized IconFactory getInstance() {
        if (sInstance == null) {
            sInstance = new IconFactory();
        }
        return sInstance;
    }
    
    public void Dispose() {
        // Dispose icons
        for (Image icon : mIconMap.values()) {
            // The map can contain null values
            if (icon != null) {
                icon.dispose();
            }
        }
        mIconMap.clear();
    }

    /**
     * Returns an Image for a given icon name.
     * <p/>
     * Callers should not dispose it.
     * 
     * @param osName The leaf name, without the extension, of an existing icon in the
     *        editor's "icons" directory. If it doesn't exists, a default icon will be
     *        generated automatically based on the name.
     */
    public Image getIcon(String osName) {
        return getIcon(osName, COLOR_DEFAULT, SHAPE_DEFAULT);
    }

    /**
     * Returns an Image for a given icon name.
     * <p/>
     * Callers should not dispose it.
     * 
     * @param osName The leaf name, without the extension, of an existing icon in the
     *        editor's "icons" directory. If it doesn't exists, a default icon will be
     *        generated automatically based on the name.
     * @param color The color of the text in the automatically generated icons,
     *        one of COLOR_DEFAULT, COLOR_RED, COLOR_BLUE or COLOR_RED.
     * @param shape The shape of the icon in the automatically generated icons,
     *        one of SHAPE_DEFAULT, SHAPE_CIRCLE or SHAPE_RECT.
     */
    public Image getIcon(String osName, int color, int shape) {
        String key = Character.toString((char) shape) + Integer.toString(color) + osName;
        Image icon = mIconMap.get(key);
        if (icon == null && !mIconMap.containsKey(key)) {
            ImageDescriptor id = getImageDescriptor(osName, color, shape);
            if (id != null) {
                icon = id.createImage();
            }
            // Note that we store null references in the icon map, to avoid looking them
            // up every time. If it didn't exist once, it will not exist later.
            mIconMap.put(key, icon);
        }
        return icon;
    }

    /**
     * Returns an ImageDescriptor for a given icon name.
     * <p/>
     * Callers should not dispose it.
     * 
     * @param osName The leaf name, without the extension, of an existing icon in the
     *        editor's "icons" directory. If it doesn't exists, a default icon will be
     *        generated automatically based on the name.
     */
    public ImageDescriptor getImageDescriptor(String osName) {
        return getImageDescriptor(osName, COLOR_DEFAULT, SHAPE_DEFAULT);
    }
    
    /**
     * Returns an ImageDescriptor for a given icon name.
     * <p/>
     * Callers should not dispose it.
     * 
     * @param osName The leaf name, without the extension, of an existing icon in the
     *        editor's "icons" directory. If it doesn't exists, a default icon will be
     *        generated automatically based on the name.
     * @param color The color of the text in the automatically generated icons.
     *        one of COLOR_DEFAULT, COLOR_RED, COLOR_BLUE or COLOR_RED.
     * @param shape The shape of the icon in the automatically generated icons,
     *        one of SHAPE_DEFAULT, SHAPE_CIRCLE or SHAPE_RECT.
     */
    public ImageDescriptor getImageDescriptor(String osName, int color, int shape) {
        String key = Character.toString((char) shape) + Integer.toString(color) + osName;
        ImageDescriptor id = mImageDescMap.get(key);
        if (id == null && !mImageDescMap.containsKey(key)) {
            id = AdtPlugin.imageDescriptorFromPlugin(
                    AdtPlugin.PLUGIN_ID,
                    String.format("/icons/%1$s.png", osName)); //$NON-NLS-1$

            if (id == null) {
                id = new LetterImageDescriptor(osName.charAt(0), color, shape);
            }
            
            // Note that we store null references in the icon map, to avoid looking them
            // up every time. If it didn't exist once, it will not exist later.
            mImageDescMap.put(key, id);
        }
        return id;
    }

    /**
     * A simple image description that generates a 16x16 image which consists
     * of a colored letter inside a black & white circle.
     */
    private static class LetterImageDescriptor extends ImageDescriptor {

        private final char mLetter;
        private final int mColor;
        private final int mShape;

        public LetterImageDescriptor(char letter, int color, int shape) {
            mLetter = letter;
            mColor = color;
            mShape = shape;
        }
        
        @Override
        public ImageData getImageData() {
            
            final int SX = 15;
            final int SY = 15;
            final int RX = 4;
            final int RY = 4;
            
            Display display = Display.getCurrent();
            if (display == null) {
                return null;
            }

            Image image = new Image(display, SX, SY);
            
            image.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
            
            GC gc = new GC(image);
            gc.setAdvanced(true);
            gc.setAntialias(SWT.ON);
            gc.setTextAntialias(SWT.ON);

            gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
            if (mShape == SHAPE_CIRCLE) {
                gc.fillOval(0, 0, SX - 1, SY - 1);
            } else if (mShape == SHAPE_RECT) {
                gc.fillRoundRectangle(0, 0, SX - 1, SY - 1, RX, RY);
            }
            
            gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.setLineWidth(1);
            if (mShape == SHAPE_CIRCLE) {
                gc.drawOval(0, 0, SX - 1, SY - 1);
            } else if (mShape == SHAPE_RECT) {
                gc.drawRoundRectangle(0, 0, SX - 1, SY - 1, RX, RY);
            }

            // Get a bold version of the default system font, if possible.
            Font font = display.getSystemFont();
            FontData[] fds = font.getFontData();
            fds[0].setStyle(SWT.BOLD);
            // use 3/4th of the circle diameter for the font size (in pixels)
            // and convert it to "font points" (font points in SWT are hardcoded in an
            // arbitrary 72 dpi and then converted in real pixels using whatever is
            // indicated by getDPI -- at least that's how it works under Win32).
            fds[0].setHeight((int) ((SY + 1) * 3./4. * 72./display.getDPI().y));
            // Note: win32 implementation always uses fds[0] so we change just that one.
            // getFontData indicates that the array of fd is really an unusual thing for X11.
            font = new Font(display, fds);
            gc.setFont(font);
            gc.setForeground(display.getSystemColor(mColor));

            // Text measurement varies so slightly depending on the platform
            int ofx = 0;
            int ofy = 0;
            if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) {
                ofx = +1;
                ofy = -1;
            }
            
            String s = Character.toString(mLetter).toUpperCase();
            Point p = gc.textExtent(s);
            int tx = (SX + ofx - p.x) / 2;
            int ty = (SY + ofy - p.y) / 2;
            gc.drawText(s, tx, ty, true /* isTransparent */);

            font.dispose();
            gc.dispose();
            
            ImageData data = image.getImageData();
            image.dispose();
            return data;
        }
        
    }
    
}
