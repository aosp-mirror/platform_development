/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.commands.monkey;

import android.os.SystemClock;
import android.view.KeyEvent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * monkey event queue. It takes a script to produce events
 * 
 * sample script format:
 *      type= raw events
 *      count= 10
 *      speed= 1.0
 *      start data >>
 *      captureDispatchPointer(5109520,5109520,0,230.75429,458.1814,0.20784314,
 *          0.06666667,0,0.0,0.0,65539,0)
 *      captureDispatchKey(5113146,5113146,0,20,0,0,0,0)
 *      captureDispatchFlip(true)
 *      ...
 */
public class MonkeySourceScript implements MonkeyEventSource {    
    private int mEventCountInScript = 0;  //total number of events in the file
    private int mVerbose = 0;
    private double mSpeed = 1.0;
    private String mScriptFileName; 
    private MonkeyEventQueue mQ;
    
    private static final String HEADER_TYPE = "type=";
    private static final String HEADER_COUNT = "count=";
    private static final String HEADER_SPEED = "speed=";
    
    private long mLastRecordedDownTimeKey = 0;    
    private long mLastRecordedDownTimeMotion = 0;
    private long mLastExportDownTimeKey = 0;
    private long mLastExportDownTimeMotion = 0;
    private long mLastExportEventTime = -1;
    private long mLastRecordedEventTime = -1;
    
    private static final boolean THIS_DEBUG = false;
    // a parameter that compensates the difference of real elapsed time and 
    // time in theory
    private static final long SLEEP_COMPENSATE_DIFF = 16;    
    
    // maximum number of events that we read at one time
    private static final int MAX_ONE_TIME_READS = 100;
    
    // number of additional events added to the script 
    // add HOME_KEY down and up events to make start UI consistent in each round 
    private static final int POLICY_ADDITIONAL_EVENT_COUNT = 2;

    // event key word in the capture log    
    private static final String EVENT_KEYWORD_POINTER = "DispatchPointer";
    private static final String EVENT_KEYWORD_TRACKBALL = "DispatchTrackball";
    private static final String EVENT_KEYWORD_KEY = "DispatchKey";
    private static final String EVENT_KEYWORD_FLIP = "DispatchFlip";
    
    // a line at the end of the header
    private static final String STARTING_DATA_LINE = "start data >>";    
    private boolean mFileOpened = false;    
    
    FileInputStream mFStream;
    DataInputStream mInputStream;
    BufferedReader mBufferReader;
    
    public MonkeySourceScript(String filename, long throttle) {
        mScriptFileName = filename;
        mQ = new MonkeyEventQueue(throttle);
    }
    
    /**
     * 
     * @return the number of total events that will be generated in a round 
     */
    public int getOneRoundEventCount() {
        //plus one home key down and up event
        return mEventCountInScript + POLICY_ADDITIONAL_EVENT_COUNT; 
    }
    
    private void resetValue() {
        mLastRecordedDownTimeKey = 0;    
        mLastRecordedDownTimeMotion = 0;
        mLastExportDownTimeKey = 0;
        mLastExportDownTimeMotion = 0;    
        mLastRecordedEventTime = -1;        
        mLastExportEventTime = -1;
    }
    
