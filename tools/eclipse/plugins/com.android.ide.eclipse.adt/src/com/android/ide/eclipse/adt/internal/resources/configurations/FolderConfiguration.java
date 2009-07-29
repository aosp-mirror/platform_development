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

package com.android.ide.eclipse.adt.internal.resources.configurations;

import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;


/**
 * Represents the configuration for Resource Folders. All the properties have a default
 * value which means that the property is not set.
 */
public final class FolderConfiguration implements Comparable<FolderConfiguration> {
    public final static String QUALIFIER_SEP = "-"; //$NON-NLS-1$

    private final ResourceQualifier[] mQualifiers = new ResourceQualifier[INDEX_COUNT];
    
    private final static int INDEX_COUNTRY_CODE = 0;
    private final static int INDEX_NETWORK_CODE = 1;
    private final static int INDEX_LANGUAGE = 2;
    private final static int INDEX_REGION = 3;
    private final static int INDEX_SCREEN_ORIENTATION = 4;
    private final static int INDEX_PIXEL_DENSITY = 5;
    private final static int INDEX_TOUCH_TYPE = 6;
    private final static int INDEX_KEYBOARD_STATE = 7;
    private final static int INDEX_TEXT_INPUT_METHOD = 8;
    private final static int INDEX_NAVIGATION_METHOD = 9;
    private final static int INDEX_SCREEN_DIMENSION = 10;
    private final static int INDEX_COUNT = 11;
    
    /**
     * Sets the config from the qualifiers of a given <var>config</var>.
     * @param config
     */
    public void set(FolderConfiguration config) {
        for (int i = 0 ; i < INDEX_COUNT ; i++) {
            mQualifiers[i] = config.mQualifiers[i];
        }
    }

    /**
     * Removes the qualifiers from the receiver if they are present (and valid)
     * in the given configuration.
     */
    public void substract(FolderConfiguration config) {
        for (int i = 0 ; i < INDEX_COUNT ; i++) {
            if (config.mQualifiers[i] != null && config.mQualifiers[i].isValid()) {
                mQualifiers[i] = null;
            }
        }
    }
    
    /**
     * Returns the first invalid qualifier, or <code>null<code> if they are all valid (or if none
     * exists).
     */
    public ResourceQualifier getInvalidQualifier() {
        for (int i = 0 ; i < INDEX_COUNT ; i++) {
            if (mQualifiers[i] != null && mQualifiers[i].isValid() == false) {
                return mQualifiers[i];
            }
        }
        
        // all allocated qualifiers are valid, we return null.
        return null;
    }
    
    /**
     * Returns whether the Region qualifier is valid. Region qualifier can only be present if a
     * Language qualifier is present as well.
     * @return true if the Region qualifier is valid.
     */
    public boolean checkRegion() {
        if (mQualifiers[INDEX_LANGUAGE] == null && mQualifiers[INDEX_REGION] != null) {
            return false;
        }

        return true;
    }
    
    /**
     * Adds a qualifier to the {@link FolderConfiguration}
     * @param qualifier the {@link ResourceQualifier} to add.
     */
    public void addQualifier(ResourceQualifier qualifier) {
        if (qualifier instanceof CountryCodeQualifier) {
            mQualifiers[INDEX_COUNTRY_CODE] = qualifier;
        } else if (qualifier instanceof NetworkCodeQualifier) {
            mQualifiers[INDEX_NETWORK_CODE] = qualifier;
        } else if (qualifier instanceof LanguageQualifier) {
            mQualifiers[INDEX_LANGUAGE] = qualifier;
        } else if (qualifier instanceof RegionQualifier) {
            mQualifiers[INDEX_REGION] = qualifier;
        } else if (qualifier instanceof ScreenOrientationQualifier) {
            mQualifiers[INDEX_SCREEN_ORIENTATION] = qualifier;
        } else if (qualifier instanceof PixelDensityQualifier) {
            mQualifiers[INDEX_PIXEL_DENSITY] = qualifier;
        } else if (qualifier instanceof TouchScreenQualifier) {
            mQualifiers[INDEX_TOUCH_TYPE] = qualifier;
        } else if (qualifier instanceof KeyboardStateQualifier) {
            mQualifiers[INDEX_KEYBOARD_STATE] = qualifier;
        } else if (qualifier instanceof TextInputMethodQualifier) {
            mQualifiers[INDEX_TEXT_INPUT_METHOD] = qualifier;
        } else if (qualifier instanceof NavigationMethodQualifier) {
            mQualifiers[INDEX_NAVIGATION_METHOD] = qualifier;
        } else if (qualifier instanceof ScreenDimensionQualifier) {
            mQualifiers[INDEX_SCREEN_DIMENSION] = qualifier;
        }
    }
    
