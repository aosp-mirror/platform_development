/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.internal.repository;

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.ISdkLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * A list of sdk-repository sources.
 */
public class RepoSources {

    private static final String KEY_COUNT = "count";

    private static final String KEY_SRC = "src";

    private static final String SRC_FILENAME = "repositories.cfg"; //$NON-NLS-1$

    private ArrayList<RepoSource> mSources = new ArrayList<RepoSource>();

    public RepoSources() {
    }

    /**
     * Adds a new source to the Sources list.
     */
    public void add(RepoSource source) {
        mSources.add(source);
    }

    /**
     * Removes a source from the Sources list.
     */
    public void remove(RepoSource source) {
        mSources.remove(source);
    }

    /**
     * Returns the sources list array. This is never null.
     */
    public RepoSource[] getSources() {
        return mSources.toArray(new RepoSource[mSources.size()]);
    }

    /**
     * Loads all user sources. This <em>replaces</em> all existing user sources
     * by the ones from the property file.
     */
    public void loadUserSources(ISdkLog log) {

        // Remove all existing user sources
        for (Iterator<RepoSource> it = mSources.iterator(); it.hasNext(); ) {
            RepoSource s = it.next();
            if (s.isUserSource()) {
                it.remove();
            }
        }

        // Load new user sources from property file
        FileInputStream fis = null;
        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SRC_FILENAME);
            if (f.exists()) {
                fis = new FileInputStream(f);

                Properties props = new Properties();
                props.load(fis);

                int count = Integer.parseInt(props.getProperty(KEY_COUNT, "0"));

                for (int i = 0; i < count; i++) {
                    String url = props.getProperty(String.format("%s%02d", KEY_SRC, i));  //$NON-NLS-1$
                    if (url != null) {
                        RepoSource s = new RepoSource(url, true /*userSource*/);
                        if (!hasSource(s)) {
                            mSources.add(s);
                        }
                    }
                }
            }

        } catch (NumberFormatException e) {
            log.error(e, null);

        } catch (AndroidLocationException e) {
            log.error(e, null);

        } catch (IOException e) {
            log.error(e, null);

        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Returns true if there's already a similar source in the sources list.
     * <p/>
     * The search is O(N), which should be acceptable on the expectedly small source list.
     */
    public boolean hasSource(RepoSource source) {
        for (RepoSource s : mSources) {
            if (s.equals(source)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Saves all the user sources.
     * @param log
     */
    public void saveUserSources(ISdkLog log) {
        FileOutputStream fos = null;
        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SRC_FILENAME);

            fos = new FileOutputStream(f);

            Properties props = new Properties();

            int count = 0;
            for (RepoSource s : mSources) {
                if (s.isUserSource()) {
                    count++;
                    props.setProperty(String.format("%s%02d", KEY_SRC, count), s.getUrl());  //$NON-NLS-1$
                }
            }
            props.setProperty(KEY_COUNT, Integer.toString(count));

            props.store( fos, "## User Sources for Android tool");  //$NON-NLS-1$

        } catch (AndroidLocationException e) {
            log.error(e, null);

        } catch (IOException e) {
            log.error(e, null);

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

    }
}
