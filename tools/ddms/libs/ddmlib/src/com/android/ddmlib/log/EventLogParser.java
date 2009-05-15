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

package com.android.ddmlib.log;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.log.EventContainer.EventValueType;
import com.android.ddmlib.log.EventValueDescription.ValueType;
import com.android.ddmlib.log.LogReceiver.LogEntry;
import com.android.ddmlib.utils.ArrayHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the "event" log.
 */
public final class EventLogParser {

    /** Location of the tag map file on the device */
    private final static String EVENT_TAG_MAP_FILE = "/system/etc/event-log-tags"; //$NON-NLS-1$

    /**
     * Event log entry types.  These must match up with the declarations in
     * java/android/android/util/EventLog.java.
     */
    private final static int EVENT_TYPE_INT      = 0;
    private final static int EVENT_TYPE_LONG     = 1;
    private final static int EVENT_TYPE_STRING   = 2;
    private final static int EVENT_TYPE_LIST     = 3;

    private final static Pattern PATTERN_SIMPLE_TAG = Pattern.compile(
    "^(\\d+)\\s+([A-Za-z0-9_]+)\\s*$"); //$NON-NLS-1$
    private final static Pattern PATTERN_TAG_WITH_DESC = Pattern.compile(
            "^(\\d+)\\s+([A-Za-z0-9_]+)\\s*(.*)\\s*$"); //$NON-NLS-1$
    private final static Pattern PATTERN_DESCRIPTION = Pattern.compile(
            "\\(([A-Za-z0-9_\\s]+)\\|(\\d+)(\\|\\d+){0,1}\\)"); //$NON-NLS-1$

    private final static Pattern TEXT_LOG_LINE = Pattern.compile(
            "(\\d\\d)-(\\d\\d)\\s(\\d\\d):(\\d\\d):(\\d\\d).(\\d{3})\\s+I/([a-zA-Z0-9_]+)\\s*\\(\\s*(\\d+)\\):\\s+(.*)"); //$NON-NLS-1$

    private final TreeMap<Integer, String> mTagMap = new TreeMap<Integer, String>();

    private final TreeMap<Integer, EventValueDescription[]> mValueDescriptionMap =
        new TreeMap<Integer, EventValueDescription[]>();

    public EventLogParser() {
    }

