/*
 * Copyright 2025 The Android Open Source Project
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

package android.service.chooser;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * <p>A class that represents an interactive Chooser session.</p>
 * <p>An instance of the class can be used as a value for <em>an</em> {@link Intent#ACTION_CHOOSER}
 * extra to establish a bi-directional communication channel with Chooser.
 * <p>A {@link ChooserSessionUpdateListener} callback can be used to receive updates about the
 * session and communication from Chooser.</p>
 */
public final class ChooserSession {

    /**
     * @hide
     */
    public static final String EXTRA_CHOOSER_SESSION =
            "com.android.extra.EXTRA_CHOOSER_INTERACTIVE_CALLBACK";

    private static final String TAG = "ChooserSession";

    private final ChooserSessionImpl mChooserSession;

    private ChooserSession(ChooserSessionImpl chooserSession) {
        mChooserSession = chooserSession;
    }

    /**
     * Start a new interactive Chooser session. The method is idempotent and will start Chooser only
     * once.
     * @param chooserIntent a {@link Intent#ACTION_CHOOSER} intent that will be used as a base
     * for the new Chooser session.
     * <p>An interactive Chooser session also supports the following chooser parameters:
     * <ul>
     * <li>{@link Intent#EXTRA_ALTERNATE_INTENTS}</li>
     * <li>{@link Intent#EXTRA_INITIAL_INTENTS}</li>
     * <li>{@link Intent#EXTRA_EXCLUDE_COMPONENTS}</li>
     * <li>{@link Intent#EXTRA_REPLACEMENT_EXTRAS}</li>
     * <li>{@link Intent#EXTRA_CHOOSER_TARGETS}</li>
     * <li>{@link Intent#EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER}</li>
     * <li>{@link Intent#EXTRA_CHOOSER_RESULT}</li>
     * <li>{@link Intent#EXTRA_CHOOSER_RESULT_INTENT_SENDER}</li>
     * <li>{@link Intent#EXTRA_CHOSEN_COMPONENT_INTENT_SENDER}</li>
     * <li>{@link Intent#EXTRA_CONTENT_ANNOTATIONS}</li>
     * <li>{@link Intent#EXTRA_AUTO_LAUNCH_SINGLE_CHOICE}</li>
     * </ul>
     * </p>
     * <p>See also {@link Intent#createChooser(Intent, CharSequence) }.</p>
     */
    public void start(Context context, Intent chooserIntent) {
        if (!Intent.ACTION_CHOOSER.equals(chooserIntent.getAction())) {
            throw new IllegalArgumentException("A chooser intent is expected");
        }
        chooserIntent = new Intent(chooserIntent);
        Bundle binderExtras = new Bundle();
        binderExtras.putBinder(EXTRA_CHOOSER_SESSION, mChooserSession);
        chooserIntent.putExtras(binderExtras);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setAllowPassThroughOnTouchOutside(true);
        context.startActivity(chooserIntent, options.toBundle());
    }

    /**
     * @return true if the session is active: i.e. is not being cancelled by the client
     * (see {@link #cancel()}) or closed by the Chooser.
     */
    public boolean isActive() {
        return mChooserSession.isActive();
    }

    /**
     * Cancel the session and close the Chooser.
     */
    public void cancel() {
        mChooserSession.cancel();
    }

    /**
     * <p>Get the active {@link ChooserController} or {@code null} if none is available.</p>
     * A chooser controller becomes available after the Chooser has registered it and stays
     * available while the session is active and the Chooser process is alive. It is possible for a
     * session to remain active without a Chooser process. For example, this could happen when the
     * client launches another activity on top of the Chooser session and the system reclaims the
     * new backgrounded chooser process. In such example, upon navigating back to the session, a
     * restored Chooser should register a new {@link ChooserController}.
     */
    @Nullable
    public ChooserController getChooserController() {
        return mChooserSession.getChooserController();
    }

    /**
     * @param listener make sure that the callback is cleared at the end of a component's lifecycle
     * (e.g. Activity) or provide a properly maintained WeakReference wrapper to avoid memory leaks.
     */
    public void setChooserStateListener(@Nullable ChooserSessionUpdateListener listener) {
        mChooserSession.setChooserStateListener(listener);
    }

