/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.host;

import static android.companion.virtual.VirtualDeviceParams.DEVICE_POLICY_CUSTOM;
import static android.companion.virtual.VirtualDeviceParams.DEVICE_POLICY_DEFAULT;
import static android.companion.virtual.VirtualDeviceParams.LOCK_STATE_ALWAYS_UNLOCKED;
import static android.companion.virtual.VirtualDeviceParams.POLICY_TYPE_AUDIO;
import static android.companion.virtual.VirtualDeviceParams.POLICY_TYPE_CLIPBOARD;
import static android.companion.virtual.VirtualDeviceParams.POLICY_TYPE_RECENTS;
import static android.companion.virtual.VirtualDeviceParams.POLICY_TYPE_SENSORS;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.role.RoleManager;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.CompanionDeviceManager;
import android.companion.virtual.VirtualDeviceManager;
import android.companion.virtual.VirtualDeviceManager.ActivityListener;
import android.companion.virtual.VirtualDeviceParams;
import android.companion.virtual.sensor.VirtualSensorConfig;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import com.example.android.vdmdemo.common.ConnectionManager;
import com.example.android.vdmdemo.common.RemoteEventProto.DeviceCapabilities;
import com.example.android.vdmdemo.common.RemoteEventProto.DisplayChangeEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.SensorCapabilities;
import com.example.android.vdmdemo.common.RemoteEventProto.StartStreaming;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.common.util.concurrent.MoreExecutors;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Objects;
import java.util.function.Consumer;
import javax.inject.Inject;

/**
 * VDM Host service, streaming apps to a remote device and processing the input coming from there.
 */
@AndroidEntryPoint(Service.class)
public final class VdmService extends Hilt_VdmService {

  public static final String TAG = "VdmHost";

  private static final String CHANNEL_ID =
          "com.example.android.vdmdemo.host.VdmService";
  private static final int NOTIFICATION_ID = 1;

  private static final String ACTION_STOP =
          "com.example.android.vdmdemo.host.VdmService.STOP";

  public static final String ACTION_LOCKDOWN =
          "com.example.android.vdmdemo.host.VdmService.LOCKDOWN";

  /** Provides an instance of this service to bound clients. */
  public class LocalBinder extends Binder {
    VdmService getService() {
      return VdmService.this;
    }
  }

  private final IBinder binder = new LocalBinder();

  @Inject ConnectionManager connectionManager;
  @Inject RemoteIo remoteIo;
  @Inject AudioStreamer audioStreamer;
  @Inject Settings settings;
  @Inject DisplayRepository displayRepository;

  private RemoteSensorManager remoteSensorManager = null;

  private final Consumer<RemoteEvent> remoteEventConsumer = this::processRemoteEvent;
  private VirtualDeviceManager.VirtualDevice virtualDevice;
  private DeviceCapabilities deviceCapabilities;
  private Intent pendingRemoteIntent = null;
  private boolean pendingMirroring = false;
  private boolean pendingHome = false;
  private DisplayManager displayManager;

