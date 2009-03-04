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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

public class MethodData {

    private int mId;
    private int mRank = -1;
    private String mClassName;
    private String mMethodName;
    private String mSignature;
    private String mName;
    private String mProfileName;
    private String mPathname;
    private int mLineNumber;
    private long mElapsedExclusive;
    private long mElapsedInclusive;
    private long mTopExclusive;
    private int[] mNumCalls = new int[2]; // index 0=normal, 1=recursive
    private Color mColor;
    private Color mFadedColor;
    private Image mImage;
    private Image mFadedImage;
    private HashMap<Integer, ProfileData> mParents;
    private HashMap<Integer, ProfileData> mChildren;
    
    // The parents of this method when this method was in a recursive call
    private HashMap<Integer, ProfileData> mRecursiveParents;
    
    // The children of this method when this method was in a recursive call
    private HashMap<Integer, ProfileData> mRecursiveChildren;

    private ProfileNode[] mProfileNodes;
    private int mX;
    private int mY;
    private double mWeight;

    public MethodData(int id, String className) {
        mId = id;
        mClassName = className;
        mMethodName = null;
        mSignature = null;
        mPathname = null;
        mLineNumber = -1;
        computeName();
        computeProfileName();
    }

    public MethodData(int id, String className, String methodName,
            String signature, String pathname, int lineNumber) {
        mId = id;
        mClassName = className;
        mMethodName = methodName;
        mSignature = signature;
        mPathname = pathname;
        mLineNumber = lineNumber;
        computeName();
        computeProfileName();
    }

    private Comparator<ProfileData> mByElapsedInclusive = new Comparator<ProfileData>() {
        public int compare(ProfileData pd1, ProfileData pd2) {
            if (pd2.getElapsedInclusive() > pd1.getElapsedInclusive())
                return 1;
            if (pd2.getElapsedInclusive() < pd1.getElapsedInclusive())
                return -1;
            return 0;
        }
    };

    public double addWeight(int x, int y, double weight) {
        if (mX == x && mY == y)
            mWeight += weight;
        else {
            mX = x;
            mY = y;
            mWeight = weight;
        }
        return mWeight;
    }

    public void clearWeight() {
        mWeight = 0;
    }

    public int getRank() {
        return mRank;
    }

    public void setRank(int rank) {
        mRank = rank;
        computeProfileName();
    }

    public void addElapsedExclusive(long time) {
        mElapsedExclusive += time;
    }

    public void addElapsedInclusive(long time, boolean isRecursive, Call parent) {
        if (isRecursive == false) {
            mElapsedInclusive += time;
            mNumCalls[0] += 1;
        } else {
            mNumCalls[1] += 1;
        }

        if (parent == null)
            return;

        // Find the child method in the parent
        MethodData parentMethod = parent.mMethodData;
        if (parent.isRecursive()) {
            parentMethod.mRecursiveChildren = updateInclusive(time,
                    parentMethod, this, false,
                    parentMethod.mRecursiveChildren);
        } else {
            parentMethod.mChildren = updateInclusive(time,
                    parentMethod, this, false, parentMethod.mChildren);
        }

        // Find the parent method in the child
        if (isRecursive) {
            mRecursiveParents = updateInclusive(time, this, parentMethod, true,
                    mRecursiveParents);
        } else {
            mParents = updateInclusive(time, this, parentMethod, true,
                    mParents);
        }
    }
    
    private HashMap<Integer, ProfileData> updateInclusive(long time,
            MethodData contextMethod, MethodData elementMethod,
            boolean elementIsParent, HashMap<Integer, ProfileData> map) {
        if (map == null) {
            map = new HashMap<Integer, ProfileData>(4);
        } else {
            ProfileData profileData = map.get(elementMethod.mId);
            if (profileData != null) {
                profileData.addElapsedInclusive(time);
                return map;
            }
        }

        ProfileData elementData = new ProfileData(contextMethod,
                elementMethod, elementIsParent);
        elementData.setElapsedInclusive(time);
        elementData.setNumCalls(1);
        map.put(elementMethod.mId, elementData);
        return map;
    }

