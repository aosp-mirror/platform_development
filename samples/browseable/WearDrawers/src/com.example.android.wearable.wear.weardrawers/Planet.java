/*
Copyright 2016 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.example.android.wearable.wear.weardrawers;

/**
 * Represents planet for app.
 */
public class Planet {

    private String name;
    private String navigationIcon;
    private String image;
    private String moons;
    private String volume;
    private String surfaceArea;

    public Planet(
            String name,
            String navigationIcon,
            String image,
            String moons,
            String volume,
            String surfaceArea) {

        this.name = name;
        this.navigationIcon = navigationIcon;
        this.image = image;
        this.moons = moons;
        this.volume = volume;
        this.surfaceArea = surfaceArea;
    }

    public String getName() {
        return name;
    }

    public String getNavigationIcon() {
        return navigationIcon;
    }

    public String getImage() {
        return image;
    }

    public String getMoons() {
        return moons;
    }

    public String getVolume() {
        return volume;
    }

    public String getSurfaceArea() {
        return surfaceArea;
    }
}