/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.systemui.shared.system;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.IBinder;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * @see WallpaperManager
 */
public class WallpaperManagerCompat {
    private final WallpaperManager mWallpaperManager;

    public WallpaperManagerCompat(Context context) {
        mWallpaperManager = context.getSystemService(WallpaperManager.class);
    }

    /**
     * @see WallpaperManager#setWallpaperZoomOut(IBinder, float)
     */
    public void setWallpaperZoomOut(IBinder windowToken, float zoom) {
        mWallpaperManager.setWallpaperZoomOut(windowToken, zoom);
    }

    /**
     * @return the max scale for the wallpaper when it's fully zoomed out
     */
    public float getWallpaperZoomOutMaxScale(Context context) {
    boolean UseWpZoom = Settings.System.getIntForUser(context.getContentResolver(),
            Settings.System.DISABLE_WP_ZOOM, 0, UserHandle.USER_CURRENT) == 1;
        if (UseWpZoom) {
        return context.getResources()
                .getFloat(Resources.getSystem().getIdentifier(
                        /* name= */ "config_wallpaperMaxScaleDisabled",
                        /* defType= */ "dimen",
                        /* defPackage= */ "android"));
         } else {
         return context.getResources()
                .getFloat(Resources.getSystem().getIdentifier( 
                        /* name= */ "config_wallpaperMaxScaleDisabled",
                        /* defType= */ "dimen",
                        /* defPackage= */ "android"));
         }
     }
}