    private boolean readScriptHeader() {
        mEventCountInScript = -1;
        mFileOpened = false;        
        try {
            if (THIS_DEBUG) {
                System.out.println("reading script header");
            }
            
            mFStream  =  new FileInputStream(mScriptFileName);
            mInputStream  = new DataInputStream(mFStream);
            mBufferReader = new BufferedReader(
                    new InputStreamReader(mInputStream));
            String sLine;
            while ((sLine = mBufferReader.readLine()) != null) {
                sLine = sLine.trim();
                if (sLine.indexOf(HEADER_TYPE) >= 0) {
                    // at this point, we only have one type of script
                } else if (sLine.indexOf(HEADER_COUNT) >= 0) {
                    try {
                        mEventCountInScript = Integer.parseInt(sLine.substring(
                                HEADER_COUNT.length() + 1).trim());
                    } catch (NumberFormatException e) {
                        System.err.println(e);
                    }
                } else if (sLine.indexOf(HEADER_SPEED) >= 0) {
                    try {
                        mSpeed = Double.parseDouble(sLine.substring(
                                HEADER_SPEED.length() + 1).trim());
                        
                    } catch (NumberFormatException e) {
                        System.err.println(e);
                    }
                } else if (sLine.indexOf(STARTING_DATA_LINE) >= 0) {
                    // header ends until we read the start data mark
                    mFileOpened = true;
                    if (THIS_DEBUG) {
                        System.out.println("read script header success");
                    }    
                    return true;
                }
            }            
        } catch (FileNotFoundException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
        
        if (THIS_DEBUG) {
            System.out.println("Error in reading script header");
        }        
        return false;        
    }    
    
    private void processLine(String s) {
        int index1 = s.indexOf('(');
        int index2 = s.indexOf(')');
        
        if (index1 < 0 || index2 < 0) {
            return;
        }
        
        StringTokenizer st = new StringTokenizer(
                s.substring(index1 + 1, index2), ",");
        
        if (s.indexOf(EVENT_KEYWORD_KEY) >= 0) {
            // key events
            try {
                long downTime = Long.parseLong(st.nextToken());
                long eventTime = Long.parseLong(st.nextToken());
                int action = Integer.parseInt(st.nextToken());
                int code = Integer.parseInt(st.nextToken());
                int repeat = Integer.parseInt(st.nextToken());
                int metaState = Integer.parseInt(st.nextToken());
                int device = Integer.parseInt(st.nextToken());
                int scancode = Integer.parseInt(st.nextToken());
                
                MonkeyKeyEvent e = new MonkeyKeyEvent(downTime, eventTime,
                        action, code, repeat, metaState, device, scancode);
                mQ.addLast(e);
                
            } catch (NumberFormatException e) {
                // something wrong with this line in the script               
            }
        } else if (s.indexOf(EVENT_KEYWORD_POINTER) >= 0 || 
                s.indexOf(EVENT_KEYWORD_TRACKBALL) >= 0) {
            // trackball/pointer event 
            try {                
                long downTime = Long.parseLong(st.nextToken());
                long eventTime = Long.parseLong(st.nextToken());
                int action = Integer.parseInt(st.nextToken());
                float x = Float.parseFloat(st.nextToken());
                float y = Float.parseFloat(st.nextToken());
                float pressure = Float.parseFloat(st.nextToken());
                float size = Float.parseFloat(st.nextToken());
                int metaState = Integer.parseInt(st.nextToken());
                float xPrecision = Float.parseFloat(st.nextToken());
                float yPrecision = Float.parseFloat(st.nextToken());
                int device = Integer.parseInt(st.nextToken());
                int edgeFlags = Integer.parseInt(st.nextToken());
                
                int type = MonkeyEvent.EVENT_TYPE_TRACKBALL;                
                if (s.indexOf("Pointer") > 0) {
                    type = MonkeyEvent.EVENT_TYPE_POINTER;
                }                
                MonkeyMotionEvent e = new MonkeyMotionEvent(type, downTime, eventTime,
                        action, x, y, pressure, size, metaState, xPrecision, yPrecision,
                        device, edgeFlags);
                mQ.addLast(e);                
            } catch (NumberFormatException e) {
                // we ignore this event                
            }
        } else if (s.indexOf(EVENT_KEYWORD_FLIP) >= 0) {
            boolean keyboardOpen = Boolean.parseBoolean(st.nextToken());
            MonkeyFlipEvent e = new MonkeyFlipEvent(keyboardOpen);
            mQ.addLast(e);
        }
    }
    
    private void closeFile() {
        mFileOpened = false;        
        if (THIS_DEBUG) {
            System.out.println("closing script file");
        }
        
        try {
            mFStream.close();
            mInputStream.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    
    /**
     * add home key press/release event to the queue
     */
    private void addHomeKeyEvent() {        
        MonkeyKeyEvent e = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, 
                KeyEvent.KEYCODE_HOME);
        mQ.addLast(e);        
        e = new MonkeyKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HOME);
        mQ.addLast(e);
    }
    
    /**
     * read next batch of events from the provided script file
     * @return true if success
     */
    private boolean readNextBatch() {
        String sLine = null;
        int readCount = 0;
        
        if (THIS_DEBUG) {
            System.out.println("readNextBatch(): reading next batch of events");
        }
        
        if (!mFileOpened) {
            if (!readScriptHeader()) {
                closeFile();
                return false;
            }             
            resetValue();
            
            /* 
            * In order to allow the Monkey to replay captured events multiple times
            * we need to define a default start UI, which is the home screen
            * Otherwise, it won't be accurate since the captured events 
            * could end anywhere
            */
            addHomeKeyEvent();
        }
        
        try {            
            while (readCount++ < MAX_ONE_TIME_READS &&                    
                   (sLine = mBufferReader.readLine()) != null) {
                sLine = sLine.trim();                        
                processLine(sLine);
            }
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
        
        if (sLine == null) {
            // to the end of the file
            if (THIS_DEBUG) {
                System.out.println("readNextBatch(): to the end of file");
            }
            closeFile();
        }        
        return true;
    }
    
    /**
     * sleep for a period of given time, introducing latency among events
     * @param time to sleep in millisecond
     */
    private void needSleep(long time) {        
        if (time < 1) {
            return;    
        }        
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {            
        }        
    }    
    
    /**
     * check whether we can successfully read the header of the script file
     */
    public boolean validate() {
        boolean b = readNextBatch(); 
        if (mVerbose > 0) {
            System.out.println("Replaying " + mEventCountInScript + 
                    " events with speed " + mSpeed);
        }
        return b;        
    }
    
    public void setVerbose(int verbose) {
        mVerbose = verbose;
    }
    
    /**
     * adjust key downtime and eventtime according to both  
     * recorded values and current system time 
     * @param e KeyEvent
     */
    private void adjustKeyEventTime(MonkeyKeyEvent e) {
        if (e.getEventTime() < 0) {
            return;
        }      
        long thisDownTime = 0;
        long thisEventTime = 0;
        long expectedDelay = 0;
        
        if (mLastRecordedEventTime <= 0) {
            // first time event            
            thisDownTime = SystemClock.uptimeMillis();
            thisEventTime = thisDownTime;
        } else {            
            if (e.getDownTime() != mLastRecordedDownTimeKey) {
                thisDownTime = e.getDownTime();
            } else {
                thisDownTime = mLastExportDownTimeKey;
            }            
            expectedDelay = (long) ((e.getEventTime() - 
                    mLastRecordedEventTime) * mSpeed);            
            thisEventTime = mLastExportEventTime + expectedDelay;
            // add sleep to simulate everything in recording
            needSleep(expectedDelay - SLEEP_COMPENSATE_DIFF);
        }        
        mLastRecordedDownTimeKey = e.getDownTime();
        mLastRecordedEventTime = e.getEventTime();         
        e.setDownTime(thisDownTime);
        e.setEventTime(thisEventTime);        
        mLastExportDownTimeKey = thisDownTime;
        mLastExportEventTime =  thisEventTime;
    }
    
    /**
     * adjust motion downtime and eventtime according to both  
     * recorded values and current system time 
     * @param e KeyEvent
     */
    private void adjustMotionEventTime(MonkeyMotionEvent e) {
        if (e.getEventTime() < 0) {
            return;
        }      
        long thisDownTime = 0;
        long thisEventTime = 0;
        long expectedDelay = 0;
        
        if (mLastRecordedEventTime <= 0) {
            // first time event            
            thisDownTime = SystemClock.uptimeMillis();
            thisEventTime = thisDownTime;
        } else {            
            if (e.getDownTime() != mLastRecordedDownTimeMotion) {
                thisDownTime = e.getDownTime();
            } else {
                thisDownTime = mLastExportDownTimeMotion;
            }            
            expectedDelay = (long) ((e.getEventTime() - 
                    mLastRecordedEventTime) * mSpeed);            
            thisEventTime = mLastExportEventTime + expectedDelay;
            // add sleep to simulate everything in recording
            needSleep(expectedDelay - SLEEP_COMPENSATE_DIFF);
        }
        
        mLastRecordedDownTimeMotion = e.getDownTime();
        mLastRecordedEventTime = e.getEventTime();         
        e.setDownTime(thisDownTime);
        e.setEventTime(thisEventTime);        
        mLastExportDownTimeMotion = thisDownTime;
        mLastExportEventTime =  thisEventTime;
    }    
    
    /**
     * if the queue is empty, we generate events first
     * @return the first event in the queue, if null, indicating the system crashes 
     */
    public MonkeyEvent getNextEvent() {
        long recordedEventTime = -1;
        
        if (mQ.isEmpty()) {
            readNextBatch();
        }
        MonkeyEvent e = mQ.getFirst();
        mQ.removeFirst();
        
        if (e.getEventType() == MonkeyEvent.EVENT_TYPE_KEY) {
            adjustKeyEventTime((MonkeyKeyEvent) e);        
        } else if (e.getEventType() == MonkeyEvent.EVENT_TYPE_POINTER || 
                e.getEventType() == MonkeyEvent.EVENT_TYPE_TRACKBALL) {
            adjustMotionEventTime((MonkeyMotionEvent) e);
        }
        return e;
    }
}
