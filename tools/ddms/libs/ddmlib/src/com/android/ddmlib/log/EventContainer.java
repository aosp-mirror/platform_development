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

import com.android.ddmlib.log.LogReceiver.LogEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an event and its data.
 */
public class EventContainer {
    
    /**
     * Comparison method for {@link EventContainer#testValue(int, Object, com.android.ddmlib.log.EventContainer.CompareMethod)}
     *
     */
    public enum CompareMethod {
        EQUAL_TO("equals", "=="),
        LESSER_THAN("less than or equals to", "<="),
        LESSER_THAN_STRICT("less than", "<"),
        GREATER_THAN("greater than or equals to", ">="),
        GREATER_THAN_STRICT("greater than", ">"),
        BIT_CHECK("bit check", "&");
        
        private final String mName;
        private final String mTestString;

        private CompareMethod(String name, String testString) {
            mName = name;
            mTestString = testString;
        }

        /**
         * Returns the display string.
         */
        @Override
        public String toString() {
            return mName;
        }

        /**
         * Returns a short string representing the comparison.
         */
        public String testString() {
            return mTestString;
        }
    }

    
    /**
     * Type for event data.
     */
    public static enum EventValueType {
        UNKNOWN(0),
        INT(1),
        LONG(2),
        STRING(3),
        LIST(4),
        TREE(5);
        
        private final static Pattern STORAGE_PATTERN = Pattern.compile("^(\\d+)@(.*)$"); //$NON-NLS-1$
        
        private int mValue;
        
        /**
         * Returns a {@link EventValueType} from an integer value, or <code>null</code> if no match
         * was found.
         * @param value the integer value.
         */
        static EventValueType getEventValueType(int value) {
            for (EventValueType type : values()) {
                if (type.mValue == value) {
                    return type;
                }
            }
            
            return null;
        }

        /**
         * Returns a storage string for an {@link Object} of type supported by
         * {@link EventValueType}.
         * <p/>
         * Strings created by this method can be reloaded with
         * {@link #getObjectFromStorageString(String)}.
         * <p/>
         * NOTE: for now, only {@link #STRING}, {@link #INT}, and {@link #LONG} are supported.
         * @param object the object to "convert" into a storage string.
         * @return a string storing the object and its type or null if the type was not recognized.
         */
        public static String getStorageString(Object object) {
            if (object instanceof String) {
                return STRING.mValue + "@" + (String)object; //$NON-NLS-1$ 
            } else if (object instanceof Integer) {
                return INT.mValue + "@" + object.toString(); //$NON-NLS-1$ 
            } else if (object instanceof Long) {
                return LONG.mValue + "@" + object.toString(); //$NON-NLS-1$ 
            }
            
            return null;
        }
        
        /**
         * Creates an {@link Object} from a storage string created with
         * {@link #getStorageString(Object)}.
         * @param value the storage string
         * @return an {@link Object} or null if the string or type were not recognized.
         */
        public static Object getObjectFromStorageString(String value) {
            Matcher m = STORAGE_PATTERN.matcher(value);
            if (m.matches()) {
                try {
                    EventValueType type = getEventValueType(Integer.parseInt(m.group(1)));

                    if (type == null) {
                        return null;
                    }
                    
                    switch (type) {
                        case STRING:
                            return m.group(2);
                        case INT:
                            return Integer.valueOf(m.group(2));
                        case LONG:
                            return Long.valueOf(m.group(2));
                    }
                } catch (NumberFormatException nfe) {
                    return null;
                }
            }
            
            return null;
        }
        
        
        /**
         * Returns the integer value of the enum.
         */
        public int getValue() {
            return mValue;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        private EventValueType(int value) {
            mValue = value;
        }
    }

    public int mTag;
    public int pid;    /* generating process's pid */
    public int tid;    /* generating process's tid */
    public int sec;    /* seconds since Epoch */
    public int nsec;   /* nanoseconds */

    private Object mData; 

    /**
     * Creates an {@link EventContainer} from a {@link LogEntry}.
     * @param entry  the LogEntry from which pid, tid, and time info is copied.
     * @param tag the event tag value
     * @param data the data of the EventContainer.
     */
    EventContainer(LogEntry entry, int tag, Object data) {
        getType(data);
        mTag = tag;
        mData = data;

        pid = entry.pid;
        tid = entry.tid;
        sec = entry.sec;
        nsec = entry.nsec;
    }
    