  private final DisplayManager.DisplayListener displayListener =
      new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
          displayRepository.onDisplayChanged(displayId);
        }
      };

  private final ConnectionManager.ConnectionCallback connectionCallback =
      new ConnectionManager.ConnectionCallback() {
        @Override
        public void onConnected(String remoteDeviceName) {}

        @Override
        public void onDisconnected() {
          deviceCapabilities = null;
          closeVirtualDevice();
        }
      };

  public VdmService() {}

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && ACTION_STOP.equals(intent.getAction())) {
      Log.i(TAG, "Stopping VDM Service.");
      connectionManager.disconnect();
      stopForeground(STOP_FOREGROUND_REMOVE);
      stopSelf();
      return START_NOT_STICKY;
    }

    if (intent != null && ACTION_LOCKDOWN.equals(intent.getAction())) {
      lockdown();
      return START_STICKY;
    }

    NotificationChannel notificationChannel =
        new NotificationChannel(
            CHANNEL_ID, "VDM Service Channel", NotificationManager.IMPORTANCE_LOW);
    notificationChannel.enableVibration(false);
    NotificationManager notificationManager = getSystemService(NotificationManager.class);
    notificationManager.createNotificationChannel(notificationChannel);

    Intent openIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntentOpen =
        PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);
    Intent stopIntent = new Intent(this, VdmService.class);
    stopIntent.setAction(ACTION_STOP);
    PendingIntent pendingIntentStop =
        PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

    Notification notification =
        new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.connected)
            .setContentTitle("VDM Demo running")
            .setContentText("Click to open")
            .setContentIntent(pendingIntentOpen)
            .addAction(
                new Notification.Action.Builder(R.drawable.close, "Stop", pendingIntentStop)
                        .build())
            .setOngoing(true)
            .build();
    startForeground(NOTIFICATION_ID, notification);

    return START_STICKY;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    connectionManager.addConnectionCallback(connectionCallback);

    displayManager = getSystemService(DisplayManager.class);
    displayManager.registerDisplayListener(displayListener, null);

    remoteIo.addMessageConsumer(remoteEventConsumer);

    if (settings.audioEnabled) {
      audioStreamer.start();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    connectionManager.removeConnectionCallback(connectionCallback);
    closeVirtualDevice();
    remoteIo.removeMessageConsumer(remoteEventConsumer);
    displayManager.unregisterDisplayListener(displayListener);
    audioStreamer.close();
  }

  private void processRemoteEvent(RemoteEvent event) {
    if (event.hasDeviceCapabilities()) {
      Log.i(TAG, "Host received device capabilities");
      deviceCapabilities = event.getDeviceCapabilities();
      associateAndCreateVirtualDevice();
    } else if (event.hasDisplayCapabilities() && !displayRepository.resetDisplay(event)) {
      RemoteDisplay remoteDisplay =
          new RemoteDisplay(
              this,
              event,
              virtualDevice,
              remoteIo,
              pendingHome,
              pendingMirroring,
              settings);
      displayRepository.addDisplay(remoteDisplay);
      pendingMirroring = false;
      pendingHome = false;
      if (pendingRemoteIntent != null) {
        remoteDisplay.launchIntent(
            PendingIntent.getActivity(this, 0, pendingRemoteIntent, PendingIntent.FLAG_IMMUTABLE));
      }
    } else if (event.hasStopStreaming() && !event.getStopStreaming().getPause()) {
      displayRepository.removeDisplayByRemoteId(event.getDisplayId());
    }
  }

  private void associateAndCreateVirtualDevice() {
    CompanionDeviceManager cdm = getSystemService(CompanionDeviceManager.class);
    RoleManager rm = getSystemService(RoleManager.class);
    final String deviceProfile =
        settings.deviceStreaming
            ? AssociationRequest.DEVICE_PROFILE_NEARBY_DEVICE_STREAMING
            : AssociationRequest.DEVICE_PROFILE_APP_STREAMING;
    for (AssociationInfo associationInfo : cdm.getMyAssociations()) {
      // Flashing the device clears the role and the permissions, but not the CDM associations.
      // TODO(b/290596625): Remove the workaround to clear the associations if the role is not held.
      if (!rm.isRoleHeld(deviceProfile)) {
        cdm.disassociate(associationInfo.getId());
      } else if (Objects.equals(associationInfo.getPackageName(), getPackageName())
          && Objects.equals(associationInfo.getDisplayName().toString(),
              deviceCapabilities.getDeviceName().toString())) {
        createVirtualDevice(associationInfo);
        return;
      }
    }

    @SuppressLint("MissingPermission")
    AssociationRequest.Builder associationRequest =
        new AssociationRequest.Builder()
            .setDeviceProfile(deviceProfile)
            .setDisplayName(deviceCapabilities.getDeviceName())
            .setSelfManaged(true);
    cdm.associate(
        associationRequest.build(),
        new CompanionDeviceManager.Callback() {
          @Override
          public void onAssociationPending(IntentSender intentSender) {
            try {
              startIntentSender(intentSender, null, 0, 0, 0);
            } catch (SendIntentException e) {
              Log.e(TAG, "onAssociationPending: Failed to send device selection intent", e);
            }
          }

          @Override
          public void onAssociationCreated(AssociationInfo associationInfo) {
            Log.i(TAG, "onAssociationCreated: ID " + associationInfo.getId());
            createVirtualDevice(associationInfo);
          }

          @Override
          public void onFailure(CharSequence error) {
            Log.e(TAG, "onFailure: RemoteDevice Association failed " + error);
          }
        },
        null);
    Log.i(TAG, "createCdmAssociation: Waiting for association to happen");
  }

  private void createVirtualDevice(AssociationInfo associationInfo) {
    VirtualDeviceManager vdm = getSystemService(VirtualDeviceManager.class);

    VirtualDeviceParams.Builder virtualDeviceBuilder =
        new VirtualDeviceParams.Builder()
            .setName("VirtualDevice - " + deviceCapabilities.getDeviceName())
            .setDevicePolicy(POLICY_TYPE_AUDIO, DEVICE_POLICY_CUSTOM)
            .setAudioPlaybackSessionId(audioStreamer.getPlaybackSessionId());

    if (settings.alwaysUnlocked) {
      virtualDeviceBuilder.setLockState(LOCK_STATE_ALWAYS_UNLOCKED);
    }

    if (settings.customHome) {
      virtualDeviceBuilder.setHomeComponent(new ComponentName(this, CustomLauncherActivity.class));
    }

    if (!settings.includeInRecents) {
      virtualDeviceBuilder.setDevicePolicy(POLICY_TYPE_RECENTS, DEVICE_POLICY_CUSTOM);
    }

    if (settings.crossDeviceClipboardEnabled) {
      virtualDeviceBuilder.setDevicePolicy(POLICY_TYPE_CLIPBOARD, DEVICE_POLICY_CUSTOM);
    }

    if (settings.sensorsEnabled) {
      for (SensorCapabilities sensor : deviceCapabilities.getSensorCapabilitiesList()) {
        virtualDeviceBuilder.addVirtualSensorConfig(
            new VirtualSensorConfig.Builder(sensor.getType(), "Remote-" + sensor.getName())
                .setMinDelay(sensor.getMinDelayUs())
                .setMaxDelay(sensor.getMaxDelayUs())
                .setPower(sensor.getPower())
                .setResolution(sensor.getResolution())
                .setMaximumRange(sensor.getMaxRange())
                .build());
      }

      if (deviceCapabilities.getSensorCapabilitiesCount() > 0) {
        remoteSensorManager = new RemoteSensorManager(remoteIo);
        virtualDeviceBuilder
            .setDevicePolicy(POLICY_TYPE_SENSORS, DEVICE_POLICY_CUSTOM)
            .setVirtualSensorCallback(
                MoreExecutors.directExecutor(), remoteSensorManager.getVirtualSensorCallback());
      }
    }

    virtualDevice = vdm.createVirtualDevice(associationInfo.getId(), virtualDeviceBuilder.build());
    if (remoteSensorManager != null) {
      remoteSensorManager.setVirtualSensors(virtualDevice.getVirtualSensorList());
    }

    virtualDevice.setShowPointerIcon(settings.showPointerIcon);

    virtualDevice.addActivityListener(
        MoreExecutors.directExecutor(),
        new ActivityListener() {

          @Override
          public void onTopActivityChanged(int displayId, ComponentName componentName) {
            int remoteDisplayId = displayRepository.getRemoteDisplayId(displayId);
            if (remoteDisplayId == Display.INVALID_DISPLAY) {
              return;
            }
            String title = "";
            try {
              ActivityInfo activityInfo = getPackageManager().getActivityInfo(componentName, 0);
              title = activityInfo.loadLabel(getPackageManager()).toString();
            } catch (NameNotFoundException e) {
              Log.w(TAG, "Failed to get activity label for " + componentName);
            }
            remoteIo.sendMessage(
                RemoteEvent.newBuilder()
                    .setDisplayId(remoteDisplayId)
                    .setDisplayChangeEvent(DisplayChangeEvent.newBuilder().setTitle(title))
                    .build());
          }

          @Override
          public void onDisplayEmpty(int displayId) {
            Log.i(TAG, "Display " + displayId + " is empty, removing");
            displayRepository.removeDisplay(displayId);
          }
        });
    virtualDevice.addActivityListener(
        MoreExecutors.directExecutor(),
        new RunningVdmUidsTracker(getApplicationContext(), audioStreamer));

    Log.i(TAG, "Created virtual device");
  }

  private void lockdown() {
    Log.i(TAG, "Initiating Lockdown.");
    displayRepository.clear();
  }

  private synchronized void closeVirtualDevice() {
    if (remoteSensorManager != null) {
      remoteSensorManager.close();
      remoteSensorManager = null;
    }
    if (virtualDevice != null) {
      Log.i(TAG, "Closing virtual device");
      displayRepository.clear();
      virtualDevice.close();
      virtualDevice = null;
    }
  }

  public int[] getRemoteDisplayIds() {
    return displayRepository.getRemoteDisplayIds();
  }

  public void startStreamingHome() {
    pendingRemoteIntent = null;
    pendingHome = true;
    if (settings.immersiveMode) {
      displayRepository.clear();
    }
    remoteIo.sendMessage(
        RemoteEvent.newBuilder()
            .setStartStreaming(
                StartStreaming.newBuilder()
                    .setHomeEnabled(true)
                    .setImmersive(settings.immersiveMode))
            .build());
  }

  public void startMirroring() {
    pendingRemoteIntent = null;
    pendingMirroring = true;
    remoteIo.sendMessage(
        RemoteEvent.newBuilder().setStartStreaming(StartStreaming.newBuilder()).build());
  }

  public void startStreaming(Intent intent) {
    pendingRemoteIntent = intent;
    remoteIo.sendMessage(
        RemoteEvent.newBuilder()
            .setStartStreaming(StartStreaming.newBuilder().setImmersive(settings.immersiveMode))
            .build());
  }

  public void startIntentOnDisplayIndex(Intent intent, int displayIndex) {
    PendingIntent pendingIntent =
        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    displayRepository.getDisplayByIndex(displayIndex).ifPresent(d -> d.launchIntent(pendingIntent));
  }

  public void setDisplayRotationEnabled(boolean enabled) {
    settings.displayRotationEnabled = enabled;
  }

  public void setSensorsEnabled(boolean enabled) {
    recreateVirtualDevice(() -> settings.sensorsEnabled = enabled);
  }

  public void setIncludeInRecents(boolean include) {
    settings.includeInRecents = include;
    if (virtualDevice != null) {
      virtualDevice.setDevicePolicy(
          POLICY_TYPE_RECENTS, include ? DEVICE_POLICY_DEFAULT : DEVICE_POLICY_CUSTOM);
    }
  }

  public void setCrossDeviceClipboardEnabled(boolean enabled) {
    settings.crossDeviceClipboardEnabled = enabled;
    if (virtualDevice != null) {
      virtualDevice.setDevicePolicy(
          POLICY_TYPE_CLIPBOARD, enabled ? DEVICE_POLICY_CUSTOM : DEVICE_POLICY_DEFAULT);
    }
  }

  public void setAlwaysUnlocked(boolean enabled) {
    recreateVirtualDevice(() -> settings.alwaysUnlocked = enabled);
  }

  public void setDeviceStreaming(boolean enabled) {
    recreateVirtualDevice(() -> settings.deviceStreaming = enabled);
  }

  public void setRecordEncoderOutput(boolean enabled) {
    recreateVirtualDevice(() -> settings.recordEncoderOutput = enabled);
  }

  public void setShowPointerIcon(boolean enabled) {
    settings.showPointerIcon = enabled;
    if (virtualDevice != null) {
      virtualDevice.setShowPointerIcon(enabled);
    }
  }

  public void setAudioEnabled(boolean enabled) {
    settings.audioEnabled = enabled;
    if (enabled) {
      audioStreamer.start();
    } else {
      audioStreamer.stop();
    }
  }

  public void setImmersiveMode(boolean enabled) {
    recreateVirtualDevice(() -> settings.immersiveMode = enabled);
  }

  public void setCustomHome(boolean enabled) {
    recreateVirtualDevice(() -> settings.customHome = enabled);
  }

  private interface DeviceSettingsChange {
    void apply();
  }

  private void recreateVirtualDevice(DeviceSettingsChange settingsChange) {
    if (virtualDevice != null) {
      closeVirtualDevice();
    }
    settingsChange.apply();
    if (deviceCapabilities != null) {
      associateAndCreateVirtualDevice();
    }
  }
}
