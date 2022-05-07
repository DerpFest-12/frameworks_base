/*
 * Copyright (C) 2022 Nameless-AOSP Project
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

package com.android.server.app;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static android.os.UserHandle.USER_SYSTEM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import com.android.internal.util.derp.derpUtils.LauncherUtils;

import com.android.server.ServiceThread;
import com.android.server.SystemService;

public final class CustomLauncherService extends SystemService {

    private static final String LAWNCHAIR_OVERLAY_PKG_NAME = "com.android.launcher.recentsComponent.overlay";
    private static final String LAWNCHAIR_PKG_NAME = "app.lawnchair";
    private static final String DERP_LAUNCHER_PKG_NAME = "com.android.launcher3";
    private static final String PIXEL_LAUNCHER_PKG_NAME = "com.google.android.apps.nexuslauncher";

    private static final int LAUNCHER_PIXEL = 0;
    private static final int LAUNCHER_DERP = 1;
    private static final int LAUNCHER_LAWNCHAIR = 2;

    private static final String TAG = "CustomLauncherService";

    private final Context mContext;
    private final IOverlayManager mOM;
    private final IPackageManager mPM;
    private final IUserManager mUM;
    private final String mOpPackageName;

    private ServiceThread mWorker;
    private Handler mHandler;

    private boolean isPixelAvailable;
    private boolean isDerpAvailable;
    private boolean isLawnchairAvailable;

    public CustomLauncherService(Context context) {
        super(context);
        mContext = context;
        mOM = IOverlayManager.Stub.asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
        mPM = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mUM = IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE));
        mOpPackageName = context.getOpPackageName();
    }

    private void updateStateForUser(int userId, int launcher) {
        try {
            try {
                if (launcher == LAUNCHER_PIXEL) {
                    if (isPixelAvailable) mPM.setApplicationEnabledSetting(PIXEL_LAUNCHER_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                            0, userId, mOpPackageName);
                    if (isDerpAvailable) mPM.setApplicationEnabledSetting(DERP_LAUNCHER_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0, userId, mOpPackageName);
                    if (isLawnchairAvailable) mPM.setApplicationEnabledSetting(LAWNCHAIR_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0, userId, mOpPackageName);
                } else if (launcher == LAUNCHER_DERP) {
                    if (isPixelAvailable) mPM.setApplicationEnabledSetting(PIXEL_LAUNCHER_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0, userId, mOpPackageName);
                    if (isDerpAvailable) mPM.setApplicationEnabledSetting(DERP_LAUNCHER_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                            0, userId, mOpPackageName);
                    if (isLawnchairAvailable) mPM.setApplicationEnabledSetting(LAWNCHAIR_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0, userId, mOpPackageName);
                } else {
                    if (isPixelAvailable) mPM.setApplicationEnabledSetting(PIXEL_LAUNCHER_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0, userId, mOpPackageName);
                    if (isDerpAvailable) mPM.setApplicationEnabledSetting(DERP_LAUNCHER_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0, userId, mOpPackageName);
                    if (isLawnchairAvailable) {
                        mPM.setApplicationEnabledSetting(LAWNCHAIR_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                            0, userId, mOpPackageName);
                        mOM.setEnabled(LAWNCHAIR_OVERLAY_PKG_NAME, true, userId);
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    private void initForUser(int userId, int launcher) {
        if (userId < 0)
            return;

        updateStateForUser(userId, launcher);
    }

    private void init() {
        final int availableStatus = LauncherUtils.getAvailableStatus(mContext);
        isPixelAvailable = LauncherUtils.isPixelAvailable(availableStatus);
        isDerpAvailable = LauncherUtils.isDerpAvailable(availableStatus);
        isLawnchairAvailable = LauncherUtils.isLawnchairAvailable(availableStatus);

        if (!LauncherUtils.isInitialized()) {
            LauncherUtils.initialize();
        } else {
            if (isLawnchairAvailable) {
                try {
                    mOM.setEnabled(LAWNCHAIR_OVERLAY_PKG_NAME, false, USER_SYSTEM);
                } catch (RemoteException e) {
                    e.rethrowAsRuntimeException();
                }
            }
            if (availableStatus == 0) {
                LauncherUtils.setUnavailable();
            } else {
                final int launcher = LauncherUtils.getLastLauncher();
                LauncherUtils.setLauncher(launcher);
                try {
                    for (UserInfo user : mUM.getUsers(false, false, false)) {
                        initForUser(user.id, launcher);
                    }
                } catch (RemoteException e) {
                    e.rethrowAsRuntimeException();
                }
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        mContext.registerReceiver(new ShutdownReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_ADDED);
        mContext.registerReceiver(new UserReceiver(), filter,
                android.Manifest.permission.MANAGE_USERS, mHandler);
    }

    @Override
    public void onStart() {
        mWorker = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mWorker.start();
        mHandler = new Handler(mWorker.getLooper());

        init();
    }

    private final class ShutdownReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                if (LauncherUtils.getAvailableStatus(context) != 0) {
                    LauncherUtils.setLauncher(LauncherUtils.getLastLauncher());
                } else {
                    LauncherUtils.setUnavailable();
                }
            }
        }
    }

    private final class UserReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);

            if (Intent.ACTION_USER_ADDED.equals(intent.getAction())) {
                if (LauncherUtils.getAvailableStatus(context) != 0) {
                    initForUser(userId, LauncherUtils.getLauncher());
                }
            }
        }
    }
}
