/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner.adb;

import com.google.common.collect.Lists;

import com.android.monkeyrunner.adb.LinearInterpolator.Point;

import junit.framework.TestCase;

import java.util.List;

/**
 * Unit tests for the LinerInterpolator class.S
 */
public class LinearInterpolatorTest extends TestCase {
    private static class Collector implements LinearInterpolator.Callback {
        private final List<LinearInterpolator.Point> points = Lists.newArrayList();

        public List<LinearInterpolator.Point> getPoints() {
            return points;
        }

        public void end(Point input) {
            points.add(input);
        }

        public void start(Point input) {
            points.add(input);
        }

        public void step(Point input) {
            points.add(input);
        }
    }

    List<Integer> STEP_POINTS = Lists.newArrayList(0, 100, 200, 300, 400, 500, 600, 700, 800, 900,
            1000);
    List<Integer> REVERSE_STEP_POINTS = Lists.newArrayList(1000, 900, 800, 700, 600, 500, 400, 300,
            200, 100, 0);

    public void testLerpRight() {
        LinearInterpolator lerp = new LinearInterpolator(10);
        Collector collector = new Collector();
        lerp.interpolate(new LinearInterpolator.Point(0, 100),
                new LinearInterpolator.Point(1000, 100),
                collector);

        List<LinearInterpolator.Point> points = collector.getPoints();
        assertEquals(11, points.size());
        for (int x = 0; x < points.size(); x++) {
            assertEquals(new Point(STEP_POINTS.get(x), 100), points.get(x));
        }
    }

    public void testLerpLeft() {
        LinearInterpolator lerp = new LinearInterpolator(10);
        Collector collector = new Collector();
        lerp.interpolate(new LinearInterpolator.Point(1000, 100),
                new LinearInterpolator.Point(0, 100),
                collector);

        List<LinearInterpolator.Point> points = collector.getPoints();
        assertEquals(11, points.size());
        for (int x = 0; x < points.size(); x++) {
            assertEquals(new Point(REVERSE_STEP_POINTS.get(x), 100), points.get(x));
        }
    }

    public void testLerpUp() {
        LinearInterpolator lerp = new LinearInterpolator(10);
        Collector collector = new Collector();
        lerp.interpolate(new LinearInterpolator.Point(100, 1000),
                new LinearInterpolator.Point(100, 0),
                collector);

        List<LinearInterpolator.Point> points = collector.getPoints();
        assertEquals(11, points.size());
        for (int x = 0; x < points.size(); x++) {
            assertEquals(new Point(100, REVERSE_STEP_POINTS.get(x)), points.get(x));
        }
    }

    public void testLerpDown() {
        LinearInterpolator lerp = new LinearInterpolator(10);
        Collector collector = new Collector();
        lerp.interpolate(new LinearInterpolator.Point(100, 0),
                new LinearInterpolator.Point(100, 1000),
                collector);

        List<LinearInterpolator.Point> points = collector.getPoints();
        assertEquals(11, points.size());
        for (int x = 0; x < points.size(); x++) {
            assertEquals(new Point(100, STEP_POINTS.get(x)), points.get(x));
        }
    }

    public void testLerpNW() {
        LinearInterpolator lerp = new LinearInterpolator(10);
        Collector collector = new Collector();
        lerp.interpolate(new LinearInterpolator.Point(0, 0),
                new LinearInterpolator.Point(1000, 1000),
                collector);

        List<LinearInterpolator.Point> points = collector.getPoints();
        assertEquals(11, points.size());
        for (int x = 0; x < points.size(); x++) {
            assertEquals(new Point(STEP_POINTS.get(x), STEP_POINTS.get(x)), points.get(x));
        }
    }

    public void testLerpNE() {
        LinearInterpolator lerp = new LinearInterpolator(10);
        Collector collector = new Collector();
        lerp.interpolate(new LinearInterpolator.Point(1000, 1000),
                new LinearInterpolator.Point(0, 0),
                collector);

        List<LinearInterpolator.Point> points = collector.getPoints();
        assertEquals(11, points.size());
        for (int x = 0; x < points.size(); x++) {
            assertEquals(new Point(REVERSE_STEP_POINTS.get(x), REVERSE_STEP_POINTS.get(x)), points.get(x));
        }
    }
}