    /**
     * Inits the parser for a specific Device.
     * <p/>
     * This methods reads the event-log-tags located on the device to find out
     * what tags are being written to the event log and what their format is.
     * @param device The device.
     * @return <code>true</code> if success, <code>false</code> if failure or cancellation.
     */
    public boolean init(IDevice device) {
        // read the event tag map file on the device.
        try {
            device.executeShellCommand("cat " + EVENT_TAG_MAP_FILE, //$NON-NLS-1$
                    new MultiLineReceiver() {
                @Override
                public void processNewLines(String[] lines) {
                    for (String line : lines) {
                        processTagLine(line);
                    }
                }
                public boolean isCancelled() {
                    return false;
                }
            });
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Inits the parser with the content of a tag file.
     * @param tagFileContent the lines of a tag file.
     * @return <code>true</code> if success, <code>false</code> if failure.
     */
    public boolean init(String[] tagFileContent) {
        for (String line : tagFileContent) {
            processTagLine(line);
        }
        return true;
    }

    /**
     * Inits the parser with a specified event-log-tags file.
     * @param filePath
     * @return <code>true</code> if success, <code>false</code> if failure.
     */
    public boolean init(String filePath)  {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));

            String line = null;
            do {
                line = reader.readLine();
                if (line != null) {
                    processTagLine(line);
                }
            } while (line != null);

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Processes a line from the event-log-tags file.
     * @param line the line to process
     */
    private void processTagLine(String line) {
        // ignore empty lines and comment lines
        if (line.length() > 0 && line.charAt(0) != '#') {
            Matcher m = PATTERN_TAG_WITH_DESC.matcher(line);
            if (m.matches()) {
                try {
                    int value = Integer.parseInt(m.group(1));
                    String name = m.group(2);
                    if (name != null && mTagMap.get(value) == null) {
                        mTagMap.put(value, name);
                    }

                    // special case for the GC tag. We ignore what is in the file,
                    // and take what the custom GcEventContainer class tells us.
                    // This is due to the event encoding several values on 2 longs.
                    // @see GcEventContainer
                    if (value == GcEventContainer.GC_EVENT_TAG) {
                        mValueDescriptionMap.put(value,
                            GcEventContainer.getValueDescriptions());
                    } else {

                        String description = m.group(3);
                        if (description != null && description.length() > 0) {
                            EventValueDescription[] desc =
                                processDescription(description);

                            if (desc != null) {
                                mValueDescriptionMap.put(value, desc);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // failed to convert the number into a string. just ignore it.
                }
            } else {
                m = PATTERN_SIMPLE_TAG.matcher(line);
                if (m.matches()) {
                    int value = Integer.parseInt(m.group(1));
                    String name = m.group(2);
                    if (name != null && mTagMap.get(value) == null) {
                        mTagMap.put(value, name);
                    }
                }
            }
        }
    }

    private EventValueDescription[] processDescription(String description) {
        String[] descriptions = description.split("\\s*,\\s*"); //$NON-NLS-1$

        ArrayList<EventValueDescription> list = new ArrayList<EventValueDescription>();

        for (String desc : descriptions) {
            Matcher m = PATTERN_DESCRIPTION.matcher(desc);
            if (m.matches()) {
                try {
                    String name = m.group(1);

                    String typeString = m.group(2);
                    int typeValue = Integer.parseInt(typeString);
                    EventValueType eventValueType = EventValueType.getEventValueType(typeValue);
                    if (eventValueType == null) {
                        // just ignore this description if the value is not recognized.
                        // TODO: log the error.
                    }

                    typeString = m.group(3);
                    if (typeString != null && typeString.length() > 0) {
                        //skip the |
                        typeString = typeString.substring(1);

                        typeValue = Integer.parseInt(typeString);
                        ValueType valueType = ValueType.getValueType(typeValue);

                        list.add(new EventValueDescription(name, eventValueType, valueType));
                    } else {
                        list.add(new EventValueDescription(name, eventValueType));
                    }
                } catch (NumberFormatException nfe) {
                    // just ignore this description if one number is malformed.
                    // TODO: log the error.
                } catch (InvalidValueTypeException e) {
                    // just ignore this description if data type and data unit don't match
                    // TODO: log the error.
                }
            } else {
                Log.e("EventLogParser",  //$NON-NLS-1$
                    String.format("Can't parse %1$s", description));  //$NON-NLS-1$
            }
        }

        if (list.size() == 0) {
            return null;
        }

        return list.toArray(new EventValueDescription[list.size()]);

    }

    public EventContainer parse(LogEntry entry) {
        if (entry.len < 4) {
            return null;
        }

        int inOffset = 0;

        int tagValue = ArrayHelper.swap32bitFromArray(entry.data, inOffset);
        inOffset += 4;

        String tag = mTagMap.get(tagValue);
        if (tag == null) {
            Log.e("EventLogParser", String.format("unknown tag number: %1$d", tagValue));
        }

        ArrayList<Object> list = new ArrayList<Object>();
        if (parseBinaryEvent(entry.data, inOffset, list) == -1) {
            return null;
        }

        Object data;
        if (list.size() == 1) {
            data = list.get(0);
        } else{
            data = list.toArray();
        }

        EventContainer event = null;
        if (tagValue == GcEventContainer.GC_EVENT_TAG) {
            event = new GcEventContainer(entry, tagValue, data);
        } else {
            event = new EventContainer(entry, tagValue, data);
        }

        return event;
    }

    public EventContainer parse(String textLogLine) {
        // line will look like
        // 04-29 23:16:16.691 I/dvm_gc_info(  427): <data>
        // where <data> is either
        // [value1,value2...]
        // or
        // value
        if (textLogLine.length() == 0) {
            return null;
        }

        // parse the header first
        Matcher m = TEXT_LOG_LINE.matcher(textLogLine);
        if (m.matches()) {
            try {
                int month = Integer.parseInt(m.group(1));
                int day = Integer.parseInt(m.group(2));
                int hours = Integer.parseInt(m.group(3));
                int minutes = Integer.parseInt(m.group(4));
                int seconds = Integer.parseInt(m.group(5));
                int milliseconds = Integer.parseInt(m.group(6));

                // convert into seconds since epoch and nano-seconds.
                Calendar cal = Calendar.getInstance();
                cal.set(cal.get(Calendar.YEAR), month-1, day, hours, minutes, seconds);
                int sec = (int)Math.floor(cal.getTimeInMillis()/1000);
                int nsec = milliseconds * 1000000;

                String tag = m.group(7);

                // get the numerical tag value
                int tagValue = -1;
                Set<Entry<Integer, String>> tagSet = mTagMap.entrySet();
                for (Entry<Integer, String> entry : tagSet) {
                    if (tag.equals(entry.getValue())) {
                        tagValue = entry.getKey();
                        break;
                    }
                }

                if (tagValue == -1) {
                    return null;
                }

                int pid = Integer.parseInt(m.group(8));

                Object data = parseTextData(m.group(9), tagValue);
                if (data == null) {
                    return null;
                }

                // now we can allocate and return the EventContainer
                EventContainer event = null;
                if (tagValue == GcEventContainer.GC_EVENT_TAG) {
                    event = new GcEventContainer(tagValue, pid, -1 /* tid */, sec, nsec, data);
                } else {
                    event = new EventContainer(tagValue, pid, -1 /* tid */, sec, nsec, data);
                }

                return event;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    public Map<Integer, String> getTagMap() {
        return mTagMap;
    }

    public Map<Integer, EventValueDescription[]> getEventInfoMap() {
        return mValueDescriptionMap;
    }

    /**
     * Recursively convert binary log data to printable form.
     *
     * This needs to be recursive because you can have lists of lists.
     *
     * If we run out of room, we stop processing immediately.  It's important
     * for us to check for space on every output element to avoid producing
     * garbled output.
     *
     * Returns the amount read on success, -1 on failure.
     */
    private static int parseBinaryEvent(byte[] eventData, int dataOffset, ArrayList<Object> list) {

        if (eventData.length - dataOffset < 1)
            return -1;

        int offset = dataOffset;

        int type = eventData[offset++];

        //fprintf(stderr, "--- type=%d (rem len=%d)\n", type, eventDataLen);

        switch (type) {
        case EVENT_TYPE_INT: { /* 32-bit signed int */
                int ival;

                if (eventData.length - offset < 4)
                    return -1;
                ival = ArrayHelper.swap32bitFromArray(eventData, offset);
                offset += 4;

                list.add(new Integer(ival));
            }
            break;
        case EVENT_TYPE_LONG: { /* 64-bit signed long */
                long lval;

                if (eventData.length - offset < 8)
                    return -1;
                lval = ArrayHelper.swap64bitFromArray(eventData, offset);
                offset += 8;

                list.add(new Long(lval));
            }
            break;
        case EVENT_TYPE_STRING: { /* UTF-8 chars, not NULL-terminated */
                int strLen;

                if (eventData.length - offset < 4)
                    return -1;
                strLen = ArrayHelper.swap32bitFromArray(eventData, offset);
                offset += 4;

                if (eventData.length - offset < strLen)
                    return -1;

                // get the string
                try {
                    String str = new String(eventData, offset, strLen, "UTF-8"); //$NON-NLS-1$
                    list.add(str);
                } catch (UnsupportedEncodingException e) {
                }
                offset += strLen;
                break;
            }
        case EVENT_TYPE_LIST: { /* N items, all different types */

                if (eventData.length - offset < 1)
                    return -1;

                int count = eventData[offset++];

                // make a new temp list
                ArrayList<Object> subList = new ArrayList<Object>();
                for (int i = 0; i < count; i++) {
                    int result = parseBinaryEvent(eventData, offset, subList);
                    if (result == -1) {
                        return result;
                    }

                    offset += result;
                }

                list.add(subList.toArray());
            }
            break;
        default:
            Log.e("EventLogParser",  //$NON-NLS-1$
                    String.format("Unknown binary event type %1$d", type));  //$NON-NLS-1$
            return -1;
        }

        return offset - dataOffset;
    }

    private Object parseTextData(String data, int tagValue) {
        // first, get the description of what we're supposed to parse
        EventValueDescription[] desc = mValueDescriptionMap.get(tagValue);

        if (desc == null) {
            // TODO parse and create string values.
            return null;
        }

        if (desc.length == 1) {
            return getObjectFromString(data, desc[0].getEventValueType());
        } else if (data.startsWith("[") && data.endsWith("]")) {
            data = data.substring(1, data.length() - 1);

            // get each individual values as String
            String[] values = data.split(",");

            if (tagValue == GcEventContainer.GC_EVENT_TAG) {
                // special case for the GC event!
                Object[] objects = new Object[2];

                objects[0] = getObjectFromString(values[0], EventValueType.LONG);
                objects[1] = getObjectFromString(values[1], EventValueType.LONG);

                return objects;
            } else {
                // must be the same number as the number of descriptors.
                if (values.length != desc.length) {
                    return null;
                }

                Object[] objects = new Object[values.length];

                for (int i = 0 ; i < desc.length ; i++) {
                    Object obj = getObjectFromString(values[i], desc[i].getEventValueType());
                    if (obj == null) {
                        return null;
                    }
                    objects[i] = obj;
                }

                return objects;
            }
        }

        return null;
    }


    private Object getObjectFromString(String value, EventValueType type) {
        try {
            switch (type) {
                case INT:
                    return Integer.valueOf(value);
                case LONG:
                    return Long.valueOf(value);
                case STRING:
                    return value;
            }
        } catch (NumberFormatException e) {
            // do nothing, we'll return null.
        }

        return null;
    }

    /**
     * Recreates the event-log-tags at the specified file path.
     * @param filePath the file path to write the file.
     * @throws IOException
     */
    public void saveTags(String filePath) throws IOException {
        File destFile = new File(filePath);
        destFile.createNewFile();
        FileOutputStream fos = null;

        try {

            fos = new FileOutputStream(destFile);

            for (Integer key : mTagMap.keySet()) {
                // get the tag name
                String tagName = mTagMap.get(key);

                // get the value descriptions
                EventValueDescription[] descriptors = mValueDescriptionMap.get(key);

                String line = null;
                if (descriptors != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("%1$d %2$s", key, tagName)); //$NON-NLS-1$
                    boolean first = true;
                    for (EventValueDescription evd : descriptors) {
                        if (first) {
                            sb.append(" ("); //$NON-NLS-1$
                            first = false;
                        } else {
                            sb.append(",("); //$NON-NLS-1$
                        }
                        sb.append(evd.getName());
                        sb.append("|"); //$NON-NLS-1$
                        sb.append(evd.getEventValueType().getValue());
                        sb.append("|"); //$NON-NLS-1$
                        sb.append(evd.getValueType().getValue());
                        sb.append("|)"); //$NON-NLS-1$
                    }
                    sb.append("\n"); //$NON-NLS-1$

                    line = sb.toString();
                } else {
                    line = String.format("%1$d %2$s\n", key, tagName); //$NON-NLS-1$
                }

                byte[] buffer = line.getBytes();
                fos.write(buffer);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }


}