    /**
     * Removes a given qualifier from the {@link FolderConfiguration}.
     * @param qualifier the {@link ResourceQualifier} to remove.
     */
    public void removeQualifier(ResourceQualifier qualifier) {
        for (int i = 0 ; i < INDEX_COUNT ; i++) {
            if (mQualifiers[i] == qualifier) {
                mQualifiers[i] = null;
                return;
            }
        }
    }
    
    public void setCountryCodeQualifier(CountryCodeQualifier qualifier) {
        mQualifiers[INDEX_COUNTRY_CODE] = qualifier;
    }

    public CountryCodeQualifier getCountryCodeQualifier() {
        return (CountryCodeQualifier)mQualifiers[INDEX_COUNTRY_CODE];
    }

    public void setNetworkCodeQualifier(NetworkCodeQualifier qualifier) {
        mQualifiers[INDEX_NETWORK_CODE] = qualifier;
    }

    public NetworkCodeQualifier getNetworkCodeQualifier() {
        return (NetworkCodeQualifier)mQualifiers[INDEX_NETWORK_CODE];
    }

    public void setLanguageQualifier(LanguageQualifier qualifier) {
        mQualifiers[INDEX_LANGUAGE] = qualifier;
    }

    public LanguageQualifier getLanguageQualifier() {
        return (LanguageQualifier)mQualifiers[INDEX_LANGUAGE];
    }

    public void setRegionQualifier(RegionQualifier qualifier) {
        mQualifiers[INDEX_REGION] = qualifier;
    }

    public RegionQualifier getRegionQualifier() {
        return (RegionQualifier)mQualifiers[INDEX_REGION];
    }

    public void setScreenOrientationQualifier(ScreenOrientationQualifier qualifier) {
        mQualifiers[INDEX_SCREEN_ORIENTATION] = qualifier;
    }

    public ScreenOrientationQualifier getScreenOrientationQualifier() {
        return (ScreenOrientationQualifier)mQualifiers[INDEX_SCREEN_ORIENTATION];
    }

    public void setPixelDensityQualifier(PixelDensityQualifier qualifier) {
        mQualifiers[INDEX_PIXEL_DENSITY] = qualifier;
    }

    public PixelDensityQualifier getPixelDensityQualifier() {
        return (PixelDensityQualifier)mQualifiers[INDEX_PIXEL_DENSITY];
    }

    public void setTouchTypeQualifier(TouchScreenQualifier qualifier) {
        mQualifiers[INDEX_TOUCH_TYPE] = qualifier;
    }

    public TouchScreenQualifier getTouchTypeQualifier() {
        return (TouchScreenQualifier)mQualifiers[INDEX_TOUCH_TYPE];
    }

    public void setKeyboardStateQualifier(KeyboardStateQualifier qualifier) {
        mQualifiers[INDEX_KEYBOARD_STATE] = qualifier;
    }

    public KeyboardStateQualifier getKeyboardStateQualifier() {
        return (KeyboardStateQualifier)mQualifiers[INDEX_KEYBOARD_STATE];
    }

    public void setTextInputMethodQualifier(TextInputMethodQualifier qualifier) {
        mQualifiers[INDEX_TEXT_INPUT_METHOD] = qualifier;
    }

    public TextInputMethodQualifier getTextInputMethodQualifier() {
        return (TextInputMethodQualifier)mQualifiers[INDEX_TEXT_INPUT_METHOD];
    }
    