    public void analyzeData() {
        // Sort the parents and children into decreasing inclusive time
        ProfileData[] sortedParents;
        ProfileData[] sortedChildren;
        ProfileData[] sortedRecursiveParents;
        ProfileData[] sortedRecursiveChildren;
        
        sortedParents = sortProfileData(mParents);
        sortedChildren = sortProfileData(mChildren);
        sortedRecursiveParents = sortProfileData(mRecursiveParents);
        sortedRecursiveChildren = sortProfileData(mRecursiveChildren);
        
        // Add "self" time to the top of the sorted children
        sortedChildren = addSelf(sortedChildren);
        
        // Create the ProfileNode objects that we need
        ArrayList<ProfileNode> nodes = new ArrayList<ProfileNode>();
        ProfileNode profileNode;
        if (mParents != null) {
            profileNode = new ProfileNode("Parents", this, sortedParents,
                    true, false);
            nodes.add(profileNode);
        }
        if (mChildren != null) {
            profileNode = new ProfileNode("Children", this, sortedChildren,
                    false, false);
            nodes.add(profileNode);
        }
        if (mRecursiveParents!= null) {
            profileNode = new ProfileNode("Parents while recursive", this,
                    sortedRecursiveParents, true, true);
            nodes.add(profileNode);
        }
        if (mRecursiveChildren != null) {
            profileNode = new ProfileNode("Children while recursive", this,
                    sortedRecursiveChildren, false, true);
            nodes.add(profileNode);
        }
        mProfileNodes = nodes.toArray(new ProfileNode[nodes.size()]);
    }
    
    // Create and return a ProfileData[] array that is a sorted copy
    // of the given HashMap values.
    private ProfileData[] sortProfileData(HashMap<Integer, ProfileData> map) {
        if (map == null)
            return null;

        // Convert the hash values to an array of ProfileData
        Collection<ProfileData> values = map.values();
        ProfileData[] sorted = values.toArray(new ProfileData[values.size()]);
        
        // Sort the array by elapsed inclusive time
        Arrays.sort(sorted, mByElapsedInclusive);
        return sorted;
    }
    
    private ProfileData[] addSelf(ProfileData[] children) {
        ProfileData[] pdata;
        if (children == null) {
            pdata = new ProfileData[1];
        } else {
            pdata = new ProfileData[children.length + 1];
            System.arraycopy(children, 0, pdata, 1, children.length);
        }
        pdata[0] = new ProfileSelf(this);
        return pdata;
    }

    public void addTopExclusive(long time) {
        mTopExclusive += time;
    }

    public long getTopExclusive() {
        return mTopExclusive;
    }

    public int getId() {
        return mId;
    }

