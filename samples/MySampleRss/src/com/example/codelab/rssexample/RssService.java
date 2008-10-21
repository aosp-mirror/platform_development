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

package com.example.codelab.rssexample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Bundle;
import android.database.Cursor;
import android.content.ContentResolver;
import android.os.Handler;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.StringBuilder;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;
    
public class RssService extends Service implements Runnable{
    private Logger mLogger = Logger.getLogger(this.getPackageName());
    public static final String REQUERY_KEY = "Requery_All"; // Sent to tell us force a requery.
    public static final String RSS_URL = "RSS_URL"; // Sent to tell us to requery a specific item.
    private NotificationManager mNM;
    private Cursor mCur;                        // RSS content provider cursor.
    private GregorianCalendar mLastCheckedTime; // Time we last checked our feeds.
    private final String LAST_CHECKED_PREFERENCE = "last_checked";
    static final int UPDATE_FREQUENCY_IN_MINUTES = 60;
    private Handler mHandler;           // Handler to trap our update reminders.
    private final int NOTIFY_ID = 1;    // Identifies our service icon in the icon tray.
    
    @Override
    protected void onCreate(){
        // Display an icon to show that the service is running.
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(Intent.ACTION_MAIN);
        clickIntent.setClassName(MyRssReader5.class.getName());
        Notification note = new Notification(this, R.drawable.rss_icon, "RSS Service",
                clickIntent, null);
        mNM.notify(NOTIFY_ID, note);
        mHandler = new Handler();

        // Create the intent that will be launched if the user clicks the 
        // icon on the status bar. This will launch our RSS Reader app.
        Intent intent = new Intent(MyRssReader.class);
        
        // Get a cursor over the RSS items.
        ContentResolver rslv = getContentResolver();
        mCur = rslv.query(RssContentProvider.CONTENT_URI, null, null, null, null);
        
        // Load last updated value.
        // We store last updated value in preferences.
        SharedPreferences pref = getSharedPreferences("", 0);
        mLastCheckedTime = new GregorianCalendar();
        mLastCheckedTime.setTimeInMillis(pref.getLong(LAST_CHECKED_PREFERENCE, 0));

//BEGIN_INCLUDE(5_1)
        // Need to run ourselves on a new thread, because 
        // we will be making resource-intensive HTTP calls.
        // Our run() method will check whether we need to requery
        // our sources.
        Thread thr = new Thread(null, this, "rss_service_thread");
        thr.start();
//END_INCLUDE(5_1)        
        mLogger.info("RssService created");
    }
    
//BEGIN_INCLUDE(5_3)
    // A cheap way to pass a message to tell the service to requery.
    @Override
    protected void onStart(Intent intent, int startId){
        super.onStart(startId, arguments);
        Bundle arguments = intent.getExtras();
        if(arguments != null) {
            if(arguments.containsKey(REQUERY_KEY)) {
                queryRssItems();
            }
            if(arguments.containsKey(RSS_URL)) {
                // Typically called after adding a new RSS feed to the list.
                queryItem(arguments.getString(RSS_URL));
            }
        }    
    }
//END_INCLUDE(5_3)
    
    // When the service is destroyed, get rid of our persistent icon.
    @Override
    protected void onDestroy(){
      mNM.cancel(NOTIFY_ID);
    }
    
    // Determines whether the next scheduled check time has passed.
    // Loads this value from a stored preference. If it has (or if no
    // previous value has been stored), it will requery all RSS feeds;
    // otherwise, it will post a delayed reminder to check again after
    // now - next_check_time milliseconds.
    public void queryIfPeriodicRefreshRequired() {
        GregorianCalendar nextCheckTime = new GregorianCalendar();
        nextCheckTime = (GregorianCalendar) mLastCheckedTime.clone();
        nextCheckTime.add(GregorianCalendar.MINUTE, UPDATE_FREQUENCY_IN_MINUTES);
        mLogger.info("last checked time:" + mLastCheckedTime.toString() + "  Next checked time: " + nextCheckTime.toString());
        
        if(mLastCheckedTime.before(nextCheckTime)) {
            queryRssItems();
        } else {
            // Post a message to query again when we get to the next check time.
            long timeTillNextUpdate = mLastCheckedTime.getTimeInMillis() - GregorianCalendar.getInstance().getTimeInMillis();
            mHandler.postDelayed(this, 1000 * 60 * UPDATE_FREQUENCY_IN_MINUTES);
        }
          
    }

