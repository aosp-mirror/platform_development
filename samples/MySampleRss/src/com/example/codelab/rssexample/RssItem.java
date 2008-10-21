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

import java.util.Date;

// Custom class to hold an RSS item.
public class RssItem{
    public String url;
    public String title;
    public boolean hasBeenRead = false;
    public String content;
    public Date lastUpdated;
    
    public RssItem(String url, String title){
        this.url = url;
        this.title = title;
    }
    
    @Override public String toString(){
        return title;
    }
}
