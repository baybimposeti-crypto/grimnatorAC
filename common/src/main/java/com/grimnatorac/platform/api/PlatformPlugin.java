package com.grimnatorac.platform.api;

public interface PlatformPlugin {
    boolean isEnabled();

    String getName();

    String getVersion();
}
