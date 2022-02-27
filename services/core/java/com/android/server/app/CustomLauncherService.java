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

    private static final String LAWNCHAIR_PKG_NAME = "app.lawnchair";
    private static final String PIXEL_LAUNCHER_PKG_NAME = "com.google.android.apps.nexuslauncher";
    private static final String COMPONENT_OVERLAY_PKG_NAME = "com.android.launcher.recentsComponent.overlay";

    private static final String TAG = "CustomLauncherService";

    private final Context mContext;
    private final IOverlayManager mOM;
    private final IPackageManager mPM;
    private final IUserManager mUM;
    private final String mOpPackageName;

    private ServiceThread mWorker;
    private Handler mHandler;

    public CustomLauncherService(Context context) {
        super(context);
        mContext = context;
        mOM = IOverlayManager.Stub.asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
        mPM = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mUM = IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE));
        mOpPackageName = context.getOpPackageName();
    }

    private void updateStateForUser(int userId, boolean enabled) {
        try {
            try {
                if (enabled) {
                    mOM.setEnabled(COMPONENT_OVERLAY_PKG_NAME, true, userId);
                    mPM.setApplicationEnabledSetting(LAWNCHAIR_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                            0, userId, mOpPackageName);
                    mPM.setApplicationEnabledSetting(PIXEL_LAUNCHER_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0, userId, mOpPackageName);
                } else {
                    mPM.setApplicationEnabledSetting(LAWNCHAIR_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0, userId, mOpPackageName);
                    mPM.setApplicationEnabledSetting(PIXEL_LAUNCHER_PKG_NAME,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                            0, userId, mOpPackageName);
                }
            } catch (IllegalArgumentException ignored) {}
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    private void initForUser(int userId, boolean enabled) {
        if (userId < 0)
            return;

        updateStateForUser(userId, enabled);
    }

    private void init() {
        if (!LauncherUtils.isInitialized()) {
            LauncherUtils.initialize();
        } else {
            if (LauncherUtils.isOverlayAvailable(mContext)) {
                try {
                    mOM.setEnabled(COMPONENT_OVERLAY_PKG_NAME, false, USER_SYSTEM);
                } catch (RemoteException e) {
                    e.rethrowAsRuntimeException();
                }
            }
            if (!LauncherUtils.isAvailable(mContext)) {
                LauncherUtils.setUnavailable();
            } else {
                boolean enabled = LauncherUtils.getLastStatus();
                LauncherUtils.setEnabled(enabled);
                try {
                    for (UserInfo user : mUM.getUsers(false, false, false)) {
                        initForUser(user.id, enabled);
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
                if (LauncherUtils.isAvailable(context)) {
                    LauncherUtils.setEnabled(LauncherUtils.getLastStatus());
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
                if (LauncherUtils.isAvailable(context)) {
                    initForUser(userId, LauncherUtils.isEnabled());
                }
            }
        }
    }
}
