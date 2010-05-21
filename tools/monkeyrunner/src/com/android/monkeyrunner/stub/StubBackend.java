package com.android.monkeyrunner.stub;

import com.android.monkeyrunner.MonkeyDevice;
import com.android.monkeyrunner.MonkeyManager;
import com.android.monkeyrunner.MonkeyRunnerBackend;

public class StubBackend implements MonkeyRunnerBackend {

    public MonkeyManager createManager(String address, int port) {
        // TODO Auto-generated method stub
        return null;
    }

    public MonkeyDevice waitForConnection(long timeout, String deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    public void shutdown() {
        // We're stub - we've got nothing to do.
    }
}
