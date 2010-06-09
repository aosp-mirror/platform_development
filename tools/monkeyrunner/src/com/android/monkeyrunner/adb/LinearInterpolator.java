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



/**
 * Linear Interpolation class.
 */
public class LinearInterpolator {
    private final int steps;

    /**
     * Use our own Point class so we don't pull in java.awt.* just for this simple class.
     */
    public static class Point {
        private final int x;
        private final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return new StringBuilder().
                append("(").
                append(x).
                append(",").
                append(y).
                append(")").toString();
        }


        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point) {
                Point that = (Point) obj;
                return this.x == that.x && this.y == that.y;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 0x43125315 + x + y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    /**
     * Callback interface to recieve interpolated points.
     */
    public interface Callback {
        /**
         * Called once to inform of the start point.
         */
        void start(Point point);
        /**
         * Called once to inform of the end point.
         */
        void end(Point point);
        /**
         * Called at every step in-between start and end.
         */
        void step(Point point);
    }

    /**
     * Create a new linear Interpolator.
     *
     * @param steps How many steps should be in a single run.  This counts the intervals
     *              in-between points, so the actual number of points generated will be steps + 1.
     */
    public LinearInterpolator(int steps) {
        this.steps = steps;
    }

    // Copied from android.util.MathUtils since we couldn't link it in on the host.
    private static float lerp(float start, float stop, float amount) {
        return start + (stop - start) * amount;
    }

    /**
     * Calculate the interpolated points.
     *
     * @param start The starting point
     * @param end The ending point
     * @param callback the callback to call with each calculated points.
     */
    public void interpolate(Point start, Point end, Callback callback) {
        int xDistance = Math.abs(end.getX() - start.getX());
        int yDistance = Math.abs(end.getY() - start.getY());
        float amount = (float) (1.0 / steps);


        callback.start(start);
        for (int i = 1; i < steps; i++) {
            float newX = lerp(start.getX(), end.getX(), amount * i);
            float newY = lerp(start.getY(), end.getY(), amount * i);

            callback.step(new Point(Math.round(newX), Math.round(newY)));
        }
        // Generate final point
        callback.end(end);
    }
}
