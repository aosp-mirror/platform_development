/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.adaptertransition;

/**
 * Sample data.
 */
public class Meat {

    public int resourceId;
    public String title;

    public Meat(int resourceId, String title) {
        this.resourceId = resourceId;
        this.title = title;
    }

    public static final Meat[] MEATS = {
            new Meat(R.drawable.p1, "First"),
            new Meat(R.drawable.p2, "Second"),
            new Meat(R.drawable.p3, "Third"),
            new Meat(R.drawable.p4, "Fourth"),
            new Meat(R.drawable.p5, "Fifth"),
            new Meat(R.drawable.p6, "Sixth"),
            new Meat(R.drawable.p7, "Seventh"),
            new Meat(R.drawable.p8, "Eighth"),
            new Meat(R.drawable.p9, "Ninth"),
            new Meat(R.drawable.p10, "Tenth"),
            new Meat(R.drawable.p11, "Eleventh"),
    };

}
