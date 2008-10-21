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

package com.android.ddmuilib.location;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * A very basic GPX parser to meet the need of the emulator control panel.
 * <p/>
 * It parses basic waypoint information, and tracks (merging segments).
 */
public class GpxParser {
    
    private final static String NS_GPX = "http://www.topografix.com/GPX/1/1";  //$NON-NLS-1$
        
    private final static String NODE_WAYPOINT = "wpt"; //$NON-NLS-1$
    private final static String NODE_TRACK = "trk"; //$NON-NLS-1$
    private final static String NODE_TRACK_SEGMENT = "trkseg"; //$NON-NLS-1$
    private final static String NODE_TRACK_POINT = "trkpt"; //$NON-NLS-1$
    private final static String NODE_NAME = "name"; //$NON-NLS-1$
    private final static String NODE_TIME = "time"; //$NON-NLS-1$
    private final static String NODE_ELEVATION = "ele"; //$NON-NLS-1$
    private final static String NODE_DESCRIPTION = "desc"; //$NON-NLS-1$
    private final static String ATTR_LONGITUDE = "lon"; //$NON-NLS-1$
    private final static String ATTR_LATITUDE = "lat"; //$NON-NLS-1$
    
    private static SAXParserFactory sParserFactory;
    
    static {
        sParserFactory = SAXParserFactory.newInstance();
        sParserFactory.setNamespaceAware(true);
    }

    private String mFileName;

    private GpxHandler mHandler;
    
    /** Pattern to parse time with optional sub-second precision, and optional
     * Z indicating the time is in UTC. */
    private final static Pattern ISO8601_TIME =
        Pattern.compile("(\\d{4})-(\\d\\d)-(\\d\\d)T(\\d\\d):(\\d\\d):(\\d\\d)(?:(\\.\\d+))?(Z)?"); //$NON-NLS-1$
    
    /**
     * Handler for the SAX parser.
     */
    private static class GpxHandler extends DefaultHandler {
        // --------- parsed data --------- 
        List<WayPoint> mWayPoints;
        List<Track> mTrackList;
        
        // --------- state for parsing --------- 
        Track mCurrentTrack;
        TrackPoint mCurrentTrackPoint;
        WayPoint mCurrentWayPoint;
        final StringBuilder mStringAccumulator = new StringBuilder();
        
        boolean mSuccess = true;

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes)
                throws SAXException {
            // we only care about the standard GPX nodes.
            try {
                if (NS_GPX.equals(uri)) {
                    if (NODE_WAYPOINT.equals(localName)) {
                        if (mWayPoints == null) {
                            mWayPoints = new ArrayList<WayPoint>();
                        }
                        
                        mWayPoints.add(mCurrentWayPoint = new WayPoint());
                        handleLocation(mCurrentWayPoint, attributes);
                    } else if (NODE_TRACK.equals(localName)) {
                        if (mTrackList == null) {
                            mTrackList = new ArrayList<Track>();
                        }
                        
                        mTrackList.add(mCurrentTrack = new Track());
                    } else if (NODE_TRACK_SEGMENT.equals(localName)) {
                        // for now we do nothing here. This will merge all the segments into
                        // a single TrackPoint list in the Track.
                    } else if (NODE_TRACK_POINT.equals(localName)) {
                        if (mCurrentTrack != null) {
                            mCurrentTrack.addPoint(mCurrentTrackPoint = new TrackPoint());
                            handleLocation(mCurrentTrackPoint, attributes);
                        }
                    }
                }
            } finally {
                // no matter the node, we empty the StringBuilder accumulator when we start
                // a new node.
                mStringAccumulator.setLength(0);
            }
        }

