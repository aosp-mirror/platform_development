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

package com.android.ddmuilib.logcat;

import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.annotation.UiThread;
import com.android.ddmuilib.logcat.LogPanel.LogMessage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

/** logcat output filter class */
public class LogFilter {

    public final static int MODE_PID = 0x01;
    public final static int MODE_TAG = 0x02;
    public final static int MODE_LEVEL = 0x04;

    private String mName;

    /**
     * Filtering mode. Value can be a mix of MODE_PID, MODE_TAG, MODE_LEVEL
     */
    private int mMode = 0;

    /**
     * pid used for filtering. Only valid if mMode is MODE_PID.
     */
    private int mPid;

    /** Single level log level as defined in Log.mLevelChar. Only valid
     * if mMode is MODE_LEVEL */
    private int mLogLevel;

    /**
     * log tag filtering. Only valid if mMode is MODE_TAG
     */
    private String mTag;

    private Table mTable;
    private TabItem mTabItem;
    private boolean mIsCurrentTabItem = false;
    private int mUnreadCount = 0;

    /** Temp keyword filtering */
    private String[] mTempKeywordFilters;

    /** temp pid filtering */
    private int mTempPid = -1;

    /** temp tag filtering */
    private String mTempTag;

    /** temp log level filtering */
    private int mTempLogLevel = -1;

    private LogColors mColors;

    private boolean mTempFilteringStatus = false;
    
    private final ArrayList<LogMessage> mMessages = new ArrayList<LogMessage>();
    private final ArrayList<LogMessage> mNewMessages = new ArrayList<LogMessage>();

    private boolean mSupportsDelete = true;
    private boolean mSupportsEdit = true;
    private int mRemovedMessageCount = 0;

    /**
     * Creates a filter with a particular mode.
     * @param name The name to be displayed in the UI
     */
    public LogFilter(String name) {
        mName = name;
    }

    public LogFilter() {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(mName);

        sb.append(':');
        sb.append(mMode);
        if ((mMode & MODE_PID) == MODE_PID) {
            sb.append(':');
            sb.append(mPid);
        }

        if ((mMode & MODE_LEVEL) == MODE_LEVEL) {
            sb.append(':');
            sb.append(mLogLevel);
        }

        if ((mMode & MODE_TAG) == MODE_TAG) {
            sb.append(':');
            sb.append(mTag);
        }

        return sb.toString();
    }

    public boolean loadFromString(String string) {
        String[] segments = string.split(":"); // $NON-NLS-1$
        int index = 0;

        // get the name
        mName = segments[index++];

        // get the mode
        mMode = Integer.parseInt(segments[index++]);

        if ((mMode & MODE_PID) == MODE_PID) {
            mPid = Integer.parseInt(segments[index++]);
        }

        if ((mMode & MODE_LEVEL) == MODE_LEVEL) {
            mLogLevel = Integer.parseInt(segments[index++]);
        }

        if ((mMode & MODE_TAG) == MODE_TAG) {
            mTag = segments[index++];
        }

        return true;
    }


    /** Sets the name of the filter. */
    void setName(String name) {
        mName = name;
    }

    /**
     * Returns the UI display name.
     */
    public String getName() {
        return mName;
    }

    /**
     * Set the Table ui widget associated with this filter.
     * @param tabItem The item in the TabFolder
     * @param table The Table object
     */
    public void setWidgets(TabItem tabItem, Table table) {
        mTable = table;
        mTabItem = tabItem;
    }

    /**
     * Returns true if the filter is ready for ui.
     */
    public boolean uiReady() {
        return (mTable != null && mTabItem != null);
    }

    /**
     * Returns the UI table object.
     * @return
     */
    public Table getTable() {
        return mTable;
    }

    public void dispose() {
        mTable.dispose();
        mTabItem.dispose();
        mTable = null;
        mTabItem = null;
    }

    /**
     * Resets the filtering mode to be 0 (i.e. no filter).
     */
    public void resetFilteringMode() {
        mMode = 0;
    }

    /**
     * Returns the current filtering mode.
     * @return A bitmask. Possible values are MODE_PID, MODE_TAG, MODE_LEVEL
     */
    public int getFilteringMode() {
        return mMode;
    }

    /**
     * Adds PID to the current filtering mode.
     * @param pid
     */
    public void setPidMode(int pid) {
        if (pid != -1) {
            mMode |= MODE_PID;
        } else {
            mMode &= ~MODE_PID;
        }
        mPid = pid;
    }

    /** Returns the pid filter if valid, otherwise -1 */
    public int getPidFilter() {
        if ((mMode & MODE_PID) == MODE_PID)
            return mPid;
        return -1;
    }

