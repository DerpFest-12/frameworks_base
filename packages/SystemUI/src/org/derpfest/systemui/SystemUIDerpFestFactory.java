package org.derpfest.systemui;

import android.content.Context;

import org.derpfest.systemui.dagger.DaggerGlobalRootComponentDerpFest;
import org.derpfest.systemui.dagger.GlobalRootComponentDerpFest;

import com.android.systemui.SystemUIFactory;
import com.android.systemui.dagger.GlobalRootComponent;

public class SystemUIDerpFestFactory extends SystemUIFactory {
    @Override
    protected GlobalRootComponent buildGlobalRootComponent(Context context) {
        return DaggerGlobalRootComponentDerpFest.builder()
                .context(context)
                .build();
    }
}