    /**
     * Creates an {@link EventContainer} with raw data
     */
    EventContainer(int tag, int pid, int tid, int sec, int nsec, Object data) {
        getType(data);
        mTag = tag;
        mData = data;
        
        this.pid = pid;
        this.tid = tid;
        this.sec = sec;
        this.nsec = nsec;
    }

    /**
     * Returns the data as an int.
     * @throws InvalidTypeException if the data type is not {@link EventValueType#INT}.
     * @see #getType()
     */
    public final Integer getInt() throws InvalidTypeException {
        if (getType(mData) == EventValueType.INT) {
            return (Integer)mData;
        }

        throw new InvalidTypeException();
    }
    
    /**
     * Returns the data as a long.
     * @throws InvalidTypeException if the data type is not {@link EventValueType#LONG}. 
     * @see #getType()
     */
    public final Long getLong() throws InvalidTypeException {
        if (getType(mData) == EventValueType.LONG) {
            return (Long)mData;
        }

        throw new InvalidTypeException();
    }

    /**
     * Returns the data as a String.
     * @throws InvalidTypeException if the data type is not {@link EventValueType#STRING}.
     * @see #getType()
     */
    public final String getString() throws InvalidTypeException {
        if (getType(mData) == EventValueType.STRING) {
            return (String)mData;
        }

        throw new InvalidTypeException();
    }
    
    /**
     * Returns a value by index. The return type is defined by its type.
     * @param valueIndex the index of the value. If the data is not a list, this is ignored.
     */
    public Object getValue(int valueIndex) {
        return getValue(mData, valueIndex, true);
    }

    /**
     * Returns a value by index as a double.
     * @param valueIndex the index of the value. If the data is not a list, this is ignored.
     * @throws InvalidTypeException if the data type is not {@link EventValueType#INT},
     * {@link EventValueType#LONG}, {@link EventValueType#LIST}, or if the item in the
     * list at index <code>valueIndex</code> is not of type {@link EventValueType#INT} or
     * {@link EventValueType#LONG}.
     * @see #getType()
     */
    public double getValueAsDouble(int valueIndex) throws InvalidTypeException {
        return getValueAsDouble(mData, valueIndex, true);
    }

    /**
     * Returns a value by index as a String.
     * @param valueIndex the index of the value. If the data is not a list, this is ignored.
     * @throws InvalidTypeException if the data type is not {@link EventValueType#INT},
     * {@link EventValueType#LONG}, {@link EventValueType#STRING}, {@link EventValueType#LIST},
     * or if the item in the list at index <code>valueIndex</code> is not of type
     * {@link EventValueType#INT}, {@link EventValueType#LONG}, or {@link EventValueType#STRING}
     * @see #getType()
     */
    public String getValueAsString(int valueIndex) throws InvalidTypeException {
        return getValueAsString(mData, valueIndex, true);
    }
    
    /**
     * Returns the type of the data.
     */
    public EventValueType getType() {
        return getType(mData);
    }

    /**
     * Returns the type of an object.
     */
    public final EventValueType getType(Object data) {
        if (data instanceof Integer) {
            return EventValueType.INT;
        } else if (data instanceof Long) {
            return EventValueType.LONG;
        } else if (data instanceof String) {
            return EventValueType.STRING;
        } else if (data instanceof Object[]) {
            // loop through the list to see if we have another list
            Object[] objects = (Object[])data;
            for (Object obj : objects) {
                EventValueType type = getType(obj);
                if (type == EventValueType.LIST || type == EventValueType.TREE) {
                    return EventValueType.TREE;
                }
            }
            return EventValueType.LIST;
        }

        return EventValueType.UNKNOWN;
    }
    
