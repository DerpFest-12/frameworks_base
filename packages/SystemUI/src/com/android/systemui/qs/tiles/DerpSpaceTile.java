/*
 * Copyright (C) 2018-2021 The PixelDust Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.app.LocalePicker;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.R;

import javax.inject.Inject;

public class DerpSpaceTile extends QSTileImpl<BooleanState> {
    private boolean mListening;

    private static final String TAG = "DerpSpaceTile";

    private static final String DERPSPACE_PKG = "com.android.settings";
    private static final Intent DERPSPACE = new Intent(Intent.ACTION_MAIN)
        .setComponent(new ComponentName(DERPSPACE_PKG,
        "com.android.settings.Settings$DerpSpaceSettingsActivity"));

    @Inject
    public DerpSpaceTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        DERPSPACE.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DERP;
    }

    @Override
    protected void handleClick(@Nullable View view) {
        startDerpSpace();
    }

    @Override
    public Intent getLongClickIntent() {
        return DERPSPACE;
    }

    @Override
    protected void handleLongClick(@Nullable View view) {
        startDerpSpace();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_derp_label);
    }

    protected void startDerpSpace() {
        mHost.collapsePanels();
        mContext.startActivity(DERPSPACE);
        refreshState();
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.icon = ResourceIcon.get(R.drawable.ic_qs_derp);
        state.label = mContext.getString(R.string.quick_settings_derp_label);
        state.secondaryLabel = mContext.getString(R.string.quick_settings_derp_description);
        state.contentDescription = mContext.getString(R.string.quick_settings_derp_description);
        state.state = Tile.STATE_ACTIVE;
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }
}
