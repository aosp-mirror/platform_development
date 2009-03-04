/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

class ProfileProvider implements ITreeContentProvider {

    private MethodData[] mRoots;
    private SelectionAdapter mListener;
    private TreeViewer mTreeViewer;
    private TraceReader mReader;
    private Image mSortUp;
    private Image mSortDown;
    private String mColumnNames[] = { "Name", "Incl %", "Inclusive", "Excl %",
            "Exclusive", "Calls+Recur\nCalls/Total", "Time/Call" };
    private int mColumnWidths[] = { 370, 70, 70, 70, 70, 90, 70 };
    private int mColumnAlignments[] = { SWT.LEFT, SWT.RIGHT, SWT.RIGHT,
            SWT.RIGHT, SWT.RIGHT, SWT.CENTER, SWT.RIGHT };
    private static final int COL_NAME = 0;
    private static final int COL_INCLUSIVE_PER = 1;
    private static final int COL_INCLUSIVE = 2;
    private static final int COL_EXCLUSIVE_PER = 3;
    private static final int COL_EXCLUSIVE = 4;
    private static final int COL_CALLS = 5;
    private static final int COL_TIME_PER_CALL = 6;
    private long mTotalTime;
    private Pattern mUppercase;
    private int mPrevMatchIndex = -1;

    public ProfileProvider(TraceReader reader) {
        mRoots = reader.getMethods();
        mReader = reader;
        mTotalTime = reader.getEndTime();
        Display display = Display.getCurrent();
        InputStream in = getClass().getClassLoader().getResourceAsStream(
                "icons/sort_up.png");
        mSortUp = new Image(display, in);
        in = getClass().getClassLoader().getResourceAsStream(
                "icons/sort_down.png");
        mSortDown = new Image(display, in);
        mUppercase = Pattern.compile("[A-Z]");
    }

    private MethodData doMatchName(String name, int startIndex) {
        // Check if the given "name" has any uppercase letters
        boolean hasUpper = mUppercase.matcher(name).matches();
        for (int ii = startIndex; ii < mRoots.length; ++ii) {
            MethodData md = mRoots[ii];
            String fullName = md.getName();
            // If there were no upper case letters in the given name,
            // then ignore case when matching.
            if (!hasUpper)
                fullName = fullName.toLowerCase();
            if (fullName.indexOf(name) != -1) {
                mPrevMatchIndex = ii;
                return md;
            }
        }
        mPrevMatchIndex = -1;
        return null;
    }

    public MethodData findMatchingName(String name) {
        return doMatchName(name, 0);
    }

    public MethodData findNextMatchingName(String name) {
        return doMatchName(name, mPrevMatchIndex + 1);
    }

    public MethodData findMatchingTreeItem(TreeItem item) {
        if (item == null)
            return null;
        String text = item.getText();
        if (Character.isDigit(text.charAt(0)) == false)
            return null;
        int spaceIndex = text.indexOf(' ');
        String numstr = text.substring(0, spaceIndex);
        int rank = Integer.valueOf(numstr);
        for (MethodData md : mRoots) {
            if (md.getRank() == rank)
                return md;
        }
        return null;
    }

    public void setTreeViewer(TreeViewer treeViewer) {
        mTreeViewer = treeViewer;
    }

    public String[] getColumnNames() {
        return mColumnNames;
    }

    public int[] getColumnWidths() {
        return mColumnWidths;
    }

    public int[] getColumnAlignments() {
        return mColumnAlignments;
    }

    public Object[] getChildren(Object element) {
        if (element instanceof MethodData) {
            MethodData md = (MethodData) element;
            return md.getProfileNodes();
        }
        if (element instanceof ProfileNode) {
            ProfileNode pn = (ProfileNode) element;
            return pn.getChildren();
        }
        return new Object[0];
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        if (element instanceof MethodData)
            return true;
        if (element instanceof ProfileNode)
            return true;
        return false;
    }

    public Object[] getElements(Object element) {
        return mRoots;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
    }

    public Object getRoot() {
        return "root";
    }

    public SelectionAdapter getColumnListener() {
        if (mListener == null)
            mListener = new ColumnListener();
        return mListener;
    }

    public LabelProvider getLabelProvider() {
        return new ProfileLabelProvider();
    }

