/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.hdrviewfinder;

import android.app.Activity;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.RenderScript;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A small demo of advanced camera functionality with the Android camera2 API.
 *
 * <p>This demo implements a real-time high-dynamic-range camera viewfinder,
 * by alternating the sensor's exposure time between two exposure values on even and odd
 * frames, and then compositing together the latest two frames whenever a new frame is
 * captured.</p>
 *
 * <p>The demo has three modes: Regular auto-exposure viewfinder, split-screen manual exposure,
 * and the fused HDR viewfinder.  The latter two use manual exposure controlled by the user,
 * by swiping up/down on the right and left halves of the viewfinder.  The left half controls
 * the exposure time of even frames, and the right half controls the exposure time of odd frames.
 * </p>
 *
 * <p>In split-screen mode, the even frames are shown on the left and the odd frames on the right,
 * so the user can see two different exposures of the scene simultaneously.  In fused HDR mode,
 * the even/odd frames are merged together into a single image.  By selecting different exposure
 * values for the even/odd frames, the fused image has a higher dynamic range than the regular
 * viewfinder.</p>
 *
 * <p>The HDR fusion and the split-screen viewfinder processing is done with RenderScript; as is the
 * necessary YUV->RGB conversion. The camera subsystem outputs YUV images naturally, while the GPU
 * and display subsystems generally only accept RGB data.  Therefore, after the images are
 * fused/composited, a standard YUV->RGB color transform is applied before the the data is written
 * to the output Allocation. The HDR fusion algorithm is very simple, and tends to result in
 * lower-contrast scenes, but has very few artifacts and can run very fast.</p>
 *
 * <p>Data is passed between the subsystems (camera, RenderScript, and display) using the
 * Android {@link android.view.Surface} class, which allows for zero-copy transport of large
 * buffers between processes and subsystems.</p>
 */