    /**
     * A callback interface for Chooser session state updates.
     */
    public interface ChooserSessionUpdateListener {

        /**
         * Gets invoked when a {@link ChooserController} becomes available.
         * @param chooserController active chooser controller.
         */
        void onChooserConnected(ChooserController chooserController);

        /**
         * Gets invoked when the session is closed by the Chooser.
         */
        void onSessionClosed();

        /**
         * Gets invoked when drawer size is changed. The rect parameter represents Chooser window
         * position in pixels.
         */
        void onSizeChanged(Rect size);
    }

    /**
     * An interface for updating the Chooser.
     */
    public interface ChooserController {

        /**
         * Update chooser intent in a Chooser session.
         * <p>Updatable Chooser parameters:
         * <ul>
         * <li> {@link Intent#EXTRA_INTENT}
         * <li> {@link Intent#EXTRA_EXCLUDE_COMPONENTS}
         * <li> {@link Intent#EXTRA_CHOOSER_TARGETS}
         * <li> {@link Intent#EXTRA_ALTERNATE_INTENTS}
         * <li> {@link Intent#EXTRA_REPLACEMENT_EXTRAS}
         * <li> {@link Intent#EXTRA_INITIAL_INTENTS}
         * <li> {@link Intent#EXTRA_CHOOSER_RESULT_INTENT_SENDER}
         * <li> {@link Intent#EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER}
         * <li> {@link Intent#EXTRA_CONTENT_ANNOTATIONS}
         * </ul>
         * </p>
         */
        void updateIntent(Intent intent) throws RemoteException;
    }

    /**
     * A ChooserSession builder.
     */
    public static class Builder {
        private Handler mHandler = new Handler(Looper.getMainLooper());

        /**
         * Set {@link Handler} the session will be running on. All callbacks will be executed on the
         * corresponding thread. By default, ChooserSession will run on the main thread.
         */
        public Builder withHandler(Handler handler) {
            Objects.requireNonNull(handler, "handler can not be null");
            mHandler = handler;
            return this;
        }

        /**
         * Create a new ChooserSession instance.
         */
        public ChooserSession build() {
            return new ChooserSession(new ChooserSessionImpl(mHandler));
        }
    }

    // Just to hide Chooser binder object from the client.
    private static class ChooserControllerWrapper implements ChooserController {
        public final IChooserController controller;

        private ChooserControllerWrapper(IChooserController controller) {
            this.controller = controller;
        }

        @Override
        public void updateIntent(Intent intent) throws RemoteException {
            controller.updateIntent(intent);
        }
    }

    private static class ChooserSessionImpl extends IChooserControllerCallback.Stub {
        private final Handler mHandler;
        @Nullable
        private volatile ChooserSessionUpdateListener mListener;
        private volatile boolean mIsActive = true;
        @Nullable
        private volatile ChooserControllerWrapper mChooserController;
        @Nullable
        private IBinder.DeathRecipient mChooserControllerLinkToDeath;

        ChooserSessionImpl(Handler handler) {
            mHandler = handler;
        }

        @Override
        public void registerChooserController(
                @Nullable final IChooserController chooserController) {
            mHandler.post(() -> doRegisterChooserController(chooserController));
        }

        @Override
        public void onSizeChanged(Rect size) {
            mHandler.post(() -> doOnSizeChanged(size));
        }

        @Override
        public void onClosed() {
            mHandler.post(this::doOnClosed);
        }

        public boolean isActive() {
            return mIsActive;
        }

        public void cancel() {
            mIsActive = false;
            mListener = null;
            if (mHandler.getLooper().isCurrentThread()) {
                doCancel();
            } else {
                mHandler.post(this::doCancel);
            }
        }

        @Nullable
        public ChooserController getChooserController() {
            return mChooserController;
        }

        public void setChooserStateListener(
                @Nullable ChooserSessionUpdateListener listener) {
            mListener = listener;
            publishState();
        }

