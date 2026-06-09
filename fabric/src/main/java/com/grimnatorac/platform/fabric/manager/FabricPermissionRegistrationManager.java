package com.grimnatorac.platform.fabric.manager;

import com.grimnatorac.platform.api.manager.PermissionRegistrationManager;
import com.grimnatorac.platform.api.permissions.PermissionDefaultValue;
import com.grimnatorac.platform.fabric.GrimnatorACFabricLoaderPlugin;
import com.grimnatorac.platform.fabric.sender.FabricSenderFactory;
import me.lucko.fabric.api.permissions.v0.Permissions;

import static com.grimnatorac.platform.fabric.sender.FabricSenderFactory.HAS_PERMISSIONS_API;

public class FabricPermissionRegistrationManager implements PermissionRegistrationManager {

    private final FabricSenderFactory fabricSenderFactory = GrimnatorACFabricLoaderPlugin.LOADER.getFabricSenderFactory();

    public FabricPermissionRegistrationManager() {
        registerPermission("grimnatorac.exempt", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.nosetback", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.nomodifypacket", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.nosetback", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.alerts.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.verbose.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.brand.enable-on-join", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.alerts.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.verbose.enable-on-join.silent", PermissionDefaultValue.FALSE);
        registerPermission("grimnatorac.brand.enable-on-join.silent", PermissionDefaultValue.FALSE);
    }

    @Override
    public void registerPermission(String name, PermissionDefaultValue defaultValue) {
        fabricSenderFactory.registerPermissionDefault(name, defaultValue);
        if (HAS_PERMISSIONS_API)
            Permissions.check(GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.createCommandSourceStack(), name);
    }
}