    public void setNavigationMethodQualifier(NavigationMethodQualifier qualifier) {
        mQualifiers[INDEX_NAVIGATION_METHOD] = qualifier;
    }

    public NavigationMethodQualifier getNavigationMethodQualifier() {
        return (NavigationMethodQualifier)mQualifiers[INDEX_NAVIGATION_METHOD];
    }
    
    public void setScreenDimensionQualifier(ScreenDimensionQualifier qualifier) {
        mQualifiers[INDEX_SCREEN_DIMENSION] = qualifier;
    }

    public ScreenDimensionQualifier getScreenDimensionQualifier() {
        return (ScreenDimensionQualifier)mQualifiers[INDEX_SCREEN_DIMENSION];
    }

    /**
     * Returns whether an object is equals to the receiver.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        if (obj instanceof FolderConfiguration) {
            FolderConfiguration fc = (FolderConfiguration)obj;
            for (int i = 0 ; i < INDEX_COUNT ; i++) {
                ResourceQualifier qualifier = mQualifiers[i];
                ResourceQualifier fcQualifier = fc.mQualifiers[i];
                if (qualifier != null) {
                    if (qualifier.equals(fcQualifier) == false) {
                        return false;
                    }
                } else if (fcQualifier != null) {
                    return false;
                }
            }

            return true;
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
    
    /**
     * Returns whether the Configuration has only default values.
     */
    public boolean isDefault() {
        for (ResourceQualifier irq : mQualifiers) {
            if (irq != null) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Returns the name of a folder with the configuration.
     */
    public String getFolderName(ResourceFolderType folder) {
        StringBuilder result = new StringBuilder(folder.getName());
        
        for (ResourceQualifier qualifier : mQualifiers) {
            if (qualifier != null) {
                result.append(QUALIFIER_SEP);
                result.append(qualifier.toString());
            }
        }
        
        return result.toString();
    }
    
    /**
     * Returns a string valid for usage in a folder name, or <code>null</code> if the configuration
     * is default.
     */
    @Override
    public String toString() {
        StringBuilder result = null;
        
        for (ResourceQualifier irq : mQualifiers) {
            if (irq != null) {
                if (result == null) {
                    result = new StringBuilder();
                } else {
                    result.append(QUALIFIER_SEP);
                }
                result.append(irq.toString());
            }
        }
        
        if (result != null) {
            return result.toString();
        } else {
            return null;
        }
    }
    
    /**
     * Returns a string valid for display purpose.
     */
    public String toDisplayString() {
        if (isDefault()) {
            return "default";
        }

        StringBuilder result = null;
        int index = 0;
        ResourceQualifier qualifier = null;
        
        // pre- language/region qualifiers
        while (index < INDEX_LANGUAGE) {
            qualifier = mQualifiers[index++];
            if (qualifier != null) {
                if (result == null) {
                    result = new StringBuilder();
                } else {
                    result.append(", "); //$NON-NLS-1$
                }
                result.append(qualifier.getStringValue());
                
            }
        }
        
        // process the language/region qualifier in a custom way, if there are both non null.
        if (mQualifiers[INDEX_LANGUAGE] != null && mQualifiers[INDEX_REGION] != null) {
            String language = mQualifiers[INDEX_LANGUAGE].getStringValue();
            String region = mQualifiers[INDEX_REGION].getStringValue();

            if (result == null) {
                result = new StringBuilder();
            } else {
                result.append(", "); //$NON-NLS-1$
            }
            result.append(String.format("%s_%s", language, region)); //$NON-NLS-1$
            
            index += 2;
        }
        
        // post language/region qualifiers.
        while (index < INDEX_COUNT) {
            qualifier = mQualifiers[index++];
            if (qualifier != null) {
                if (result == null) {
                    result = new StringBuilder();
                } else {
                    result.append(", "); //$NON-NLS-1$
                }
                result.append(qualifier.getStringValue());
                
            }
        }

        return result == null ? null : result.toString();
    }

    public int compareTo(FolderConfiguration folderConfig) {
        // default are always at the top.
        if (isDefault()) {
            if (folderConfig.isDefault()) {
                return 0;
            }
            return -1;
        }
        
        // now we compare the qualifiers
        for (int i = 0 ; i < INDEX_COUNT; i++) {
            ResourceQualifier qualifier1 = mQualifiers[i];
            ResourceQualifier qualifier2 = folderConfig.mQualifiers[i];
            
            if (qualifier1 == null) {
                if (qualifier2 == null) {
                    continue;
                } else {
                    return -1;
                }
            } else {
                if (qualifier2 == null) {
                    return 1;
                } else {
                    int result = qualifier1.compareTo(qualifier2);
                    
                    if (result == 0) {
                        continue;
                    }
                    
                    return result;
                }
            }
        }
        
        // if we arrive here, all the qualifier matches
        return 0;
    }

    /**
     * Returns whether the configuration match the given reference config.
     * <p/>A match means that:
     * <ul>
     * <li>This config does not use any qualifier not used by the reference config</li>
     * <li>The qualifier used by this config have the same values as the qualifiers of
     * the reference config.</li>
     * </ul>
     * @param referenceConfig The reference configuration to test against.
     * @return the number of matching qualifiers or -1 if the configurations are not compatible.
     */
    public int match(FolderConfiguration referenceConfig) {
        int matchCount = 0;
        
        for (int i = 0 ; i < INDEX_COUNT ; i++) {
            ResourceQualifier testQualifier = mQualifiers[i];
            ResourceQualifier referenceQualifier = referenceConfig.mQualifiers[i];
            
            // we only care if testQualifier is non null. If it's null, it's a match but
            // without increasing the matchCount.
            if (testQualifier != null) {
                if (referenceQualifier == null) {
                    return -1;
                } else if (testQualifier.equals(referenceQualifier) == false) {
                    return -1;
                }
                
                // the qualifier match, increment the count
                matchCount++;
            }
        }
        return matchCount;
    }

    /**
     * Returns the index of the first non null {@link ResourceQualifier} starting at index
     * <var>startIndex</var>
     * @param startIndex
     * @return -1 if no qualifier was found.
     */
    public int getHighestPriorityQualifier(int startIndex) {
        for (int i = startIndex ; i < INDEX_COUNT ; i++) {
            if (mQualifiers[i] != null) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Create default qualifiers.
     */
    public void createDefault() {
        mQualifiers[INDEX_COUNTRY_CODE] = new CountryCodeQualifier();
        mQualifiers[INDEX_NETWORK_CODE] = new NetworkCodeQualifier();
        mQualifiers[INDEX_LANGUAGE] = new LanguageQualifier();
        mQualifiers[INDEX_REGION] = new RegionQualifier();
        mQualifiers[INDEX_SCREEN_ORIENTATION] = new ScreenOrientationQualifier();
        mQualifiers[INDEX_PIXEL_DENSITY] = new PixelDensityQualifier();
        mQualifiers[INDEX_TOUCH_TYPE] = new TouchScreenQualifier();
        mQualifiers[INDEX_KEYBOARD_STATE] = new KeyboardStateQualifier();
        mQualifiers[INDEX_TEXT_INPUT_METHOD] = new TextInputMethodQualifier();
        mQualifiers[INDEX_NAVIGATION_METHOD] = new NavigationMethodQualifier();
        mQualifiers[INDEX_SCREEN_DIMENSION] = new ScreenDimensionQualifier();
    }

    /**
     * Returns an array of all the non null qualifiers.
     */
    public ResourceQualifier[] getQualifiers() {
        int count = 0;
        for (int i = 0 ; i < INDEX_COUNT ; i++) {
            if (mQualifiers[i] != null) {
                count++;
            }
        }
        
        ResourceQualifier[] array = new ResourceQualifier[count];
        int index = 0;
        for (int i = 0 ; i < INDEX_COUNT ; i++) {
            if (mQualifiers[i] != null) {
                array[index++] = mQualifiers[i];
            }
        }
        
        return array;
    }
}
