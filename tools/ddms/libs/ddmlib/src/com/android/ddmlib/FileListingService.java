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

package com.android.ddmlib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides {@link Device} side file listing service.
 * <p/>To get an instance for a known {@link Device}, call {@link Device#getFileListingService()}.
 */
public final class FileListingService {

    /** Pattern to find filenames that match "*.apk" */
    private final static Pattern sApkPattern =
        Pattern.compile(".*\\.apk", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private final static String PM_FULL_LISTING = "pm list packages -f"; //$NON-NLS-1$

    /** Pattern to parse the output of the 'pm -lf' command.<br>
     * The output format looks like:<br>
     * /data/app/myapp.apk=com.mypackage.myapp */
    private final static Pattern sPmPattern = Pattern.compile("^package:(.+?)=(.+)$"); //$NON-NLS-1$

    /** Top level data folder. */
    public final static String DIRECTORY_DATA = "data"; //$NON-NLS-1$
    /** Top level sdcard folder. */
    public final static String DIRECTORY_SDCARD = "sdcard"; //$NON-NLS-1$
    /** Top level system folder. */
    public final static String DIRECTORY_SYSTEM = "system"; //$NON-NLS-1$
    /** Top level temp folder. */
    public final static String DIRECTORY_TEMP = "tmp"; //$NON-NLS-1$
    /** Application folder. */
    public final static String DIRECTORY_APP = "app"; //$NON-NLS-1$

    private final static String[] sRootLevelApprovedItems = {
        DIRECTORY_DATA,
        DIRECTORY_SDCARD,
        DIRECTORY_SYSTEM,
        DIRECTORY_TEMP
    };

    public static final long REFRESH_RATE = 5000L;
    /**
     * Refresh test has to be slightly lower for precision issue.
     */
    static final long REFRESH_TEST = (long)(REFRESH_RATE * .8);

    /** Entry type: File */
    public static final int TYPE_FILE = 0;
    /** Entry type: Directory */
    public static final int TYPE_DIRECTORY = 1;
    /** Entry type: Directory Link */
    public static final int TYPE_DIRECTORY_LINK = 2;
    /** Entry type: Block */
    public static final int TYPE_BLOCK = 3;
    /** Entry type: Character */
    public static final int TYPE_CHARACTER = 4;
    /** Entry type: Link */
    public static final int TYPE_LINK = 5;
    /** Entry type: Socket */
    public static final int TYPE_SOCKET = 6;
    /** Entry type: FIFO */
    public static final int TYPE_FIFO = 7;
    /** Entry type: Other */
    public static final int TYPE_OTHER = 8;

    /** Device side file separator. */
    public static final String FILE_SEPARATOR = "/"; //$NON-NLS-1$

    private static final String FILE_ROOT = "/"; //$NON-NLS-1$


    /**
     * Regexp pattern to parse the result from ls.
     */
    private static Pattern sLsPattern = Pattern.compile(
        "^([bcdlsp-][-r][-w][-xsS][-r][-w][-xsS][-r][-w][-xstST])\\s+(\\S+)\\s+(\\S+)\\s+([\\d\\s,]*)\\s+(\\d{4}-\\d\\d-\\d\\d)\\s+(\\d\\d:\\d\\d)\\s+(.*)$"); //$NON-NLS-1$

    private Device mDevice;
    private FileEntry mRoot;

    private ArrayList<Thread> mThreadList = new ArrayList<Thread>();

    /**
     * Represents an entry in a directory. This can be a file or a directory.
     */
    public final static class FileEntry {
        /** Pattern to escape filenames for shell command consumption. */
        private final static Pattern sEscapePattern = Pattern.compile(
                "([\\\\()*+?\"'#/\\s])"); //$NON-NLS-1$

        /**
         * Comparator object for FileEntry
         */
        private static Comparator<FileEntry> sEntryComparator = new Comparator<FileEntry>() {
            public int compare(FileEntry o1, FileEntry o2) {
                if (o1 instanceof FileEntry && o2 instanceof FileEntry) {
                    FileEntry fe1 = (FileEntry)o1;
                    FileEntry fe2 = (FileEntry)o2;
                    return fe1.name.compareTo(fe2.name);
                }
                return 0;
            }
        };

        FileEntry parent;
        String name;
        String info;
        String permissions;
        String size;
        String date;
        String time;
        String owner;
        String group;
        int type;
        boolean isAppPackage;

        boolean isRoot;

        /**
         * Indicates whether the entry content has been fetched yet, or not.
         */
        long fetchTime = 0;

        final ArrayList<FileEntry> mChildren = new ArrayList<FileEntry>();

        /**
         * Creates a new file entry.
         * @param parent parent entry or null if entry is root
         * @param name name of the entry.
         * @param type entry type. Can be one of the following: {@link FileListingService#TYPE_FILE},
         * {@link FileListingService#TYPE_DIRECTORY}, {@link FileListingService#TYPE_OTHER}.
         */
        private FileEntry(FileEntry parent, String name, int type, boolean isRoot) {
            this.parent = parent;
            this.name = name;
            this.type = type;
            this.isRoot = isRoot;

            checkAppPackageStatus();
        }

        /**
         * Returns the name of the entry
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the size string of the entry, as returned by <code>ls</code>.
         */
        public String getSize() {
            return size;
        }

        /**
         * Returns the size of the entry.
         */
        public int getSizeValue() {
            return Integer.parseInt(size);
        }

        /**
         * Returns the date string of the entry, as returned by <code>ls</code>.
         */
        public String getDate() {
            return date;
        }

        /**
         * Returns the time string of the entry, as returned by <code>ls</code>.
         */
        public String getTime() {
            return time;
        }

        /**
         * Returns the permission string of the entry, as returned by <code>ls</code>.
         */
        public String getPermissions() {
            return permissions;
        }

        /**
         * Returns the extra info for the entry.
         * <p/>For a link, it will be a description of the link.
         * <p/>For an application apk file it will be the application package as returned
         * by the Package Manager.  
         */
        public String getInfo() {
            return info;
        }

        /**
         * Return the full path of the entry.
         * @return a path string using {@link FileListingService#FILE_SEPARATOR} as separator.
         */
        public String getFullPath() {
            if (isRoot) {
                return FILE_ROOT;
            }
            StringBuilder pathBuilder = new StringBuilder();
            fillPathBuilder(pathBuilder, false);

            return pathBuilder.toString();
        }

        /**
         * Return the fully escaped path of the entry. This path is safe to use in a
         * shell command line.
         * @return a path string using {@link FileListingService#FILE_SEPARATOR} as separator
         */
        public String getFullEscapedPath() {
            StringBuilder pathBuilder = new StringBuilder();
            fillPathBuilder(pathBuilder, true);

            return pathBuilder.toString();
        }

        /**
         * Returns the path as a list of segments.
         */
        public String[] getPathSegments() {
            ArrayList<String> list = new ArrayList<String>();
            fillPathSegments(list);

            return list.toArray(new String[list.size()]);
        }

        /**
         * Returns true if the entry is a directory, false otherwise;
         */
        public int getType() {
            return type;
        }

        /**
         * Returns if the entry is a folder or a link to a folder.
         */
        public boolean isDirectory() {
            return type == TYPE_DIRECTORY || type == TYPE_DIRECTORY_LINK;
        }

        /**
         * Returns the parent entry.
         */
        public FileEntry getParent() {
            return parent;
        }

        /**
         * Returns the cached children of the entry. This returns the cache created from calling
         * <code>FileListingService.getChildren()</code>.
         */
        public FileEntry[] getCachedChildren() {
            return mChildren.toArray(new FileEntry[mChildren.size()]);
        }

        /**
         * Returns the child {@link FileEntry} matching the name.
         * This uses the cached children list.
         * @param name the name of the child to return.
         * @return the FileEntry matching the name or null.
         */
        public FileEntry findChild(String name) {
            for (FileEntry entry : mChildren) {
                if (entry.name.equals(name)) {
                    return entry;
                }
            }
            return null;
        }

        /**
         * Returns whether the entry is the root.
         */
        public boolean isRoot() {
            return isRoot;
        }

        void addChild(FileEntry child) {
            mChildren.add(child);
        }

        void setChildren(ArrayList<FileEntry> newChildren) {
            mChildren.clear();
            mChildren.addAll(newChildren);
        }

        boolean needFetch() {
            if (fetchTime == 0) {
                return true;
            }
            long current = System.currentTimeMillis();
            if (current-fetchTime > REFRESH_TEST) {
                return true;
            }

            return false;
        }

        /**
         * Returns if the entry is a valid application package.
         */
        public boolean isApplicationPackage() {
            return isAppPackage;
        }

        /**
         * Returns if the file name is an application package name.
         */
        public boolean isAppFileName() {
            Matcher m = sApkPattern.matcher(name);
            return m.matches();
        }

        /**
         * Recursively fills the pathBuilder with the full path
         * @param pathBuilder a StringBuilder used to create the path.
         * @param escapePath Whether the path need to be escaped for consumption by
         * a shell command line.
         */
        protected void fillPathBuilder(StringBuilder pathBuilder, boolean escapePath) {
            if (isRoot) {
                return;
            }

            if (parent != null) {
                parent.fillPathBuilder(pathBuilder, escapePath);
            }
            pathBuilder.append(FILE_SEPARATOR);
            pathBuilder.append(escapePath ? escape(name) : name);
        }

        /**
         * Recursively fills the segment list with the full path.
         * @param list The list of segments to fill.
         */
        protected void fillPathSegments(ArrayList<String> list) {
            if (isRoot) {
                return;
            }

            if (parent != null) {
                parent.fillPathSegments(list);
            }

            list.add(name);
        }

        /**
         * Sets the internal app package status flag. This checks whether the entry is in an app
         * directory like /data/app or /system/app
         */
        private void checkAppPackageStatus() {
            isAppPackage = false;

            String[] segments = getPathSegments();
            if (type == TYPE_FILE && segments.length == 3 && isAppFileName()) {
                isAppPackage = DIRECTORY_APP.equals(segments[1]) &&
                    (DIRECTORY_SYSTEM.equals(segments[0]) || DIRECTORY_DATA.equals(segments[0]));
            }
        }

        /**
         * Returns an escaped version of the entry name.
         * @param entryName
         */
        private String escape(String entryName) {
            return sEscapePattern.matcher(entryName).replaceAll("\\\\$1"); //$NON-NLS-1$
        }
    }

    private class LsReceiver extends MultiLineReceiver {

        private ArrayList<FileEntry> mEntryList;
        private ArrayList<String> mLinkList;
        private FileEntry[] mCurrentChildren;
        private FileEntry mParentEntry;

        /**
         * Create an ls receiver/parser.
         * @param currentChildren The list of current children. To prevent
         *      collapse during update, reusing the same FileEntry objects for
         *      files that were already there is paramount.
         * @param entryList the list of new children to be filled by the
         *      receiver.
         * @param linkList the list of link path to compute post ls, to figure
         *      out if the link pointed to a file or to a directory.
         */
        public LsReceiver(FileEntry parentEntry, ArrayList<FileEntry> entryList,
                ArrayList<String> linkList) {
            mParentEntry = parentEntry;
            mCurrentChildren = parentEntry.getCachedChildren();
            mEntryList = entryList;
            mLinkList = linkList;
        }

        @Override
        public void processNewLines(String[] lines) {
            for (String line : lines) {
                // no need to handle empty lines.
                if (line.length() == 0) {
                    continue;
                }

                // run the line through the regexp
                Matcher m = sLsPattern.matcher(line);
                if (m.matches() == false) {
                    continue;
                }

                // get the name
                String name = m.group(7);

                // if the parent is root, we only accept selected items
                if (mParentEntry.isRoot()) {
                    boolean found = false;
                    for (String approved : sRootLevelApprovedItems) {
                        if (approved.equals(name)) {
                            found = true;
                            break;
                        }
                    }

                    // if it's not in the approved list we skip this entry.
                    if (found == false) {
                        continue;
                    }
                }

                // get the rest of the groups
                String permissions = m.group(1);
                String owner = m.group(2);
                String group = m.group(3);
                String size = m.group(4);
                String date = m.group(5);
                String time = m.group(6);
                String info = null;

                // and the type
                int objectType = TYPE_OTHER;
                switch (permissions.charAt(0)) {
                    case '-' :
                        objectType = TYPE_FILE;
                        break;
                    case 'b' :
                        objectType = TYPE_BLOCK;
                        break;
                    case 'c' :
                        objectType = TYPE_CHARACTER;
                        break;
                    case 'd' :
                        objectType = TYPE_DIRECTORY;
                        break;
                    case 'l' :
                        objectType = TYPE_LINK;
                        break;
                    case 's' :
                        objectType = TYPE_SOCKET;
                        break;
                    case 'p' :
                        objectType = TYPE_FIFO;
                        break;
                }


                // now check what we may be linking to
                if (objectType == TYPE_LINK) {
                    String[] segments = name.split("\\s->\\s"); //$NON-NLS-1$

                    // we should have 2 segments
                    if (segments.length == 2) {
                        // update the entry name to not contain the link
                        name = segments[0];

                        // and the link name
                        info = segments[1];

                        // now get the path to the link
                        String[] pathSegments = info.split(FILE_SEPARATOR);
                        if (pathSegments.length == 1) {
                            // the link is to something in the same directory,
                            // unless the link is ..
                            if ("..".equals(pathSegments[0])) { //$NON-NLS-1$
                                // set the type and we're done.
                                objectType = TYPE_DIRECTORY_LINK;
                            } else {
                                // either we found the object already
                                // or we'll find it later.
                            }
                        }
                    }

                    // add an arrow in front to specify it's a link.
                    info = "-> " + info; //$NON-NLS-1$;
                }

                // get the entry, either from an existing one, or a new one
                FileEntry entry = getExistingEntry(name);
                if (entry == null) {
                    entry = new FileEntry(mParentEntry, name, objectType, false /* isRoot */);
                }

                // add some misc info
                entry.permissions = permissions;
                entry.size = size;
                entry.date = date;
                entry.time = time;
                entry.owner = owner;
                entry.group = group;
                if (objectType == TYPE_LINK) {
                    entry.info = info;
                }

                mEntryList.add(entry);
            }
        }

        /**
         * Queries for an already existing Entry per name
         * @param name the name of the entry
         * @return the existing FileEntry or null if no entry with a matching
         * name exists.
         */
        private FileEntry getExistingEntry(String name) {
            for (int i = 0 ; i < mCurrentChildren.length; i++) {
                FileEntry e = mCurrentChildren[i];

                // since we're going to "erase" the one we use, we need to
                // check that the item is not null.
                if (e != null) {
                    // compare per name, case-sensitive.
                    if (name.equals(e.name)) {
                        // erase from the list
                        mCurrentChildren[i] = null;

                        // and return the object
                        return e;
                    }
                }
            }

            // couldn't find any matching object, return null
            return null;
        }

        public boolean isCancelled() {
            return false;
        }

        public void finishLinks() {
            // TODO Handle links in the listing service
        }
    }

    /**
     * Classes which implement this interface provide a method that deals with asynchronous
     * result from <code>ls</code> command on the device.
     *
     * @see FileListingService#getChildren(com.android.ddmlib.FileListingService.FileEntry, boolean, com.android.ddmlib.FileListingService.IListingReceiver)
     */
    public interface IListingReceiver {
        public void setChildren(FileEntry entry, FileEntry[] children);

        public void refreshEntry(FileEntry entry);
    }

    /**
     * Creates a File Listing Service for a specified {@link Device}.
     * @param device The Device the service is connected to.
     */
    FileListingService(Device device) {
        mDevice = device;
    }

    /**
     * Returns the root element.
     * @return the {@link FileEntry} object representing the root element or
     * <code>null</code> if the device is invalid.
     */
    public FileEntry getRoot() {
        if (mDevice != null) {
            if (mRoot == null) {
                mRoot = new FileEntry(null /* parent */, "" /* name */, TYPE_DIRECTORY,
                        true /* isRoot */);
            }

            return mRoot;
        }

        return null;
    }

    /**
     * Returns the children of a {@link FileEntry}.
     * <p/>
     * This method supports a cache mechanism and synchronous and asynchronous modes.
     * <p/>
     * If <var>receiver</var> is <code>null</code>, the device side <code>ls</code>
     * command is done synchronously, and the method will return upon completion of the command.<br>
     * If <var>receiver</var> is non <code>null</code>, the command is launched is a separate
     * thread and upon completion, the receiver will be notified of the result.
     * <p/>
     * The result for each <code>ls</code> command is cached in the parent
     * <code>FileEntry</code>. <var>useCache</var> allows usage of this cache, but only if the
     * cache is valid. The cache is valid only for {@link FileListingService#REFRESH_RATE} ms.
     * After that a new <code>ls</code> command is always executed.
     * <p/>
     * If the cache is valid and <code>useCache == true</code>, the method will always simply
     * return the value of the cache, whether a {@link IListingReceiver} has been provided or not.
     *
     * @param entry The parent entry.
     * @param useCache A flag to use the cache or to force a new ls command.
     * @param receiver A receiver for asynchronous calls.
     * @return The list of children or <code>null</code> for asynchronous calls.
     *
     * @see FileEntry#getCachedChildren()
     */
    public FileEntry[] getChildren(final FileEntry entry, boolean useCache,
            final IListingReceiver receiver) {
        // first thing we do is check the cache, and if we already have a recent
        // enough children list, we just return that.
        if (useCache && entry.needFetch() == false) {
            return entry.getCachedChildren();
        }

        // if there's no receiver, then this is a synchronous call, and we
        // return the result of ls
        if (receiver == null) {
            doLs(entry);
            return entry.getCachedChildren();
        }

        // this is a asynchronous call.
        // we launch a thread that will do ls and give the listing
        // to the receiver
        Thread t = new Thread("ls " + entry.getFullPath()) { //$NON-NLS-1$
            @Override
            public void run() {
                doLs(entry);

                receiver.setChildren(entry, entry.getCachedChildren());

                final FileEntry[] children = entry.getCachedChildren();
                if (children.length > 0 && children[0].isApplicationPackage()) {
                    final HashMap<String, FileEntry> map = new HashMap<String, FileEntry>();

                    for (FileEntry child : children) {
                        String path = child.getFullPath();
                        map.put(path, child);
                    }

                    // call pm.
                    String command = PM_FULL_LISTING;
                    try {
                        mDevice.executeShellCommand(command, new MultiLineReceiver() {
                            @Override
                            public void processNewLines(String[] lines) {
                                for (String line : lines) {
                                    if (line.length() > 0) {
                                        // get the filepath and package from the line
                                        Matcher m = sPmPattern.matcher(line);
                                        if (m.matches()) {
                                            // get the children with that path
                                            FileEntry entry = map.get(m.group(1));
                                            if (entry != null) {
                                                entry.info = m.group(2);
                                                receiver.refreshEntry(entry);
                                            }
                                        }
                                    }
                                }
                            }
                            public boolean isCancelled() {
                                return false;
                            }
                        });
                    } catch (IOException e) {
                        // adb failed somehow, we do nothing.
                    }
                }


                // if another thread is pending, launch it
                synchronized (mThreadList) {
                    // first remove ourselves from the list
                    mThreadList.remove(this);

                    // then launch the next one if applicable.
                    if (mThreadList.size() > 0) {
                        Thread t = mThreadList.get(0);
                        t.start();
                    }
                }
            }
        };

        // we don't want to run multiple ls on the device at the same time, so we
        // store the thread in a list and launch it only if there's no other thread running.
        // the thread will launch the next one once it's done.
        synchronized (mThreadList) {
            // add to the list
            mThreadList.add(t);

            // if it's the only one, launch it.
            if (mThreadList.size() == 1) {
                t.start();
            }
        }

        // and we return null.
        return null;
    }

    private void doLs(FileEntry entry) {
        // create a list that will receive the list of the entries
        ArrayList<FileEntry> entryList = new ArrayList<FileEntry>();

        // create a list that will receive the link to compute post ls;
        ArrayList<String> linkList = new ArrayList<String>();

        try {
            // create the command
            String command = "ls -l " + entry.getFullPath(); //$NON-NLS-1$

            // create the receiver object that will parse the result from ls
            LsReceiver receiver = new LsReceiver(entry, entryList, linkList);

            // call ls.
            mDevice.executeShellCommand(command, receiver);

            // finish the process of the receiver to handle links
            receiver.finishLinks();
        } catch (IOException e) {
        }


        // at this point we need to refresh the viewer
        entry.fetchTime = System.currentTimeMillis();

        // sort the children and set them as the new children
        Collections.sort(entryList, FileEntry.sEntryComparator);
        entry.setChildren(entryList);
    }
}