        private void publishState() {
            if (mHandler.getLooper().isCurrentThread()) {
                if (!mIsActive) {
                    notifySessionClosed();
                } else if (mChooserController != null) {
                    notifyChooserConnected(mChooserController);
                }
            } else {
                mHandler.post(this::publishState);
            }
        }

        private void doCancel() {
            ChooserControllerWrapper controllerWrapper = mChooserController;
            disconnectCurrentController();
            if (controllerWrapper != null) {
                safeUpdateChooserIntent(controllerWrapper.controller, null);
            }
        }

        private void doRegisterChooserController(@Nullable IChooserController chooserController) {
            if (chooserController == null) {
                doOnClosed();
                return;
            }
            Log.d(
                    TAG,
                    "setIntentUpdater; isOpen: " + mIsActive
                            + ", chooserController: " + chooserController);
            if (!mIsActive) {
                // close Chooser
                safeUpdateChooserIntent(chooserController, null);
                return;
            }
            ChooserControllerWrapper controllerWrapper = mChooserController;
            if (controllerWrapper != null
                    && areEqual(controllerWrapper.controller, chooserController)) {
                return;
            }

            disconnectCurrentController();

            controllerWrapper = new ChooserControllerWrapper(chooserController);
            this.mChooserController = controllerWrapper;
            mChooserControllerLinkToDeath = createDeathRecipient(chooserController);
            try {
                chooserController.asBinder().linkToDeath(mChooserControllerLinkToDeath, 0);
                notifyChooserConnected(controllerWrapper);
            } catch (RemoteException e) {
                // binder has already died
                this.mChooserController = null;
                mChooserControllerLinkToDeath = null;
            }
        }

        @MainThread
        private void disconnectCurrentController() {
            ChooserControllerWrapper controllerWrapper = mChooserController;
            DeathRecipient linkToDeath = mChooserControllerLinkToDeath;
            mChooserController = null;
            mChooserControllerLinkToDeath = null;
            if (controllerWrapper != null && linkToDeath != null) {
                safeUnlinkToDeath(controllerWrapper.controller.asBinder(), linkToDeath);
            }
        }

        private IBinder.DeathRecipient createDeathRecipient(IChooserController chooserController) {
            return () -> {
                Log.d(TAG, "chooser died");
                mHandler.post(() -> {
                    ChooserControllerWrapper controllerWrapper = mChooserController;
                    if (areEqual(
                            controllerWrapper == null ? null : controllerWrapper.controller,
                            chooserController)) {
                        mChooserController = null;
                        mChooserControllerLinkToDeath = null;
                        mIsActive = false;
                        notifySessionClosed();
                    }
                });
            };
        }

        private void doOnSizeChanged(Rect size) {
            ChooserSessionUpdateListener listener = mListener;
            if (listener != null) {
                listener.onSizeChanged(size);
            }
        }

        private void doOnClosed() {
            mIsActive = false;
            disconnectCurrentController();
            notifySessionClosed();
        }

        private void notifyChooserConnected(ChooserController chooserController) {
            ChooserSessionUpdateListener listener = mListener;
            if (listener != null) {
                listener.onChooserConnected(chooserController);
            }
        }

        private void notifySessionClosed() {
            ChooserSessionUpdateListener listener = mListener;
            mListener = null;
            if (listener != null) {
                listener.onSessionClosed();
            }
        }

        private static void safeUpdateChooserIntent(
                IChooserController chooserController, @Nullable Intent chooserIntent) {
            try {
                chooserController.updateIntent(chooserIntent);
            } catch (RemoteException ignored) {
            }
        }

        private static void safeUnlinkToDeath(IBinder binder, IBinder.DeathRecipient linkToDeath) {
            try {
                binder.unlinkToDeath(linkToDeath, 0);
            } catch (Exception ignored) {
            }
        }

        private static boolean areEqual(
                @Nullable IChooserController left, @Nullable IChooserController right) {
            if (left == null && right == null) {
                return true;
            }
            if (left == null || right == null) {
                return false;
            }
            return left.asBinder().equals(right.asBinder());
        }
    }
}