    // Query all feeds. If the new feed has a newer pubDate than the previous,
    // then update it.
    void queryRssItems(){
        mLogger.info("Querying Rss feeds...");
 
        // The cursor might have gone stale. Requery to be sure.
        // We need to call next() after a requery to get to the 
        // first record.
        mCur.requery();
        while (mCur.next()){
             // Get the URL for the feed from the cursor.
             int urlColumnIndex = mCur.getColumnIndex(RssContentProvider.URL);
             String url = mCur.getString(urlColumnIndex);
             queryItem(url);
        }
        // Reset the global "last checked" time
        mLastCheckedTime.setTimeInMillis(System.currentTimeMillis());
      
        // Post a message to query again in [update_frequency] minutes
        mHandler.postDelayed(this, 1000 * 60 * UPDATE_FREQUENCY_IN_MINUTES);
    }
    
    
    // Query an individual RSS feed. Returns true if successful, false otherwise.
    private boolean queryItem(String url) {
        try {
            URL wrappedUrl = new URL(url);
            String rssFeed = readRss(wrappedUrl);
            mLogger.info("RSS Feed " + url + ":\n " + rssFeed);
            if(TextUtils.isEmpty(rssFeed)) {
                return false;
            }
              
            // Parse out the feed update date, and compare to the current version.
            // If feed update time is newer, or zero (if never updated, for new 
            // items), then update the content, date, and hasBeenRead fields.
            // lastUpdated = <rss><channel><pubDate>value</pubDate></channel></rss>.
            // If that value doesn't exist, the current date is used.
            GregorianCalendar feedPubDate = parseRssDocPubDate(rssFeed);
            GregorianCalendar lastUpdated = new GregorianCalendar();
            int lastUpdatedColumnIndex = mCur.getColumnIndex(RssContentProvider.LAST_UPDATED);
            lastUpdated.setTimeInMillis(mCur.getLong(lastUpdatedColumnIndex));
            if(lastUpdated.getTimeInMillis() == 0 ||
                lastUpdated.before(feedPubDate) && !TextUtils.isEmpty(rssFeed)) {
                // Get column indices.
                int contentColumnIndex = mCur.getColumnIndex(RssContentProvider.CONTENT);
                int updatedColumnIndex = mCur.getColumnIndex(RssContentProvider.HAS_BEEN_READ);
                 
                // Update values.
                mCur.updateString(contentColumnIndex, rssFeed);
                mCur.updateLong(lastUpdatedColumnIndex, feedPubDate.getTimeInMillis());
                mCur.updateInt(updatedColumnIndex, 0);
                mCur.commitUpdates();
            }
        } catch (MalformedURLException ex) {
              mLogger.warning("Error in queryItem: Bad url");
              return false;
        }
        return true;
    }  
 
 // BEGIN_INCLUDE(5_2)    
    // Get the <pubDate> content from a feed and return a 
    // GregorianCalendar version of the date.
    // If the element doesn't exist or otherwise can't be
    // found, return a date of 0 to force a refresh.
    private GregorianCalendar parseRssDocPubDate(String xml){
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(0);
        String patt ="<[\\s]*pubDate[\\s]*>(.+?)</pubDate[\\s]*>";
        Pattern p = Pattern.compile(patt);
        Matcher m = p.matcher(xml);
        try {
            if(m.find()) {
                mLogger.info("pubDate: " + m.group());
                SimpleDateFormat pubDate = new SimpleDateFormat();
                cal.setTime(pubDate.parse(m.group(1)));
            }
       } catch(ParseException ex) {
            mLogger.warning("parseRssDocPubDate couldn't find a <pubDate> tag. Returning default value.");
       }
        return cal;
    }    
    
    // Read the submitted RSS page.
    String readRss(URL url){
      String html = "<html><body><h2>No data</h2></body></html>";
      try {
          mLogger.info("URL is:" + url.toString());
          BufferedReader inStream =
              new BufferedReader(new InputStreamReader(url.openStream()),
                      1024);
          String line;
          StringBuilder rssFeed = new StringBuilder();
          while ((line = inStream.readLine()) != null){
              rssFeed.append(line);
          }
          html = rssFeed.toString();
      } catch(IOException ex) {
          mLogger.warning("Couldn't open an RSS stream");
      }
      return html;
    }
//END_INCLUDE(5_2)

    // Callback we send to ourself to requery all feeds.
    public void run() {
        queryIfPeriodicRefreshRequired();
    }
    
    // Required by Service. We won't implement it here, but need to 
    // include this basic code.
    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    // This is the object that receives RPC calls from clients.See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new Binder()  {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            return super.onTransact(code, data, reply, flags);
        }
    };
}
