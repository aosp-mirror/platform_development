/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.intentresolver.IChooserController;
import com.android.intentresolver.IChooserInteractiveSessionCallback;

/**
 * <p>An interactive Chooser session API candidate.</p>
 * <p>A class that represents an interactive Chooser session.</p>
 * <p>An instance of the class should be put in an argument to a chooser intent.
 * <p>A {@link ChooserSessionUpdateListener} callback can be used to receive updates about the
 * session and communication from Chooser.</p>
 */
public final class ChooserSession implements Parcelable {

    private static final String TAG = "ChooserSession";

    private final IChooserInteractiveSessionCallback mSessionCallbackBinder;

    // mChooserSession is expected to be null only on the Chooser side
    @Nullable
    private final ChooserSessionImpl mChooserSession;

    /**
     * An alias for {@code ChooserSession(Looper.getMainLooper())}.
     */
    public ChooserSession() {
        this(new Handler(Looper.getMainLooper()));
    }

    /**
     * @param handler a thread {@link ChooserSessionUpdateListener} callbacks will be delivered on.
     */
    public ChooserSession(Handler handler) {
        this(new ChooserSessionImpl(handler));
    }

    private ChooserSession(IChooserInteractiveSessionCallback sessionBinder) {
        mSessionCallbackBinder = sessionBinder;
        mChooserSession = (sessionBinder instanceof ChooserSessionImpl)
                ? (ChooserSessionImpl) sessionBinder
                : null;
    }

    /**
     * @return true if the session is active: i.e. is not being cancelled by the client
     * (see {@link #cancel()}) or closed by the Chooser.
     */
    public boolean isActive() {
        return mChooserSession != null && mChooserSession.isActive();
    }

    /**
     * Cancel the session and close the Chooser.
     */
    public void cancel() {
        if (mChooserSession != null) {
            mChooserSession.cancel();
        }
    }

    /**
     * Should be a @Hidden API for Chooser to get access to the binder.
     */
    IChooserInteractiveSessionCallback getSessionCallbackBinder() {
        return mSessionCallbackBinder;
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
        return mChooserSession == null ? null : mChooserSession.getChooserController();
    }

