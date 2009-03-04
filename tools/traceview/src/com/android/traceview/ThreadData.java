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

import java.util.ArrayList;
import java.util.HashMap;

class ThreadData implements TimeLineView.Row {

    private int mId;
    private String mName;
    private long mGlobalStartTime = -1;
    private long mGlobalEndTime = -1;
    private long mLastEventTime;
    private long mCpuTime;
    private Call mRoot;
    private Call mCurrent;
    private Call mLastContextSwitch;
    private ArrayList<Call> mStack = new ArrayList<Call>();
    
    // This is a hash of all the methods that are currently on the stack.
    private HashMap<MethodData, Integer> mStackMethods = new HashMap<MethodData, Integer>();
    
    // True if no calls have ever been added to this thread
    private boolean mIsEmpty;

    ThreadData(int id, String name, MethodData topLevel) {
        mId = id;
        mName = String.format("[%d] %s", id, name);
        mRoot = new Call(mName, topLevel);
        mCurrent = mRoot;
        mIsEmpty = true;
    }

    public boolean isEmpty() {
        return mIsEmpty;
    }

    public String getName() {
        return mName;
    }

    public Call getCalltreeRoot() {
        return mRoot;
    }

    void handleCall(Call call, long globalTime) {
        mIsEmpty = false;
        long currentTime = call.mThreadStartTime;
        if (currentTime < mLastEventTime) {
            System.err
            .printf(
                    "ThreadData: '%1$s' call time (%2$d) is less than previous time (%3$d) for thread '%4$s'\n",
                    call.getName(), currentTime, mLastEventTime, mName);
            System.exit(1);
        }
        long elapsed = currentTime - mLastEventTime;
        mCpuTime += elapsed;
        if (call.getMethodAction() == 0) {
            // This is a method entry.
            enter(call, elapsed);
        } else {
            // This is a method exit.
            exit(call, elapsed, globalTime);
        }
        mLastEventTime = currentTime;
        mGlobalEndTime = globalTime;
    }

    private void enter(Call c, long elapsed) {
        Call caller = mCurrent;
        push(c);
        
        // Check the stack for a matching method to determine if this call
        // is recursive.
        MethodData md = c.mMethodData;
        Integer num = mStackMethods.get(md);
        if (num == null) {
            num = 0;
        } else if (num > 0) {
            c.setRecursive(true);
        }
        num += 1;
        mStackMethods.put(md, num);
        mCurrent = c;

        // Add the elapsed time to the caller's exclusive time
        caller.addExclusiveTime(elapsed);
    }

    private void exit(Call c, long elapsed, long globalTime) {
        mCurrent.mGlobalEndTime = globalTime;
        Call top = pop();
        if (top == null) {
            return;
        }

        if (mCurrent.mMethodData != c.mMethodData) {
            String error = "Method exit (" + c.getName()
                    + ") does not match current method (" + mCurrent.getName()
                    + ")";
            throw new RuntimeException(error);
        } else {
            long duration = c.mThreadStartTime - mCurrent.mThreadStartTime;
            Call caller = top();
            mCurrent.addExclusiveTime(elapsed);
            mCurrent.addInclusiveTime(duration, caller);
            if (caller == null) {
                caller = mRoot;
            }
            mCurrent = caller;
        }
    }

    public void push(Call c) {
        mStack.add(c);
    }

    public Call pop() {
        ArrayList<Call> stack = mStack;
        if (stack.size() == 0)
            return null;
        Call top = stack.get(stack.size() - 1);
        stack.remove(stack.size() - 1);
        
        // Decrement the count on the method in the hash table and remove
        // the entry when it goes to zero.
        MethodData md = top.mMethodData;
        Integer num = mStackMethods.get(md);
        if (num != null) {
            num -= 1;
            if (num <= 0) {
                mStackMethods.remove(md);
            } else {
                mStackMethods.put(md, num);
            }
        }
        return top;
    }

    public Call top() {
        ArrayList<Call> stack = mStack;
        if (stack.size() == 0)
            return null;
        return stack.get(stack.size() - 1);
    }

    public long endTrace() {
        // If we have calls on the stack when the trace ends, then clean up
        // the stack and compute the inclusive time of the methods by pretending
        // that we are exiting from their methods now.
        while (mCurrent != mRoot) {
            long duration = mLastEventTime - mCurrent.mThreadStartTime;
            pop();
            Call caller = top();
            mCurrent.addInclusiveTime(duration, caller);
            mCurrent.mGlobalEndTime = mGlobalEndTime;
            if (caller == null) {
                caller = mRoot;
            }
            mCurrent = caller;
        }
        return mLastEventTime;
    }

    @Override
    public String toString() {
        return mName;
    }

    public int getId() {
        return mId;
    }

    public void setCpuTime(long cpuTime) {
        mCpuTime = cpuTime;
    }

    public long getCpuTime() {
        return mCpuTime;
    }

    public void setGlobalStartTime(long globalStartTime) {
        mGlobalStartTime = globalStartTime;
    }

    public long getGlobalStartTime() {
        return mGlobalStartTime;
    }

    public void setLastEventTime(long lastEventTime) {
        mLastEventTime = lastEventTime;
    }

    public long getLastEventTime() {
        return mLastEventTime;
    }

    public void setGlobalEndTime(long globalEndTime) {
        mGlobalEndTime = globalEndTime;
    }

    public long getGlobalEndTime() {
        return mGlobalEndTime;
    }

    public void setLastContextSwitch(Call lastContextSwitch) {
        mLastContextSwitch = lastContextSwitch;
    }

    public Call getLastContextSwitch() {
        return mLastContextSwitch;
    }
}