    private void computeName() {
        if (mMethodName == null) {
            mName = mClassName;
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(mClassName);
        sb.append(".");  //$NON-NLS-1$
        sb.append(mMethodName);
        sb.append(" ");  //$NON-NLS-1$
        sb.append(mSignature);
        mName = sb.toString();
    }

    public String getName() {
        return mName;
    }

    public String getClassName() {
        return mClassName;
    }

    public String getMethodName() {
        return mMethodName;
    }

    public String getProfileName() {
        return mProfileName;
    }

    public void computeProfileName() {
        if (mRank == -1) {
            mProfileName = mName;
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(mRank);
        sb.append(" ");  //$NON-NLS-1$
        sb.append(getName());
        mProfileName = sb.toString();
    }

    public String getCalls() {
        return String.format("%d+%d", mNumCalls[0], mNumCalls[1]);
    }

    public int getTotalCalls() {
        return mNumCalls[0] + mNumCalls[1];
    }

    public Color getColor() {
        return mColor;
    }

    public void setColor(Color color) {
        mColor = color;
    }

    public void setImage(Image image) {
        mImage = image;
    }

    public Image getImage() {
        return mImage;
    }

    @Override
    public String toString() {
        return getName();
    }

    public long getElapsedExclusive() {
        return mElapsedExclusive;
    }

    public long getElapsedInclusive() {
        return mElapsedInclusive;
    }

    public void setFadedColor(Color fadedColor) {
        mFadedColor = fadedColor;
    }

    public Color getFadedColor() {
        return mFadedColor;
    }

    public void setFadedImage(Image fadedImage) {
        mFadedImage = fadedImage;
    }

    public Image getFadedImage() {
        return mFadedImage;
    }

    public void setPathname(String pathname) {
        mPathname = pathname;
    }

    public String getPathname() {
        return mPathname;
    }

    public void setLineNumber(int lineNumber) {
        mLineNumber = lineNumber;
    }

    public int getLineNumber() {
        return mLineNumber;
    }

    public ProfileNode[] getProfileNodes() {
        return mProfileNodes;
    }

    public static class Sorter implements Comparator<MethodData> {
        public int compare(MethodData md1, MethodData md2) {
            if (mColumn == Column.BY_NAME) {
                int result = md1.getName().compareTo(md2.getName());
                return (mDirection == Direction.INCREASING) ? result : -result;
            }
            if (mColumn == Column.BY_INCLUSIVE) {
                if (md2.getElapsedInclusive() > md1.getElapsedInclusive())
                    return (mDirection == Direction.INCREASING) ? -1 : 1;
                if (md2.getElapsedInclusive() < md1.getElapsedInclusive())
                    return (mDirection == Direction.INCREASING) ? 1 : -1;
                return md1.getName().compareTo(md2.getName());
            }
            if (mColumn == Column.BY_EXCLUSIVE) {
                if (md2.getElapsedExclusive() > md1.getElapsedExclusive())
                    return (mDirection == Direction.INCREASING) ? -1 : 1;
                if (md2.getElapsedExclusive() < md1.getElapsedExclusive())
                    return (mDirection == Direction.INCREASING) ? 1 : -1;
                return md1.getName().compareTo(md2.getName());
            }
            if (mColumn == Column.BY_CALLS) {
                int result = md1.getTotalCalls() - md2.getTotalCalls();
                if (result == 0)
                    return md1.getName().compareTo(md2.getName());
                return (mDirection == Direction.INCREASING) ? result : -result;
            }
            if (mColumn == Column.BY_TIME_PER_CALL) {
                double time1 = md1.getElapsedInclusive();
                time1 = time1 / md1.getTotalCalls();
                double time2 = md2.getElapsedInclusive();
                time2 = time2 / md2.getTotalCalls();
                double diff = time1 - time2;
                int result = 0;
                if (diff < 0)
                    result = -1;
                else if (diff > 0)
                    result = 1;
                if (result == 0)
                    return md1.getName().compareTo(md2.getName());
                return (mDirection == Direction.INCREASING) ? result : -result;
            }
            return 0;
        }

        public void setColumn(Column column) {
            // If the sort column specified is the same as last time,
            // then reverse the sort order.
            if (mColumn == column) {
                // Reverse the sort order
                if (mDirection == Direction.INCREASING)
                    mDirection = Direction.DECREASING;
                else
                    mDirection = Direction.INCREASING;
            } else {
                // Sort names into increasing order, data into decreasing order.
                if (column == Column.BY_NAME)
                    mDirection = Direction.INCREASING;
                else
                    mDirection = Direction.DECREASING;
            }
            mColumn = column;
        }

        public Column getColumn() {
            return mColumn;
        }

        public void setDirection(Direction direction) {
            mDirection = direction;
        }

        public Direction getDirection() {
            return mDirection;
        }

        public static enum Column {
            BY_NAME, BY_EXCLUSIVE, BY_INCLUSIVE, BY_CALLS, BY_TIME_PER_CALL
        };

        public static enum Direction {
            INCREASING, DECREASING
        };

        private Column mColumn;
        private Direction mDirection;
    }
}
