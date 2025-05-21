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
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.concurrent.GuardedBy;

/**
 * <p>A class that represents an interactive Chooser session.</p>
 * <p>An instance of the class can be used as a value for <em>an</em> {@link Intent#ACTION_CHOOSER}
 * extra to establish a bi-directional communication channel with Chooser.
 * <p>A {@link UpdateListener} callback can be used to receive updates about the
 * session and communication from Chooser.</p>
 */
public final class ChooserSession {

    /**
     * A callback interface for Chooser session state updates.
     */
    public interface UpdateListener {

        /**
         * Gets invoked when a {@link ChooserController} becomes available.
         * @param chooserController active chooser controller.
         */
        void onChooserConnected(ChooserController chooserController);

        /**
         * Gets invoked when the session is closed by the Chooser.
         */
        void onClosed();

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

        /**
         * Collapses Chooser to temporary yield more screen space for the app.
         * Chooser will stay collapsed until its first user interaction.
         */
        void collapse() throws RemoteException;

        /**
         * Sets whether Chooser targets should be enabled.
         * <p>
         * This method is primarily intended to allow for managing a transient state,
         * particularly useful during long-running operations. By disabling targets,
         * launching application can prevent unintended interactions.
         */
        void setTargetsEnabled(boolean isEnabled) throws RemoteException;
    }

    /**
     * @hide
     */
    public static final String EXTRA_CHOOSER_SESSION =
            "com.android.extra.EXTRA_CHOOSER_INTERACTIVE_CALLBACK";

    private static final String TAG = "ChooserSession";