public class HdrViewfinderActivity extends Activity implements
        SurfaceHolder.Callback, CameraOps.ErrorDisplayer, CameraOps.CameraReadyListener {

    private static final String TAG = "HdrViewfinderDemo";

    private static final String FRAGMENT_DIALOG = "dialog";

    /**
     * View for the camera preview.
     */
    private FixedAspectSurfaceView mPreviewView;

    /**
     * This shows the current mode of the app.
     */
    private TextView mModeText;

    // These show lengths of exposure for even frames, exposure for odd frames, and auto exposure.
    private TextView mEvenExposureText, mOddExposureText, mAutoExposureText;

    private Handler mUiHandler;

    private CameraCharacteristics mCameraInfo;

    private Surface mPreviewSurface;
    private Surface mProcessingHdrSurface;
    private Surface mProcessingNormalSurface;
    CaptureRequest.Builder mHdrBuilder;
    ArrayList<CaptureRequest> mHdrRequests = new ArrayList<CaptureRequest>(2);

    CaptureRequest mPreviewRequest;

    RenderScript mRS;
    ViewfinderProcessor mProcessor;
    CameraManager mCameraManager;
    CameraOps mCameraOps;

    private int mRenderMode = ViewfinderProcessor.MODE_NORMAL;

    // Durations in nanoseconds
    private static final long MICRO_SECOND = 1000;
    private static final long MILLI_SECOND = MICRO_SECOND * 1000;
    private static final long ONE_SECOND = MILLI_SECOND * 1000;

    private long mOddExposure = ONE_SECOND / 33;
    private long mEvenExposure = ONE_SECOND / 33;

    private Object mOddExposureTag = new Object();
    private Object mEvenExposureTag = new Object();
    private Object mAutoExposureTag = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPreviewView = (FixedAspectSurfaceView) findViewById(R.id.preview);
        mPreviewView.getHolder().addCallback(this);
        mPreviewView.setGestureListener(this, mViewListener);

        Button helpButton = (Button) findViewById(R.id.help_button);
        helpButton.setOnClickListener(mHelpButtonListener);

        mModeText = (TextView) findViewById(R.id.mode_label);
        mEvenExposureText = (TextView) findViewById(R.id.even_exposure);
        mOddExposureText = (TextView) findViewById(R.id.odd_exposure);
        mAutoExposureText = (TextView) findViewById(R.id.auto_exposure);

        mUiHandler = new Handler(Looper.getMainLooper());

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mCameraOps = new CameraOps(mCameraManager,
                /*errorDisplayer*/ this,
                /*readyListener*/ this,
                /*readyHandler*/ mUiHandler);

        mHdrRequests.add(null);
        mHdrRequests.add(null);

        mRS = RenderScript.create(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        findAndOpenCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Wait until camera is closed to ensure the next application can open it
        mCameraOps.closeCameraAndWait();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info: {
                MessageDialogFragment.newInstance(R.string.intro_message)
                        .show(getFragmentManager(), FRAGMENT_DIALOG);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private GestureDetector.OnGestureListener mViewListener
            = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            switchRenderMode(1);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mRenderMode == ViewfinderProcessor.MODE_NORMAL) return false;

            float xPosition = e1.getAxisValue(MotionEvent.AXIS_X);
            float width = mPreviewView.getWidth();
            float height = mPreviewView.getHeight();

            float xPosNorm = xPosition / width;
            float yDistNorm = distanceY / height;

            final float ACCELERATION_FACTOR = 8;
            double scaleFactor = Math.pow(2.f, yDistNorm * ACCELERATION_FACTOR);

            // Even on left, odd on right
            if (xPosNorm > 0.5) {
                mOddExposure *= scaleFactor;
            } else {
                mEvenExposure *= scaleFactor;
            }

            setHdrBurst();

            return true;
        }
    };

    // Show help dialog
    private View.OnClickListener mHelpButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            MessageDialogFragment.newInstance(R.string.help_text)
                    .show(getFragmentManager(), FRAGMENT_DIALOG);
        }
    };

    private void findAndOpenCamera() {

        String errorMessage = "Unknown error";
        boolean foundCamera = false;
        try {
            // Find first back-facing camera that has necessary capability
            String[] cameraIds = mCameraManager.getCameraIdList();
            for (String id : cameraIds) {
                CameraCharacteristics info = mCameraManager.getCameraCharacteristics(id);
                int facing = info.get(CameraCharacteristics.LENS_FACING);

                int level = info.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                boolean hasFullLevel
                        = (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);

                int[] capabilities = info.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                int syncLatency = info.get(CameraCharacteristics.SYNC_MAX_LATENCY);
                boolean hasManualControl = hasCapability(capabilities,
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);
                boolean hasEnoughCapability = hasManualControl &&
                        syncLatency == CameraCharacteristics.SYNC_MAX_LATENCY_PER_FRAME_CONTROL;

                // All these are guaranteed by
                // CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, but checking for only
                // the things we care about expands range of devices we can run on
                // We want:
                //  - Back-facing camera
                //  - Manual sensor control
                //  - Per-frame synchronization (so that exposure can be changed every frame)
                if (facing == CameraCharacteristics.LENS_FACING_BACK &&
                        (hasFullLevel || hasEnoughCapability)) {
                    // Found suitable camera - get info, open, and set up outputs
                    mCameraInfo = info;
                    mCameraOps.openCamera(id);
                    configureSurfaces();
                    foundCamera = true;
                    break;
                }
            }
            if (!foundCamera) {
                errorMessage = getString(R.string.camera_no_good);
            }
        } catch (CameraAccessException e) {
            errorMessage = getErrorString(e);
        }

        if (!foundCamera) {
            showErrorDialog(errorMessage);
        }
    }

    private boolean hasCapability(int[] capabilities, int capability) {
        for (int c : capabilities) {
            if (c == capability) return true;
        }
        return false;
    }

    private void switchRenderMode(int direction) {
        mRenderMode = (mRenderMode + direction) % 3;

        mModeText.setText(getResources().getStringArray(R.array.mode_label_array)[mRenderMode]);

        if (mProcessor != null) {
            mProcessor.setRenderMode(mRenderMode);
        }
        if (mRenderMode == ViewfinderProcessor.MODE_NORMAL) {
            mCameraOps.setRepeatingRequest(mPreviewRequest,
                    mCaptureCallback, mUiHandler);
        } else {
            setHdrBurst();
        }
    }

    /**
     * Configure the surfaceview and RS processing
     */
    private void configureSurfaces() {
        // Find a good size for output - largest 16:9 aspect ratio that's less than 720p
        final int MAX_WIDTH = 1280;
        final float TARGET_ASPECT = 16.f / 9.f;
        final float ASPECT_TOLERANCE = 0.1f;

        StreamConfigurationMap configs =
                mCameraInfo.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size[] outputSizes = configs.getOutputSizes(SurfaceHolder.class);

        Size outputSize = outputSizes[0];
        float outputAspect = (float) outputSize.getWidth() / outputSize.getHeight();
        for (Size candidateSize : outputSizes) {
            if (candidateSize.getWidth() > MAX_WIDTH) continue;
            float candidateAspect = (float) candidateSize.getWidth() / candidateSize.getHeight();
            boolean goodCandidateAspect =
                    Math.abs(candidateAspect - TARGET_ASPECT) < ASPECT_TOLERANCE;
            boolean goodOutputAspect =
                    Math.abs(outputAspect - TARGET_ASPECT) < ASPECT_TOLERANCE;
            if ((goodCandidateAspect && !goodOutputAspect) ||
                    candidateSize.getWidth() > outputSize.getWidth()) {
                outputSize = candidateSize;
                outputAspect = candidateAspect;
            }
        }
        Log.i(TAG, "Resolution chosen: " + outputSize);

        // Configure processing
        mProcessor = new ViewfinderProcessor(mRS, outputSize);
        setupProcessor();

        // Configure the output view - this will fire surfaceChanged
        mPreviewView.setAspectRatio(outputAspect);
        mPreviewView.getHolder().setFixedSize(outputSize.getWidth(), outputSize.getHeight());
    }

    /**
     * Once camera is open and output surfaces are ready, configure the RS processing
     * and the camera device inputs/outputs.
     */
    private void setupProcessor() {
        if (mProcessor == null || mPreviewSurface == null) return;

        mProcessor.setOutputSurface(mPreviewSurface);
        mProcessingHdrSurface = mProcessor.getInputHdrSurface();
        mProcessingNormalSurface = mProcessor.getInputNormalSurface();

        List<Surface> cameraOutputSurfaces = new ArrayList<Surface>();
        cameraOutputSurfaces.add(mProcessingHdrSurface);
        cameraOutputSurfaces.add(mProcessingNormalSurface);

        mCameraOps.setSurfaces(cameraOutputSurfaces);
    }

    /**
     * Start running an HDR burst on a configured camera session
     */
    public void setHdrBurst() {

        mHdrBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 1600);
        mHdrBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, ONE_SECOND / 30);

        mHdrBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mEvenExposure);
        mHdrBuilder.setTag(mEvenExposureTag);
        mHdrRequests.set(0, mHdrBuilder.build());

        mHdrBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mOddExposure);
        mHdrBuilder.setTag(mOddExposureTag);
        mHdrRequests.set(1, mHdrBuilder.build());

        mCameraOps.setRepeatingBurst(mHdrRequests, mCaptureCallback, mUiHandler);
    }

    /**
     * Listener for completed captures
     * Invoked on UI thread
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {

            // Only update UI every so many frames
            // Use an odd number here to ensure both even and odd exposures get an occasional update
            long frameNumber = result.getFrameNumber();
            if (frameNumber % 3 != 0) return;

            long exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);

            // Format exposure time nicely
            String exposureText;
            if (exposureTime > ONE_SECOND) {
                exposureText = String.format("%.2f s", exposureTime / 1e9);
            } else if (exposureTime > MILLI_SECOND) {
                exposureText = String.format("%.2f ms", exposureTime / 1e6);
            } else if (exposureTime > MICRO_SECOND) {
                exposureText = String.format("%.2f us", exposureTime / 1e3);
            } else {
                exposureText = String.format("%d ns", exposureTime);
            }

            Object tag = request.getTag();
            Log.i(TAG, "Exposure: " + exposureText);

            if (tag == mEvenExposureTag) {
                mEvenExposureText.setText(exposureText);

                mEvenExposureText.setEnabled(true);
                mOddExposureText.setEnabled(true);
                mAutoExposureText.setEnabled(false);
            } else if (tag == mOddExposureTag) {
                mOddExposureText.setText(exposureText);

                mEvenExposureText.setEnabled(true);
                mOddExposureText.setEnabled(true);
                mAutoExposureText.setEnabled(false);
            } else {
                mAutoExposureText.setText(exposureText);

                mEvenExposureText.setEnabled(false);
                mOddExposureText.setEnabled(false);
                mAutoExposureText.setEnabled(true);
            }
        }
    };

    /**
     * Callbacks for the FixedAspectSurfaceView
     */

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mPreviewSurface = holder.getSurface();

        setupProcessor();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // ignored
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mPreviewSurface = null;
    }

    /**
     * Callbacks for CameraOps
     */
    @Override
    public void onCameraReady() {
        // Ready to send requests in, so set them up
        try {
            CaptureRequest.Builder previewBuilder =
                    mCameraOps.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(mProcessingNormalSurface);
            previewBuilder.setTag(mAutoExposureTag);
            mPreviewRequest = previewBuilder.build();

            mHdrBuilder =
                    mCameraOps.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mHdrBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF);
            mHdrBuilder.addTarget(mProcessingHdrSurface);

            switchRenderMode(0);

        } catch (CameraAccessException e) {
            String errorMessage = getErrorString(e);
            showErrorDialog(errorMessage);
        }
    }

    /**
     * Utility methods
     */
    @Override
    public void showErrorDialog(String errorMessage) {
        MessageDialogFragment.newInstance(errorMessage).show(getFragmentManager(), FRAGMENT_DIALOG);
    }

    @Override
    public String getErrorString(CameraAccessException e) {
        String errorMessage;
        switch (e.getReason()) {
            case CameraAccessException.CAMERA_DISABLED:
                errorMessage = getString(R.string.camera_disabled);
                break;
            case CameraAccessException.CAMERA_DISCONNECTED:
                errorMessage = getString(R.string.camera_disconnected);
                break;
            case CameraAccessException.CAMERA_ERROR:
                errorMessage = getString(R.string.camera_error);
                break;
            default:
                errorMessage = getString(R.string.camera_unknown, e.getReason());
                break;
        }
        return errorMessage;
    }

}
