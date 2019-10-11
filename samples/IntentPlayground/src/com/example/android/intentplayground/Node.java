/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.example.android.intentplayground;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents a node in the tree of tasks. It can either represent a task
 * or an activity.
 */
public class Node implements Parcelable, Comparable<Node> {
    static final int NEW_TASK_ID = 0xa4d701d;
    public static final int ROOT_NODE_ID = 0xAABBCCDD;
    public int mTaskId;
    public List<Node> mChildren = new LinkedList<>();
    public ComponentName mName;
    private static final int CURRENT  = 0x1;
    private static final int MODIFIED  = 0x2;
    private static final int NEW  = 0x4;
    private boolean mIsTaskNode;
    private int mOptionFlags;
    private Intent mIntent;

    Node(ComponentName data) {
        mIsTaskNode = false;
        mName = data;
    }

    /**
     * Create a task Node.
     * @param taskId the id of the task.
     */
    Node(int taskId) {
        mIsTaskNode = true;
        mTaskId = taskId;
    }

    /**
     * Creates a Node with the same data as the parameter (copy constructor).
     * @param other Node to copy over.
     */
    Node(Node other) {
        if (other.mIsTaskNode) {
            mIsTaskNode = true;
            mTaskId = other.mTaskId;
        } else {
            mIsTaskNode = false;
            mName = other.mName.clone();
        }
        mOptionFlags = other.mOptionFlags;
        mIntent = other.mIntent;
        other.mChildren.forEach(child -> addChild(new Node(child)));
    }

    /**
     * Adds a child to this Node's children.
     * @param child The child node to add.
     * @return returns This Node object for method chaining.
     */
    Node addChild(Node child) {
        mChildren.add(child);
        return this;
    }

    /**
     * Adds a child to the beginning of the list of this Node's children.
     * @param child The child node to add.
     * @return This Node object for method chaining.
     */
    Node addFirstChild(Node child) {
        mChildren.add(0, child);
        return this;
    }

    /**
     * Clear children from this Node.
     * @return returns This Node object for method chaining.
     */
    Node clearChildren() {
        mChildren.clear();
        return this;
    }

    static Node newTaskNode() {
        return new Node(NEW_TASK_ID);
    }

    static Node newRootNode() {
        return new Node(ROOT_NODE_ID);
    }

    boolean isModified() {
        return (mOptionFlags & MODIFIED) != 0;
    }

    void setModified(boolean value) {
        if (value) {
            mOptionFlags |= MODIFIED;
        } else {
            mOptionFlags &= ~MODIFIED;
        }
    }

    boolean isNew() {
        return ((mOptionFlags & NEW) != 0) || (mIsTaskNode && (mTaskId == NEW_TASK_ID));
    }
    void setNew(boolean value) {
        if (value) {
            mOptionFlags |= NEW;
        } else {
            mOptionFlags &= ~NEW;
        }
    }

    boolean isCurrent() {
        return (mOptionFlags & CURRENT) != 0;
    }

    Node setCurrent(boolean value) {
        if (value) {
            mOptionFlags |= CURRENT;
        } else {
            mOptionFlags &= ~CURRENT;
        }
        return this;
    }

    public Node setIntent(Intent intent) {
        mIntent = new Intent(intent);
        return this;
    }

    public Intent getIntent() {
        return mIntent;
    }

    private Node(Parcel in) {
        mIsTaskNode = in.readInt() == 1;
        if (mIsTaskNode) {
            mTaskId = in.readInt();
        } else {
            mName = ComponentName.CREATOR.createFromParcel(in);
        }
        if (in.readInt() > 0) {
            in.readTypedList(mChildren, Node.CREATOR);
        } else {
            mChildren = new LinkedList<>();
        }
        mOptionFlags = in.readInt();
        if (in.readInt() > 0) {
            mIntent = Intent.CREATOR.createFromParcel(in);
        }
    }

    /**
     * Compare the tree represented by this Node to another to determine if
     * they are isomorphic.
     * @param other The Node to compare to this.
     */
    public boolean equals(Node other) {
        if (mIsTaskNode && other.mIsTaskNode) {
            // Check if taskIds are equal, or if one is a new task (which is essentially a wildcard)
            if ((mTaskId != other.mTaskId) && (mTaskId != NEW_TASK_ID)
                    && (other.mTaskId != NEW_TASK_ID)) {
                return false;
            }
        } else if (!mIsTaskNode && !other.mIsTaskNode){
            if (!other.mName.equals(mName)) return false;
        } else return false;
        if (mChildren.size() == 0 && other.mChildren.size() == 0) {
            return true;
        } else if (mChildren.size() != other.mChildren.size()){
            return false;
        } else {
            Collections.sort(mChildren);
            Collections.sort(other.mChildren);
            for (int i = 0; i < mChildren.size(); i++) {
                if (!mChildren.get(i).equals(other.mChildren.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Note: this class has a natural ordering that is inconsistent with equals().
     * compareTo() makes comparison based on the {@link ComponentName} that this class
     * holds, and does not consider its children.
     */
    public int compareTo(Node o) {
        return mIsTaskNode ? Integer.valueOf(mTaskId).compareTo(o.mTaskId)
                : mName.compareTo(o.mName);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("Node ");
        if (isCurrent()) output.append("current ");
        if (isNew()) output.append("new ");
        if (isModified()) output.append("modified ");
        output.append("<<");
        if (mIsTaskNode) output.append("taskId=").append(mTaskId);
        else output.append(mName.toShortString());
        if (mIntent != null) {
            output.append("intent:(");
            FlagUtils.discoverFlags(mIntent).forEach(flag -> {
                output.append(flag.replace(FlagUtils.INTENT_FLAG_PREFIX, "")).append(',');
            });
            output.append(")");
        }
        output.append(">> {");
        if (!mChildren.isEmpty()) output.append('\n');
        mChildren.forEach(child -> Arrays.asList(child.toString().split("\n")).forEach(line ->
                output.append("\t\t").append(line).append("\n")));
        output.append("}\n");
        return output.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt( mIsTaskNode ? 1 : 0);
        if (mIsTaskNode) {
            dest.writeInt(mTaskId);
        } else {
            mName.writeToParcel(dest, 0);
        }
        if (mChildren.size() == 0 || mChildren == null) {
            dest.writeInt(0);
        } else {
            dest.writeInt(1);
            dest.writeTypedList(mChildren);
        }
        dest.writeInt(mOptionFlags);
        dest.writeInt(mIntent == null ? 0 : 1);
        if (mIntent != null) mIntent.writeToParcel(dest, 0 /* flags */);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Node> CREATOR = new Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel in) {
            return new Node(in);
        }

        @Override
        public Node[] newArray(int size) {
            return new Node[size];
        }
    };
}
