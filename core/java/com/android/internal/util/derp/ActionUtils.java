/*
 * Copyright (C) 2014 The TeamEos Project
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
 * 
 * Helper functions mostly for device configuration and some utilities
 * including a fun ViewGroup crawler and dpi conversion
 * 
 */

package com.android.internal.util.derp;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
//import android.content.res.ThemeConfig;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.provider.MediaStore;
import android.view.IWindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.android.internal.telephony.PhoneConstants;

import com.android.internal.statusbar.IStatusBarService;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public final class ActionUtils {

    public static final String INTENT_SCREENSHOT = "action_take_screenshot";
    public static final String INTENT_REGION_SCREENSHOT = "action_take_region_screenshot";

    private static IStatusBarService mStatusBarService = null;

    private static IStatusBarService getStatusBarService() {
        synchronized (ActionUtils.class) {
            if (mStatusBarService == null) {
                mStatusBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.getService("statusbar"));
            }
            return mStatusBarService;
        }
    }

    public static boolean hasNavbarByDefault(Context context) {
        boolean needsNav = context.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            needsNav = false;
        } else if ("0".equals(navBarOverride)) {
            needsNav = true;
        }
        return needsNav;
    }

/*
    public static boolean deviceSupportsLte(Context ctx) {
        final TelephonyManager tm = (TelephonyManager)
                ctx.getSystemService(Context.TELEPHONY_SERVICE);
//        return (tm.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE)
//                || tm.getLteOnGsmMode() != 0;
        return tm.getLteOnCdmaModeStatic() == PhoneConstants.LTE_ON_CDMA_TRUE;
    }
*/

    public static boolean deviceSupportsDdsSupported(Context context) {
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.isMultiSimEnabled()
                && tm.getMultiSimConfiguration() == TelephonyManager.MultiSimVariants.DSDA;
    }

    public static boolean deviceSupportsMobileData(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        Network[] networks = cm.getAllNetworks();
        for (int i = 0; i < networks.length; i++) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(networks[i]);
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true;
             }
        }
        return false;
    }

    public static boolean deviceSupportsBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public static boolean deviceSupportsNfc(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    public static boolean deviceSupportsFlashLight(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null
                        && flashAvailable
                        && lensFacing != null
                        && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true;
                }
            }
        } catch (CameraAccessException | AssertionError e) {
            // Ignore
        }
        return false;
    }

    public static boolean deviceSupportsCompass(Context context) {
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                && sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
    }

    // Launch camera
    public static void launchCamera(Context context) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    // Launch voice search
    public static void launchVoiceSearch(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // Screen off
    public static void switchScreenOff(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm!= null && pm.isScreenOn()) {
            pm.goToSleep(SystemClock.uptimeMillis());
        }
    }

    // Screen on
    public static void switchScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        pm.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
    }

    // Volume panel
    public static void toggleVolumePanel(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    // Toggle flashlight
    public static void toggleCameraFlash() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.toggleCameraFlash();
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Clear notifications
    public static void clearAllNotifications() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.onClearAllNotifications(ActivityManager.getCurrentUser());
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Toggle notifications panel
    public static void toggleNotifications() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.togglePanel();
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Toggle qs panel
    public static void toggleQsPanel() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.toggleSettingsPanel();
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Cycle ringer modes
    public static void toggleRingerModes (Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Vibrator mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (mVibrator.hasVibrator()) {
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                }
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
        }
    }

    // Trigger Hush Mute mode
    public static void triggerHushMute(Context context) {
        // We can't call AudioService#silenceRingerModeInternal from here, so this is a partial copy of it
        int silenceRingerSetting = Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_OFF,
                UserHandle.USER_CURRENT);

        int ringerMode;
        int toastText;
        if (silenceRingerSetting == Settings.Secure.VOLUME_HUSH_VIBRATE) {
            ringerMode = AudioManager.RINGER_MODE_VIBRATE;
            toastText = com.android.internal.R.string.volume_dialog_ringer_guidance_vibrate;
        } else {
            // VOLUME_HUSH_MUTE and VOLUME_HUSH_OFF
            ringerMode = AudioManager.RINGER_MODE_SILENT;
            toastText = com.android.internal.R.string.volume_dialog_ringer_guidance_silent;
        }
        AudioManager audioMan = (AudioManager)
                context.getSystemService(Context.AUDIO_SERVICE);
        audioMan.setRingerModeInternal(ringerMode);
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
    }
}
