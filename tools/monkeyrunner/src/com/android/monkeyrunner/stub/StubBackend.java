package com.android.monkeyrunner.stub;

import com.android.monkeyrunner.MonkeyDevice;
import com.android.monkeyrunner.MonkeyManager;
import com.android.monkeyrunner.MonkeyRunnerBackend;

/**
 * This is a stub backend that doesn't do anything at all.  Useful for
 * running unit tests.
 */
public class StubBackend implements MonkeyRunnerBackend {

    public MonkeyManager createManager(String address, int port) {
        // We're stub - we've got nothing to do.
        return null;
    }

    public MonkeyDevice waitForConnection(long timeout, String deviceId) {
        // We're stub - we've got nothing to do.
        return null;
    }

    public void shutdown() {
        // We're stub - we've got nothing to do.
    }
}