    private final ChooserSessionImpl mChooserSession = new ChooserSessionImpl();

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
    public void start(@NonNull Context context, @NonNull Intent chooserIntent) {
        if (context == null) {
            throw new IllegalArgumentException("context should not be null");
        }
        if (chooserIntent == null) {
            throw new IllegalArgumentException("chooserIntent should not be null");
        }
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
     * (see {@link #close()}) or closed by the Chooser.
     */
    public boolean isActive() {
        return mChooserSession.isActive();
    }

    /**
     * Cancel the session and close the Chooser.
     */
    public void close() {
        mChooserSession.close();
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
    public void addUpdateListener(
            @NonNull Executor executor, @NonNull UpdateListener listener) {
        if (executor == null) {
            throw new IllegalArgumentException("executor should not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener should not be null");
        }
        mChooserSession.addUpdateListener(executor, listener);
    }

    /**
     * Removes a previously added UpdateListener callback.
     */
    public void removeUpdateListener(@NonNull UpdateListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener should not be null");
        }
        mChooserSession.removeUpdateListener(listener);
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

        @Override
        public void collapse() throws RemoteException {
            // TODO: implement
        }

        @Override
        public void setTargetsEnabled(boolean isEnabled) throws RemoteException {
            // TODO: implement
        }
    }

    private static class ChooserSessionImpl extends IChooserControllerCallback.Stub {
        private final Object mListenerLock = new Object();
        @GuardedBy("mListenerLock")
        private Map<UpdateListener, UpdateListenerWrapper> mListenerMap = new HashMap<>();
        private final AtomicBoolean mIsActive = new AtomicBoolean(true);

        private final Object mControllerLock = new Object();

        @GuardedBy("mControllerLock")
        @Nullable
        private volatile ChooserControllerWrapper mChooserController;

        @GuardedBy("mControllerLock")
        @Nullable
        private volatile IBinder.DeathRecipient mChooserControllerLinkToDeath;

        @Override
        public void registerChooserController(
                @Nullable final IChooserController chooserController) {
            if (chooserController == null) {
                // Interaction session did not start.
                onClosed();
                return;
            }
            Log.d(
                    TAG,
                    "setIntentUpdater; isOpen: " + mIsActive
                            + ", chooserController: " + chooserController);
            if (!mIsActive.get()) {
                // close Chooser
                safeUpdateChooserIntent(chooserController, null);
                return;
            }
            ChooserControllerWrapper controllerWrapper;
            synchronized (mControllerLock) {
                if (areEqual(mChooserController, chooserController)) {
                    return;
                }
                disconnectCurrentController();
                controllerWrapper = connectController(chooserController);
            }
            if (controllerWrapper == null) {
                // we've got a binder that had died, notify session closed
                onClosed();
            } else {
                notifyListeners((listener -> {
                    if (mIsActive.get()) {
                        listener.onChooserConnected(controllerWrapper);
                    }
                }));
            }
        }

        @Override
        public void onSizeChanged(Rect size) {
            if (mIsActive.get()) {
                notifyListeners((listener) -> {
                    if (mIsActive.get()) {
                        listener.onSizeChanged(size);
                    }
                });
            }
        }

        @Override
        public void onClosed() {
            doClose(true);
        }

        public boolean isActive() {
            return mIsActive.get();
        }

        public void close() {
            doClose(false);
        }

        @Nullable
        public ChooserController getChooserController() {
            synchronized (mControllerLock) {
                return mChooserController;
            }
        }

        public void addUpdateListener(Executor executor, UpdateListener listener) {
            synchronized (mListenerLock) {
                if (!mListenerMap.containsKey(listener)) {
                    mListenerMap = new HashMap<>(mListenerMap);
                    mListenerMap.put(listener, new UpdateListenerWrapper(listener, executor));
                }
            }
        }

        public void removeUpdateListener(UpdateListener listener) {
            synchronized (mListenerLock) {
                if (mListenerMap.containsKey(listener)) {
                    mListenerMap = new HashMap<>(mListenerMap);
                    UpdateListenerWrapper lw = mListenerMap.remove(listener);
                    lw.isSubscribed.set(false);
                }
            }
        }

        private void notifyListeners(Consumer<UpdateListener> block) {
            Collection<UpdateListenerWrapper> listeners;
            synchronized (mListenerLock) {
                listeners = mListenerMap.values();
            }
            for (UpdateListenerWrapper lw: listeners) {
                lw.executor.execute(() -> {
                    if (lw.isSubscribed.get()) {
                        block.accept(lw.listener);
                    }
                });
            }
        }

        private void doClose(boolean isClosedByChooser) {
            boolean wasActive = mIsActive.compareAndSet(true, false);
            synchronized (mControllerLock) {
                if (!isClosedByChooser && mChooserController != null) {
                    safeUpdateChooserIntent(mChooserController.controller, null);
                }
                disconnectCurrentController();
            }
            if (wasActive && isClosedByChooser) {
                notifyListeners((UpdateListener::onClosed));
            }
            synchronized (mListenerLock) {
                mListenerMap = Collections.emptyMap();
            }
        }

        @GuardedBy("mControllerLock")
        private void disconnectCurrentController() {
            if (mChooserController != null && mChooserControllerLinkToDeath != null) {
                safeUnlinkToDeath(
                        mChooserController.controller.asBinder(), mChooserControllerLinkToDeath);
            }
            mChooserController = null;
            mChooserControllerLinkToDeath = null;
        }

        @GuardedBy("mControllerLock")
        private ChooserControllerWrapper connectController(IChooserController chooserController) {
            ChooserControllerWrapper controllerWrapper =
                    new ChooserControllerWrapper(chooserController);
            this.mChooserController = controllerWrapper;
            mChooserControllerLinkToDeath = createDeathRecipient(chooserController);
            try {
                chooserController.asBinder().linkToDeath(mChooserControllerLinkToDeath, 0);
            } catch (RemoteException e) {
                // binder has already died
                mChooserController = null;
                mChooserControllerLinkToDeath = null;
                controllerWrapper = null;
            }
            return controllerWrapper;
        }

        private IBinder.DeathRecipient createDeathRecipient(IChooserController chooserController) {
            return () -> {
                Log.d(TAG, "chooser died");
                boolean shouldClose = false;
                synchronized (mControllerLock) {
                    if (areEqual(mChooserController, chooserController)) {
                        // is it ever true?
                        mChooserController = null;
                        mChooserControllerLinkToDeath = null;
                        shouldClose = true;
                    }
                }
                if (shouldClose) {
                    doClose(true);
                }
            };
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
                @Nullable ChooserControllerWrapper wrapper, @Nullable IChooserController right) {
            IChooserController left = wrapper == null ? null : wrapper.controller;
            if (left == null && right == null) {
                return true;
            }
            if (left == null || right == null) {
                return false;
            }
            return left.asBinder().equals(right.asBinder());
        }
    }

    private static class UpdateListenerWrapper {
        public final ChooserSession.UpdateListener listener;
        public final Executor executor;
        public final AtomicBoolean isSubscribed = new AtomicBoolean(true);

        UpdateListenerWrapper(ChooserSession.UpdateListener listener, Executor executor) {
            this.listener = listener;
            this.executor = executor;
        }
    }
}
