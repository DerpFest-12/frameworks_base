package org.derpfest.systemui.dagger;

import com.android.systemui.dagger.DefaultComponentBinder;
import com.android.systemui.dagger.DependencyProvider;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.SystemUIBinder;
import com.android.systemui.dagger.SysUIComponent;
import com.android.systemui.dagger.SystemUIModule;

import org.derpfest.systemui.columbus.ColumbusModule;
import org.derpfest.systemui.elmyra.ElmyraModule;
import org.derpfest.systemui.keyguard.KeyguardSliceProviderDerpFest;
import org.derpfest.systemui.smartspace.KeyguardSmartspaceController;

import dagger.Subcomponent;

@SysUISingleton
@Subcomponent(modules = {
        ColumbusModule.class,
        DefaultComponentBinder.class,
        DependencyProvider.class,
        ElmyraModule.class,
        SystemUIModule.class,
        SystemUIDerpFestBinder.class,
        SystemUIDerpFestModule.class})
public interface SysUIComponentDerpFest extends SysUIComponent {
    @SysUISingleton
    @Subcomponent.Builder
    interface Builder extends SysUIComponent.Builder {
        SysUIComponentDerpFest build();
    }

    /**
     * Member injection into the supplied argument.
     */
    void inject(KeyguardSliceProviderDerpFest keyguardSliceProviderDerpFest);

    @SysUISingleton
    KeyguardSmartspaceController createKeyguardSmartspaceController();
}
