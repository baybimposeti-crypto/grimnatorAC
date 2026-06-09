package com.grimnatorac.checks;

import com.grimnatorac.GrimAPI;
import ac.grim.grimac.api.AbstractProcessor;
import ac.grim.grimac.api.config.ConfigReloadable;
import com.grimnatorac.utils.common.ConfigReloadObserver;

public abstract class GrimProcessor implements AbstractProcessor, ConfigReloadable, ConfigReloadObserver {

    // Not everything has to be a check for it to process packets & be configurable

    @Override
    public void reload() {
        reload(GrimAPI.INSTANCE.getConfigManager().getConfig());
    }

}