    /**
     * @param listener make sure that the callback is cleared at the end of a component's lifecycle
     * (e.g. Activity) or provide a properly maintained WeakReference wrapper to avoid memory leaks.
     */
    public void setChooserStateListener(@Nullable ChooserSessionUpdateListener listener) {
        if (mChooserSession != null) {
            mChooserSession.setChooserStateListener(
                    listener == null
                            ? null
                            : new ChooserSessionUpdateListenerWrapper(this, listener));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        if (mChooserSession != null) {
            synchronized (mChooserSession) {
                dest.writeStrongBinder(mChooserSession);
            }
        }
    }

    public static final Parcelable.Creator<ChooserSession> CREATOR = new Creator<>() {
                @Override
                public ChooserSession createFromParcel(Parcel source) {
                    IChooserInteractiveSessionCallback binder =
                            IChooserInteractiveSessionCallback.Stub.asInterface(
                                    source.readStrongBinder());
                    return binder == null ? null : new ChooserSession(binder);
                }

                @Override
                public ChooserSession[] newArray(int size) {
                    return new ChooserSession[size];
                }
            };

    /**
     * A callback interface for Chooser session state updates.
     */
    public interface ChooserSessionUpdateListener {

        /**
         * Gets invoked when a {@link ChooserController} becomes available.
         * @param session a reference this callback is registered to.
         * @param chooserController active chooser controller.
         */
        void onChooserConnected(ChooserSession session, ChooserController chooserController);

        /**
         * Gets invoked when a {@link ChooserController} becomes unavailable.
         */
        void onChooserDisconnected(ChooserSession session);

        /**
         * Gets invoked when the session is closed by the Chooser.
         */
        void onSessionClosed(ChooserSession session);

        /**
         * A temporary support method; expected to be replaced by some other WindowManager API.
         */
        void onDrawerVerticalOffsetChanged(ChooserSession session, int offset);
    }

    /**
     * An interface for updating the Chooser.
     */
    public interface ChooserController {

        /**
         * Update chooser intent in a Chooser session.
         */
        // TODO: list all the updatable parameters in the javadoc.
        void updateIntent(Intent intent) throws RemoteException;
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

    private static class ChooserSessionUpdateListenerWrapper {
        private final ChooserSession mSession;
        private final ChooserSessionUpdateListener mListener;

        ChooserSessionUpdateListenerWrapper(
                ChooserSession mSession, ChooserSessionUpdateListener mListener) {
            this.mSession = mSession;
            this.mListener = mListener;
        }

        public void onChooserConnected(ChooserController chooserController) {
            mListener.onChooserConnected(mSession, chooserController);
        }

        public void onChooserDisconnected() {
            mListener.onChooserDisconnected(mSession);
        }

        public void onSessionClosed() {
            mListener.onSessionClosed(mSession);
        }

        public void onDrawerVerticalOffsetChanged(int offset) {
            mListener.onDrawerVerticalOffsetChanged(mSession, offset);
        }
    }

    private static class ChooserSessionImpl extends IChooserInteractiveSessionCallback.Stub {
        private final Handler mHandler;
        private volatile ChooserSessionUpdateListenerWrapper mListener;
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
            mHandler.post(() -> setChooserController(chooserController));
        }

        @Override
        public void onDrawerVerticalOffsetChanged(int offset) {
            mHandler.post(() -> notifyDrawerVerticalOffsetChanged(offset));
        }

        public boolean isActive() {
            return mIsActive;
        }

        public void cancel() {
            mIsActive = false;
            mListener = null;
            if (mHandler.getLooper().isCurrentThread()) {
                doClose();
            } else {
                mHandler.post(this::doClose);
            }
        }

        @Nullable
        public ChooserController getChooserController() {
            return mChooserController;
        }

        public void setChooserStateListener(
                @Nullable ChooserSessionUpdateListenerWrapper listener) {
            mListener = listener;
            publishState();
        }

        private void publishState() {
            if (mHandler.getLooper().isCurrentThread()) {
                if (!mIsActive) {
                    notifySessionClosed();
                } else if (mChooserController == null) {
                    notifyChooserDisconnected();
                } else {
                    notifyChooserConnected(mChooserController);
                }
            } else {
                mHandler.post(this::publishState);
            }
        }

        private void doClose() {
            ChooserControllerWrapper controllerWrapper = mChooserController;
            if (controllerWrapper != null) {
                if (mChooserControllerLinkToDeath != null) {
                    safeUnlinkToDeath(
                            controllerWrapper.controller.asBinder(), mChooserControllerLinkToDeath);
                }
                safeUpdateChooserIntent(controllerWrapper.controller, null);
            }
            mChooserController = null;
            mChooserControllerLinkToDeath = null;
        }

        private void setChooserController(IChooserController chooserController) {
            Log.d(
                    TAG,
                    "setIntentUpdater; isOpen: " + mIsActive
                            + ", chooserController: " + chooserController);
            if (!mIsActive && chooserController != null) {
                // close Chooser
                safeUpdateChooserIntent(chooserController, null);
                return;
            }
            ChooserControllerWrapper controllerWrapper = mChooserController;
            if (controllerWrapper != null
                    && areEqual(controllerWrapper.controller, chooserController)) {
                return;
            }

            disconnectCurrentIntentUpdater();

            if (chooserController != null) {
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
            } else {
                mIsActive = false;
                notifySessionClosed();
            }
        }

        @MainThread
        private void disconnectCurrentIntentUpdater() {
            ChooserControllerWrapper controllerWrapper = mChooserController;
            if (controllerWrapper != null) {
                if (mChooserControllerLinkToDeath != null) {
                    safeUnlinkToDeath(
                            controllerWrapper.controller.asBinder(), mChooserControllerLinkToDeath);
                }
                mChooserController = null;
                mChooserControllerLinkToDeath = null;
                notifyChooserDisconnected();
            }
        }

        private DeathRecipient createDeathRecipient(IChooserController chooserController) {
            return () -> {
                Log.d(TAG, "chooser died");
                mHandler.post(() -> {
                    ChooserControllerWrapper controllerWrapper = this.mChooserController;
                    if (areEqual(
                            controllerWrapper == null ? null : controllerWrapper.controller,
                            chooserController)) {
                        this.mChooserController = null;
                        mChooserControllerLinkToDeath = null;
                        mListener.onChooserDisconnected();
                    }
                });
            };
        }

        private void notifyDrawerVerticalOffsetChanged(int offset) {
            ChooserSessionUpdateListenerWrapper listener = mListener;
            if (listener != null) {
                listener.onDrawerVerticalOffsetChanged(offset);
            }
        }

        private void notifyChooserConnected(ChooserController chooserController) {
            ChooserSessionUpdateListenerWrapper listener = mListener;
            if (listener != null) {
                listener.onChooserConnected(chooserController);
            }
        }

        private void notifySessionClosed() {
            ChooserSessionUpdateListenerWrapper listener = mListener;
            if (listener != null) {
                listener.onSessionClosed();
            }
        }

        private void notifyChooserDisconnected() {
            ChooserSessionUpdateListenerWrapper listener = mListener;
            if (listener != null) {
                listener.onChooserDisconnected();
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