    public void setTagMode(String tag) {
        if (tag != null && tag.length() > 0) {
            mMode |= MODE_TAG;
        } else {
            mMode &= ~MODE_TAG;
        }
        mTag = tag;
    }

    public String getTagFilter() {
        if ((mMode & MODE_TAG) == MODE_TAG)
            return mTag;
        return null;
    }

    public void setLogLevel(int level) {
        if (level == -1) {
            mMode &= ~MODE_LEVEL;
        } else {
            mMode |= MODE_LEVEL;
            mLogLevel = level;
        }

    }

    public int getLogLevel() {
        if ((mMode & MODE_LEVEL) == MODE_LEVEL) {
            return mLogLevel;
        }

        return -1;
    }


    public boolean supportsDelete() {
        return mSupportsDelete ;
    }

    public boolean supportsEdit() {
        return mSupportsEdit;
    }

    /**
     * Sets the selected state of the filter.
     * @param selected selection state.
     */
    public void setSelectedState(boolean selected) {
        if (selected) {
            if (mTabItem != null) {
                mTabItem.setText(mName);
            }
            mUnreadCount = 0;
        }
        mIsCurrentTabItem = selected;
    }
    
    /**
     * Adds a new message and optionally removes an old message.
     * <p/>The new message is filtered through {@link #accept(LogMessage)}.
     * Calls to {@link #flush()} from a UI thread will display it (and other
     * pending messages) to the associated {@link Table}.
     * @param logMessage the MessageData object to filter
     * @return true if the message was accepted.
     */
    public boolean addMessage(LogMessage newMessage, LogMessage oldMessage) {
        synchronized (mMessages) {
            if (oldMessage != null) {
                int index = mMessages.indexOf(oldMessage);
                if (index != -1) {
                    // TODO check that index will always be -1 or 0, as only the oldest message is ever removed.
                    mMessages.remove(index);
                    mRemovedMessageCount++;
                }
                
                // now we look for it in mNewMessages. This can happen if the new message is added
                // and then removed because too many messages are added between calls to #flush()
                index = mNewMessages.indexOf(oldMessage);
                if (index != -1) {
                    // TODO check that index will always be -1 or 0, as only the oldest message is ever removed.
                    mNewMessages.remove(index);
                }
            }

            boolean filter = accept(newMessage);

            if (filter) {
                // at this point the message is accepted, we add it to the list
                mMessages.add(newMessage);
                mNewMessages.add(newMessage);
            }

            return filter;
        }
    }
    
    /**
     * Removes all the items in the filter and its {@link Table}.
     */
    public void clear() {
        mRemovedMessageCount = 0;
        mNewMessages.clear();
        mMessages.clear();
        mTable.removeAll();
    }
    
