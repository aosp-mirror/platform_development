/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.debug.ui;

import com.android.ide.eclipse.common.AndroidConstants;

import java.io.File;
import java.util.ArrayList;

/**
 * Repository for the emulator skins. This class is responsible for parsing the skin folder.
 */
public class SkinRepository {

    private final static SkinRepository sInstance = new SkinRepository();

    private Skin[] mSkins;

    public static class Skin {

        String mName;

        public Skin(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }

        /**
         * Returns the human readable description of the skin.
         */
        public String getDescription() {
            // TODO: parse the skin and output the description.
            return mName;
        }
    }

    /**
     * Returns the singleton instance.
     */
    public static SkinRepository getInstance() {
        return sInstance;
    }

    /**
     * Parse the skin folder and build the skin list.
     * @param osPath The path of the skin folder.
     */
    public void parseFolder(String osPath) {
        File skinFolder = new File(osPath);

        if (skinFolder.isDirectory()) {
            ArrayList<Skin> skinList = new ArrayList<Skin>();

            File[] files = skinFolder.listFiles();

            for (File skin : files) {
                if (skin.isDirectory()) {
                    // check for layout file
                    File layout = new File(skin.getPath() + File.separator
                            + AndroidConstants.FN_LAYOUT);

                    if (layout.isFile()) {
                        // for now we don't parse the content of the layout and
                        // simply add the directory to the list.
                        skinList.add(new Skin(skin.getName()));
                    }
                }
            }

            mSkins = skinList.toArray(new Skin[skinList.size()]);
        } else {
            mSkins = new Skin[0];
        }
    }

    public Skin[] getSkins() {
        return mSkins;
    }

    /**
     * Returns a valid skin folder name. If <code>skin</code> is valid, then it is returned,
     * otherwise the first valid skin name is returned.
     * @param skin the Skin name to check
     * @return a valid skin name or null if there aren't any.
     */
    public String checkSkin(String skin) {
        if (mSkins != null) {
            for (Skin s : mSkins) {
                if (s.getName().equals(skin)) {
                    return skin;
                }
            }

            if (mSkins.length > 0) {
                return mSkins[0].getName();
            }
        }

        return null;
    }


    /**
     * Returns the name of a skin by index.
     * @param index The index of the skin to return
     * @return the skin name of null if the index is invalid.
     */
    public String getSkinNameByIndex(int index) {
        if (mSkins != null) {
            if (index >= 0 && index < mSkins.length) {
                return mSkins[index].getName();
            }
        }
        return null;
    }

    /**
     * Returns the index (0 based) of the skin matching the name.
     * @param name The name of the skin to look for.
     * @return the index of the skin or -1 if the skin was not found.
     */
    public int getSkinIndex(String name) {
        if (mSkins != null) {
            int count = mSkins.length;
            for (int i = 0 ; i < count ; i++) {
                Skin s = mSkins[i];
                if (s.mName.equals(name)) {
                    return i;
                }
            }
        }

        return -1;
    }
}