    class ProfileLabelProvider extends LabelProvider implements
            ITableLabelProvider, IColorProvider {
        Color colorRed;
        Color colorParentsBack;
        Color colorChildrenBack;
        TraceUnits traceUnits;

        public ProfileLabelProvider() {
            Display display = Display.getCurrent();
            colorRed = display.getSystemColor(SWT.COLOR_RED);
            colorParentsBack = new Color(display, 230, 230, 255); // blue
            colorChildrenBack = new Color(display, 255, 255, 210); // yellow
            traceUnits = mReader.getTraceUnits();
        }

        public String getColumnText(Object element, int col) {
            if (element instanceof MethodData) {
                MethodData md = (MethodData) element;
                if (col == COL_NAME)
                    return md.getProfileName();
                if (col == COL_EXCLUSIVE) {
                    double val = md.getElapsedExclusive();
                    val = traceUnits.getScaledValue(val);
                    return String.format("%.3f", val);
                }
                if (col == COL_EXCLUSIVE_PER) {
                    double val = md.getElapsedExclusive();
                    double per = val * 100.0 / mTotalTime;
                    return String.format("%.1f%%", per);
                }
                if (col == COL_INCLUSIVE) {
                    double val = md.getElapsedInclusive();
                    val = traceUnits.getScaledValue(val);
                    return String.format("%.3f", val);
                }
                if (col == COL_INCLUSIVE_PER) {
                    double val = md.getElapsedInclusive();
                    double per = val * 100.0 / mTotalTime;
                    return String.format("%.1f%%", per);
                }
                if (col == COL_CALLS)
                    return md.getCalls();
                if (col == COL_TIME_PER_CALL) {
                    int numCalls = md.getTotalCalls();
                    double val = md.getElapsedInclusive();
                    val = val / numCalls;
                    val = traceUnits.getScaledValue(val);
                    return String.format("%.3f", val);
                }
            } else if (element instanceof ProfileSelf) {
                ProfileSelf ps = (ProfileSelf) element;
                if (col == COL_NAME)
                    return ps.getProfileName();
                if (col == COL_INCLUSIVE) {
                    double val = ps.getElapsedInclusive();
                    val = traceUnits.getScaledValue(val);
                    return String.format("%.3f", val);
                }
                if (col == COL_INCLUSIVE_PER) {
                    double total;
                    double val = ps.getElapsedInclusive();
                    MethodData context = ps.getContext();
                    total = context.getElapsedInclusive();
                    double per = val * 100.0 / total;
                    return String.format("%.1f%%", per);
                }
                return "";
            } else if (element instanceof ProfileData) {
                ProfileData pd = (ProfileData) element;
                if (col == COL_NAME)
                    return pd.getProfileName();
                if (col == COL_INCLUSIVE) {
                    double val = pd.getElapsedInclusive();
                    val = traceUnits.getScaledValue(val);
                    return String.format("%.3f", val);
                }
                if (col == COL_INCLUSIVE_PER) {
                    double total;
                    double val = pd.getElapsedInclusive();
                    MethodData context = pd.getContext();
                    total = context.getElapsedInclusive();
                    double per = val * 100.0 / total;
                    return String.format("%.1f%%", per);
                }
                if (col == COL_CALLS)
                    return pd.getNumCalls();
                return "";
            } else if (element instanceof ProfileNode) {
                ProfileNode pn = (ProfileNode) element;
                if (col == COL_NAME)
                    return pn.getLabel();
                return "";
            }
            return "col" + col;
        }

        public Image getColumnImage(Object element, int col) {
            if (col != COL_NAME)
                return null;
            if (element instanceof MethodData) {
                MethodData md = (MethodData) element;
                return md.getImage();
            }
            if (element instanceof ProfileData) {
                ProfileData pd = (ProfileData) element;
                MethodData md = pd.getMethodData();
                return md.getImage();
            }
            return null;
        }

        public Color getForeground(Object element) {
            return null;
        }

        public Color getBackground(Object element) {
            if (element instanceof ProfileData) {
                ProfileData pd = (ProfileData) element;
                if (pd.isParent())
                    return colorParentsBack;
                return colorChildrenBack;
            }
            if (element instanceof ProfileNode) {
                ProfileNode pn = (ProfileNode) element;
                if (pn.isParent())
                    return colorParentsBack;
                return colorChildrenBack;
            }
            return null;
        }
    }

    class ColumnListener extends SelectionAdapter {
        MethodData.Sorter sorter = new MethodData.Sorter();

        @Override
        public void widgetSelected(SelectionEvent event) {
            TreeColumn column = (TreeColumn) event.widget;
            String name = column.getText();
            Tree tree = column.getParent();
            tree.setRedraw(false);
            TreeColumn[] columns = tree.getColumns();
            for (TreeColumn col : columns) {
                col.setImage(null);
            }
            if (name == mColumnNames[COL_NAME]) {
                // Sort names alphabetically
                sorter.setColumn(MethodData.Sorter.Column.BY_NAME);
                Arrays.sort(mRoots, sorter);
            } else if (name == mColumnNames[COL_EXCLUSIVE]) {
                sorter.setColumn(MethodData.Sorter.Column.BY_EXCLUSIVE);
                Arrays.sort(mRoots, sorter);
            } else if (name == mColumnNames[COL_EXCLUSIVE_PER]) {
                sorter.setColumn(MethodData.Sorter.Column.BY_EXCLUSIVE);
                Arrays.sort(mRoots, sorter);
            } else if (name == mColumnNames[COL_INCLUSIVE]) {
                sorter.setColumn(MethodData.Sorter.Column.BY_INCLUSIVE);
                Arrays.sort(mRoots, sorter);
            } else if (name == mColumnNames[COL_INCLUSIVE_PER]) {
                sorter.setColumn(MethodData.Sorter.Column.BY_INCLUSIVE);
                Arrays.sort(mRoots, sorter);
            } else if (name == mColumnNames[COL_CALLS]) {
                sorter.setColumn(MethodData.Sorter.Column.BY_CALLS);
                Arrays.sort(mRoots, sorter);
            } else if (name == mColumnNames[COL_TIME_PER_CALL]) {
                sorter.setColumn(MethodData.Sorter.Column.BY_TIME_PER_CALL);
                Arrays.sort(mRoots, sorter);
            }
            MethodData.Sorter.Direction direction = sorter.getDirection();
            if (direction == MethodData.Sorter.Direction.INCREASING)
                column.setImage(mSortDown);
            else
                column.setImage(mSortUp);
            tree.setRedraw(true);
            mTreeViewer.refresh();
        }
    }
}