    /**
     * Checks that the <code>index</code>-th value of this event against a provided value.
     * @param index the index of the value to test
     * @param value the value to test against
     * @param compareMethod the method of testing
     * @return true if the test passed.
     * @throws InvalidTypeException in case of type mismatch between the value to test and the value
     * to test against, or if the compare method is incompatible with the type of the values.
     * @see CompareMethod
     */
    public boolean testValue(int index, Object value,
            CompareMethod compareMethod) throws InvalidTypeException {
        EventValueType type = getType(mData);
        if (index > 0 && type != EventValueType.LIST) {
            throw new InvalidTypeException();
        }
        
        Object data = mData;
        if (type == EventValueType.LIST) {
            data = ((Object[])mData)[index];
        }

        if (data.getClass().equals(data.getClass()) == false) {
            throw new InvalidTypeException();
        }

        switch (compareMethod) {
            case EQUAL_TO:
                return data.equals(value);
            case LESSER_THAN:
                if (data instanceof Integer) {
                    return (((Integer)data).compareTo((Integer)value) <= 0);
                } else if (data instanceof Long) {
                    return (((Long)data).compareTo((Long)value) <= 0);
                }

                // other types can't use this compare method.
                throw new InvalidTypeException();
            case LESSER_THAN_STRICT:
                if (data instanceof Integer) {
                    return (((Integer)data).compareTo((Integer)value) < 0);
                } else if (data instanceof Long) {
                    return (((Long)data).compareTo((Long)value) < 0);
                }

                // other types can't use this compare method.
                throw new InvalidTypeException();
            case GREATER_THAN:
                if (data instanceof Integer) {
                    return (((Integer)data).compareTo((Integer)value) >= 0);
                } else if (data instanceof Long) {
                    return (((Long)data).compareTo((Long)value) >= 0);
                }

                // other types can't use this compare method.
                throw new InvalidTypeException();
            case GREATER_THAN_STRICT:
                if (data instanceof Integer) {
                    return (((Integer)data).compareTo((Integer)value) > 0);
                } else if (data instanceof Long) {
                    return (((Long)data).compareTo((Long)value) > 0);
                }

                // other types can't use this compare method.
                throw new InvalidTypeException();
            case BIT_CHECK:
                if (data instanceof Integer) {
                    return (((Integer)data).intValue() & ((Integer)value).intValue()) != 0;
                } else if (data instanceof Long) {
                    return (((Long)data).longValue() & ((Long)value).longValue()) != 0;
                }

                // other types can't use this compare method.
                throw new InvalidTypeException();
            default :
                throw new InvalidTypeException();
        }
    }
    
    private final Object getValue(Object data, int valueIndex, boolean recursive) {
        EventValueType type = getType(data);
        
        switch (type) {
            case INT:
            case LONG:
            case STRING:
                return data;
            case LIST:
                if (recursive) {
                    Object[] list = (Object[]) data;
                    if (valueIndex >= 0 && valueIndex < list.length) {
                        return getValue(list[valueIndex], valueIndex, false);
                    }
                }
        }
        
        return null;
    }

    private final double getValueAsDouble(Object data, int valueIndex, boolean recursive)
            throws InvalidTypeException {
        EventValueType type = getType(data);
        
        switch (type) {
            case INT:
                return ((Integer)data).doubleValue();
            case LONG:
                return ((Long)data).doubleValue();
            case STRING:
                throw new InvalidTypeException();
            case LIST:
                if (recursive) {
                    Object[] list = (Object[]) data;
                    if (valueIndex >= 0 && valueIndex < list.length) {
                        return getValueAsDouble(list[valueIndex], valueIndex, false);
                    }
                }
        }
        
        throw new InvalidTypeException();
    }

    private final String getValueAsString(Object data, int valueIndex, boolean recursive)
            throws InvalidTypeException {
        EventValueType type = getType(data);
        
        switch (type) {
            case INT:
                return ((Integer)data).toString();
            case LONG:
                return ((Long)data).toString();
            case STRING:
                return (String)data;
            case LIST:
                if (recursive) {
                    Object[] list = (Object[]) data;
                    if (valueIndex >= 0 && valueIndex < list.length) {
                        return getValueAsString(list[valueIndex], valueIndex, false);
                    }
                } else {
                    throw new InvalidTypeException(
                            "getValueAsString() doesn't support EventValueType.TREE");
                }
        }

        throw new InvalidTypeException(
                "getValueAsString() unsupported type:" + type);
    }
}