        /**
         * Processes new characters for the node content. The characters are simply stored,
         * and will be processed when {@link #endElement(String, String, String)} is called.
         */
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            mStringAccumulator.append(ch, start, length);
        }
        
        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if (NS_GPX.equals(uri)) {
                if (NODE_WAYPOINT.equals(localName)) {
                    mCurrentWayPoint = null;
                } else if (NODE_TRACK.equals(localName)) {
                    mCurrentTrack = null;
                } else if (NODE_TRACK_POINT.equals(localName)) {
                    mCurrentTrackPoint = null;
                } else if (NODE_NAME.equals(localName)) {
                    if (mCurrentTrack != null) {
                        mCurrentTrack.setName(mStringAccumulator.toString());
                    } else if (mCurrentWayPoint != null) {
                        mCurrentWayPoint.setName(mStringAccumulator.toString());
                    }
                } else if (NODE_TIME.equals(localName)) {
                    if (mCurrentTrackPoint != null) {
                        mCurrentTrackPoint.setTime(computeTime(mStringAccumulator.toString()));
                    }
                } else if (NODE_ELEVATION.equals(localName)) {
                    if (mCurrentTrackPoint != null) {
                        mCurrentTrackPoint.setElevation(
                                Double.parseDouble(mStringAccumulator.toString()));
                    } else if (mCurrentWayPoint != null) {
                        mCurrentWayPoint.setElevation(
                                Double.parseDouble(mStringAccumulator.toString()));
                    }
                } else if (NODE_DESCRIPTION.equals(localName)) {
                    if (mCurrentWayPoint != null) {
                        mCurrentWayPoint.setDescription(mStringAccumulator.toString());
                    }
                }
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            mSuccess = false;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            mSuccess = false;
        }
        
        /**
         * Converts the string description of the time into milliseconds since epoch.
         * @param timeString the string data.
         * @return date in milliseconds.
         */
        private long computeTime(String timeString) {
            // Time looks like: 2008-04-05T19:24:50Z
            Matcher m = ISO8601_TIME.matcher(timeString);
            if (m.matches()) {
                // get the various elements and reconstruct time as a long.
                try {
                    int year = Integer.parseInt(m.group(1));
                    int month = Integer.parseInt(m.group(2));
                    int date = Integer.parseInt(m.group(3));
                    int hourOfDay = Integer.parseInt(m.group(4));
                    int minute = Integer.parseInt(m.group(5));
                    int second = Integer.parseInt(m.group(6));
                    
                    // handle the optional parameters.
                    int milliseconds = 0;

                    String subSecondGroup = m.group(7);
                    if (subSecondGroup != null) {
                        milliseconds = (int)(1000 * Double.parseDouble(subSecondGroup));
                    }
                    
                    boolean utcTime = m.group(8) != null;

                    // now we convert into milliseconds since epoch.
                    Calendar c;
                    if (utcTime) {
                        c = Calendar.getInstance(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
                    } else {
                        c = Calendar.getInstance();
                    }
                    
                    c.set(year, month, date, hourOfDay, minute, second);
                    
                    return c.getTimeInMillis() + milliseconds;
                } catch (NumberFormatException e) {
                    // format is invalid, we'll return -1 below.
                }
                
            }

            // invalid time!
            return -1;
        }
        
        /**
         * Handles the location attributes and store them into a {@link LocationPoint}.
         * @param locationNode the {@link LocationPoint} to receive the location data.
         * @param attributes the attributes from the XML node.
         */
        private void handleLocation(LocationPoint locationNode, Attributes attributes) {
            try {
                double longitude = Double.parseDouble(attributes.getValue(ATTR_LONGITUDE));
                double latitude = Double.parseDouble(attributes.getValue(ATTR_LATITUDE));
                
                locationNode.setLocation(longitude, latitude);
            } catch (NumberFormatException e) {
                // wrong data, do nothing.
            }
        }

        WayPoint[] getWayPoints() {
            if (mWayPoints != null) {
                return mWayPoints.toArray(new WayPoint[mWayPoints.size()]);
            }

            return null;
        }

        Track[] getTracks() {
            if (mTrackList != null) {
                return mTrackList.toArray(new Track[mTrackList.size()]);
            }

            return null;
        }
        
        boolean getSuccess() {
            return mSuccess;
        }
    }

    /**
     * A GPS track.
     * <p/>A track is composed of a list of {@link TrackPoint} and optional name and comment.
     */
    public final static class Track {
        private String mName;
        private String mComment;
        private List<TrackPoint> mPoints = new ArrayList<TrackPoint>();

        void setName(String name) {
            mName = name;
        }
        
        public String getName() {
            return mName;
        }
        
        void setComment(String comment) {
            mComment = comment;
        }
        
        public String getComment() {
            return mComment;
        }
        
        void addPoint(TrackPoint trackPoint) {
            mPoints.add(trackPoint);
        }
        
        public TrackPoint[] getPoints() {
            return mPoints.toArray(new TrackPoint[mPoints.size()]);
        }
        
        public long getFirstPointTime() {
            if (mPoints.size() > 0) {
                return mPoints.get(0).getTime();
            }
            
            return -1;
        }

        public long getLastPointTime() {
            if (mPoints.size() > 0) {
                return mPoints.get(mPoints.size()-1).getTime();
            }
            
            return -1;
        }
        
        public int getPointCount() {
            return mPoints.size();
        }
    }
    
    /**
     * Creates a new GPX parser for a file specified by its full path.
     * @param fileName The full path of the GPX file to parse.
     */
    public GpxParser(String fileName) {
        mFileName = fileName;
    }

    /**
     * Parses the GPX file.
     * @return <code>true</code> if success.
     */
    public boolean parse() {
        try {
            SAXParser parser = sParserFactory.newSAXParser();

            mHandler = new GpxHandler();

            parser.parse(new InputSource(new FileReader(mFileName)), mHandler);
            
            return mHandler.getSuccess();
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        } catch (IOException e) {
        } finally {
        }

        return false;
    }
    
    /**
     * Returns the parsed {@link WayPoint} objects, or <code>null</code> if none were found (or
     * if the parsing failed.
     */
    public WayPoint[] getWayPoints() {
        if (mHandler != null) {
            return mHandler.getWayPoints();
        }
        
        return null;
    }
    
    /**
     * Returns the parsed {@link Track} objects, or <code>null</code> if none were found (or
     * if the parsing failed.
     */
    public Track[] getTracks() {
        if (mHandler != null) {
            return mHandler.getTracks();
        }
        
        return null;
    }
}
