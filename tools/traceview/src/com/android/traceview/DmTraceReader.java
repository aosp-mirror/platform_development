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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DmTraceReader extends TraceReader {

    private int mVersionNumber = 0;
    private boolean mDebug = false;
    private static final int TRACE_MAGIC = 0x574f4c53;
    private boolean mRegression;
    private ProfileProvider mProfileProvider;
    private String mTraceFileName;
    private MethodData mTopLevel;
    private ArrayList<Call> mCallList;
    private ArrayList<Call> mSwitchList;
    private HashMap<Integer, MethodData> mMethodMap;
    private HashMap<Integer, ThreadData> mThreadMap;
    private ThreadData[] mSortedThreads;
    private MethodData[] mSortedMethods;
    private long mGlobalEndTime;
    private MethodData mContextSwitch;
    private int mOffsetToData;
    private byte[] mBytes = new byte[8];

    // A regex for matching the thread "id name" lines in the .key file
    private static final Pattern mIdNamePattern = Pattern.compile("(\\d+)\t(.*)");  // $NON-NLS-1$

    DmTraceReader(String traceFileName, boolean regression) {
        mTraceFileName = traceFileName;
        mRegression = regression;
        mMethodMap = new HashMap<Integer, MethodData>();
        mThreadMap = new HashMap<Integer, ThreadData>();

        // Create a single top-level MethodData object to hold the profile data
        // for time spent in the unknown caller.
        mTopLevel = new MethodData(0, "(toplevel)");
        mContextSwitch = new MethodData(-1, "(context switch)");
        mMethodMap.put(0, mTopLevel);
        generateTrees();
        // dumpTrees();
    }

    void generateTrees() {
        try {
            long offset = parseKeys();
            parseData(offset);
            analyzeData();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public ProfileProvider getProfileProvider() {
        if (mProfileProvider == null)
            mProfileProvider = new ProfileProvider(this);
        return mProfileProvider;
    }

    Call readCall(MappedByteBuffer buffer, Call call) {
        int threadId;
        int methodId;
        long time;
        
        try {
            if (mVersionNumber == 1)
                threadId = buffer.get();
            else
                threadId = buffer.getShort();
            methodId = buffer.getInt();
            time = buffer.getInt();
        } catch (BufferUnderflowException ex) {
            return null;
        }
        
        int methodAction = methodId & 0x03;
        methodId = methodId & ~0x03;
        MethodData methodData = mMethodMap.get(methodId);
        if (methodData == null) {
            String name = String.format("(0x%1$x)", methodId);  // $NON-NLS-1$
            methodData = new MethodData(methodId, name);
        }
        
        if (call != null) {
            call.set(threadId, methodData, time, methodAction);
        } else {
            call = new Call(threadId, methodData, time, methodAction);
        }
        return call;
    }
    
    private MappedByteBuffer mapFile(String filename, long offset) {
        MappedByteBuffer buffer = null;
        try {
            FileInputStream dataFile = new FileInputStream(filename);
            File file = new File(filename);
            FileChannel fc = dataFile.getChannel();
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, offset, file.length() - offset);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        
        return buffer;
    }
    
    private void readDataFileHeader(MappedByteBuffer buffer) {
        int magic = buffer.getInt();
        if (magic != TRACE_MAGIC) {
            System.err.printf(
                    "Error: magic number mismatch; got 0x%x, expected 0x%x\n",
                    magic, TRACE_MAGIC);
            throw new RuntimeException();
        }
        // read version
        int version = buffer.getShort();
        
        // read offset
        mOffsetToData = buffer.getShort() - 16;
        
        // read startWhen
        buffer.getLong();
        
        // Skip over "mOffsetToData" bytes
        for (int ii = 0; ii < mOffsetToData; ii++) {
            buffer.get();
        }
        
        // Save this position so that we can re-read the data later
        buffer.mark();
    }

    private void parseData(long offset) {
        MappedByteBuffer buffer = mapFile(mTraceFileName, offset);
        readDataFileHeader(buffer);
        parseDataPass1(buffer);
        
        buffer.reset();
        parseDataPass2(buffer);
    }
    
    private void parseDataPass1(MappedByteBuffer buffer) {
        mSwitchList = new ArrayList<Call>();

        // Read the first call so that we can set "prevThreadData"
        Call call = new Call();
        call = readCall(buffer, call);
        if (call == null)
            return;
        long callTime = call.mThreadStartTime;
        long prevCallTime = 0;
        ThreadData threadData = mThreadMap.get(call.getThreadId());
        if (threadData == null) {
            String name = String.format("[%1$d]", call.getThreadId());  // $NON-NLS-1$
            threadData = new ThreadData(call.getThreadId(), name, mTopLevel);
            mThreadMap.put(call.getThreadId(), threadData);
        }
        ThreadData prevThreadData = threadData;
        while (true) {
            // If a context switch occurred, then insert a placeholder "call"
            // record so that we can do something reasonable with the global
            // timestamps.
            if (prevThreadData != threadData) {
                Call switchEnter = new Call(prevThreadData.getId(),
                        mContextSwitch, prevCallTime, 0);
                prevThreadData.setLastContextSwitch(switchEnter);
                mSwitchList.add(switchEnter);
                Call contextSwitch = threadData.getLastContextSwitch();
                if (contextSwitch != null) {
                    long prevStartTime = contextSwitch.mThreadStartTime;
                    long elapsed = callTime - prevStartTime;
                    long beforeSwitch = elapsed / 2;
                    long afterSwitch = elapsed - beforeSwitch;
                    long exitTime = callTime - afterSwitch;
                    contextSwitch.mThreadStartTime = prevStartTime + beforeSwitch;
                    Call switchExit = new Call(threadData.getId(),
                            mContextSwitch, exitTime, 1);
                    
                    mSwitchList.add(switchExit);
                }
                prevThreadData = threadData;
            }

            // Read the next call
            call = readCall(buffer, call);
            if (call == null) {
                break;
            }
            prevCallTime = callTime;
            callTime = call.mThreadStartTime;

            threadData = mThreadMap.get(call.getThreadId());
            if (threadData == null) {
                String name = String.format("[%d]", call.getThreadId());
                threadData = new ThreadData(call.getThreadId(), name, mTopLevel);
                mThreadMap.put(call.getThreadId(), threadData);
            }
        }
    }

    void parseDataPass2(MappedByteBuffer buffer) {
        mCallList = new ArrayList<Call>();

        // Read the first call so that we can set "prevThreadData"
        Call call = readCall(buffer, null);
        long callTime = call.mThreadStartTime;
        long prevCallTime = callTime;
        ThreadData threadData = mThreadMap.get(call.getThreadId());
        ThreadData prevThreadData = threadData;
        threadData.setGlobalStartTime(0);
        
        int nthContextSwitch = 0;

        // Assign a global timestamp to each event.
        long globalTime = 0;
        while (true) {
            long elapsed = callTime - prevCallTime;
            if (threadData != prevThreadData) {
                // Get the next context switch.  This one is entered
                // by the previous thread.
                Call contextSwitch = mSwitchList.get(nthContextSwitch++);
                mCallList.add(contextSwitch);
                elapsed = contextSwitch.mThreadStartTime - prevCallTime;
                globalTime += elapsed;
                elapsed = 0;
                contextSwitch.mGlobalStartTime = globalTime;
                prevThreadData.handleCall(contextSwitch, globalTime);
                
                if (!threadData.isEmpty()) {
                    // This context switch is exited by the current thread.
                    contextSwitch = mSwitchList.get(nthContextSwitch++);
                    mCallList.add(contextSwitch);
                    contextSwitch.mGlobalStartTime = globalTime;
                    elapsed = callTime - contextSwitch.mThreadStartTime;
                    threadData.handleCall(contextSwitch, globalTime);
                }

                // If the thread's global start time has not been set yet,
                // then set it.
                if (threadData.getGlobalStartTime() == -1)
                    threadData.setGlobalStartTime(globalTime);
                prevThreadData = threadData;
            }

            globalTime += elapsed;
            call.mGlobalStartTime = globalTime;
            
            threadData.handleCall(call, globalTime);
            mCallList.add(call);
            
            // Read the next call
            call = readCall(buffer, null);
            if (call == null) {
                break;
            }
            prevCallTime = callTime;
            callTime = call.mThreadStartTime;
            threadData = mThreadMap.get(call.getThreadId());
        }

        // Allow each thread to do any cleanup of the call stack.
        // Also add the elapsed time for each thread to the toplevel
        // method's inclusive time.
        for (int id : mThreadMap.keySet()) {
            threadData = mThreadMap.get(id);
            long endTime = threadData.endTrace();
            if (endTime > 0)
                mTopLevel.addElapsedInclusive(endTime, false, null);
        }

        mGlobalEndTime = globalTime;
        
        if (mRegression) {
            dumpCallTimes();
        }
    }

    static final int PARSE_VERSION = 0;
    static final int PARSE_THREADS = 1;
    static final int PARSE_METHODS = 2;
    static final int PARSE_OPTIONS = 4;

    long parseKeys() throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(mTraceFileName));
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        }

        long offset = 0;
        int mode = PARSE_VERSION;
        String line = null;
        while (true) {
            line = in.readLine();
            if (line == null) {
                throw new IOException("Key section does not have an *end marker");
            }
            
            // Calculate how much we have read from the file so far.  The
            // extra byte is for the line ending not included by readLine().
            offset += line.length() + 1;
            if (line.startsWith("*")) {
                if (line.equals("*version")) {
                    mode = PARSE_VERSION;
                    continue;
                }
                if (line.equals("*threads")) {
                    mode = PARSE_THREADS;
                    continue;
                }
                if (line.equals("*methods")) {
                    mode = PARSE_METHODS;
                    continue;
                }
                if (line.equals("*end")) {
                    return offset;
                }
            }
            switch (mode) {
            case PARSE_VERSION:
                mVersionNumber = Integer.decode(line);
                mode = PARSE_OPTIONS;
                break;
            case PARSE_THREADS:
                parseThread(line);
                break;
            case PARSE_METHODS:
                parseMethod(line);
                break;
            case PARSE_OPTIONS:
                break;
            }
        }
    }

    void parseThread(String line) {
        String idStr = null;
        String name = null;
        Matcher matcher = mIdNamePattern.matcher(line);
        if (matcher.find()) {
            idStr = matcher.group(1);
            name = matcher.group(2);
        }
        if (idStr == null) return;
        if (name == null) name = "(unknown)";

        int id = Integer.decode(idStr);
        mThreadMap.put(id, new ThreadData(id, name, mTopLevel));
    }

    void parseMethod(String line) {
        String[] tokens = line.split("\t");
        int id = Long.decode(tokens[0]).intValue();
        String className = tokens[1];
        String methodName = null;
        String signature = null;
        String pathname = null;
        int lineNumber = -1;
        if (tokens.length == 6) {
            methodName = tokens[2];
            signature = tokens[3];
            pathname = tokens[4];
            lineNumber = Integer.decode(tokens[5]);
            pathname = constructPathname(className, pathname);
        } else if (tokens.length > 2) {
            if (tokens[3].startsWith("(")) {
                methodName = tokens[2];
                signature = tokens[3];
            } else {
                pathname = tokens[2];
                lineNumber = Integer.decode(tokens[3]);
            }
        }

        mMethodMap.put(id, new MethodData(id, className, methodName, signature,
                pathname, lineNumber));
    }

    private String constructPathname(String className, String pathname) {
        int index = className.lastIndexOf('/');
        if (index > 0 && index < className.length() - 1
                && pathname.endsWith(".java"))
            pathname = className.substring(0, index + 1) + pathname;
        return pathname;
    }

    private void analyzeData() {
        // Sort the threads into decreasing cpu time
        Collection<ThreadData> tv = mThreadMap.values();
        mSortedThreads = tv.toArray(new ThreadData[tv.size()]);
        Arrays.sort(mSortedThreads, new Comparator<ThreadData>() {
            public int compare(ThreadData td1, ThreadData td2) {
                if (td2.getCpuTime() > td1.getCpuTime())
                    return 1;
                if (td2.getCpuTime() < td1.getCpuTime())
                    return -1;
                return td2.getName().compareTo(td1.getName());
            }
        });

        // Analyze the call tree so that we can label the "worst" children.
        // Also set all the root pointers in each node in the call tree.
        long sum = 0;
        for (ThreadData t : mSortedThreads) {
            if (t.isEmpty() == false) {
                Call root = t.getCalltreeRoot();
                root.mGlobalStartTime = t.getGlobalStartTime();
            }
        }

        // Sort the methods into decreasing inclusive time
        Collection<MethodData> mv = mMethodMap.values();
        MethodData[] methods;
        methods = mv.toArray(new MethodData[mv.size()]);
        Arrays.sort(methods, new Comparator<MethodData>() {
            public int compare(MethodData md1, MethodData md2) {
                if (md2.getElapsedInclusive() > md1.getElapsedInclusive())
                    return 1;
                if (md2.getElapsedInclusive() < md1.getElapsedInclusive())
                    return -1;
                return md1.getName().compareTo(md2.getName());
            }
        });

        // Count the number of methods with non-zero inclusive time
        int nonZero = 0;
        for (MethodData md : methods) {
            if (md.getElapsedInclusive() == 0)
                break;
            nonZero += 1;
        }

        // Copy the methods with non-zero time
        mSortedMethods = new MethodData[nonZero];
        int ii = 0;
        for (MethodData md : methods) {
            if (md.getElapsedInclusive() == 0)
                break;
            md.setRank(ii);
            mSortedMethods[ii++] = md;
        }

        // Let each method analyze its profile data
        for (MethodData md : mSortedMethods) {
            md.analyzeData();
        }

        // Update all the calls to include the method rank in
        // their name.
        for (Call call : mCallList) {
            call.updateName();
        }
        
        if (mRegression) {
            dumpMethodStats();
        }
    }

    /*
     * This method computes a list of records that describe the the execution
     * timeline for each thread. Each record is a pair: (row, block) where: row:
     * is the ThreadData object block: is the call (containing the start and end
     * times)
     */
    @Override
    public ArrayList<TimeLineView.Record> getThreadTimeRecords() {
        TimeLineView.Record record;
        ArrayList<TimeLineView.Record> timeRecs;
        timeRecs = new ArrayList<TimeLineView.Record>();

        // For each thread, push a "toplevel" call that encompasses the
        // entire execution of the thread.
        for (ThreadData threadData : mSortedThreads) {
            if (!threadData.isEmpty() && threadData.getId() != 0) {
                Call call = new Call(threadData.getId(), mTopLevel,
                        threadData.getGlobalStartTime(), 0);
                call.mGlobalStartTime = threadData.getGlobalStartTime();
                call.mGlobalEndTime = threadData.getGlobalEndTime();
                record = new TimeLineView.Record(threadData, call);
                timeRecs.add(record);
            }
        }

        for (Call call : mCallList) {
            if (call.getMethodAction() != 0 || call.getThreadId() == 0)
                continue;
            ThreadData threadData = mThreadMap.get(call.getThreadId());
            record = new TimeLineView.Record(threadData, call);
            timeRecs.add(record);
        }
        
        if (mRegression) {
            dumpTimeRecs(timeRecs);
            System.exit(0);
        }
        return timeRecs;
    }
        
    private void dumpCallTimes() {
        String action;
        
        System.out.format("id thread  global start,end   method\n");
        for (Call call : mCallList) {
            if (call.getMethodAction() == 0) {
                action = "+";
            } else {
                action = " ";
            }
            long callTime = call.mThreadStartTime;
            System.out.format("%2d %6d %8d %8d %s %s\n",
                    call.getThreadId(), callTime, call.mGlobalStartTime,
                    call.mGlobalEndTime, action, call.getMethodData().getName());
//            if (call.getMethodAction() == 0 && call.getGlobalEndTime() < call.getGlobalStartTime()) {
//                System.out.printf("endtime %d < startTime %d\n",
//                        call.getGlobalEndTime(), call.getGlobalStartTime());
//            }
        }
    }
    
    private void dumpMethodStats() {
        System.out.format("\nExclusive Inclusive     Calls  Method\n");
        for (MethodData md : mSortedMethods) {
            System.out.format("%9d %9d %9s  %s\n",
                    md.getElapsedExclusive(), md.getElapsedInclusive(),
                    md.getCalls(), md.getProfileName());
        }
    }

    private void dumpTimeRecs(ArrayList<TimeLineView.Record> timeRecs) {
        System.out.format("\nid thread  global start,end  method\n");
        for (TimeLineView.Record record : timeRecs) {
            Call call = (Call) record.block;
            long callTime = call.mThreadStartTime;
            System.out.format("%2d %6d %8d %8d  %s\n",
                    call.getThreadId(), callTime,
                    call.mGlobalStartTime, call.mGlobalEndTime,
                    call.getMethodData().getName());
        }
    }

    @Override
    public HashMap<Integer, String> getThreadLabels() {
        HashMap<Integer, String> labels = new HashMap<Integer, String>();
        for (ThreadData t : mThreadMap.values()) {
            labels.put(t.getId(), t.getName());
        }
        return labels;
    }

    @Override
    public MethodData[] getMethods() {
        return mSortedMethods;
    }

    @Override
    public ThreadData[] getThreads() {
        return mSortedThreads;
    }

    @Override
    public long getEndTime() {
        return mGlobalEndTime;
    }
}
