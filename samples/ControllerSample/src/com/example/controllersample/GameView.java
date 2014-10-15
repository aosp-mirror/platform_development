/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.controllersample;

import com.example.inputmanagercompat.InputManagerCompat;
import com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/*
 * A trivial joystick based physics game to demonstrate joystick handling. If
 * the game controller has a vibrator, then it is used to provide feedback when
 * a bullet is fired or the ship crashes into an obstacle. Otherwise, the system
 * vibrator is used for that purpose.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class GameView extends View implements InputDeviceListener {
    private static final int MAX_OBSTACLES = 12;

    private static final int DPAD_STATE_LEFT = 1 << 0;
    private static final int DPAD_STATE_RIGHT = 1 << 1;
    private static final int DPAD_STATE_UP = 1 << 2;
    private static final int DPAD_STATE_DOWN = 1 << 3;

    private final Random mRandom;
    /*
     * Each ship is created as an event comes in from a new Joystick device
     */
    private final SparseArray<Ship> mShips;
    private final Map<String, Integer> mDescriptorMap;
    private final List<Bullet> mBullets;
    private final List<Obstacle> mObstacles;

    private long mLastStepTime;
    private final InputManagerCompat mInputManager;

    private final float mBaseSpeed;

    private final float mShipSize;

    private final float mBulletSize;

    private final float mMinObstacleSize;
    private final float mMaxObstacleSize;
    private final float mMinObstacleSpeed;
    private final float mMaxObstacleSpeed;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRandom = new Random();
        mShips = new SparseArray<Ship>();
        mDescriptorMap = new HashMap<String, Integer>();
        mBullets = new ArrayList<Bullet>();
        mObstacles = new ArrayList<Obstacle>();

        setFocusable(true);
        setFocusableInTouchMode(true);

        float baseSize = getContext().getResources().getDisplayMetrics().density * 5f;
        mBaseSpeed = baseSize * 3;

        mShipSize = baseSize * 3;

        mBulletSize = baseSize;

        mMinObstacleSize = baseSize * 2;
        mMaxObstacleSize = baseSize * 12;
        mMinObstacleSpeed = mBaseSpeed;
        mMaxObstacleSpeed = mBaseSpeed * 3;

        mInputManager = InputManagerCompat.Factory.getInputManager(this.getContext());
        mInputManager.registerInputDeviceListener(this, null);
    }

    // Iterate through the input devices, looking for controllers. Create a ship
    // for every device that reports itself as a gamepad or joystick.
    void findControllersAndAttachShips() {
        int[] deviceIds = mInputManager.getInputDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = mInputManager.getInputDevice(deviceId);
            int sources = dev.getSources();
            // if the device is a gamepad/joystick, create a ship to represent it
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
                    ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
                // if the device has a gamepad or joystick
                getShipForId(deviceId);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int deviceId = event.getDeviceId();
        if (deviceId != -1) {
            Ship currentShip = getShipForId(deviceId);
            if (currentShip.onKeyDown(keyCode, event)) {
                step(event.getEventTime());
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int deviceId = event.getDeviceId();
        if (deviceId != -1) {
            Ship currentShip = getShipForId(deviceId);
            if (currentShip.onKeyUp(keyCode, event)) {
                step(event.getEventTime());
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        mInputManager.onGenericMotionEvent(event);

        // Check that the event came from a joystick or gamepad since a generic
        // motion event could be almost anything. API level 18 adds the useful
        // event.isFromSource() helper function.
        int eventSource = event.getSource();
        if ((((eventSource & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
                ((eventSource & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK))
                && event.getAction() == MotionEvent.ACTION_MOVE) {
            int id = event.getDeviceId();
            if (-1 != id) {
                Ship curShip = getShipForId(id);
                if (curShip.onGenericMotionEvent(event)) {
                    return true;
                }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // Turn on and off animations based on the window focus.
        // Alternately, we could update the game state using the Activity
        // onResume()
        // and onPause() lifecycle events.
        if (hasWindowFocus) {
            mLastStepTime = SystemClock.uptimeMillis();
            mInputManager.onResume();
        } else {
            int numShips = mShips.size();
            for (int i = 0; i < numShips; i++) {
                Ship currentShip = mShips.valueAt(i);
                if (currentShip != null) {
                    currentShip.setHeading(0, 0);
                    currentShip.setVelocity(0, 0);
                    currentShip.mDPadState = 0;
                }
            }
            mInputManager.onPause();
        }

        super.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Reset the game when the view changes size.
        reset();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Update the animation
        animateFrame();

        // Draw the ships.
        int numShips = mShips.size();
        for (int i = 0; i < numShips; i++) {
            Ship currentShip = mShips.valueAt(i);
            if (currentShip != null) {
                currentShip.draw(canvas);
            }
        }

        // Draw bullets.
        int numBullets = mBullets.size();
        for (int i = 0; i < numBullets; i++) {
            final Bullet bullet = mBullets.get(i);
            bullet.draw(canvas);
        }

        // Draw obstacles.
        int numObstacles = mObstacles.size();
        for (int i = 0; i < numObstacles; i++) {
            final Obstacle obstacle = mObstacles.get(i);
            obstacle.draw(canvas);
        }
    }

    /**
     * Uses the device descriptor to try to assign the same color to the same
     * joystick. If there are two joysticks of the same type connected over USB,
     * or the API is < API level 16, it will be unable to distinguish the two
     * devices.
     *
     * @param shipID
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private Ship getShipForId(int shipID) {
        Ship currentShip = mShips.get(shipID);
        if (null == currentShip) {

            // do we know something about this ship already?
            InputDevice dev = InputDevice.getDevice(shipID);
            String deviceString = null;
            Integer shipColor = null;
            if (null != dev) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    deviceString = dev.getDescriptor();
                } else {
                    deviceString = dev.getName();
                }
                shipColor = mDescriptorMap.get(deviceString);
            }

            if (null != shipColor) {
                int color = shipColor;
                int numShips = mShips.size();
                // do we already have a ship with this color?
                for (int i = 0; i < numShips; i++) {
                    if (mShips.valueAt(i).getColor() == color) {
                        shipColor = null;
                        // we won't store this value either --- if the first
                        // controller gets disconnected/connected, it will get
                        // the same color.
                        deviceString = null;
                    }
                }
            }
            if (null != shipColor) {
                currentShip = new Ship(shipColor);
                if (null != deviceString) {
                    mDescriptorMap.remove(deviceString);
                }
            } else {
                currentShip = new Ship(getNextShipColor());
            }
            mShips.append(shipID, currentShip);
            currentShip.setInputDevice(dev);

            if (null != deviceString) {
                mDescriptorMap.put(deviceString, currentShip.getColor());
            }
        }
        return currentShip;
    }

    /**
     * Remove the ship from the array of active ships by ID.
     *
     * @param shipID
     */
    private void removeShipForID(int shipID) {
        mShips.remove(shipID);
    }

    private void reset() {
        mShips.clear();
        mBullets.clear();
        mObstacles.clear();
        findControllersAndAttachShips();
    }

    private void animateFrame() {
        long currentStepTime = SystemClock.uptimeMillis();
        step(currentStepTime);
        invalidate();
    }

    private void step(long currentStepTime) {
        float tau = (currentStepTime - mLastStepTime) * 0.001f;
        mLastStepTime = currentStepTime;

        // Move the ships
        int numShips = mShips.size();
        for (int i = 0; i < numShips; i++) {
            Ship currentShip = mShips.valueAt(i);
            if (currentShip != null) {
                currentShip.accelerate(tau);
                if (!currentShip.step(tau)) {
                    currentShip.reincarnate();
                }
            }
        }

        // Move the bullets.
        int numBullets = mBullets.size();
        for (int i = 0; i < numBullets; i++) {
            final Bullet bullet = mBullets.get(i);
            if (!bullet.step(tau)) {
                mBullets.remove(i);
                i -= 1;
                numBullets -= 1;
            }
        }

        // Move obstacles.
        int numObstacles = mObstacles.size();
        for (int i = 0; i < numObstacles; i++) {
            final Obstacle obstacle = mObstacles.get(i);
            if (!obstacle.step(tau)) {
                mObstacles.remove(i);
                i -= 1;
                numObstacles -= 1;
            }
        }

        // Check for collisions between bullets and obstacles.
        for (int i = 0; i < numBullets; i++) {
            final Bullet bullet = mBullets.get(i);
            for (int j = 0; j < numObstacles; j++) {
                final Obstacle obstacle = mObstacles.get(j);
                if (bullet.collidesWith(obstacle)) {
                    bullet.destroy();
                    obstacle.destroy();
                    break;
                }
            }
        }

        // Check for collisions between the ship and obstacles --- this could
        // get slow
        for (int i = 0; i < numObstacles; i++) {
            final Obstacle obstacle = mObstacles.get(i);
            for (int j = 0; j < numShips; j++) {
                Ship currentShip = mShips.valueAt(j);
                if (currentShip != null) {
                    if (currentShip.collidesWith(obstacle)) {
                        currentShip.destroy();
                        obstacle.destroy();
                        break;
                    }
                }
            }
        }

        // Spawn more obstacles offscreen when needed.
        // Avoid putting them right on top of the ship.
        int tries = MAX_OBSTACLES - mObstacles.size() + 10;
        final float minDistance = mShipSize * 4;
        while (mObstacles.size() < MAX_OBSTACLES && tries-- > 0) {
            float size = mRandom.nextFloat() * (mMaxObstacleSize - mMinObstacleSize)
                    + mMinObstacleSize;
            float positionX, positionY;
            int edge = mRandom.nextInt(4);
            switch (edge) {
                case 0:
                    positionX = -size;
                    positionY = mRandom.nextInt(getHeight());
                    break;
                case 1:
                    positionX = getWidth() + size;
                    positionY = mRandom.nextInt(getHeight());
                    break;
                case 2:
                    positionX = mRandom.nextInt(getWidth());
                    positionY = -size;
                    break;
                default:
                    positionX = mRandom.nextInt(getWidth());
                    positionY = getHeight() + size;
                    break;
            }
            boolean positionSafe = true;

            // If the obstacle is too close to any ships, we don't want to
            // spawn it.
            for (int i = 0; i < numShips; i++) {
                Ship currentShip = mShips.valueAt(i);
                if (currentShip != null) {
                    if (currentShip.distanceTo(positionX, positionY) < minDistance) {
                        // try to spawn again
                        positionSafe = false;
                        break;
                    }
                }
            }

            // if the position is safe, add the obstacle and reset the retry
            // counter
            if (positionSafe) {
                tries = MAX_OBSTACLES - mObstacles.size() + 10;
                // we can add the obstacle now since it isn't close to any ships
                float direction = mRandom.nextFloat() * (float) Math.PI * 2;
                float speed = mRandom.nextFloat() * (mMaxObstacleSpeed - mMinObstacleSpeed)
                        + mMinObstacleSpeed;
                float velocityX = (float) Math.cos(direction) * speed;
                float velocityY = (float) Math.sin(direction) * speed;

                Obstacle obstacle = new Obstacle();
                obstacle.setPosition(positionX, positionY);
                obstacle.setSize(size);
                obstacle.setVelocity(velocityX, velocityY);
                mObstacles.add(obstacle);
            }
        }
    }

    private static float pythag(float x, float y) {
        return (float) Math.hypot(x, y);
    }

    private static int blend(float alpha, int from, int to) {
        return from + (int) ((to - from) * alpha);
    }

    private static void setPaintARGBBlend(Paint paint, float alpha,
            int a1, int r1, int g1, int b1,
            int a2, int r2, int g2, int b2) {
        paint.setARGB(blend(alpha, a1, a2), blend(alpha, r1, r2),
                blend(alpha, g1, g2), blend(alpha, b1, b2));
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device,
            int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        if (range != null) {
            final float flat = range.getFlat();
            final float value = historyPos < 0 ? event.getAxisValue(axis)
                    : event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            // A joystick at rest does not always report an absolute position of
            // (0,0).
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    /**
     * Any gamepad button + the spacebar or DPAD_CENTER will be used as the fire
     * key.
     *
     * @param keyCode
     * @return true of it's a fire key.
     */
    private static boolean isFireKey(int keyCode) {
        return KeyEvent.isGamepadButton(keyCode)
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_SPACE;
    }

    private abstract class Sprite {
        protected float mPositionX;
        protected float mPositionY;
        protected float mVelocityX;
        protected float mVelocityY;
        protected float mSize;
        protected boolean mDestroyed;
        protected float mDestroyAnimProgress;

        public void setPosition(float x, float y) {
            mPositionX = x;
            mPositionY = y;
        }

        public void setVelocity(float x, float y) {
            mVelocityX = x;
            mVelocityY = y;
        }

        public void setSize(float size) {
            mSize = size;
        }

        public float distanceTo(float x, float y) {
            return pythag(mPositionX - x, mPositionY - y);
        }

        public float distanceTo(Sprite other) {
            return distanceTo(other.mPositionX, other.mPositionY);
        }

        public boolean collidesWith(Sprite other) {
            // Really bad collision detection.
            return !mDestroyed && !other.mDestroyed
                    && distanceTo(other) <= Math.max(mSize, other.mSize)
                            + Math.min(mSize, other.mSize) * 0.5f;
        }

        public boolean isDestroyed() {
            return mDestroyed;
        }

        /**
         * Moves the sprite based on the elapsed time defined by tau.
         *
         * @param tau the elapsed time in seconds since the last step
         * @return false if the sprite is to be removed from the display
         */
        public boolean step(float tau) {
            mPositionX += mVelocityX * tau;
            mPositionY += mVelocityY * tau;

            if (mDestroyed) {
                mDestroyAnimProgress += tau / getDestroyAnimDuration();
                if (mDestroyAnimProgress >= getDestroyAnimCycles()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Draws the sprite.
         *
         * @param canvas the Canvas upon which to draw the sprite.
         */
        public abstract void draw(Canvas canvas);

        /**
         * Returns the duration of the destruction animation of the sprite in
         * seconds.
         *
         * @return the float duration in seconds of the destruction animation
         */
        public abstract float getDestroyAnimDuration();

        /**
         * Returns the number of cycles to play the destruction animation. A
         * destruction animation has a duration and a number of cycles to play
         * it for, so we can have an extended death sequence when a ship or
         * object is destroyed.
         *
         * @return the float number of cycles to play the destruction animation
         */
        public abstract float getDestroyAnimCycles();

        protected boolean isOutsidePlayfield() {
            final int width = GameView.this.getWidth();
            final int height = GameView.this.getHeight();
            return mPositionX < 0 || mPositionX >= width
                    || mPositionY < 0 || mPositionY >= height;
        }

        protected void wrapAtPlayfieldBoundary() {
            final int width = GameView.this.getWidth();
            final int height = GameView.this.getHeight();
            while (mPositionX <= -mSize) {
                mPositionX += width + mSize * 2;
            }
            while (mPositionX >= width + mSize) {
                mPositionX -= width + mSize * 2;
            }
            while (mPositionY <= -mSize) {
                mPositionY += height + mSize * 2;
            }
            while (mPositionY >= height + mSize) {
                mPositionY -= height + mSize * 2;
            }
        }

        public void destroy() {
            mDestroyed = true;
            step(0);
        }
    }

    private static int sShipColor = 0;

    /**
     * Returns the next ship color in the sequence. Very simple. Does not in any
     * way guarantee that there are not multiple ships with the same color on
     * the screen.
     *
     * @return an int containing the index of the next ship color
     */
    private static int getNextShipColor() {
        int color = sShipColor & 0x07;
        if (0 == color) {
            color++;
            sShipColor++;
        }
        sShipColor++;
        return color;
    }

    /*
     * Static constants associated with Ship inner class
     */
    private static final long[] sDestructionVibratePattern = new long[] {
            0, 20, 20, 40, 40, 80, 40, 300
    };

    private class Ship extends Sprite {
        private static final float CORNER_ANGLE = (float) Math.PI * 2 / 3;
        private static final float TO_DEGREES = (float) (180.0 / Math.PI);

        private final float mMaxShipThrust = mBaseSpeed * 0.25f;
        private final float mMaxSpeed = mBaseSpeed * 12;

        // The ship actually determines the speed of the bullet, not the bullet
        // itself
        private final float mBulletSpeed = mBaseSpeed * 12;

        private final Paint mPaint;
        private final Path mPath;
        private final int mR, mG, mB;
        private final int mColor;

        // The current device that is controlling the ship
        private InputDevice mInputDevice;

        private float mHeadingX;
        private float mHeadingY;
        private float mHeadingAngle;
        private float mHeadingMagnitude;

        private int mDPadState;

        /**
         * The colorIndex is used to create the color based on the lower three
         * bits of the value in the current implementation.
         *
         * @param colorIndex
         */
        public Ship(int colorIndex) {
            mPaint = new Paint();
            mPaint.setStyle(Style.FILL);

            setPosition(getWidth() * 0.5f, getHeight() * 0.5f);
            setVelocity(0, 0);
            setSize(mShipSize);

            mPath = new Path();
            mPath.moveTo(0, 0);
            mPath.lineTo((float) Math.cos(-CORNER_ANGLE) * mSize,
                    (float) Math.sin(-CORNER_ANGLE) * mSize);
            mPath.lineTo(mSize, 0);
            mPath.lineTo((float) Math.cos(CORNER_ANGLE) * mSize,
                    (float) Math.sin(CORNER_ANGLE) * mSize);
            mPath.lineTo(0, 0);

            mR = (colorIndex & 0x01) == 0 ? 63 : 255;
            mG = (colorIndex & 0x02) == 0 ? 63 : 255;
            mB = (colorIndex & 0x04) == 0 ? 63 : 255;

            mColor = colorIndex;
        }

        public boolean onKeyUp(int keyCode, KeyEvent event) {

            // Handle keys going up.
            boolean handled = false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    setHeadingX(0);
                    mDPadState &= ~DPAD_STATE_LEFT;
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    setHeadingX(0);
                    mDPadState &= ~DPAD_STATE_RIGHT;
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    setHeadingY(0);
                    mDPadState &= ~DPAD_STATE_UP;
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    setHeadingY(0);
                    mDPadState &= ~DPAD_STATE_DOWN;
                    handled = true;
                    break;
                default:
                    if (isFireKey(keyCode)) {
                        handled = true;
                    }
                    break;
            }
            return handled;
        }

        /*
         * Firing is a unique case where a ship creates a bullet. A bullet needs
         * to be created with a position near the ship that is firing with a
         * velocity that is based upon the speed of the ship.
         */
        private void fire() {
            if (!isDestroyed()) {
                Bullet bullet = new Bullet();
                bullet.setPosition(getBulletInitialX(), getBulletInitialY());
                bullet.setVelocity(getBulletVelocityX(),
                        getBulletVelocityY());
                mBullets.add(bullet);
                vibrateController(20);
            }
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            // Handle DPad keys and fire button on initial down but not on
            // auto-repeat.
            boolean handled = false;
            if (event.getRepeatCount() == 0) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        setHeadingX(-1);
                        mDPadState |= DPAD_STATE_LEFT;
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        setHeadingX(1);
                        mDPadState |= DPAD_STATE_RIGHT;
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        setHeadingY(-1);
                        mDPadState |= DPAD_STATE_UP;
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        setHeadingY(1);
                        mDPadState |= DPAD_STATE_DOWN;
                        handled = true;
                        break;
                    default:
                        if (isFireKey(keyCode)) {
                            fire();
                            handled = true;
                        }
                        break;
                }
            }
            return handled;
        }

        /**
         * Gets the vibrator from the controller if it is present. Note that it
         * would be easy to get the system vibrator here if the controller one
         * is not present, but we don't choose to do it in this case.
         *
         * @return the Vibrator for the controller, or null if it is not
         *         present. or the API level cannot support it
         */
        @SuppressLint("NewApi")
        private final Vibrator getVibrator() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                    null != mInputDevice) {
                return mInputDevice.getVibrator();
            }
            return null;
        }

        private void vibrateController(int time) {
            Vibrator vibrator = getVibrator();
            if (null != vibrator) {
                vibrator.vibrate(time);
            }
        }

        private void vibrateController(long[] pattern, int repeat) {
            Vibrator vibrator = getVibrator();
            if (null != vibrator) {
                vibrator.vibrate(pattern, repeat);
            }
        }

        /**
         * The ship directly handles joystick input.
         *
         * @param event
         * @param historyPos
         */
        private void processJoystickInput(MotionEvent event, int historyPos) {
            // Get joystick position.
            // Many game pads with two joysticks report the position of the
            // second
            // joystick
            // using the Z and RZ axes so we also handle those.
            // In a real game, we would allow the user to configure the axes
            // manually.
            if (null == mInputDevice) {
                mInputDevice = event.getDevice();
            }
            float x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_X, historyPos);
            if (x == 0) {
                x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_HAT_X, historyPos);
            }
            if (x == 0) {
                x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Z, historyPos);
            }

            float y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Y, historyPos);
            if (y == 0) {
                y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_HAT_Y, historyPos);
            }
            if (y == 0) {
                y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RZ, historyPos);
            }

            // Set the ship heading.
            setHeading(x, y);
            GameView.this.step(historyPos < 0 ? event.getEventTime() : event
                    .getHistoricalEventTime(historyPos));
        }

        public boolean onGenericMotionEvent(MotionEvent event) {
            if (0 == mDPadState) {
                // Process all historical movement samples in the batch.
                final int historySize = event.getHistorySize();
                for (int i = 0; i < historySize; i++) {
                    processJoystickInput(event, i);
                }

                // Process the current movement sample in the batch.
                processJoystickInput(event, -1);
            }
            return true;
        }

        /**
         * Set the game controller to be used to control the ship.
         *
         * @param dev the input device that will be controlling the ship
         */
        public void setInputDevice(InputDevice dev) {
            mInputDevice = dev;
        }

        /**
         * Sets the X component of the joystick heading value, defined by the
         * platform as being from -1.0 (left) to 1.0 (right). This function is
         * generally used to change the heading in response to a button-style
         * DPAD event.
         *
         * @param x the float x component of the joystick heading value
         */
        public void setHeadingX(float x) {
            mHeadingX = x;
            updateHeading();
        }

        /**
         * Sets the Y component of the joystick heading value, defined by the
         * platform as being from -1.0 (top) to 1.0 (bottom). This function is
         * generally used to change the heading in response to a button-style
         * DPAD event.
         *
         * @param y the float y component of the joystick heading value
         */
        public void setHeadingY(float y) {
            mHeadingY = y;
            updateHeading();
        }

        /**
         * Sets the heading as floating point values returned by a joystick.
         * These values are normalized by the Android platform to be from -1.0
         * (left, top) to 1.0 (right, bottom)
         *
         * @param x the float x component of the joystick heading value
         * @param y the float y component of the joystick heading value
         */
        public void setHeading(float x, float y) {
            mHeadingX = x;
            mHeadingY = y;
            updateHeading();
        }

        /**
         * Converts the heading values from joystick devices to the polar
         * representation of the heading angle if the magnitude of the heading
         * is significant (> 0.1f).
         */
        private void updateHeading() {
            mHeadingMagnitude = pythag(mHeadingX, mHeadingY);
            if (mHeadingMagnitude > 0.1f) {
                mHeadingAngle = (float) Math.atan2(mHeadingY, mHeadingX);
            }
        }

        /**
         * Bring our ship back to life, stopping the destroy animation.
         */
        public void reincarnate() {
            mDestroyed = false;
            mDestroyAnimProgress = 0.0f;
        }

        private float polarX(float radius) {
            return (float) Math.cos(mHeadingAngle) * radius;
        }

        private float polarY(float radius) {
            return (float) Math.sin(mHeadingAngle) * radius;
        }

        /**
         * Gets the initial x coordinate for the bullet.
         *
         * @return the x coordinate of the bullet adjusted for the position and
         *         direction of the ship
         */
        public float getBulletInitialX() {
            return mPositionX + polarX(mSize);
        }

        /**
         * Gets the initial y coordinate for the bullet.
         *
         * @return the y coordinate of the bullet adjusted for the position and
         *         direction of the ship
         */
        public float getBulletInitialY() {
            return mPositionY + polarY(mSize);
        }

        /**
         * Returns the bullet speed Y component.
         *
         * @return adjusted Y component bullet speed for the velocity and
         *         direction of the ship
         */
        public float getBulletVelocityY() {
            return mVelocityY + polarY(mBulletSpeed);
        }

        /**
         * Returns the bullet speed X component
         *
         * @return adjusted X component bullet speed for the velocity and
         *         direction of the ship
         */
        public float getBulletVelocityX() {
            return mVelocityX + polarX(mBulletSpeed);
        }

        /**
         * Uses the heading magnitude and direction to change the acceleration
         * of the ship. In theory, this should be scaled according to the
         * elapsed time.
         *
         * @param tau the elapsed time in seconds between the last step
         */
        public void accelerate(float tau) {
            final float thrust = mHeadingMagnitude * mMaxShipThrust;
            mVelocityX += polarX(thrust) * tau * mMaxSpeed / 4;
            mVelocityY += polarY(thrust) * tau * mMaxSpeed / 4;

            final float speed = pythag(mVelocityX, mVelocityY);
            if (speed > mMaxSpeed) {
                final float scale = mMaxSpeed / speed;
                mVelocityX = mVelocityX * scale * scale;
                mVelocityY = mVelocityY * scale * scale;
            }
        }

        @Override
        public boolean step(float tau) {
            if (!super.step(tau)) {
                return false;
            }
            wrapAtPlayfieldBoundary();
            return true;
        }

        @Override
        public void draw(Canvas canvas) {
            setPaintARGBBlend(mPaint, mDestroyAnimProgress - (int) (mDestroyAnimProgress),
                    255, mR, mG, mB,
                    0, 255, 0, 0);

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.translate(mPositionX, mPositionY);
            canvas.rotate(mHeadingAngle * TO_DEGREES);
            canvas.drawPath(mPath, mPaint);
            canvas.restore();
        }

        @Override
        public float getDestroyAnimDuration() {
            return 1.0f;
        }

        @Override
        public void destroy() {
            super.destroy();
            vibrateController(sDestructionVibratePattern, -1);
        }

        @Override
        public float getDestroyAnimCycles() {
            return 5.0f;
        }

        public int getColor() {
            return mColor;
        }
    }

    private static final Paint mBulletPaint;
    static {
        mBulletPaint = new Paint();
        mBulletPaint.setStyle(Style.FILL);
    }

    private class Bullet extends Sprite {

        public Bullet() {
            setSize(mBulletSize);
        }

        @Override
        public boolean step(float tau) {
            if (!super.step(tau)) {
                return false;
            }
            return !isOutsidePlayfield();
        }

        @Override
        public void draw(Canvas canvas) {
            setPaintARGBBlend(mBulletPaint, mDestroyAnimProgress,
                    255, 255, 255, 0,
                    0, 255, 255, 255);
            canvas.drawCircle(mPositionX, mPositionY, mSize, mBulletPaint);
        }

        @Override
        public float getDestroyAnimDuration() {
            return 0.125f;
        }

        @Override
        public float getDestroyAnimCycles() {
            return 1.0f;
        }

    }

    private static final Paint mObstaclePaint;
    static {
        mObstaclePaint = new Paint();
        mObstaclePaint.setARGB(255, 127, 127, 255);
        mObstaclePaint.setStyle(Style.FILL);
    }

    private class Obstacle extends Sprite {

        @Override
        public boolean step(float tau) {
            if (!super.step(tau)) {
                return false;
            }
            wrapAtPlayfieldBoundary();
            return true;
        }

        @Override
        public void draw(Canvas canvas) {
            setPaintARGBBlend(mObstaclePaint, mDestroyAnimProgress,
                    255, 127, 127, 255,
                    0, 255, 0, 0);
            canvas.drawCircle(mPositionX, mPositionY,
                    mSize * (1.0f - mDestroyAnimProgress), mObstaclePaint);
        }

        @Override
        public float getDestroyAnimDuration() {
            return 0.25f;
        }

        @Override
        public float getDestroyAnimCycles() {
            return 1.0f;
        }
    }

    /*
     * When an input device is added, we add a ship based upon the device.
     * @see
     * com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener
     * #onInputDeviceAdded(int)
     */
    @Override
    public void onInputDeviceAdded(int deviceId) {
        getShipForId(deviceId);
    }

    /*
     * This is an unusual case. Input devices don't typically change, but they
     * certainly can --- for example a device may have different modes. We use
     * this to make sure that the ship has an up-to-date InputDevice.
     * @see
     * com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener
     * #onInputDeviceChanged(int)
     */
    @Override
    public void onInputDeviceChanged(int deviceId) {
        Ship ship = getShipForId(deviceId);
        ship.setInputDevice(InputDevice.getDevice(deviceId));
    }

    /*
     * Remove any ship associated with the ID.
     * @see
     * com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener
     * #onInputDeviceRemoved(int)
     */
    @Override
    public void onInputDeviceRemoved(int deviceId) {
        removeShipForID(deviceId);
    }
}