    /**
     * Filters a message.
     * @param logMessage the Message
     * @return true if the message is accepted by the filter.
     */
    boolean accept(LogMessage logMessage) {
        // do the regular filtering now
        if ((mMode & MODE_PID) == MODE_PID && mPid != logMessage.data.pid) {
            return false;
        }

        if ((mMode & MODE_TAG) == MODE_TAG && (
                logMessage.data.tag == null ||
                logMessage.data.tag.equals(mTag) == false)) {
            return false;
        }

        int msgLogLevel = logMessage.data.logLevel.getPriority();

        // test the temp log filtering first, as it replaces the old one
        if (mTempLogLevel != -1) {
            if (mTempLogLevel > msgLogLevel) {
                return false;
            }
        } else if ((mMode & MODE_LEVEL) == MODE_LEVEL &&
                mLogLevel > msgLogLevel) {
            return false;
        }

        // do the temp filtering now.
        if (mTempKeywordFilters != null) {
            String msg = logMessage.msg;

            for (String kw : mTempKeywordFilters) {
                try {
                    if (msg.contains(kw) == false && msg.matches(kw) == false) {
                        return false;
                    }
                } catch (PatternSyntaxException e) {
                    // if the string is not a valid regular expression,
                    // this exception is thrown.
                    return false;
                }
            }
        }

        if (mTempPid != -1 && mTempPid != logMessage.data.pid) {
           return false;
        }

        if (mTempTag != null && mTempTag.length() > 0) {
            if (mTempTag.equals(logMessage.data.tag) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * Takes all the accepted messages and display them.
     * This must be called from a UI thread.
     */
    @UiThread
    public void flush() {
        // if scroll bar is at the bottom, we will scroll
        ScrollBar bar = mTable.getVerticalBar();
        boolean scroll = bar.getMaximum() == bar.getSelection() + bar.getThumb();
        
        // if we are not going to scroll, get the current first item being shown.
        int topIndex = mTable.getTopIndex();

        // disable drawing
        mTable.setRedraw(false);
        
        int totalCount = mNewMessages.size();

        try {
            // remove the items of the old messages.
            for (int i = 0 ; i < mRemovedMessageCount && mTable.getItemCount() > 0 ; i++) {
                mTable.remove(0);
            }
    
            if (mUnreadCount > mTable.getItemCount()) {
                mUnreadCount = mTable.getItemCount();
            }
    
            // add the new items
            for (int i = 0  ; i < totalCount ; i++) {
                LogMessage msg = mNewMessages.get(i);
                addTableItem(msg);
            }
        } catch (SWTException e) {
            // log the error and keep going. Content of the logcat table maybe unexpected
            // but at least ddms won't crash.
            Log.e("LogFilter", e);
        }
        
        // redraw
        mTable.setRedraw(true);

        // scroll if needed, by showing the last item
        if (scroll) {
            totalCount = mTable.getItemCount();
            if (totalCount > 0) {
                mTable.showItem(mTable.getItem(totalCount-1));
            }
        } else if (mRemovedMessageCount > 0) {
            // we need to make sure the topIndex is still visible.
            // Because really old items are removed from the list, this could make it disappear
            // if we don't change the scroll value at all.

            topIndex -= mRemovedMessageCount;
            if (topIndex < 0) {
                // looks like it disappeared. Lets just show the first item
                mTable.showItem(mTable.getItem(0));
            } else {
                mTable.showItem(mTable.getItem(topIndex));
            }
        }

        // if this filter is not the current one, we update the tab text
        // with the amount of unread message
        if (mIsCurrentTabItem == false) {
            mUnreadCount += mNewMessages.size();
            totalCount = mTable.getItemCount();
            if (mUnreadCount > 0) {
                mTabItem.setText(mName + " (" // $NON-NLS-1$
                        + (mUnreadCount > totalCount ? totalCount : mUnreadCount)
                        + ")");  // $NON-NLS-1$
            } else {
                mTabItem.setText(mName);  // $NON-NLS-1$
            }
        }
        
        mNewMessages.clear();
    }

    void setColors(LogColors colors) {
        mColors = colors;
    }

    int getUnreadCount() {
        return mUnreadCount;
    }

    void setUnreadCount(int unreadCount) {
        mUnreadCount = unreadCount;
    }

    void setSupportsDelete(boolean support) {
        mSupportsDelete = support;
    }

    void setSupportsEdit(boolean support) {
        mSupportsEdit = support;
    }

    void setTempKeywordFiltering(String[] segments) {
        mTempKeywordFilters = segments;
        mTempFilteringStatus = true;
    }

    void setTempPidFiltering(int pid) {
        mTempPid = pid;
        mTempFilteringStatus = true;
    }

    void setTempTagFiltering(String tag) {
        mTempTag = tag;
        mTempFilteringStatus = true;
    }

    void resetTempFiltering() {
        if (mTempPid != -1 || mTempTag != null || mTempKeywordFilters != null) {
            mTempFilteringStatus = true;
        }

        mTempPid = -1;
        mTempTag = null;
        mTempKeywordFilters = null;
    }

    void resetTempFilteringStatus() {
        mTempFilteringStatus = false;
    }

    boolean getTempFilterStatus() {
        return mTempFilteringStatus;
    }


    /**
     * Add a TableItem for the index-th item of the buffer
     * @param filter The index of the table in which to insert the item.
     */
    private void addTableItem(LogMessage msg) {
        TableItem item = new TableItem(mTable, SWT.NONE);
        item.setText(0, msg.data.time);
        item.setText(1, new String(new char[] { msg.data.logLevel.getPriorityLetter() }));
        item.setText(2, msg.data.pidString);
        item.setText(3, msg.data.tag);
        item.setText(4, msg.msg);

        // add the buffer index as data
        item.setData(msg);

        if (msg.data.logLevel == LogLevel.INFO) {
            item.setForeground(mColors.infoColor);
        } else if (msg.data.logLevel == LogLevel.DEBUG) {
            item.setForeground(mColors.debugColor);
        } else if (msg.data.logLevel == LogLevel.ERROR) {
            item.setForeground(mColors.errorColor);
        } else if (msg.data.logLevel == LogLevel.WARN) {
            item.setForeground(mColors.warningColor);
        } else {
            item.setForeground(mColors.verboseColor);
        }
    }
}
